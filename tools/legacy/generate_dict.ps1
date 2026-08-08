$corpusPath = "C:\Users\khias\.gemini\antigravity\scratch\train.jsonl"
$enginePath = "f:\likhibi\app\src\main\java\com\likhibi\keyboard\NagameseOfflineEngine.kt"
$backupPath = "f:\likhibi\app\src\main\java\com\likhibi\keyboard\NagameseOfflineEngine.kt.bak"

if (-not (Test-Path $corpusPath)) {
    Write-Error "Corpus file not found at $corpusPath"
    exit 1
}

# 1. Backup existing file if backup doesn't already exist
if (-not (Test-Path $backupPath)) {
    Write-Host "Backing up NagameseOfflineEngine.kt to $backupPath..."
    Copy-Item -Path $enginePath -Destination $backupPath -Force
} else {
    Write-Host "Backup already exists at $backupPath. Using it as source to prevent double-accumulation."
}

# 2. Read existing hand-curated vocabulary and bigrams from backup file
Write-Host "Parsing existing hand-curated dictionary entries from $backupPath..."
$existingWords = New-Object 'System.Collections.Generic.Dictionary[string, int]'
$existingBigrams = New-Object 'System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]'

$lines = Get-Content $backupPath
$inDictBlock = $false
$inBigramBlock = $false

foreach ($line in $lines) {
    if ($line -match 'private val localDictionary = mapOf\(') {
        $inDictBlock = $true
        continue
    }
    if ($inDictBlock -and $line -match '^\s*\)\s*$') {
        $inDictBlock = $false
        continue
    }
    if ($line -match 'private val localBigrams = mapOf\(') {
        $inBigramBlock = $true
        continue
    }
    if ($inBigramBlock -and $line -match '^\s*\)\s*$') {
        $inBigramBlock = $false
        continue
    }

    if ($inDictBlock) {
        if ($line -match '^\s*"([^"]+)"\s*to\s*(\d+)') {
            $word = $Matches[1].ToLower().Trim()
            $freq = [int]$Matches[2]
            $existingWords[$word] = $freq
        }
    }

    if ($inBigramBlock) {
        if ($line -match '^\s*"([^"]+)"\s*to\s*listOf\((.*?)\)') {
            $word = $Matches[1].ToLower().Trim()
            $listContent = $Matches[2]
            $nextWordsMatches = [regex]::Matches($listContent, '"([^"]+)"')
            $nextWordsList = New-Object 'System.Collections.Generic.List[string]'
            foreach ($m in $nextWordsMatches) {
                $nextWordsList.Add($m.Groups[1].Value.ToLower().Trim())
            }
            $existingBigrams[$word] = $nextWordsList
        }
    }
}

Write-Host "Parsed $($existingWords.Count) existing hand-curated words."
Write-Host "Parsed $($existingBigrams.Count) existing hand-curated bigram transitions."

# 3. Read Hugging Face corpus and count frequencies using pure .NET generic Dictionaries
Write-Host "Processing corpus to extract words and transitions..."
$wordCounts = New-Object 'System.Collections.Generic.Dictionary[string, int]'
$bigramCounts = New-Object 'System.Collections.Generic.Dictionary[string, int]'

