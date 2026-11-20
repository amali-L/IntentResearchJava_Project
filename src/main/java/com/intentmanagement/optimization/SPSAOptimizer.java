package com.intentmanagement.optimization;

import java.util.Arrays;
import java.util.Random;
import java.util.function.ToDoubleFunction;

/** SPSA numerical optimizer using two objective evaluations per iteration. */
public class SPSAOptimizer {
    public record Result(double[] parameters, double initialLoss, double finalLoss, int iterations) {}
    public Result optimize(double[] initial, ToDoubleFunction<double[]> objective, int iterations, long seed) {
        if(initial==null||initial.length==0||iterations<1) throw new IllegalArgumentException("Invalid optimizer input");
        double[] theta=initial.clone(); double initialLoss=objective.applyAsDouble(theta.clone()); Random rng=new Random(seed);
        double a=0.08,A=Math.max(10,iterations*0.1),alpha=0.602,c=0.05,gamma=0.101;
        for(int k=0;k<iterations;k++) {
            double ak=a/Math.pow(k+1+A,alpha), ck=c/Math.pow(k+1,gamma); double[] delta=new double[theta.length], plus=theta.clone(), minus=theta.clone();
            for(int i=0;i<theta.length;i++){delta[i]=rng.nextBoolean()?1:-1;plus[i]+=ck*delta[i];minus[i]-=ck*delta[i];}
            double fp=objective.applyAsDouble(plus), fm=objective.applyAsDouble(minus);
            for(int i=0;i<theta.length;i++){double grad=(fp-fm)/(2*ck*delta[i]);theta[i]-=ak*grad;theta[i]=Math.max(0,Math.min(1,theta[i]));}
        }
        return new Result(theta,initialLoss,objective.applyAsDouble(theta.clone()),iterations);
    }
}
