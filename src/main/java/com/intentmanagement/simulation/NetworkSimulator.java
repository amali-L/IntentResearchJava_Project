package com.intentmanagement.simulation;

import com.intentmanagement.core.Intent;
import java.util.Random;

/** Synthetic simulator; values are not measurements from a physical 5G network. */
public class NetworkSimulator {
    private final Random random;
    public NetworkSimulator(long seed){random=new Random(seed);}
    public void measure(Intent i, double totalCapacityMbps, double allocatedMbps) {
        double load=Math.min(1.5,allocatedMbps/Math.max(1,totalCapacityMbps));
        i.measuredLatencyMs=Math.max(1,i.targetLatencyMs*(0.72+load*0.55)+(random.nextDouble()*8-4));
        i.measuredThroughputMbps=Math.max(0,Math.min(allocatedMbps,i.requiredBandwidthMbps*(0.92+random.nextDouble()*0.15)));
        i.measuredPacketLossPercent=Math.max(0,Math.min(100,0.15+load*1.8+random.nextDouble()*0.5));
    }
}
