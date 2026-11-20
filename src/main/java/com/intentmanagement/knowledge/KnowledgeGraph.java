package com.intentmanagement.knowledge;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Small RDF-like triple store. Supports dynamic TSV loading and query-by-head/relation. */
public class KnowledgeGraph {
    private final List<Triple> triples = new ArrayList<>();
    public KnowledgeGraph() {
        seed("Voice", "voice,call,conversation,convvoice", 50,40,1,3);
        seed("Video", "video,stream,streaming,video conference", 100,35,2,2);
        seed("Emergency", "emergency,ambulance,mission critical,mcptt,critical push to talk", 20,30,0.5,5);
        seed("IoT", "iot,sensor,telemetry,machine type", 250,5,5,1);
        seed("URLLC", "urllc,ultra reliable,low latency", 10,20,0.1,5);
        seed("eMBB", "embb,broadband,high throughput", 100,80,1,2);
        seed("mMTC", "mmtc,massive iot,massive sensors", 500,2,5,1);
        add("Network", "hasCapacityMbps", "100");
        add("Network", "technology", "5G-NSA (ns-3 model required for packet-level results)");
    }
    private void seed(String service, String keywords, double latency, double bandwidth, double loss, int priority) {
        add(service,"requiresLatencyMs",Double.toString(latency)); add(service,"requiresBandwidthMbps",Double.toString(bandwidth));
        add(service,"maxPacketLossPercent",Double.toString(loss)); add(service,"priority",Integer.toString(priority));
        for(String k:keywords.split(",")) add(service,"hasKeyword",k.trim());
        add(service,"serviceClass", service.equals("Emergency")||service.equals("URLLC")?"GBR":"NGBR");
    }
    public synchronized void add(String h, String r, String t) { triples.add(new Triple(clean(h),clean(r),clean(t))); }
    private String clean(String s){return Objects.requireNonNull(s).trim();}
    public synchronized List<Triple> all() { return Collections.unmodifiableList(new ArrayList<>(triples)); }
    public synchronized List<Triple> find(String head, String relation) { return triples.stream().filter(t -> (head == null || t.head().equalsIgnoreCase(head)) && (relation == null || t.relation().equalsIgnoreCase(relation))).toList(); }
    public synchronized Optional<String> value(String head, String relation) { return find(head, relation).stream().map(Triple::tail).findFirst(); }
    public synchronized String resolveService(String text) {
        String lower=text.toLowerCase(Locale.ROOT);
        for(Triple t:triples) if(t.relation().equals("hasKeyword") && lower.contains(t.tail().toLowerCase(Locale.ROOT))) return t.head();
        return "General";
    }
    public synchronized void loadTsv(Path path) throws IOException {
        if(!Files.exists(path)) throw new IOException("Knowledge graph TSV not found: "+path.toAbsolutePath());
        int loaded=0; for(String line:Files.readAllLines(path)) { if(line.isBlank()||line.startsWith("#")||line.toLowerCase(Locale.ROOT).startsWith("head\t")) continue; String[] p=line.split("\t",3); if(p.length==3){add(p[0],p[1],p[2]);loaded++;} }
        if(loaded==0) throw new IOException("No triples found in "+path);
    }
    public synchronized String queryText(String query) {
        if(query==null||query.isBlank()) return "Enter a service/entity or relation to query the knowledge graph.";
        String q=query.toLowerCase(Locale.ROOT); List<Triple> found=triples.stream().filter(t->t.head().toLowerCase(Locale.ROOT).contains(q)||t.relation().toLowerCase(Locale.ROOT).contains(q)||t.tail().toLowerCase(Locale.ROOT).contains(q)).limit(300).toList();
        if(found.isEmpty()) return "No matching triples for: "+query;
        StringBuilder b=new StringBuilder("KG query: ").append(query).append("\nMatches: ").append(found.size()).append(" (display capped at 300)\n\n"); found.forEach(t->b.append(t).append('\n')); return b.toString();
    }
    public synchronized String describe() { StringBuilder b = new StringBuilder("Knowledge graph triples: ").append(triples.size()).append("\n\n"); triples.forEach(t -> b.append(t).append('\n')); return b.toString(); }
}
