# Paper-alignment implementation map

Reference: *Knowledge-Driven Intent Life-Cycle Management for Cellular Networks*. This document distinguishes the paper's method from this project's current implementation.

## Lifecycle alignment

| Paper step | Project implementation | Evidence/output | Status |
|---|---|---|---|
| A — Processing of user intent | `IntentParser` and `UserIntentCsvProcessor` | `id,intent` input and extracted service/QoS fields | Partial: regex/rule-based, not the paper's hybrid HMM NER |
| B — Creation of service intent template | RDF service profiles in `data/ontology/intent-schema.ttl` | Service profile selected from RDF aliases | Partial |
| C — Identification of incomplete intent triples | User-specified values compared with service defaults in `IntentManager.previewIndependent` | Missing fields filled from template | Partial |
| D — Link prediction using pretrained KGE | `KG2EModel` / RDF matching | Prototype scoring/training | Not paper-validated; no matching authors' trained model/data |
| E — Completion of service intent template | KG profile defaults + extracted parameters | `results/processed_intents.csv` | Partial |
| F — Verification of network intent | SHACL and per-service feasibility checks | ACTIVE / REJECTED / NEEDS_REVIEW | Partial |
| G — Deployment and optimization | Per-intent service-specific ns-3 LTE/EPC run and FlowMonitor output | `results/ns3-batch-kpis.csv`, per-run logs | Deployment/monitoring partial; optimization is not a closed-loop FQ-CoDel controller |

## Batch experiment semantics

Each CSV row is handled as an independent candidate and, when validated, as an independent simulation scenario. This is suitable for measuring a distribution across input intents, but it is **not** equivalent to the paper's coexisting-intent experiments or arrival-triggered re-optimization scenario. A separate simultaneous-intent experiment should model service arrivals and resource competition in one simulation timeline.

## Required next steps for exact reproduction

1. Reproduce the paper's HMM-based NER/bootstrapping and label/evaluate extraction.
2. Train and validate KG2E using the paper's triple data, negative sampling/OWA protocol, scoring function and hyperparameters.
3. Recreate the authors' 5G NSA simulation setup, service traffic, FQ-CoDel target queue settings, and simulation timing.
4. Implement Algorithm 2 from the paper so SPSA gradient estimates and MGDA weights update the actual queue-target/control parameters.
5. Run repeated seeds and reproduce the paper's compliance/convergence and ECDF/box-plot analyses against baselines.

The current source intentionally does not claim those uncompleted items as implemented.
