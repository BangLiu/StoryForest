package edu.ualberta.storyteller.core.util;

import java.io.FileReader;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;

/**
 * This class contains methods that do some NLP tasks.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class NlpUtils {

    /**
     * Calculate word's inverse document frequency.
     * <p>
     * @param df Word's document frequency.
     * @param size Total number of documents.
     * @return Word's idf.
     */
    public static double idf(double df, int size) {
        return Math.log(size / (df+1)) / Math.log(2);
    }

    /**
     * Calculate word's tf-idf.
     * <p>
     * @param tf Word's term frequency.
     * @param idf Word's inverse document frequency.
     * @return Word's tf-idf value.
     */
    public static double tfidf(double tf, double idf) {
        if (tf == 0 || idf == 0) {
            return 0;
        }
        return tf * idf;
    }

	/**
	 * Import English stop words from file.
     * <p>
	 * @param fileName File name of English stop words.
	 * @return stopwords A hash set of stop words.
     */
	private static HashSet<String> importEnglishStopwords(String fileName) {
		HashSet<String> stopwords = new HashSet<>();
		try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = in.readLine()) != null) {
                stopwords.add(line.trim().toLowerCase());
            }
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stopwords;
	}

    /**
     * Import Chinese stop words from file.
     * <p>
     * @param fileName File name of Chinese stop words.
     * @return stopwords A hash set of stop words.
     */
	private static HashSet<String> importChineseStopwords(String fileName) {
		HashSet<String> stopwords = new HashSet<>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName),"UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
                stopwords.add(line.trim());
            }
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stopwords;
	}

	/**
	 * Import stop words from file.
     * <p>
	 * @param filename File that stores stopwords.
	 * @param language Language.
     * @return A set of stopwords.
     */
	public static HashSet<String> importStopwords(String filename, String language) {
		HashSet<String> stopwords = new HashSet<>();
		switch (language) {
            case "Chinese":
                stopwords = importChineseStopwords(filename);
                break;
            case "English":
                stopwords = importEnglishStopwords(filename);
                break;
            default:
                System.out.println("Not supported language!");
                System.exit(1);
        }
		return stopwords;
	}

}
