# IntentOps — Knowledge-Driven Intent Lifecycle Research Prototype

A Java 21 Swing desktop application with a high-contrast dashboard, dynamic user intents, Apache Jena RDF/OWL data, SPARQL alias/profile retrieval, SHACL constraint validation, labelled service-matching evaluation, intent lifecycle and conflict-aware admission, executable equation traces, dataset preprocessing, and an optional ns-3.49 LTE/EPC scenario launcher.

> **Research integrity:** this is a research prototype, not a validated faithful reproduction of the complete IEEE paper. Apache Jena RDF/SPARQL and SHACL are integrated into dynamic service matching and completion. KG2E training remains a simplified diagonal-Gaussian/ranking-inspired approximation. The equation trace executes KG2E score/ranking loss, KPI MSE, priority-weighted loss, SPSA, MGDA and a combined update on Java-simulator KPI snapshots; the combined update is not yet calibrated as a closed-loop controller driven by ns-3 measurements. The proposed novelty is conflict-aware admission/preemption with explanation and requires baseline/ablation experiments. All bundled datasets are synthetic, not operator data.

## Requirements
- JDK 21 and Apache Maven 3.9+ installed; `java`, `javac`, and `mvn` must be on PATH.
- Internet access for the first Maven build to download Apache Jena dependencies.
- Windows: VS Code PowerShell terminal.
- Optional packet-level simulation: ns-3.49 installed and built in WSL2 Ubuntu (you have verified `./ns3 run hello-simulator` and the `simulator` test on your machine).

## Open and run in VS Code (Windows)
1. Extract the ZIP completely.
2. In VS Code choose **File → Open Folder** and select `IntentResearchJava`.
3. Open **Terminal → New Terminal** (PowerShell).
4. Build: `./build.ps1` (runs Maven and packages Jena dependencies).
5. Run GUI: `java -jar target/knowledge-driven-intent-management-1.1.0.jar`
6. Run self-tests: `java -jar target/knowledge-driven-intent-management-1.1.0.jar --self-test`
7. Run CLI demonstration: `java -jar target/knowledge-driven-intent-management-1.1.0.jar --cli-demo`

Alternatively run `./run.ps1` to build and launch the GUI in one step. On Linux/macOS, use `./run-linux.sh`. See `docs/JENA_SETUP.md` if Maven is not installed.

## Apache Jena RDF, SPARQL, SHACL and matching evaluation
- RDF ontology: `data/ontology/intent-schema.ttl` (service classes, aliases, QoS values, resources, compatibility/conflict relations).
- SHACL shapes: `data/ontology/shapes/service-shapes.ttl` (positive latency/bandwidth, bounded loss/priority, required aliases).
- `JenaKnowledgeBase.java`: loads the ontology with Jena, queries profiles/aliases using SPARQL, ranks exact normalized phrase matches, and exposes SHACL validation.
- Labelled data: `data/datasets/labelled_intent_matching.csv` contains 1,000 synthetic labelled examples with `train`/`test` splits. Evaluation uses only the 200 `test` rows and reports accuracy, macro precision, macro recall, macro F1 and a confusion table.

Commands:
```powershell
java -jar target/knowledge-driven-intent-management-1.1.0.jar --validate-ontology
java -jar target/knowledge-driven-intent-management-1.1.0.jar --rdf-query Emergency
java -jar target/knowledge-driven-intent-management-1.1.0.jar --evaluate-matching
```

The benchmark is generated from the same curated alias vocabulary and is intended as a pipeline sanity check, not an independent scientific test. For research claims, create a separately annotated test set that includes unseen paraphrases, ambiguous intents, out-of-domain examples and a second annotator.

## Try dynamic intents
In **Intent lifecycle**, enter examples such as:
- `Emergency MCPTT, latency 20ms, bandwidth 30Mbps, critical`
- `Video streaming, latency 80ms, bandwidth 12Mbps`
- `IoT telemetry`

Explicit values are preserved. Missing latency, bandwidth, packet-loss target, or priority are completed from matching knowledge-graph service triples. Search triples in the Knowledge graph tab. Capacity conflicts are checked and high-priority feasible requests may preempt lower-priority active requests under the prototype policy.

