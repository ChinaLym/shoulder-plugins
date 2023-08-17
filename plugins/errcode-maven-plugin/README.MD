# 错误码、翻译文件生成 maven 插件
该工程需要jdk lib目录下的 tools.jar，本地编译需要修改 pom.xml 的路径

https://blog.csdn.net/qq_42403906/article/details/127633176

## 使用

### 构建配置中引入该插件

pom.xml

```xml

    <build>
        <plugins>
            <!-- 自动生成错误码以及对应的翻译 可以在 target 目录生成所有实现 ErrorCode 接口错误码枚举类的翻译文件 -->
            <plugin>
                <groupId>cn.itlym</groupId>
                <artifactId>shoulder-maven-plugin</artifactId>
                <version>0.1-SNAPSHOT</version>
                <configuration>
                    <!-- 【必填】本组件对应的错误码前缀 -->
                    <errorCodePrefix>0x000a</errorCodePrefix>
                    <!-- 【必填】要生成错误码文件，扫描的包路径，通常为组件错误码枚举/常量类所在包路径，越精确，越能减少插件构建时间 -->
                    <scanPackage>com.example.demo1</scanPackage>
                    <!-- @sug 标签缺少时，补充的默认值 -->
                    <defaultSuggestionInfo>1.请前往服务器 xxx 目录查看并导出并保留程序运行日志 \r\n 2.联系相应开发人员根据保留的日志信息，协助排查。</defaultSuggestionInfo>
                </configuration>
                <executions>
                    <!-- 开启插件的哪些功能 -->
                    <execution>
                        <!-- 在 mvn compile 阶段触发该插件。该插件占用时间大概在几秒，compile 开启即可，也可以延迟到 package 等 -->
                        <phase>compile</phase>
                        <goals>
                            <!-- 执行该插件的 生成错误码翻译 能力 -->
                            <goal>generateErrorCodeInfo</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

        </plugins>
    </build>
```


### 功能

1. 允许将描述 建议写在代码注释,并根据注释生成可插拔的 错误码解释文件(方便后续替换与修改)
2. 支持生成多种语言的翻译

### 代码中使用(一个类仅同时支持生效一种方式)

1. 枚举类实现 ErrorCode 接口 枚举常量上方写注释

```java

public enum DemoErrorCodeEnum implements ErrorCode {

    /**
     * @desc 报名者年龄不符合要求
     * @sug 输入合法的值
     */
    AGE_OUT_OF_RANGE(100001, "age out of range"),

    /**
     * 开发人员看的注释
     * 
     * @desc 错误描述
     * @sug 发生后的排查建议
     */
    SIGN_UP_FAIL(100002, "third service error"),

    ;

    private String code;

    private String message;

    DemoErrorCodeEnum(String code) {
        this.code = code;
    }

    /**
     * 转为异常抛出时，默认记录 warn 日志，返回 500 状态码
     */
    DemoErrorCodeEnum(long code, String message) {
        String hex = Long.toHexString(code);
        this.code = "0x" + "0".repeat(Math.max(0, 8 - hex.length())) + hex;
        this.message = message;
    }


    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

}

```

2. 类名带特定字符(ErrorCode)的类内静态字段

```java

public class ErrorCodeConstant {
    
    /**
     * @desc 错误描述
     * @sug 发生后的排查建议
     */
    public static final String ERROR_CODE_1 = "0x112233";

    /**
     * @desc 错误描述
     * @sug 发生后的排查建议
     */
    public static String ERROR_CODE_2 = "0x" + App.id + "33";
}
```


3. 【暂不推荐】~~异常类实现 Error code 接口, 在类上注释~~

```java
/**
 * 序列化异常，非强制捕获
 * 由于这类异常及其可能为编码导致，故不应该报给前端暴露代码漏洞，应由开发排查
 *
 * @author lym
 * 
 * @desc 错误描述-序列化异常
 * @sug 发生后的排查建议-联系开发: #app.developers#
 */
public class SerialException extends BaseRuntimeException {

    private static final long serialVersionUID = 5330049957369887114L;

    //xxxxx
}
```

## 使用方式介绍

一般的使用：

```java
    /**
     * @desc 错误描述
     * @sug 发生后的排查建议
     */
     XXX     

```

可能会出现的情况：

- 不写任何标签
```java
    /**
     * 调用外部接口失败
     */
     XXX     

```
此时，将使用注释内容作为错误码的描述部分(相当于使用了 @desc 标签)

- 只写了 @desc
```java
    /**
     * @desc 调用外部接口失败
     */
     XXX     

```
插件生成 @sug 时，将使用插件声明时配置的参数 `defaultSuggestionInfo`，默认为 `1.请前往服务器 xxx 目录查看并导出并保留程序运行日志 \r\n 2.联系相应开发人员根据保留的日志信息，协助排查。`

----


### 以下情况插件是不推荐的写法，需求明确之后再做


- ~~只写了 @sug~~
```java
    /**
     * @sug 检查日志
     */
     XXX     

```
**目前不建议这么写，不规范的编码，不利于排查问题**


- 标签中内容存在换行
```java
    /**
     * @desc 这是一条有换行的描述
     *  第二行
     * @sug 检查日志
     */
     XXX     

```
**目前不建议这么写，语言不够简练**


- 标签内容存在换行
```java
    /**
     * @desc 这是一条有换行的描述
     *  第二行
     * @sug 检查日志
     */
     XXX     

```
**目前不建议这么写，语言不够简练**


- 标签内容存在 html 标签，如 `<p>`
```java
    /**
     * @desc 这是一条有 html 标签的描述
     * <p>
     *  第二行
     * @sug 检查日志
     */
     XXX     

```
**目前不建议这么写，显示格式不应写死在后端开发代码里**

---

## IDE 提示问题

在 javadoc中写 `@desc`，IDEA 会有警告：wrong tag 'desc'

是因为该标签为自定义标签，未将该标签添加到 IDEA 的自定义标签列表，若要消除警告，只需要添加到自定义标签列表即可

`alt + enter` -> `add to custom tags`


----

## 原理

- 扫描目标项目，获取所有和 `org.shoulder.core.exception.ErrorCode` 相关的类
- 根据类定义（枚举类、常量类、异常类）启动不同的解析策略
- 利用 jdk 中的 javaDoc 解析工具，获取目标类的 javaDoc
- 最终解析对应的 javaDoc 上的 @desc、@sug 来生成所需要的 properties 文件

### 重点

- maven 插件有自己的类加载器，默认无法加载到项目的类，因此需要使用单独的类加载器加载
- 目标项目有自己的类路径，其可能会依赖第三方jar，而第三方jar通常是 maven 仓库中的jar，因此需要使用支持 jar 的 ClassLoad

### 其他

在插件执行中，触发 jdk 解析 javaDoc 时，jdk 会有以下警告：

- 错误: 找不到符号
- 错误: 程序包org.shoulder.core.exception不存在

这是 jdk 的 javaDoc 工具类的输出，并不影响生成，忽略这些即可：

原因：shoulder 为了加快插件的生成速度，默认告诉 `jdk`只解析工程中的类，跳过非目标工程中的类，但 `jdk` 未提供关闭这些输出的相关配置。

**由于占用大量时间、磁盘、CPU，强烈推荐关闭，并忽略警告（默认就是关闭）**。

~~若实在不想看到这些提示，加入 `<strictMode>true</strictMode>` 即可，但这可能会导致，每个模块额外消耗几分甚至十几分钟时间 + 100M+ 的磁盘空间，供 jdk 中 sun 的 javaDoc 工具解析，由于代价太高，默认关闭。~~


