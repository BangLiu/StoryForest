package edu.ualberta.storyteller.core.eventdetector;

import edu.ualberta.storyteller.core.util.*;
import edu.ualberta.storyteller.core.parameter.*;
import edu.ualberta.storyteller.core.dataloader.*;
import edu.ualberta.storyteller.core.keywordorganizer.*;
import java.io.PrintStream;
import java.util.*;

public class EventDetector {

    /**
     * Configuration.
     */
	public Parameters parameters;

    /**
     * Community detector.
     */
    public CommunityDetector cd;

    /**
     * Parametric constructor.
     * <p>
     * @param cons Configuration.
     */
	public EventDetector(Parameters cons) {
		parameters = cons;
		cd = new CommunityDetector(cons);
	}

    /**
     * Detect events from input file.
     * @param fileName Input file name.
     * @param outputFileName Output file name. If set to be "", then we don't output to a file.
     * @return A list of document clusters.
     * @throws Exception
     */
    public ArrayList<Event> detectEvents(String fileName,
                                         String outputFileName) throws Exception {
        // load documents into corpus
        System.out.println("Detecting events from file " + fileName);
        DataLoader loader = new DataLoader(parameters);
        Corpus corpus = loader.loadCorpus(fileName);

        // detect events from corpus
        ArrayList<Event> events = extractEventsFromCorpus(corpus);
        if (outputFileName != "") {
            PrintStream out = new PrintStream(outputFileName);
            EventDetector.printTopics(events, out);
            out.close();
        }

        return events;
    }

    /**
     * Extract events from corpus.
     * @param corpus A corpus.
     * @return An array list of document clusters.
     */
	public ArrayList<Event> extractEventsFromCorpus(Corpus corpus)  throws Exception {
        // build keyword graph from corpus
		KeywordGraph g = new KeywordGraph(parameters);
		g.buildGraph(corpus);

        // extract keyword communities from keyword graph
		calcDocsTFIDFVectorSizeWithGraph(corpus.docs, corpus.DF, g.graphNodes);  // NOTICE: consider change it into document class itself?
		ArrayList<HashMap<String, KeywordNode>> communities = cd.detectCommunities(g.graphNodes);
		System.out.println(communities.size());

        // extract events from corpus based on keyword communities
		ArrayList<Event> events = extractTopicsByKeywordCommunities(corpus, communities);

        // further split events by rule or by supervised learning
        events = splitEvents(events, corpus.DF, corpus.docs.size());

        // remove small size events
        // TODO: notice, here it highly influence the final cluster number with parameter "minTopicSize".
        for (int i = 0; i < events.size(); ++i) {
            if (events.get(i).docs.size() < parameters.minTopicSize) {
                events.remove(i);
                i--;
            }
        }

        // refine each document cluster's keyword graph, filter redundant keywords
        for (Event e: events) {
            e.refineKeyGraph();
        }

        System.out.println("#Document Clusters (final): " + events.size());

        return events;
	}

    /**
     * Split topics to events with the algorithm set in parameter file.
     * <p>
     * @param events Topics to split.
     * @param DF Keyword to DF map.
     * @param docAmount Document amount.
     * @return Fine grained events.
     */
    public ArrayList<Event> splitEvents(ArrayList<Event> events,
                                        HashMap<String, Double> DF,
                                        int docAmount) throws Exception {
        // further split events by rule or by supervised learning
        switch (parameters.eventSplitAlg) {
            case "DocRelation":
                System.out.println("Use DocRelation algorithm......");
                EventSplitterDocRelation splitterDocRelation = new EventSplitterDocRelation(parameters);
                events = splitterDocRelation.splitEvents(events, DF, docAmount);
                break;
            case "Rule":
                System.out.println("Use Rule algorithm......");
                EventSplitterRule splitterRule = new EventSplitterRule(parameters);
                events = splitterRule.splitEvents(events);
                break;
            case "DocGraph":
                System.out.println("Use DocGraph algorithm......");
                EventSplitterDocGraph splitterDocGraph = new EventSplitterDocGraph(parameters);
                events = splitterDocGraph.splitEvents(events, DF, docAmount);
                break;
            case "None":
                break;
            default:
                break;
        }

        return events;
    }

