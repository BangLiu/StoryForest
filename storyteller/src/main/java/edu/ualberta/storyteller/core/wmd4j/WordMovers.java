package edu.ualberta.storyteller.core.wmd4j;

import edu.ualberta.storyteller.core.wmd4j.emd.EarthMovers;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Majer on 21.9.2016.
 */
public class WordMovers {
    
    private static final double DEFAULT_STOPWORD_WEIGHT = 0.0;

    private static final double DEFAULT_MAX_DISTANCE    = 1;
    
    public WordVectors wordVectors;

    private Set<String> stopwords;

    private double stopwordWeight;
    
    private EarthMovers earthMovers;

    public static  boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }
    
    private WordMovers(Builder builder) {
        this.wordVectors = builder.wordVectors;
        this.stopwords = builder.stopwords;
        this.stopwordWeight = builder.stopwordWeight;
        this.earthMovers = new EarthMovers();
    }
    
    public double distance(String a, String b) {
        if(isEmpty(a) || isEmpty(b)) {
            throw new IllegalArgumentException();
        }
        return distance(a.split(" "), b.split(" "));
    }
    
    public double distance(String[] tokensA, String[] tokensB) {
        if(tokensA.length < 1 || tokensB.length < 1) {
            throw new IllegalArgumentException();
        }
        
        Map<String, FrequencyVector> mapA = bagOfVectors(tokensA);
        Map<String, FrequencyVector> mapB = bagOfVectors(tokensB);
        
        if(mapA.size() == 0 || mapB.size() == 0) {
            return 1000000000.0;  // TODO:
        }
        //vocabulary of current tokens
        List<String> vocab = Stream.of(mapA.keySet(), mapB.keySet())
                                   .flatMap(Collection::stream)
                                   .distinct()
                                   .collect(Collectors.toList());
        double matrix[][] = new double[vocab.size()][vocab.size()];
        
        for(int i = 0 ; i < matrix.length ; i++) {
            String tokenA = vocab.get(i);
            for(int j = 0 ; j < matrix.length ; j++) {
                String tokenB = vocab.get(j);
                if(mapA.containsKey(tokenA) && mapB.containsKey(tokenB)) {
                    double distance = distance2(mapA.get(tokenA).getVector(), mapB.get(tokenB).getVector());
                    //if tokenA and tokenB are stopwords, calculate distance according to stopword weight
                    if(stopwords != null && tokenA.length() != 1 && tokenB.length() != 1) {
                        distance *= stopwords.contains(tokenA) && stopwords.contains(tokenB) ? 1 : stopwordWeight;
                    }
                    matrix[i][j] = distance;
                    matrix[j][i] = distance;
                }
            }
        }
        
        double[] freqA = frequencies(vocab, mapA);
        double[] freqB = frequencies(vocab, mapB);
        
        return earthMovers.distance(freqA, freqB, matrix, 0);
    }

    public double squaredDistance(ArrayList<Double> vec1, ArrayList<Double> vec2) {
        double sd = 0.0;

        for(int i = 0; (long)i < vec1.size(); ++i) {
            double d = vec1.get(i) - vec2.get(i);
            sd += d * d;
        }

        return sd;
    }

    public double distance2(ArrayList<Double> vec1, ArrayList<Double> vec2) {
        return Math.sqrt(squaredDistance(vec1, vec2));
    }

    
    private Map<String, FrequencyVector> bagOfVectors(String[] tokens) {
        
        Map<String, FrequencyVector> map = new LinkedHashMap<>(tokens.length);
        Arrays.stream(tokens)
              .filter(x -> wordVectors.hasWord(x))
              .forEach(x -> map.merge(x, new FrequencyVector(wordVectors.getWordVectorMatrix(x)), (v, o) -> {
                  v.incrementFrequency();
                  return v;
              }));
        
        return map;
    }
    
    /**
     * Normalized frequencies for vocab
     */
    private double[] frequencies(List<String> vocab, Map<String, FrequencyVector> map) {
        return vocab.stream().mapToDouble(x -> {
            if(map.containsKey(x)) {
                return (double) map.get(x).getFrequency() / map.size();
            }
            return 0d;
        }).toArray();
    }
    
    public static Builder Builder() {
        return new Builder();
    }
    
    public static final class Builder {
        
        private WordVectors wordVectors;

        private Set<String> stopwords;
        
        private double stopwordWeight = DEFAULT_STOPWORD_WEIGHT;
        
        private Builder() {}
        
        public WordMovers build() {
            return new WordMovers(this);
        }
        
        public Builder wordVectors(WordVectors wordVectors) {
            this.wordVectors = wordVectors;
            return this;
        }
        
        public Builder stopwords(Set<String> stopwords) {
            this.stopwords = stopwords;
            return this;
        }
        
        public Builder stopwordWeight(double stopwordWeight) {
            this.stopwordWeight = stopwordWeight;
            return this;
        }
        
    }

}
