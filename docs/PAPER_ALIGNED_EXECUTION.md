# Paper-aligned execution flow (prototype)

The CSV page is the execution entry point. The intended flow is:

1. **Intent expression/input:** user uploads a CSV with `id,intent`.
2. **Intent understanding:** rule-based parser extracts service and explicit latency/bandwidth/loss/priority values.
3. **Knowledge-driven completion:** RDF/Jena and the local service graph supply configured defaults for missing fields.
4. **Verification and feasibility:** configured validation/SHACL checks and admission/resource-conflict checks determine ACTIVE, REJECTED, or NEEDS_REVIEW.
5. **Deployment:** only ACTIVE intents are passed to the batch ns-3 runner; rejected/ambiguous rows are recorded as not deployed.
6. **Assurance:** FlowMonitor CSVs provide measured delay, throughput and packet loss.
7. **Compliance analysis:** target and measured metrics are compared and summarized by box plots.
8. **Optimization/re-optimization:** research equation and SPSA/MGDA demo components are available elsewhere in the prototype, but are not yet connected as a verified closed-loop controller that changes LTE scheduler parameters and reruns ns-3.

## Important research boundary

This execution order is aligned with the broad intent lifecycle in the reference paper, but it is **not a faithful reproduction** of the paper. The parser is rule-based; the KG2E implementation is a prototype/approximation; the ns-3 scenario is LTE/EPC rather than a verified reproduction of the paper's 5G NSA setup; and SPSA/MGDA are not a demonstrated end-to-end network-control loop. The ns-3 scenario records target latency/loss/priority as metadata; it does not by itself guarantee those targets or enforce them through a custom scheduler.

## Outputs

- `results/processed_intents.csv`: extracted/completed fields, validation status, and lifecycle stage.
- `results/ns3-batch-kpis.csv`: per-flow KPI output and explicit non-deployment statuses for rejected/review rows.
- `results/ns3-batch/`: per-intent simulation CSV and log files.

For scientific reporting, retain failed and rejected rows in summary counts. Do not interpret missing KPI values as zero.
