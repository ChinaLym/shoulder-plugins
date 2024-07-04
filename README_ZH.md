<h1 align="center"><img src="doc/img/shoulder_plugin_svg_logo.svg" height="40" width="40" /><a href="https://github.com/ChinaLym/shoulder-plugins" target="_blank">Shoulder Plugin</a></h1>

[![AUR](https://img.shields.io/badge/license-Apache%20License%202.0-yellow.svg)](https://github.com/ChinaLym/shoulder-framework)
[![](https://img.shields.io/badge/Author-lym-yellow.svg)](https://github.com/ChinaLym)
[![](https://img.shields.io/badge/CICD-PASS-green.svg)](https://github.com/ChinaLym/shoulder-framework)

[![](https://img.shields.io/badge/Latest%20Version-1.2.2-blue.svg)](https://github.com/ChinaLym/shoulder-plugins)

# 📖 简介
自动生成错误码、多语言文件 maven 插件.

搭配[shoulder-framework](https://github.com/ChinaLym/shoulder-framework) 中[多语言部分](https://github.com/ChinaLym/shoulder-framework/blob/master/shoulder-build/shoulder-base/shoulder-core/README.md#%E7%BF%BB%E8%AF%91%E4%B8%8E%E5%A4%9A%E8%AF%AD%E8%A8%80).

---

# 🚀 快速开始

在项目中引入以下插件
```
    <build>
        <plugins>
        
            <!-- shoulder 插件 -->
            <plugin>
                <groupId>cn.itlym.shoulder</groupId>
                <artifactId>shoulder-maven-plugin</artifactId>
                <version>1.2.2</version>
                <executions>
                    <execution>
                        <!-- 编译时触发该插件 -->
                        <phase>compile</phase>
                        <!-- 使用该插件的生成错误码信息功能 -->
                        <goals>
                            <goal>generateErrorCodeInfo</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            
        </plugins>
    </build>
```

详细使用见 [插件介绍](plugins/errcode-maven-plugin/README.MD).


### 功能说明

引入本插件后，在 maven 打包时，自动生成错误码的相关信息（多语言翻译、error message不同环境的说明），供其他软件/程序读取或使用。

- 根据代码的注释信息生成多语言对应的 key，供翻译能力读取（为什么不在代码中：配置文件方式方便修改）
- 可检索所有错误码，供统一索引，如提供错误码统一查询平台，根据错误码查询原因和建议
- ....

### 效果

1. 去 [shoulder-framework-demo1](https://github.com/ChinaLym/shoulder-framework-demo/tree/main/demo1) 中查看demo中`人工`写法 [翻译文件](https://github.com/ChinaLym/shoulder-framework-demo/blob/main/demo1/src/main/resources/language/zh_CN/messages.properties) 的翻译文件
![manual.png](doc/img/manual.png)

2. 去 [shoulder-framework](https://github.com/ChinaLym/shoulder-framework/blob/master/shoulder-build/shoulder-base/shoulder-core/pom.xml) 查看框架中`自动生成`的使用

> shoulder-core 引入该插件后，编译时，会根据代码注释自动生成以下用于展示多语言错误码提示的文件，并自动打包进jar中。

![shoulder-usecase.png](doc/img/shoulder-usecase.png)

# ❓常见问题

**Q:** [shoulder-framework](https://github.com/ChinaLym/shoulder-framework) 是什么?
- **A:** a fantastic framework based on [Spring Boot](https://github.com/spring-projects/spring-boot)

# 💗 贡献代码

欢迎各类型代码提交，不限于`优化代码格式`、`优化注释/JavaDoc`、`修复 BUG`、`新增功能`，更多请参考 [如何贡献代码](CONTRIBUTING.MD)

# 🤝🏼 联系我们 & 建议

感谢小伙伴们的 **[Star](https://gitee.com/ChinaLym/shoulder-framework/star)** 、 **Fork** 、 **PR**，欢迎使用 `issue` 或 [cn_lym@foxmail.com](mailto:cn_lym@foxmail.com) 交流，如 留下你的建议、期待的新功能等~
