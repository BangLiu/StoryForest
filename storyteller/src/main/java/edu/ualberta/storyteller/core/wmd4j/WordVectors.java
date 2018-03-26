package edu.ualberta.storyteller.core.wmd4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Define word vectors container.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.0729
 */
public class WordVectors {

    public HashMap<String, ArrayList<Double>> lookupTable;

    public HashSet<String> vocab;

    /**
     * Returns true if the model has this word in the vocab
     * <p>
     * @param word the word to test for
     * @return true if the model has the word in the vocab
     */
    public boolean hasWord(String word) {
        return vocab.contains(word);
    }

    public ArrayList<Double> getWordVectorMatrix(String word) {
        return lookupTable.get(word);
    }

    public void normalizeWordVectors() {
        HashMap<String, ArrayList<Double>> normalizedTable = new HashMap<>();
        for (String key: lookupTable.keySet()) {
            ArrayList<Double> v = lookupTable.get(key);
            double norm = 0;
            for (Double value: v) {
                norm += value * value;
            }
            if (norm > 0) {
                for (int i = 0; i < v.size(); i++) {
                    v.set(i, v.get(i) / norm);
                }
            }
            normalizedTable.put(key, v);
        }
        lookupTable = normalizedTable;
    }

}
