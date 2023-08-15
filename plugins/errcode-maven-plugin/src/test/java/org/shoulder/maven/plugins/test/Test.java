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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Test {
    public static       String  TestField  = "xx";
    public static final String  TestField2 = "yy";
    public static final boolean TestField3 = true;

    //

    /**
     * 每个类前加入：
     * #########################
     * # ClassName
     * #########################
     * 常量类 fields 每个字段一个 ErrorCodeJavaDoc
     * Enum：每个 constant 字段一个 ErrorCodeJavaDoc
     * 低优先-异常类：不涉及JavaDoc
     */

    public static ErrorCodeJavaDoc readClassDoc(JavaSource javaSrc) {
        return parseDoc(javaSrc);
    }

    public static Map<String, ErrorCodeJavaDoc> readEnumClassFieldAndDoc(JavaSource<?> javaSrc) {
        Map<String, ErrorCodeJavaDoc> fieldDocMap = Optional.of(javaSrc)
                .filter(s -> s instanceof JavaEnumSource)
                .map(s -> (JavaEnumSource) s)
                .map(JavaEnumSource::getEnumConstants)
                .filter(CollectionUtil::isNotEmpty)
                .map(enumConstants -> {
                    Map<String, ErrorCodeJavaDoc> map = new HashMap<>();
                    for (EnumConstantSource enumConstant : enumConstants) {
                        String name = enumConstant.getName();
                        ErrorCodeJavaDoc errorCodeJavaDoc = parseDoc(enumConstant);
                        map.put(name, errorCodeJavaDoc);
                    }
                    return map;
                })
                .orElse(null);
        return fieldDocMap;
    }

    public static Map<String, ErrorCodeJavaDoc> readFieldDocFromFinalStringFields(JavaSource<?> javaSrc) {
        //JavaSource<?> javaSrc = Roaster.parse(JavaSource.class, sourceCode);
        Map<String, ErrorCodeJavaDoc> fieldDocMap = Optional.of(javaSrc)
                .filter(s -> s instanceof FieldHolderSource)
                .map(s -> (FieldHolderSource<?>) s)
                .map(FieldHolderSource::getFields)
                .filter(CollectionUtil::isNotEmpty)
                .map(fields -> {
                    Map<String, ErrorCodeJavaDoc> map = new HashMap<>();
                    for (FieldSource<?> field : fields) {
                        if (field.isPublic() && field.isStatic() && "java.lang.String".equals(field.getType().getQualifiedName())) {
                            String name = field.getName();
                            ErrorCodeJavaDoc errorCodeJavaDoc = parseDoc(field);
                            map.put(name, errorCodeJavaDoc);
                        }
                    }
                    return map;
                })
                .orElse(null);
        return fieldDocMap;
    }

    public static ErrorCodeJavaDoc parseDoc(JavaDocCapableSource<?> javaDocCapableSource) {
        List<JavaDocTag> tagList = Optional.ofNullable(javaDocCapableSource)
                .map(JavaDocCapableSource::getJavaDoc)
                .map(JavaDoc::getTags)
                .filter(CollectionUtil::isNotEmpty)
                .orElse(null);

        if (CollectionUtil.isEmpty(tagList)) {
            return null;
        }

        boolean havingValue = false;
        ErrorCodeJavaDoc errorCodeJavaDoc = new ErrorCodeJavaDoc();
        for (JavaDocTag tag : tagList) {
            if ("@desc".equals(tag.getName())) {
                errorCodeJavaDoc.setDescription(tag.getValue());
                havingValue = true;
            } else if ("@sug".equals(tag.getName())) {
                errorCodeJavaDoc.setSuggestion(tag.getValue());
                havingValue = true;
            } else if ("@language".equals(tag.getName())) {
                errorCodeJavaDoc.setLanguage(tag.getValue());
            }
        }
        return havingValue ? errorCodeJavaDoc : null;
    }


    public static void main(String[] args) {


        JavaSource<?> errorEnumSource = Roaster.parse(JavaSource.class, FileUtil.readString("CommonErrorCodeEnum.java", StandardCharsets.UTF_8));
        readEnumClassFieldAndDoc(errorEnumSource);

        JavaSource<?> staticStringFieldSource = Roaster.parse(JavaSource.class, FileUtil.readString(
                "MyErrorCodes.java", StandardCharsets.UTF_8));

        readFieldDocFromFinalStringFields(staticStringFieldSource);

        JavaSource<?> classSource = Roaster.parse(JavaSource.class, FileUtil.readString(JavadocReader.TEST_Ex_FILE, StandardCharsets.UTF_8));
        readClassDoc(classSource);
    }
}
