package edu.ualberta.storyteller.core.keywordorganizer;

import edu.ualberta.storyteller.core.parameter.Parameters;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by bangliu on 2017-12-23.
 */
public class CommunityDetector {

    /**
     * Configuration.
     */
    Parameters parameters;

    /**
     * The output stream to show program logs.
     */
    public PrintStream logger = System.out;

    /**
     * Parametric constructor.
     * <p>
     * @param cons Configuration.
     */
    public CommunityDetector(Parameters cons) {
        parameters = cons;
    }

    /**
     * Extract sub graph communities from the whole keyword graph.
     * A graph is a map of nodes. Each node has a map of edges.
     * <p>
     * @param nodes The whole graph of keywords.
     * @return A list of sub graphs.
     */
    public ArrayList<HashMap<String, KeywordNode>> detectCommunities(HashMap<String, KeywordNode> nodes) {
        logger.println("Extract Communities...");
        for (KeywordNode n : nodes.values()) {
            n.visited = false;
        }
        ArrayList<HashMap<String, KeywordNode>> communities = new ArrayList<>();

        // extract connected components from graph
        ArrayList<HashMap<String, KeywordNode>> connectedComponents = findConnectedComponents(nodes);

        // processing each connected component sub graph
        while (connectedComponents.size() != 0) {
            HashMap<String, KeywordNode> subNodes = connectedComponents.remove(0);

            // filter small connected components
            if (subNodes.size() >= parameters.minClusterNodeSize) {
                // iteratively split big connected components into smaller ones
                // this step is in case the graph is too big, and calculate betweenness score is too time consuming.
                if (subNodes.size() > parameters.maxClusterNodeSize) {
                    filterTopKPercentOfEdges(subNodes, 1);
                    for (KeywordNode n : subNodes.values()) {
                        n.visited = false;
                    }
                    connectedComponents.addAll(0, findConnectedComponents(subNodes));
                }
                // clustering using betweenness centrality
                else {
                    switch (parameters.communityDetectAlg.toLowerCase()) {
                        case "betweenness":
                            CommunityDetectorBetweenness.detectCommunitiesBetweenness(subNodes, communities, parameters);
                            break;
                        default:
                            System.out.println("We haven't implement other community detection algorithms");
                            return null;
                    }
                }
            }
        }

        return communities;
    }

    /**
     * Extract connected components from a graph.
     * <p>
     * @param nodes The graph we want to extract connected components from.
     * @return cc Array list of sub graphs. Each sub graph is a connected component.
     */
    public ArrayList<HashMap<String, KeywordNode>> findConnectedComponents(HashMap<String, KeywordNode> nodes) {
        ArrayList<HashMap<String, KeywordNode>> cc = new ArrayList<HashMap<String, KeywordNode>>();
        while (nodes.size() > 0) {
            KeywordNode source = nodes.values().iterator().next();
            HashMap<String, KeywordNode> subNodes = new HashMap<String, KeywordNode>();
            ArrayList<KeywordNode> q = new ArrayList<KeywordNode>();
            q.add(0, source);
            while (q.size() > 0) {
                KeywordNode n = q.remove(0);
                n.visited = true;
                nodes.remove(n.keyword.baseForm);
                subNodes.put(n.keyword.baseForm, n);
                for (KeywordEdge e : n.edges.values()) {
                    KeywordNode n2 = e.opposite(n);
                    if (!n2.visited) {
                        n2.visited = true;
                        q.add(n2);
                    }
                }
            }
            cc.add(subNodes);
        }
        return cc;
    }

    /**
     * Filter top k percentage of edges in a graph.
     * <p>
     * @param nodes The graph to remove edges from.
     * @param k The percentage of edge we want to remove.
     * @return Successfully filtered some edges or not.
     */
    private boolean filterTopKPercentOfEdges(HashMap<String, KeywordNode> nodes, double k) {
        // count total number of edges in graph
        int edgeSize = 0;
        for (KeywordNode n1 : nodes.values()) {
            edgeSize += n1.edges.size();
        }
        edgeSize /= 2;

        // create edge list to save the most un-significant edges to remove
        int nToRemove = (int) (edgeSize * k / 100);
        if (nToRemove == 0) {
            return false;
        } else {
            KeywordEdge[] toRemove = new KeywordEdge[nToRemove];

            // iterate through all edges and remove appropriate edges from nodes' edge list
            // TODO: notice that here seems remove randomly top k percent edges.
            for (KeywordNode n1 : nodes.values()) {
                for (KeywordEdge e : n1.edges.values()) {
                    // to save time, do not try to remove an edge for twice.
                    if (n1.equals(e.n1)) {
                        // NOTICE: only for non-directional graph.
                        insertInto(toRemove, e);
                    }
                }
            }

            for (KeywordEdge e : toRemove) {
                e.n1.edges.remove(e.id);
                e.n2.edges.remove(e.id);
            }

            return true;
        }
    }

    /**
     * Insert edge e into remove array if the array is not full or
     * e is less significant than any of the array elements.
     * <p>
     * @param toRemove The edge array to save edges to be removed.
     *                 Edges are sorted by strength (or significance).
     *                 toRemove[0] is the edge with smallest strength.
     * @param e The new edge that may be insert into the remove edge array.
     * @return true if edge e is successfully inserted into toRemove array,
     *         false if edge e is not inserted into toRemove array.
     * NOTICE: seems the original code is incorrect. I revised this.
     */
    public boolean insertInto(KeywordEdge[] toRemove, KeywordEdge e) {
        // if array toRemove is of length 1
        if (toRemove.length == 1) {
            if (toRemove[0] == null || toRemove[0].compareEdgeStrength(e) < 0) {
                toRemove[0] = e;
                return true;
            }
            else {
                return false;
            }
        }

        // insert the new edge e into an appropriate position
        if (toRemove[toRemove.length - 1] != null && toRemove[toRemove.length - 1].compareEdgeStrength(e) >= 0) {
            return false;
        }
        else {
            int i = toRemove.length - 1;
            while (i >= 1 && (toRemove[i - 1] == null || toRemove[i - 1].compareEdgeStrength(e) < 0)) {
                toRemove[i] = toRemove[i - 1];
                i--;
            }
            toRemove[i] = e;
            return true;
        }
    }

}
