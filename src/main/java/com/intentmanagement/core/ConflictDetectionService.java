package com.intentmanagement.core;

import java.util.*;

public class ConflictDetectionService {
    public record Result(boolean feasible, double requestedMbps, double availableMbps, String explanation) {}
    public Result check(List<Intent> active, Intent candidate, double totalCapacityMbps) {
        double used=active.stream().filter(i -> i.status.equals("ACTIVE")).mapToDouble(i->i.requiredBandwidthMbps).sum();
        double requested=used+candidate.requiredBandwidthMbps;
        if(candidate.targetLatencyMs<=0 || candidate.requiredBandwidthMbps<=0) return new Result(false,requested,Math.max(0,totalCapacityMbps-used),"Invalid latency or bandwidth requirement.");
        double free=totalCapacityMbps-used;
        if(candidate.requiredBandwidthMbps>free) return new Result(false,requested,Math.max(0,free),String.format(Locale.ROOT,"Bandwidth conflict: existing intents use %.1f Mbps; request needs %.1f Mbps; only %.1f Mbps remains from %.1f Mbps total capacity.",used,candidate.requiredBandwidthMbps,Math.max(0,free),totalCapacityMbps));
        // Paper-aligned service-specific checks: the paper's MCPTT example uses a 75 ms
        // latency target, while strict-latency URLLC uses a much tighter target. Do not use
        // an arbitrary universal 50 ms cutoff for every high-priority intent.
        if (candidate.serviceType.equalsIgnoreCase("Emergency") && candidate.targetLatencyMs > 75.0)
            return new Result(false, requested, free, "Policy conflict: emergency/MCPTT target exceeds the 75 ms service target used by the paper's example scenario.");
        if (candidate.serviceType.equalsIgnoreCase("URLLC") && candidate.targetLatencyMs > 10.0)
            return new Result(false, requested, free, "Policy conflict: URLLC target exceeds the configured 10 ms strict-latency profile.");
        return new Result(true,requested,Math.max(0,free),String.format(Locale.ROOT,"Feasible: %.1f Mbps requested after allocation, within %.1f Mbps capacity. %.1f Mbps remains before this request.",requested,totalCapacityMbps,free));
    }
}
