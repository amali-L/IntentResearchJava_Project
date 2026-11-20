package com.intentmanagement.core;

import com.intentmanagement.knowledge.*;import com.intentmanagement.optimization.*;import com.intentmanagement.simulation.NetworkSimulator;import java.util.*;

public class IntentManager {
    public final List<Intent> intents=new ArrayList<>();
    public final KnowledgeGraph graph=new KnowledgeGraph();
    public final KG2EModel kg2e=new KG2EModel(8,42);
    private final IntentParser parser=new IntentParser(); private final com.intentmanagement.knowledge.JenaKnowledgeBase rdfGraph; private final ConflictDetectionService conflict=new ConflictDetectionService(); private final NetworkSimulator simulator=new NetworkSimulator(2026);
    private final double capacityMbps; private final com.intentmanagement.optimization.ResearchEquationPipeline equationPipeline=new com.intentmanagement.optimization.ResearchEquationPipeline(); private final List<com.intentmanagement.optimization.ResearchEquationPipeline.Trace> equationTraces=new ArrayList<>();
    public IntentManager(double capacityMbps){if(capacityMbps<=0)throw new IllegalArgumentException("capacity must be positive");this.capacityMbps=capacityMbps;
        try { java.nio.file.Path kgFile=java.nio.file.Path.of("data","knowledge-graph","sample-triples.tsv");if(java.nio.file.Files.exists(kgFile))graph.loadTsv(kgFile); } catch(java.io.IOException ex){System.err.println("KG TSV load warning: "+ex.getMessage());}
        com.intentmanagement.knowledge.JenaKnowledgeBase loaded=null;
        try { java.nio.file.Path ontology=java.nio.file.Path.of("data","ontology","intent-schema.ttl");if(java.nio.file.Files.exists(ontology))loaded=new com.intentmanagement.knowledge.JenaKnowledgeBase(ontology); } catch(RuntimeException ex){System.err.println("Apache Jena RDF graph warning: "+ex.getMessage());}
        rdfGraph=loaded;
    }
    public Optional<com.intentmanagement.knowledge.JenaKnowledgeBase.Match> matchIntentWithRdf(String text){return rdfGraph==null?Optional.empty():rdfGraph.match(text);}
    public String rdfGraphStatus(){return rdfGraph==null?"Apache Jena ontology unavailable; lexical triple-store fallback active.":"Apache Jena RDF graph active; service profiles="+rdfGraph.profiles().size();}
    public String validateOntology(){return rdfGraph==null?rdfGraphStatus():rdfGraph.validateWithShacl(java.nio.file.Path.of("data","ontology","shapes","service-shapes.ttl"));}
    public String queryRdf(String text){return rdfGraph==null?rdfGraphStatus():rdfGraph.queryFacts(text);}
    public synchronized Optional<com.intentmanagement.optimization.ResearchEquationPipeline.Trace> latestEquationTrace(){return equationTraces.isEmpty()?Optional.empty():Optional.of(equationTraces.get(equationTraces.size()-1));}
    public synchronized String runEquationPipeline(){return latestEquationTrace().map(t->"EXECUTABLE EQUATION TRACE\nEq.3 KG2E score (diagonal approximation)="+t.kg2eScore()+"\nEq.4 ranking loss (prototype)="+t.eq4RankingLoss()+"\nEq.5 KPI MSE="+t.eq5Mse()+"\nEq.6 weighted cumulative loss="+t.eq6WeightedCumulativeLoss()+"\nEq.9 SPSA objective="+t.eq9InitialObjective()+" -> "+t.eq9FinalObjective()+"\nEq.11 MGDA weights="+java.util.Arrays.toString(t.eq11MgdaWeights())+"\nEq.12 updated policy parameters="+java.util.Arrays.toString(t.eq12UpdatedParameters())+"\nEq.6 cumulative loss across active intents="+equationPipeline.weightedCumulativeLoss(intents)+"\n\n"+t.note()).orElse("Submit an intent first to calculate KPI-driven equations.");}
    /**
     * Processes one CSV row as an independent candidate, without mutating the live dashboard's
     * active-intent list. This allows reproducible per-intent ns-3 runs while still applying
     * service-template completion, SHACL checks, and single-intent capacity/policy feasibility.
     * It is intentionally not a simultaneous multi-intent admission test.
     */
    public synchronized Intent previewIndependent(String text) {
        String rdfService = matchIntentWithRdf(text)
                .map(com.intentmanagement.knowledge.JenaKnowledgeBase.Match::service).orElse(null);
        Intent i = parser.parse(text, graph, rdfService);
        String lower = text.toLowerCase(Locale.ROOT);
        if (!lower.matches("(?s).*\\d+(?:\\.\\d+)?\\s*ms.*"))
            i.targetLatencyMs = kgNumber(i.serviceType, "requiresLatencyMs", i.targetLatencyMs);
        if (!lower.matches("(?s).*\\d+(?:\\.\\d+)?\\s*(mbps|mb/s).*"))
            i.requiredBandwidthMbps = kgNumber(i.serviceType, "requiresBandwidthMbps", i.requiredBandwidthMbps);
        if (!lower.matches("(?s).*\\d+(?:\\.\\d+)?\\s*(%|percent).*"))
            i.maxPacketLossPercent = kgNumber(i.serviceType, "maxPacketLossPercent", i.maxPacketLossPercent);
        if (!(lower.contains("critical") || lower.contains("highest priority") || lower.contains("high priority")))
            i.priority = (int) kgNumber(i.serviceType, "priority", i.priority);
        if (rdfGraph != null) {
            String shapeResult = rdfGraph.validateIntent(i.targetLatencyMs, i.requiredBandwidthMbps,
                    i.maxPacketLossPercent, i.priority,
                    java.nio.file.Path.of("data", "ontology", "shapes", "service-shapes.ttl"));
            if (!shapeResult.contains("PASSED")) {
                i.status = "REJECTED";
                i.explanation = "Step F verification failed: " + shapeResult;
                return i;
            }
        }
        ConflictDetectionService.Result feasibility = conflict.check(Collections.emptyList(), i, capacityMbps);
        if (!feasibility.feasible()) {
            i.status = "REJECTED";
            i.explanation = "Step F verification/feasibility failed: " + feasibility.explanation();
        } else if ("General".equalsIgnoreCase(i.serviceType)) {
            i.status = "NEEDS_REVIEW";
            i.explanation = "Steps A-E completed with low-confidence service mapping; human review required before Step G deployment.";
        } else {
            i.status = "ACTIVE";
            i.explanation = "Steps A-E: parsed intent, mapped service template, identified missing parameters, applied RDF/KG template completion; Step F: SHACL/policy and single-intent capacity checks passed. Ready for isolated Step G ns-3 evaluation.";
        }
        return i;
    }

