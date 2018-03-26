package edu.ualberta.storyteller.core.wmd4j.emd;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

/**
 * Algorithm authors:
 *
 * @author Telmo Menezes (telmo@telmomenezes.com)
 * @author Ofir Pele
 *         <p>
 *         Refactored by Črtomir Majer
 */
public class EarthMovers {
    
    private static final int    REMOVE_NODE_FLAG = -1;
    private static final double MULT_FACTOR      = 1000000;
    
    private MinimalCostFlow minimalCostFlow = new MinimalCostFlow();
    
    public double distance(double[] P, double[] Q, double[][] C, double extraMassPenalty) {
        
        int N = P.length;
        long[] iP = new long[N];
        long[] iQ = new long[N];
        long[][] iC = new long[N][N];
        
        double sumP = 0;
        double sumQ = 0;
        double maxC = C[0][0];
        
        for(int i = 0 ; i < N ; i++) {
            sumP += P[i];
            sumQ += Q[i];
            for(int j = 0 ; j < N ; j++) {
                if(C[i][j] > maxC)
                    maxC = C[i][j];
            }
        }
        
        double minSum = Math.min(sumP, sumQ);
        double maxSum = Math.max(sumP, sumQ);
        double PQnormFactor = MULT_FACTOR / maxSum;
        double CnormFactor = MULT_FACTOR / maxC;
        
        for(int i = 0 ; i < N ; i++) {
            iP[i] = (long) Math.floor(P[i] * PQnormFactor + 0.5);
            iQ[i] = (long) Math.floor(Q[i] * PQnormFactor + 0.5);
            for(int j = 0 ; j < N ; j++) {
                iC[i][j] = (long) (Math.floor(C[i][j] * CnormFactor + 0.5));
            }
        }
        
        double dist = distance(iP, iQ, iC, 0);
        dist = (dist / PQnormFactor) / CnormFactor;
        
        if(extraMassPenalty == -1)
            extraMassPenalty = maxC;
        dist += (maxSum - minSum) * extraMassPenalty;
        
        return dist;
    }
    
    public long distance(long[] Pc, long[] Qc, long[][] C, long extraMassPenalty) {
        
        int N = Pc.length;
        
        if(Qc.length != N)
            throw new IllegalArgumentException();
        
        long[] P;
        long[] Q;
        long absDiffSumPSumQ;
        long sumP = 0;
        long sumQ = 0;
        
        for(int i = 0 ; i < N ; i++) {
            sumP += Pc[i];
            sumQ += Qc[i];
        }
        
        if(sumQ > sumP) {
            P = Qc;
            Q = Pc;
            absDiffSumPSumQ = sumQ - sumP;
        }
        else {
            P = Pc;
            Q = Qc;
            absDiffSumPSumQ = sumP - sumQ;
        }
        
        long[] b = new long[2 * N + 2];
        int THRESHOLD_NODE = 2 * N;
        int ARTIFICIAL_NODE = THRESHOLD_NODE + 1;
        System.arraycopy(P, 0, b, 0, N);
        
        for(int i = N ; i < 2 * N ; i++) {
            b[i] = Q[i - N];
        }
        
        b[THRESHOLD_NODE] = -absDiffSumPSumQ;
        b[ARTIFICIAL_NODE] = 0L;
        
        long maxC = 0;
        for(int i = 0 ; i < N ; i++) {
            for(int j = 0 ; j < N ; j++) {
                if(C[i][j] > maxC)
                    maxC = C[i][j];
            }
        }
        if(extraMassPenalty == -1)
            extraMassPenalty = maxC;
        
        Set<Integer> sourcesThatFlowNotOnlyToThresh = new HashSet<>();
        Set<Integer> sinksThatGetFlowNotOnlyFromThresh = new HashSet<>();
        long preFlowCost = 0;
        
        List<List<Edge>> c = new ArrayList<>(b.length);
        
        for(int i = 0 ; i < b.length ; i++) {
            c.add(new LinkedList<>());
        }
        
        for(int i = 0 ; i < N ; i++) {
            if(b[i] == 0)
                continue;
            for(int j = 0 ; j < N ; j++) {
                if(b[j + N] == 0 || C[i][j] == maxC)
                    continue;
                c.get(i).add(new Edge(j + N, C[i][j]));
                sourcesThatFlowNotOnlyToThresh.add(i);
                sinksThatGetFlowNotOnlyFromThresh.add(j + N);
            }
        }
        
        for(int i = N ; i < 2 * N ; i++) {
            b[i] = -b[i];
        }
        
        for(int i = 0 ; i < N ; ++i) {
            c.get(i).add(new Edge(THRESHOLD_NODE, 0));
            c.get(THRESHOLD_NODE).add(new Edge(i + N, maxC));
        }
        
        for(int i = 0 ; i < ARTIFICIAL_NODE ; i++) {
            c.get(i).add(new Edge(ARTIFICIAL_NODE, maxC + 1));
            c.get(ARTIFICIAL_NODE).add(new Edge(i, maxC + 1));
        }
        
        int currentNodeName = 0;
        int[] nodesNewNames = new int[b.length];
        Arrays.fill(nodesNewNames, REMOVE_NODE_FLAG);
        
        for(int i = 0 ; i < N * 2 ; i++) {
            if(b[i] != 0) {
                if(sourcesThatFlowNotOnlyToThresh.contains(i) || sinksThatGetFlowNotOnlyFromThresh.contains(i)) {
                    nodesNewNames[i] = currentNodeName;
                    currentNodeName++;
                }
                else {
                    if(i >= N) {
                        preFlowCost -= (b[i] * maxC);
                    }
                    b[THRESHOLD_NODE] = b[THRESHOLD_NODE] + b[i];
                }
            }
        }
        
        nodesNewNames[THRESHOLD_NODE] = currentNodeName;
        currentNodeName++;
        nodesNewNames[ARTIFICIAL_NODE] = currentNodeName;
        currentNodeName++;
        
        long[] bb = new long[currentNodeName];
        
        int j = 0;
        for(int i = 0 ; i < b.length ; i++) {
            if(nodesNewNames[i] != REMOVE_NODE_FLAG) {
                bb[j] = b[i];
                j++;
            }
        }
        
        List<List<Edge>> cc = new ArrayList<>(bb.length);
        List<List<Edge0>> flows = new ArrayList<>(bb.length);
        
        for(int i = 0 ; i < bb.length ; i++) {
            cc.add(new LinkedList<>());
            flows.add(new ArrayList<>(bb.length * 2));
        }
        
        for(int i = 0 ; i < c.size() ; i++) {
            if(nodesNewNames[i] == REMOVE_NODE_FLAG)
                continue;
            for(Edge it : c.get(i)) {
                if(nodesNewNames[it.to] != REMOVE_NODE_FLAG) {
                    cc.get(nodesNewNames[i]).add(new Edge(nodesNewNames[it.to], it.cost));
                }
            }
        }
        
        long mcfDist = minimalCostFlow.compute(bb, cc, flows);
        return preFlowCost + mcfDist + (absDiffSumPSumQ * extraMassPenalty);
    }
}
    

