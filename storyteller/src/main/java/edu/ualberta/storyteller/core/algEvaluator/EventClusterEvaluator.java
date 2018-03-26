package edu.ualberta.storyteller.core.algEvaluator;

import edu.ualberta.storyteller.core.svm.*;
import edu.ualberta.storyteller.core.parameter.*;
import edu.ualberta.storyteller.core.dataloader.*;
import edu.ualberta.storyteller.core.eventdetector.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by bangliu on 2017-05-19.
 */
public class EventClusterEvaluator {

    /**
     * Load corpus from event-story-cluster dataset.
     * <p>
     * @param inputFileName File name of cluster dataset.
     * @param fParameters File name of parameters.
     * @return Loaded corpus.
     * @throws Exception
     */
    public static Corpus loadTencentClusterDataset(String inputFileName, String fParameters) throws Exception {
        // open input file, each line is a Tencent news document
        File inputFile = new File(inputFileName);
        BufferedReader in = new BufferedReader(new FileReader(inputFile));
        Corpus corpus = new Corpus();
        Parameters parameters = new Parameters(fParameters);

        // read header line and get index of different columns
        System.out.println("Start loading: " + inputFileName);
        String header = in.readLine();
        String[] cols = header.split("\\|");
        int numCols = cols.length;
        System.out.println("numCols is " + numCols);
        int idxOfDocId = Arrays.asList(cols).indexOf("doc_id");
        int idxOfSegTitle = Arrays.asList(cols).indexOf("title");
        int idxOfSegContent = Arrays.asList(cols).indexOf("content");
        int idxOfTopic = Arrays.asList(cols).indexOf("category");
        int idxOfTimestamp = Arrays.asList(cols).indexOf("time");
        int idxOfAllKeywords = Arrays.asList(cols).indexOf("keywords");
        int idxOfMainKeywords = Arrays.asList(cols).indexOf("main_keywords");

        // read each line to create documents
        String line;
        int i = 0;
        while ((line = in.readLine()) != null) {
            try {
                // get document information from parsed line
                String[] tokens = line.split("\\|");

                // skip bad rows that contains some empty column value.
                if (tokens.length != numCols) {
                    // TODO: it is because some document's don't have main_keywords
                    // TODO: In the future, we shall replace the way to read different columns.
                    // TODO: as we may have \|\n or \|\|, which leads to column doean't match.
                    System.out.println("Column number doesn't match!");
                    continue;
                }

                // read different parts of a document from a line
                String id = tokens[idxOfDocId];
                String segTitle = tokens[idxOfSegTitle];
                String segContent = tokens[idxOfSegContent];
                String topic = tokens[idxOfTopic];
                String timestamp = tokens[idxOfTimestamp];
                String[] allKeywords = tokens[idxOfAllKeywords].split(",");
                String[] mainKeywords = tokens[idxOfMainKeywords].split(",");

                // create document
                Document d = new Document(id);
                d.segTitle = segTitle;
                d.title = segTitle.replaceAll("\\s+","");
                d.segContent = segContent;
                d.topic = topic;
                d.publishTime = new Timestamp((long) Double.parseDouble(timestamp));
                d.language = parameters.language;

                String[] kws = d.segTitle.split("\\s+");
                d.titleKeywords = new HashSet<>(Arrays.asList(kws));
                d.titleKeywords.removeAll(parameters.stopwords);

                d.mainKeywords = new HashSet<>(Arrays.asList(mainKeywords));

                // create document's keywords
                String[] words = segContent.split("\\s+");
                for (int j = 0; j < words.length; ++j) {
                    // handle different words
                    double tf = 0;
                    if (Arrays.asList(mainKeywords).contains(words[j])) {
                        tf = 1 * parameters.boostRateMainKeyword;
                    } else if (Arrays.asList(allKeywords).contains(words[j])) {
                        tf = 1 * parameters.boostRateNormalKeyword;
                    } else {
                        tf = 1 * parameters.boostRateNormalWord;
                    }

                    // add the word token as document's keyword or update existing keyword's tf
                    if (tf > 0 && words[j].length() > 0 && !parameters.stopwords.contains(words[j])) {
                        if (!d.keywords.containsKey(words[j])) {
                            d.keywords.put(words[j], new Keyword(words[j], words[j], tf, 1));
                        } else {
                            d.keywords.get(words[j]).tf += tf;
                        }
                    }
                }

                // add new document to docs
                corpus.docs.put(id, d);

                if (++i % 10000 == 0) {
                    System.out.println(i + " documents are loaded.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        in.close();
        System.out.println(corpus.docs.size() + " documents are loaded.");

        // filter documents with not enough keywords
        corpus.filterDocsByNumKeywords(parameters.minDocKeywordSize);

        // count each keyword' df and save into the hash map DF
        corpus.updateDF();

        // print load information
        System.out.println(corpus.docs.size()
                + " documents remained after filtering small documents (Documents that have less than "
                + parameters.minDocKeywordSize
                + " keywords).");

        return corpus;
    }

    /**
     * Given cluster dataset and corresponding document pair dataset, generate SVM training feature file.
     * <p>
     * @param fDocIdPair Document pair dataset file.
     * @param fData Cluster dataset file.
     * @param fModelTrain Output libSVM format training file.
     * @param fParameters Parameter file.
     * @throws Exception
     */
    public static void getSVMTrainFeatures(String fDocIdPair, String fData, String fModelTrain, String fParameters)
            throws  Exception {
        // load English news data corpus
        Corpus corpus = loadTencentClusterDataset(fData, fParameters);

        // load label|doc1_id|doc2_id file and create SVM train feature file
        File docIdPairFile = new File(fDocIdPair);
        BufferedReader in = new BufferedReader(new FileReader(docIdPairFile));

        String header = in.readLine();
        String[] cols = header.split("\\|");
        int numCols = cols.length;
        int idxOfLabel = Arrays.asList(cols).indexOf("label");
        int idxOfID1 = Arrays.asList(cols).indexOf("doc_id1");
        int idxOfID2 = Arrays.asList(cols).indexOf("doc_id2");

        // read each line to create documents
        Parameters parameters = new Parameters(fParameters);
        EventSplitterDocRelation eventSplitter = new EventSplitterDocRelation(parameters);
        String fModelTrainData = fModelTrain;
        PrintStream outModelTrainData = new PrintStream(fModelTrainData);

        String line;
        while ((line = in.readLine()) != null) {
            try {
                String[] tokens = line.split("\\|");
                if (tokens.length != numCols) {
                    continue;
                }

                String label = tokens[idxOfLabel];
                String ID1 = tokens[idxOfID1];
                String ID2 = tokens[idxOfID2];

                // get doc pair and print SVM feature
                Document d1 = corpus.docs.get(ID1);
                Document d2 = corpus.docs.get(ID2);
                if (d1 == null || d2 == null) {
                    continue; // some document may be filtered during the data loading process.
                }

                HashMap<String, Double> features = eventSplitter.docPairFeature(d1, d2, corpus.DF, corpus.docs.size());
                outModelTrainData.println(label + " " + eventSplitter.formatSameEventFeature(features));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        in.close();
        outModelTrainData.close();
    }

    /**
     * Train SVM model given training feature file.
     * <p>
     * @param fModelTrain Training feature file.
     * @param fModel Output model file.
     * @throws Exception
     */
    public static void trainSVM(String fModelTrain, String fModel) throws Exception {
        // train parameters setting
        String[] arg = {"-h", "0", "-w1", "1.1", "-w-1", "1", fModelTrain, fModel};
        SVM.train(arg);
    }

    /**
     * Test SVM model accuracy given test feature file.
     * <p>
     * @param fTestData Test feature file.
     * @param fResult Output test label result.
     * @param fModel The SVM model to test.
     * @throws Exception
     */
    public static void testSVM(String fTestData, String fResult, String fModel) throws Exception {
        // test parameters setting
        String[] parg = {"-b", "0", fTestData, fModel, fResult};
        SVM.predict(parg);
    }

    /**
     * Given cluster dataset and corresponding document pair dataset, train and test new SVM model.
     * <p>
     * @param fTrainData Input cluster dataset.
     * @param fDocIdPair Input corresponding document pair dataset.
     * @param fModelTrain Generated training feature file.
     * @param fModel Generated svm model file.
     * @param fTestResult Generated test result file.
     * @param fParameters Parameter file.
     * @throws Exception
     */
    public static void trainDocPairRelationModel(String fTrainData, String fDocIdPair, String fModelTrain,
                                                 String fModel, String fTestResult, String fParameters)
            throws Exception {
        getSVMTrainFeatures(fDocIdPair, fTrainData, fModelTrain, fParameters);
        trainSVM(fModelTrain, fModel);

        // test SVM model
        String fTestData = fModelTrain;
        testSVM(fTestData, fTestResult, fModel);
    }

    /**
     * OnlineStoryTeller function to run tests.
     * @param args Program arguments.
     * @throws Exception
     */
    public static void main(String args[]) throws Exception{
        // Train SVM model file for Tencent news cluster dataset.
        // parameter settings
        String fParameters = "conf/ChineseNewsParameters.txt";
        String fTrainData = "resources/event-story-cluster/event_story_cluster.txt";
        String fDocIdPair = "resources/event-story-cluster/same_event_doc_pair.txt";
        String fModelTrain = "resources/event-story-cluster/same_event_doc_pair.svm_feature.txt";
        String fModel = "model/new_svm_model.Chinese-news";
        String fTestFeatureData = fModelTrain;
        String fTestResult = fModelTrain + ".result-test.txt";

        // train and test SVM model for same event
        trainDocPairRelationModel(fTrainData, fDocIdPair, fModelTrain, fModel, fTestResult, fParameters);
    }

}
