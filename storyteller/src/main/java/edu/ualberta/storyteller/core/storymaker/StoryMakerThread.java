package edu.ualberta.storyteller.core.storymaker;

import edu.ualberta.storyteller.core.parameter.*;
import edu.ualberta.storyteller.core.keywordorganizer.*;
import edu.ualberta.storyteller.core.eventdetector.*;

import java.util.ArrayList;


/**
 * Implement algorithm Event Threading (specifically, the Best Similarity Model in it).
 * Ref: Event Threading within News Topics
 */
public class StoryMakerThread  extends StoryMaker {

    /**
     * Parametric constructor.
     * @param parameters Configuration.
     */
    public StoryMakerThread(Parameters parameters) {
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
        if (e.centroid == null) {
            e.calcCentroid();
        }
        double maxSim = 0;

        // compare with each story node
        for (int i = 1; i < storyNodes.size(); ++i) {
            StoryNode sn = storyNodes.get(i);
            // check whether it is an existing event in the tree
            sameEvent = sameEvent(e, sn, sf.corpus.DF, sf.corpus.docs.size(), parameters.model); // TODO: which DF?
            if (sameEvent) {
                matchIdx = i;
                break;
            }

            // if not an existing event, find the closest story node by publish time
            if (sn.e.centroid == null) {
                sn.e.calcCentroid();
            }
            if (e.centroid.publishTime.getTime() > sn.e.centroid.publishTime.getTime()) {
                double sim = FeatureExtractor.cosineSimilarityByTF(e.centroid, sn.e.centroid);
                if (sim > maxSim && sim > parameters.minTFCosineSimilarityDc2Sn) {
                    maxSim = sim;
                    matchIdx = i;
                }
            }
        }

        // add new story node
        if (sameEvent) {
            // merge with existing node
            //merge(e, storyNodes.get(matchIdx)); //TODO
            return;
        } else if (matchIdx == -1) {
            // connect root
            extend(e, st.root);
        } else {
            // connect node
            extend(e, storyNodes.get(matchIdx));
        }

        // update tree's info
        st.keyGraph = KeywordGraph.mergeKeyGraphs(st.keyGraph, e.keyGraph);
        if (st.startTimestamp > e.getStartTimestamp())
            st.startTimestamp = e.getStartTimestamp();
        if (st.endTimestamp < e.getEndTimestamp())
            st.endTimestamp = e.getEndTimestamp();
    }
}
