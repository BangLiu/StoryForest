package edu.ualberta.storyteller.storylayer;

import edu.ualberta.storyteller.core.parameter.*;
import edu.ualberta.storyteller.core.dataloader.*;
import edu.ualberta.storyteller.core.eventdetector.*;
import edu.ualberta.storyteller.core.storymaker.*;
import edu.ualberta.storyteller.core.keywordorganizer.*;
import edu.ualberta.storyteller.core.util.TimeUtils;
import org.apache.commons.lang3.SerializationUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.sql.Timestamp;

/**
 * This class implements the procedures for online system.
 */
public class StoryLayer {

    public static void printStoryForestToCSV(StoryForest sf, String fOutput, int minStorySize, int maxStaleAge)
            throws Exception {
        PrintStream out = new PrintStream(new File(fOutput));
        for (StoryTree st: sf.storyTrees) {
            if (st.getNumberOfNodes() > minStorySize && st.staleAge <= maxStaleAge) {
                out.println(serializeStoryTree(st));
            }
        }
    }

    public static String serializeStoryTree(StoryTree st) {
        String result;

        String id = st.id;
        String startTime = TimeUtils.convertTime(st.getStartTimestamp());
        String endTime = TimeUtils.convertTime(st.getEndTimestamp());
        String summary = st.summary.replace("\n", "\t");

        String nodes = "";
        ArrayList<StoryNode> sns = st.build(st.root, TreeTraversalOrderEnum.PRE_ORDER);
        for (StoryNode sn: sns) {
            if (!sn.isRoot()) {
                nodes += "<node> " + serializeStoryNode(sn) + " </node>";
            }
        }

        double hotness = st.hotness;

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String update_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);

        StringBuilder sb = new StringBuilder();
        for (KeywordNode n : st.keyGraph.values()) {
            sb.append(n.keyword.word.replaceAll("[,'\"]", " ") + "\t");
        }
        String keywords = sb.toString();

