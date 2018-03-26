package edu.ualberta.storyteller.core.wmd4j.emd;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Telmo Menezes (telmo@telmomenezes.com)
 */

class Edge {
    
    int  to;
    long cost;
    
    Edge(int to, long cost) {
        this.to = to;
        this.cost = cost;
    }
}

class Edge0 {
    int  to;
    long cost;
    long flow;
    
    Edge0(int to, long cost, long flow) {
        this.to = to;
        this.cost = cost;
        this.flow = flow;
    }
}

class Edge1 {
    
    int  to;
    long reducedCost;
    
    Edge1(int to, long reducedCost) {
        this.to = to;
        this.reducedCost = reducedCost;
    }
}

class Edge2 {
    
    int  to;
    long reducedCost;
    long residualCapacity;
    
    Edge2(int to, long reducedCost, long residualCapacity) {
        this.to = to;
        this.reducedCost = reducedCost;
        this.residualCapacity = residualCapacity;
    }
}

class Edge3 {
    
    int  to;
    long dist;
    
    public Edge3() {
        to = 0;
        dist = 0;
    }
}