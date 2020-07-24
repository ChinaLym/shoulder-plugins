package org.shoulder.maven.plugins.mojo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author lym
 */
public class ClassUtil {

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

    /**
     * 从一个指定路径下查找所有的类
     */
    @SuppressWarnings("rawtypes")
    private static List<Class<?>> getAllClass(String packageName) {
        ArrayList<Class<?>> classes = new ArrayList<>();
        //先把包名转换为路径,首先得到项目的classpath
        String classpath = ClassUtil.class.getResource("/").getPath();
        //然后把我们的包名basPach转换为路径名
        packageName = packageName.replace(".", File.separator);
        //然后把classpath和basePack合并
        String searchPath = classpath + packageName;
        List<String> classPaths = new LinkedList<>();
        doPath(new File(searchPath), classPaths);
        //这个时候我们已经得到了指定包下所有的类的绝对路径了。我们现在利用这些绝对路径和java的反射机制得到他们的类对象
        for (String s : classPaths) {
            //把 D:\work\code\20170401\search-class\target\classes\com\baibin\search\a\A.class 这样的绝对路径转换为全类名com.baibin.search.a.A
            s = s.replace(classpath.replace("/","\\").replaceFirst("\\\\",""),"").replace("\\",".").replace(".class","");
            Class cls = null;
            try {
                cls = Class.forName(s);
            } catch (ClassNotFoundException e) {
                //log.error("class not found " + s, e);
            }
            classes.add(cls);
        }
        return classes;
    }

    private static void doPath(File file, List<String> allFile) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    doPath(f, allFile);
                }
            }
        } else {
            if (file.getName().endsWith(".class")) {
                //如果是class文件我们就放入我们的集合中。
                allFile.add(file.getPath());
            }
        }
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