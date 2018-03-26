package edu.ualberta.storyteller.core.storymaker;

import edu.ualberta.storyteller.core.parameter.*;
import edu.ualberta.storyteller.core.keywordorganizer.*;
import edu.ualberta.storyteller.core.eventdetector.*;

import java.util.ArrayList;


/**
 * Implement algorithm Story Timeline.
 * Stories are linear structure. Ordered by event time.
 * Ref: Event Detection and Tracking in Social Streams
 *      A Graph Analytical Approach for Fast Topic Detection
 */
public class StoryMakerLinear extends StoryMaker {

    /**
     * Parametric constructor.
     * @param parameters Configuration.
     */
    public StoryMakerLinear(Parameters parameters) {
        this.parameters = parameters;
        ed = new EventDetector(parameters);
        eventSplitter = new EventSplitterDocRelation(parameters);
    }

    /**
     * Update linear structure story.
     * @param sf Story forest.
     * @param storyIdx Index of story tree to update.
     * @param e Event.
     * @throws Exception
     */
    public void updateStoryTree(StoryForest sf, int storyIdx, Event e) throws Exception {
        // remove duplicated docs
        StoryTree st = sf.storyTrees.get(storyIdx);
        e = removeDuplicatedDocs(e, st);
        if (e.docs.size() == 0) {
            return;
        }

        // add remain docs
        // get tree's all story nodes
        ArrayList<StoryNode> storyNodes = st.build(TreeTraversalOrderEnum.PRE_ORDER);

        int matchIdx = -1;
        boolean sameEvent = false;
        long minTimestampDiff = Long.MAX_VALUE;
        if (e.centroid == null) {
            e.calcCentroid();
        }

        // compare with each story node
        for (int i = 1; i < storyNodes.size(); ++i) {
            // check whether it is an existing event in the tree
            sameEvent = sameEvent(e, storyNodes.get(i), sf.corpus.DF, sf.corpus.docs.size(), parameters.model); // TODO: which DF?
            if (sameEvent) {
                matchIdx = i;
                break;
            }

            // if not an existing event, find the closest story node by publish time
            if (storyNodes.get(i).e.centroid == null) {
                storyNodes.get(i).e.calcCentroid();
            }
            long timestampDiff = e.centroid.publishTime.getTime() - storyNodes.get(i).e.centroid.publishTime.getTime();
            if (timestampDiff < minTimestampDiff && timestampDiff > 0) {
                minTimestampDiff = timestampDiff;
                matchIdx = i;
            }
        }

        // add new story node
        if (sameEvent) {
            // merge with existing node
            //merge(e, storyNodes.get(matchIdx)); //TODO
            return;
        } else if (matchIdx == -1) {
            // connect root
            if (st.root.hasChildren()) {
                insert(e, st.root);
            } else {
                extend(e, st.root);
            }
        } else if (storyNodes.get(matchIdx).hasChildren()) {
            // insert the node into the matched node and its child
            // assume it only has one child, as we are generating linear story
            insert(e, storyNodes.get(matchIdx));
        } else {
            // extend the leaf node
            extend(e, storyNodes.get(matchIdx));
        }

        // update tree's info
        st.keyGraph = KeywordGraph.mergeKeyGraphs(st.keyGraph, e.keyGraph);
        if (st.startTimestamp > e.getStartTimestamp())
            st.startTimestamp = e.getStartTimestamp();
        if (st.endTimestamp < e.getEndTimestamp())
            st.endTimestamp = e.getEndTimestamp();
    }

    /**
     * Insert event between story node and its child.
     * @param e Event.
     * @param sn Story node.
     */
    public void insert(Event e, StoryNode sn) {
        if (e.docs.size() > 0) {
            StoryNode newSn = new StoryNode(e);
            StoryNode child = sn.getChildAt(0);
            sn.removeChildren();
            sn.addChild(newSn);
            newSn.addChild(child);
        }
    }

}