    /**
     * Given documents and keyword communities, find matched documents for each keyword community
     * and return document clusters. Each cluster corresponds to a topic or event.
     * @param corpus The corpus we are handling.
     * @param communities Keyword communities.
     * @return A list of document clusters.
     */
	public ArrayList<Event> extractTopicsByKeywordCommunities(Corpus corpus,
                                                              ArrayList<HashMap<String, KeywordNode>> communities)
            throws Exception {
        // initialize document clusters
        ArrayList<Event> result = new ArrayList<>();

        // STEP 1: keyword graph based document clustering
        HashMap<String, Integer> doc_community = new HashMap<>();
        HashMap<String, Double> doc_similarity = new HashMap<>();
        for (Document d: corpus.docs.values()) {
            doc_community.put(d.id, -1);
            doc_similarity.put(d.id, -1.0);
        }


//        // for each keyword community, find matched documents by cosine similarity
//		for (HashMap<String, KeywordNode> c : communities) {
//            Event e = new Event();
//			e.keyGraph = c;
//
//			for (KeywordNode n : c.values()) {
//                // only try to match the documents that contain keyword in the keyword community
//                for (Document d : n.keyword.documents.values()) {
//                    //if (!e.docs.containsKey(d.id) && !d.processed) {
//                    if (!e.docs.containsKey(d.id)) {
//                        double cosineSimilarity = tfidfCosineSimilarityGraph2Doc(c, d, corpus.DF, corpus.docs.size());
//                        // System.out.println("similarity: " + cosineSimilarityByTFIDF); //!!!!!!!!!!!!!
//                        if (cosineSimilarity > parameters.minSimDoc2KeyGraph) {
//                            e.docs.put(d.id, d);
//                            e.similarities.put(d.id, cosineSimilarity);
//                        }
//                        d.processed = true;
//                    }
//                }
//            }
//
//            // add document cluster to the cluster array
//            if (e.docs.size() >= parameters.minTopicSize) {
//                result.add(e);
//            }
//		}

        // for each keyword community, find matched documents by cosine similarity
        for (int i = 0; i < communities.size(); ++i) {
            HashMap<String, KeywordNode> c = communities.get(i);

            for (KeywordNode n : c.values()) {
                // only try to match the documents that contain keyword in the keyword community
                for (Document d : n.keyword.documents.values()) {
                    double cosineSimilarity = tfidfCosineSimilarityGraph2Doc(c, d, corpus.DF, corpus.docs.size());
                    //System.out.println("cosineSimilarity is " + cosineSimilarity); //DEBUG previously contains NaN !!!
                    if (//cosineSimilarity > parameters.minSimDoc2KeyGraph &&  // some similarity is always 0!!!!???
                            cosineSimilarity > doc_similarity.get(d.id)) {
                        doc_community.put(d.id, i);
                        doc_similarity.put(d.id, cosineSimilarity);
                    }
                }
            }
        }

        for (int i = 0; i < communities.size(); ++i) {
            HashMap<String, KeywordNode> c = communities.get(i);

            Event e = new Event();
            e.keyGraph = c;

            for (String doc_id: doc_community.keySet()) {
                if (doc_community.get(doc_id) == i) {
                    Document d = corpus.docs.get(doc_id);
                    if (!e.docs.containsKey(d.id)) {
                        e.docs.put(d.id, d);
                        e.similarities.put(d.id, doc_similarity.get(doc_id));
                    }
                    d.processed = true;
                }
            }

            // add document cluster to the cluster array
            if (e.docs.size() >= parameters.minTopicSize) {
                result.add(e);
            }
        }

		return result;
	}



    /**
     * Merge similar events based on the intersect documents' proportion.
     * @param events The events we want to merge. After execute this function,
     *               similar events inside it will be merged into one event.
     */
    private void mergeSimilarEvents(ArrayList<Event> events) {
        ArrayList<Event> topics = new ArrayList<Event>();

        while (events.size() > 0) {
            Event dc1 = events.remove(0);
            ArrayList<Event> toRemove = new ArrayList<Event>();

            boolean isChanged = false;
            do {
                isChanged = false;
                for (Event dc2 : events) {
                    double intersect = MapUtils.numIntersect(dc1.docs, dc2.docs);

                    // merge two clusters if their intersect proportion is bigger than a threshold
                    if (intersect / Math.min(dc1.docs.size(), dc2.docs.size()) >=
                            parameters.minIntersectPercentToMergeCluster) {
                        mergeEvents(dc1, dc2);
                        isChanged = true;
                        toRemove.add(dc2);
                    }
                }
                events.removeAll(toRemove);
            } while (isChanged);

            topics.add(dc1);
        }

        events.addAll(topics);
    }

