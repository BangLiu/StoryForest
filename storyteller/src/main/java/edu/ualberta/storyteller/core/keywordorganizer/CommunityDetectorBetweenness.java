package edu.ualberta.storyteller.core.keywordorganizer;

import edu.ualberta.storyteller.core.dataloader.Keyword;
import edu.ualberta.storyteller.core.parameter.Parameters;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by bangliu on 2017-12-22.
 */
public class CommunityDetectorBetweenness {

    /**
     * Find communities using betweenness centrality.
     * <p>
     * @param nodes The graph we need to find communities from.
     * @param communities The communities we found from the subgraph.
     *                    This parameter is used to iteratively find communities.
     * @param parameters Configuration.
     * @return communities The communities extracted from the graph.
     */
    public static ArrayList<HashMap<String, KeywordNode>> detectCommunitiesBetweenness(
            HashMap<String, KeywordNode> nodes,
            ArrayList<HashMap<String, KeywordNode>> communities,
            Parameters parameters) {
        // find the edge with maximum betweenness score
        KeywordEdge maxKeywordEdge = findMaxEdge(nodes);

        // decide whether continue to find sub communities
        if (getFilterStatus(nodes.size(), maxKeywordEdge, parameters)) {
            // remove the edge with maximum betweenness score
            maxKeywordEdge.n1.edges.remove(maxKeywordEdge.id);
            maxKeywordEdge.n2.edges.remove(maxKeywordEdge.id);

            // check if the graph still connected,
            // if yes, iteratively run to find communities
            HashMap<String, KeywordNode> subgraph1 = findSubgraph(maxKeywordEdge.n1, nodes);
            if (subgraph1.size() == nodes.size()) {
                return detectCommunitiesBetweenness(nodes, communities, parameters);
            } else {
                // remove a subgraph from the whole graph
                for (String key : subgraph1.keySet()) {
                    nodes.remove(key);
                }

                // duplicate edge if the conditional probability is higher than threshold
                if (maxKeywordEdge.cp1 > parameters.minCpToDuplicateEdge) {
                    Keyword k = maxKeywordEdge.n2.keyword;
                    KeywordNode newn = new KeywordNode(new Keyword(k.baseForm, k.word, k.tf, k.df));
                    KeywordEdge e = new KeywordEdge(maxKeywordEdge.n1, newn);
                    maxKeywordEdge.n1.edges.put(e.id, e);
                    newn.edges.put(e.id, e);
                    subgraph1.put(k.baseForm, newn);
                }
                if (maxKeywordEdge.cp2 > parameters.minCpToDuplicateEdge) {
                    Keyword k = maxKeywordEdge.n1.keyword;
                    KeywordNode newn = new KeywordNode(new Keyword(k.baseForm, k.word, k.tf, k.df));
                    KeywordEdge e = new KeywordEdge(newn, maxKeywordEdge.n2);
                    maxKeywordEdge.n2.edges.put(e.id, e);
                    newn.edges.put(e.id, e);
                    nodes.put(k.baseForm, newn);
                }

                // keep find communities for the separated two subgraphs
                detectCommunitiesBetweenness(subgraph1, communities, parameters);
                detectCommunitiesBetweenness(nodes, communities, parameters);

                return communities;
            }
        } else {
            communities.add(nodes);
            return communities;
        }
    }

    /**
     * Find the edge with maximum betweenness score.
     * <p>
     * @param nodes The graph we are analyzing.
     * @return maxEdge The edge with maximum betweenness score.
     */
    public static KeywordEdge findMaxEdge(HashMap<String, KeywordNode> nodes) {
        // clear each edge's betweenness score,
        // as it changes when graph structure changes
        for (KeywordNode n : nodes.values()) {
            for (KeywordEdge e : n.edges.values()) {
                e.betweennessScore = 0;
            }
        }

        // initialize variable to save the edge with maximum betweenness score
        KeywordEdge maxKeywordEdge = new KeywordEdge(null, null, null);
        maxKeywordEdge.betweennessScore = -1;

        // find the edge with maximum betweenness score
        for (KeywordNode source : nodes.values()) {
            for (KeywordNode n : nodes.values()) {
                n.visited = false;
            }
            maxKeywordEdge = BFS(source, maxKeywordEdge);
        }

        // for undirected graph, each shortest path will be count for twice,
        // as each node will be retrieved for twice as source or destination
        maxKeywordEdge.betweennessScore /= 2;
        return maxKeywordEdge;
    }