$reader = [System.IO.File]::OpenText($corpusPath)
$lineCount = 0
try {
    while ($line = $reader.ReadLine()) {
        $lineCount++
        $prompt = ""
        $completion = ""
        if ($line -match '"prompt":\s*"(.*?)(?<!\\)"') { $prompt = $Matches[1] }
        if ($line -match '"completion":\s*"(.*?)(?<!\\)"') { $completion = $Matches[1] }
        
        $fullText = "$prompt $completion"
        $fullText = $fullText -replace "(?i)user:|assistant:", ""
        $fullText = $fullText -replace '\\"', '"'
        $fullText = $fullText -replace '\\n', ' '
        $fullText = $fullText -replace '\\t', ' '
        $fullText = $fullText -replace '\\u[0-9a-fA-F]{4}', ''
        
        $cleanText = $fullText.ToLower()
        $matches = [regex]::Matches($cleanText, '[a-z]{2,}')
        $tokens = New-Object 'System.Collections.Generic.List[string]'
        foreach ($m in $matches) {
            $tokens.Add($m.Value)
        }
        
        # Add to unigram count
        foreach ($token in $tokens) {
            if ($wordCounts.ContainsKey($token)) {
                $wordCounts[$token] = $wordCounts[$token] + 1
            } else {
                $wordCounts[$token] = 1
            }
        }
        
        # Add to bigram count
        for ($i = 0; $i -lt ($tokens.Count - 1); $i++) {
            $w1 = $tokens[$i]
            $w2 = $tokens[$i+1]
            $bigramKey = "$w1|$w2"
            if ($bigramCounts.ContainsKey($bigramKey)) {
                $bigramCounts[$bigramKey] = $bigramCounts[$bigramKey] + 1
            } else {
                $bigramCounts[$bigramKey] = 1
            }
        }
    }
} finally {
    $reader.Close()
}

Write-Host "Corpus processing completed."
Write-Host "Unique words found in corpus: $($wordCounts.Count)"
Write-Host "Unique transitions found in corpus: $($bigramCounts.Count)"

# 4. Scale and Merge words
Write-Host "Scaling and merging words..."
$maxCorpusFreq = 1.0
foreach ($val in $wordCounts.Values) {
    if ($val -gt $maxCorpusFreq) { $maxCorpusFreq = $val }
}
$logMax = [Math]::Log($maxCorpusFreq)
if ($logMax -le 0) { $logMax = 1.0 }

$mergedWords = New-Object 'System.Collections.Generic.Dictionary[string, int]'

# Load all corpus words with scaled frequency
foreach ($word in $wordCounts.Keys) {
    $freq = $wordCounts[$word]
    if ($freq -le 0) { $freq = 1 }
    # Logarithmic scaling from 30 to 90
    $scaledFreq = 30 + [int](([Math]::Log($freq) / $logMax) * 60)
    $mergedWords[$word] = $scaledFreq
}

# Merge with hand-curated list (ensure hand-curated frequencies are preserved if higher)
foreach ($word in $existingWords.Keys) {
    $curatedFreq = $existingWords[$word]
    if ($mergedWords.ContainsKey($word)) {
        if ($curatedFreq -gt $mergedWords[$word]) {
            $mergedWords[$word] = $curatedFreq
        }
    } else {
        $mergedWords[$word] = $curatedFreq
    }
}

# 5. Select top 3,000 words
Write-Host "Selecting top 3,000 words..."
$topWords = $mergedWords.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 3000
$topWordsMap = New-Object 'System.Collections.Generic.Dictionary[string, int]'
foreach ($item in $topWords) {
    $topWordsMap[$item.Key] = $item.Value
}

# Make sure we don't accidentally drop any hand-curated words from the top map
foreach ($word in $existingWords.Keys) {
    if (-not $topWordsMap.ContainsKey($word)) {
        $topWordsMap[$word] = $existingWords[$word]
    }
}

Write-Host "Final dictionary contains $($topWordsMap.Count) words."

# 6. Generate localBigrams for the top 300 most frequent words
Write-Host "Generating bigram predictions for the top 300 words..."
$finalBigrams = New-Object 'System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]'

# Start with existing bigrams (convert nextWords to list item-by-item)
foreach ($word in $existingBigrams.Keys) {
    $list = New-Object 'System.Collections.Generic.List[string]'
    foreach ($item in $existingBigrams[$word]) {
        $list.Add($item)
    }
    $finalBigrams[$word] = $list
}

