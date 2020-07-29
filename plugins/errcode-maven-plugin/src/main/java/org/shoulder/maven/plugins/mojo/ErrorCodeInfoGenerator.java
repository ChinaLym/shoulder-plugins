package org.shoulder.maven.plugins.mojo;

import cn.hutool.core.io.FileUtil;
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
import java.nio.charset.StandardCharsets;
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
     * 生成错误码信息文件的目标路径，错误码信息文件名称，推荐为应用 标识_error_code.properties
     */
    @Parameter(property = "outputFile", defaultValue = "output/errorCode.properties")
    private File outputFile;

    /**
     * 生成的错误码信息格式，默认 properties，根据 fileName 后缀可为 properties，json，默认 properties
     */
    private String formatType;


    public ErrorCodeInfoGenerator() {
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("源码目录 sourceDirectory: " + sourceDirectory);
        getLog().info("需要扫描的包路径 scanPackage: " + scanPackage);
        ClassUtil.setClassLoader(getProjectClassLoader(project));
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

            // 获取错误码信息。暂时只支持枚举
            List<List<String>> enumErrorCodeInfoList = generateErrorCodeInfoByEnumList(errCodeEnumList);
            //List<String> exErrorCodeInfoList = generateByExList(errCodeEnumList);

            getLog().info("generate SUCCESS, writing to outputDir: " + outputFile.getAbsolutePath());
            // 写文件
            outputErrorCodeInfo(enumErrorCodeInfoList);

        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }


    /**
     * 生成错误码信息，默认将其写入文件
     * @param allErrorCodeInfoList 错误码信息行
     */

    private void outputErrorCodeInfo(List<List<String>> allErrorCodeInfoList) {
        for (List<String> errorCodeInfoList : allErrorCodeInfoList) {
            StringBuilder perGroup = new StringBuilder("\r\n");
            errorCodeInfoList.forEach(perGroup::append);
            FileUtil.writeString(perGroup.toString(), outputFile, StandardCharsets.UTF_8);
        }
    }

    /**
     * 根据枚举型错误码生成错误码对应文档的每行
     * @param errCodeEnumList 枚举类类列表
     * @return 错误码信息行，每个枚举一个 List
     */
    private List<List<String>> generateErrorCodeInfoByEnumList(List<Class<? extends ErrorCode>> errCodeEnumList) {

        // 所有行
        List<List<String>> errorCodeInfoList = new LinkedList<>();

        errCodeEnumList.forEach(errCodeEnumClazz -> {

            // 特定类的包含错误码信息的每一行
            List<String> errorCodeInfo = new LinkedList<>();
            getLog().debug("analyzing Enum: " + errCodeEnumClazz.getName());
            errorCodeInfo.add(genEnumSplitLine(errCodeEnumClazz));
            // 获取所有枚举实例
            ErrorCode[] instances = errCodeEnumClazz.getEnumConstants();
            for (ErrorCode instance : instances) {
                getLog().debug("analyzing Enum-Item: " + instance);
                String errorCode = instance.getCode();
                errorCodeInfo.add(genDescriptionKey(errorCode));
                errorCodeInfo.add(genSuggestionKey(errorCode));
            }
            errorCodeInfoList.add(errorCodeInfo);
        });
        return errorCodeInfoList;
    }


    /**
     * 根据异常型错误码生成错误码对应文档的每行
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


    /**
     * 自定义类加载器，以获取目标项目的类环境
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