    /**
     * Merge two events.
     * @param e1 One event.
     * @param e2 Another event. It will be merged into dc1.
     */
    public void mergeEvents(Event e1, Event e2) {
        for (Document d : e2.docs.values()) {
            if (!e1.docs.containsKey(d.id)) {
                e1.docs.put(d.id, d);
                e1.similarities.put(d.id, e2.similarities.get(d.id));
            } else if (e1.similarities.get(d.id) < e2.similarities.get(d.id)) {
                e1.similarities.put(d.id, e2.similarities.get(d.id));
            }
        }
        e1.keyGraph.putAll(e2.keyGraph);
    }

    /**
     * Print clustered documents. Each cluster is a topic.
     * @param clusters A collection of document clusters.
     * @param out Output stream.
     */
	public static void printTopics(Collection<Event> clusters, PrintStream out) {
		for (Event dc : clusters) {
			out.print("KEYWORDS:\t");
			printKeywords(dc, out);

			out.print("\nDOCUMNETS:\t");
			for (Document d : dc.docs.values()) {
                out.print(d.title + " | ");
            }

			/*
			out.print("\nKEYGRAPH_NODES:\t");
			for (KeywordNode n : e.keyGraph.values())
				out.print(n.id + ":" + n.keyword.baseForm + ":" + n.keyword.word.replaceAll("[,'\"]", " ") + ",");

			out.println("\nKEYGRAPH_EDGES:\t");
			for (KeywordNode n : e.keyGraph.values()) {
				for (KeywordEdge e : n.edges.values())
					if (e.n1.equals(n))
						out.print(e.n1.id + ":" + e.n1.keyword.baseForm + "-" + e.n2.id + ":" + e.n2.keyword.baseForm + ",");
			}
			*/

			out.println("\n");
		}

	}

    /**
     * Print the keyword graph of a document cluster.
     * @param dc A document cluster.
     * @param out Output stream.
     */
	public static void printKeywords(Event dc, PrintStream out) {
		for (KeywordNode n : dc.keyGraph.values()) {
            out.print(n.keyword.word.replaceAll("[,'\"]", " ") + ",");
        }
	}

    /**
     * Print keyword graph.
     * @param keyGraph The keyword graph we want to print.
     * @param out The output stream where we print.
     */
	public static void printKeyGraph(HashMap<String, KeywordNode> keyGraph, PrintStream out) {
		for (KeywordNode n : keyGraph.values()) {
            out.print(n.id + ":" + n.keyword.baseForm + ":" + n.keyword.word.replaceAll("[,'\"]", " ") + ",");
        }
		out.println();
		for (KeywordNode n : keyGraph.values()) {
			for (KeywordEdge e : n.edges.values()) {
                if (e.n1.equals(n)) {
                    out.print(e.n1.id + ":" + e.n1.keyword.baseForm + "-" + e.n2.id + ":" + e.n2.keyword.baseForm + ",");
                }
            }
		}
		out.println();
	}

    /**
     * Calculate the cosine similarity between a keyword community and a document.
     *
     * As a community of keywords can be seen as a "key document", the cosine similarity
     * between it and a document is similar calculated with that between two documents.
     *
     * @param community A keyword graph.
     * @param d2 A document.
     * @param DF The map between keywords and their df.
     * @param docSize The number of total documents.
     * @return Cosine similarity between community and document d2.
     */
	public static double tfidfCosineSimilarityGraph2Doc(HashMap<String, KeywordNode> community,
                                                        Document d2,
                                                        HashMap<String, Double> DF,
                                                        int docSize) {
		double sim = 0;
		double vectorSize1 = 0;
		int numberOfKeywordsInCommon = 0;

		for (KeywordNode n : community.values()) {
            // calculate the community keyword's tf
			double nTF = 0;
			for (KeywordEdge e : n.edges.values()) {
                nTF += Math.max(e.cp1, e.cp2);
            }
			n.keyword.tf = nTF / n.edges.size();

            if (DF.containsKey(n.keyword.baseForm)) {  //TODO: 暂缓之计
                // update vector size of community
                vectorSize1 += Math.pow(NlpUtils.tfidf(n.keyword.tf, NlpUtils.idf(DF.get(n.keyword.baseForm), docSize)), 2);

                // update similarity between document d2 and community
                if (d2.keywords.containsKey(n.keyword.baseForm)) {
                    numberOfKeywordsInCommon++;
                    sim += NlpUtils.tfidf(n.keyword.tf, NlpUtils.idf(DF.get(n.keyword.baseForm), docSize)) *
                            NlpUtils.tfidf(d2.keywords.get(n.keyword.baseForm).tf, NlpUtils.idf(DF.get(n.keyword.baseForm), docSize));
                }
            }
		}
		vectorSize1 = Math.sqrt(vectorSize1);

        // return similarity
		if (vectorSize1 > 0 && d2.tfidfVectorSizeWithKeygraph > 0) {  // TODO: before it is 2
            return sim / vectorSize1 / d2.tfidfVectorSizeWithKeygraph;
        } else {
            return 0;
        }
	}

