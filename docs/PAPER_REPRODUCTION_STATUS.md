# Paper-alignment and reproducibility status

Reference: Mehmood, Kralevska, and Palma, “Knowledge-Driven Intent Life-Cycle Management for Cellular Networks,” IEEE TNSM, 2025.

## Execution path in this project

1. Upload a user CSV with `id,intent` columns.
2. Extract service/QoS mentions and map common aliases to the supported service catalogue.
3. Retrieve defaults from the RDF/knowledge graph when explicit values are absent.
4. Build a completed intent candidate and run SHACL/policy/capacity verification.
5. Export every input row with status and explanation; rejected/review rows are retained and not batch-deployed.
6. Run an isolated ns-3 scenario for each ACTIVE row and write per-run logs/KPI CSVs.
7. Aggregate successful simulation KPIs and compare target values with measured outputs in box plots.

## Important scope boundary

This is a paper-inspired research prototype, not a faithful reproduction of the authors' experiment. The paper describes a hybrid bootstrapping HMM NER pipeline, a trained KG2E model with its own knowledge-graph triples and evaluation protocol, a 5G NSA deployment setup, and a closed-loop deployment controller using measured service KPIs with SPSA and MGDA. This project does not include the authors' trained model/checkpoints or complete original KG/training corpus, and its current KG2E and optimization code is a prototype. The included ns-3 scenario uses the modules and traffic model present in this repository; it must not be described as the paper's original 5G NSA experiment without verification.

## Experimental safeguards

- CSV rows are evaluated independently by the batch runner; they are not all simultaneous users in one shared simulation.
- A successful Java process or generated CSV is not proof that an intent meets its SLA.
- Only successful ns-3 output rows should enter measured-KPI plots.
- Keep target values and measured values in separate columns; do not label targets as observations.
- Report uploaded, parsed, ACTIVE, REJECTED, NEEDS_REVIEW, simulated, and failed counts.
- Do not claim the paper's KG2E, HMM, SPSA/MGDA, or 5G NSA results unless those methods are implemented and validated against the paper's algorithms, settings, and data.

## Remaining work for a defensible reproduction

1. Recover/construct and document the same service-orchestration KG and train/evaluate KG2E with the paper's scoring/training protocol.
2. Implement the paper's HMM NER bootstrapping and report extraction metrics on a labelled set.
3. Reproduce the paper's 5G NSA topology, traffic arrivals, service mappings, and control parameters in ns-3.
4. Connect SPSA gradient estimates and MGDA weights to actual supported network control parameters.
5. Close the loop: measure -> calculate loss -> update controls -> rerun -> compare baseline vs optimized compliance.
6. Run repeatable experiments across the paper's service/strict-latency combinations and compare against its baselines.

The project labels these gaps explicitly so prototype outputs are not mistaken for published-paper results.
