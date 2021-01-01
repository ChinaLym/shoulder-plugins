package org.shoulder.maven.plugins.mojo;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.sun.javadoc.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.shoulder.maven.plugins.pojo.ErrorCodeJavaDoc;
import org.shoulder.maven.plugins.util.ClassUtil;
import org.shoulder.maven.plugins.util.JarUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 生成错误码文档
 * todo 英文 description 默认取 message
 * todo 错误码格式有误，怀疑 class 文件未更新
 *
 * @author lym
 * @goal 生成错误码文档
 * @goal extract
 * @phase package
 * @requiresDependencyResolution compile
 */
@SuppressWarnings({"all"})
@Mojo(name = "generateErrorCodeInfo", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ErrorCodeInfoGenerator extends AbstractMojo {

    /**
     * 源码目录
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
    private File sourceDirectory;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * 错误码前缀（与应用挂钩），必填，如 "0x0001" 代表用户中心
     */
    @Parameter(property = "errorCodePrefix", required = true)
    private String errorCodePrefix;

    /**
     * 要扫描错误码的路径，一般是自己的包路径，如 org.shoulder
     */
    @Parameter(property = "scanPackage", required = true)
    private String scanPackage;

    /**
     * 统一错误码多语言key的前缀，默认为 "err."
     */
    @Parameter(property = "i18nKeyPrefix", defaultValue = "err.")
    private String i18nKeyPrefix;

    /**
     * 生成错误码描述信息的后缀，默认为 ".desc"
     */
    @Parameter(property = "descriptionSuffix", defaultValue = ".desc")
    private String descriptionSuffix;

    /**
     * 生成错误码解决建议信息的后缀，默认为 ".sug"
     */
    @Parameter(property = "suggestionSuffix", defaultValue = ".sug")
    private String suggestionSuffix;

    /**
     * 生成错误码信息文件的目标路径，错误码信息文件名称，推荐为应用 标识_error_code.properties
     */
    @Parameter(property = "outputFile", defaultValue = "${project.build.directory}/errorCode.properties")
    private File outputFile;

    /**
     * 生成错误码信息文件的目标路径，错误码信息文件名称，推荐为应用 标识_error_code.properties
     */
    @Parameter(property = "genJavaDocTempDir", defaultValue = "${project.build.directory}/shoulderTempDir")
    private File tempDir;

    /**
     * 依赖分析缓存
     */
    @Parameter(property = "enableCache", defaultValue = "true")
    private boolean enableCache;

    /**
     * 依赖分析缓存
     */
    @Parameter(property = "useCache", defaultValue = "false")
    private boolean useCache;

    /**
     * 默认的建议 todo 只能使用一次？
     */
    @Parameter(property = "defaultSuggestionInfo", defaultValue = "1.请前往服务器 xxx 目录查看并导出并保留程序运行日志 \\r\\n 2.联系相应开发人员根据保留的日志信息，协助排查")
    private String defaultSuggestionInfo;

    /**
     * 生成的错误码信息格式，默认 properties，根据 fileName 后缀可为 properties，json，默认 properties
     */
    private String formatType;

    public static String NEW_LINE = "\r\n";//System.getProperty("line.separator");

    private static RootDoc rootDoc = null;

    public ErrorCodeInfoGenerator() {
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("源码目录 sourceDirectory: " + sourceDirectory);
        getLog().info("需要扫描的包路径 scanPackage: " + scanPackage);
        ClassUtil.setClassLoader(getProjectClassLoader(project));
        try {
            // 列出所有类
            List<Class<?>> allClasses =
                    ClassUtil.getAllClass(sourceDirectory.getAbsolutePath(), scanPackage);

            // 获取所有错误码实现类
            List<Class<?>> allErrorCodeImplList =
                    ClassUtil.filterSonOfClass(allClasses, "org.shoulder.core.exception.ErrorCode");

            // 获取所有规范命名的错误码常量类 【类名中包含 ErrorCode】 且不是 ErrorCode 的子类
            List<Class<?>> allErrorCodeConstantClasses = allClasses.stream()
                    .filter(c -> c.getSimpleName().contains("ErrorCode"))
                    .filter(c -> !c.isEnum() && !c.isPrimitive())
                    .filter(c -> {
                        Class<?>[] interfaces = c.getInterfaces();
                        if (c.getInterfaces() == null || interfaces.length == 0) {
                            return true;
                        }
                        // 且不能是 ErrorCode 的子类
                        return true;
                    })
                    .collect(Collectors.toList());

            // 过滤出所有异常类、枚举类
            List<Class<Enum>> errCodeEnumList = allErrorCodeImplList.stream()
                    .filter(Class::isEnum)
                    .map(c -> (Class<Enum>) c)
                    .collect(Collectors.toList());

            // 拿到这些类的 javaDoc 信息
            Set<String> fileNames = allErrorCodeImplList.stream()
                    .map(Class::getSimpleName)
                    .map(className -> className + ".java")
                    .collect(Collectors.toSet());
            fileNames.addAll(allErrorCodeConstantClasses.stream().map(Class::getSimpleName).map(className -> className + ".java").collect(Collectors.toSet()));
            List<String> sourceCodeFiles = ClassUtil.listFilesAndSelect(new File(sourceDirectory.getAbsolutePath()), f -> fileNames.contains(f.getName()));
            readJavaDoc(sourceCodeFiles, project.getCompileClasspathElements());


            // 解析枚举 的错误码信息
            getLog().info("enum(impl ErrorCode) class num:" + errCodeEnumList.size());
            List<List<String>> enumErrorCodeInfoList = generateErrorCodeInfoByEnumList(errCodeEnumList);

            // 解析常量类，接口
            enumErrorCodeInfoList.add(generateByConstantClassList(allErrorCodeConstantClasses));

            // 解析异常类
            List<Class<Exception>> exList = allErrorCodeImplList.stream()
                    .filter(Exception.class::isAssignableFrom)
                    .map(e -> (Class<Exception>) e)
                    .collect(Collectors.toList());
            getLog().info("exception(impl ErrorCode) class num:" + exList.size());
            //List<String> exErrorCodeInfoList = generateByExList(errCodeEnumList);

            // 写文件 todo 每个类名，注释
            outputErrorCodeInfo(enumErrorCodeInfoList);
            getLog().info("generate SUCCESS, writing to outputDir: " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }


    /**
     * 生成错误码信息，默认将其写入文件
     *
     * @param allErrorCodeInfoList 错误码信息行
     */
    private void outputErrorCodeInfo(List<List<String>> allErrorCodeInfoList) {
        getLog().info("analyzed count: " + allErrorCodeInfoList.size());
        // 先备份旧的
        if (FileUtil.exist(outputFile)) {
            FileUtil.move(outputFile, new File(outputFile.getPath() + ".bak"), true);
            FileUtil.del(outputFile);
        }
        FileUtil.touch(outputFile);
        for (List<String> errorCodeInfoGroup : allErrorCodeInfoList) {
            getLog().info("count: " + errorCodeInfoGroup.size());
            StringBuilder perGroup = new StringBuilder(NEW_LINE + NEW_LINE);
            // 每两行之间插入空格
            int count = 0;
            for (String errorCodeInfo : errorCodeInfoGroup) {
                perGroup.append(errorCodeInfo)
                        .append(NEW_LINE)
                        .append(errorCodeInfo.startsWith(i18nKeyPrefix) && errorCodeInfo.contains(suggestionSuffix) ? NEW_LINE : "");
            }
            FileUtil.appendString(perGroup.toString(), outputFile, StandardCharsets.UTF_8);

            /*getLog().info("count: " + errorCodeInfoGroup.size());
            List<String> linesToWrite = new ArrayList<>(errorCodeInfoGroup.size() + 2);
            linesToWrite.add("");
            linesToWrite.add("");
            // 每两行之间插入空格
            int count = 0;
            for (String errorCodeInfo : errorCodeInfoGroup) {
                linesToWrite.add(errorCodeInfo);
                if((count ++ & 1) == 0){
                    linesToWrite.add("");
                }
            }
            FileUtil.appendLines(linesToWrite, outputFile, StandardCharsets.UTF_8);*/
        }
    }


    /**
     * 根据枚举型错误码生成错误码对应文档的每行
     *
     * @param errCodeEnumList 枚举类类列表
     * @return 错误码信息行，每个枚举一个 List
     */
    private List<List<String>> generateErrorCodeInfoByEnumList(List<Class<Enum>> errCodeEnumList) {

        // 所有行
        List<List<String>> errorCodeInfoList = new LinkedList<>();
        errCodeEnumList.forEach(errCodeEnumClazz -> {
            Method getCodeMethod = ClassUtil.findNoParamMethod(errCodeEnumClazz, "getCode");
            // 通过完整类名，获取到对应的 javaDoc
            ClassDoc classDoc = rootDoc.classNamed(errCodeEnumClazz.getName());
            if (classDoc == null) {
                getLog().error(errCodeEnumClazz.getName() + " without classDoc == null");
                return;
            }
            Map<String, FieldDoc> fieldDocMap = Arrays.stream(classDoc.fields()).collect(Collectors.toMap(Doc::name, d -> d));
            // 特定类的包含错误码信息的每一行
            List<String> errorCodeInfo = new LinkedList<>();
            getLog().debug("analyzing Enum: " + errCodeEnumClazz.getName());
            errorCodeInfo.add("#########################");
            errorCodeInfo.add("# " + errCodeEnumClazz.getName());
            errorCodeInfo.add("#########################");
            // 获取所有枚举实例
            Enum[] instances = errCodeEnumClazz.getEnumConstants();
            for (Enum instance : instances) {
                getLog().debug("analyzing Enum-Item: " + instance);
                String fieldName = instance.name();
                FieldDoc doc = fieldDocMap.get(fieldName);
                if (doc == null) {
                    getLog().error(errCodeEnumClazz.getName() + "without FieldDoc(" + fieldName + ") == null");
                    continue;
                }
                ErrorCodeJavaDoc errorCodeJavaDoc = analyzeFieldDoc(doc);
                try {
                    String errorCode = (String) getCodeMethod.invoke(instance);
                    getLog().debug(errCodeEnumClazz.getName() + "." + fieldName + " errorCode=" + errorCode + errorCodeJavaDoc);
                    errorCodeInfo.add("# " + fieldName);
                    errorCodeInfo.add(genDescriptionKey(errorCode) + "=" + errorCodeJavaDoc.description);
                    errorCodeInfo.add(genSuggestionKey(errorCode) + "=" + errorCodeJavaDoc.suggestion);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            errorCodeInfoList.add(errorCodeInfo);
        });
        return errorCodeInfoList;
    }


    /**
     * 根据异常型错误码生成错误码对应文档的每行
     *
     * @param errCodeExList 异常类列表 ErrorCode 的子类
     * @return 错误码信息行
     */
    private List<String> generateByExList(List<Class<? extends Exception>> errCodeExList) {
        List<String> errorCodeInfo = new LinkedList<>();
        for (Class<? extends Exception> ex : errCodeExList) {
            // 直接调 getCode 获取，如果不为 null 说明这是专属异常，记录，否则跳过
        }
        return Collections.emptyList();
    }

    /**
     * 常量类 / 接口
     */
    private List<String> generateByConstantClassList(List<Class<?>> errCodeConstantClassList) {
        List<String> errorCodeInfo = new LinkedList<>();
        for (Class clazz : errCodeConstantClassList) {
            // 列出全部 public static String 常量字段
            List<Field> fields = Arrays.stream(clazz.getFields())
                    .filter(f -> f.getType() == String.class)
                    .filter(f -> Modifier.isStatic(f.getModifiers()))
                    .collect(Collectors.toList());
            if (fields.isEmpty()) {
                continue;
            }
            ClassDoc classDoc = rootDoc.classNamed(clazz.getName());
            Map<String, FieldDoc> fieldDocMap = Arrays.stream(classDoc.fields())
                    .collect(Collectors.toMap(Doc::name, d -> d));
            errorCodeInfo.add("#########################");
            errorCodeInfo.add("# " + clazz.getName());
            errorCodeInfo.add("#########################");
            for (Field field : fields) {
                try {
                    FieldDoc doc = fieldDocMap.get(field.getName());
                    String errorCode = (String) field.get(clazz);
                    ErrorCodeJavaDoc errorCodeJavaDoc = analyzeFieldDoc(doc);
                    errorCodeInfo.add(genDescriptionKey(errorCode) + "=" + errorCodeJavaDoc.description);
                    errorCodeInfo.add(genSuggestionKey(errorCode) + "=" + errorCodeJavaDoc.suggestion);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
        return errorCodeInfo;
    }

    /**
     * 生成完整错误码
     *
     * @param sourceErrorCode 源错误码，不带应用标识前缀
     * @return 生成完整错误码
     */
    /*private String formatErrorCode(String sourceErrorCode) {
        final int excepted = 8;
        int addZero = excepted - sourceErrorCode.length();
        return errorCodePrefix + "0".repeat(Math.max(0, addZero)) + sourceErrorCode;
    }
*/

    /**
     * 生成错误码描述信息 key
     *
     * @param sourceErrorCode 源错误码，不带应用标识前缀
     * @return 错误码描述信息 key
     */
    private String genDescriptionKey(String sourceErrorCode) {
        return i18nKeyPrefix + sourceErrorCode + descriptionSuffix;
    }

    /**
     * 生成错误码对应的排查解决建议信息 key
     *
     * @param sourceErrorCode 源错误码，不带应用标识前缀
     * @return 错误码对应的排查解决建议信息 key
     */
    private String genSuggestionKey(String sourceErrorCode) {
        return i18nKeyPrefix + sourceErrorCode + suggestionSuffix;
    }

    /**
     * 生成错误码枚举的分割行注释
     *
     * @param errCodeEnumClazz 错误码枚举
     * @return 错误码枚举的分割注释 key
     */
    private String genEnumSplitLine(Class<?> errCodeEnumClazz) {
        return "# " + errCodeEnumClazz.getName();
    }


    /**
     * 自定义类加载器，以获取目标项目的类环境
     *
     * @param project mavenProject
     * @return 目标项目编译环境类加载器
     */
    private ClassLoader getProjectClassLoader(MavenProject project) {
        try {
            List<String> classpathList = project.getCompileClasspathElements();
            classpathList.add(project.getBuild().getOutputDirectory());
            classpathList.add(project.getBuild().getTestOutputDirectory());
            // 转为 URL
            URL[] urls = new URL[classpathList.size()];
            for (int i = 0; i < classpathList.size(); ++i) {
                urls[i] = new File(classpathList.get(i)).toURI().toURL();
            }
            // 生成类加载器
            return new URLClassLoader(urls, this.getClass().getClassLoader());
        } catch (Exception e) {
            getLog().warn("Couldn't get the aim project classloader. Fallback to plugin classLoader.");
            return this.getClass().getClassLoader();
        }
    }


    /**
     * 分析字段文档，从javadoc注释中提取标签
     * todo 如果没有 @desc 则直接使用注释，如果没有 @sug 则使用默认值
     */
    @Nonnull
    private ErrorCodeJavaDoc analyzeFieldDoc(FieldDoc fieldDoc) {
        ErrorCodeJavaDoc result = new ErrorCodeJavaDoc();
        Tag[] languages = fieldDoc.tags("language");
        if (languages != null && languages.length > 0) {
            result.setLanguage(languages[0].text().trim());
        }
        if (StrUtil.isBlank(result.language)) {
            result.setLanguage("zh_CN");
        }

        Tag[] descriptions = fieldDoc.tags("desc");
        if (descriptions != null && descriptions.length > 0) {
            String description = descriptions[0].text().trim();
            result.setDescription(description);
        }

        result.setSuggestion(defaultSuggestionInfo);
        Tag[] suggestions = fieldDoc.tags("sug");
        if (suggestions != null && suggestions.length > 0) {
            String suggestion = suggestions[0].text().trim();
            if (StrUtil.isNotBlank(suggestion)) {
                result.setSuggestion(suggestion);
                if (StrUtil.isBlank(result.description)) {
                    // todo 多行问题，取第一行？还是把换行符替换成空格？ todo 去掉多余的 @tag
                    String all = fieldDoc.getRawCommentText().trim();
                    System.out.println(all.contains("\r\n"));
                    System.out.println(all.contains("\r"));
                    System.out.println(all.contains("\n"));
                    result.setDescription(all);
                }
            }
        }
        return result;
    }

    private void readJavaDoc(List<String> sourceCode, List<String> classesDirectory) throws InterruptedException {
        String[] commandArg = {
                "-doclet", Doclet.class.getName(),
                "-quiet", // 不产生输出
                "-encoding", "utf-8",
                "-classpath"
        };
        getLog().debug("=========== sourceCode ==========");
        sourceCode.forEach(getLog()::debug);
        getLog().debug("==================================");

        getLog().debug("========= classesDirectory =======");
        classesDirectory.forEach(getLog()::debug);
        getLog().debug("==================================");

        ExecutorService executor = new ThreadPoolExecutor(8, 8,
                2, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        if (useCache && tempDir.exists()) {
            // 且这个目录存在
            getLog().info("============ use cache ===========");
        } else {
            // 先删除整个目录 todo 计时 | 重写类加载器，支持加载多个 jar 路径的资源
            FileUtil.del(tempDir);
            getLog().info("============ first build cache: copying classes =============");
            // 把插件所在工程依赖的第三方 jar 解压到 tempDir，然后使用 tempDir 这个过程太慢了，考虑使用多线程。 减去插件所在工程 todo 实际只处理 if 条件满足的
            CountDownLatch latch = new CountDownLatch(classesDirectory.size() - 1);
            Log log = getLog();
            for (String classPath : classesDirectory) {
                executor.execute(() -> {
                    if (classPath.endsWith(".jar")) {
                        // 仅处理 jar
                        try {
                            log.debug("START copying " + classPath);
                            JarUtil.unzipJar(classPath, tempDir.getPath());
                            log.debug("FINISHED copying " + classPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    latch.countDown();
                });
            }
            executor.shutdown();
            latch.await();
            getLog().info("============ copy finished =============");
        }

        // 无论是否使用缓存，均更新插件所在工程源码 class
        for (String classPath : classesDirectory) {
            File[] files = new File(classPath).listFiles(ff -> StrUtil.isAllCharMatch(ff.getName(), Character::isLowerCase) && ff.isDirectory());
            if (files != null && files.length > 0) {
                for (File file : files) {
                    // todo 不能保证已经删了的类，class 也删除（数据一致性）
                    FileUtil.copy(file.getPath(), tempDir.getPath(), true);
                }
            }
        }
        List<String> args = new ArrayList<>(commandArg.length + classesDirectory.size() + sourceCode.size());
        args.addAll(Arrays.stream(commandArg).collect(Collectors.toList()));
        StringJoiner sj = new StringJoiner(",");
        classesDirectory.forEach(sj::add);
        //args.add(sj.toString());
        args.add(tempDir.getPath());

        args.addAll(sourceCode);
        String[] docArgs = args.toArray(new String[0]);
        com.sun.tools.javadoc.Main.execute(docArgs);

        // 删掉临时目录
        if (enableCache) {
            return;
        }
        FileUtil.del(tempDir);
        getLog().debug("del " + tempDir.getPath());
    }

    public static class Doclet {
        public static boolean start(RootDoc root) {
            rootDoc = root;
            return true;
        }
    }

}
