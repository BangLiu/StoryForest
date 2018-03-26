package edu.ualberta.storyteller.core.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

/**
 * This class contains methods that manipulating files.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class FileUtils {

    /**
     * Create data input stream from file name.
     * <p>
     * @param fileName Input file name.
     * @return Input data stream of the file.
     */
    public static DataInputStream openDataInputStream(String fileName) throws Exception {
        return new DataInputStream(new FileInputStream(fileName));
    }

    /**
     * Given a directory path, return a list of absolute file paths in the directory.
     * <p>
     * @param directory Input directory absolute path.
     * @return A list of file absolute paths.
     */
    public static ArrayList<String> getFilesInDirectory(String directory) {
        ArrayList<String> results = new ArrayList<>();
        File[] files = new File(directory).listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    results.add(file.getAbsolutePath());
                }
            }
        }
        return results;
    }

    /**
     * Given a directory path, return a list of absolute file paths in the directory
     * that ends with specific string.
     * <p>
     * @param directory Input directory absolute path.
     * @param endWith A specific string that we need filename end with.
     * @return A list of file absolute paths.
     */
    public static ArrayList<String> getSpecificFilesInDirectory(String directory, String endWith) {
        ArrayList<String> textFiles = new ArrayList<>();
        File dir = new File(directory);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith((endWith))) {
                    textFiles.add(file.getAbsolutePath());
                }
            }
        }
        return textFiles;
    }

}
