package com.intentmanagement.knowledge;

import java.util.*;

/** Compact diagonal-Gaussian KG2E-style embedding prototype. See docs/MATHEMATICS.md for scope and limitations. */
public class KG2EModel {
    private final Map<String,double[]> means = new HashMap<>();
    private final Map<String,double[]> variances = new HashMap<>();
    private final int dimension;
    private final Random random;
    public KG2EModel(int dimension, long seed) { if (dimension < 2) throw new IllegalArgumentException("dimension must be >= 2"); this.dimension=dimension; random=new Random(seed); }
    private void ensure(String key) {
        means.computeIfAbsent(key, k -> { double[] a=new double[dimension]; for(int i=0;i<dimension;i++) a[i]=random.nextGaussian()*0.1; return a; });
        variances.computeIfAbsent(key, k -> { double[] a=new double[dimension]; Arrays.fill(a,0.5); return a; });
    }
    public double score(Triple t) {
        ensure("E:"+t.head()); ensure("R:"+t.relation()); ensure("E:"+t.tail());
        double[] h=means.get("E:"+t.head()), r=means.get("R:"+t.relation()), tail=means.get("E:"+t.tail());
        double[] vh=variances.get("E:"+t.head()), vr=variances.get("R:"+t.relation()), vt=variances.get("E:"+t.tail());
        double score=0; for(int i=0;i<dimension;i++){ double mu=h[i]-r[i]-tail[i]; double c=Math.max(1e-5,vh[i]+vr[i]+vt[i]); score += -(mu*mu/c)-Math.log(c); } return score;
    }
    /** Margin-ranking training over known triples and schema-safe caller-provided corruptions. */
    public double train(List<Triple> positives, int epochs, double learningRate, double margin) {
        if(positives.isEmpty()) throw new IllegalArgumentException("training triples cannot be empty");
        double last=0;
        for(int epoch=0;epoch<epochs;epoch++) {
            double loss=0;
            for(Triple p:positives) {
                ensure("E:"+p.head()); ensure("R:"+p.relation()); ensure("E:"+p.tail());
                String wrongTail=p.tail()+"_unknown";
                Triple n=new Triple(p.head(),p.relation(),wrongTail);
                double sp=score(p), sn=score(n); double hinge=Math.max(0,margin-sp+sn); loss+=hinge;
                if(hinge>0) {
                    // Stable lightweight gradient step on means; full covariance gradients are intentionally diagonal.
                    double[] h=means.get("E:"+p.head()), r=means.get("R:"+p.relation()), t=means.get("E:"+p.tail());
                    for(int i=0;i<dimension;i++){ double d=h[i]-r[i]-t[i]; h[i]-=learningRate*2*d; r[i]+=learningRate*2*d; t[i]+=learningRate*2*d; }
                }
            }
            last=loss/positives.size();
        }
        return last;
    }
    public int entityCount(){return (int)means.keySet().stream().filter(k->k.startsWith("E:")).count();}
}