    public synchronized Intent submit(String text){String rdfService=matchIntentWithRdf(text).map(com.intentmanagement.knowledge.JenaKnowledgeBase.Match::service).orElse(null);Intent i=parser.parse(text,graph,rdfService);
        // KG-backed template completion: user-specified values win; absent values are fetched from graph.
        String lower=text.toLowerCase(Locale.ROOT);
        if(!lower.matches("(?s).*\\d+(?:\\.\\d+)?\\s*ms.*")) i.targetLatencyMs=kgNumber(i.serviceType,"requiresLatencyMs",i.targetLatencyMs);
        if(!lower.matches("(?s).*\\d+(?:\\.\\d+)?\\s*(mbps|mb/s).*")) i.requiredBandwidthMbps=kgNumber(i.serviceType,"requiresBandwidthMbps",i.requiredBandwidthMbps);
        if(!lower.matches("(?s).*\\d+(?:\\.\\d+)?\\s*(%|percent).*")) i.maxPacketLossPercent=kgNumber(i.serviceType,"maxPacketLossPercent",i.maxPacketLossPercent);
        if(!(lower.contains("critical")||lower.contains("highest priority")||lower.contains("high priority"))) i.priority=(int)kgNumber(i.serviceType,"priority",i.priority);
        if(rdfGraph!=null){String shapeResult=rdfGraph.validateIntent(i.targetLatencyMs,i.requiredBandwidthMbps,i.maxPacketLossPercent,i.priority,java.nio.file.Path.of("data","ontology","shapes","service-shapes.ttl"));if(!shapeResult.contains("PASSED")){i.status="REJECTED";i.explanation="Ontology/SHACL constraint violation: "+shapeResult;intents.add(i);return i;}}
        ConflictDetectionService.Result r=conflict.check(intents,i,capacityMbps);
        // Novelty prototype: preempt lower-priority active intents only for a feasible high-priority request.
        List<Intent> preempted=new ArrayList<>();
        if(!r.feasible() && i.priority>=4 && i.targetLatencyMs<=50 && r.explanation().startsWith("Bandwidth conflict")) {
            List<Intent> candidates=intents.stream().filter(x->x.status.equals("ACTIVE")&&x.priority<i.priority).sorted(Comparator.comparingInt(x->x.priority)).toList();
            for(Intent victim:candidates){victim.status="PREEMPTED";victim.explanation="Temporarily preempted by higher-priority intent "+i.id+" under conflict-aware admission policy.";preempted.add(victim);r=conflict.check(intents,i,capacityMbps);if(r.feasible())break;}
            if(!r.feasible()) for(Intent victim:preempted){victim.status="ACTIVE";victim.explanation="Preemption rolled back because request remained infeasible.";}
        }
        if(!r.feasible()){i.status="REJECTED";i.explanation=r.explanation();}else{i.status="ACTIVE";i.explanation=r.explanation()+" KG template: "+i.serviceType+"; defaults queried from KG for unspecified fields."+(preempted.isEmpty()?"":" Novelty policy preempted lower-priority intents: "+preempted.stream().map(x->x.id).reduce((a,b)->a+", "+b).orElse(""));simulator.measure(i,capacityMbps,i.requiredBandwidthMbps);equationTraces.add(equationPipeline.evaluate(i,graph.find(i.serviceType,null)));}intents.add(i);return i;}
    private double kgNumber(String service,String relation,double fallback){
        if(rdfGraph!=null){var p=rdfGraph.profile(service);if(p.isPresent()){var v=p.get();return switch(relation){case "requiresLatencyMs"->v.latencyMs();case "requiresBandwidthMbps"->v.bandwidthMbps();case "maxPacketLossPercent"->v.lossPercent();case "priority"->v.priority();default->fallback;};}}
        try{return Double.parseDouble(graph.value(service,relation).orElse(Double.toString(fallback)));}catch(NumberFormatException ex){return fallback;}}
    public synchronized void simulateMonitoring(){for(Intent i:intents)if(i.status.equals("ACTIVE")){simulator.measure(i,capacityMbps,i.requiredBandwidthMbps);equationTraces.add(equationPipeline.evaluate(i,graph.find(i.serviceType,null)));double latencyLoss=Math.pow((i.measuredLatencyMs-i.targetLatencyMs)/Math.max(1,i.targetLatencyMs),2);double packetLoss=Math.pow((i.measuredPacketLossPercent-i.maxPacketLossPercent)/Math.max(0.1,i.maxPacketLossPercent),2);if(i.measuredLatencyMs>i.targetLatencyMs||i.measuredPacketLossPercent>i.maxPacketLossPercent)i.explanation=String.format(Locale.ROOT,"SLA warning: normalized latency error %.3f; packet-loss error %.3f. Simulation only.",latencyLoss,packetLoss);}}
    public synchronized double usedBandwidth(){return intents.stream().filter(i->i.status.equals("ACTIVE")).mapToDouble(i->i.requiredBandwidthMbps).sum();}
    public double capacity(){return capacityMbps;}
    public synchronized String trainKg2e(){return "KG2E-style training loss="+String.format(Locale.ROOT,"%.5f",kg2e.train(graph.all(),10,0.001,1.0))+"; entities="+kg2e.entityCount();}
    public synchronized String optimizeDemo(){SPSAOptimizer optimizer=new SPSAOptimizer();double[] initial={0.2,0.7};SPSAOptimizer.Result r=optimizer.optimize(initial,p->Math.pow(p[0]-0.75,2)+Math.pow(p[1]-0.25,2),150,7);double[] weights=new MGDAOptimizer().weights(new double[][]{{1,0},{0,1}},200);return String.format(Locale.ROOT,"SPSA demo loss %.6f -> %.6f; MGDA weights [%.3f, %.3f]. Demonstration objective, not network performance evidence.",r.initialLoss(),r.finalLoss(),weights[0],weights[1]);}
}
