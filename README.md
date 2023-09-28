# shoulder-plugins

## 简介
shoulder-framework 的插件

### 功能说明

按照 `shoulder` 推荐的风格编写代码时，可以通过该插件，自动生成错误码的相关信息，供其他软件/程序读取或使用。

- 可生成多语言对应的 key，供翻译能力读取（为什么不在代码中：配置文件方式方便修改）
- 可检索所有错误码，供统一索引，如提供错误码统一查询平台，根据错误码查询原因和建议
- ....


### 使用说明


在项目中引入以下插件
```
    <build>
        <plugins>
        
            <!-- shoulder 错误码插件 -->
            <plugin>
                <groupId>cn.itlym.shoulder.plugins</groupId>
                <artifactId>errcode-maven-plugin</artifactId>
                <version>0.0.1-SNAPSHOT</version>
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

### 参与贡献

1.  Fork 本仓库
2.  新建 issue/{issue对应编号} 分支
3.  提交代码
4.  新建 Pull Request

5.  新模块命名：该项目是 shoulder 协助插件，故主要与shoulder相关，groupId需要携带shoulder，举例：
```xml
<groupId>org.jetbrains.kotlin</groupId>
<artifactId>kotlin-maven-plugin</artifactId>

<groupId>org.apache.maven.plugins</groupId>
<artifactId>maven-compiler-plugin</artifactId>
```