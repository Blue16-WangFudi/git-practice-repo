# WSL 与 Android 调试说明

## 当前已验证环境

2026-10-04 在本机验证通过：

- Ubuntu 运行在 WSL 2。
- Docker Desktop 的 `docker-desktop` 发行版运行正常。
- WSL 内的 Docker CLI 可以连接 Docker Desktop，Compose 版本为 5.4.0。
- 项目可以从 WSL 路径 `/mnt/d/学习/sentinel-monitor` 访问。
- WSL 内的 `docker compose ps` 可以看到 backend、gateway、monitor-web、MySQL、MongoDB、Redis 和 RabbitMQ 容器。
- WSL 内已有 OpenJDK 21，可用于运行普通 Spring Boot 进程；项目容器化运行不依赖 WSL 额外安装 Maven。

快速检查：

```bash
cd /mnt/d/学习/sentinel-monitor
docker compose config -q
docker compose ps
```

也可以使用项目脚本：

```bash
bash scripts/wsl-smoke-test.sh
```

## Flutter 的 Windows/WSL 边界

当前 Flutter SDK 在 `D:\DevTools\Flutter SDK\flutter`，用于 Windows 构建；依赖缓存和 Android SDK 也都放在 `D:\DevTools`。这个 Windows SDK 的 shell 文件带有 Windows 换行符，不能直接当作 Linux Flutter SDK 在 WSL 中运行，因此不要在 WSL 中直接执行 `/mnt/d/DevTools/Flutter SDK/flutter/bin/flutter`。

当前推荐的学习路径是：

1. 在 WSL 中运行 Docker Compose、Spring Boot 容器和采集器。
2. 在 Windows 中使用 Flutter SDK、Android SDK 和 USB/ADB 调试真机。
3. Flutter 使用 `API_BASE_URL=http://127.0.0.1:8088` 访问 Docker 网关；真机调试时改为电脑在局域网中的 IPv4 地址。

## Android 真机调试

Windows 侧准备：

1. 手机上打开开发者选项和 USB 调试。
2. 用数据线连接电脑，在手机上确认 RSA 调试授权。
3. 在 PowerShell 中设置 `ANDROID_HOME=D:\DevTools\AndroidSDK`，执行 `adb devices`。
4. 设备状态显示为 `device` 后，在客户端目录执行：

```powershell
$env:PUB_CACHE = 'D:\DevTools\sentinel-monitor-pub-cache'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.18'
$env:ANDROID_HOME = 'D:\DevTools\AndroidSDK'
& 'D:\DevTools\Flutter SDK\flutter\bin\flutter.bat' devices
& 'D:\DevTools\Flutter SDK\flutter\bin\flutter.bat' run -d <设备ID> --dart-define=API_BASE_URL=http://电脑局域网IP:8088
```

也可以使用项目脚本自动检查设备并启动：

```powershell
.\scripts\flutter-android-run.ps1 -ApiBaseUrl http://电脑局域网IP:8088
```

本次检查时 `adb devices -l` 为空，说明当前没有连接并授权的 Android 真机；这不是 Flutter 或 APK 构建错误。

如果必须让 WSL 直接看到 USB 设备，需要额外配置 `usbipd-win` 把 USB 设备附加到 WSL；这不是本机当前已验证的路径。教学项目先采用 Windows Flutter + Windows ADB，后端和容器仍然运行在 WSL/Docker 环境中，排障成本更低。

## 没有 Android 真机时

没有手机不影响 Flutter 学习和接口联调。直接使用 Chrome：

```powershell
$env:PUB_CACHE = 'D:\DevTools\sentinel-monitor-pub-cache'
& 'D:\DevTools\Flutter SDK\flutter\bin\flutter.bat' run -d chrome --web-port 5200 --dart-define=API_BASE_URL=http://127.0.0.1:8088
```

本机已启动过该调试入口：`http://127.0.0.1:5200`。它访问的是 Docker 网关，能验证 Flutter 页面、Spring Boot API、MySQL/Redis 数据链路。以后拿到 Android 设备后，再使用 APK 或 `flutter-android-run.ps1`，不需要修改业务代码。

## 常见问题

- `Android SDK location contains spaces`：SDK 应放在 `D:\DevTools\AndroidSDK`，不要放到带空格的目录。
- `project path contains non-ASCII characters`：项目在 `D:\学习` 下，Android 工程已配置 `android.overridePathCheck=true`；正式团队项目更建议使用纯英文路径。
- 真机能安装但请求失败：把 `API_BASE_URL` 从 `127.0.0.1` 改成电脑局域网 IP，并确认 Windows 防火墙允许 8088 端口。
- Android 模拟器访问宿主机：通常使用 `http://10.0.2.2:8088`，不要使用 `127.0.0.1`。
