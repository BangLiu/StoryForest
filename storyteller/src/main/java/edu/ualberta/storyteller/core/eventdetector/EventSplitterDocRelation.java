package edu.ualberta.storyteller.core.eventdetector;

import edu.ualberta.storyteller.core.util.StringUtils;
import edu.ualberta.storyteller.core.parameter.Parameters;
import edu.ualberta.storyteller.core.dataloader.*;
import edu.ualberta.storyteller.core.svm.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by bangliu on 2017-05-15.
 */
public class EventSplitterDocRelation extends EventSplitterRule {

    /**
     * Default constructor.
     */
    public EventSplitterDocRelation() {
    }

    /**
     * Default constructor.
     */
    public EventSplitterDocRelation(Parameters cons) {
        parameters = cons;
    }

    public ArrayList<Event> splitEvents(ArrayList<Event> events,
                                        HashMap<String, Double> DF,
                                        int docAmount) throws Exception {
        // split cluster according to document topic
        if (parameters.useDocumentTopic) {
            events = splitEventsByTopic(events);
        }

        events = splitEventsByDocRelation(events, DF, docAmount, parameters.model);

        if (parameters.useDocumentTitleCommonWords) {
            events = splitEventsByTitleCommonWords(events, parameters.minTitleCommonWordsSize,
                    parameters.stopwords);
        }

        return events;
    }

    public ArrayList<Event> splitEventsByDocRelation(ArrayList<Event> events,
                                                     HashMap<String, Double> DF,
                                                     int docAmount,
                                                     libsvm.svm_model model)
            throws Exception {
        // initialize document clusters
        ArrayList<Event> result = new ArrayList<>();

        for (Event e: events) {
            if (e.docs.size() >= 2) {
                ArrayList<Event> splitEvents = new ArrayList<>();

                ArrayList<String> docKeys = new ArrayList<>(e.docs.keySet());
                ArrayList<String> processedDocKeys = new ArrayList<>();
                for (int i = 0; i < docKeys.size(); ++i) {

                    if (processedDocKeys.contains(docKeys.get(i))) {
                        continue;
                    } else {
                        processedDocKeys.add(docKeys.get(i));
                        Document d1 = e.docs.get(docKeys.get(i));
                        Event subEvent = new Event();
                        subEvent.keyGraph = e.keyGraph;
                        subEvent.docs.put(d1.id, d1);

                        for (int j = i + 1; j < docKeys.size(); ++j) {
                            if (processedDocKeys.contains(docKeys.get(j))) {
                                continue;
                            } else {
                                Document d2 = e.docs.get(docKeys.get(j));
                                boolean isSameEvent = sameEvent(d1, d2, DF, docAmount, model);
                                if (!isSameEvent) {
                                    continue;
                                } else {
                                    subEvent.docs.put(d2.id, d2);
                                    subEvent.similarities.put(d2.id, e.similarities.get(d2.id));
                                    processedDocKeys.add(docKeys.get(j));
                                }
                            }
                        }

                        splitEvents.add(subEvent);
                    }
                }

                result.addAll(splitEvents);
            } else {
                result.add(e);
            }
        }

        return result;
    }

    public HashMap<String, Double> docPairFeature(Document d1, Document d2, HashMap<String, Double> DF, int docAmount) {
        HashMap<String, Double> feature = new HashMap<>();
        if (parameters.dataType.equals("ChineseNews")) {
            feature = docPairFeatureChinese(d1, d2, DF, docAmount);
        } else if (parameters.dataType.equals("EnglishNews")) {
            feature = docPairFeatureEnglish(d1, d2, DF, docAmount);
        }  //TODO other dataType
        return feature;
    }

