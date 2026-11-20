package com.intentmanagement.core;

import java.time.Instant;
import java.util.UUID;

public class Intent {
    public final String id;
    public final String rawText;
    public String serviceType;
    public double targetLatencyMs;
    public double requiredBandwidthMbps;
    public double maxPacketLossPercent;
    public int priority;
    public String status;
    public String explanation;
    public double measuredLatencyMs;
    public double measuredThroughputMbps;
    public double measuredPacketLossPercent;
    public final Instant createdAt;

    public Intent(String rawText, String serviceType, double latency, double bandwidth, double packetLoss, int priority) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.rawText = rawText;
        this.serviceType = serviceType;
        this.targetLatencyMs = latency;
        this.requiredBandwidthMbps = bandwidth;
        this.maxPacketLossPercent = packetLoss;
        this.priority = priority;
        this.status = "RECEIVED";
        this.explanation = "Awaiting validation";
        this.createdAt = Instant.now();
    }
}
