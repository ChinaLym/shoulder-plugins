package org.shoulder.maven.plugins.util;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在maven插件中是加载不到目标项目的类及目标项目引用的第三方所提供的类的，
 * 需要通过动态读取目标项目所依赖的classpath并根据这些classpath生成相应的url数组，
 * 以这个url数组作为参数得到的类加载器可以实现在maven插件中动态加载目标项目类及第三方引用包的目的。
 * 官方文档：http://maven.apache.org/guides/mini/guide-maven-classloading.html
 * 参考插件：maven-compiler-plugin
 *
 * @author lym
 */
public class ClassUtil {

    private static final Log log = new SystemStreamLog();

    private static final ConcurrentHashMap<Class, ConcurrentHashMap<String, Method>> METHOD_CACHE = new ConcurrentHashMap<>(16);

    /**
     *  支持获取当前类和父类的public方法
     */
    public static Method findNoParamMethod(Class clazz, String methodName) {
        ConcurrentHashMap<String, Method> methods = METHOD_CACHE.computeIfAbsent(clazz, c -> new ConcurrentHashMap<>());
        return methods.computeIfAbsent(methodName, n -> {
            try {
                return clazz.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("no such method", e);
            }
        });
    }

    public static <T> List<Class<? extends T>> getAllSonOfClass(String sourcePath, String packageName, Class<T> clazz) {
        return filterSonOfClass(getAllClass(sourcePath, packageName), clazz);
    }

    public static List<Class<?>> filterSonOfClass(Collection<Class<?>> allClass, String fullClassName) {
        log.info("class total num: " + allClass.size());
        List<Class<?>> list = new LinkedList<>();
        try {
            Class<?> clazz = getClassLoader().loadClass(fullClassName);
            for (Class aClass : allClass) {
                boolean isSon = clazz.isAssignableFrom(aClass) && !clazz.equals(aClass);
                // 继承或实现类，去除自身
                if (isSon) {
                    log.info("MATCH: " + aClass.getName());
                    list.add(aClass);
                } else {
                    log.debug("Ignored class: " + aClass.getName());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("filterSonOfClass fail", e);
        }
        return list;
    }


    public static <T> List<Class<? extends T>> filterSonOfClass(Collection<Class<?>> allClass, Class<T> clazz) {
        List<Class<? extends T>> list = new LinkedList<>();
        log.info("class total num: " + allClass.size());
        try {
            for (Class aClass : allClass) {
                boolean isSon = clazz.isAssignableFrom(aClass) && !clazz.equals(aClass);
                // 继承或实现类，去除自身
                if (isSon) {
                    log.info("MATCH: " + aClass.getName());
                    list.add(aClass);
                } else {
                    log.debug("Ignored class: " + aClass.getName());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("filterSonOfClass fail", e);
        }
        return list;
    }

    @SuppressWarnings("rawtypes")
    public static List<Class<?>> getAllClass(String sourcePath, String packageName) {
        List<String> classFullNames = convertJavaSourceToClassFullName(packageName, getAllClassFilePath(sourcePath, packageName));
        log.info("total source file num: " + classFullNames.size());
        ArrayList<Class<?>> classes = new ArrayList<>();
        // 利用这些绝对路径和反射机制得类对象
        for (String classFullName : classFullNames) {
            try {
                classes.add(getClassLoader().loadClass(classFullName));
                log.debug("loaded class: " + classFullName);
            } catch (ClassNotFoundException e) {
                log.warn("class not found " + classFullName, e);
            }
        }
        return classes;
    }

    /**
     * 列出指定包名下所有的类的文件路径
     */
    private static List<String> getAllClassFilePath(String sourcePath, String packageName) {
        //先把包名转换为路径,首先得到项目的classpath
        log.debug("sourcePath: " + sourcePath);

        //然后把我们的包名basPath转换为路径名
        packageName = packageName.replace(".", File.separator);

        //然后把classpath和basePack合并
        String searchPath = sourcePath + File.separator + packageName;
        log.debug("searchPath: " + searchPath);

        return listFilesAndSelect(new File(searchPath), new JavaSourceFileSelector());
    }


    /**
     * 将文件路径转化为 类的全限定名
     * 把 D:\projects\shoulder-core\src\main\java\org\shoulder\core\A.class 这样的绝对路径转换为全类名org.shoulder.core.A
     */
    private static List<String> convertJavaSourceToClassFullName(String packageName, List<String> classFilePath) {
        List<String> result = new ArrayList<>(classFilePath.size());
        for (String filePath : classFilePath) {
            String classPath = filePath.replaceAll("/+|\\\\+", ".");
            classPath = classPath.substring(classPath.indexOf(packageName))
                    .replace(".java", "");
            result.add(classPath);
            log.debug("converted filePath(" + filePath + ") to classPath(" + classPath + ")");
        }
        return result;
    }

    public static List<String> listFilesAndSelect(File file, FileSelector fileSelector) {
        List<String> allFile = new LinkedList<>();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    allFile.addAll(listFilesAndSelect(f, fileSelector));
                }
            }
        } else {
            if (fileSelector.include(file)) {
                //如果需要就加入
                allFile.add(file.getPath());
            }
        }
        return allFile;
    }

    /**
     * 默认使用自己的类加载器
     */
    private static ThreadLocal<ClassLoader> classLoader = ThreadLocal.withInitial(ClassUtil.class::getClassLoader);

    public static void setClassLoader(ClassLoader clazzLoader) {
        classLoader.set(clazzLoader);
    }

    public static void clean(){
        classLoader.remove();
    }

    public static ClassLoader getClassLoader(){
        return classLoader.get();
    }

    @FunctionalInterface
    public interface FileSelector {
        /**
         * 是否包含该文件
         *
         * @param file
         * @return
         */
        boolean include(File file);

        class All implements FileSelector {
            @Override
            public boolean include(File file) {
                return true;
            }
        }
    }

    static abstract class AbstractFileSelector implements FileSelector {

        protected FileSelector delegate;

        AbstractFileSelector(FileSelector delegate) {
            this.delegate = delegate;
        }

        AbstractFileSelector() {
        }

        @Override
        public boolean include(File file) {
            return delegate == null ? isWanted(file) :
                    delegate.include(file) && isWanted(file);
        }

        protected abstract boolean isWanted(File file);


    }

    static class ClassFileSelector extends AbstractFileSelector {
        public ClassFileSelector(FileSelector delegate) {
            super(delegate);
        }

        ClassFileSelector() {
        }

        @Override
        public boolean isWanted(File file) {
            return file.getName().endsWith(".class");
        }
    }

    static class JavaSourceFileSelector extends AbstractFileSelector {
        public JavaSourceFileSelector(FileSelector delegate) {
            super(delegate);
        }

        JavaSourceFileSelector() {
        }

        @Override
        public boolean isWanted(File file) {
            return file.getName().endsWith(".java") && !file.getName().contains("package-info");
        }
    }

    static class SpecialTypeFilter extends ClassFileSelector {

        private Class<?> type;

        public SpecialTypeFilter(FileSelector delegate, Class<?> type) {
            super(delegate);
            this.type = type;
        }

        @Override
        public boolean isWanted(File file) {
            return super.isWanted(file) && type.getTypeName().equals(file.getName().substring(0,
                    file.getName().length() - ".class".length()));
        }
    }


    public static void main(String[] args) {
        String s = "F:.codes.java.self.shoulder-framework.shoulder-build.shoulder-base.shoulder-operation-log.src.main.java.org.shoulder.log.operation.annotation.OperationLog.java";
        System.out.println(s.substring(s.indexOf("org.shoulder.log")));
    }
}