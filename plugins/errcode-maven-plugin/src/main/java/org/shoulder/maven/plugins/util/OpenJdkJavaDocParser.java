package org.shoulder.maven.plugins.util;

import cn.hutool.core.util.ReflectUtil;
import org.jboss.forge.roaster.model.JavaDoc;
import org.jboss.forge.roaster.model.JavaDocTag;
import org.jboss.forge.roaster.model.source.*;
import org.shoulder.maven.plugins.pojo.ErrorCodeJavaDoc;

import java.util.*;
import java.util.function.Function;

public class OpenJdkJavaDocParser {
    public static volatile ClassLoader cl = OpenJdkJavaDocParser.class.getClassLoader();

    /**
     * 每个类前加入：
     * #########################
     * # ClassName
     * #########################
     * 常量类 fields 每个字段一个 ErrorCodeJavaDoc
     * Enum：每个 constant 字段一个 ErrorCodeJavaDoc
     * 低优先-异常类：不涉及JavaDoc
     */

    public static List<String> convertToErrorCodeInfo(String className, Map<String, ErrorCodeJavaDoc> map,
                                                      Function<String, String> descriptionKeyTranslator,
                                                      Function<String, String> suggestionKeyTranslator) {
        List<String> errorCodeInfo = new ArrayList<>(3 + map.size() * 3);
        errorCodeInfo.add("#########################");
        errorCodeInfo.add("# " + className);
        errorCodeInfo.add("#########################");
        map.forEach((k, errorCodeJavaDoc) -> {
            errorCodeInfo.add("# " + k);
            errorCodeInfo.add(descriptionKeyTranslator.apply(errorCodeJavaDoc.getErrorCode()) + "=" + errorCodeJavaDoc.getDescription());
            errorCodeInfo.add(suggestionKeyTranslator.apply(errorCodeJavaDoc.getErrorCode()) + "=" + errorCodeJavaDoc.getSuggestion());
        });
        return errorCodeInfo;
    }

    public static ErrorCodeJavaDoc readClassDoc(JavaSource javaSrc) {
        return parseDoc(javaSrc);
    }

    public static String getCodeFromEnum(String enumFullName, String enumConstantName) {
        Class<?> enumClass = null;
        try {
            enumClass = cl.loadClass(enumFullName);
        } catch (ClassNotFoundException e) {
            // todo log error、cache
            return null;
        }
        Enum<?>[] instances = (Enum<?>[]) enumClass.getEnumConstants();
        for (Enum<?> instance : instances) {
            if (instance.name().equals(enumConstantName)) {
                return ReflectUtil.invoke(instance, "getCode");
            }
        }
        return null;
    }

    /**
     * @param javaSrc JavaEnumSource
     * @return
     */
    public static Map<String, ErrorCodeJavaDoc> readEnumClassFieldAndDoc(JavaSource<?> javaSrc) {
        String fullName = javaSrc.getCanonicalName();
        Map<String, ErrorCodeJavaDoc> fieldDocMap = Optional.of(javaSrc)
                .filter(s -> s instanceof JavaEnumSource)
                .map(s -> (JavaEnumSource) s)
                .map(JavaEnumSource::getEnumConstants)
                .filter(c -> !c.isEmpty())
                .map(enumConstants -> {
                    Map<String, ErrorCodeJavaDoc> map = new HashMap<>();
                    for (EnumConstantSource enumConstant : enumConstants) {
                        String name = enumConstant.getName();
                        ErrorCodeJavaDoc errorCodeJavaDoc = parseDoc(enumConstant);
                        Optional.ofNullable(errorCodeJavaDoc)
                                .ifPresent(errJavaDoc -> {
                                    // 拿到该类 class，反射查找 getCode 方法，反射拿到对应值
                                    String errorCode = getCodeFromEnum(fullName, name);
                                    errJavaDoc.setErrorCode(errorCode);
                                    map.put(name, errJavaDoc);
                                });
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
                .filter(c -> !c.isEmpty())
                .map(fields -> {
                    Map<String, ErrorCodeJavaDoc> map = new HashMap<>();
                    for (FieldSource<?> field : fields) {
                        if (field.isPublic() && field.isStatic() && "java.lang.String".equals(field.getType().getQualifiedName())) {
                            String name = field.getName();
                            ErrorCodeJavaDoc errorCodeJavaDoc = parseDoc(field);
                            Optional.ofNullable(errorCodeJavaDoc)
                                    .ifPresent(errJavaDoc -> {
                                        String errorCode = field.getStringInitializer();
                                        errJavaDoc.setErrorCode(errorCode);
                                        map.put(name, errJavaDoc);
                                    });
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
                .filter(c -> !c.isEmpty())
                .orElse(null);

        if (tagList == null) {
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

}
