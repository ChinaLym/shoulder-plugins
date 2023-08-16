package org.shoulder.maven.plugins.test;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaDoc;
import org.jboss.forge.roaster.model.JavaDocTag;
import org.jboss.forge.roaster.model.source.EnumConstantSource;
import org.jboss.forge.roaster.model.source.FieldHolderSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaDocCapableSource;
import org.jboss.forge.roaster.model.source.JavaEnumSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.shoulder.maven.plugins.pojo.ErrorCodeJavaDoc;
import org.shoulder.maven.plugins.util.OpenJdkJavaDocParser;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OpenJdkParseDocTest {

    public static void main(String[] args) {

        JavaSource<?> errorEnumSource = Roaster.parse(JavaSource.class, FileUtil.readString("CommonErrorCodeEnum.java", StandardCharsets.UTF_8));
        OpenJdkJavaDocParser.readEnumClassFieldAndDoc(errorEnumSource);

        JavaSource<?> staticStringFieldSource = Roaster.parse(JavaSource.class, FileUtil.readString(
                "MyErrorCodes.java", StandardCharsets.UTF_8));

        OpenJdkJavaDocParser.readFieldDocFromFinalStringFields(staticStringFieldSource);

        JavaSource<?> classSource = Roaster.parse(JavaSource.class, FileUtil.readString(JavadocReader.TEST_Ex_FILE, StandardCharsets.UTF_8));
        OpenJdkJavaDocParser.readClassDoc(classSource);
    }
}
