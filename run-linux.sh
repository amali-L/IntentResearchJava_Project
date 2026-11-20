#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v mvn >/dev/null || { echo "Maven is required. See docs/JENA_SETUP.md"; exit 1; }
mvn -DskipTests package
java -jar target/knowledge-driven-intent-management-1.1.0.jar