    /**
     * Using Breath First Search to get edge with maximum betweenness score.
     * <p>
     * Given a source node, the BFS visits each other node in the graph. Every
     * time it reaches a new unvisited node, all the edges' betweenness score
     * in the path (source node -> new unvisited node) will increase by 1 using
     * the 'updateBetweennessScore' function. After executing this function, the
     * total increased betweenness score of each edge is: how many shortest paths
     * from source node to any other nodes in the graph go through this edge.
     * <p>
     * @param source The search source node where we start the BFS algorithm.
     * @param maxKeywordEdge The edge with maximum betweenness score.
     *                       This parameter is used for iteratively executing this function.
     * @return maxKeywordEdge The edge with maximum betweenness score.
     */
    public static KeywordEdge BFS(KeywordNode source, KeywordEdge maxKeywordEdge) {
        ArrayList<KeywordNode> q = new ArrayList<>();
        q.add(source);
        while (q.size() > 0) {
            KeywordNode n = q.remove(0);
            for (KeywordEdge e : n.edges.values()) {
                KeywordNode n2 = e.opposite(n);
                if (!n2.visited) {
                    n2.visited = true;
                    n2.prev = n;
                    updateBetweennessScore(n2, source, maxKeywordEdge);
                    if (e.compareBetweenness(maxKeywordEdge) > 0) {
                        maxKeywordEdge = e;
                    }
                    q.add(n2);
                }
            }
        }
        return maxKeywordEdge;
    }

    /**
     * Update the betweenness score of edges in the path (root -> n).
     * Update maxKeywordEdge.
     * <p>
     * @param n The destination node we now arrive.
     * @param root The source node where we start the path search.
     * @param maxKeywordEdge The edge with maximum betweenness score.
     * @return maxKeywordEdge The edge with maximum betweenness score.
     */
    public static KeywordEdge updateBetweennessScore(KeywordNode n, KeywordNode root, KeywordEdge maxKeywordEdge) {
        do {
            KeywordEdge e = n.edges.get(KeywordEdge.getId(n, n.prev));
            e.betweennessScore++;
            if (e.compareBetweenness(maxKeywordEdge) > 0) {
                maxKeywordEdge = e;
            }
            n = n.prev;
        } while (!n.equals(root));
        return maxKeywordEdge;
    }

    /**
     * Decide whether continue to split graph into subgraphs.
     * <p>
     * @param graphSize Current graph size (number of nodes).
     * @param maxKeywordEdge The edge with maximum betweenness score.
     * @param parameters Configuration.
     * @return true or false. If true, it means we should continue to detect communities from current graph.
     */
    private static boolean getFilterStatus(int graphSize, KeywordEdge maxKeywordEdge, Parameters parameters) {
        double possiblePath = Math.min(graphSize * (graphSize - 1) / 2,
                parameters.maxClusterNodeSize * (parameters.maxClusterNodeSize - 1) / 2);
        double threshold = 4.2 * Math.log(possiblePath) / Math.log(2) + 1;
        boolean keepDetectCommunity =  (graphSize > parameters.minClusterNodeSize &&
                maxKeywordEdge != null &&
                maxKeywordEdge.df > 0 &&
                maxKeywordEdge.betweennessScore > threshold);
        return keepDetectCommunity;
    }