## Dataset (1,000 rows included)
`data/datasets/` contains:
- `raw_intents.csv` — 1,000 generated synthetic intent records with some deliberately missing KPI fields.
- `preprocessed_intents.csv` — 1,000 rows after template-based imputation, numeric validation, and z-score standardization columns.
- `generated_kg_triples.tsv` — service templates and generated graph links.
- `preprocessing_summary.txt` — seed, summary statistics, and preprocessing policy.

The GUI has a **Generate 1,000 synthetic examples** action. To regenerate from PowerShell:

```powershell
java -jar target/knowledge-driven-intent-management-1.1.0.jar --generate-dataset 1000
```

The compliance label is synthetic and rule-derived; do not treat it as real-world ground truth.

## Connect Java GUI to your ns-3.49 in WSL2
Your ns-3 root from the verified terminal is `/home/Amali/ns-3.49`. In the **same VS Code PowerShell terminal** from which you launch the GUI, set:

```powershell
$env:NS3_ROOT = '/home/Amali/ns-3.49'
java -jar target/knowledge-driven-intent-management-1.1.0.jar
```

Use the **Intent lifecycle** tab to submit a dynamic intent first. Then go to **Dataset & ns-3 → Run ns-3 simulation**. The adapter uses the latest submitted intent's service type, bandwidth, latency target, packet-loss target, and priority as simulation metadata; its requested bandwidth influences the corresponding UDP offered rate and the generated UE count. It copies `ns-3/scratch/intent-lifecycle.cc` into the WSL ns-3 `scratch/` directory, launches `./ns3 run ...`, waits up to 15 minutes, and writes:
- `results/ns3-run.log`
- `results/ns3-intent-kpis.csv`
- ns-3 FlowMonitor XML in the ns-3 root directory

The CSV reports per-flow transmitted/received/lost packets, mean delay, throughput and packet-loss percentage, alongside intent metadata. These are ns-3 simulation measurements, not live-network observations. If the GUI reports a failure, inspect `results/ns3-run.log` first.

### Run scenario directly in Ubuntu (optional)
From Ubuntu:

```bash
cd ~/ns-3.49
cp /mnt/c/Users/ACER/Downloads/IntentResearchJava/ns-3/scratch/intent-lifecycle.cc scratch/intent-lifecycle.cc
./ns3 run "scratch/intent-lifecycle --nUes=4 --simTime=12 --voiceRate=256kbps --videoRate=4Mbps --intentService=Emergency --targetLatencyMs=20 --targetLossPct=0.5 --intentPriority=5 --output=/home/Amali/intent-lifecycle-kpis.csv"
```

Replace the example Windows project path if you extracted the ZIP elsewhere. For Java-triggered runs, the adapter copies the scenario automatically; the manual copy is only for direct testing.

## Modules
- `core/`: parser, intent lifecycle manager, conflict/admission logic.
- `knowledge/`: triple store, TSV loading/search, KG2E-style prototype.
- `optimization/`: QoS MSE, SPSA and MGDA demonstration utilities.
- `dataset/`: deterministic synthetic dataset generation and preprocessing.
- `simulation/`: local synthetic KPI simulator and ns-3 process adapter.
- `reporting/`: CSV export.
- `ns-3/scratch/intent-lifecycle.cc`: independent LTE/EPC + UDP + FlowMonitor scenario.
- `docs/MATHEMATICS.md`, `docs/PAPER_PIPELINE.md`, `docs/EXPERIMENT_PROTOCOL.md`: equation mapping, pipeline and evaluation plan.

## Verification performed while packaging
- Compile and package using Maven: `mvn -DskipTests package`.
- Run the self-test using the packaged Jena-inclusive JAR: `java -jar target/knowledge-driven-intent-management-1.1.0.jar --self-test`.
- Dataset regenerated: exactly 1,000 raw rows and 1,000 preprocessed rows (excluding header).
- ns-3 itself has been verified by the user on their machine (`Hello Simulator`; `PASS simulator`). The custom C++ scenario has **not** been executed in this packaging environment; compile/run it through the configured local ns-3 before using its output in experiments.


## Existing dataset pattern processing

