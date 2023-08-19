package org.shoulder.maven.plugins.test;

import java.io.*;
import java.util.Enumeration;
import java.util.Optional;
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
            JarEntry entry = enums.nextElement();
            String fileName = destinationDir + File.separator + entry.getName();
            File f = new File(fileName);
            if (fileName.endsWith("/")) {
                f.mkdirs();
            }
        }

        //now create all files
        for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements(); ) {
            JarEntry entry = enums.nextElement();
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
        File destFile = new File(destPath);
        if (!destFile.exists()) {
            destFile.mkdirs();
        }
        Optional.ofNullable(srcPath)
                .map(File::new)
                .map(File::listFiles)
                .ifPresent(files -> {
                    for (File file : files) {
                        if (file.isFile()) {
                            fileCopy(file.getPath(), destPath + File.separator + file.getName());
                        } else {
                            dirCopy(file.getPath(), destPath + File.separator + file.getName());
                        }
                    }
                });
    }

    public static void fileCopy(String srcPath, String destPath) {
        File src = new File(srcPath);
        File dest = new File(destPath);
        //使用jdk1.7 try-with-resource 释放资源，并添加了缓存流
        try (InputStream is = new BufferedInputStream(new FileInputStream(src));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {
            byte[] flush = new byte[1024];
            int len = -1;
            while ((len = is.read(flush)) != -1) {
                out.write(flush, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
