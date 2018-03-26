package edu.ualberta.storyteller.core.eventdetector;

import edu.ualberta.storyteller.core.parameter.*;
import edu.ualberta.storyteller.core.dataloader.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by bangliu on 2017-05-15.
 */
public class EventSplitterRule {

    /**
     * Configuration.
     */
    public Parameters parameters;

    /**
     * Default constructor.
     */
    public EventSplitterRule() {
    }

    /**
     * Default constructor.
     */
    public EventSplitterRule(Parameters cons) {
        parameters = cons;
    }

    /**
     * This is used to further check a event is a pure event.
     * Different strategies are combined to split a event into final events.
     * <p>
     * @param events Input events.
     * @return Refined events.
     */
    ArrayList<Event> splitEvents(ArrayList<Event> events) throws Exception {
        // split cluster according to document topic
        if (parameters.useDocumentTopic) {
            events = splitEventsByTopic(events);
        }

        // split cluster according to title similarity
        if (parameters.useDocumentTitleCommonWords) {
            events = splitEventsByTitleCommonWords(events, parameters.minTitleCommonWordsSize,
                    parameters.stopwords);
//            events = splitEventsByTitleCommonWordsPercent(events, parameters.minTitleCommonWordsSize,
//                    parameters.minTitleCommonWordsPercent, parameters.stopwords);
        }

        return events;
    }

    /**
     * Split a document cluster into multiple clusters based on each document's topic.
     * <p>
     * @param dcs An input array of document clusters.
     * @return An array list of document clusters.
     */
    public ArrayList<Event> splitEventsByTopic(ArrayList<Event> dcs) {
        // initialize document clusters
        ArrayList<Event> splitDcs = new ArrayList<Event>();

        for (Event dc: dcs) {
            // get each topic's document amount
            HashMap<String, Integer> dcTopicDistribution = new HashMap<String, Integer>();
            for (String docId : dc.docs.keySet()) {
                Document d = dc.docs.get(docId);
                int count = dcTopicDistribution.containsKey(d.topic) ? dcTopicDistribution.get(d.topic) : 0;
                dcTopicDistribution.put(d.topic, count + 1);
            }

            // split cluster according to topic
            for (String docTopic : dcTopicDistribution.keySet()) {
                Event subDc = new Event();
                subDc.keyGraph = dc.keyGraph;
                for (String docId : dc.docs.keySet()) {
                    Document d = dc.docs.get(docId);
                    if (d.topic.equals(docTopic)) {
                        subDc.docs.put(d.id, d);
                        subDc.similarities.put(d.id, dc.similarities.get(d.id));
                    }
                }
                splitDcs.add(subDc);
            }
        }

        return splitDcs;
    }

    /**
     * Split a document cluster into multiple clusters based on each document's segmented title.
     * <p>
     * If two document's title contains more than n words, they will be considered the same event document.
     * <p>
     * @param dcs An input array of document clusters.
     * @param n Threshold of common words in title for same event document.
     * @param stopwords The set of stopwords.
     * @return An array list of document clusters.
     */
    public ArrayList<Event> splitEventsByTitleCommonWords(ArrayList<Event> dcs,
                                                          int n,
                                                          HashSet<String> stopwords) {
        // initialize document clusters
        ArrayList<Event> splitDcs = new ArrayList<Event>();

        for (Event dc: dcs) {
            // get each document's title words set
            HashMap<String, HashSet<String>> dcTitleWord = new HashMap<String, HashSet<String>>();
            for (String docId : dc.docs.keySet()) {
                Document d = dc.docs.get(docId);
                String[] titleWords = d.segTitle.split("\\s+");
                HashSet<String> titleWordsSet = new HashSet<String>(Arrays.asList(titleWords));
                dcTitleWord.put(docId, titleWordsSet);
            }

            // split cluster according to number of common title words
            ArrayList<HashMap<String, HashSet<String>>> tcs = new ArrayList<HashMap<String, HashSet<String>>>();
            while (dcTitleWord.size() > 0) {
                // title cluster
                HashMap<String, HashSet<String>> tc = new HashMap<>();

                // put first doc
                Map.Entry<String, HashSet<String>> firstEntry = dcTitleWord.entrySet().iterator().next();
                String firstDocId = firstEntry.getKey();
                tc.put(firstDocId, dcTitleWord.get(firstDocId));

                dcTitleWord.remove(firstDocId);

                // put the rest
                boolean tcChanged;
                do {
                    tcChanged = false;
                    ArrayList<String> toRemove = new ArrayList<String>();
                    for (String id : dcTitleWord.keySet()) {
                        for (String docId : tc.keySet()) {
                            // calc common title keywords number
                            HashSet<String> val = dcTitleWord.get(id);
                            val.retainAll(tc.get(docId));
                            val.removeAll(stopwords);
                            if (val.size() >= n) {
                                tc.put(id, dcTitleWord.get(id));
                                toRemove.add(id);
                                tcChanged = true;
                                break;
                            }
                        }
                    }

                    for (String id : toRemove) {
                        dcTitleWord.remove(id);
                    }
                } while (dcTitleWord.size() > 0 && tcChanged);

                // create subDc according to tc and put into dcs
                Event subDc = new Event();
                subDc.keyGraph = dc.keyGraph;
                for (String docId : tc.keySet()) {
                    Document d = dc.docs.get(docId);
                    subDc.docs.put(d.id, d);
                    subDc.similarities.put(d.id, dc.similarities.get(d.id));
                }

                splitDcs.add(subDc);
            }
        }

        return splitDcs;
    }

