package edu.ualberta.storyteller.core.dataloader;

import com.google.common.collect.Maps;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Define the corpus data type: a collection of documents.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class Corpus implements Serializable {

    /**
     * Documents contained in this corpus.
     */
    public HashMap<String, Document> docs = new HashMap<>();

    /**
     * Words' DF.
     */
    public HashMap<String, Double> DF = new HashMap<>();

    /**
     * Recalculate DF of this corpus.
     */
    public void updateDF() {
        DF.clear();
        for (Document d : docs.values()) {
            for (Keyword k : d.keywords.values()) {
                if (DF.containsKey(k.baseForm)) {
                    DF.put(k.baseForm, DF.get(k.baseForm) + 1);
                }
                else {
                    DF.put(k.baseForm, 1.0);
                }
            }
        }
    }

    /**
     * Merge two corpus.
     * <p>
     * @param corpus Merged corpus.
     */
    public void merge(Corpus corpus) {
        docs.putAll(Maps.difference(corpus.docs, docs).entriesOnlyOnLeft());
        updateDF();
    }

    /**
     * Iterate over second map and merge its elements into map 1 using
     * same key and sum of values
     * <p>
     * @param DF1 The DF to change.
     * @param DF2 The DF that will be merged into above DF1.
     * @return Merged DF1.
     */
    public static HashMap<String, Double> mergeDF(HashMap<String, Double> DF1, HashMap<String, Double> DF2) {
        DF2.forEach((k, v) -> DF1.merge(k, v, Double::sum));
        return DF1;
    }

    /**
     * Get start time of this corpus.
     * <p>
     * @return Start time.
     */
    public Timestamp startTime() {
        HashMap.Entry<String, Document> entry = docs.entrySet().iterator().next();
        Document value = entry.getValue();

        Timestamp t = value.publishTime;
        for (Document d: docs.values()) {
            if (d.publishTime.before(t)) {
                t = d.publishTime;
            }
        }
        return t;
    }

    /**
     * Get end time of this corpus.
     * <p>
     * @return End time.
     */
    public Timestamp endTime() {
        HashMap.Entry<String, Document> entry = docs.entrySet().iterator().next();
        Document value = entry.getValue();

        Timestamp t = value.publishTime;
        for (Document d: docs.values()) {
            if (d.publishTime.after(t)) {
                t = d.publishTime;
            }
        }
        return t;
    }

    /**
     * Filter docs without enough keywords.
     * <p>
     * @param threshold Threshold of keywords.
     */
    public void filterDocsByNumKeywords(int threshold) {
        ArrayList<String> toRemove = new ArrayList<>();
        for (Document d : docs.values()) {
            if (d.keywords.size() < threshold) {
                toRemove.add(d.id);
            }
        }
        for (String id : toRemove) {
            docs.remove(id);
        }
        updateDF();
    }

    /**
     * Filter docs outside the time section.
     * <p>
     * @param start Start time.
     * @param end End time.
     */
    public void filterDocsByTime(Timestamp start, Timestamp end) {
        ArrayList<String> toRemove = new ArrayList<>();
        for (Document d : docs.values()) {
            if (d.publishTime.before(start) || d.publishTime.after(end)) {
                toRemove.add(d.id);
            }
        }
        for (String id : toRemove) {
            docs.remove(id);
        }
        updateDF();
    }

}
