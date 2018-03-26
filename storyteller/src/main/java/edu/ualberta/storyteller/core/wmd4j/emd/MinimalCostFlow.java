package edu.ualberta.storyteller.core.wmd4j.emd;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Telmo Menezes (telmo@telmomenezes.com)
 */
class MinimalCostFlow {
    
    long compute(long[] e, List<List<Edge>> c, List<List<Edge0>> x) {
        
        Integer numNodes = e.length;
        
        List<Integer> nodesToQ = new ArrayList<>(numNodes);
        List<List<Edge1>> rCostForward = new ArrayList<>(numNodes);
        List<List<Edge2>> rCostCapBackward = new ArrayList<>(numNodes);
        
        for(int i = 0 ; i < numNodes ; i++) {
            nodesToQ.add(0);
            rCostForward.add(new ArrayList<>(c.get(i).size()));
            
            for(Edge it : c.get(i)) {
                x.get(i).add(new Edge0(it.to, it.cost, 0));
                x.get(it.to).add(new Edge0(i, -it.cost, 0));
                rCostForward.get(i).add(new Edge1(it.to, it.cost));
            }
            
            rCostCapBackward.add(new ArrayList<>(c.get(i).size()));
        }
        
        long U = 0;
        long delta;
        
        List<Long> d = new ArrayList<>(numNodes);
        List<Integer> prev = new ArrayList<>(numNodes);
        
        for(int i = 0 ; i < numNodes ; ++i) {
            if(e[i] > U)
                U = e[i];
            for(Edge it : c.get(i)) {
                rCostCapBackward.get(it.to).add(new Edge2(i, -it.cost, 0));
            }
            d.add(0l);
            prev.add(0);
        }
        
        while(true) {
            long maxSupply = 0;
            int k = 0;
            for(int i = 0 ; i < numNodes ; i++) {
                if(e[i] > 0) {
                    if(maxSupply < e[i]) {
                        maxSupply = e[i];
                        k = i;
                    }
                }
            }
            if(maxSupply == 0)
                break;
            delta = maxSupply;
            
            int[] l = new int[1];
            computeShortestPath(d, prev, k, rCostForward, rCostCapBackward, e, l, nodesToQ, numNodes);
            
            int to = l[0];
            do {
                int from = prev.get(to);
                
                int itccb = 0;
                while((itccb < rCostCapBackward.get(from).size()) && (rCostCapBackward.get(from).get(itccb).to != to)) {
                    itccb++;
                }
                if(itccb < rCostCapBackward.get(from).size()) {
                    if(rCostCapBackward.get(from).get(itccb).residualCapacity < delta)
                        delta = rCostCapBackward.get(from).get(itccb).residualCapacity;
                }
                
                to = from;
            } while(to != k);
            
            to = l[0];
            do {
                int from = prev.get(to);
                int itx = 0;
                while(x.get(from).get(itx).to != to) {
                    itx++;
                }
                x.get(from).get(itx).flow += delta;
                
                // update residual for backward edges
                int itccb = 0;
                while((itccb < rCostCapBackward.get(to).size()) && (rCostCapBackward.get(to).get(itccb).to != from)) {
                    itccb++;
                }
                if(itccb < rCostCapBackward.get(to).size()) {
                    rCostCapBackward.get(to).get(itccb).residualCapacity += delta;
                }
                itccb = 0;
                while((itccb < rCostCapBackward.get(from).size()) && (rCostCapBackward.get(from).get(itccb).to != to)) {
                    itccb++;
                }
                if(itccb < rCostCapBackward.get(from).size()) {
                    rCostCapBackward.get(from).get(itccb).residualCapacity -= delta;
                }
                
                // update e
                e[to] = e[to] + delta;
                e[from] = e[from] - delta;
                
                to = from;
            } while(to != k);
        }
        
        // compute distance from x
        long dist = 0;
        for(int from = 0 ; from < numNodes ; from++) {
            for(Edge0 it : x.get(from)) {
                dist += (it.cost * it.flow);
            }
        }
        return dist;
    }
    
