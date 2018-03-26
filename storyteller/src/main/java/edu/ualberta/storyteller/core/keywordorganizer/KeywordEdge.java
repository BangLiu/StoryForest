package edu.ualberta.storyteller.core.keywordorganizer;

import java.io.Serializable;

/**
 * This class defines the edge for keyword graph.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class KeywordEdge implements Serializable {

	/**
	 * ID
	 */
	public String id;

	/**
	 * connected nodes
	 */
	public KeywordNode n1, n2;

	/**
	 * how many times this edge exist within all documents
	 */
	public int df;

	/**
	 * conditional probability p(n2 | n1) and p(n1 | n2)
	 */
	public double cp1, cp2;

	/**
	 * betweenness score
	 */
	public double betweennessScore;

	/**
	 * Parametric constructor.
	 * <p>
	 * @param n1 KeywordEdge node 1.
	 * @param n2 KeywordEdge node 2.
     * @param id KeywordEdge id.
     */
	public KeywordEdge(KeywordNode n1, KeywordNode n2, String id) {
		this.n1 = n1;
		this.n2 = n2;
		this.id = id;
	}

	/**
	 * Parametric constructor with automatically generated edge id.
     * <p>
	 * @param n1 KeywordEdge node 1.
	 * @param n2 KeywordEdge node 2.
     */
	public KeywordEdge(KeywordNode n1, KeywordNode n2) {
		this(n1, n2, getId(n1, n2));
	}

	/**
	 * Generate edge id from two connected nodes.
     * <p>
	 * @param n1 KeywordEdge node 1.
	 * @param n2 KeywordEdge node 2.
     * @return Generated string edge id.
     */
	public static String getId(KeywordNode n1, KeywordNode n2) {
		if (n1.keyword.baseForm.compareTo(n2.keyword.baseForm) < 1) {
            return n1.keyword.baseForm + "_" + n2.keyword.baseForm;
        } else {
            return n2.keyword.baseForm + "_" + n1.keyword.baseForm;
        }
	}

	/**
	 * Update edge's two conditional probability.
	 */
	public void computeCPs() {
		cp1 = 1.0 * df / n1.keyword.tf;
		cp2 = 1.0 * df / n2.keyword.tf;
	}

	/**
	 * Given one end node of edge, return the node in the other end.
     * <p>
	 * @param n One of the nodes linked by this edge.
	 * @return The other end node linked by this edge.
     */
	public KeywordNode opposite(KeywordNode n) {
		if (n1.keyword.baseForm.equals(n.keyword.baseForm)) {
            return n2;
        } if (n2.keyword.baseForm.equals(n.keyword.baseForm)) {
            return n1;
        }
		return null;
	}

	/**
	 * Compare two edges' betweenness score.
     * <p>
	 * @param e The edge to be compared with this edge.
	 * @return -1 if edge e has higher betweenness score (less important)
	 *         1 if edge e has lower betweenness score (more important)
	 *         0 if the two edges are of the same.
     */
	public int compareBetweenness(KeywordEdge e) {
		if (n1.edges.size() < 2 || n2.edges.size() < 2 || betweennessScore < e.betweennessScore) {
            return -1;
        }
		if (betweennessScore > e.betweennessScore) {
            return 1;
        }
		if (df > e.df) {
            return -1;
        }
		if (df < e.df) {
            return 1;
        }
		return 0;
	}

	/**
	 * Compare two edges' link strength.
	 * Link strength is compared by edges' cp and df.
     * <p>
	 * @param e The edge to be compared with this edge.
	 * @return -1 denotes the edge to be compared with is less important,
	 *         1 denotes the edge to be compared with is more important,
	 *         0 denotes their strengths are the same.
     */
	public int compareEdgeStrength(KeywordEdge e) {
		double cp = Math.max(cp1, cp2);
		double ecp = Math.max(e.cp1, e.cp2);

		if (cp > ecp) {
            return -1;
        }
		if (cp < ecp) {
            return 1;
        }
		if (df > e.df) {
            return -1;
        }
		if (df < e.df) {
            return 1;
        }
		return 0;
	}

}
