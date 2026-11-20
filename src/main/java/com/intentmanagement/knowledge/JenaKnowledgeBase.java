package com.intentmanagement.knowledge;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.vocabulary.RDFS;

import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** RDF/OWL knowledge base powered by Apache Jena ARQ (SPARQL) and SHACL validation. */
public final class JenaKnowledgeBase implements AutoCloseable {
    public static final String NS = "https://example.org/intent-management#";
    private final Model model = ModelFactory.createDefaultModel();
    private final Map<String, ServiceProfile> profiles = new LinkedHashMap<>();
    public record ServiceProfile(String service, double latencyMs, double bandwidthMbps, double lossPercent, int priority, String serviceClass) {}
    public record Match(String service, double score, List<String> evidence, ServiceProfile profile) {}

    public JenaKnowledgeBase(Path ontologyPath) {
        if (!Files.exists(ontologyPath)) throw new IllegalArgumentException("Ontology file not found: " + ontologyPath.toAbsolutePath());
        RDFDataMgr.read(model, ontologyPath.toUri().toString());
        loadProfiles();
    }
    private void loadProfiles() {
        String q = "PREFIX im:<"+NS+"> PREFIX rdfs:<"+RDFS.getURI()+"> SELECT ?s ?label ?lat ?bw ?loss ?priority ?class WHERE { ?s a im:Service . ?s rdfs:label ?label ; im:requiresLatencyMs ?lat ; im:requiresBandwidthMbps ?bw ; im:maxPacketLossPercent ?loss ; im:priority ?priority ; im:serviceClass ?class . }";
        try (QueryExecution qe = QueryExecutionFactory.create(q, model)) {
            ResultSet rs=qe.execSelect();
            while(rs.hasNext()) { QuerySolution row=rs.next(); String name=row.getLiteral("label").getString();
                profiles.put(name.toLowerCase(Locale.ROOT), new ServiceProfile(name, row.getLiteral("lat").getDouble(), row.getLiteral("bw").getDouble(), row.getLiteral("loss").getDouble(), row.getLiteral("priority").getInt(), row.getLiteral("class").getString()));
            }
        }
    }
    public Collection<ServiceProfile> profiles() { return Collections.unmodifiableCollection(profiles.values()); }
    public Optional<ServiceProfile> profile(String service) { return Optional.ofNullable(profiles.get(norm(service))); }
    public Optional<Match> match(String rawIntent) {
        if (rawIntent == null || rawIntent.isBlank()) return Optional.empty();
        String text=normalize(rawIntent);
        String q="PREFIX im:<"+NS+"> PREFIX rdfs:<"+RDFS.getURI()+"> SELECT ?label ?alias WHERE { ?s a im:Service ; rdfs:label ?label ; im:hasAlias ?alias . }";
        Map<String, List<String>> aliases=new LinkedHashMap<>();
        try(QueryExecution qe=QueryExecutionFactory.create(q,model)) { ResultSet rs=qe.execSelect(); while(rs.hasNext()){QuerySolution row=rs.next();String name=row.getLiteral("label").getString();aliases.computeIfAbsent(name,k->new ArrayList<>()).add(row.getLiteral("alias").getString());} }
        String best=null; double bestScore=0; List<String> evidence=List.of();
        for(var entry:aliases.entrySet()) {
            List<String> hits=new ArrayList<>(); double score=0;
            String label=normalize(entry.getKey()); if(containsPhrase(text,label)){double labelScore=text.equals(label)?0.99:(label.length()<=3?0.55:0.92);score=Math.max(score,labelScore);hits.add(entry.getKey());}
            for(String alias:entry.getValue()) {String a=normalize(alias);if(containsPhrase(text,a)){double candidate=Math.min(1.0,0.65+Math.min(0.30,a.length()/80.0));if(candidate>score)score=candidate;hits.add(alias);}}
            if(score>bestScore){best=entry.getKey();bestScore=score;evidence=hits;}
        }
        if(best==null) return Optional.empty();
        return Optional.of(new Match(best,bestScore,List.copyOf(evidence),profiles.get(norm(best))));
    }
    /** Execute read-only SPARQL SELECT against the in-memory ontology. */
    public List<Map<String,String>> select(String sparql) {
        Query query=QueryFactory.create(sparql); if(!query.isSelectType())throw new IllegalArgumentException("Only SPARQL SELECT is allowed by this query method.");
        List<Map<String,String>> rows=new ArrayList<>(); try(QueryExecution qe=QueryExecutionFactory.create(query,model)){ResultSet rs=qe.execSelect();while(rs.hasNext()){QuerySolution sol=rs.next();Map<String,String> row=new LinkedHashMap<>();var vars=rs.getResultVars();for(String v:vars)row.put(v,sol.contains(v)?sol.get(v).toString():"");rows.add(row);}}return rows;
    }
    public String queryFacts(String term) {
        String safe=term==null?"":term.toLowerCase(Locale.ROOT);
        String q="PREFIX im:<"+NS+"> PREFIX rdfs:<"+RDFS.getURI()+"> SELECT ?s ?p ?o WHERE { ?s ?p ?o . FILTER(CONTAINS(LCASE(STR(?s)), "+sparqlString(safe)+") || CONTAINS(LCASE(STR(?p)), "+sparqlString(safe)+") || CONTAINS(LCASE(STR(?o)), "+sparqlString(safe)+")) } LIMIT 250";
        StringBuilder b=new StringBuilder("Apache Jena RDF/SPARQL results for: ").append(term).append("\n");for(Map<String,String> row:select(q))b.append(row).append('\n');return b.toString();
    }
    public String validateIntent(double latencyMs,double bandwidthMbps,double lossPercent,int priority,Path shapePath) {
        if(!Files.exists(shapePath))return "SHACL shapes not found: "+shapePath.toAbsolutePath();
        Model shapes=RDFDataMgr.loadModel(shapePath.toUri().toString());
        Model data=ModelFactory.createDefaultModel().add(model);
        Resource request=data.createResource().addProperty(org.apache.jena.vocabulary.RDF.type,data.createResource(NS+"IntentRequest"));
        request.addLiteral(data.createProperty(NS+"targetLatencyMs"),data.createTypedLiteral(latencyMs));
        request.addLiteral(data.createProperty(NS+"requestedBandwidthMbps"),data.createTypedLiteral(bandwidthMbps));
        request.addLiteral(data.createProperty(NS+"requestedMaxPacketLossPercent"),data.createTypedLiteral(lossPercent));
        request.addLiteral(data.createProperty(NS+"requestedPriority"),data.createTypedLiteral(Integer.toString(priority),org.apache.jena.datatypes.xsd.XSDDatatype.XSDinteger));
        ValidationReport report=ShaclValidator.get().validate(shapes.getGraph(),data.getGraph());
        String result=report.getEntries().isEmpty()?"SHACL intent validation PASSED.":"SHACL intent validation FAILED: "+report.getEntries();
        data.close();shapes.close();return result;
    }
    public String validateWithShacl(Path shapePath) {
        if(!Files.exists(shapePath))return "SHACL shapes not found: "+shapePath.toAbsolutePath();
        Model shapes=RDFDataMgr.loadModel(shapePath.toUri().toString());
        ValidationReport report=ShaclValidator.get().validate(shapes.getGraph(),model.getGraph());
        String result=report.getEntries().isEmpty()?"SHACL validation PASSED: ontology service profiles satisfy the configured constraints.":"SHACL validation FAILED:\n"+report.getEntries();
        shapes.close();return result;
    }
    private static String norm(String s){return normalize(s).replace(" ","").toLowerCase(Locale.ROOT);}
    private static String normalize(String s){return Pattern.compile("[^a-z0-9]+",Pattern.CASE_INSENSITIVE).matcher(s.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();}
    private static boolean containsPhrase(String text,String phrase){if(phrase.isBlank())return false;return (" "+text+" ").contains(" "+phrase+" ");}
    private static String sparqlString(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
    @Override public void close(){model.close();}
}
