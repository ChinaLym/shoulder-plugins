package org.shoulder.maven.plugins.test;

import cn.hutool.core.io.FileUtil;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.shoulder.maven.plugins.util.OpenJdkJavaDocParser;

import java.nio.charset.StandardCharsets;

public class OpenJdkParseDocTest {

    public static void main(String[] args) {

        JavaSource<?> errorEnumSource = Roaster.parse(JavaSource.class, FileUtil.readString("CommonErrorCodeEnum.java", StandardCharsets.UTF_8));
        OpenJdkJavaDocParser.readEnumClassFieldAndDoc(errorEnumSource);

        JavaSource<?> staticStringFieldSource = Roaster.parse(JavaSource.class, FileUtil.readString(
                "MyErrorCodes.java", StandardCharsets.UTF_8));

        OpenJdkJavaDocParser.readFieldDocFromFinalStringFields(staticStringFieldSource);

        JavaSource<?> classSource = Roaster.parse(JavaSource.class, FileUtil.readString("BusinessRuntimeException.java", StandardCharsets.UTF_8));
        OpenJdkJavaDocParser.readClassDoc(classSource);
    }
}
