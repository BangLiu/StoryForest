package edu.ualberta.storyteller.core.storymaker;

import edu.ualberta.storyteller.core.util.*;
import edu.ualberta.storyteller.core.parameter.*;
import edu.ualberta.storyteller.core.dataloader.*;
import edu.ualberta.storyteller.core.keywordorganizer.*;
import edu.ualberta.storyteller.core.ranker.EventRanker;
import edu.ualberta.storyteller.core.svm.*;
import edu.ualberta.storyteller.core.eventdetector.*;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

public class StoryNode implements Serializable {

    //! Current max id. Used for automatically assign id to new nodes.
    static int MAX_ID = 1;

    //! ID.
    public String id;

    //! Document cluster of this node.
    public Event e;

    //! Parent.
    public StoryNode parent;

    //! Start time.
    public long startTimestamp = -1;

    //! End time.
    public long endTimestamp = -1;

    //! Number of nodes in the path (root --- this).
    public int numPathNode;

    //! Consistency of this story branch (root --- this).
    public double consistency;

    //! TF vector of this node.
    public HashMap<String, Double> TF;

    //! TF vector of the path from root until this node.
    public HashMap<String, Double> pathTF;

    //! Children.
    public ArrayList<StoryNode> children;

    /**
     * Constructor.
     * Create an empty tree node without data and child.
     */
    public StoryNode() {
        super();
        id = MAX_ID++ + "";
        children = new ArrayList<StoryNode>();
    }

    /**
     * Constructor.
     * Create a tree node with data.
     * @param e Document cluster contained by this node.
     */
    public StoryNode(Event e) {
        this();
        id = MAX_ID++ + "";
        children = new ArrayList<StoryNode>();
        this.e = e;
        startTimestamp = e.getStartTimestamp();
        endTimestamp = e.getEndTimestamp();
    }

    /**
     * Set children.
     * @param children
     */
    public void setChildren(ArrayList<StoryNode> children) {
        for(StoryNode child : children) {
            child.parent = this;
        }

        this.children = children;
    }

    /**
     * Get number of children.
     * @return Size of children list.
     */
    public int numberOfChildren() {
        return children.size();
    }

    /**
     * Judge whether this node has any child.
     * @return True or false.
     */
    public boolean hasChildren() {
        return (numberOfChildren() > 0);
    }

    /**
     * Add a child node to this node.
     * @param child
     */
    public void addChild(StoryNode child) {
        child.parent = this;
        child.numPathNode = this.numPathNode + 1;
        children.add(child);
    }

    /**
     * Add a child node to specific index.
     * @param index Child node's index in the children list.
     * @param child New child node.
     * @throws IndexOutOfBoundsException
     */
    public void addChildAt(int index, StoryNode child) throws IndexOutOfBoundsException {
        child.parent = this;
        child.numPathNode = this.numPathNode + 1;
        children.add(index, child);
    }

    /**
     * Remove this node's all children nodes.
     */
    public void removeChildren() {
        this.children = new ArrayList<StoryNode>();
    }

    /**
     * Remove child node at specific index.
     * @param index The index of node to be removed.
     * @throws IndexOutOfBoundsException
     */
    public void removeChildAt(int index) throws IndexOutOfBoundsException {
        children.remove(index);
    }

    /**
     * Get the child node at specific index.
     * @param index KeywordNode index.
     * @return The specific child node.
     * @throws IndexOutOfBoundsException
     */
    public StoryNode getChildAt(int index) throws IndexOutOfBoundsException {
        return children.get(index);
    }

    /**
     * Check whether this node is a root node.
     * @return
     */
    public boolean isRoot() {
        return (this.parent == null);
    }

    public String toString() {
        String result = "<" + id + "> ";
        if (!this.isRoot()) {
            result += "(" + e.summary + ") ";
            for (String docId : e.docs.keySet()) {
                result += e.docs.get(docId).title + " | ";
            }
        } else {
            result += "ROOT";
        }
        return result;
    }



}