The default GUI workflow uses `data/datasets/labelled_intent_matching.csv`; it does not create another 1,000-row dataset. Choose **Analyze existing dataset → KG patterns** to count service-class records, extract frequent descriptive tokens, add `datasetObservedIntentCount` and `observedIntentPattern` facts to the runtime `KnowledgeGraph`, and write `results/dataset_pattern_analysis.txt` plus `results/dataset_pattern_triples.tsv`. These are descriptive features of the included labelled corpus, not independently validated scientific findings.

## Windows + WSL ns-3

Set the Linux path in the same PowerShell terminal before starting the app: `$env:NS3_ROOT = "/home/Amali/ns-3.49"`. The adapter normalizes leading-slash WSL paths that Windows Java may render with backslashes, calls `wsl.exe bash -lc`, copies the scenario into the ns-3 scratch directory, and writes output under the Windows project `results` folder via `/mnt/<drive>/...`. Verify `wsl bash -lc 'cd /home/Amali/ns-3.49 && ./ns3 run hello-simulator'` first. The scenario still needs a real run on the user's machine; do not describe ns-3 integration as validated until `results/ns3-intent-kpis.csv` is produced.

## Updated UI and local account flow
- The app now opens a red/black sign-in window before the research dashboard.
- Create account, sign in, and forgot/reset password are included. Accounts are local to `data/auth/users.properties`; passwords are stored as salted PBKDF2 hashes.
- Password reset code is shown inside the prototype dialog; SMTP/email delivery is not configured. Do not treat this as production identity infrastructure.
- The dashboard tab strip uses a dark background, readable light labels, and a red selected tab to prevent white-on-white tab labels under Windows Look & Feel.
- `Paper KPI comparison` plots target/allowed and observed latency, throughput, and packet loss for the current in-memory intent records. Submit intents and run monitoring first. Java measurements are synthetic; use the ns-3 tab for packet-level simulation output.


## Windows quick start (full-screen red/black edition)

1. Extract the ZIP and open the `IntentResearchJava` folder that contains `pom.xml` in VS Code.
2. Open PowerShell in that folder.
3. Check Java: `java -version`.
4. Ensure Apache Maven is installed. `build.ps1` checks `PATH` and common local Maven locations.
5. Build: `.\build.ps1`
6. Launch from the project root: `java -jar target\knowledge-driven-intent-management-1.1.0.jar`

Run from the project root so the relative `data/`, `results/`, and `data/auth/` paths resolve consistently.

### Authentication notes
- Create an account through **Create Account** before signing in.
- Emails are normalized to lowercase; sign-in is case-insensitive for the email address.
- Passwords require at least 8 characters and are stored as salted PBKDF2 hashes.
- Password recovery is a local prototype: the reset code is shown in the application. Email delivery is not configured.
- If you used a prior broken build, create the account again in this fixed build. Account data is local to the project working directory.

### Research integrity
The project contains a paper-aligned prototype, not a validated exact reproduction of the IEEE paper. The 1,000-row intent dataset is synthetic; Java simulator values are not ns-3 measurements. The ns-3 adapter requires a working ns-3/WSL installation. See `docs/IMPLEMENTATION_STATUS.md`, `docs/MATHEMATICS.md`, and `docs/NS3_SETUP.md` for current limitations and validation steps.


## Button-triggered ns-3 simulation for a submitted intent

1. Start the app from the project root using `.\build.ps1` (Maven must be installed).
2. In **Intent lifecycle**, enter an intent such as `Emergency voice service, latency 50ms, bandwidth 2Mbps, priority 5`.
3. Click **Submit intent + run ns-3**. The app sends the latest submitted intent's service, QoS target metadata, priority, and bandwidth-derived traffic rate to the ns-3 runner through WSL2.
4. On this machine, the default ns-3 root is `/home/Amali/ns-3.49`; if yours differs, set `NS3_ROOT` to the correct WSL Linux path before starting Java.
5. Results are written to `results/ns3-intent-kpis.csv` and `results/ns3-run.log`.

The simulator must already be installed and buildable in WSL. The included scenario is a research scaffold; target latency, target loss, and priority are passed as metadata, not enforced by a priority-aware LTE scheduler.

## User-provided intent CSV workflow

