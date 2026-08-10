$root = "D:\PROJECTS\AndroidStudioProjects\unfold"
$patterns = @(
  @{pattern='^package\s+com\.volt'; replacement='package com.unfold'},
  @{pattern='import\s+com\.volt'; replacement='import com.unfold'},
  @{pattern='com\.volt'; replacement='com.unfold'},
  @{pattern='namespace\s*=\s*"com\.volt'; replacement='namespace = "com.unfold'},
  @{pattern='\bVoltRoute\b'; replacement='UnfoldRoute'},
  @{pattern='\bVoltThemeColors\b'; replacement='UnfoldThemeColors'},
  @{pattern='\bLocalVoltTheme\b'; replacement='LocalUnfoldTheme'},
  @{pattern='\bVoltTheme\b'; replacement='UnfoldTheme'},
  @{pattern='\bVoltApp\b'; replacement='UnfoldApp'}
)

$exts = '*.kt','*.java','*.xml','*.gradle','*.kts','*.md'
Get-ChildItem -Path $root -Recurse -Include $exts -File | ForEach-Object {
  $path = $_.FullName
  try {
    $text = Get-Content -Raw -LiteralPath $path
    $new = $text
    foreach ($p in $patterns) {
      $new = [regex]::Replace($new, $p.pattern, $p.replacement)
    }
    if ($new -ne $text) {
      Set-Content -LiteralPath $path -Value $new
      Write-Output "Updated: $path"
    }
  } catch {
    Write-Output "Skip: $path -> $($_.Exception.Message)"
  }
}

# Update AndroidManifest
$manifest = Join-Path $root 'app\src\main\AndroidManifest.xml'
if (Test-Path $manifest) {
  (Get-Content -Raw $manifest) -replace 'android:name="\\.VoltApp"','android:name=".UnfoldApp"' | Set-Content $manifest
  Write-Output "Updated AndroidManifest application name"
}

Write-Output "Done"
