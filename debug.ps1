param (
    [ValidateSet("mobile", "leanback", "tv", "logcat", "")]
    [string]$Target = ""
)

$ErrorActionPreference = "Continue"
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

function Get-AdbDeviceList {
    $list = @()
    $lines = & adb devices 2>$null
    foreach ($line in $lines) {
        $trimmed = $line.Trim()
        if ($trimmed -match '^(\S+)\s+device$') {
            $list += $matches[1]
        }
    }
    return $list
}

$devices = @(Get-AdbDeviceList)

if ($devices.Count -eq 0) {
    Write-Host "[*] 未检测到已连接设备，正在尝试自动连接常见第三方模拟器端口..." -ForegroundColor Gray
    
    # 常见模拟器端口: 雷电(5555), MuMu12(16384), MuMu旧版(7555), 夜神(62001), 逍遥(21503)
    $ports = @("127.0.0.1:5555", "127.0.0.1:16384", "127.0.0.1:7555", "127.0.0.1:62001", "127.0.0.1:21503")
    foreach ($port in $ports) {
        & adb connect $port 2>$null | Out-Null
    }
    
    Start-Sleep -Seconds 1
    $devices = @(Get-AdbDeviceList)
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

$script:activeDevice = [string]$devices[0]
Write-Host "[✓] 成功连接到模拟器: $script:activeDevice" -ForegroundColor Green

function Run-Adb {
    if ($script:activeDevice) {
        & adb -s $script:activeDevice $args
    } else {
        & adb $args
    }
}

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
    $appPid = (Run-Adb shell pidof com.cy.android.tv com.cy.android.mobile 2>$null)
    if ($appPid) {
        $appPid = ($appPid -split '\s+')[0].Trim()
        Write-Host "[*] 正在捕获应用实时日志 (PID: $appPid, 按 Ctrl + C 停止)..." -ForegroundColor Cyan
        Write-Host "-------------------------------------------------------" -ForegroundColor Gray
        Run-Adb logcat -v time --pid=$appPid
    } else {
        Write-Host "[*] 未检测到应用运行，正在捕获全局日志 (过滤模拟器后台干扰，Ctrl + C 停止)..." -ForegroundColor Cyan
        Write-Host "-------------------------------------------------------" -ForegroundColor Gray
        Run-Adb logcat -v time NewFileUpdater:S NewFileUpdaterUtils:S Watchdog:S nemu:S *:V
    }
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
$appPkg = if ($Target -eq "leanback") { "com.cy.android.tv" } else { "com.cy.android.mobile" }
Run-Adb shell am start -n "$appPkg/com.fongmi.android.tv.ui.activity.HomeActivity"

# 等待应用进程拉起并获取 PID (最多轮询 4 秒，确保捕获冷启动 PID)
$appPid = $null
for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Milliseconds 400
    $pidOut = (Run-Adb shell pidof $appPkg 2>$null)
    if ($pidOut) {
        $candidate = ($pidOut -split '\s+')[0].Trim()
        if ($candidate -match '^\d+$') {
            $appPid = $candidate
            break
        }
    }
}

# 7. 开启实时日志监听
Write-Host ""
if ($appPid) {
    Write-Host "[*] 正在开启应用实时日志监听 (应用: $appPkg, PID: $appPid, 按 Ctrl + C 可退出日志)..." -ForegroundColor Green
    Write-Host "-------------------------------------------------------" -ForegroundColor Gray
    Run-Adb logcat -v time --pid=$appPid
} else {
    Write-Host "[*] 未能精准获取应用 PID，正在开启过滤模式监听 (过滤模拟器底层干扰日志)..." -ForegroundColor Yellow
    Write-Host "-------------------------------------------------------" -ForegroundColor Gray
    Run-Adb logcat -v time NewFileUpdater:S NewFileUpdaterUtils:S Watchdog:S nemu:S *:V
}
