$root = 'D:\PROJECTS\AndroidStudioProjects\unfold'
$dirs = Get-ChildItem -Path $root -Recurse -Directory | Where-Object { $_.FullName -match '\\com\\volt(\\|$)' } | Sort-Object -Property FullName -Descending
foreach ($d in $dirs) {
    $old = $d.FullName
    $new = $old -replace '\\com\\volt','\\com\\unfold'
    $parent = Split-Path $new -Parent
    if (-not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
    try {
        Move-Item -LiteralPath $old -Destination $new -Force
        Write-Output "Moved: $old -> $new"
    } catch {
        Write-Output "Failed: $old -> $new -> $($_.Exception.Message)"
    }
}
Write-Output 'Done'
