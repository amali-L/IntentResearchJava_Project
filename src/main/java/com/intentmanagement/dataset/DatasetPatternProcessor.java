package com.intentmanagement.dataset;

import com.intentmanagement.knowledge.KnowledgeGraph;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Learns descriptive intent-pattern facts from the dataset already shipped with the project.
 * It never generates or duplicates dataset rows. Derived facts are added to the runtime KG and saved as TSV.
 */
public final class DatasetPatternProcessor {
    public record Result(long rows, long trainRows, long testRows, int services, int patternFacts, Path report, Path triples) {}
    private static final Set<String> STOP = Set.of("i","need","a","an","the","for","to","my","new","set","up","create","configure","enable","please","provision","request","support","service","intent","network","cellular","during","peak","load","in","on","of","with","as","use","would","like","be","and","is","this","that","customer","application","factory","user","mobile","connected");

    public Result process(Path csv, Path outputDir, KnowledgeGraph graph) throws IOException {
        if (!Files.isRegularFile(csv)) throw new IOException("Existing labelled dataset not found: " + csv.toAbsolutePath());
        Files.createDirectories(outputDir);
        Map<String,Long> serviceCounts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String,Map<String,Long>> tokenCounts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        long rows=0, train=0, test=0;
        try (BufferedReader br = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String header = br.readLine();
            if (header == null || !header.toLowerCase(Locale.ROOT).contains("intent") || !header.toLowerCase(Locale.ROOT).contains("gold_service"))
                throw new IOException("Dataset needs intent and gold_service columns: " + csv);
            String line;
            while ((line=br.readLine())!=null) {
                if(line.isBlank()) continue;
                List<String> cols=parseCsv(line);
                if(cols.size()<4) continue;
                String intent=cols.get(1), service=cols.get(2).trim(), split=cols.get(3).trim();
                if(service.isBlank() || intent.isBlank()) continue;
                rows++; if(split.equalsIgnoreCase("test"))test++; else train++;
                serviceCounts.merge(service,1L,Long::sum);
                Map<String,Long> words=tokenCounts.computeIfAbsent(service,k->new HashMap<>());
                for(String token:intent.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.%]+"," ").trim().split("\\s+")) {
                    if(token.length()<3 || STOP.contains(token) || token.matches("\\d+(?:\\.\\d+)?") || token.endsWith("mbps") || token.endsWith("ms")) continue;
                    words.merge(token,1L,Long::sum);
                }
            }
        }
        if(rows==0) throw new IOException("No usable records found in " + csv);
        Path triples=outputDir.resolve("dataset_pattern_triples.tsv"); int facts=0;
        try(BufferedWriter out=Files.newBufferedWriter(triples,StandardCharsets.UTF_8)){
            out.write("head\trelation\ttail\n");
            for(var entry:serviceCounts.entrySet()){
                String service=entry.getKey(); long count=entry.getValue();
                graph.add(service,"datasetObservedIntentCount",Long.toString(count));
                out.write(service+"\tdatasetObservedIntentCount\t"+count+"\n"); facts++;
                List<Map.Entry<String,Long>> top=tokenCounts.getOrDefault(service,Map.of()).entrySet().stream()
                        .sorted(Map.Entry.<String,Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).limit(12).toList();
                for(var token:top){graph.add(service,"observedIntentPattern",token.getKey());out.write(service+"\tobservedIntentPattern\t"+token.getKey()+"\n");facts++;}
            }
        }
        Path report=outputDir.resolve("dataset_pattern_analysis.txt");
        StringBuilder text=new StringBuilder("EXISTING DATASET → KNOWLEDGE GRAPH PATTERN ANALYSIS\n\nSource: ").append(csv.toAbsolutePath())
                .append("\nRows read (no new rows generated): ").append(rows).append("\nTrain rows: ").append(train).append("\nTest rows: ").append(test)
                .append("\nService classes: ").append(serviceCounts.size()).append("\nDerived pattern facts: ").append(facts)
                .append("\nRuntime KG triples after update: ").append(graph.all().size())
                .append("\n\nService distribution and frequent descriptive tokens:\n");
        for(var entry:serviceCounts.entrySet()){
            text.append("\n").append(entry.getKey()).append(" (rows=").append(entry.getValue()).append(")\n  ");
            text.append(tokenCounts.getOrDefault(entry.getKey(),Map.of()).entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).limit(12)
                    .map(e->e.getKey()+"="+e.getValue()).reduce((a,b)->a+", ").orElse("no frequent tokens"));
        }
        text.append("\n\nInterpretation: token counts are descriptive patterns derived from the supplied labelled intent corpus. They are added as observedIntentPattern facts; they do not replace curated RDF ontology constraints or prove scientific generalization.\n");
        Files.writeString(report,text,StandardCharsets.UTF_8);
        return new Result(rows,train,test,serviceCounts.size(),facts,report,triples);
    }
    private static List<String> parseCsv(String line){
        List<String> result=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean quoted=false;
        for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){cell.append('"');i++;}else quoted=!quoted;}else if(c==','&&!quoted){result.add(cell.toString());cell.setLength(0);}else cell.append(c);}result.add(cell.toString());return result;
    }
}
