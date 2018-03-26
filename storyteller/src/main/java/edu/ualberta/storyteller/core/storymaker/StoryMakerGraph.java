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
 * Implement algorithm Event Evolution Graph.
 * Ref: Discovering Event Evolution Graphs From News Corpora
 */
public class StoryMakerGraph extends StoryMaker {

    /**
     * Parametric constructor.
     * @param parameters Configuration.
     */
    public StoryMakerGraph(Parameters parameters) {
        this.parameters = parameters;
        ed = new EventDetector(parameters);
        eventSplitter = new EventSplitterDocRelation(parameters);
    }

    /**
     * Update graph structure story.
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

        // compare with each story node to merge same event
        int matchIdx = -1;
        boolean isSameEvent = false;
        for (int i = 1; i < storyNodes.size(); ++i) {
            // check whether it is an existing event in the tree
            isSameEvent = sameEvent(e, storyNodes.get(i), sf.corpus.DF, sf.corpus.docs.size(), parameters.model); // TODO: which DF?
            if (isSameEvent) {
                matchIdx = i;
                break;
            }
        }
        if (isSameEvent) {
            // merge with existing node
            //merge(e, storyNodes.get(matchIdx)); //TODO
            return;
        }

        // if not same event,
        // 1. create new node and add to graph
        StoryNode newSn = new StoryNode(e);
        st.root.addChild(newSn);

        // 2. calculate edge compatibilities and add edges to graph
        for (int i = 1; i < storyNodes.size(); ++i) {
            double compatibility = calcCompatibilityEvent2StoryNode(e, storyNodes.get(i), st);
            if (compatibility > 0) {
                double edgeKey = compatibility;  //TODO: !!!!!!! this is a bug! if same compatibility, it will be replaced!!!
                String edgeValue = storyNodes.get(i).id + " --- " + newSn.id;
                st.graphEdges.put(edgeKey, edgeValue);
            } else {
                double edgeKey = 0 - compatibility;
                String edgeValue = newSn.id + " --- " + storyNodes.get(i).id;
                st.graphEdges.put(edgeKey, edgeValue);
            }
        }

        // update tree's info
        st.keyGraph = KeywordGraph.mergeKeyGraphs(st.keyGraph, e.keyGraph);
        if (st.startTimestamp > e.getStartTimestamp())
            st.startTimestamp = e.getStartTimestamp();
        if (st.endTimestamp < e.getEndTimestamp())
            st.endTimestamp = e.getEndTimestamp();
    }

    /**
     * Calculate event to story node compatibility by considering similarity and time gap.
     * @param e Event.
     * @param sn Story node.
     * @param st Story tree that contains the story node.
     * @return Compatibility.
     */
    public double calcCompatibilityEvent2StoryNode(Event e, StoryNode sn, StoryTree st) {
        double compatibility = 0;

        // content similarity
        if (e.centroid == null) {
            e.calcCentroid();
        }
        if (sn.e.centroid == null) {
            sn.e.calcCentroid();
        }
        double contentSimilarity = FeatureExtractor.cosineSimilarityByTF(e.centroid, sn.e.centroid);

        // temporal proximity
        double timeProximity = 0;
        long T = Math.abs(Math.max(st.endTimestamp, e.getEndTimestamp()) -
                          Math.min(st.startTimestamp, e.getStartTimestamp()));
        double timeGap = 0;
        if (T != 0) {
            timeGap = (e.getStartTimestamp() - sn.e.getStartTimestamp()) / (T + 0.0);
        }
        if (timeGap >= 0) {
            timeProximity = Math.exp(-timeGap * parameters.deltaTimeGap);
        } else {
            timeProximity = 0 - Math.exp(timeGap * parameters.deltaTimeGap);
        }

        // document distributional proximity
        double docDistributionProximity = 0;
        double m = st.getNumberOfDocsByTime(e.getStartTimestamp(), sn.e.getStartTimestamp());
        double N = st.getNumberOfDocs();
        if (N != 0) {
            docDistributionProximity = Math.exp(- m / N * parameters.deltaDocDistribution);
        }

        // calculate comprehensive compatibility
        compatibility = contentSimilarity * timeProximity * docDistributionProximity;

        return compatibility;
    }

}
