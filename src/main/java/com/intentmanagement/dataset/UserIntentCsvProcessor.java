package com.intentmanagement.dataset;

import com.intentmanagement.core.Intent;
import com.intentmanagement.core.IntentManager;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Loads a user CSV containing exactly the required id,intent inputs and exports parsed intents separately. */
public final class UserIntentCsvProcessor {
    public record Row(String id, String rawIntent, String service, double latencyMs,
                      double bandwidthMbps, double packetLossPct, int priority,
                      String status, String explanation) {}
    public record Result(Path source, Path output, List<Row> rows) {}

    public Result process(Path input, Path output, IntentManager manager) throws IOException {
        List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
        if (lines.isEmpty()) throw new IOException("The selected CSV is empty.");
        List<String> header = parseCsvLine(lines.get(0));
        Map<String,Integer> ix = new HashMap<>();
        for (int i=0;i<header.size();i++) ix.put(header.get(i).trim().toLowerCase(Locale.ROOT), i);
        if (!ix.containsKey("id") || !ix.containsKey("intent"))
            throw new IOException("CSV must contain these columns: id,intent (column names are case-insensitive). Found: " + header);
        List<Row> rows = new ArrayList<>();
        for (int line=1; line<lines.size(); line++) {
            if (lines.get(line).isBlank()) continue;
            List<String> cells;
            try { cells=parseCsvLine(lines.get(line)); }
            catch (RuntimeException ex) { rows.add(failed("line-"+(line+1), lines.get(line), "CSV parse error: "+ex.getMessage())); continue; }
            String id = cell(cells,ix.get("id")).trim();
            String text = cell(cells,ix.get("intent")).trim();
            if (id.isBlank()) id="row-"+(line+1);
            if (text.isBlank()) { rows.add(failed(id,text,"EMPTY_INTENT: intent text is blank")); continue; }
            try {
                Intent intent=manager.previewIndependent(text);
                String status=intent.status;
                String explanation=intent.explanation==null?"":intent.explanation;
                if ("General".equalsIgnoreCase(intent.serviceType)) {
                    status = status.equals("REJECTED") ? status : "NEEDS_REVIEW";
                    explanation += " | Service could not be confidently inferred; inspect this row before simulation.";
                }
                rows.add(new Row(id,text,intent.serviceType,intent.targetLatencyMs,intent.requiredBandwidthMbps,
                    intent.maxPacketLossPercent,intent.priority,status,explanation));
            } catch (Exception ex) { rows.add(failed(id,text,"PARSE_ERROR: "+safe(ex.getMessage()))); }
        }
        if(rows.isEmpty()) throw new IOException("CSV has a header but no data rows.");
        Files.createDirectories(output.toAbsolutePath().getParent());
        try(BufferedWriter w=Files.newBufferedWriter(output,StandardCharsets.UTF_8)) {
            w.write("id,intent,service,target_latency_ms,required_bandwidth_mbps,max_packet_loss_pct,priority,status,explanation,lifecycle_trace\n");
            for(Row r:rows) {
                String stage = "ACTIVE".equalsIgnoreCase(r.status()) ?
                    "A_INTENT_PROCESSING|B_SERVICE_TEMPLATE|C_MISSING_TRIPLES|D_KG_ASSISTED_MATCH_PROTOTYPE|E_TEMPLATE_COMPLETION|F_VERIFIED_READY_FOR_ISOLATED_DEPLOYMENT" :
                    ("REJECTED".equalsIgnoreCase(r.status()) ? "A-E_PROCESSED|F_VERIFICATION_REJECTED" : "A-E_PROCESSED|F_REVIEW_REQUIRED");
                w.write(csv(r.id)+","+csv(r.rawIntent)+","+csv(r.service)+","+r.latencyMs+","+r.bandwidthMbps+","+r.packetLossPct+","+r.priority+","+csv(r.status)+","+csv(r.explanation)+","+csv(stage)+"\n");
            }
        }
        return new Result(input.toAbsolutePath(),output.toAbsolutePath(),List.copyOf(rows));
    }
    private static Row failed(String id,String text,String why){return new Row(id,text,"Unknown",Double.NaN,Double.NaN,Double.NaN,0,"NEEDS_REVIEW",why);}
    private static String cell(List<String> c,int i){return i>=0&&i<c.size()?c.get(i):"";}
    private static String safe(String s){return s==null?"Unknown error":s.replace('\n',' ');}
    private static String csv(String s){if(s==null)return "";return "\""+s.replace("\"","\"\"").replace("\r"," ").replace("\n"," ")+"\"";}
    /** CSV single-line parser supporting quoted commas and escaped double quotes. */
    public static List<String> parseCsvLine(String s){List<String> out=new ArrayList<>();StringBuilder b=new StringBuilder();boolean q=false;for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='"'){if(q&&i+1<s.length()&&s.charAt(i+1)=='"'){b.append('"');i++;}else q=!q;}else if(c==','&&!q){out.add(b.toString());b.setLength(0);}else b.append(c);}if(q)throw new IllegalArgumentException("unclosed quoted field");out.add(b.toString());return out;}
}
