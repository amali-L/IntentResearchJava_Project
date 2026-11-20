# Paper alignment: implemented changes and remaining validation

## What was changed in this revision
- Added `HmmServiceRecognizer`, a bootstrapped first-order HMM service-class recognizer and connected it as a fallback in `IntentParser` when RDF service matching returns `General`.
- Kept explicit extraction of numeric latency, bandwidth and loss values.
- Kept CSV-first batch processing and per-intent ns-3 execution/reporting from the base project.
- Cleaned the Swing panel imports to avoid `java.awt.List` versus `java.util.List` ambiguity.

## Important distinction from the paper
This HMM is a small bootstrapped baseline trained from seed phrases in source code. It is NOT the paper's original trained NER model and must be trained/evaluated on a labelled domain corpus before a paper-reproduction claim. The KG2E class remains a diagonal-Gaussian prototype and is not the paper's complete OWA positive/negative triple training setup unless the code and experiment are extended accordingly. The ns-3 scenario is LTE/EPC and is not, by itself, proof of reproducing the paper's 5G NSA topology. SPSA/MGDA calculations in the project are not yet a verified controller loop that tunes actual network parameters and reruns ns-3.

## Paper workflow to validate
1. CSC intent submission.
2. NER and service-template generation.
3. KGE training and validation on service orchestration KG triples.
4. Completion via ranked predictions for incomplete triples, filtered by NER context and verified against the KG.
5. Intent deployment in the target network environment.
6. QoS measurement and loss calculation for active intents.
7. SPSA perturbation/measurement, MGDA objective weighting, parameter update, and closed-loop re-evaluation.
8. Baseline and proposed-algorithm evaluation across strict-latency (SL) and non-strict-latency (NSL) mixes and increasing numbers of intents.

## Novelty extension (proposal, not a validated claim)
A conflict-aware, explainable admission policy can be evaluated as an extension: explain why intents are admitted/rejected/deferred, apply explicit priority and resource constraints, then compare it against first-come-first-served and paper-baseline policies. Report successful/failed simulation counts and KPI distributions from real simulator outputs. Do not report synthetic or target values as measured KPIs.

## Verification status
The code was edited in this package, but a full Maven build, ns-3 build, and 1,000-run end-to-end experiment have not been executed in this environment. Run `mvn clean package -DskipTests` and the ns-3 validation commands in `docs/USER_INTENT_CSV_WORKFLOW.md` on the target machine.
