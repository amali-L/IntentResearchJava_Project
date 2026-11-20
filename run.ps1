$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Apache Maven is required. See docs/JENA_SETUP.md.' }
mvn -DskipTests package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
java -jar target/knowledge-driven-intent-management-1.1.0.jar
