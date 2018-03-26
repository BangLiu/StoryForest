package edu.ualberta.storyteller.core.storymaker;

import edu.ualberta.storyteller.core.util.*;
import edu.ualberta.storyteller.core.parameter.*;
import edu.ualberta.storyteller.core.dataloader.*;
import edu.ualberta.storyteller.core.keywordorganizer.*;
import edu.ualberta.storyteller.core.svm.*;
import edu.ualberta.storyteller.core.summarygenerator.*;
import edu.ualberta.storyteller.core.eventdetector.*;
import java.io.PrintStream;
import java.io.File;
import java.util.*;
import java.nio.file.*;


public class StoryMaker {

    //! Configuration.
    Parameters parameters;

    //! Event detector.
    EventDetector ed;

    //! Event splitter.
    EventSplitterDocRelation eventSplitter;

    /**
     * Default constructor.
     */
    public StoryMaker() {}

    /**
     * Parametric constructor.
     * @param parameters Configuration.
     */
    public StoryMaker(Parameters parameters) {
        this.parameters = parameters;
        ed = new EventDetector(parameters);
        eventSplitter = new EventSplitterDocRelation(parameters);
    }

    /**
     * Generate stories from input document stream.
     * @param inputFileNames A list of files to mimic input document stream.
     * @param outputFileName Output file name to save story forest.
     * @param printStoryFormat The format to print stories to file. "tree" or "graph".
     * @throws Exception exception
     * @return A story forest.
     */
    public StoryForest generateStories(ArrayList<String> inputFileNames,
                                       String outputFileName,
                                       String printStoryFormat) throws Exception {
        // initialization
        StoryForest sf = new StoryForest();
        EventDetector eventDetector = new EventDetector(parameters);
        DataLoader loader = new DataLoader(parameters);

        // process each day
        for (int i = 0; i < inputFileNames.size(); ++i) {
            // cluster a new day's events
            String inputFileName = inputFileNames.get(i);

            Corpus newCorpus = loader.loadCorpus(inputFileName);
            sf.corpus.merge(newCorpus);
            sf.corpus.filterDocsByTime(TimeUtils.addDays(sf.corpus.endTime(), -parameters.historyLength),
                    sf.corpus.endTime());
            Corpus.mergeDF(sf.cumulativeDF, newCorpus.DF);
            sf.cumulativeDocAmount += newCorpus.docs.size();

            System.out.println("#Documents in story forest is " + sf.corpus.docs.size());

            ArrayList<Event> events = eventDetector.extractEventsFromCorpus(sf.corpus);

            String eventFileName = Paths.get(inputFileName).getFileName().toString() + ".event.txt";
            File outFile = new File(outputFileName);
            String eventFileFolder = outFile.getAbsoluteFile().getParent();
            // String eventFileFolder = Paths.get(outputFileName).getParent().toString();  // not good using jar. NullPointer.
            String eventFilePath = eventFileFolder + File.separator + eventFileName;
            PrintStream outEvent = new PrintStream(eventFilePath);
            EventDetector.printTopics(events, outEvent);
            outEvent.close();

            // update existing stories or create new story
            sf = updateStoriesByEvents(sf, events);

            // summarize, rank, and print each day's new stories
            sf = summarizeStories(sf);

            String storyFileName = Paths.get(inputFileName).getFileName().toString() + ".story.txt";
            File outputFile = new File(outputFileName);
            String storyFileFolder = outputFile.getAbsoluteFile().getParent();
            String storyFilePath = storyFileFolder + File.separator + storyFileName;
            PrintStream outStory = new PrintStream(storyFilePath);
            sf.print(outStory, 2, 0, printStoryFormat);
        }

        // summarize, rank, and print final all stories
        sf = summarizeStories(sf);
        PrintStream out = new PrintStream(outputFileName);
        sf.print(out, 2, Integer.MAX_VALUE, printStoryFormat);

        return sf;
    }


