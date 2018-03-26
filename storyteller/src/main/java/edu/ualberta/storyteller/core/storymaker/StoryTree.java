package edu.ualberta.storyteller.core.storymaker;

import edu.ualberta.storyteller.core.dataloader.*;
import edu.ualberta.storyteller.core.keywordorganizer.*;
import edu.ualberta.storyteller.core.eventdetector.*;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.*;


public class StoryTree implements Serializable {

    //! Current max id. Used for automatically assign id to new trees.
    public static int MAX_ID = 1;

    //! ID.
    public String id;

    //! Virtual root node.
    public StoryNode root;

    //! Keyword graph of this story tree.
    public HashMap<String, KeywordNode> keyGraph = new HashMap<String, KeywordNode>();

    //! All document titles in this tree.
    public HashSet<String> docTitles = new HashSet<String>();

    //! Hotness.
    public double hotness;

    //! Summary.
    public String summary = "";

    //! Age. How many days this tree has been created.
    public int age = 0;

    //! Stale age. How many days this tree has been unchanged.
    public int staleAge = 0;

    //! Graph edges. It will be used when generating graph structured "tree".
    public TreeMap<Double, String> graphEdges = new TreeMap<Double, String>(Collections.reverseOrder());

    //! Start time.
    public long startTimestamp = -1;

    //! End time.
    public long endTimestamp = -1;

    /**
     * Default constructor.
     */
    public StoryTree() {
        id = MAX_ID++ + "";
        root = new StoryNode();
        root.numPathNode = 0;
        root.consistency = 0;
    }

    /**
     * Parametric constructor.
     */
    public StoryTree(Event e) {
        id = MAX_ID++ + "";
        root = new StoryNode();
        root.numPathNode = 0;
        root.consistency = 0;

        StoryNode sn = new StoryNode(e);
        root.addChild(sn);
        keyGraph = e.keyGraph;

        for (String key: e.docs.keySet()) {
            docTitles.add(e.docs.get(key).segTitle);
        }

        startTimestamp = e.getStartTimestamp();
        endTimestamp = e.getEndTimestamp();
    }

    /**
     * Check whether this tree is empty.
     * @return True or false.
     */
    public boolean isEmpty() {
        return (root == null);
    }

    /**
     * Get number of nodes, including root.
     * @return Number of nodes.
     */
    public int getNumberOfNodes() {
        int numberOfNodes = 0;
        if(root != null) {
            numberOfNodes = auxiliaryGetNumberOfNodes(root) + 1;
        }
        return numberOfNodes;
    }

    /**
     * Get number of nodes under specific node, including itself.
     * @return Number of nodes.
     */
    public int getNumberOfNodes(StoryNode node) {
        int numberOfNodes = 0;
        if(node != null) {
            numberOfNodes = auxiliaryGetNumberOfNodes(node) + 1;
        }
        return numberOfNodes;
    }

    /**
     * Auxiliary function to get number of nodes under a specific node.
     * Doesn't including the node itself.
     * @param node The specific node as the subtree's root.
     * @return Number of nodes.
     */
    private int auxiliaryGetNumberOfNodes(StoryNode node) {
        int numberOfNodes = node.numberOfChildren();
        for(StoryNode child : node.children) {
            numberOfNodes += auxiliaryGetNumberOfNodes(child);
        }
        return numberOfNodes;
    }

    /**
     * Get total number of docs.
     * @return Total number of docs.
     */
    public int getNumberOfDocs() {
        return docTitles.size();
    }

    /**
     * Get number of docs within two timestamp.
     * @param timestamp1 A timestamp.
     * @param timestamp2 A timestamp.
     * @return Number of docs.
     */
    public int getNumberOfDocsByTime(long timestamp1, long timestamp2) {
        long startTimestamp;
        long endTimestamp;
        if (timestamp1 < timestamp2) {
            startTimestamp = timestamp1;
            endTimestamp = timestamp2;
        } else {
            startTimestamp = timestamp2;
            endTimestamp = timestamp1;
        }

        int numberOfDocs = 0;
        ArrayList<StoryNode> storyNodes = build(root, TreeTraversalOrderEnum.PRE_ORDER);
        for (int i = 1; i < storyNodes.size(); ++i) {
            StoryNode sn = storyNodes.get(i);
            for (Document d: sn.e.docs.values()) {
                if (d.publishTime.getTime() >= startTimestamp && d.publishTime.getTime() <= endTimestamp)
                    numberOfDocs++;
            }
        }
        return numberOfDocs;
    }

