param (
    [ValidateSet("mobile", "leanback", "tv", "logcat", "")]
    [string]$Target = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "        TV Android 模拟器一键调试运行脚本" -ForegroundColor Cyan
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

# 3. 检查 ADB 与第三方模拟器连接
Write-Host "[*] 正在检测第三方模拟器连接状态..." -ForegroundColor Yellow

function Get-ConnectedDevices {
    $lines = & adb devices | Out-String -Stream
    $devs = @()
    foreach ($line in $lines) {
        if ($line -match '^(\S+)\s+device$') {
            $devs += $matches[1]
        }
    }
    return $devs
}

$devices = Get-ConnectedDevices

if ($devices.Count -eq 0) {
    Write-Host "[*] 未检测到已连接设备，正在尝试自动连接常见第三方模拟器端口..." -ForegroundColor Gray
    
    # 常见模拟器端口: 雷电(5555), MuMu12(16384), MuMu旧版(7555), 夜神(62001), 逍遥(21503)
    $ports = @("127.0.0.1:5555", "127.0.0.1:16384", "127.0.0.1:7555", "127.0.0.1:62001", "127.0.0.1:21503")
    foreach ($port in $ports) {
        Write-Host "  -> 尝试连接 $port ..." -ForegroundColor Gray
        & adb connect $port | Out-Null
    }
    
    Start-Sleep -Seconds 1
    $devices = Get-ConnectedDevices
}

if ($devices.Count -eq 0) {
    Write-Host ""
    Write-Host "[!] 未检测到运行中的第三方模拟器！" -ForegroundColor Red
    Write-Host "    请先打开您的安卓模拟器（例如：雷电模拟器、MuMu、夜神等）。" -ForegroundColor Yellow
    Write-Host "    开机进入桌面后，再次运行本脚本即可自动识别并连接！" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "按 Enter 键退出..."
    exit 1
}

$activeDevice = $devices[0]
Write-Host "[✓] 成功连接到模拟器: $activeDevice" -ForegroundColor Green

# 4. 选择调试目标
if (-not $Target) {
    Write-Host ""
    Write-Host "请选择要调试的版本:" -ForegroundColor Yellow
    Write-Host "  [1] 调试 手机版 (Mobile Debug - 推荐常规/竖屏测试)"
    Write-Host "  [2] 调试 电视版 (Leanback Debug - 推荐大屏/TV横屏测试)"
    Write-Host "  [3] 仅查看当前模拟器实时日志 (Logcat)"
    Write-Host "  [0] 退出"
    Write-Host ""
    
    $choice = Read-Host "请输入选项 [1-3, 0]"
    switch ($choice) {
        "1" { $Target = "mobile" }
        "2" { $Target = "leanback" }
        "3" { $Target = "logcat" }
        default {
            Write-Host "[*] 已退出。" -ForegroundColor Yellow
            return
        }
    }
}

if ($Target -eq "tv") { $Target = "leanback" }

# 如果只需抓取日志
if ($Target -eq "logcat") {
    Write-Host ""
    Write-Host "[*] 开始捕获日志 (Ctrl + C 停止)..." -ForegroundColor Cyan
    & adb -s $activeDevice logcat -v time -s TV:V OkHttp:D AndroidRuntime:E
    return
}

# 5. 快速增量编译并安装 Debug 版到模拟器
$gradleTask = if ($Target -eq "leanback") { ":app:installLeanbackDebug" } else { ":app:installMobileDebug" }

Write-Host ""
Write-Host "[*] 开始编译并安装到模拟器: .\gradlew.bat $gradleTask" -ForegroundColor Green
$startTime = Get-Date

cmd.exe /c "gradlew.bat $gradleTask"

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[x] 编译/安装失败，退出码: $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}

$duration = (Get-Date) - $startTime
Write-Host "[✓] 安装成功! 耗时: $([math]::Round($duration.TotalSeconds, 1)) 秒" -ForegroundColor Green

# 6. 启动应用
Write-Host ""
Write-Host "[*] 正在模拟器中启动应用..." -ForegroundColor Cyan
& adb -s $activeDevice shell am start -n com.fongmi.android.tv/.ui.activity.HomeActivity | Out-Null

# 7. 清空并监听日志
Write-Host "[*] 正在开启实时日志监听 (按 Ctrl + C 可退出日志)..." -ForegroundColor Green
Write-Host "-------------------------------------------------------" -ForegroundColor Gray
& adb -s $activeDevice logcat -c
& adb -s $activeDevice logcat -v time -s TV:V OkHttp:D AndroidRuntime:E
