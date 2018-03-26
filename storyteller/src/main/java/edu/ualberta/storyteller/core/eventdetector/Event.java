package edu.ualberta.storyteller.core.eventdetector;

import edu.ualberta.storyteller.core.dataloader.*;
import edu.ualberta.storyteller.core.keywordorganizer.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

public class Event implements Serializable {

	/**
	 * Current maximum cluster id.
	 */
	public static int max_id = 1;

    /**
     * Id of this cluster.
     */
	public int id = max_id++;

    /**
     * Keyword graph of this document cluster.
     */
	public HashMap<String, KeywordNode> keyGraph = new HashMap<>();

    /**
     * Documents contained in this cluster.
     */
	public HashMap<String, Document> docs = new HashMap<>();

    /**
     * Documents' corresponding similarities to the keyword graph.
     */
	public HashMap<String, Double> similarities = new HashMap<>();

    /**
     * Centroid document of this cluster. (Calculated centroid, not a real document.)
     */
	public Document centroid;

    /**
     * Variance of document vectors.
     */
	public double variance;

    /**
     * Topic hotness of this document cluster.
     */
	public double hotness;

    /**
     * Summary.
     */
	public String summary = "";

	//! Start time.
	//public Timestamp startTimestamp;

	//! End time.
	//public Timestamp endTimestamp;

	/**
	 * Get start timestamp of all docs in this event.
	 * @return Start timestamp. -1 denotes no doc in this event.
	 */
	public long getStartTimestamp() {
		if (docs.size() == 0) {
            return -1;
        }

		long timestamp = System.currentTimeMillis();
		for (Document d: docs.values()) {
			if (d.publishTime.getTime() < timestamp) {
				timestamp = d.publishTime.getTime();
			}
		}

		//startTimestamp = new Timestamp(timestamp);
		return timestamp;
	}

	/**
	 * Get end timestamp of all docs in this event.
	 * @return End timestamp. -1 denotes no doc in this event.
	 */
	public long getEndTimestamp() {
		if (docs.size() == 0) {
            return -1;
        }

		long timestamp = -1;
		for (Document d: docs.values()) {
			if (d.publishTime.getTime() > timestamp) {
				timestamp = d.publishTime.getTime();
			}
		}

		//endTimestamp = new Timestamp(timestamp);
		return timestamp;
	}

	/**
	 * Calculate the centroid document of this document cluster.
	 * TODO: centroid is the concatenation of all docs in this event.
	 */
	public void calcCentroid() {
		centroid = new Document("-1");
        long timestamp = Long.MAX_VALUE;
		for (Document d : docs.values()) {
            if (d.publishTime.getTime() < timestamp) {
				timestamp = d.publishTime.getTime();
			}
			for (Keyword k : d.keywords.values()) {
				if (centroid.keywords.containsKey(k.baseForm)) {
					Keyword kk = centroid.keywords.get(k.baseForm);
					kk.tf += k.tf;
					kk.df = k.df;
				} else {
					centroid.keywords.put(k.baseForm, new Keyword(k.baseForm, k.word, k.tf, k.df));
				}
			}
		}
        centroid.calcTFVectorSize();
        centroid.publishTime = new Timestamp(timestamp);
	}

    public void refineKeyGraph() {
        ArrayList<String> toRemove = new ArrayList<String>();
        for (String key: keyGraph.keySet()) {
            KeywordNode keywordNode = keyGraph.get(key);
            String keyword = keywordNode.keyword.baseForm;

            boolean exist = false;
            for (Document d: docs.values()) {
                if (d.containsKeyword(keyword)) {
                    exist = true;
                    break;
                }
            }

            if (!exist) {
                toRemove.add(keyword);
            }
        }

        for (String kw: toRemove) {
            KeywordGraph.removeNode(keyGraph, kw);
        }
    }

}
