package edu.ualberta.storyteller.core.wmd4j;

import java.util.ArrayList;

/**
 * Created by Majer on 21.9.2016.
 */
public class FrequencyVector {
    
    private volatile long     frequency;

    private ArrayList<Double> vector;
    
    public FrequencyVector(ArrayList<Double> vector) {
        this(1, vector);
    }
    
    public FrequencyVector(long frequency, ArrayList<Double> vector) {
        this.frequency = frequency;
        this.vector = vector;
    }
    
    public void incrementFrequency() {
        frequency += 1;
    }
    
    public long getFrequency() {
        return frequency;
    }
    
    public ArrayList<Double> getVector() {
        return vector;
    }

}