    /**
     * Extract sub graph that contains a specific node.
     * <p>
     * @param source The node which contained by the sub graph.
     * @param nodes The whole graph.
     * @return subNodes A sub graph that contains the source node.
     */
    public static HashMap<String, KeywordNode> findSubgraph(KeywordNode source, HashMap<String, KeywordNode> nodes) {
        for (KeywordNode n : nodes.values()) {
            n.visited = false;
        }
        HashMap<String, KeywordNode> subNodes = new HashMap<>();
        ArrayList<KeywordNode> q = new ArrayList<>();
        q.add(source);
        while (q.size() > 0) {
            KeywordNode n = q.remove(0);
            n.visited = true;
            subNodes.put(n.keyword.baseForm, n);
            for (KeywordEdge e : n.edges.values()) {
                KeywordNode n2 = e.opposite(n);
                if (!n2.visited) {
                    n2.visited = true;
                    q.add(n2);
                }
            }
        }
        return subNodes;
    }

    /**
     * Using Breath First Search to get edge with approximate maximum betweenness score.
     * <p>
     * Sample nodes pairs when graph size is over threshold to save time.
     * <p>
     * @param nodes The graph we are analyzing.
     * @param parameters Configuration.
     * @return maxEdge The edge with approximate maximum betweenness score.
     */
    public static KeywordEdge findMaxEdgeApproximation(HashMap<String, KeywordNode> nodes, Parameters parameters) {
        // clear each edge's betweenness score,
        // as it changes when graph structure changes
        System.out.println("KeywordNode size: " + nodes.size());
        for (KeywordNode n : nodes.values()) {
            for (KeywordEdge e : n.edges.values()) {
                e.betweennessScore = 0;
            }
        }

        // initialize variable to save the edge with maximum betweenness score
        KeywordEdge maxKeywordEdge = new KeywordEdge(null, null, null);
        maxKeywordEdge.betweennessScore = -1;

        // find the edge with approximate maximum betweenness score
        long milis = System.currentTimeMillis();
        for (KeywordNode source : nodes.values()) {
            for (KeywordNode dest : nodes.values()) {
                // sample node pairs when graph size is bigger than threshold
                if (source.id.compareTo(dest.id) < 0 &&
                        Math.random() < Math.pow((double) parameters.maxClusterNodeSize / nodes.size(), 2)) {
                    for (KeywordNode nn : nodes.values()) {
                        nn.visited = false;
                    }
                    maxKeywordEdge = BFS(source, dest, maxKeywordEdge);
                }
            }
        }
        System.out.println("Time (milis) for finding approximate maximum betweenness score edge: " +
                (System.currentTimeMillis() - milis) / 1000);
        return maxKeywordEdge;
    }

    /**
     * Using Breath First Search to get edge with maximum betweenness score.
     * <p>
     * Given source and destination node, BFS the graph and update edges' betweenness score
     * in the search path. Update maxKeywordEdge and return it.
     * NOTICE: I am suspicious about the way they do this.
     * <p>
     * @param source The source node for BFS.
     * @param dest The destination node for BFS.
     * @param maxKeywordEdge The edge with maximum betweenness score after this search.
     * @return maxKeywordEdge The edge with maximum betweenness score after this search.
     */
    public static KeywordEdge BFS(KeywordNode source, KeywordNode dest, KeywordEdge maxKeywordEdge) {
        ArrayList<KeywordNode> q = new ArrayList<KeywordNode>();
        q.add(source);
        source.visited = true;
        while (q.size() > 0) {
            KeywordNode n = q.remove(0);
            for (KeywordEdge e : n.edges.values()) {
                KeywordNode n2 = e.opposite(n);
                if (n2.equals(dest)) {
                    e.betweennessScore++;
                    if (e.compareBetweenness(maxKeywordEdge) > 0) {
                        maxKeywordEdge = e;
                    }
                    return maxKeywordEdge;
                }
                if (!n2.visited) {
                    n2.visited = true;
                    e.betweennessScore++;
                    if (e.compareBetweenness(maxKeywordEdge) > 0) {
                        maxKeywordEdge = e;
                    }
                    q.add(n2);
                }
            }
        }
        return maxKeywordEdge;
    }

}
