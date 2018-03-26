package edu.ualberta.storyteller.core.dataloader;

import edu.ualberta.storyteller.core.util.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Define the document data type.
 *
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class Document implements Serializable {
    /**
     * Document id.
     */
    public String id;

    /**
     * Document urls.
     */
    public ArrayList<String> urls = new ArrayList<>();

    /**
     * Document transformed urls.
     */
    public ArrayList<String> transformedUrls = new ArrayList<>();

    /**
     * Document source.
     */
    public String from;

    /**
     * Document unsegmented title.
     */
    public String title;

    /**
     * Document segmented title.
     */
    public String segTitle;

    /**
     * Document segmented content.
     */
    public String segContent;

    /**
     * Document topic category.
     */
    public String topic;

    /**
     * Document publish date.
     */
    public Timestamp publishTime = null;

    /**
     * Document language.
     */
    public String language;

    /**
     * The map of (keyword id, keyword) for content keywords.
     */
    public HashMap<String, Keyword> keywords = new HashMap<>();

    /**
     * The set of main keywords.
     */
    public HashSet<String> mainKeywords = new HashSet<>();

    /**
     * The set of title keywords.
     */
    public HashSet<String> titleKeywords = new HashSet<>();

    /**
     * The set of title NER keywords.
     */
    public HashMap<String, HashSet<String>> titleNer = new HashMap<>();

    /**
     * Document TF-IDF vector size that consider keygraph keywords.
     * For keywords that doesn't included in a keygraph, we don't calculate the keyword's tf.
     * This is used to calculate the similarity between a keygraph and a document.
     * It is the norm of document vector, where each element in the vector
     * is the feature (such as tf, tf-idf) of one document keyword.
     */
    public double tfidfVectorSizeWithKeygraph = -1;

    /**
     * Document TF-IDF vector size.
     */
    public double tfidfVectorSize = -1;

    /**
     * Document TF vector's size.
     */
    public double tfVectorSize = -1;

    /**
     * Whether this document has been processed.
     */
    public boolean processed = false;

    /**
     * Document LDA feature.
     */
    public String lda;

    /**
     * Parametric constructor.
     * <p>
     * @param id Document id.
     */
    public Document(String id) {
        this.id = id;
    }

    /**
     * Check whether the content and title contains a keyword.
     * <p>
     * @param keyword The keyword we need to check.
     * @return Boolean.
     */
    public boolean containsKeyword(String keyword) {
        boolean result = false;
        for (Keyword kw: keywords.values()) {
            if (kw.baseForm.equals(keyword) || kw.word.equals(keyword)) {
                result = true;
            }
        }

        String[] titleWords = segTitle.split(" ");
        for (String w: titleWords) {
            if (w.equals(keyword)) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Compute document's TF vector size.
     * <p>
     * Given document's keywords [w1, w2, ..., wn],
     * the vector size of a document is sqrt(sum(tf(wi)^2)).
     * NOTICE: it only considered keywords here.
     */
    public double calcTFVectorSize() {
        tfVectorSize = 0;
        for (Keyword k : keywords.values()) {
            tfVectorSize += Math.pow(k.tf, 2);
        }
        tfVectorSize = Math.sqrt(tfVectorSize);

        return tfVectorSize;
    }

    /**
     * Compute document's vector size.
     * <p>
     * Given document's keywords [w1, w2, ..., wn], it is represented by keywords' tf-idf values.
     * Thus, if our dictionary contains N words, a document will be represented by a sparse vector, where
     * each element value in the vector is the tf-idf value of the corresponding keyword.
     * The vector size of a document is sqrt(sum(tf-idf(wi)^2)).
     * <p>
     * @param DF The map between keywords and their df.
     * @param docSize Number of total docs.
     */
    public void calcTFIDFVectorSize(HashMap<String, Double> DF, int docSize) {
        tfidfVectorSize = 0;
        for (Keyword k : keywords.values()) {
            tfidfVectorSize += Math.pow(NlpUtils.tfidf(k.tf, NlpUtils.idf(DF.get(k.baseForm), docSize)), 2);
        }
        tfidfVectorSize = Math.sqrt(tfidfVectorSize);
    }

}