    public StoryForest updateStoriesByEvents(StoryForest sf, ArrayList<Event> events) throws Exception {
        for (Event e: events) {
            int storyIdx = findRelatedStory(e, sf);
            if (storyIdx >= 0) {
                updateStoryTree(sf, storyIdx, e);
                sf.storyTrees.get(storyIdx).staleAge = -1;
            } else {
                StoryTree newSt = new StoryTree(e);
                newSt.staleAge = -1;
                sf.storyTrees.add(newSt);
            }
        }
        for (int idx = 0; idx < sf.storyTrees.size(); ++idx) {
            sf.storyTrees.get(idx).age++;
            sf.storyTrees.get(idx).staleAge++;
        }

        return sf;
    }

    /**
     * Match a event to a story tree in the forest.
     * The first matched tree's index will be returned.
     * @param e Event to match.
     * @param sf Story forest to match.
     * @return The matched tree index. If none, return -1.
     */
    public int findRelatedStory(Event e, StoryForest sf) throws Exception {
        int matchIdx = -1;
        for (int i = 0; i < sf.storyTrees.size(); ++i) {
            //if (sameStory(e, sf, i, sf.cumulativeDF, sf.cumulativeDocAmount, parameters.sameStoryModel)) {
            if (sameStoryByRule(e, sf, i)) {  // TODO: debug why supervised same story cannot work well
                matchIdx = i;
                break;
            }
        }
        return matchIdx;
    }

    /**
     * Check whether a event is the same story with a tree in a forest.
     * @param e Event.
     * @param sf Story forest.
     * @param storyTreeIdx The tree index in the story forest.
     * @return Boolean.
     */
    public boolean sameStoryByRule(Event e, StoryForest sf, int storyTreeIdx) {
        StoryTree st = sf.storyTrees.get(storyTreeIdx);

        // check whether there are duplicated document title
        HashSet<String> eventDocTitles = new HashSet<>();
        for (Document d: e.docs.values()) {
            eventDocTitles.add(d.segTitle);
        }
        eventDocTitles.retainAll(st.docTitles);
        if (eventDocTitles.size() > 0) {
            return true;
        }

        // use some rules for brand new event
        // compare event and story's keyword graphs
        double keyGraphCompatibility = calcKeygraphCompatibilityEvent2Story(e, st);
        if (keyGraphCompatibility < parameters.minKeygraphCompatibilityDc2St) {
            return false;
        }

        //if (e.keyGraph.size() > 5 && keyGraphCompatibility > 0.9)
        //    return true;

        // calculate each event document title to story document title
        for (Document d: e.docs.values()) {
            for (String title: st.docTitles) {
                // at least have 1 common keyword
                String[] kws = title.split("\\s+");
                HashSet<String> titleKeywords = new HashSet<>(Arrays.asList(kws));
                titleKeywords.removeAll(parameters.stopwords);
                HashSet<String> commonWords = d.titleKeywords;
                commonWords.retainAll(titleKeywords);
                if (commonWords.size() > 0) {
                    return true;
                }
            }
        }

        return false;
    }



    /**
     * Check whether a event is the same story with a tree in a forest.
     * @param e Event.
     * @param sf Story forest.
     * @param storyTreeIdx The tree index in the story forest.
     * @return Boolean.
     */
    public boolean sameStory(Event e, StoryForest sf, int storyTreeIdx,
                             HashMap<String, Double> DF,
                             int docAmount,
                             libsvm.svm_model model) throws Exception {
        if (e.docs.size() == 0) {
            return false;
        }

        StoryTree st = sf.storyTrees.get(storyTreeIdx);

        // get tree's all story nodes
        ArrayList<StoryNode> storyNodes = st.build(TreeTraversalOrderEnum.PRE_ORDER);

        // check whether there are duplicated document title
        HashSet<String> eventDocTitles = new HashSet<>();
        for (Document d: e.docs.values()) {
            eventDocTitles.add(d.segTitle);
        }
        eventDocTitles.retainAll(st.docTitles);
        if (eventDocTitles.size() > 0) {
            return true;
        }

        // use some rules for brand new event
        // compare event and story's keyword graphs
        double keyGraphCompatibility = calcKeygraphCompatibilityEvent2Story(e, st);
        if (keyGraphCompatibility < parameters.minKeygraphCompatibilityDc2St) {
            return false;
        }

//        if (e.keyGraph.size() > 5 && keyGraphCompatibility > 0.9)
//            return true;

        // calculate each event document title to story document title
        double numMatch = 0;
        double numCompare = 0;
        for (Document d: e.docs.values()) {
            for (int i=1; i < storyNodes.size(); ++i) {
                Event e_i = storyNodes.get(i).e;
                for (Document d2: e_i.docs.values()) {
                    numCompare++;
                    if (sameStory(d, d2, DF, docAmount, model)) {
                        numMatch++;
                    }
                }
            }
        }
        if (numMatch == 0) {
            return false;
        } else if (numMatch / numCompare >= 0.6) {
            return true;
        } else {
            return false;
        }
    }


