package org.shoulder.maven.plugins.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.shoulder.core.exception.ErrorCode;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 生成错误码文档
 *
 * @author lym
 * @goal 生成错误码文档
 * @goal extract
 * @phase package
 * @requiresDependencyResolution compile
 */
@Mojo(name = "generateErrorCodeInfo", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ErrorCodeInfoGenerator extends AbstractMojo {

    /**
     * 源码目录
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}", required = true, readonly = true)
    private File sourceDirectory;;

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
     * 生成错误码信息文件的目标路径
     */
    @Parameter(property = "outputDirectory", defaultValue = "./output")
    private String outputDirectory;

    /**
     * 错误码信息文件名称，推荐为应用 标识_error_code.properties
     */
    @Parameter(property = "fileName", defaultValue = "errorCode.properties")
    private String fileName;

    /**
     * 生成的错误码信息格式，默认 properties，可选 properties，json
     */
    @Parameter(property = "formatType", defaultValue = "properties")
    private String formatType;

    @Parameter(defaultValue = "${project.build.sourceDirectory}", required = true, readonly = true)
    private File sourceDir;

    public ErrorCodeInfoGenerator() {
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("源码目录 sourceDirectory: " + sourceDirectory);
        getLog().info("需要扫描的包路径 scanPackage: " + scanPackage);
        ClassUtil.setClassLoader(getClassLoader(project));
        try {

            // 获取所有错误码实现类
            List<Class<? extends ErrorCode>> allErrorCodeImplList =
                    ClassUtil.getAllSonOfClass(sourceDirectory.getAbsolutePath(), scanPackage, ErrorCode.class);

            // 过滤出所有异常类、枚举类
            List<Class<? extends ErrorCode>> errCodeEnumList = allErrorCodeImplList.stream()
                    .filter(Class::isEnum)
                    .collect(Collectors.toList());

            getLog().info("enum(impl ErrorCode) class num:" + errCodeEnumList.size());

            List<Class<? extends ErrorCode>> exList = allErrorCodeImplList.stream()
                    .filter(RuntimeException.class::isAssignableFrom)
                    .collect(Collectors.toList());

            getLog().info("exception(impl ErrorCode) class num:" + exList.size());

            // 获取
            List<String> enumErrorCodeInfoList = generateByEnumList(errCodeEnumList);
            List<String> exErrorCodeInfoList = generateByExList(errCodeEnumList);

            // 写文件
            generateErrorCodeInfo(enumErrorCodeInfoList);

        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }


    /**
     * 生成错误码信息，默认将其写入文件
     * @param errorCodeInfoList 错误码信息行
     */
    private void generateErrorCodeInfo(List<String> errorCodeInfoList) {
        String outputFileFullPath = "";
        errorCodeInfoList.forEach(getLog()::info);
    }

    /**
     * 根据枚举型错误码生成错误码对应文档的每行
     * @param errCodeEnumList 枚举类类列表
     * @return 错误码信息行
     */
    private List<String> generateByEnumList(List<Class<? extends ErrorCode>> errCodeEnumList) {

        // 包含错误码信息的每一行
        List<String> errorCodeInfoList = new LinkedList<>();

        errCodeEnumList.forEach(errCodeEnumClazz -> {
            errorCodeInfoList.add(genEnumSplitLine(errCodeEnumClazz));
            // 获取所有枚举实例
            ErrorCode[] instances = errCodeEnumClazz.getEnumConstants();
            for (ErrorCode instance : instances) {
                String errorCode = instance.getCode();
                errorCodeInfoList.add(genDescriptionKey(errorCode));
                errorCodeInfoList.add(genSuggestionKey(errorCode));
            }
        });
        return errorCodeInfoList;
    }


    /**
     * 根据异常型错误码生成错误码对应文档的每行
     * @param errCodeExList 异常类列表
     * @return 错误码信息行
     */
    private List<String> generateByExList(List<Class<? extends ErrorCode>> errCodeExList) {
        return Collections.emptyList();
    }

    /**
     * 生成完整错误码
     *
     * @param sourceErrorCode 源错误码，不带应用标识前缀
     * @return 生成完整错误码
     */
    private String formatErrorCode(String sourceErrorCode) {
        return errorCodePrefix + sourceErrorCode;
    }

    /**
     * 生成错误码描述信息 key
     *
     * @param sourceErrorCode 源错误码，不带应用标识前缀
     * @return 错误码描述信息 key
     */
    private String genDescriptionKey(String sourceErrorCode) {
        return i18nKeyPrefix + formatErrorCode(sourceErrorCode) + descriptionSuffix;
    }

    /**
     * 生成错误码对应的排查解决建议信息 key
     *
     * @param sourceErrorCode 源错误码，不带应用标识前缀
     * @return 错误码对应的排查解决建议信息 key
     */
    private String genSuggestionKey(String sourceErrorCode) {
        return i18nKeyPrefix + formatErrorCode(sourceErrorCode) + suggestionSuffix;
    }

    /**
     * 生成错误码枚举的分割行注释
     *
     * @param errCodeEnumClazz 错误码枚举
     * @return 错误码枚举的分割注释 key
     */
    private String genEnumSplitLine(Class<? extends ErrorCode> errCodeEnumClazz) {
        return "# " + errCodeEnumClazz.getName();
    }


    private ClassLoader getClassLoader(MavenProject project) {
        try {
            List<String> classpathElements = project.getCompileClasspathElements();
            classpathElements.add(project.getBuild().getOutputDirectory());
            classpathElements.add(project.getBuild().getTestOutputDirectory());
            URL[] urls = new URL[classpathElements.size()];
            for (int i = 0; i < classpathElements.size(); ++i) {
                urls[i] = new File(classpathElements.get(i)).toURI().toURL();
            }
            return new URLClassLoader(urls, this.getClass().getClassLoader());
        } catch (Exception e) {
            getLog().debug("Couldn't get the classloader.");
            return this.getClass().getClassLoader();
        }
    }
}
