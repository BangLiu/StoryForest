package edu.ualberta.storyteller.core.keywordorganizer;

import edu.ualberta.storyteller.core.dataloader.Keyword;
import java.io.Serializable;
import java.util.HashMap;

/**
 * This class defines the vertex for keyword graph.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class KeywordNode implements Serializable {

	/**
	 * Current max id. Used for automatically assign id to new nodes.
	 */
	static int maxID = 1;

    /**
     * ID
     */
	public String id;

    /**
     * Keyword contained in this node.
     */
	public Keyword keyword;

	/**
     * Edges connected with this node.
	 * Map <KeywordEdge id, KeywordEdge>.
     */
	public HashMap<String, KeywordEdge> edges = new HashMap<>();

    /**
     * Whether this node has been visited or not.
     */
	public boolean visited;

    /**
     * Previously visited node.
     */
	public KeywordNode prev;

	/**
	 * Parametric constructor.
     * <p>
	 * @param keyword Keyword contained in this node.
     */
	public KeywordNode(Keyword keyword) {
		id = maxID++ + "";
		this.keyword = keyword;
	}

	/**
	 * Connect this node with new node n2.
	 * Return the new created edge.
     * <p>
	 * @param n2 The node to connect.
	 * @return KeywordEdge between this node and n2.
     */
	public KeywordEdge insertEdge(KeywordNode n2) {
		String edgeId = KeywordEdge.getId(this, n2);
		if (!edges.containsKey(edgeId)) {
			KeywordEdge e = new KeywordEdge(this, n2, edgeId);
			edges.put(edgeId, e);
			n2.edges.put(edgeId, e);
		}
		return edges.get(edgeId);
	}

    /**
     * Remove edge that connected with a specific node.
     * <p>
     * @param n2 The node connect with.
     */
    public void removeEdge(KeywordNode n2) {
        String edgeId = KeywordEdge.getId(this, n2);
        if (edges.containsKey(edgeId)) {
            KeywordEdge keywordEdge = edges.get(edgeId);
            KeywordNode n = keywordEdge.opposite(this);
            String edgeId2 = KeywordEdge.getId(n2, this);
            edges.remove(edgeId);
            n.edges.remove(edgeId2);
        }
    }

    /**
     * Delete all edges connect with this node.
     */
    public void removeAllEdges() {
        for (KeywordEdge keywordEdge : edges.values()) {
            KeywordNode n = keywordEdge.opposite(this);
            removeEdge(n);
        }
    }

}
