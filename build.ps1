$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$maven = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $maven) {
  $candidates = @(
    'C:\Tools\apache-maven-3.10.0\bin\mvn.cmd',
    'C:\Tools\apache-maven-3.9.9\bin\mvn.cmd',
    'C:\Tools\apache-maven-3.9.11\bin\mvn.cmd',
    "$env:USERPROFILE\apache-maven\bin\mvn.cmd"
  )
  $found = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
  if ($found) { $mavenPath = $found } else {
    throw 'Apache Maven was not found. Install/extract Maven, then either add its bin folder to PATH or edit this script with the actual path. Official download: https://maven.apache.org/download.cgi. See docs/JENA_SETUP.md.'
  }
} else { $mavenPath = $maven.Source }
& $mavenPath -U -DskipTests package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Build succeeded. Run: java -jar target\knowledge-driven-intent-management-1.1.0.jar' -ForegroundColor Green
