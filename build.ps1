param (
    [ValidateSet("all", "leanback", "mobile", "clean", "")]
    [string]$Target = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "                TV Android 本地打包脚本" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

# 1. 配置 JDK 21 环境变量 (必须使用 JDK 21)
$Jdk21 = "E:\work\tools\jdk\jdk-21.0.12.1+1"
if (Test-Path "$Jdk21\bin\java.exe") {
    $env:JAVA_HOME = $Jdk21
}
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 2. 配置 Python 环境变量 (Chaquopy 需要 Python 3.10+)
$UvPython = "C:\Users\EDZ\AppData\Roaming\uv\python\cpython-3.10-windows-x86_64-none"
if (Test-Path $UvPython) {
    $env:PATH = "$UvPython;$env:PATH"
}

Write-Host "[*] JAVA_HOME : $env:JAVA_HOME" -ForegroundColor Gray
Write-Host "[*] Java 版本 :" -ForegroundColor Gray
& "$env:JAVA_HOME\bin\java.exe" -version

# 3. 交互式选择菜单
if (-not $Target) {
    Write-Host ""
    Write-Host "请选择要执行的打包目标:" -ForegroundColor Yellow
    Write-Host "  [1] 打包全部 (电视版 Leanback + 手机版 Mobile)"
    Write-Host "  [2] 仅打包 电视版 (Leanback Release)"
    Write-Host "  [3] 仅打包 手机版 (Mobile Release)"
    Write-Host "  [4] 清理构建缓存 (Clean)"
    Write-Host "  [0] 退出"
    Write-Host ""
    
    $choice = Read-Host "请输入选项 [1-4, 0]"
    switch ($choice) {
        "1" { $Target = "all" }
        "2" { $Target = "leanback" }
        "3" { $Target = "mobile" }
        "4" { $Target = "clean" }
        default {
            Write-Host "[*] 已退出。" -ForegroundColor Yellow
            return
        }
    }
}

# 4. 执行构建
$gradleTask = switch ($Target) {
    "all"      { "assembleRelease" }
    "leanback" { ":app:assembleLeanbackRelease" }
    "mobile"   { ":app:assembleMobileRelease" }
    "clean"    { "clean" }
}

Write-Host ""
Write-Host "[*] 开始执行构建: .\gradlew.bat $gradleTask" -ForegroundColor Green
$startTime = Get-Date

cmd.exe /c "gradlew.bat $gradleTask"

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[x] 构建失败，退出码: $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}

$duration = (Get-Date) - $startTime
Write-Host ""
Write-Host "[*] 构建完成! 耗时: $([math]::Round($duration.TotalSeconds, 1)) 秒" -ForegroundColor Green

# 5. 整理产物
if ($Target -ne "clean") {
    $outDir = Join-Path $ScriptDir "Release\apk"
    if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }
    
    Write-Host ""
    Write-Host "=======================================================" -ForegroundColor Cyan
    Write-Host "                    APK 产物列表" -ForegroundColor Cyan
    Write-Host "=======================================================" -ForegroundColor Cyan
    
    Get-ChildItem -Path "app\build\outputs\apk" -Recurse -Filter "*.apk" | ForEach-Object {
        $dest = Join-Path $outDir $_.Name
        Copy-Item $_.FullName $dest -Force
        $sizeMb = [math]::Round($_.Length / 1MB, 2)
        Write-Host ("[+] " + $_.Name + " (" + $sizeMb + " MB) -> " + $dest) -ForegroundColor Green
    }
    
    Write-Host ""
    Write-Host "[*] 所有 APK 文件已集中存放到: $outDir" -ForegroundColor Cyan
}
