param(
    [string]$ApiBaseUrl = 'http://127.0.0.1:8088'
)

$ErrorActionPreference = 'Stop'
$adb = 'D:\DevTools\AndroidSDK\platform-tools\adb.exe'
$flutter = 'D:\DevTools\Flutter SDK\flutter\bin\flutter.bat'
$project = 'D:\学习\sentinel-monitor\clients\flutter-status'

if (-not (Test-Path -LiteralPath $adb)) {
    throw "找不到 adb：$adb"
}
if (-not (Test-Path -LiteralPath $flutter)) {
    throw "找不到 Flutter：$flutter"
}

$env:PUB_CACHE = 'D:\DevTools\sentinel-monitor-pub-cache'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.18'
$env:ANDROID_HOME = 'D:\DevTools\AndroidSDK'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

& $adb start-server | Out-Null
$deviceLine = @(& $adb devices | Select-Object -Skip 1) |
    Where-Object { $_ -match '^\S+\s+device(\s|$)' } |
    Select-Object -First 1

if (-not $deviceLine) {
    Write-Host '没有发现已授权的 Android 设备。请连接手机、打开 USB 调试并在手机上允许 RSA 调试授权。' -ForegroundColor Yellow
    & $adb devices -l
    exit 1
}

$deviceId = ($deviceLine -split '\s+')[0]
Write-Host "使用设备 $deviceId，API=$ApiBaseUrl" -ForegroundColor Green
Push-Location $project
try {
    & $flutter run -d $deviceId --dart-define="API_BASE_URL=$ApiBaseUrl"
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}
