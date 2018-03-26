package edu.ualberta.storyteller.core.eventdetector;

import edu.ualberta.storyteller.core.util.*;
import edu.ualberta.storyteller.core.dataloader.*;

import java.text.BreakIterator;
import java.util.*;
import static java.lang.Math.min;

public class FeatureExtractor {

    /**
     * Calculate #common_keywords
     * @param s1 Segmented string.
     * @param s2 Segmented string.
     * @param stopwords Stopwords set.
     * @return Common keywords number.
     */
    public static int numCommonKeyword(String s1, String s2, HashSet<String> stopwords) {
        int result = 0;
        HashSet<String> kws1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        HashSet<String> kws2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));
        for (String kw: kws1) {
            if (!stopwords.contains(kw) && kws2.contains(kw)) {
                result++;
            }
        }

        return result;
    }

    /**
     * Calculate #common_keywords / #keywords_in_s1_and_s2
     * @param s1 Segmented string.
     * @param s2 Segmented string.
     * @param stopwords Stopwords set.
     * @return Common keywords percentage.
     */
    public static double percentCommonKeyword(String s1, String s2, HashSet<String> stopwords) {
        double numCommon = 0;
        HashSet<String> kws1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        HashSet<String> kws2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));
        for (String kw: kws1) {
            if (!stopwords.contains(kw) && kws2.contains(kw)) {
                numCommon++;
            }
        }

        double result = 0.0;
        if (kws1.size() > 0 && kws2.size() > 0) {
            result = numCommon / (kws1.size() + kws2.size() - numCommon);
        }

        return result;
    }

    /**
     * Calculate #common_keywords / #keywords_in_s1
     * @param s1 Segmented string.
     * @param s2 Segmented string.
     * @param stopwords Stopwords set.
     * @return Common keywords percentage.
     */
    public static double percentCommonKeywordLeft(String s1, String s2, HashSet<String> stopwords) {
        double numCommon = 0;
        HashSet<String> kws1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        HashSet<String> kws2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));
        for (String kw: kws1) {
            if (!stopwords.contains(kw) && kws2.contains(kw)) {
                numCommon++;
            }
        }

        double result = 0.0;
        if (kws1.size() > 0 && kws2.size() > 0) {
            result = numCommon / kws1.size();
        }

        return result;
    }


    public static int numCommonCharacter(String s1, String s2) {
        int result=0;
        ArrayList<Character> s1Split = new ArrayList<>();
        ArrayList<Character> s2Split = new ArrayList<>();
        for(int i=0;i<s1.length();i++){
            s1Split.add(s1.charAt(i));
        }
        for(int i=0;i<s2.length();i++){
            s2Split.add(s2.charAt(i));
        }
        for (Character c: s1Split) {
            if (s2Split.contains(c)) {
                result++;
            }
        }
        return result;
    }


    public static int numCharacter(String s) {
        return s.length();
    }

    public static double percentCommonCharacterLeft(String s1, String s2) {
        if (s1.length()==0) {
            return 0.0;
        }

        return numCommonCharacter(s1, s2) / (s1.length() + 0.0);
    }

    public static double percentCommonCharacterShort(String s1, String s2) {
        if (s1.length()==0 || s2.length()==0) {
            return 0.0;
        }

        return numCommonCharacter(s1, s2) / (min(s1.length(), s2.length()) + 0.0);
    }

    public static int numCommonSequentialCharacterLeft(String s1, String s2) {
        List<Character> s1Split = new ArrayList<>();
        List<Character> s2Split = new ArrayList<>();
        for(int i=0;i<s1.length();i++){
            s1Split.add(s1.charAt(i));
        }
        for(int i=0;i<s2.length();i++){
            s2Split.add(s2.charAt(i));
        }

        ArrayList<Integer> commonCharacterIdx = new ArrayList<>();
        for (Character c: s1Split) {
            if (s2Split.contains(c)) {
                commonCharacterIdx.add(s2Split.indexOf(c));
            }
        }

        if (commonCharacterIdx.size()==0) {
            return 0;
        } else {
            int[] ints = commonCharacterIdx.stream().mapToInt(i -> i).toArray();
            return ArrayUtils.lengthOfLIS(ints);
        }
    }


    public static double percentCommonSequentialCharacterLeft(String s1, String s2) {
        if (s1.length()==0) {
            return 0.0;
        }

        return numCommonSequentialCharacterLeft(s1, s2) / (s1.length() + 0.0);
    }


    /**
     * Get TF map of s.
     * @param s Segmented string.
     * @param stopwords Stopwords set.
     * @return TF map.
     */
    public static HashMap<String, Double> getTF(String s, HashSet<String> stopwords) {
        HashMap<String, Double> result = new HashMap<>();
        String[] kws = s.split("\\s+");
        for (int i = 0; i < kws.length; ++i) {
            if (!stopwords.contains(kws[i])) {
                if (result.containsKey(kws[i])) {
                    result.put(kws[i], result.get(kws[i]) + 1);
                } else {
                    result.put(kws[i], 1.0);
                }
            }
        }
        return result;
    }

    /**
     * Get tfidf map of s.
     * @param s Segmented string.
     * @param stopwords Stopwords set.
     * @param DF DF map.
     * @param docAmount Amount of documents for DF.
     * @return tfidf map.
     */
    public static HashMap<String, Double> getTFIDF(String s,
                                                   HashSet<String> stopwords,
                                                   HashMap<String, Double> DF,
                                                   int docAmount) {
        HashMap<String, Double> result = new HashMap<>();
        String[] kws = s.split("\\s+");
        for (int i = 0; i < kws.length; ++i) {
            if (!stopwords.contains(kws[i])) {
                Double df = 1.0;
                if (DF.containsKey(kws[i])) {
                    df = DF.get(kws[i]);
                }
                if (result.containsKey(kws[i])) {
                    result.put(kws[i], result.get(kws[i]) + NlpUtils.tfidf(1.0, NlpUtils.idf(df, docAmount)));
                } else {
                    result.put(kws[i], NlpUtils.tfidf(1.0, NlpUtils.idf(df, docAmount)));
                }
            }
        }
        return result;
    }

    /**
     * Calculate a map's two order vector norm.
     * @param map A map.
     * @return Vector norm.
     */
    public static double vectorNorm2Order(HashMap<String, Double> map) {
        double result = 0;
        for (String k : map.keySet()) {
            result += Math.pow(map.get(k), 2);
        }
        result = Math.sqrt(result);
        return result;
    }


    /**
     * Calculate two strings' similarity by TF vector.
     * @param s1 Segmented string.
     * @param s2 Segmented string.
     * @return TF vector cosine similarity.
     */
    public static double cosineSimilarityByTF(String s1, String s2, HashSet<String> stopwords) {
        HashMap<String, Double> TF1 = getTF(s1, stopwords);
        HashMap<String, Double> TF2 = getTF(s2, stopwords);

        double sim = 0;
        for (String k1 : TF1.keySet()) {
            if (TF2.containsKey(k1)) {
                double tf1 = TF1.get(k1);
                double tf2 = TF2.get(k1);
                sim += tf1 * tf2;
            }
        }

        double vectorSize1 = vectorNorm2Order(TF1);
        double vectorSize2 = vectorNorm2Order(TF2);

        if (vectorSize1 > 0 && vectorSize2 > 0) {
            sim = sim / vectorSize1 / vectorSize2;
        } else {
            sim = 0;
        }

        return sim;
    }

    /**
     * Calculate two strings' similarity by tfidf vector.
     * @param s1 Segmented string.
     * @param s2 Segmented string.
     * @param stopwords Stopwords set.
     * @param DF DF map.
     * @param docAmount Amount of documents for DF.
     * @return tfidf vector cosine similarity.
     */
    public static double cosineSimilarityByTFIDF(String s1,
                                                 String s2,
                                                 HashSet<String> stopwords,
                                                 HashMap<String, Double> DF,
                                                 int docAmount) {
        HashMap<String, Double> TFIDF1 = getTFIDF(s1, stopwords, DF, docAmount);
        HashMap<String, Double> TFIDF2 = getTFIDF(s2, stopwords, DF, docAmount);

        double sim = 0;
        for (String k1 : TFIDF1.keySet()) {
            if (TFIDF2.containsKey(k1)) {
                sim += TFIDF1.get(k1) * TFIDF2.get(k1);
            }
        }

        double vectorSize1 = vectorNorm2Order(TFIDF1);
        double vectorSize2 = vectorNorm2Order(TFIDF2);

        if (vectorSize1 > 0 && vectorSize2 > 0) {
            sim = sim / vectorSize1 / vectorSize2;
        } else {
            sim = 0;
        }

        return sim;
    }


    public static int numQueryWordsInDocKeywords(String query, Document d) {
        int result = 0;
        HashSet<String> query_words = new HashSet<>(Arrays.asList(query.split("\\s+")));
        for (Keyword kw: d.keywords.values()) {
            if (query_words.contains(kw.baseForm)) {
                result++;
            }
        }
        return result;
    }


    public static double percentQueryWordsInDocKeywords(String query, Document d) {
        double result = 0;
        HashSet<String> query_words = new HashSet<>(Arrays.asList(query.split("\\s+")));
        for (Keyword kw: d.keywords.values()) {
            if (query_words.contains(kw.baseForm)) {
                result++;
            }
        }

        if (query_words.size() > 0) {
            result = result / query_words.size();
        } else {
            result = 0;
        }

        return result;
    }


    public static int numQueryWordsInDocMainKeywords(String query, Document d) {
        int result = 0;
        HashSet<String> query_words = new HashSet<>(Arrays.asList(query.split("\\s+")));
        for (String kw: d.mainKeywords) {
            if (query_words.contains(kw)) {
                result++;
            }
        }
        return result;
    }


    public static double percentQueryWordsInDocMainKeywords(String query, Document d) {
        double result = 0;
        HashSet<String> query_words = new HashSet<>(Arrays.asList(query.split("\\s+")));
        for (String kw: d.mainKeywords) {
            if (query_words.contains(kw)) {
                result++;
            }
        }

        if (query_words.size() > 0) {
            result = result / query_words.size();
        } else {
            result = 0;
        }

        return result;
    }


    /**
     * Calculate two document's similarity by TF vector.
     * @param d1 One document.
     * @param d2 One document.
     * @return TF vector cosine similarity of two documents.
     */
    public static double cosineSimilarityByTF(Document d1, Document d2) {
        double sim = 0;
        for (Keyword k1 : d1.keywords.values()) {
            if (d2.keywords.containsKey(k1.baseForm)) {
                double tf1 = k1.tf;
                double tf2 = d2.keywords.get(k1.baseForm).tf;
                sim += tf1 * tf2;
            }
        }

        if (d1.tfVectorSize < 0) {
            d1.calcTFVectorSize();
        }
        if (d2.tfVectorSize < 0) {
            d2.calcTFVectorSize();
        }

        if (d1.tfVectorSize == 0 || d2.tfVectorSize == 0) {
            return 0;
        } else {
            return sim / d1.tfVectorSize / d2.tfVectorSize;
        }
    }

    /**
     * Calculate cosine similarity of two documents.
     * @param d1 One document.
     * @param d2 Another document.
     * @param DF The map between keywords and their df.
     * @param docSize Number of total documents.
     * @return Cosine similarity between d1 and d2.
     */
    public static double cosineSimilarityByTFIDF(Document d1,
                                                 Document d2,
                                                 HashMap<String, Double> DF,
                                                 int docSize) {
        double sim = 0;
        for (Keyword k1 : d1.keywords.values()) {
            if (d2.keywords.containsKey(k1.baseForm)) {
                Double df = DF.get(k1.baseForm);
                double tf1 = k1.tf;
                double tf2 = d2.keywords.get(k1.baseForm).tf;
                sim += NlpUtils.tfidf(tf1, NlpUtils.idf(df, docSize)) * NlpUtils.tfidf(tf2, NlpUtils.idf(df, docSize));
            }
        }

        if (d1.tfidfVectorSize < 0) {
            d1.calcTFIDFVectorSize(DF, docSize);
        }
        if (d2.tfidfVectorSize < 0) {
            d2.calcTFIDFVectorSize(DF, docSize);
        }

        if (d1.tfidfVectorSize == 0 || d2.tfidfVectorSize == 0) {
            return 0;
        } else {
            return sim / d1.tfidfVectorSize / d2.tfidfVectorSize;
        }
    }



    public static double numCommonTitleKeyword(Document d1, Document d2) {
        double result = 0;
        for (String k: d1.titleKeywords) {
            if (d2.titleKeywords.contains(k)) {
                result++;
            }
        }

        return result;
    }


    public static double percentCommonTitleKeyword(Document d1, Document d2) {
        double intersect = numCommonTitleKeyword(d1, d2);
        double result = 0;
        if (intersect > 0) {
            result = intersect / (d1.titleKeywords.size() + d2.titleKeywords.size() - intersect);
        }
        return result;
    }


    public static String[] splitSentences(String text, String language) {
        if (language.equals("Chinese")) {
            return splitChineseSentences(text);
        } else if (language.equals("English")) {
            return splitEnglishSentences(text);
        } else {
            System.out.println("Warning: other language not supported yet!");
        }
        return null;
    }


    public static String[] splitChineseSentences(String text) {
        return text.split("[。？!;\\?][\\r\\n\\t\\s ]+");
    }


    public static String[] splitEnglishSentences(String text) {
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
        iterator.setText(text);
        int start = iterator.first();

        ArrayList<String> sentences = new ArrayList<String>();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            sentences.add(text.substring(start,end));
        }

        return sentences.toArray(new String[0]);
    }


    public static double firstNSentencesCosineSimilarityByTF(Document d1, Document d2,
                                                             int n, HashSet<String> stopwords, String language) {
        String[] content1 = splitSentences(d1.segContent, language);
        String[] content2 = splitSentences(d2.segContent, language);

        String sentences1 = "";
        if (content1.length > n) {
            for (int i = 0; i < n; ++i) {
                sentences1 += content1[i];
            }
        } else {
            sentences1 = d1.segContent;
        }

        String sentences2 = "";
        if (content2.length > n) {
            for (int i = 0; i < n; ++i) {
                sentences2 += content2[i];
            }
        } else {
            sentences2 = d2.segContent;
        }

        return cosineSimilarityByTF(sentences1, sentences2, stopwords);
    }


    public static double firstNSentencesCosineSimilarityByTF(String query, Document d,
                                                             int n, HashSet<String> stopwords, String language) {
        String[] content = splitSentences(d.segContent, language);

        String sentences = "";
        if (content.length > n) {
            for (int i = 0; i < n; ++i) {
                sentences += content[i];
            }
        } else {
            sentences = d.segContent;
        }

        return cosineSimilarityByTF(query, sentences, stopwords);
    }

}