    /**
     * Get start timestamp of this tree.
     * @return Start timestamp.
     */
    public long getStartTimestamp() {
        ArrayList<StoryNode> storyNodes = build(root, TreeTraversalOrderEnum.PRE_ORDER);
        if (storyNodes.size() <= 1)
            return -1;
        long timestamp = System.currentTimeMillis();
        for (int i = 1; i < storyNodes.size(); ++i) {
            StoryNode sn = storyNodes.get(i);
            if (sn.e.getStartTimestamp() < timestamp) {
                timestamp = sn.e.getStartTimestamp();
            }
        }
        return timestamp;
    }

    /**
     * Get end timestamp of this tree.
     * @return End timestamp.
     */
    public long getEndTimestamp() {
        ArrayList<StoryNode> storyNodes = build(root, TreeTraversalOrderEnum.PRE_ORDER);
        if (storyNodes.size() <= 1)
            return -1;
        long timestamp = -1;
        for (int i = 1; i < storyNodes.size(); ++i) {
            StoryNode sn = storyNodes.get(i);
            if (sn.e.getEndTimestamp() > timestamp) {
                timestamp = sn.e.getEndTimestamp();
            }
        }
        return timestamp;
    }

    /**
     * Get tree nodes of this tree by specific order.
     * @param traversalOrder Traverse order.
     * @return KeywordNode list.
     */
    public ArrayList<StoryNode> build(TreeTraversalOrderEnum traversalOrder) {
        ArrayList<StoryNode> returnList = null;

        if(root != null) {
            returnList = build(root, traversalOrder);
        }

        return returnList;
    }

    /**
     * Get tree nodes by specific order.
     * @param node The starting node.
     * @param traversalOrder Traverse order.
     * @return KeywordNode list.
     */
    public ArrayList<StoryNode> build(StoryNode node,
                                      TreeTraversalOrderEnum traversalOrder) {
        ArrayList<StoryNode> traversalResult = new ArrayList<StoryNode>();

        if(traversalOrder == TreeTraversalOrderEnum.PRE_ORDER) {
            buildPreOrder(node, traversalResult);
        }

        else if(traversalOrder == TreeTraversalOrderEnum.POST_ORDER) {
            buildPostOrder(node, traversalResult);
        }

        return traversalResult;
    }

    /**
     * Get tree nodes by pre-order.
     * Start from given node, save traversed nodes to a list.
     * @param node The starting node.
     * @param traversalResult List to save all traversed nodes.
     */
    private void buildPreOrder(StoryNode node, ArrayList<StoryNode> traversalResult) {
        traversalResult.add(node);

        for(StoryNode child : node.children) {
            buildPreOrder(child, traversalResult);
        }
    }

    /**
     * Get tree nodes by post-order.
     * Start from given node, save traversed nodes to a list.
     * @param node The starting node.
     * @param traversalResult List to save all traversed nodes.
     */
    private void buildPostOrder(StoryNode node, ArrayList<StoryNode> traversalResult) {
        for(StoryNode child : node.children) {
            buildPostOrder(child, traversalResult);
        }

        traversalResult.add(node);
    }


    /**
     * Get map of nodes with specific order. Start from root.
     * @param traversalOrder Traverse order.
     * @return <node, depth> map.
     */
    public Map<StoryNode, Integer> buildWithDepth(TreeTraversalOrderEnum traversalOrder) {
        Map<StoryNode, Integer> returnMap = null;

        if(root != null) {
            returnMap = buildWithDepth(root, traversalOrder);
        }

        return returnMap;
    }

    /**
     * Get map of nodes with specific order and starting node.
     * @param node Starting node.
     * @param traversalOrder Traverse order.
     * @return <node, depth> map.
     */
    public Map<StoryNode, Integer> buildWithDepth(StoryNode node,
                                                  TreeTraversalOrderEnum traversalOrder) {
        Map<StoryNode, Integer> traversalResult = new LinkedHashMap<StoryNode, Integer>();

        if(traversalOrder == TreeTraversalOrderEnum.PRE_ORDER) {
            buildPreOrderWithDepth(node, traversalResult, 0);
        }

        else if(traversalOrder == TreeTraversalOrderEnum.POST_ORDER) {
            buildPostOrderWithDepth(node, traversalResult, 0);
        }

        return traversalResult;
    }

