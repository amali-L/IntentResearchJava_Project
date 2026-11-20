package com.intentmanagement.optimization;

import com.intentmanagement.core.Intent;
import com.intentmanagement.knowledge.KG2EModel;
import com.intentmanagement.knowledge.Triple;
import java.util.*;

/** Executable equation trace for one intent. Equations are computed from current measured/simulated KPIs. */
public final class ResearchEquationPipeline {
    public record Trace(double eq4RankingLoss, double eq5Mse, double eq6WeightedCumulativeLoss, double[] eq9SpsaParameters,
                        double eq9InitialObjective, double eq9FinalObjective, double[] eq11MgdaWeights,
                        double[] eq12UpdatedParameters, double kg2eScore, String note) {}
    private final QoSLossCalculator lossCalculator=new QoSLossCalculator();
    private final SPSAOptimizer spsa=new SPSAOptimizer();
    private final MGDAOptimizer mgda=new MGDAOptimizer();
    private final KG2EModel kg2e=new KG2EModel(8,42);
    public Trace evaluate(Intent intent, List<Triple> facts) {
        double[] observed={intent.measuredLatencyMs,intent.measuredThroughputMbps,intent.measuredPacketLossPercent};
        double targetThroughput=Math.max(0.01,intent.requiredBandwidthMbps);
        double[] target={intent.targetLatencyMs,targetThroughput,intent.maxPacketLossPercent};
        double eq4=facts.isEmpty()?0:kg2e.train(facts,1,0.001,1.0);
        double eq5=lossCalculator.mse(observed,target);
        double serviceLoss=lossCalculator.normalizedServiceLoss(intent.measuredLatencyMs,intent.targetLatencyMs,intent.measuredPacketLossPercent,intent.maxPacketLossPercent,intent.measuredThroughputMbps,targetThroughput);
        double scaling=Math.max(0.0,Math.min(1.0,intent.priority/5.0));
        double eq6=scaling*serviceLoss;
        // Eq. (9): SPSA estimates a descent direction for a bounded two-objective surrogate.
        SPSAOptimizer.Result s=spsa.optimize(new double[]{0.5,0.5},p->objective(p,serviceLoss,eq5/(1.0+eq5)),80,Math.abs(intent.id.hashCode())+42L);
        // Eq. (11): MGDA simplex weights over latency, throughput and packet-loss objective gradients.
        double latencyError=(intent.measuredLatencyMs-intent.targetLatencyMs)/Math.max(1,intent.targetLatencyMs);
        double throughputError=(targetThroughput-intent.measuredThroughputMbps)/Math.max(1,targetThroughput);
        double lossError=(intent.measuredPacketLossPercent-intent.maxPacketLossPercent)/Math.max(0.1,intent.maxPacketLossPercent);
        double[][] gradients={{latencyError,0.1,0.0},{0.0,throughputError,0.1},{0.1,0.0,lossError}};
        double[] weights=mgda.weights(gradients,250);
        // Eq. (12): combined normalized direction updates bounded policy parameters.
        double[] combined=new double[3];for(int i=0;i<gradients.length;i++)for(int j=0;j<3;j++)combined[j]+=weights[i]*gradients[i][j];
        double[] updated={clamp(s.parameters()[0]-0.05*combined[0]),clamp(s.parameters()[1]-0.05*combined[1])};
        double score=facts.isEmpty()?0:kg2e.score(facts.get(0));
        return new Trace(eq4,eq5,eq6,s.parameters(),s.initialLoss(),s.finalLoss(),weights,updated,score,
                "Eq.5 MSE and Eq.6 priority-weighted loss use the current KPI snapshot; Eq.9 SPSA, Eq.11 MGDA, and Eq.12 combined update are executable prototype calculations. Eq.3 score is diagonal-Gaussian KG2E approximation; Eq.4 training remains the project's ranking-inspired approximation, not paper-faithful training.");
    }
    public double weightedCumulativeLoss(List<Intent> intents) {
        double total=0;for(Intent i:intents){if(!"ACTIVE".equals(i.status))continue;double l=lossCalculator.normalizedServiceLoss(i.measuredLatencyMs,i.targetLatencyMs,i.measuredPacketLossPercent,i.maxPacketLossPercent,i.measuredThroughputMbps,Math.max(0.01,i.requiredBandwidthMbps));double scale=Math.max(0,Math.min(1,i.priority/5.0));total+=scale*l;}return total;
    }
    private static double objective(double[] p,double serviceLoss,double mse){return p[0]*serviceLoss+p[1]*mse+0.01*(p[0]*p[0]+p[1]*p[1]);}
    private static double clamp(double x){return Math.max(0,Math.min(1,x));}
}
