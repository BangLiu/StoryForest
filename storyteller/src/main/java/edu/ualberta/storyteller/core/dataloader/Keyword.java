package edu.ualberta.storyteller.core.dataloader;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Define the keyword data type.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class Keyword implements Serializable {

    /**
     * Base form of the word.
     */
    public String baseForm;

    /**
     * The word itself.
     */
    public String word;

    /**
     * Term frequency of the word.
     */
    public double tf;

    /**
     * Document frequency of the word.
     */
    public double df;

    /**
     * The list of documents that contains this word.
     */
    public HashMap<String, Document> documents = new HashMap<>();

    /**
     * Parametric constructor.
     * <p>
     * @param baseForm Base form of this word.
     * @param word Value of this word.
     * @param tf This word's tf.
     * @param df This word's df.
     */
    public Keyword(String baseForm, String word, double tf, double df) {
        this.baseForm = baseForm;
        this.word = word;
        this.tf = tf;
        this.df = df;
    }

}
