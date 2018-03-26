package edu.ualberta.storyteller.core.eventdetector;

import edu.ualberta.storyteller.core.parameter.*;
import edu.ualberta.storyteller.core.dataloader.*;
import edu.ualberta.storyteller.core.keywordorganizer.*;
import java.util.ArrayList;
import java.util.HashMap;

public class EventSplitterDocGraph extends EventSplitterDocRelation {

    /**
     * Default constructor.
     */
    public EventSplitterDocGraph() {
    }

    /**
     * Parametric constructor.
     * @param cons Configuration.
     */
    public EventSplitterDocGraph(Parameters cons) {
        parameters = cons;
    }

    @Override
    public ArrayList<Event> splitEvents(ArrayList<Event> events,
                                        HashMap<String, Double> DF,
                                        int docAmount) throws Exception {
        // split cluster according to document topic
        if (parameters.useDocumentTopic) {
            events = splitEventsByTopic(events);
        }

        events = splitEventsByDocGraph(events, DF, docAmount, parameters.model);

        if (parameters.useDocumentTitleCommonWords) {
            events = splitEventsByTitleCommonWords(events, parameters.minTitleCommonWordsSize,
                    parameters.stopwords);
        }

        return events;
    }

    public ArrayList<Event> splitEventsByDocGraph(ArrayList<Event> events,
                                                  HashMap<String, Double> DF,
                                                  int docAmount,
                                                  libsvm.svm_model model)
            throws Exception {
        ArrayList<Event> result = new ArrayList<>();
        KeywordGraph kg = new KeywordGraph(parameters);
        CommunityDetector cd = new CommunityDetector(parameters);

        for (Event e: events) {
            if (e.docs.size() > 2) {
                ArrayList<Event> splitEvents = new ArrayList<Event>();
                System.out.println("Start build doc graph.....");
                HashMap<String, KeywordNode> graphNodes = buildDocGraph(e, DF, docAmount, model);
                System.out.println("Start extract communities from doc graph.....");
                ArrayList<HashMap<String, KeywordNode>> communities = cd.detectCommunities(graphNodes);
                splitEvents = docGraphsToEvents(communities, e);
                result.addAll(splitEvents);
            } else {
                result.add(e);
            }
        }
        return result;
    }

    public HashMap<String, KeywordNode> buildDocGraph(Event e,
                              HashMap<String, Double> DF,
                              int docAmount,
                              libsvm.svm_model model) throws Exception {
        HashMap<String, KeywordNode> graphNodes = new HashMap<>();

        // add nodes
        for (Document d : e.docs.values()) {
            Keyword k = new Keyword(d.id, d.id, 1, 1);

            // create a new node or retrieve existing node given a keyword of a document
            KeywordNode n;
            if (graphNodes.containsKey(k.baseForm)) {
                n = graphNodes.get(k.baseForm);
            } else {
                n = new KeywordNode(k);
                graphNodes.put(k.baseForm, n);
            }

            // record the documents associated with this node
            n.keyword.documents.put(d.id, d);

            // update keyword's tf
            n.keyword.tf++;
        }

        // add edges
        //System.out.println("Add edges.....");
        //int i = 0;
        for (Document d1: e.docs.values()) {
            //System.out.println("Edge: " + i);
            //i++;
            for (Document d2: e.docs.values()) {
                if (d1.id.compareTo(d2.id) < 0) {
                    // check whether d1 and d2 is related
                    boolean isRelated = sameEvent(d1, d2, DF, docAmount, model);

                    // if related, create edge
                    if (isRelated) {
//
//                        // calc common title keywords number
//                        // TODO
//                        if (parameters.useDocumentTitleCommonWords) {
//                            String[] titleWords1 = d1.segTitle.split("\\s+");
//                            String[] titleWords2 = d2.segTitle.split("\\s+");
//                            HashSet<String> titleWordsSet1 = new HashSet<String>(Arrays.asList(titleWords1));
//                            HashSet<String> titleWordsSet2 = new HashSet<String>(Arrays.asList(titleWords2));
//                            titleWordsSet2.retainAll(titleWordsSet1);
//                            titleWordsSet2.removeAll(parameters.stopwords);
//                            if (titleWordsSet2.size() < parameters.minTitleCommonWordsSize)
//                                continue;
//                        }

                        KeywordNode n1 = getKeywordNodeByDoc(graphNodes, d1);
                        KeywordNode n2 = getKeywordNodeByDoc(graphNodes, d2);

                        String edgeId = KeywordEdge.getId(n1, n2);
                        if (!n1.edges.containsKey(edgeId)) {
                            KeywordEdge newEdge = new KeywordEdge(n1, n2, edgeId);
                            n1.edges.put(edgeId, newEdge);
                            n2.edges.put(edgeId, newEdge);
                        }
                    }
                }
            }
        }

        return graphNodes;
    }

    public KeywordNode getKeywordNodeByDoc(HashMap<String, KeywordNode> graphNodes, Document d) {
        KeywordNode result = null;
        for (KeywordNode kn: graphNodes.values()) {
            if (kn.keyword.baseForm.equals(d.id)) {
                result = kn;
                break;
            }
        }
        return result;
    }

    public ArrayList<Event> docGraphsToEvents(ArrayList<HashMap<String, KeywordNode>> communities, Event e) {
        ArrayList<Event> result = new ArrayList<Event>();

        for (int i = 0; i < communities.size(); ++i) {
            Event subEvent = new Event();
            subEvent.keyGraph = e.keyGraph;
            for (KeywordNode kn: communities.get(i).values()) {
                Document d = e.docs.get(kn.keyword.baseForm);
                subEvent.docs.put(d.id, d);
                subEvent.similarities.put(d.id, e.similarities.get(d.id));
            }
            result.add(subEvent);
        }
        return result;
    }

}