    public HashMap<String, Double> docPairFeatureChinese(Document d1, Document d2, HashMap<String, Double> DF, int docAmount) {
        HashMap<String, Double> feature = new HashMap<>();

        double fContentKeywordsTFIDFSim = FeatureExtractor.cosineSimilarityByTFIDF(d1, d2, DF, docAmount);
        feature.put("ContentKeywordsTFIDFSim", fContentKeywordsTFIDFSim);

        double fContentKeywordsTFSim = FeatureExtractor.cosineSimilarityByTF(d1, d2);
        feature.put("ContentKeywordsTFSim", fContentKeywordsTFSim);

        double fContentTFSim = FeatureExtractor.cosineSimilarityByTF(d1.segContent, d2.segContent, parameters.stopwords);
        feature.put("ContentTFSim", fContentTFSim);

        double fFirst1SentenceTFSim = FeatureExtractor.firstNSentencesCosineSimilarityByTF(d1, d2, 1, parameters.stopwords, parameters.language);
        feature.put("First1SentenceTFSim", fFirst1SentenceTFSim);

        double fFirst2SentenceTFSim = FeatureExtractor.firstNSentencesCosineSimilarityByTF(d1, d2, 2, parameters.stopwords, parameters.language);
        feature.put("First2SentenceTFSim", fFirst2SentenceTFSim);

        double fFirst3SentenceTFSim = FeatureExtractor.firstNSentencesCosineSimilarityByTF(d1, d2, 3, parameters.stopwords, parameters.language);
        feature.put("First3SentenceTFSim", fFirst3SentenceTFSim);

        double fTitleTFSim = FeatureExtractor.cosineSimilarityByTF(d1.segTitle, d2.segTitle, parameters.stopwords);
        feature.put("TitleTFSim", fTitleTFSim);

        double fTitleCommonNum = FeatureExtractor.numCommonTitleKeyword(d1, d2);
        feature.put("TitleCommonNum", fTitleCommonNum);

        double fTitleCommonPercent = FeatureExtractor.percentCommonTitleKeyword(d1, d2);
        feature.put("TitleCommonPercent", fTitleCommonPercent);

        double fTitleLevenshteinDistance = StringUtils.calcLevenshteinDistance(d1.title, d2.title);
        feature.put("TitleLevenshteinDistance", fTitleLevenshteinDistance);

        double fTitleNormalizedLevenshteinDistance = StringUtils.calcNormalizedLevenshteinDistance(d1.title, d2.title);
        feature.put("TitleNormalizedLevenshteinDistance", fTitleNormalizedLevenshteinDistance);

        double fTitleDamerauLevenshteinDistance = StringUtils.calcDamerauLevenshteinDistance(d1.title, d2.title);
        feature.put("TitleDamerauLevenshteinDistance", fTitleDamerauLevenshteinDistance);

        double fTitleJaroWinklerSimilarity = StringUtils.calcJaroWinklerSimilarity(d1.title, d2.title);
        feature.put("TitleJaroWinklerSimilarity", fTitleJaroWinklerSimilarity);

        double fTitleLCSDistance = StringUtils.calcLCSDistance(d1.title, d2.title);
        feature.put("TitleLCSDistance", fTitleLCSDistance);

        double fTitleMetricLCSDistance = StringUtils.calcMetricLCSDistance(d1.title, d2.title);
        feature.put("TitleMetricLCSDistance", fTitleMetricLCSDistance);

        double fTitleNGramDistance = StringUtils.calcNGramDistance(d1.title, d2.title, 2);
        feature.put("TitleNGramDistance", fTitleNGramDistance);

        double fTitleQGramDistance = StringUtils.calcQGramDistance(d1.title, d2.title, 2);
        feature.put("TitleQGramDistance", fTitleQGramDistance);

        double fDocTopicType = Double.parseDouble(d1.topic);
        feature.put("DocTopicType", fDocTopicType);

        return feature;
    }

    public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public HashMap<String, Double> docPairFeatureEnglish(Document d1, Document d2, HashMap<String, Double> DF, int docAmount) {
        HashMap<String, Double> feature = new HashMap<>();

        double fContentTFSim = FeatureExtractor.cosineSimilarityByTF(d1.segContent, d2.segContent, parameters.stopwords);
        feature.put("ContentTFSim", fContentTFSim);

        double fContentKeywordsTFIDFSim = FeatureExtractor.cosineSimilarityByTFIDF(d1, d2, DF, docAmount);
        feature.put("ContentKeywordsTFIDFSim", fContentKeywordsTFIDFSim);

        double fContentKeywordsTFSim = FeatureExtractor.cosineSimilarityByTF(d1, d2);
        feature.put("ContentKeywordsTFSim", fContentKeywordsTFSim);

        String[] lda1 = d1.lda.split(",");
        double[] lda1_double = Arrays.stream(lda1).mapToDouble(Double::parseDouble).toArray();
        String[] lda2 = d2.lda.split(",");
        double[] lda2_double = Arrays.stream(lda2).mapToDouble(Double::parseDouble).toArray();

//        for (int i = 0; i < lda1_double.length; ++i) {
//            feature.put(Integer.toString(i), Math.abs(lda1_double[i] - lda2_double[i]));
//        }

        double fLDAcosineSim = cosineSimilarity(lda1_double, lda2_double);
        feature.put("LDAcosineSim", fLDAcosineSim);

        return feature;
    }


    public String formatSameEventFeature(HashMap<String, Double> features) {
        String result = "";
        if (parameters.dataType.equals("ChineseNews")) {
            result = formatSameEventFeatureChinese(features);
        } else if (parameters.dataType.equals("EnglishNews")) {
            result = formatSameEventFeatureEnglish(features);
        }  //TODO other dataType
        return result;
    }


