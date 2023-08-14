package org.shoulder.maven.plugins.test;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author lym
 */
public class JarUtil {

    public static void main(String[] args) throws IOException {
        //unzipJar("./src/a.jar", "./src/dest");
    }

    public static void unzipJar(String jarPath, String destinationDir) throws IOException {

        File file = new File(jarPath);
        JarFile jar = new JarFile(file);

// fist get all directories, then make those directory on the destination Path
        for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements(); ) {
            JarEntry entry = (JarEntry) enums.nextElement();
            String fileName = destinationDir + File.separator + entry.getName();
            File f = new File(fileName);
            if (fileName.endsWith("/")) {
                f.mkdirs();
            }
        }

//now create all files
        for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements(); ) {
            JarEntry entry = (JarEntry) enums.nextElement();
            String fileName = destinationDir + File.separator + entry.getName();
            File f = new File(fileName);
            if (!fileName.endsWith("/")) {
                InputStream is = jar.getInputStream(entry);
                FileOutputStream fos = new FileOutputStream(f);
// write contents of 'is' to 'fos'
                while (is.available() > 0) {
                    fos.write(is.read());
                }
                fos.close();
                is.close();

            }
        }
    }

    public static void dirCopy(String srcPath, String destPath) {
        File src = new File(srcPath);
        if (!new File(destPath).exists()) {
            new File(destPath).mkdirs();
        }
        for (File s : src.listFiles()) {
            if (s.isFile()) {
                fileCopy(s.getPath(), destPath + File.separator + s.getName());
            } else {
                dirCopy(s.getPath(), destPath + File.separator + s.getName());
            }
        }
    }

    public static void fileCopy(String srcPath, String destPath) {
        File src = new File(srcPath);
        File dest = new File(destPath);
        //使用jdk1.7 try-with-resource 释放资源，并添加了缓存流
        try(InputStream is = new BufferedInputStream(new FileInputStream(src));
            OutputStream out =new BufferedOutputStream(new FileOutputStream(dest))) {
            byte[] flush = new byte[1024];
            int len = -1;
            while ((len = is.read(flush)) != -1) {
                out.write(flush, 0, len);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
