package com.intentmanagement.optimization;

public class QoSLossCalculator {
    public double mse(double[] observed, double[] target) {
        if(observed==null||target==null||observed.length==0||observed.length!=target.length) throw new IllegalArgumentException("Arrays must be non-empty and equal length");
        double sum=0; for(int i=0;i<observed.length;i++){double d=observed[i]-target[i];sum+=d*d;} return sum/observed.length;
    }
    public double normalizedServiceLoss(double latency, double targetLatency, double loss, double maxLoss, double throughput, double targetThroughput) {
        double l=(latency-targetLatency)/Math.max(1,targetLatency); double p=(loss-maxLoss)/Math.max(0.1,maxLoss); double t=(targetThroughput-throughput)/Math.max(1,targetThroughput);
        return (l*l+p*p+t*t)/3.0;
    }
}
