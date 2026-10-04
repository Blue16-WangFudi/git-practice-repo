# Sentinel Monitor Flutter 客户端

这是 Sentinel Monitor 的跨平台状态页客户端，使用 Flutter + Dart 编写，与 Vue 状态页共用 Spring Boot API。

## 当前能力

- 请求 `GET /api/v1/status` 展示整体状态、服务分组和历史状态条。
- 每 15 秒自动刷新，也支持下拉刷新和手动刷新。
- 调用 `POST /api/v1/status/subscriptions` 保存邮箱订阅。
- 使用 `API_BASE_URL` 编译参数切换本地后端、Docker 网关或真机可访问地址。

## 本机运行

Flutter SDK 位于 `D:\DevTools\Flutter SDK\flutter`，依赖缓存位于 `D:\DevTools\sentinel-monitor-pub-cache`。

```powershell
$env:PUB_CACHE = 'D:\DevTools\sentinel-monitor-pub-cache'
cd D:\学习\sentinel-monitor\clients\flutter-status

& 'D:\DevTools\Flutter SDK\flutter\bin\flutter.bat' pub get
& 'D:\DevTools\Flutter SDK\flutter\bin\flutter.bat' test
& 'D:\DevTools\Flutter SDK\flutter\bin\flutter.bat' run -d chrome --dart-define=API_BASE_URL=http://127.0.0.1:8080
```

Docker 网关运行时可以改用 `http://127.0.0.1:8088`。Android 模拟器通常使用 `http://10.0.2.2:8080`；Android 真机要使用电脑在局域网中的地址，并保证手机能访问该端口。

## 打包

Flutter Web 已验证可以构建：

```powershell
$env:PUB_CACHE = 'D:\DevTools\sentinel-monitor-pub-cache'
& 'D:\DevTools\Flutter SDK\flutter\bin\flutter.bat' build web --release --dart-define=API_BASE_URL=http://127.0.0.1:8088
```

Android APK 已在本机验证成功：

```powershell
& 'D:\DevTools\Flutter SDK\flutter\bin\flutter.bat' build apk --release --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

本机 Android SDK 位于 `D:\DevTools\AndroidSDK`，使用 JDK 17。iOS IPA 需要 macOS、Xcode、Apple 证书和签名环境，Windows 不能直接生成正式 IPA。
