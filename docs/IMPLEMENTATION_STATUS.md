# Implementation status and research integrity

## Added in the RDF research revision
- Apache Jena ARQ 5.6.0 integration: ontology loaded from Turtle; SPARQL queries retrieve service profiles and aliases; normalized phrase matcher returns match score/evidence.
- OWL-style service classes and object relationships (`requiresResource`, `compatibleWith`, `conflictsWith`) in `data/ontology/intent-schema.ttl`.
- SHACL constraints for service profiles and user intent values: positive latency/bandwidth, loss in [0,100], priority in [1,5], required aliases.
- 1,000-row labelled synthetic intent-matching dataset with train/test labels; evaluator uses the 200 held-out rows and reports accuracy, macro precision/recall/F1 and confusion counts.
- Equation trace runs when an admitted intent receives a Java-simulator KPI snapshot: diagonal KG2E score, ranking-inspired loss, KPI MSE, priority-scaled loss, SPSA objective, MGDA weights and a combined policy-parameter update.
- GUI controls for RDF/SPARQL search, ontology validation, labelled dataset evaluation, and viewing the latest equation trace.

## Existing functionality
- Redesigned Java Swing dashboard and dynamic user-entered intent parser.
- Legacy TSV triple store for graph inspection and KG2E-style experiments.
- Conflict-aware capacity admission with preemption/rollback for selected feasible high-priority requests.
- Dataset preprocessing and optional ns-3.49 LTE/EPC scenario adapter.

## Verification status
- Java sources passed a syntax compilation using temporary API stubs for Jena; this is not a substitute for compiling against the real Jena artifacts.
- Both Turtle ontology and SHACL shape files parsed successfully with an independent RDF parser.
- The labelled dataset contains exactly 1,000 rows, including 200 test rows.
- Maven packaging, actual Apache Jena runtime, SHACL report, matcher metrics, and end-to-end GUI execution must be run on a machine with Java 21, Maven and network access to download dependencies. Do not claim those runtime checks passed until `mvn package` and the listed CLI checks pass.

## Still incomplete relative to the IEEE paper
- KG2E training is not yet a faithful implementation of all paper Eq. (3)/(4) training details and comparisons with TransD, TransH and ComplEx.
- SPSA+MGDA calculations are now executed in a trace, but the complete paper Algorithm 2—including its exact scaling factors, curvature/Hessian handling and stopping criteria—is not fully reproduced. The combined policy update is not yet fed back into the ns-3 scheduler.
- The ns-3 scenario is independently authored and not the authors' original source. Its topology, traffic, radio assumptions, queue discipline and timings need calibration against the paper before claiming reproduction.
- Java does not yet automatically import ns-3 KPI output into the live intent model and close the optimizer feedback loop over those measurements.
- The labelled intent dataset is synthetic and generated from the same alias vocabulary; it validates the pipeline, not generalization to real unseen user language. A separately annotated test set is required for scientific claims.
- MySQL runtime persistence is not integrated.

The project is a research prototype, not a completed or validated reproduction. The proposed novelty—conflict-aware, priority-sensitive admission with explanations—requires literature review, baselines, ablations and multi-seed experiments before being claimed as a validated contribution.


## Current workflow adjustment

- The GUI now analyzes the existing `labelled_intent_matching.csv` instead of offering 1,000/10,000/100,000-row generation buttons. It derives service-specific token patterns, injects them into the in-memory graph, and writes a report plus TSV facts.
- The ns-3 adapter normalizes leading-slash Linux paths that Windows Java may render with backslashes, allowing `/home/...` roots to select the WSL execution branch. Runtime integration must still be tested on the target Windows/WSL installation.
