package edu.ualberta.storyteller.core.keywordorganizer;

import edu.ualberta.storyteller.core.parameter.Parameters;
import edu.ualberta.storyteller.core.dataloader.Keyword;
import edu.ualberta.storyteller.core.dataloader.Corpus;
import edu.ualberta.storyteller.core.dataloader.Document;
import java.io.Serializable;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class defines keyword graph (KeyGraph).
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class KeywordGraph implements Serializable {

    /**
     * Configuration.
     */
	Parameters parameters;

    /**
     * The map of graph nodes.
     */
	public HashMap<String, KeywordNode> graphNodes;


    /**
     * Default constructor.
     */
	public KeywordGraph() {
    }

    /**
     * Parametric constructor.
     * <p>
     * @param cons Configuration.
     */
	public KeywordGraph(Parameters cons) {
		parameters = cons;
	}

    /**
     * Create keyword graph given documents.
     * @param corpus The corpus we are handling.
     */
	public void buildGraph(Corpus corpus) {
		graphNodes = new HashMap<>();

		// add nodes
		for (Document d : corpus.docs.values()) {
			for (Keyword k : d.keywords.values()) {
                // create a new node or retrieve existing node given a keyword of a document
				KeywordNode n;
				if (graphNodes.containsKey(k.baseForm)) {
                    n = graphNodes.get(k.baseForm);
                } else {
					Keyword keyword = new Keyword(k.baseForm, k.word, 0, corpus.DF.get(k.baseForm));
					n = new KeywordNode(keyword);
					graphNodes.put(keyword.baseForm, n);
				}

                // record the documents associated with this node
				n.keyword.documents.put(d.id, d);  //!!!!!!!!!!!!!!!!!!!! some documents are missing. why???

                // update keyword's tf
				n.keyword.tf++;
			}
		}

//		// filter nodes
//		ArrayList<String> toRemoveIds = new ArrayList<String>();
//		for (KeywordNode n : graphNodes.values()) {
//            // remove nodes whose keyword's tf smaller than a threshold (too rare)
//            // or keyword's df bigger than a threshold (too frequent and normal)
//            if (n.keyword.tf < parameters.minNodeDF ||
//                corpus.DF.get(n.keyword.baseForm) > parameters.maxNodeDFPercent * corpus.docs.size()) {
//                toRemoveIds.add(n.keyword.baseForm);
//            }
//        }
//		for (String baseForm : toRemoveIds) {
//            graphNodes.remove(baseForm);
//        }
//		toRemoveIds.clear();

		// add edges
		for (Document d : corpus.docs.values()) {
			for (Keyword k1 : d.keywords.values()) {
				if (graphNodes.containsKey(k1.baseForm)) {
					KeywordNode n1 = graphNodes.get(k1.baseForm);
					for (Keyword k2 : d.keywords.values()) {
						if (graphNodes.containsKey(k2.baseForm) && k1.baseForm.compareTo(k2.baseForm) < 0) {
							KeywordNode n2 = graphNodes.get(k2.baseForm);
							String edgeId = KeywordEdge.getId(n1, n2);
							if (!n1.edges.containsKey(edgeId)) {
								KeywordEdge e = new KeywordEdge(n1, n2, edgeId);
								n1.edges.put(edgeId, e);
								n2.edges.put(edgeId, e);
							}
							n1.edges.get(edgeId).df++;
						}
					}
				}
			}
		}

		// filter edges
		ArrayList<KeywordEdge> toRemove = new ArrayList<KeywordEdge>();
		for (KeywordNode n : graphNodes.values()) {
			for (KeywordEdge e : n.edges.values()) {
                // remove edges with small df or edges with small edge correlation
                // (which means node n1 n2 may also be connected with a lot of other nodes)
				Double MI = e.df / (e.n1.keyword.df + e.n2.keyword.df - e.df);
				if (e.df < parameters.minEdgeDF || MI < parameters.minEdgeCorrelation) {
					toRemove.add(e);
				} else {
                    e.computeCPs();
                }
			}
			for (KeywordEdge e : toRemove) {
				e.n1.edges.remove(e.id);
				e.n2.edges.remove(e.id);
			}
			toRemove.clear();
		}

//		// post-filter nodes
//		for (KeywordNode n : graphNodes.values()) {
//            // remove nodes without edge
//            if (n.edges.size() == 0) {
//                toRemoveIds.add(n.keyword.baseForm);
//            }
//        }
//		for (String baseForm : toRemoveIds)
//			graphNodes.remove(baseForm);
//		toRemoveIds.clear();
	}

    /**
     * Merge two graphs into one graph.
     * @param kg1 One graph to merge.
     * @param kg2 Another graph to merge.
     * @return kg The merges graph.
     */
	public static HashMap<String, KeywordNode> mergeKeyGraphs(HashMap<String, KeywordNode> kg1,
                                                              HashMap<String, KeywordNode> kg2) {
        // initialize the merged graph
		HashMap<String, KeywordNode> kg = new HashMap<String, KeywordNode>();

        // add the nodes of two graphs to the merged graph
		for (KeywordNode n : kg1.values())
			kg.put(n.keyword.baseForm, new KeywordNode(n.keyword));
		for (KeywordNode n : kg2.values())
			kg.put(n.keyword.baseForm, new KeywordNode(n.keyword));

        // add the edges of two graphs to the merged graph
		for (KeywordNode n : kg1.values()) {
            for (KeywordEdge e : n.edges.values()) {
                if (n.keyword.baseForm.compareTo(e.opposite(n).keyword.baseForm) < 0) {
                    if (!kg.get(e.n1.keyword.baseForm).edges.containsKey(e.id)) {
                        KeywordNode n1 = kg.get(e.n1.keyword.baseForm);
                        KeywordNode n2 = kg.get(e.n2.keyword.baseForm);
                        KeywordEdge ee = new KeywordEdge(n1, n2, e.id);
                        n1.edges.put(ee.id, ee);
                        n2.edges.put(ee.id, ee);
                    }
                }
            }
        }
		for (KeywordNode n : kg2.values()) {
            for (KeywordEdge e : n.edges.values()) {
                if (n.keyword.baseForm.compareTo(e.opposite(n).keyword.baseForm) < 0) {
                    if (!kg.get(e.n1.keyword.baseForm).edges.containsKey(e.id)) {
                        KeywordNode n1 = kg.get(e.n1.keyword.baseForm);
                        KeywordNode n2 = kg.get(e.n2.keyword.baseForm);
                        KeywordEdge ee = new KeywordEdge(n1, n2, e.id);
                        n1.edges.put(ee.id, ee);
                        n2.edges.put(ee.id, ee);
                    }
                }
            }
        }

		return kg;
	}

    public static void removeNode(HashMap<String, KeywordNode> graphNodes, String keyword) {
        ArrayList<String> toRemove = new ArrayList<String>();
        for (String key: graphNodes.keySet()) {
            KeywordNode keywordNode = graphNodes.get(key);
            if (keywordNode.keyword.equals(keyword)) {
                toRemove.add(key);
            }
        }
        for (String key: toRemove) {
            graphNodes.get(key).removeAllEdges();
            graphNodes.remove(key);
        }
    }

    /**
     * Get all string keywords contained in a graph.
     * @param graph The graph we are analyzing.
     * @return A set of keywords.
     */
    public static HashSet<String> getKeywords(HashMap<String, KeywordNode> graph) {
        HashSet<String> keywords = new HashSet<>();
        for (KeywordNode kw: graph.values()) {
            keywords.add(kw.keyword.word);
        }
        return keywords;
    }

    /**
     * Print graph nodes and edges to node.txt and edge.txt.
     * <p>
     * @param nodes The graph to print.
     */
    public void printGraph(HashMap<String, KeywordNode> nodes) {
        try {
            DataOutputStream nout = new DataOutputStream(new FileOutputStream("node.txt"));
            DataOutputStream eout = new DataOutputStream(new FileOutputStream("edge.txt"));
            printGraph(nodes, nout, eout);
            nout.close();
            eout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Print a list of graph to node.txt and edge.txt.
     * <p>
     * @param communities The graph array to print.
     */
    public void printGraph(ArrayList<HashMap<String, KeywordNode>> communities) {
        try {
            DataOutputStream nout = new DataOutputStream(new FileOutputStream("node.txt"));
            DataOutputStream eout = new DataOutputStream(new FileOutputStream("edge.txt"));
            for (HashMap<String, KeywordNode> nodes : communities) {
                printGraph(nodes, nout, eout);
            }
            nout.close();
            eout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Print graph nodes and edges to specific output streams.
     * <p>
     * @param nodes The graph to print.
     * @param nout The output stream of nodes.
     * @param eout The output stream of edges.
     * @throws IOException
     */
    private void printGraph(HashMap<String, KeywordNode> nodes,
                            DataOutputStream nout,
                            DataOutputStream eout) throws IOException {
        for (KeywordNode n : nodes.values()) {
            nout.writeBytes(n.id + "\t\t\t\t\t\t" + n.keyword.word + "\n");
            for (KeywordEdge e : n.edges.values()) {
                if (e.n1.equals(n)) {
                    eout.writeBytes(e.n1.id + "\t" + e.n2.id + "\n");
                }
            }
        }
    }

}
