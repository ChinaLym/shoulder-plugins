package org.shoulder.maven.plugins.mojo;

import cn.hutool.core.io.FileUtil;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.FieldHolderSource;
import org.jboss.forge.roaster.model.source.JavaEnumSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.shoulder.maven.plugins.pojo.ErrorCodeJavaDoc;
import org.shoulder.maven.plugins.util.ClassUtil;
import org.shoulder.maven.plugins.util.OpenJdkJavaDocParser;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生成错误码文档
 * 英文 description 默认取 message？ 还是调用翻译接口？
 *
 * @author lym
 */
@SuppressWarnings({"all"})
@Execute(goal = "generateErrorCodeInfo")
@Mojo(name = "generateErrorCodeInfo", defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class ErrorCodeInfoGeneratorForOpenJdk extends AbstractMojo {

    private static final String ERRORCODE_CLASS = "org.shoulder.core.exception.ErrorCode";

    private static final String APPINFO_CLASS = "org.shoulder.core.context.AppInfo";

    /**
     * 源码目录
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
    private File sourceDirectory;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * 源码文件编码 默认 UTF-8
     */
    @Parameter(property = "sourceCodeCharset", defaultValue = "UTF-8")
    private String sourceCodeCharset;

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
     * 默认的建议
     */
    @Parameter(property = "defaultSuggestionInfo", defaultValue = "1.请前往服务器 xxx 目录查看并导出并保留程序运行日志 \\r\\n 2.联系相应开发人员根据保留的日志信息，协助排查。")
    private String defaultSuggestionInfo;

    /**
     * 输出文件编码 默认 UTF-8
     */
    @Parameter(property = "outputCharset", defaultValue = "UTF-8")
    private String outputCharset;

    /**
     * 生成错误码信息文件的目标路径，错误码信息文件名称，推荐为应用 标识_error_code.properties
     */
    @Parameter(property = "outputFile", defaultValue = "${project.build.outputDirectory}/language/zh_CN/errorCode.properties")
    private File outputFile;

    /**
     * 编译后的源代码输出到哪里
     */
    @Parameter(property = "compileOutputDirectory", defaultValue = "${project.build.outputDirectory}")
    private File compileOutputDirectory;

    /**
     * 生成的错误码信息格式，默认 properties，根据 fileName 后缀可为 properties，json，默认 properties
     */
    @Parameter(property = "formatType", defaultValue = "properties")
    private String formatType;


    // 生成错误码信息文件的目标路径，错误码信息文件名称，推荐为应用 标识_error_code.properties
    /*@Parameter(property = "tempDir", defaultValue = "${project.build.directory}/shoulderTempDir")
    private File tempDir;

    // 依赖分析缓存
    @Parameter(property = "enableCache", defaultValue = "true")
    private boolean enableCache;

    // 依赖分析缓存
    @Parameter(property = "useCache", defaultValue = "false")
    private boolean useCache;*/


    public static String NEW_LINE = "\r\n";//System.getProperty("line.separator");

    public Charset OUTPUT_CHARSET;

    public ErrorCodeInfoGeneratorForOpenJdk() {
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("=====================【START】 shoulder-maven-plugin:generateErrorCodeInfo =====================");
        getLog().info("源码目录 sourceDirectory: " + sourceDirectory);
        getLog().info("需要扫描的包路径 scanPackage: " + scanPackage);

        OUTPUT_CHARSET = Charset.forName(outputCharset);

        ClassLoader classLoader = ClassUtil.getProjectClassLoader(project, this.getClass().getClassLoader(), getLog());
        ClassUtil.setClassLoader(classLoader);
        OpenJdkJavaDocParser.cl = classLoader;
        try {
            // 注册 shoulder 的 AppInfo 信息 todo，如果没有，则尝试读取 application.properties / application.yml "shoulder.application.errorCodePrefix"
            Class appInfoClass = ClassUtil.getClassLoader().loadClass(APPINFO_CLASS);
            Method appInfo_initErrorCodePrefix = appInfoClass.getMethod("initErrorCodePrefix", String.class);
            appInfo_initErrorCodePrefix.invoke(appInfoClass, errorCodePrefix);

            // 列出所有类
            List<Class<?>> allClasses = ClassUtil.getAllClass(sourceDirectory.getAbsolutePath(), scanPackage);

            // 获取所有错误码实现类
            List<Class<?>> allErrorCodeImplList = ClassUtil.filterSonOfClass(allClasses, ERRORCODE_CLASS);

            // 获取所有规范命名的错误码常量类 【类名中包含 ErrorCode】 且不是 ErrorCode 的子类
            List<Class<?>> allErrorCodeConstantClasses = allClasses.stream().filter(c -> c.getSimpleName().contains("ErrorCode")).filter(c -> !c.isEnum() && !c.isPrimitive()).filter(c -> {
                Class<?>[] interfaces = c.getInterfaces();
                if (c.getInterfaces() == null || interfaces.length == 0) {
                    return true;
                }
                // 且不能是 ErrorCode 的子类
                return true;
            }).collect(Collectors.toList());

            // 过滤出所有异常类、枚举类
            List<Class<Enum>> errCodeEnumList = allErrorCodeImplList.stream().filter(Class::isEnum).map(c -> (Class<Enum>) c).collect(Collectors.toList());

            // 拿到这些类的 javaDoc 信息
            Set<String> fileNames = allErrorCodeImplList.stream().map(Class::getSimpleName).map(className -> className + ".java").collect(Collectors.toSet());
            fileNames.addAll(allErrorCodeConstantClasses.stream().map(Class::getSimpleName).map(className -> className + ".java").collect(Collectors.toSet()));
            List<String> sourceCodeFilePathList = ClassUtil.listFilesAndSelect(new File(sourceDirectory.getAbsolutePath()), f -> fileNames.contains(f.getName()));

            List<List<String>> enumErrorCodeInfoList = new ArrayList<>();
            // READ JAVA DOC
            List<JavaSource<?>> javaSources = readJavaDocx(sourceCodeFilePathList);
            for (JavaSource<?> javaSource : javaSources) {
                if (javaSource instanceof JavaEnumSource) {
                    Map<String, ErrorCodeJavaDoc> map = OpenJdkJavaDocParser.readEnumClassFieldAndDoc(javaSource);
                    if (map != null && !map.isEmpty()) {
                        List<String> errorCodeInfoList = OpenJdkJavaDocParser.convertToErrorCodeInfo(javaSource.getCanonicalName(), map, this::genDescriptionKey, this::genSuggestionKey);
                        enumErrorCodeInfoList.add(errorCodeInfoList);
                    }
                } else if (javaSource instanceof Exception) {
                    // todo ex class: use calss doc
                } else if (javaSource instanceof FieldHolderSource && javaSource.getName().contains("ErrorCode")) {
                    // class name contains ErrorCode: use public static String field doc
                    Map<String, ErrorCodeJavaDoc> map = OpenJdkJavaDocParser.readEnumClassFieldAndDoc(javaSource);
                    if (map != null && !map.isEmpty()) {
                        List<String> errorCodeInfoList = OpenJdkJavaDocParser.convertToErrorCodeInfo(javaSource.getCanonicalName(), map, this::genDescriptionKey, this::genSuggestionKey);
                        enumErrorCodeInfoList.add(errorCodeInfoList);
                    }
                }
            }
//
//            // 解析异常类
//            List<Class<Exception>> exList = allErrorCodeImplList.stream()
//                    .filter(Exception.class::isAssignableFrom)
//                    .map(e -> (Class<Exception>) e)
//                    .collect(Collectors.toList());
//            getLog().info("exception(impl ErrorCode) class num:" + exList.size());
            //List<String> exErrorCodeInfoList = generateByExList(errCodeEnumList);

            // 写文件
            outputErrorCodeInfo(enumErrorCodeInfoList);
            getLog().info("generate SUCCESS, writing to outputDir: " + outputFile.getAbsolutePath());

            getLog().info("=====================【END】 shoulder-maven-plugin:generateErrorCodeInfo =====================");

        } catch (Exception e) {
            getLog().error("shoulder-error-code-plugin fail, please send the bug info to shoulder.org~", e);
            throw new MojoExecutionException(e.getMessage());
        } finally {
            ClassUtil.clean();
        }
    }


    /**
     * 生成错误码信息，默认将其写入文件
     *
     * @param allErrorCodeInfoList 错误码信息行，外层按错误码所在文件分，内层按照单个枚举字段分
     */
    private void outputErrorCodeInfo(List<List<String>> allErrorCodeInfoList) {
        getLog().info("number of analyzed class: " + allErrorCodeInfoList.size());
        // 先备份旧的
        File bak = new File(outputFile.getPath() + ".bak");
        try {
            if (FileUtil.exist(outputFile)) {
                FileUtil.move(outputFile, bak, true);
                FileUtil.del(outputFile);
            }
            FileUtil.touch(outputFile);
            for (List<String> errorCodeInfoGroup : allErrorCodeInfoList) {
                getLog().debug("line num: " + errorCodeInfoGroup.size());

                // 将每个分组的信息写入
                StringBuilder perGroup = new StringBuilder(NEW_LINE);
                // 每两行之间插入空格
                int count = 0;
                for (String errorCodeInfo : errorCodeInfoGroup) {
                    perGroup.append(errorCodeInfo).append(NEW_LINE)
                    //.append(errorCodeInfo.startsWith(i18nKeyPrefix) && errorCodeInfo.contains(suggestionSuffix) ? NEW_LINE : "")
                    ;
                }
                FileUtil.appendString(perGroup.toString(), outputFile, OUTPUT_CHARSET);
            }
        } catch (Exception e) {
            getLog().error("outputErrorCodeInfo FAIL", e);
            // 还原
            if (FileUtil.exist(bak)) {
                FileUtil.move(bak, outputFile, true);
            }
        }

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

    private List<JavaSource<?>> readJavaDocx(List<String> sourceCodeFilePathList) throws InterruptedException {
        return sourceCodeFilePathList.stream().map(path -> FileUtil.readString(path, sourceCodeCharset)).parallel().map(javaSourceContent -> Roaster.parse(JavaSource.class, javaSourceContent)).map(s -> (JavaSource<?>) s).collect(Collectors.toList());
    }
}
