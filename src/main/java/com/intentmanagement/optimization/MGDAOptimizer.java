package com.intentmanagement.optimization;

/** Computes simplex weights for a small set of estimated objective gradients by projected gradient descent. */
public class MGDAOptimizer {
    public double[] weights(double[][] gradients, int iterations) {
        if(gradients==null||gradients.length==0) throw new IllegalArgumentException("At least one gradient is required");
        int objectives=gradients.length, dims=gradients[0].length; if(dims==0) throw new IllegalArgumentException("Gradient vectors cannot be empty");
        for(double[] g:gradients) if(g.length!=dims) throw new IllegalArgumentException("Gradient dimensions differ");
        double[] w=new double[objectives]; java.util.Arrays.fill(w,1.0/objectives); double step=0.05;
        for(int it=0;it<iterations;it++) {
            double[] combined=new double[dims]; for(int i=0;i<objectives;i++) for(int d=0;d<dims;d++) combined[d]+=w[i]*gradients[i][d];
            double[] next=w.clone(); for(int i=0;i<objectives;i++){double dot=0;for(int d=0;d<dims;d++)dot+=gradients[i][d]*combined[d];next[i]-=step*2*dot;}
            w=projectSimplex(next);
        }
        return w;
    }
    private double[] projectSimplex(double[] v) {
        double[] sorted=v.clone(); java.util.Arrays.sort(sorted); for(int i=0;i<sorted.length/2;i++){double x=sorted[i];sorted[i]=sorted[sorted.length-1-i];sorted[sorted.length-1-i]=x;}
        double sum=0,theta=0;int rho=0;for(int i=0;i<sorted.length;i++){sum+=sorted[i];double t=(sum-1)/(i+1);if(sorted[i]-t>0){rho=i+1;theta=t;}}
        double[] out=new double[v.length];double total=0;for(int i=0;i<v.length;i++){out[i]=Math.max(0,v[i]-theta);total+=out[i];}if(total==0){java.util.Arrays.fill(out,1.0/out.length);}else for(int i=0;i<out.length;i++)out[i]/=total;return out;
    }
}
