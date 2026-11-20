# Paper pipeline implementation map

The paper separates intent processing/translation (CL1) from deployment and assurance (CL2).

1. **Intent expression** — free-form user text enters `Dashboard`.
2. **Keyword/parameter extraction (NER/template approximation)** — `IntentParser` detects service terms, latency, bandwidth, packet-loss limit and priority. This is deterministic rule-based extraction, not a trained NER model.
3. **Service template completion from KG** — `IntentManager.submit()` queries `KnowledgeGraph` for missing `requiresLatencyMs`, `requiresBandwidthMbps`, `maxPacketLossPercent` and `priority`. Explicit user values take precedence.
4. **KG completion / link prediction** — `KG2EModel` offers a diagonal-Gaussian embedding prototype. It is not a fully paper-faithful training and ranking implementation yet.
5. **Verification** — `ConflictDetectionService` checks capacity and an illustrative critical-latency policy; the graph query UI exposes evidence triples.
6. **Network intent mapping/deployment** — the Java manager reserves abstract bandwidth and creates a lifecycle record. The included `ns-3/scratch/intent-lifecycle.cc` is a separate packet-level LTE/EPC scenario scaffold and currently requires an ns-3 installation and calibration.
7. **Monitoring** — Java's `NetworkSimulator` gives deterministic-seed synthetic KPI values. ns-3 FlowMonitor output is actual simulated packet-level data when that C++ scenario is built and run.
8. **Closed-loop optimization** — SPSA and MGDA components are implemented as independently tested optimizer utilities/demos. Coupling perturbations to ns-3, applying the paper's full Hessian/early-stop logic and re-running deployment is unfinished.
9. **Novelty extension** — conflict-aware admission + priority-sensitive re-optimization + decision explanation. This is a proposal to test, not a validated research result.

## Current boundary

The desktop GUI, KG-backed dynamic defaults, query view, dataset generator/preprocessor, Java optimizer utilities, and ns-3 scenario source are included. Full reproduction still requires implementing exact KG2E training, integrating the two closed loops, orchestrating ns-3 runs from Java, and conducting the paper's baseline/scalability/ablation experiments. Do not claim these unfinished pieces are complete.
