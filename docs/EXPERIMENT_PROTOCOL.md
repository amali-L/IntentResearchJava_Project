# Reproducible experiment protocol

1. Fix and record the random seed for every run.
2. Use the same scenarios, total capacity, intent set, and QoS targets for all methods.
3. Compare at minimum: (A) no optimization; (B) SPSA only; (C) MGDA without priority weighting; (D) paper-style priority scaling; (E) proposed conflict-aware admission/preemption and explanation. Keep identical intent arrivals, capacities, traffic profiles and seeds.
4. For each method/run, record deployment success, invalid deployment count, detected conflicts, true/false positives and negatives against scenario ground truth, SLA violation observations, average latency, packet loss, utilization, and processing time.
5. Run at least 30 seeds per scenario when computationally feasible; report mean and standard deviation or confidence intervals.
6. Tune thresholds on validation scenarios, not the final test scenarios.
7. Do not compare different methods using different input scenarios or resource capacity.
8. Preserve raw CSV results and the experiment configuration with every run.

For the proposed novelty, include a high-priority arrival while lower-priority flows are active; record preempted/rejected intents and time-to-recovery. Also include an ablation with preemption disabled. Do not claim statistical improvement from a single run.

Suggested metrics:
- Precision = TP/(TP+FP), with undefined denominators reported as N/A.
- Recall = TP/(TP+FN), with undefined denominators reported as N/A.
- Invalid deployment rate = invalid deployments / attempted deployments.
- SLA violation rate = violating monitored observations / all monitored observations.
- Utilization = allocated capacity / total capacity.
