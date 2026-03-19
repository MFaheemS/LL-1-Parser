# Consolidation script to run all grammars and merge outputs

$OutputDir = "output"
$TempDir = "temp_output"

# Ensure output directory exists
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

# Clean up any previous temp directory
if (Test-Path $TempDir) {
    Remove-Item $TempDir -Recurse -Force
}
New-Item -ItemType Directory -Path $TempDir -Force | Out-Null

# Grammar definitions
$Grammars = @(
    @{
        Name = "Grammar 1: Simple"
        File = "input/grammar1.txt"
        Input = "input/input_grammar1_simple_valid_invalid_missing_extra_empty.txt"
        TempDir = "$TempDir/g1"
    },
    @{
        Name = "Grammar 2: Expression"
        File = "input/grammar2.txt"
        Input = "input/input_grammar2_expression_valid_invalid_missing_extra_empty_error_recovery.txt"
        TempDir = "$TempDir/g2"
    },
    @{
        Name = "Grammar 3: Statement (Left Factoring)"
        File = "input/grammar3.txt"
        Input = "input/input_grammar3_statement_left_factoring_valid_invalid_missing_extra.txt"
        TempDir = "$TempDir/g3"
    },
    @{
        Name = "Grammar 4: Indirect Left Recursion"
        File = "input/grammar4.txt"
        Input = "input/input_grammar4_indirect_left_recursion_valid_invalid_missing_extra.txt"
        TempDir = "$TempDir/g4"
    }
)

Write-Host "Building project..." -ForegroundColor Cyan
& .\build.bat
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# Run each grammar and collect outputs
$TransformedGrammars = @()
$FirstFollowSets = @()
$ParsingTables = @()
$ParseTraces = @()
$ParseTrees = @()

foreach ($Grammar in $Grammars) {
    Write-Host ""
    Write-Host "Processing $($Grammar.Name)..." -ForegroundColor Cyan
    
    # Create temp directory for this grammar
    New-Item -ItemType Directory -Path $Grammar.TempDir -Force | Out-Null
    
    # Run the parser
    & java -cp out Main $Grammar.File $Grammar.TempDir $Grammar.Input
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error processing $($Grammar.Name)" -ForegroundColor Red
        exit 1
    }
    
    # Collect outputs
    $GrammarName = $Grammar.Name
    
    # Grammar transformed
    $transformedFile = "$($Grammar.TempDir)/grammar_transformed.txt"
    if (Test-Path $transformedFile) {
        $TransformedGrammars += "`n`n" + ("=" * 80)
        $TransformedGrammars += "`n$GrammarName`n"
        $TransformedGrammars += ("=" * 80) + "`n"
        $TransformedGrammars += (Get-Content $transformedFile -Raw)
    }
    
    # First/Follow sets
    $firstFollowFile = "$($Grammar.TempDir)/first_follow_sets.txt"
    if (Test-Path $firstFollowFile) {
        $FirstFollowSets += "`n`n" + ("=" * 80)
        $FirstFollowSets += "`n$GrammarName`n"
        $FirstFollowSets += ("=" * 80) + "`n"
        $FirstFollowSets += (Get-Content $firstFollowFile -Raw)
    }
    
    # Parsing table
    $parsingTableFile = "$($Grammar.TempDir)/parsing_table.txt"
    if (Test-Path $parsingTableFile) {
        $ParsingTables += "`n`n" + ("=" * 80)
        $ParsingTables += "`n$GrammarName`n"
        $ParsingTables += ("=" * 80) + "`n"
        $ParsingTables += (Get-Content $parsingTableFile -Raw)
    }
    
    # Parsing trace
    $parsingTraceFile = "$($Grammar.TempDir)/parsing_trace1.txt"
    if (Test-Path $parsingTraceFile) {
        $ParseTraces += "`n`n" + ("=" * 80)
        $ParseTraces += "`n$GrammarName - Parsing Trace`n"
        $ParseTraces += ("=" * 80) + "`n"
        $ParseTraces += (Get-Content $parsingTraceFile -Raw)
    }
    
    # Parse trees
    $parseTreesFile = "$($Grammar.TempDir)/parse_trees.txt"
    if (Test-Path $parseTreesFile) {
        $ParseTrees += "`n`n" + ("=" * 80)
        $ParseTrees += "`n$GrammarName - Parse Trees`n"
        $ParseTrees += ("=" * 80) + "`n"
        $ParseTrees += (Get-Content $parseTreesFile -Raw)
    }
}

# Write consolidated output files
Write-Host ""
Write-Host "Writing consolidated output files..." -ForegroundColor Cyan

Set-Content -Path "$OutputDir/grammar_transformed.txt" -Value $TransformedGrammars.TrimStart()
Set-Content -Path "$OutputDir/first_follow_sets.txt" -Value $FirstFollowSets.TrimStart()
Set-Content -Path "$OutputDir/parsing_table.txt" -Value $ParsingTables.TrimStart()
Set-Content -Path "$OutputDir/parsing_trace1.txt" -Value $ParseTraces.TrimStart()
Set-Content -Path "$OutputDir/parse_trees.txt" -Value $ParseTrees.TrimStart()

# Clean up temp directory
Remove-Item $TempDir -Recurse -Force

Write-Host ""
Write-Host "Consolidation complete!" -ForegroundColor Green
Write-Host "Output files written to: $OutputDir" -ForegroundColor Green
Write-Host "- grammar_transformed.txt"
Write-Host "- first_follow_sets.txt"
Write-Host "- parsing_table.txt"
Write-Host "- parsing_trace1.txt"
Write-Host "- parse_trees.txt"
