# Mathematical equations and implementation traceability

This document separates equations in Mehmood et al., *Knowledge-Driven Intent Life-Cycle Management for Cellular Networks*, IEEE TNSM 22(5), 2025, from project-specific approximations.

## Paper pipeline and equation map

- **KG definition**: `G=(E,R,F)`, with facts `(h,r,t) ∈ F`. Implemented as `Triple` and `KnowledgeGraph`.
- **Paper Eq. (3), KG2E scoring**: `f_r(h,t) ∝ - μᵀ C⁻¹ μ - ln(det(C))`, where `μ=μ_h-μ_r-μ_t` and `C=C_h+C_r+C_t`. `KG2EModel.score()` approximates this with diagonal covariance: `-Σ(μ_d²/C_d) - Σln(C_d)`. This is a diagonal simplification; it does not fully implement all paper training details.
- **Paper Eq. (4), pairwise ranking objective**: `min_φ Σ_{f+∈F+} Σ_{f−∈F−} max(0, γ - f_r(h,t) + f_{r'}(h',t'))`. `KG2EModel.train()` is currently a simplified ranking-inspired trainer, not an exact implementation of Eq. (4). It must be replaced before claiming faithful KG2E reproduction.
- **Paper Eq. (5), per-intent KPI loss**: `L_i = (1/l) Σ_{j=1..l}(P_{j,k}-T_{j,k})²`. `QoSLossCalculator.mse()` implements ordinary MSE; `normalizedServiceLoss()` is a project-specific normalized multi-KPI loss.
- **Paper Eq. (6), weighted cumulative loss**: `L_c = Σ_{i=1..n} s_i L_i`, with intent scaling factors `s_i∈[0,1]`. The current app does not yet integrate this weighted cumulative loss into its network simulator loop.
- **Paper Eq. (9), SPSA gradient estimate**: `ĝ_{t,i} = [O(θ_t+c_tΔ_t)-O(θ_t-c_tΔ_t)]/(2c_tΔ_{t,i})`, `Δ_{t,i}∈{-1,+1}`. Implemented in `SPSAOptimizer.optimize()` with two objective evaluations per iteration and bounded demo parameters.
- **Paper Eq. (11), MGDA simplex weights**: `min_w ||Σ_i w_i ĝ_i||²`, subject to `Σ_i w_i=1`, `w_i≥0`. `MGDAOptimizer.weights()` approximates this via projected gradient descent.
- **Paper Eq. (12), combined update**: `θ_{t+1}=θ_t-Δ_t w_t ĝ_f`. The app currently has SPSA and MGDA demonstrations but not a faithful, network-measurement-coupled implementation of the complete paper Algorithm 2. Do not describe the demos as complete Algorithm 2 reproduction.

## Proposed novelty (not claimed by the paper)

Conflict-aware intent admission, an explicit cross-intent conflict explanation, and priority-aware re-optimization when a new high-priority intent arrives or monitored KPIs violate targets. Evaluate against (a) no optimization, (b) SPSA only, (c) MGDA without priority weights, (d) paper-style priority scaling, and (e) proposed conflict-aware policy. Report compliance ratio, mean/95th-percentile latency, packet loss, throughput, convergence time, rejected intents, and runtime over multiple independent seeds. A novelty claim requires literature review and statistically defensible experimental results.

## Honest status

The Java application is a functioning educational prototype. The included ns-3 scenario is a separately run C++ scaffold. Neither the Java synthetic simulator nor the generated synthetic dataset is evidence of real 5G performance. For paper-level reproduction, calibrate topology, radio/channel assumptions, service model, queue discipline, optimizer settings, time scales, seeds, and traffic profiles against the paper's Section VI and figures before comparing results.

## Runtime equation execution added in the RDF integration revision

`ResearchEquationPipeline.evaluate()` is called after an admitted intent receives a Java-simulator KPI snapshot. It calculates:

- Eq. (3): `KG2EModel.score()` for a graph fact (diagonal Gaussian approximation).
- Eq. (4): `KG2EModel.train()` returns the current ranking-inspired hinge loss for the service facts (not exact paper training).
- Eq. (5): `QoSLossCalculator.mse()` over observed latency/throughput/loss versus targets. Since these KPI units differ, this raw MSE is an audit value; normalized multi-KPI loss is also computed.
- Eq. (6): per-intent priority-scaled normalized loss and cumulative weighted loss across active intents.
- Eq. (9): `SPSAOptimizer.optimize()` estimates an update direction for a two-parameter surrogate objective based on the KPI losses.
- Eq. (11): `MGDAOptimizer.weights()` computes nonnegative simplex weights for three KPI objective-gradient vectors.
- Eq. (12): combines those MGDA-weighted vectors and calculates a bounded policy-parameter update.

The trace makes the formulas executable and inspectable, but it does not establish exact reproduction of the paper's derivation, hyperparameters or Algorithm 2. In particular, the calculated policy update is presently reported for research inspection rather than fed back into the ns-3 scheduler. That coupling remains a required next integration step.
