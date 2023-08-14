package org.shoulder.maven.plugins.test;

import cn.hutool.core.io.FileUtil;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.EnumDeclaration;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.SimpleName;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.TagElement;
import org.jboss.forge.roaster.model.JavaDoc;
import org.jboss.forge.roaster.model.JavaDocTag;
import org.jboss.forge.roaster.model.impl.JavaClassImpl;
import org.jboss.forge.roaster.model.impl.JavaEnumImpl;
import org.jboss.forge.roaster.model.source.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Test {
    public static String TestField = "xx";
    public static final String TestField2 = "yy";
    public static final boolean TestField3 = true;

    public static Map<String, String> readClassDoc(String sourceCode) {
        JavaSource<?> javaSrc = Roaster.parse(JavaSource.class, sourceCode);
        JavaDoc<?> doc = javaSrc.getJavaDoc();
        List<JavaDocTag> tagList = doc.getTags();
        return tagList.stream().collect(Collectors.toMap(JavaDocTag::getName, JavaDocTag::getValue));
    }
    public static Map<String, String> readEnumClassFieldAndDoc(String sourceCode) {
        JavaSource<?> javaSrc = Roaster.parse(JavaSource.class, sourceCode);
        if(javaSrc instanceof JavaEnumSource) {
            List<EnumConstantSource> enumConstantList = ((JavaEnumSource)javaSrc).getEnumConstants();
            for (EnumConstantSource enumConstant : enumConstantList) {
                String n = enumConstant.getName();
                List<JavaDocTag> tagDocList = enumConstant.getJavaDoc().getTags();
                Map<String, String> tagList = tagDocList.stream().collect(Collectors.toMap(JavaDocTag::getName, JavaDocTag::getValue));
            }
        }
        JavaDoc<?> doc = javaSrc.getJavaDoc();
        List<JavaDocTag> tagList = doc.getTags();
        return tagList.stream().collect(Collectors.toMap(JavaDocTag::getName, JavaDocTag::getValue));
    }

    public static Map<String, String> readFieldDoc(String sourceCode) {
        JavaSource<?> javaSrc = Roaster.parse(JavaSource.class, sourceCode);
        if(javaSrc instanceof FieldHolderSource) {
            List<FieldSource> fields = ((FieldHolderSource) javaSrc).getFields();
            for (FieldSource field : fields) {
                if(field.isPublic() && field.isStatic() && "java.lang.String".equals(field.getType().getQualifiedName())) {
                    JavaDoc<?> doc = field.getJavaDoc();
                    List<JavaDocTag> tagList = doc.getTags();


                    return tagList.stream().collect(Collectors.toMap(JavaDocTag::getName, JavaDocTag::getValue));
                }
            }
        }
        JavaDoc<?> doc = javaSrc.getJavaDoc();
        List<JavaDocTag> tagList = doc.getTags();
        return tagList.stream().collect(Collectors.toMap(JavaDocTag::getName, JavaDocTag::getValue));
    }
    public static void main(String[] args) {
        readClassDoc(FileUtil.readString(JavadocReader.TEST_Ex_FILE, StandardCharsets.UTF_8));
        readEnumClassFieldAndDoc(FileUtil.readString(JavadocReader.TEST_ErrorCodeEnum_FILE, StandardCharsets.UTF_8));
        readFieldDoc(FileUtil.readString("D:\\code\\java\\self\\shoulder-plugins\\plugins\\errcode-maven-plugin\\src\\test\\java\\org\\shoulder\\maven\\plugins\\test\\Test.java", StandardCharsets.UTF_8));
    }
}
