# User-provided intent CSV workflow

## Input contract
Upload a CSV with `id` and `intent` headers (case-insensitive). Quoted commas and escaped quotes are supported. Embedded line breaks inside a quoted intent are not supported by the current line-oriented reader.

## Process every row
1. Open the **User Intent CSV** dashboard page.
2. Click **Upload intent CSV** and select your own file (the included `data/datasets/templates/user_intents_1000.csv` is an example with 1,000 rows).
3. Click **Process + export CSV**.
4. The rule-based parser and RDF/Knowledge Graph service profiles extract a service and complete missing service parameters where possible.
5. The original input is not overwritten. Processed data is written to `results/processed_intents.csv`.
6. The initial box plots summarize the processed target/request distributions for latency, bandwidth and packet loss.

## Simulate all processed intents
Click **Simulate ALL intents in ns-3** after processing. The application runs one short ns-3 scenario per CSV row sequentially in a background worker, displays progress, and continues after individual failures. The current batch runner uses a 3-second simulation per row to keep the batch manageable; 1,000 rows can still take a substantial amount of wall-clock time. A cancel button stops the batch.

Outputs:
- `results/ns3-batch-kpis.csv`: consolidated per-flow KPIs with the originating intent ID/text and processing/simulation status.
- `results/ns3-batch/`: per-intent KPI CSV and log files.
- `results/ns3-intent-kpis.csv` and `results/ns3-run.log`: latest individual run files from the runner.

After a batch finishes, the chart changes to paired box plots for target/requested versus measured values: target latency vs ns-3 mean delay, requested bandwidth vs ns-3 throughput, and target packet loss vs ns-3 packet loss. It uses available successful simulation rows only.

## Interpretation and limitations
- Parsing is rule-based and knowledge-graph-assisted, not a general-purpose NLP model. Review `NEEDS_REVIEW` rows.
- Every row is attempted in batch mode, including rows marked `NEEDS_REVIEW` or `REJECTED`; missing or invalid numeric values use documented safe fallback values (150 ms latency, 1% loss, 1 Mbps requested bandwidth, priority 1) for simulation. The original processing status is retained in the report.
- The ns-3 scenario records target latency/loss/priority as metadata. This scaffold does not enforce these targets through a custom LTE scheduler. Results are simulation observations, not proof that the targets were achieved.
- The scenario is an LTE/EPC packet-level prototype, not a verified reproduction of the paper's complete 5G NSA testbed.
- The box plots are a custom research-style comparison, not a claim to reproduce an exact figure from the IEEE paper.
- WSL/ns-3 must be installed and the path in `NS3_ROOT` (or the Windows default `/home/Amali/ns-3.49`) must be correct. Full end-to-end build and batch runtime still need to be verified on the target Windows/WSL machine.
