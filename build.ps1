$ErrorActionPreference = 'Stop'

$sourceDirectory = Join-Path $PSScriptRoot 'src/main/java'
$outputDirectory = Join-Path $PSScriptRoot 'out/classes'
$sourceFiles = @(Get-ChildItem -LiteralPath $sourceDirectory -Filter '*.java' -File -Recurse |
    ForEach-Object { $_.FullName })

if ($sourceFiles.Count -eq 0) {
    throw "No Java source files found in $sourceDirectory"
}

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
& javac --release 21 -encoding UTF-8 -d $outputDirectory @sourceFiles
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Compiled $($sourceFiles.Count) Java source files to $outputDirectory"
