package com.intentmanagement.reporting;

import com.intentmanagement.core.Intent;
import java.io.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.*;

public class ReportService {
    public Path exportIntents(List<Intent> intents, Path output) throws IOException {
        Files.createDirectories(output.getParent());
        try(BufferedWriter w=Files.newBufferedWriter(output,StandardCharsets.UTF_8)){
            w.write("id,service,status,latency_target_ms,bandwidth_required_mbps,packet_loss_target_pct,priority,measured_latency_ms,measured_throughput_mbps,measured_packet_loss_pct,explanation\n");
            for(Intent i:intents) w.write(String.join(",",csv(i.id),csv(i.serviceType),csv(i.status),n(i.targetLatencyMs),n(i.requiredBandwidthMbps),n(i.maxPacketLossPercent),Integer.toString(i.priority),n(i.measuredLatencyMs),n(i.measuredThroughputMbps),n(i.measuredPacketLossPercent),csv(i.explanation))+"\n");
        }return output;
    }
    private String n(double x){return String.format(Locale.ROOT,"%.3f",x);} private String csv(String s){return "\""+(s==null?"":s.replace("\"","\"\""))+"\"";}
}