The **User Intent CSV** tab accepts any CSV whose header contains `id,intent` (case-insensitive). Each row's free-text intent is passed through the existing Java intent parser, RDF/knowledge-graph service matching and configured validation. It exports a separate `results/processed_intents.csv` with extracted service, latency, bandwidth, packet-loss target, priority, status and explanation. Ambiguous `General` service matches are marked `NEEDS_REVIEW` rather than silently treated as confidently understood.

Use **Simulate selected intent** for a single-row check, or **Simulate ALL intents in ns-3** to attempt one separate short LTE/EPC simulation per processed CSV row. Batch KPI output is written to `results/ns3-batch-kpis.csv`; per-intent CSV and log files are stored under `results/ns3-batch/`. The latest individual run is also written to `results/ns3-intent-kpis.csv` and `results/ns3-run.log`. The batch may take substantial time for 1,000 rows and continues after individual failures. ns-3 must build successfully in WSL/Linux before either button can work.

A small input example is provided at `data/datasets/templates/user_intents_template.csv`. The system does not generate or substitute a synthetic 1,000-row input dataset; you upload your own CSV.


## User CSV processing and full ns-3 batch run

The **User Intent CSV** page has a dedicated light, high-contrast page theme and larger fonts. Upload a CSV with `id,intent`, process all rows, and export `results/processed_intents.csv`. Use **Simulate ALL intents in ns-3** to run a separate short ns-3 scenario for every processed row. The consolidated report is `results/ns3-batch-kpis.csv`; individual logs and KPI files are stored under `results/ns3-batch/`. After a batch, the box plots compare target/requested latency, bandwidth and packet loss with the measured ns-3 delay, throughput and packet loss. See `docs/USER_INTENT_CSV_WORKFLOW.md` for interpretation and limitations.


## Paper-aligned execution order

The CSV-first UI follows input → intent extraction → RDF/KG completion → validation/resource feasibility → deploy ACTIVE intents to ns-3 → FlowMonitor KPI collection → QoS comparison/box plots. REJECTED and NEEDS_REVIEW rows remain in the batch report but are not deployed. See `docs/PAPER_ALIGNED_EXECUTION.md` for the workflow and explicit research limitations. This is a paper-inspired prototype, not a verified exact reproduction of KG2E, the paper's SPSA/MGDA lifecycle, or its 5G NSA experiments.


## Paper-alignment status and experimental interpretation

The CSV page follows the paper's broad A–G intent lifecycle: (A) parse intent text, (B) map a service template, (C) identify missing parameters, (D) use knowledge-graph matching, (E) complete the template, (F) verify the request and check per-intent feasibility, and (G) deploy/monitor a service scenario. The input CSV is a project-specific batch interface, not a claim that the paper's own experiments used this 1,000-row file.

Each CSV row is processed as an **independent scenario** so processing a large CSV does not accidentally consume the dashboard's live intent capacity or make later rows fail solely because earlier CSV rows were added to an in-memory list. Therefore, this batch mode does not evaluate simultaneous admission competition between all rows. The ns-3 scenario now selects a voice-like or video/data-like traffic profile based on the intent's service class and records measured FlowMonitor KPIs.

**Research limitations:** the parser is rule-based rather than the paper's hybrid HMM NER; the KG2E class is a diagonal-Gaussian prototype and has not been validated against the paper's training algorithm/dataset; the bundled ns-3 LTE/EPC scenario is not the authors' full 5G NSA experiment; and SPSA/MGDA remain research-inspired calculations rather than a verified closed-loop controller that updates FQ-CoDel queue targets and reruns the network. Do not report this project as an exact paper reproduction until those pieces are implemented and evaluated.


## Paper reproduction status

See `docs/PAPER_REPRODUCTION_STATUS.md` for the implemented CSV-first execution path, safeguards, and exact research-reproduction gaps. The application is paper-inspired; it is not claimed to be an exact reproduction of the authors' HMM/KG2E/5G-NSA/SPSA-MGDA experiments.


See `docs/PAPER_ALIGNMENT_REMAINING_WORK.md` for the added HMM-assisted recognizer, paper-stage mapping, novelty proposal, and explicit validation limits.
