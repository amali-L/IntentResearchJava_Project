# Apache Jena setup (required for RDF/SPARQL)

This revision uses Apache Jena ARQ 5.6.0 for RDF loading and SPARQL 1.1 queries, plus Jena SHACL for ontology constraint validation. Jena 5.6.0 requires a supported modern JDK; this project targets Java 21.

## Windows / VS Code
1. Install JDK 21 and Apache Maven 3.9+.
2. Restart VS Code so `java`, `javac`, and `mvn` are on PATH.
3. From the project root run `mvn -U -DskipTests package` or `./build.ps1`.
4. Run `java -jar target/knowledge-driven-intent-management-1.1.0.jar`.

The first Maven build needs internet access to download Jena and its transitive dependencies. Subsequent builds use the local Maven cache.

## RDF commands

```powershell
java -jar target/knowledge-driven-intent-management-1.1.0.jar --validate-ontology
java -jar target/knowledge-driven-intent-management-1.1.0.jar --rdf-query Emergency
java -jar target/knowledge-driven-intent-management-1.1.0.jar --evaluate-matching
java -jar target/knowledge-driven-intent-management-1.1.0.jar --self-test
```

The ontology is `data/ontology/intent-schema.ttl`; SHACL constraints are `data/ontology/shapes/service-shapes.ttl`. The labelled dataset is `data/datasets/labelled_intent_matching.csv`. The evaluator uses rows marked `test` and writes `results/service-matching-evaluation.txt`.

## Scope and caveats

The current ontology matcher performs exact normalized phrase/alias matching with evidence scores; it is not a learned semantic embedding classifier. SHACL validates schema constraints. The evaluation data is synthetic and template-generated. Use a separately annotated, independently collected dataset for scientific claims.