    /**
     * Get map of nodes with pre-order.
     * @param node Starting node.
     * @param traversalResult <node, depth> map.
     * @param depth Current node depth.
     */
    private void buildPreOrderWithDepth(StoryNode node,
                                        Map<StoryNode, Integer> traversalResult,
                                        int depth) {
        traversalResult.put(node, depth);

        for(StoryNode child : node.children) {
            buildPreOrderWithDepth(child, traversalResult, depth + 1);
        }
    }

    /**
     * Get map of nodes with post-order.
     * @param node Starting node.
     * @param traversalResult <node, depth> map.
     * @param depth Current node depth.
     */
    private void buildPostOrderWithDepth(StoryNode node,
                                         Map<StoryNode, Integer> traversalResult,
                                         int depth) {
        for(StoryNode child : node.children) {
            buildPostOrderWithDepth(child, traversalResult, depth + 1);
        }

        traversalResult.put(node, depth);
    }

    /**
     * Print tree.
     * @param out Output print stream.
     */
    public void print(PrintStream out) {
        out.println("Tree id: " + this.id);

        ArrayList<StoryNode> sns = build(root, TreeTraversalOrderEnum.PRE_ORDER);
        for (StoryNode sn: sns) {
            if (sn != root) {
                out.print("ID:\t");
                out.println(sn.id);

                out.print("Parent ID:\t");
                out.println(sn.parent.id);

                out.print("Children ID:\t");
                for (StoryNode child : sn.children) {
                    out.print(child.id + " | ");
                }
                out.println();

                out.print("KEYWORDS:\t");
                EventDetector.printKeywords(sn.e, out);

                out.print("\nDOCUMNETS:\t");
                for (Document d : sn.e.docs.values())
                    out.print(d.title + " | ");

                out.println("\n");
            } else {
                out.println("ROOT");
                out.print("ID:\t");
                out.println(sn.id);

                out.print("Children ID:\t");
                for (StoryNode child : sn.children) {
                    out.print(child.id + " | ");
                }
                out.println("\n");
            }
        }
    }

    /**
     * Print tree in directory tree style.
     * @param out Output print stream.
     **/
    public void printStoryTree(PrintStream out) {
        int indent = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("Tree " + id + "\n");
        sb.append("Tree summary: " + summary + "\n");
        sb.append("Tree hotness: " + hotness + "\n");
        sb.append("Tree keywords: (");
        for (KeywordNode n : keyGraph.values())
            sb.append(n.keyword.word.replaceAll("[,'\"]", " ") + ",");
        sb.append(")\n");
        printStoryTree(this.root, indent, sb);
        out.println(sb.toString());
    }

    public void printStoryTree(StoryNode sn, int indent, StringBuilder sb) {
        if (!sn.hasChildren()) {
            printStoryNode(sn, indent, sb);
        }
        sb.append(getIndentString(indent));
        sb.append("+-- ");
        sb.append(sn.toString());
        sb.append("\n");
        for (StoryNode child: sn.children) {
            if (child.hasChildren()) {
                printStoryTree(child, indent + 1, sb);
            } else {
                printStoryNode(child, indent + 1, sb);
            }
        }

    }

    public void printStoryNode(StoryNode sn, int indent, StringBuilder sb) {
        sb.append(getIndentString(indent));
        sb.append("+-- ");
        sb.append(sn.toString());
        sb.append("\n");
    }

    public static String getIndentString(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("|  ");
        }
        return sb.toString();
    }

    /**
     * Print DAG structure.
     */
    public void printDAG(PrintStream out) {
        out.println("Tree " + id);
        ArrayList<StoryNode> sns = build(root, TreeTraversalOrderEnum.PRE_ORDER);
        for (StoryNode sn: sns) {
            out.println(sn.toString());
        }

        out.println("\nEdges: ");
        for (Double key: graphEdges.keySet()) {
            String value = graphEdges.get(key);
            out.println(key + " : " + value);
        }

        out.println("\n");
    }

}
