package com.intentmanagement.core;

import com.intentmanagement.knowledge.KnowledgeGraph;
import java.util.Locale;
import java.util.regex.*;

/** HMM-assisted service recognition plus explicit numeric-parameter extraction. Service defaults are retrieved from the KG by IntentManager. */
public class IntentParser {
    private final HmmServiceRecognizer hmmRecognizer = new HmmServiceRecognizer();
    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(ms|mbps|mb/s|%|percent)", Pattern.CASE_INSENSITIVE);
    public Intent parse(String text) { return parse(text, new KnowledgeGraph()); }
    public Intent parse(String text, KnowledgeGraph graph) { return parse(text, graph, null); }
    public Intent parse(String text, KnowledgeGraph graph, String rdfMatchedService) {
        if(text==null || text.isBlank()) throw new IllegalArgumentException("Intent text cannot be empty");
        String s=text.toLowerCase(Locale.ROOT); String service=(rdfMatchedService == null || rdfMatchedService.isBlank()) ? graph.resolveService(s) : rdfMatchedService;
        if(service.equals("General")) { service=mapServiceAlias(s); if(service.equals("General")) service=hmmRecognizer.recognize(s); }
        double latency=defaultLatency(service), bandwidth=defaultBandwidth(service), loss=defaultLoss(service); int priority=defaultPriority(service);
        Matcher m=NUMBER.matcher(s); while(m.find()) { double v=Double.parseDouble(m.group(1)); String unit=m.group(2).toLowerCase(Locale.ROOT); if(unit.equals("ms")) latency=v; else if(unit.equals("mbps")||unit.equals("mb/s")) bandwidth=v; else loss=v; }
        if(s.contains("critical")||s.contains("highest priority")) priority=5; else if(s.contains("high priority")) priority=4;
        if(latency<=0 || bandwidth<=0 || loss<0 || loss>100) throw new IllegalArgumentException("Latency/bandwidth must be positive; packet loss must be between 0 and 100%");
        return new Intent(text,service,latency,bandwidth,loss,priority);
    }
    /** Bootstrapped domain NER fallback: maps common application wording to the paper's service classes.
     * This is a deterministic lexical fallback, not a claim to reproduce the paper's trained HMM. */
    private String mapServiceAlias(String s) {
        if (containsAny(s, "emergency", "ambulance", "public safety", "mission-critical", "push-to-talk", "critical alert")) return "Emergency";
        if (containsAny(s, "urllc", "ultra reliable", "ultra-reliable", "gaming", "game lag", "real-time control", "industrial control")) return "URLLC";
        if (containsAny(s, "voice", "voip", "voice call", "audio call", "conference call", "push to talk")) return "Voice";
        if (containsAny(s, "video", "stream", "video call", "video conference", "live broadcast")) return "Video";
        if (containsAny(s, "iot", "sensor", "telemetry", "smart meter", "smart building", "mmtc", "machine-type")) return "mMTC";
        if (containsAny(s, "embb", "web", "browsing", "file transfer", "file upload", "download", "cloud backup", "bulk data", "high bandwidth")) return "eMBB";
        return "General";
    }
    private boolean containsAny(String text, String... terms) { for (String term : terms) if (text.contains(term)) return true; return false; }
    private double defaultLatency(String s){return switch(s){case "Emergency"->20;case "Voice"->50;case "Video"->100;case "IoT"->250;case "URLLC"->10;case "eMBB"->100;case "mMTC"->500;default->150;};}
    private double defaultBandwidth(String s){return switch(s){case "Emergency"->30;case "Voice"->40;case "Video"->35;case "IoT"->5;case "URLLC"->20;case "eMBB"->80;case "mMTC"->2;default->10;};}
    private double defaultLoss(String s){return switch(s){case "Emergency","Voice"->1;case "URLLC"->0.1;case "Video","eMBB"->2;default->5;};}
    private int defaultPriority(String s){return switch(s){case "Emergency","URLLC"->5;case "Voice"->3;case "Video","eMBB"->2;default->1;};}
}