    public String formatSameEventFeatureChinese(HashMap<String, Double> features) {
        HashMap<Double, Integer> docTopicType2FeatureIdx = new HashMap<>();
        docTopicType2FeatureIdx.put(100.0, 17);
        docTopicType2FeatureIdx.put(101.0, 18);
        docTopicType2FeatureIdx.put(103.0, 19);
        docTopicType2FeatureIdx.put(104.0, 20);
        docTopicType2FeatureIdx.put(105.0, 21);
        docTopicType2FeatureIdx.put(106.0, 22);
        docTopicType2FeatureIdx.put(107.0, 23);
        docTopicType2FeatureIdx.put(108.0, 24);
        docTopicType2FeatureIdx.put(109.0, 25);
        docTopicType2FeatureIdx.put(110.0, 26);
        docTopicType2FeatureIdx.put(111.0, 27);
        docTopicType2FeatureIdx.put(112.0, 28);
        docTopicType2FeatureIdx.put(113.0, 29);
        docTopicType2FeatureIdx.put(114.0, 30);
        docTopicType2FeatureIdx.put(115.0, 31);
        docTopicType2FeatureIdx.put(116.0, 32);
        docTopicType2FeatureIdx.put(117.0, 33);
        docTopicType2FeatureIdx.put(118.0, 34);
        docTopicType2FeatureIdx.put(119.0, 35);
        docTopicType2FeatureIdx.put(121.0, 36);
        docTopicType2FeatureIdx.put(122.0, 37);
        docTopicType2FeatureIdx.put(123.0, 38);
        docTopicType2FeatureIdx.put(124.0, 39);
        docTopicType2FeatureIdx.put(125.0, 40);
        docTopicType2FeatureIdx.put(126.0, 41);
        docTopicType2FeatureIdx.put(128.0, 42);
        docTopicType2FeatureIdx.put(141.0, 43);
        docTopicType2FeatureIdx.put(142.0, 44);
        docTopicType2FeatureIdx.put(143.0, 45);
        docTopicType2FeatureIdx.put(145.0, 46);
        String result = "0:" + String.format("%.3f", features.get("ContentKeywordsTFIDFSim")) + " " +
                "1:" + String.format("%.3f", features.get("ContentKeywordsTFSim")) + " " +
                "2:" + String.format("%.3f", features.get("ContentTFSim")) + " " +
                "3:" + String.format("%.3f", features.get("First1SentenceTFSim")) + " " +
                "4:" + String.format("%.3f", features.get("First2SentenceTFSim")) + " " +
                "5:" + String.format("%.3f", features.get("First3SentenceTFSim")) + " " +
                "6:" + String.format("%.3f", features.get("TitleTFSim")) + " " +
                "7:" + features.get("TitleCommonNum") + " " +
                "8:" + String.format("%.3f", features.get("TitleCommonPercent")) + " " +
                "9:" + features.get("TitleLevenshteinDistance") + " " +
                "10:" + String.format("%.3f", features.get("TitleNormalizedLevenshteinDistance")) + " " +
                "11:" + features.get("TitleDamerauLevenshteinDistance") + " " +
                "12:" + String.format("%.3f", features.get("TitleJaroWinklerSimilarity")) + " " +
                "13:" + features.get("TitleLCSDistance") + " " +
                "14:" + String.format("%.3f", features.get("TitleMetricLCSDistance")) + " " +
                "15:" + String.format("%.3f", features.get("TitleNGramDistance")) + " " +
                "16:" + features.get("TitleQGramDistance");

        // Doc topic type 0 means cannot classify topic.
        if (features.get("DocTopicType") != 0) {
            String topicFeature = docTopicType2FeatureIdx.get(features.get("DocTopicType")) + ":1.0";
            result = result + " " + topicFeature;
        }

        return result;
    }


    public String formatSameEventFeatureEnglish(HashMap<String, Double> features) {
        String result = "";

//        // LDA difference and other features
//        for (int i=0; i < 1000; ++i) {  //TODO LDA vector size 1000
//            result += i + ":" + features.get(Integer.toString(i)) + " ";
//        }
//        result += "1000:" + String.format("%.3f", features.get("ContentTFSim")) + " " +
//                  "1001:" + String.format("%.3f", features.get("ContentKeywordsTFIDFSim")) + " " +
//                  "1002:" + String.format("%.3f", features.get("ContentKeywordsTFSim"));

        result = "0:" + String.format("%.3f", features.get("LDAcosineSim")) + " " +
                 "1:" + String.format("%.3f", features.get("ContentTFSim")) + " " +
                 "2:" + String.format("%.3f", features.get("ContentKeywordsTFIDFSim")) + " " +
                 "3:" + String.format("%.3f", features.get("ContentKeywordsTFSim"));
        return result;
    }


    public boolean sameEvent(Document d1,
                             Document d2,
                             HashMap<String, Double> DF,
                             int docAmount,
                             libsvm.svm_model model) throws Exception {
        HashMap<String, Double> features = docPairFeature(d1, d2, DF, docAmount);
        String input = "0 " + formatSameEventFeature(features);
        double result = 0;
        try {
            result = SVM.predict_x(model, input);
        } catch (NumberFormatException e){
            e.printStackTrace();
            System.err.println("SVM feature error: " + input + "|request type: " + features.get("DocTopicType"));
        }
        if (result == 1) {
            return true;
        } else {
            return false;
        }
    }

}