    /**
     * Calculate the variance of documents.
     * @param dc The document cluster.
     * @param DF The map between keywords and their df.
     * @param docSize Number of total documents.
     * @return The variance of documents in cluster e.
     */
	public double variance(Event dc,
                           HashMap<String, Double> DF,
                           int docSize) {
		double var = 0;
		if (dc.centroid == null) {
            dc.centroid = centroid(dc.docs, DF);
        }
		for (Document d : dc.docs.values()) {
			double diff = 1 - FeatureExtractor.cosineSimilarityByTFIDF(dc.centroid, d, DF, docSize);
			var += diff * diff;
		}
		return var / dc.docs.size();
	}

    /**
     * Calculate the centroid document of a set of documents.
     * @param docs A set of documents.
     * @param DF The map between keywords and their df.
     * @return centroid The centroid document of the set of documents.
     */
	public Document centroid(HashMap<String, Document> docs,
                             HashMap<String, Double> DF) {
		Document centroid = new Document("-1");
		for (Document d : docs.values()) {
            for (Keyword k : d.keywords.values()) {
                if (centroid.keywords.containsKey(k.baseForm)) {
                    Keyword kk = centroid.keywords.get(k.baseForm);
                    kk.tf += k.tf;
                    kk.df++;  // ??
                } else {
                    centroid.keywords.put(k.baseForm, new Keyword(k.baseForm, k.word, k.tf, k.df));
                }
            }
        }
		for (Keyword k : centroid.keywords.values()) {
            if (NlpUtils.idf(k.df, docs.size()) != 0) {
                k.tf /= docs.size();
                centroid.tfidfVectorSizeWithKeygraph += Math.pow(NlpUtils.tfidf(k.tf, DF.get(k.baseForm)), 2);
            } else {
                k.tf = 0;
            }
        }
		centroid.tfidfVectorSizeWithKeygraph = Math.sqrt(centroid.tfidfVectorSizeWithKeygraph);
		return centroid;
	}

    /**
     * Compute document's vector size.
     * <p>
     * Given document's keywords [w1, w2, ..., wn], it is represented by keywords' tf-idf values.
     * Thus, if our dictionary contains N words, a document will be represented by a sparse vector, where
     * each element value in the vector is the tf-idf value of the corresponding keyword.
     * The vector size of a document is sqrt(sum(tf-idf(wi)^2)).
     * <p>
     * @param docs A set of documents.
     * @param DF The map between keywords and their df.
     * @param graphNodes The whole graph nodes hash map built from docs.
     */
    public void calcDocsTFIDFVectorSizeWithGraph(HashMap<String, Document> docs,
                                                 HashMap<String, Double> DF,
                                                 HashMap<String, KeywordNode> graphNodes) {
        for (Document d : docs.values()) {
            d.tfidfVectorSizeWithKeygraph = 0;
            for (Keyword k : d.keywords.values()) {
                if (graphNodes.containsKey(k.baseForm)) {
                    d.tfidfVectorSizeWithKeygraph += Math.pow(NlpUtils.tfidf(k.tf, NlpUtils.idf(DF.get(k.baseForm), docs.size())), 2);
                }
            }
            d.tfidfVectorSizeWithKeygraph = Math.sqrt(d.tfidfVectorSizeWithKeygraph);
        }
    }

}
