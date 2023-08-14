package org.shoulder.maven.plugins.test;

import com.sun.javadoc.*;
import org.apache.commons.lang3.StringUtils;
import org.shoulder.maven.plugins.pojo.ErrorCodeJavaDoc;
import org.shoulder.maven.plugins.util.ClassUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * https://docs.oracle.com/javase/6/docs/technotes/guides/javadoc/doclet/overview.html#simple
 *
 * @author lym
 */
@SuppressWarnings("all")
public class ExtractErrorInfoDoclet extends Doclet {

    private static final String ERRCODE_TEMPLATE = "err.0x%08x.";
    private static final String DESCRIPTION_TEMPLATE = ERRCODE_TEMPLATE + "desc";
    private static final String SUGGESTION_TEMPLATE = ERRCODE_TEMPLATE + "sug";


    private static final String ERROR_CODE_CLASS_MARK = "ErrorCode";


    public static boolean start(RootDoc rootDoc) {
        ClassDoc[] classDocs = rootDoc.classes();
        Properties properties = new Properties();

        try {

            Arrays.stream(classDocs).forEach(classDoc -> {
                // qualifiedName 全类名；fields() 字段数; isEnum() 判断枚举；isEnumConstant 枚举常类；simpleTypeName/typeName 类名
                // isInterface 是否接口；isClass 类；isAbstract 抽象；isOrdinaryClass 一般类型
                // 只关注后缀为 ErrorCode 的类
                if (!classDoc.simpleTypeName().contains(ERROR_CODE_CLASS_MARK)) {
                    return;
                }
                FieldDoc[] fieldDocs = classDoc.fields();
                Map<String, String> enumFieldNameToErrorCodeMap = Collections.emptyMap();
                boolean isEnumErrorCode = false;
                if (Optional.ofNullable(classDoc.superclassType()).map(supperType -> "java.lang.Enum".equals(supperType.qualifiedTypeName())).orElse(false)) {
                    isEnumErrorCode = true;
                    enumFieldNameToErrorCodeMap = extractFromEnum(classDoc.qualifiedName());
                    if (enumFieldNameToErrorCodeMap.isEmpty()) {
                        // 枚举没有值
                        return;
                    }
                }
                // 循环每个字段
                for (FieldDoc fieldDoc : fieldDocs) {
                    // 根据文档找到 @language @desc @sug
                    ErrorCodeJavaDoc result = analyzeFieldDoc(fieldDoc);
                    if (StringUtils.isAnyBlank(result.description, result.suggestion)) {
                        // 若包含未填写的，给出提示
                        System.err.println(String.format("[%s].%s - @desc or @sug is blank.", classDoc.qualifiedName(), fieldDoc.name()));
                        //System.exit(0);
                    }
                    String errorCode;
                    if (isEnumErrorCode) {
                        // 枚举根据枚举字段名 map 找
                        errorCode = enumFieldNameToErrorCodeMap.get(fieldDoc.name());
                    } else {
                        // 常量类，字段值就是错误码
                        errorCode = (String) fieldDoc.constantValue();
                    }
                    BigInteger number = new BigInteger(errorCode, 16);
                    properties.put(String.format(DESCRIPTION_TEMPLATE, number), result.description);
                    properties.put(String.format(SUGGESTION_TEMPLATE, number), result.suggestion);

                    // debug
                    //System.out.println("errorCode : " + errorCodeStr + result + ",name:" + fieldDoc.name() + ",qualifiedName:" + fieldDoc.qualifiedName());
                }

            });


            try (FileWriter fw = new FileWriter(new File(translateDir + File.separator + "translate.properties"))) {
                properties.store(fw, "generate by shoulder.shoulder-maven-plugin:generateErrorCodeInfo");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Doclet.start(rootDoc);
    }


    /**
     * 分析字段文档，从javadoc注释中提取标签
     */
    @Nonnull
    private static ErrorCodeJavaDoc analyzeFieldDoc(FieldDoc fieldDoc) {
        ErrorCodeJavaDoc result = new ErrorCodeJavaDoc();
        Tag[] languages = fieldDoc.tags("language");
        if (languages != null && languages.length > 0) {
            result.setLanguage(languages[0].text().trim());
        }
        Tag[] descriptions = fieldDoc.tags("desc");
        if (descriptions != null && descriptions.length > 0) {
            result.setDescription(descriptions[0].text().trim());
        }
        Tag[] suggestions = fieldDoc.tags("sug");
        if (suggestions != null && suggestions.length > 0) {
            result.setSuggestion(suggestions[0].text().trim());
        }
        return result;
    }

    /**
     * 生成 枚举字段名 -> 错误码 的映射
     *
     * @param qualifiedClassName 类名
     * @return 枚举字段名 -> 错误码 的映射
     */
    private static Map<String, String> extractFromEnum(String qualifiedClassName) {
        Map<String, String> map = new HashMap<>();
        // 类路径，默认为 outPath/classes
        File classDir = new File(outPath + File.separator + "classes");
        URL url = null;
        try {
            url = classDir.toURI().toURL();
            // System.out.println("===============classDir:" + classDir);
            URL[] urls = new URL[]{url};
            final ClassLoader cl = new URLClassLoader(urls);
            Class enumClass = cl.loadClass(qualifiedClassName);
            if (!enumClass.isEnum()) {
                return Collections.emptyMap();
            }

            Field[] flds = enumClass.getDeclaredFields();

            // enum constants
            List<Field> cst = new ArrayList<>();
            // member fields
            List<Field> mbr = new ArrayList<>();
            for (Field f : flds) {
                if (f.isEnumConstant()) {
                    cst.add(f);
                } else {
                    mbr.add(f);
                }
            }
            if (cst.isEmpty() || mbr.isEmpty()) {
                return Collections.emptyMap();
            }

            // System.out.println("Enum class : " + qualifiedClassName + ",fields size:" + cst.size());

            Method mth = enumClass.getDeclaredMethod("getCode");
            Object[] enumObjects = enumClass.getEnumConstants();
            for (Object enumObject : enumObjects) {
                String code = (String) mth.invoke(enumObject);
                map.put(enumObject.toString(), code);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    private static final String OUT_PATH_PARAM = "-outPath";

    private static String projectPath = "F:\\codes\\java\\self\\shoulder-framework\\shoulder-build\\shoulder-base\\shoulder-core";

    /**
     * 输出路径  xxx/target
     */
    private static String outPath;

    /**
     * outPath/language/zh_CN
     */
    private static String translateDir;

    /**
     * 初始化时，将调用该方法，读取入参
     */
    public static boolean validOptions(String[][] options, DocErrorReporter errorReporter) {
        /*Arrays.stream(options).forEach(option -> {
            if (OUT_PATH_PARAM.equals(option[0])) {

            }
        });*/
        outPath = projectPath + File.separator + "target";
        translateDir = outPath + File.separator + "language" + File.separator + "zh_CN";
        try {
            new File(translateDir).mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * debug : {@link JavadocTool#getRootDocImpl(java.lang.String, java.lang.String, com.sun.tools.javadoc.main.ModifierFilter, com.sun.tools.javac.util.List, com.sun.tools.javac.util.List, java.lang.Iterable, boolean, com.sun.tools.javac.util.List, com.sun.tools.javac.util.List, boolean, boolean, boolean)}
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        String sourcePath = "F:\\codes\\java\\self\\shoulder-framework\\shoulder-build\\shoulder-base\\shoulder-core\\src\\main\\java\\org\\shoulder\\core";
        List<String> filePaths = ClassUtil.listFilesAndSelect(new File(sourcePath), f -> f.getName().contains("Error"));
        String[] commandArg = {
                "-doclet", ExtractErrorInfoDoclet.class.getName(),
                "-quiet", // 不产生输出
                "-encoding", "utf-8"
        };
        for (int i = 0; i < commandArg.length; i++) {
            filePaths.add(i, commandArg[i]);
        }
        String[] docArgs = filePaths.toArray(new String[0]);
        com.sun.tools.javadoc.Main.execute(docArgs);
    }

}