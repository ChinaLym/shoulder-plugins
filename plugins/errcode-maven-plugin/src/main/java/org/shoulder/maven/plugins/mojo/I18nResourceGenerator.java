package org.shoulder.maven.plugins.mojo;

import jdk.javadoc.doclet.DocletEnvironment;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.shoulder.core.exception.ErrorCode;
import org.shoulder.maven.plugins.util.ClassUtil;
import org.springframework.context.MessageSourceResolvable;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 生成翻译资源包，扫描所有 {@link MessageSourceResolvable} 生成对应的多语言翻译，默认生成中文，英文 key，以注释作为中文翻译
 *
 * @author lym
 * @goal 生成翻译资源包
 * @goal extract
 * @phase package
 * @requiresDependencyResolution compile
 */
@Mojo(name = "generateI18nResource", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class I18nResourceGenerator extends AbstractMojo {


    private DocletEnvironment docletEnvironment;

    /**
     * 源码目录，扫描多语言翻译的路径，用于提取翻译
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}", required = true, readonly = true)
    private File sourceDirectory;
    ;


    /**
     * 用于获取类加载器
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * 需要扫描的包路径，可以指定该值仅生成特定包路径下的对应翻译，如 org.shoulder
     */
    @Parameter(property = "scanPackage")
    private String scanPackage;

    /**
     * 统一多语言key的前缀，默认为空，可以设置为应用标识
     */
    @Parameter(property = "i18nKeyPrefix", defaultValue = "")
    private String i18nKeyPrefix;

    /**
     * 生成错误码信息文件的目标路径，默认为 output/language
     */
    @Parameter(property = "outputFile", defaultValue = "output/language")
    private File outputFile;

    /**
     * 生成的错误码信息格式，默认 properties，可为 properties xml
     */
    private String formatType;

    // todo 策略，一个类一个文件？

    public static String NEW_LINE = "\r\n";//System.getProperty("line.separator");

    public I18nResourceGenerator() {
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("源码目录 sourceDirectory: " + sourceDirectory);
        getLog().info("需要扫描的包路径 scanPackage: " + scanPackage);
        ClassUtil.setClassLoader(getProjectClassLoader(project));
        try {

            //docletEnvironment.getDocTrees().getDocCommentTree(JavacProcessingEnvironment.getElementUtils()).get

            // 获取所有多语言实现类
            List<Class<? extends MessageSourceResolvable>> allI18nAbleList =
                    ClassUtil.getAllSonOfClass(sourceDirectory.getAbsolutePath(), scanPackage, MessageSourceResolvable.class);

            // 过滤出所有枚举类
            List<Class<? extends MessageSourceResolvable>> i18nAbleEnumList = allI18nAbleList.stream()
                    .filter(Class::isEnum)
                    .collect(Collectors.toList());

            getLog().info("enum(impl MessageSourceResolvable) class num:" + i18nAbleEnumList.size());

            // todo 获取所有类名后缀包含 I18nTip 的类和接口，获取其内部所有常量

            // 获取枚举对应的多语言翻译文件内容 key 枚举名，value 包含的一行行内容
            Map<String, List<String>> i18nMap = generateI18nResourceByEnumList(i18nAbleEnumList);
            //List<String> exErrorCodeInfoList = generateByExList(i18nAbleEnumList);

            getLog().info("generate SUCCESS, writing to outputDir: " + outputFile.getAbsolutePath());
            // 写文件
            outputI18nInfo(i18nMap);

        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }


    /**
     * 根据解析的翻译相关信息，默认将其写入文件
     *
     * @param i18nMap 翻译相关
     */
    private void outputI18nInfo(Map<String, List<String>> i18nMap) {
        i18nMap.forEach((k, values) -> {
            // todo 写到文件
            /*StringBuilder perGroup = new StringBuilder(NEW_LINE + NEW_LINE);
            // 每两行之间插入空格
            int count = 0;
            for (String errorCodeInfo : errorCodeInfoGroup) {
                perGroup.append(errorCodeInfo)
                        .append(NEW_LINE)
                        .append((count++ & 1) == 0 ? NEW_LINE : "");
            }
            FileUtil.writeString(perGroup.toString(), outputFile, StandardCharsets.UTF_8);*/
        });
    }

    /**
     * 根据枚举的所有枚举值和对应注释生成多语言文件的每行
     *
     * @param i18nEnumList 枚举类类列表
     * @return 对应的多语言翻译文件内容 key 枚举名，value 包含的一行行内容
     */
    private Map<String, List<String>> generateI18nResourceByEnumList(List<Class<? extends MessageSourceResolvable>> i18nEnumList) {

        // 所有行
        Map<String, List<String>> i18nInfoList = new HashMap<>(i18nEnumList.size());

        i18nEnumList.forEach(errCodeEnumClazz -> {

            // 特定类的包含错误码信息的每一行
            List<String> errorCodeInfo = new LinkedList<>();
            getLog().debug("analyzing Enum: " + errCodeEnumClazz.getName());
            errorCodeInfo.add(genEnumSplitLine(errCodeEnumClazz));
            // 获取所有枚举实例
            MessageSourceResolvable[] instances = errCodeEnumClazz.getEnumConstants();
            for (MessageSourceResolvable instance : instances) {
                getLog().debug("analyzing Enum-Item: " + instance);
                String[] errorCodes = instance.getCodes();
                if(errorCodes != null && errorCodes.length > 0){
                    for (String errorCode : errorCodes) {
                        // todo value 从 java doc 中读取？
                        errorCodeInfo.add(formatI18nKey(errorCode) + "=");
                    }
                }
            }
            i18nInfoList.put(errCodeEnumClazz.getSimpleName(), errorCodeInfo);
        });
        return i18nInfoList;
    }


    /**
     * 根据异常型错误码生成错误码对应文档的每行
     *
     * @param errCodeExList 异常类列表
     * @return 错误码信息行
     */

    private List<String> generateByExList(List<Class<? extends ErrorCode>> errCodeExList) {
        List<String> errorCodeInfo = new LinkedList<>();
        for (Class<? extends ErrorCode> ex : errCodeExList) {
        }
        return Collections.emptyList();
    }

    /**
     * 生成完整 key
     *
     * @param i18nKey key
     * @return 生成完整key
     */

    private String formatI18nKey(String i18nKey) {
        return i18nKeyPrefix + i18nKey;
    }


    /**
     * 生成错误码枚举的分割行注释
     *
     * @param errCodeEnumClazz 错误码枚举
     * @return 错误码枚举的分割注释 key
     */

    private String genEnumSplitLine(Class<? extends MessageSourceResolvable> errCodeEnumClazz) {
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
}