        result = id + "|" + summary + "|" + keywords + "|" +
                startTime + "|" + endTime + "|" + nodes + "|" +
                hotness + "|" + update_time;
        return result;
    }

    public static String serializeStoryNode(StoryNode sn) {
        String result = "";
        result += "<nodeID> " + sn.id + " </nodeID>";
        result += "<parentNodeID> " + sn.parent.id + " </parentNodeID>";
        result += "<event> " + serializeEvent(sn.e) + " </event>";
        return result;
    }

    public static String serializeEvent(Event e) {
        String result = "";
        result += "<startTime> " + TimeUtils.convertTime(e.getStartTimestamp()) + " </startTime>";
        result += "<endTime> " + TimeUtils.convertTime(e.getEndTimestamp()) + " </endTime>";
        result += "<hotness> " + e.hotness + " </hotness>";
        for (Document d: e.docs.values()) {
            result += "<doc> " + serializeDocument(d) + " </doc>";
        }
        return result;
    }

    public static String serializeDocument(Document d) {
        String result = "";
        result += "<docID> " + d.id + " </docID>";
        result += "<publishDate> " + d.publishTime.toString() + " </publishDate>";
        result += "<topic> " + d.topic + " </topic>";
        result += "<title> " + d.segTitle + " </title>";
        return result;
    }

    /**
     * Show sample usage of story layer.
     * @param args Program arguments.
     * @throws Exception
     */
    public static void main(String args[]) throws Exception{
        // load params
        ArrayList<String> fNewsNames = new ArrayList<>();
        fNewsNames.add("../test_data/2017-07-19.txt");
        fNewsNames.add("../test_data/2017-07-20.txt");
        fNewsNames.add("../test_data/2017-07-21.txt");
        fNewsNames.add("../test_data/2017-07-22.txt");
        String fParameters = "conf/ChineseNewsParameters.txt";
        String fHistoryCorpus = "../test_data/history_corpus.ser";
        String fHistoryStoryForest = "../test_data/history_sf.ser";
        String fOutputEvents = "../test_data/events_sf.txt";
        String fOutputStories = "../test_data/stories.txt";
        String fOutputStoriesCSV = "../test_data/stories.csv";

        // initialization
        Parameters parameters = new Parameters(fParameters);
        DataLoader loader = new DataLoader(parameters);

        // run day by day
        for (String fNews: fNewsNames) {
            // load corpus
            Corpus corpus = loader.loadCorpus(fNews);

            // filter corpus by 1st topic
            HashSet<String> filterTopics = new HashSet<>();
            filterTopics.add("141");
            filterTopics.add("142");
            filterTopics.add("116");
            filterTopics.add("117");
            filterTopics.add("124");
            ArrayList<String> toRemove = new ArrayList<>();
            for (String key : corpus.docs.keySet()) {
                Document d = corpus.docs.get(key);
                if (filterTopics.contains(d.topic)) {
                    toRemove.add(key);
                }
            }
            for (String key : toRemove) {
                corpus.docs.remove(key);
            }
            System.out.println("Corpus size is " + corpus.docs.size() + " after filter by topics.");

            // load historical corpus
            Corpus historicalCorpus = new Corpus();
            File f = new File(fHistoryCorpus);
            if (f.exists() && !f.isDirectory()) {
                historicalCorpus = SerializationUtils.deserialize(new FileInputStream(fHistoryCorpus));
            }

            // merge new and historical corpus
            historicalCorpus.merge(corpus);

            // filter and update historical corpus
            historicalCorpus.filterDocsByTime(TimeUtils.addDays(historicalCorpus.endTime(), -parameters.historyLength),
                    historicalCorpus.endTime());

            // extract events
            EventDetector eventDetector = new EventDetector(parameters);
            ArrayList<Event> events = eventDetector.extractEventsFromCorpus(historicalCorpus);

            // output new events
            File outputEventFile = new File(fOutputEvents);
            String eventFileFolder = outputEventFile.getAbsoluteFile().getParent();
            String eventFilePath = eventFileFolder + File.separator + fOutputEvents;
            PrintStream outputEventStream = new PrintStream(eventFilePath);
            EventDetector.printTopics(events, outputEventStream);
            outputEventStream.close();

            // load historical story forest
            StoryForest historicalStoryForest = new StoryForest();
            File fs = new File(fHistoryStoryForest);
            if (fs.exists() && !fs.isDirectory()) {
                historicalStoryForest = SerializationUtils.deserialize(new FileInputStream(fHistoryStoryForest));
            }

            int maxId = 0;
            for (StoryTree st : historicalStoryForest.storyTrees) {
                int id = Integer.parseInt(st.id);
                if (id > maxId) {
                    maxId = id;
                }
            }
            StoryTree.MAX_ID = ++maxId;

            // update stories
            // TODO  separate corpus etc. with story forest
            historicalStoryForest.corpus.merge(corpus);
            Corpus.mergeDF(historicalStoryForest.cumulativeDF, corpus.DF);
            historicalStoryForest.cumulativeDocAmount += corpus.docs.size();

            StoryMaker sm = new StoryMaker(parameters);
            if (events.size() > 0) {
                historicalStoryForest = sm.updateStoriesByEvents(historicalStoryForest, events);
                historicalStoryForest = sm.summarizeStories(historicalStoryForest);
            }

//        historicalStoryForest.corpus.filterDocsByTime(
//                TimeUtils.addDays(historicalStoryForest.corpus.endTime(), -parameters.historyLength),
//                historicalStoryForest.corpus.endTime());
//
//        Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.DAY_OF_YEAR, -5);
//        long fiveDaysAgo = cal.getTimeInMillis();
//        historicalStoryForest.filterStoryTreesByTime(new Timestamp(fiveDaysAgo));

            // output new stories
            File outputStoryFile = new File(fOutputStories);
            String storyFileFolder = outputStoryFile.getAbsoluteFile().getParent();
            String storyFilePath = storyFileFolder + File.separator + fOutputStories;
            PrintStream outputStoryStream = new PrintStream(storyFilePath);
            historicalStoryForest.print(outputStoryStream, 1, 0, "tree");
            outputStoryStream.close();

            // output news stories in the csv format
            printStoryForestToCSV(historicalStoryForest, fOutputStoriesCSV, 2, 0);

            // save historical corpus
            FileOutputStream outCorpus = new FileOutputStream(fHistoryCorpus);
            SerializationUtils.serialize(historicalCorpus, outCorpus);
            outCorpus.close();

            // save historical story forest
            FileOutputStream outStory = new FileOutputStream(fHistoryStoryForest);
            SerializationUtils.serialize(historicalStoryForest, outStory);
            outStory.close();
        }
    }

}