    void computeShortestPath(List<Long> d,
                             List<Integer> prev,
                             int from,
                             List<List<Edge1>> costForward,
                             List<List<Edge2>> costBackward,
                             long[] e,
                             int[] l,
                             List<Integer> nodesToQ,
                             Integer numNodes) {
        // Making heap (all inf except 0, so we are saving comparisons...)
        List<Edge3> Q = new ArrayList<>(numNodes);
        for(int i = 0 ; i < numNodes ; i++) {
            Q.add(new Edge3());
        }
        
        Q.get(0).to = from;
        nodesToQ.set(from, 0);
        Q.get(0).dist = 0;
        
        int j = 1;
        
        for(int i = 0 ; i < from ; i++) {
            Q.get(j).to = i;
            nodesToQ.set(i, j);
            Q.get(j).dist = Long.MAX_VALUE;
            j++;
        }
        
        for(int i = from + 1 ; i < numNodes ; i++) {
            Q.get(j).to = i;
            nodesToQ.set(i, j);
            Q.get(j).dist = Long.MAX_VALUE;
            j++;
        }
        
        boolean[] finalNodesFlg = new boolean[numNodes];
        
        do {
            int u = Q.get(0).to;
            
            d.set(u, Q.get(0).dist);
            finalNodesFlg[u] = true;
            if(e[u] < 0) {
                l[0] = u;
                break;
            }
            
            heapRemoveFirst(Q, nodesToQ);
            
            for(Edge1 it : costForward.get(u)) {
                long alt = d.get(u) + it.reducedCost;
                int v = it.to;
                if((nodesToQ.get(v) < Q.size()) && (alt < Q.get(nodesToQ.get(v)).dist)) {
                    heapDecreaseKey(Q, nodesToQ, v, alt);
                    prev.set(v, u);
                }
            }
            for(Edge2 it : costBackward.get(u)) {
                if(it.residualCapacity > 0) {
                    long alt = d.get(u) + it.reducedCost;
                    int v = it.to;
                    if((nodesToQ.get(v) < Q.size()) && (alt < Q.get(nodesToQ.get(v)).dist)) {
                        heapDecreaseKey(Q, nodesToQ, v, alt);
                        prev.set(v, u);
                    }
                }
            }
            
        } while(Q.size() > 0);
        
        for(int _from = 0 ; _from < numNodes ; ++_from) {
            for(Edge1 it : costForward.get(_from)) {
                if(finalNodesFlg[_from]) {
                    it.reducedCost += d.get(_from) - d.get(l[0]);
                }
                if(finalNodesFlg[it.to]) {
                    it.reducedCost -= d.get(it.to) - d.get(l[0]);
                }
            }
        }
        
        for(int _from = 0 ; _from < numNodes ; ++_from) {
            for(Edge2 it : costBackward.get(_from)) {
                if(finalNodesFlg[_from]) {
                    it.reducedCost += d.get(_from) - d.get(l[0]);
                }
                if(finalNodesFlg[it.to]) {
                    it.reducedCost -= d.get(it.to) - d.get(l[0]);
                }
            }
        }
    }
    
    void heapDecreaseKey(List<Edge3> Q, List<Integer> nodesto_Q, int v, long alt) {
        int i = nodesto_Q.get(v);
        Q.get(i).dist = alt;
        while(i > 0 && Q.get(PARENT(i)).dist > Q.get(i).dist) {
            swapHeap(Q, nodesto_Q, i, PARENT(i));
            i = PARENT(i);
        }
    }
    
    void heapRemoveFirst(List<Edge3> Q, List<Integer> nodesto_Q) {
        swapHeap(Q, nodesto_Q, 0, Q.size() - 1);
        Q.remove(Q.size() - 1);
        heapify(Q, nodesto_Q, 0);
    }
    
    void heapify(List<Edge3> Q, List<Integer> nodesto_Q, int i) {
        do {
            // TODO: change to loop
            int l = LEFT(i);
            int r = RIGHT(i);
            int smallest;
            if((l < Q.size()) && (Q.get(l).dist < Q.get(i).dist)) {
                smallest = l;
            }
            else {
                smallest = i;
            }
            if((r < Q.size()) && (Q.get(r).dist < Q.get(smallest).dist)) {
                smallest = r;
            }
            
            if(smallest == i)
                return;
            
            swapHeap(Q, nodesto_Q, i, smallest);
            i = smallest;
            
        } while(true);
    }
    
    void swapHeap(List<Edge3> Q, List<Integer> nodesToQ, int i, int j) {
        Edge3 tmp = Q.get(i);
        Q.set(i, Q.get(j));
        Q.set(j, tmp);
        nodesToQ.set(Q.get(j).to, j);
        nodesToQ.set(Q.get(i).to, i);
    }
    
    int LEFT(int i) {
        return 2 * (i + 1) - 1;
    }
    
    int RIGHT(int i) {
        return 2 * (i + 1); // 2 * (i + 1) + 1 - 1
    }
    
    int PARENT(int i) {
        return (i - 1) / 2;
    }
    
}

