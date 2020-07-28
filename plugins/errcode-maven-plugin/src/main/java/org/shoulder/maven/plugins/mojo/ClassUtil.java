package org.shoulder.maven.plugins.mojo;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author lym
 */
public class ClassUtil {

    private static final Log log = new SystemStreamLog();

    public static <T> List<Class<? extends T>> getAllSonOfClass(String packageName, Class<T> clazz) {
        return filterSonOfClass(getAllClass(packageName), clazz);
    }

    @SuppressWarnings({"unchecked"})
    private static <T> List<Class<? extends T>> filterSonOfClass(Collection<Class<?>> allClass, Class<T> clazz) {
        List<Class<? extends T>> list = new LinkedList<>();
        try {
            for (Class aClass : allClass) {
                if (clazz.isAssignableFrom(aClass)) {
                    // 自身并不加进去
                    if (!clazz.equals(aClass)) {
                        list.add(aClass);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("error when scan package");
        }
        return list;
    }

    @SuppressWarnings("rawtypes")
    private static List<Class<?>> getAllClass(String packageName) {
        List<String> classFullNames = convertFileToClassFullName(getAllClassFilePath(packageName));

        ArrayList<Class<?>> classes = new ArrayList<>();
        // 利用这些绝对路径和反射机制得类对象
        for (String classFullName : classFullNames) {
            try {
                log.debug("try loading " + classFullName + "");
                classes.add(Class.forName(classFullName));
                log.debug("load (" + classFullName + ") SUCCESS!");
            } catch (ClassNotFoundException e) {
                log.error("class not found " + classFullName, e);
            }
        }
        return classes;
    }

    /**
     * 列出指定包名下所有的类的文件路径
     */
    private static List<String> getAllClassFilePath(String packageName) {
        //先把包名转换为路径,首先得到项目的classpath
        String classpath = ClassUtil.class.getResource("/").getPath();
        log.debug("classpath: " + classpath);

        //然后把我们的包名basPath转换为路径名
        packageName = packageName.replace(".", File.separator);

        //然后把classpath和basePack合并
        String searchPath = classpath + packageName;
        log.debug("searchPath: " + searchPath);

        return listAllClassFiles(new File(searchPath));
    }


    /**
     * 将文件路径转化为 类的全限定名
     * 把 D:\work\code\20170401\search-class\target\classes\org\shoulder\core\A.class 这样的绝对路径转换为全类名org.shoulder.core.A
     */
    private static List<String> convertFileToClassFullName(List<String> classFilePath) {
        String classpath = ClassUtil.class.getResource("/").getPath();
        String filePathPrefix = classpath.replace("/", "\\").replaceFirst("\\\\", "");
        List<String> result = new ArrayList<>(classFilePath.size());
        for (String filePath : classFilePath) {
            String classPath = filePath.replace(filePathPrefix, "")
                    .replace("\\", ".")
                    .replace(".class", "");
            result.add(classPath);
            log.debug("converted filePath(" + filePath + ") to classPath(" + classPath + ")");
        }
        return result;
    }

    private static List<String> listAllClassFiles(File file) {
        List<String> allFile = new LinkedList<>();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    allFile.addAll(listAllClassFiles(f));
                }
            }
        } else {
            if (file.getName().endsWith(".class")) {
                //如果是class文件我们就放入我们的集合中。
                allFile.add(file.getPath());
            }
        }
        return allFile;
    }



    @FunctionalInterface
    interface FileScanFilter {
        boolean include(File file);
    }

    abstract class AbstractFileScanFilter implements FileScanFilter {

        protected FileScanFilter delegate;

        AbstractFileScanFilter(FileScanFilter delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean include(File file) {
            return delegate == null ? isWanted(file) :
                    delegate.include(file) && isWanted(file);
        }

        protected abstract boolean isWanted(File file);


    }

    class ClassFileFilter extends AbstractFileScanFilter {

        public ClassFileFilter(FileScanFilter delegate) {
            super(delegate);
        }

        @Override
        public boolean isWanted(File file) {
            return file.getName().endsWith(".class");
        }
    }

    class SpecialTypeFilter extends ClassFileFilter {

        private Class<?> type;

        public SpecialTypeFilter(FileScanFilter delegate, Class<?> type) {
            super(delegate);
            this.type = type;
        }

        @Override
        public boolean isWanted(File file) {
            return super.isWanted(file) && type.getTypeName().equals(file.getName().substring(0,
                    file.getName().length() - ".class".length()));
        }
    }

}