    public boolean sameStory(Document d1,
                             Document d2,
                             HashMap<String, Double> DF,
                             int docAmount,
                             libsvm.svm_model model) throws Exception {
        HashMap<String, Double> features = eventSplitter.docPairFeature(d1, d2, DF, docAmount);
        String input = "0 " + eventSplitter.formatSameEventFeature(features);  // TODO: chong gou. same with same event.
        double result = SVM.predict_x(model, input);
        if (result == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Calculate event and story's keygraph compatibility
     * @param e Event.
     * @param st Story.
     * @return Keygraph compatibility.
     */
    public double calcKeygraphCompatibilityEvent2Story(Event e, StoryTree st) {
        double compatibility = 0;

        int numIntersection = MapUtils.numIntersect(e.keyGraph, st.keyGraph);
        int numUnion = e.keyGraph.size() + st.keyGraph.size() - numIntersection;

        if (numUnion > 0) {
            compatibility = (numIntersection + .0) / numUnion;
        }

        return compatibility;
    }

    /**
     * Update story by node-path-time scoring algorithm.
     * @param sf Story forest.
     * @param storyIdx Index of story tree to update.
     * @param e Event.
     * @throws Exception
     */
    public void updateStoryTree(StoryForest sf, int storyIdx, Event e) throws Exception {
        // remove duplicated docs
        StoryTree st = sf.storyTrees.get(storyIdx);
//        e = removeDuplicatedDocs(e, st);
//        if (e.docs.size() == 0) {
//            return;
//        }

        // add remain docs
        // get tree's all story nodes
        ArrayList<StoryNode> storyNodes = st.build(TreeTraversalOrderEnum.PRE_ORDER);

        double maxCompatibility = -1;
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

            // if not an existing event, calculate compatibility
            double compatibility = calcCompatibilityEvent2StoryNode(e, storyNodes.get(i), st);
            if (compatibility > maxCompatibility) {
                maxCompatibility = compatibility;
                matchIdx = i;
            }
        }

        // based on compatibility and idx, choose appropriate operation.
        if (sameEvent) {
            // merge with existing node
            //merge(e, storyNodes.get(matchIdx)); //TODO
            return;
        } else if (maxCompatibility > parameters.minCompatibilityDc2Sn) {
            // connect with existing node
            extend(e, storyNodes.get(matchIdx));
        } else {
            // connect with tree's root node
            extend(e, st.root);
        }

        // update tree's info
        st.keyGraph = KeywordGraph.mergeKeyGraphs(st.keyGraph, e.keyGraph);
        if (st.startTimestamp > e.getStartTimestamp()) {
            st.startTimestamp = e.getStartTimestamp();
        }
        if (st.endTimestamp < e.getEndTimestamp()) {
            st.endTimestamp = e.getEndTimestamp();
        }
        for (Document d: e.docs.values()) {
            st.docTitles.add(d.segTitle);
        }
    }

    /**
     * Given a event and a story tree, remove duplicated docs in event
     * that already exist in the story tree.
     * @param e Event
     * @param st Story tree.
     * @return
     */
    public Event removeDuplicatedDocs(Event e, StoryTree st) {
        ArrayList<String> toRemove = new ArrayList<String>();
        for (String key: e.docs.keySet()) {
            if (st.docTitles.contains(e.docs.get(key).segTitle)) {
                toRemove.add(key);
            } else {
                st.docTitles.add(e.docs.get(key).segTitle);
            }
        }
        for (String key: toRemove) {
            e.docs.remove(key);
        }

        return e;
    }

    /**
     * Check whether a event is the same event with a story node.
     * @param e Event.
     * @param sn Story node.
     * @param DF Document frequency.
     * @param docAmount Document total amount.
     * @param model SVM classifier model.
     * @return Boolean.
     * @throws Exception
     */
    public boolean sameEvent(Event e, StoryNode sn, HashMap<String, Double> DF, int docAmount, libsvm.svm_model model)
            throws Exception {
        // check whether contains duplicated docs
        HashSet<String> eDocTitles = new HashSet<>();
        for (Document d: e.docs.values()) {
            eDocTitles.add(d.segTitle);
        }

        HashSet<String> snDocTitles = new HashSet<>();
        for (Document d: sn.e.docs.values()) {
            snDocTitles.add(d.segTitle);
        }

        snDocTitles.retainAll(eDocTitles);
        if (snDocTitles.size() > 0) {
            return true;
        }

        // get the first document in each document cluster
        Document d1 = e.docs.entrySet().iterator().next().getValue();
        Document d2 = sn.e.docs.entrySet().iterator().next().getValue();

        return eventSplitter.sameEvent(d1, d2, DF, docAmount, model);
    }

    /**
     * Calculate event to story node's compatibility by considering node similarity, path similarity, and time gap.
     * @param e Event.
     * @param sn Story node.
     *  @param st Story tree that contains the story node.
     * @return Event to node compatibility.
     */
    public double calcCompatibilityEvent2StoryNode(Event e, StoryNode sn, StoryTree st) {
        double compatibility = 0;

        if (e.centroid == null) {
            e.calcCentroid();
        }
        if (sn.e.centroid == null) {
            sn.e.calcCentroid();
        }

        // content similarity
        double event2StoryNodeCompatibility = FeatureExtractor.cosineSimilarityByTF(e.centroid, sn.e.centroid);

        // path similarity
        double event2PathCompatibility = (event2StoryNodeCompatibility + (sn.numPathNode - 1) * sn.consistency) / sn.numPathNode;

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

        // calculate comprehensive compatibility
        // maybe we guarantee path consistency and maximize node similarity?
        compatibility = event2StoryNodeCompatibility * event2PathCompatibility * timeProximity;

        return compatibility;
    }

    /**
     * Merge story node and event.
     * @param e
     * @param sn
     */
    public void merge(Event e, StoryNode sn) {
        sn.e.docs.putAll(e.docs);
        sn.startTimestamp = sn.e.getStartTimestamp();
        sn.endTimestamp = sn.e.getEndTimestamp();
    }

    /**
     * Extend story node by adding new event node as its child.
     * @param e
     * @param sn
     */
    public void extend(Event e, StoryNode sn) {
        if (e.docs.size() > 0) {
            StoryNode newSn = new StoryNode(e);
            sn.addChild(newSn);
            if (!sn.isRoot()) {
                double event2StoryNodeCompatibility = FeatureExtractor.cosineSimilarityByTF(e.centroid, sn.e.centroid);
                double event2PathCompatibility = (event2StoryNodeCompatibility + (sn.numPathNode - 1) * sn.consistency)
                                              / sn.numPathNode;
                newSn.consistency = event2PathCompatibility;
            } else {
                newSn.consistency = 0;
            }
        }
    }

    /**
     * Insert event into two story nodes.
     * It becomes from sn1 -> sn2 to sn1 -> sn(e) -> sn2.
     * TODO: implement it.
     * @param e
     * @param sn1
     * @param sn2
     */
    public void insert(Event e, StoryNode sn1, StoryNode sn2) {

    }

    /**
     * Summarize stories by text rank algorithm.
     * @param sf Story forest to summarize.
     * @return Story forest with summarizes.
     */
    public StoryForest summarizeStories(StoryForest sf) {
        for (int i = 0; i < sf.storyTrees.size(); ++i) {
            StorySummaryGenerator.getStorySummaryByTextRank(sf.storyTrees.get(i));
        }

        return sf;
    }

}