# Group bigram counts by starting word
$bigramGroups = @{}
foreach ($key in $bigramCounts.Keys) {
    if ($key -match '^([a-z]+)\|([a-z]+)$') {
        $w1 = $Matches[1]
        $w2 = $Matches[2]
        $count = $bigramCounts[$key]
        
        # Only build bigrams for words that are in our vocabulary
        if ($topWordsMap.ContainsKey($w1) -and $topWordsMap.ContainsKey($w2)) {
            if (-not $bigramGroups.ContainsKey($w1)) {
                $bigramGroups[$w1] = New-Object 'System.Collections.Generic.Dictionary[string, int]'
            }
            $bigramGroups[$w1][$w2] = $count
        }
    }
}

# Sort top words by frequency to select the top 300 for generating bigram maps
$top300Words = $topWordsMap.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 300 | ForEach-Object { $_.Key }

foreach ($w1 in $top300Words) {
    if ($bigramGroups.ContainsKey($w1)) {
        $succeeding = $bigramGroups[$w1]
        # Sort succeeding words by frequency
        $sortedSucceeding = $succeeding.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 6 | ForEach-Object { $_.Key }
        
        if ($sortedSucceeding) {
            if (-not $finalBigrams.ContainsKey($w1)) {
                $finalBigrams[$w1] = New-Object 'System.Collections.Generic.List[string]'
            }
            
            # Merge with existing bigrams (keeping existing ones first, then adding new unique ones up to 6)
            $list = $finalBigrams[$w1]
            foreach ($nextW in $sortedSucceeding) {
                if (-not $list.Contains($nextW)) {
                    $list.Add($nextW)
                }
            }
            # Limit to top 6 transitions
            if ($list.Count -gt 6) {
                $truncated = New-Object 'System.Collections.Generic.List[string]'
                for ($i = 0; $i -lt 6; $i++) {
                    $truncated.Add($list[$i])
                }
                $finalBigrams[$w1] = $truncated
            }
        }
    }
}

Write-Host "Final bigram maps count: $($finalBigrams.Count)"

# 7. Generate Kotlin strings
Write-Host "Formatting Kotlin map syntax..."
$dictStrings = New-Object 'System.Collections.Generic.List[string]'
# Sort words by frequency in descending order
$sortedFinalWords = $topWordsMap.GetEnumerator() | Sort-Object Value -Descending

foreach ($item in $sortedFinalWords) {
    $dictStrings.Add("        `"$($item.Key)`" to $($item.Value)")
}
$dictMapContent = $dictStrings -join ",`r`n"

$bigramStrings = New-Object 'System.Collections.Generic.List[string]'
$sortedFinalBigrams = $finalBigrams.GetEnumerator() | Sort-Object Key

foreach ($item in $sortedFinalBigrams) {
    $wordListList = New-Object 'System.Collections.Generic.List[string]'
    foreach ($w in $item.Value) {
        $wordListList.Add("`"$w`"")
    }
    $wordList = $wordListList -join ", "
    $bigramStrings.Add("        `"$($item.Key)`" to listOf($wordList)")
}
$bigramMapContent = $bigramStrings -join ",`r`n"

# 8. Reconstruct NagameseOfflineEngine.kt
Write-Host "Updating NagameseOfflineEngine.kt..."
$rawContent = Get-Content -Path $backupPath -Raw

# Replace localDictionary
$dictPattern = '(?s)private val localDictionary = mapOf\(.*?\r?\n\s*\)'
$dictReplacement = "private val localDictionary = mapOf(`r`n$dictMapContent`r`n    )"
$rawContent = $rawContent -replace $dictPattern, $dictReplacement

# Replace localBigrams
$bigramPattern = '(?s)private val localBigrams = mapOf\(.*?\)\s*(?=\s*/\*\*?\s*\*\s*Attempts to fetch cached)'
$bigramReplacement = "private val localBigrams = mapOf(`r`n$bigramMapContent`r`n    )"
$rawContent = $rawContent -replace $bigramPattern, $bigramReplacement

[System.IO.File]::WriteAllText($enginePath, $rawContent, [System.Text.Encoding]::UTF8)
Write-Host "Dictionary expansion successful! NagameseOfflineEngine.kt updated with $($topWordsMap.Count) words and $($finalBigrams.Count) bigrams."
