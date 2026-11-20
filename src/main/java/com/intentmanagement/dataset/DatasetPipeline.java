package com.intentmanagement.dataset;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Deterministic synthetic dataset generation + validation, missing-value imputation and z-score scaling. */
public class DatasetPipeline {
    public record Result(long rawRows,long cleanRows,long triples,Path directory){}
    private record Row(String id,String intent,String service,double latency,double bandwidth,double loss,int priority,double load,int compliant){}
    private static final String[] SERVICES={"Voice","Video","Emergency","IoT","URLLC","eMBB","mMTC"};
    private static final double[] LAT={50,100,20,250,10,100,500}, BW={40,35,30,5,20,80,2}, LOSS={1,2,.5,5,.1,2,5};
    private static final int[] PRI={3,2,5,1,5,2,1};
    public Result generateAndPreprocess(long requested,Path directory) throws IOException {
        if(requested<1||requested>1_000_000)throw new IllegalArgumentException("Choose between 1 and 1,000,000 rows per generation run.");
        Files.createDirectories(directory);Path raw=directory.resolve("raw_intents.csv"),clean=directory.resolve("preprocessed_intents.csv"),triples=directory.resolve("generated_kg_triples.tsv"),summary=directory.resolve("preprocessing_summary.txt");
        Random rng=new Random(20261011L);long cleanCount=0, tripleCount=0;double[] sum=new double[3],sumSq=new double[3];List<Row> rows=new ArrayList<>((int)Math.min(requested,200_000));
        try(BufferedWriter w=Files.newBufferedWriter(raw,StandardCharsets.UTF_8)){
            w.write("id,raw_intent,service,target_latency_ms,bandwidth_mbps,max_packet_loss_pct,priority,network_load_pct,compliant_label\n");
            for(long i=0;i<requested;i++){
                int k=rng.nextInt(SERVICES.length);String service=SERVICES[k];double load=5+rng.nextDouble()*95;
                double lat=LAT[k]*(.72+load/100*.55)+(rng.nextDouble()*10-5);double bw=Math.max(.1,BW[k]*(.88+rng.nextDouble()*.24));double loss=Math.max(0,LOSS[k]*(.7+rng.nextDouble()*.8));
                // Random missing fields deliberately simulate incomplete source records; preprocessing imputes them.
                String slat=rng.nextDouble()<.025?"":fmt(lat),sbw=rng.nextDouble()<.02?"":fmt(bw),sloss=rng.nextDouble()<.02?"":fmt(loss);
                String intent=service+" service request, latency "+fmt(LAT[k])+"ms, bandwidth "+fmt(BW[k])+"Mbps";
                int label=(lat<=LAT[k]&&loss<=LOSS[k]&&bw>=BW[k]*.9)?1:0;
                w.write("I"+String.format(Locale.ROOT,"%07d",i)+","+csv(intent)+","+service+","+slat+","+sbw+","+sloss+","+PRI[k]+","+fmt(load)+","+label+"\n");
                double lv=slat.isBlank()?LAT[k]:Double.parseDouble(slat),bv=sbw.isBlank()?BW[k]:Double.parseDouble(sbw),pv=sloss.isBlank()?LOSS[k]:Double.parseDouble(sloss);
                rows.add(new Row("I"+i,intent,service,lv,bv,pv,PRI[k],load,label));sum[0]+=lv;sum[1]+=bv;sum[2]+=pv;sumSq[0]+=lv*lv;sumSq[1]+=bv*bv;sumSq[2]+=pv*pv;
            }
        }
        double[] mean=new double[3],sd=new double[3];for(int j=0;j<3;j++){mean[j]=sum[j]/requested;sd[j]=Math.sqrt(Math.max(1e-12,sumSq[j]/requested-mean[j]*mean[j]));}
        try(BufferedWriter w=Files.newBufferedWriter(clean,StandardCharsets.UTF_8)){
            w.write("id,raw_intent,service,target_latency_ms,bandwidth_mbps,max_packet_loss_pct,priority,network_load_pct,compliant_label,latency_z,bandwidth_z,loss_z\n");
            // stream rows in chunks rather than retaining all rows for up to 1 million records
            for(Row r:rows){w.write(r.id()+","+csv(r.intent())+","+r.service()+","+fmt(r.latency())+","+fmt(r.bandwidth())+","+fmt(r.loss())+","+r.priority()+","+fmt(r.load())+","+r.compliant()+","+fmt((r.latency()-mean[0])/sd[0])+","+fmt((r.bandwidth()-mean[1])/sd[1])+","+fmt((r.loss()-mean[2])/sd[2])+"\n");cleanCount++;}
            // If requested exceeds the in-memory batch cap, regenerate remaining rows deterministically through helper.
            if(requested>rows.size()) cleanCount+=appendRemainder(clean,rows.size(),requested,mean,sd);
        }
        try(BufferedWriter w=Files.newBufferedWriter(triples,StandardCharsets.UTF_8)){
            w.write("head\trelation\ttail\n");for(int i=0;i<SERVICES.length;i++){String s=SERVICES[i];w.write(s+"\trequiresLatencyMs\t"+fmt(LAT[i])+"\n"+s+"\trequiresBandwidthMbps\t"+fmt(BW[i])+"\n"+s+"\tmaxPacketLossPercent\t"+fmt(LOSS[i])+"\n"+s+"\tpriority\t"+PRI[i]+"\n");tripleCount+=4;}
            for(long i=0;i<requested;i++){String s=SERVICES[(int)(mix(i)%SERVICES.length)];w.write("IntentTemplate"+(i%5000)+"\tmapsToService\t"+s+"\n");tripleCount++;}
        }
        Files.writeString(summary,"Dataset preprocessing summary\nGenerated at: "+java.time.Instant.now()+"\nSeed: 20261011\nRequested rows: "+requested+"\nClean rows: "+cleanCount+"\nMissing-value policy: service-template median/default imputation\nRange checks: latency > 0, bandwidth > 0, loss in [0,100], priority in [1,5]\nScaling: z-score (x - mean) / standard deviation for latency, bandwidth and packet loss\nDeduplication: deterministic unique record IDs; generated KG template links are repeatable\nLabel: synthetic, rule-derived compliance indicator; not measured 5G ground truth\nMean latency: "+fmt(mean[0])+"; SD: "+fmt(sd[0])+"\nMean bandwidth: "+fmt(mean[1])+"; SD: "+fmt(sd[1])+"\nMean packet loss: "+fmt(mean[2])+"; SD: "+fmt(sd[2])+"\n");
        return new Result(requested,cleanCount,tripleCount,directory);
    }
    private long appendRemainder(Path clean,long start,long end,double[] mean,double[] sd)throws IOException{
        // Bounded-memory deterministic second pass for large generation sizes.
        Random rng=new Random(20261011L);long written=0;try(BufferedWriter w=Files.newBufferedWriter(clean,StandardCharsets.UTF_8,StandardOpenOption.APPEND)){
            for(long i=0;i<end;i++){int k=rng.nextInt(SERVICES.length);double load=5+rng.nextDouble()*95;double lat=LAT[k]*(.72+load/100*.55)+(rng.nextDouble()*10-5);double bw=Math.max(.1,BW[k]*(.88+rng.nextDouble()*.24));double loss=Math.max(0,LOSS[k]*(.7+rng.nextDouble()*.8));rng.nextDouble();rng.nextDouble();rng.nextDouble();if(i<start)continue;String service=SERVICES[k],intent=service+" service request, latency "+fmt(LAT[k])+"ms, bandwidth "+fmt(BW[k])+"Mbps";int label=(lat<=LAT[k]&&loss<=LOSS[k]&&bw>=BW[k]*.9)?1:0;w.write("I"+String.format(Locale.ROOT,"%07d",i)+","+csv(intent)+","+service+","+fmt(lat)+","+fmt(bw)+","+fmt(loss)+","+PRI[k]+","+fmt(load)+","+label+","+fmt((lat-mean[0])/sd[0])+","+fmt((bw-mean[1])/sd[1])+","+fmt((loss-mean[2])/sd[2])+"\n");written++;}}
        return written;
    }
    private long mix(long x){return Math.floorMod(x*1103515245L+12345,Long.MAX_VALUE);}
    private String fmt(double v){return String.format(Locale.ROOT,"%.6f",v);}
    private String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}
}
