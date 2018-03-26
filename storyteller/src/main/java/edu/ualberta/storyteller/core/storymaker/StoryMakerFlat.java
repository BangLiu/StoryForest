package edu.ualberta.storyteller.core.storymaker;

import edu.ualberta.storyteller.core.util.*;
import edu.ualberta.storyteller.core.parameter.*;
import edu.ualberta.storyteller.core.dataloader.*;
import edu.ualberta.storyteller.core.keywordorganizer.*;
import edu.ualberta.storyteller.core.ranker.EventRanker;
import edu.ualberta.storyteller.core.svm.*;
import edu.ualberta.storyteller.core.eventdetector.*;

import java.util.ArrayList;


/**
 * Implement algorithm Flat Story.
 * Stories are of flat structure, i.e., no links between events.
 */
public class StoryMakerFlat extends StoryMaker {

    /**
     * Parametric constructor.
     * @param parameters Configuration.
     */
    public StoryMakerFlat(Parameters parameters) {
        this.parameters = parameters;
        ed = new EventDetector(parameters);
        eventSplitter = new EventSplitterDocRelation(parameters);

    }

    /**
     * Update flat structure story.
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

        // compare with each story node
        for (int i = 1; i < storyNodes.size(); ++i) {
            // check whether it is an existing event in the tree
            sameEvent = sameEvent(e, storyNodes.get(i), sf.corpus.DF, sf.corpus.docs.size(), parameters.model); // TODO: use which DF?
            if (sameEvent) {
                matchIdx = i;
                break;
            }
        }

        // add new story node
        if (sameEvent) {
            // merge with existing node
            //merge(e, storyNodes.get(matchIdx)); //TODO
            return;
        } else {
            // connect with tree's root node
            extend(e, st.root);
        }

        // update tree's info
        st.keyGraph = KeywordGraph.mergeKeyGraphs(st.keyGraph, e.keyGraph);
        if (st.startTimestamp > e.getStartTimestamp())
            st.startTimestamp = e.getStartTimestamp();
        if (st.endTimestamp < e.getEndTimestamp())
            st.endTimestamp = e.getEndTimestamp();
    }

}
