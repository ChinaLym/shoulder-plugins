package org.shoulder.maven.plugins.mojo;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.annotation.concurrent.ThreadSafe;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 统计代码行数（JUST FOR TEST）
 *
 * @author lym
 * @goal 统计代码行数
 */
@ThreadSafe
@Mojo(name = "countLine")
public class CodeLineCounter extends AbstractMojo {

    private static final String[] INCLUDES_DEFAULT = {"properties", "xml", "java", "yml"};

    @Parameter(defaultValue = "${basedir}")
    private File baseDir;

    @Parameter(defaultValue = "${project.build.resources}", readonly = true, required = true)
    private List<Resource> resources;

    @Parameter(defaultValue = "${project.build.sourceDirectory}", required = true, readonly = true)
    private File sourceDir;

    @Parameter(defaultValue = "${project.build.testResources}", readonly = true, required = true)
    private List<Resource> testResources;

    @Parameter(defaultValue = "${project.build.testSourceDirectory}", readonly = true, required = true)
    private File testSourceDir;

    @Parameter(property = "count.include")
    private String[] includes;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("baseDir目录" + baseDir);
        if (includes == null || includes.length == 0) {
            includes = INCLUDES_DEFAULT;
        }

        try {

            getLog().info("sourceDir: " + sourceDir.getAbsolutePath());
            countDir(sourceDir);

            getLog().info("testSourceDir: " + testSourceDir.getAbsolutePath());
            countDir(testSourceDir);

            for (Resource resource : resources) {
                countDir(new File(resource.getDirectory()));
            }

            for (Resource testResource : testResources) {
                countDir(new File(testResource.getDirectory()));
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    private void showInclude() {
        getLog().info("include包括" + Arrays.asList(includes));
    }

    public void countDir(File file) throws IOException {

        for (String fileType : includes) {
            int fileNum = countFileNum(file, fileType);
            int codeLineNum = countLineNum(file, fileType);
            if(fileNum == 0 || codeLineNum == 0) {
                continue;
            }
            getLog().info(file.getAbsolutePath().substring(baseDir.getName().length() + "de\\javaCode\\".length()) +
                    " 共 " + fileNum + " 个 " + fileType + " 文件. " + codeLineNum + " 行代码：");
        }
    }

    /**
     * 统计文件多少个
     */
    public int countFileNum(File file, String fileType) {
        int num = 0;
        if (file.isFile() && file.getName().endsWith("." + fileType)) {
            return 1;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if(files != null){
                for (File f : files) {
                    num += countFileNum(f, fileType);
                }
            }
        }
        return num;
    }

    /**
     * 统计文件多少行
     */
    public int countLineNum(File file, String fileType) throws IOException {
        int lineNum = 0;
        if (file.isFile() && file.getName().endsWith("." + fileType)) {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = br.readLine()) != null) {
                if(!isBlank(line)){
                    lineNum++;
                }
            }
            return lineNum;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if(files != null){
                for (File f : files) {
                    lineNum += countLineNum(f, fileType);
                }
            }
        }
        return lineNum;
    }

    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}