    public ArrayList<Event> splitEventsByTitleCommonWordsPercent(ArrayList<Event> dcs,
                                                                 int n, double pCommon,
                                                                 HashSet<String> stopwords) {
        // initialize document clusters
        ArrayList<Event> splitDcs = new ArrayList<Event>();

        for (Event dc: dcs) {
            // get each document's title words set
            HashMap<String, HashSet<String>> dcTitleWord = new HashMap<String, HashSet<String>>();
            for (String docId : dc.docs.keySet()) {
                Document d = dc.docs.get(docId);
                String[] titleWords = d.segTitle.split("\\s+");
                HashSet<String> titleWordsSet = new HashSet<String>(Arrays.asList(titleWords));
                dcTitleWord.put(docId, titleWordsSet);
            }

            // split cluster according to common title words
            ArrayList<HashMap<String, HashSet<String>>> tcs = new ArrayList<HashMap<String, HashSet<String>>>();
            while (dcTitleWord.size() > 0) {
                HashMap<String, HashSet<String>> tc = new HashMap<String, HashSet<String>>();  // title cluster

                // put first doc
                Map.Entry<String, HashSet<String>> firstEntry = dcTitleWord.entrySet().iterator().next();
                String firstDocId = firstEntry.getKey();
                tc.put(firstDocId, dcTitleWord.get(firstDocId));

                dcTitleWord.remove(firstDocId);

                // put the rest
                boolean tcChanged;
                do {
                    tcChanged = false;
                    ArrayList<String> toRemove = new ArrayList<String>();
                    for (String id : dcTitleWord.keySet()) {
                        for (String docId : tc.keySet()) {
                            // calc common title keywords number
                            HashSet<String> val = dcTitleWord.get(id);
                            val.retainAll(tc.get(docId));
                            val.removeAll(stopwords);

                            double nCommon = val.size();

                            // TODO:!!!!!! here when we calculate percent, we haven't delete stop words!
                            // Thus, current we set 0.18. After we remove, it should be bigger.
                            // We still have to test a good parameter.
                            //HashSet<String> title1 = dcTitleWord.get(id);
                            //HashSet<String> title2 = tc.get(docId);
                            //title1.removeAll(stopwords);
                            //title2.removeAll(stopwords);

                            double n1 = dcTitleWord.get(id).size();
                            double n2 = tc.get(docId).size();

                            double percentOfCommon = 0;
                            if (nCommon > 0) {
                                percentOfCommon = nCommon * nCommon / (n1 * n2);
                            }

                            if (val.size() >= n && percentOfCommon >= pCommon) {
                                tc.put(id, dcTitleWord.get(id));
                                toRemove.add(id);
                                tcChanged = true;
                                break;
                            }
                        }
                    }

                    for (String id : toRemove) {
                        dcTitleWord.remove(id);
                    }
                } while (dcTitleWord.size() > 0 && tcChanged);

                // create subDc according to tc and put into dcs
                Event subDc = new Event();
                subDc.keyGraph = dc.keyGraph;
                for (String docId : tc.keySet()) {
                    Document d = dc.docs.get(docId);
                    subDc.docs.put(d.id, d);
                    subDc.similarities.put(d.id, dc.similarities.get(d.id));
                }

                splitDcs.add(subDc);
            }
        }

        return splitDcs;
    }

}
