# Git Practice Repository

This repository is a sandbox for practicing Git and GitHub workflows across several small projects. Existing practice projects remain under their original directories; the root-level `backend`, `monitor-web`, `collector` and `clients/flutter-status` directories form the full-stack monitoring project added for the deployment, performance and collaboration exercises.

## Repository layout

- `backend/`, `monitor-web/`, `collector/`, `clients/flutter-status/`: Sentinel Monitor learning project.
- `springboot/`: original Spring Boot practice project.
- `flutter/`: original Flutter application, package and plugin templates.
- `frontend/`, `python/`: other small practice projects.
- `docs/`: repository notes and project documentation.

## Sentinel Monitor

Sentinel Monitor is a small platform for learning deployment, performance analysis, server inspection, caching, messaging and front-end/back-end collaboration.

It currently includes:

- Spring Boot monitoring API with MySQL persistence and service CRUD.
- Redis Cache-Aside caching, cache hit-rate metrics and a latency comparison script.
- RabbitMQ asynchronous alert publishing and consuming.
- Python server metrics collector.
- Vue public Status Page and service catalog management panel.
- Flutter Android/Web status client skeleton.
- Docker Compose orchestration for MySQL, MongoDB, Redis, RabbitMQ, Spring Boot and Nginx.
- k6 load test, SSH tunnel, inspection and daily report templates.
- GitHub Actions checks for the backend, Web, Flutter and Compose configuration.

### Local location

The working copy is at `D:\学习\sentinel-monitor`.

Windows local backend:

```powershell
cd D:\学习\sentinel-monitor\backend
java -jar target\sentinel-monitor-api-0.1.0-SNAPSHOT.jar
```

Vue development panel:

```powershell
cd D:\学习\sentinel-monitor\monitor-web
npm install
npm run dev
```

- Dev panel: `http://127.0.0.1:5173`
- Docker gateway: `http://127.0.0.1:8088`
- MongoDB Compass: `mongodb://root:change-me@127.0.0.1:27019/?authSource=admin`

The page includes public service status, operational metrics and a teaching-only service catalog management area. Production use still requires authentication and audit logging.

### Documentation

- [Project architecture and question answers](docs/项目架构与问题答案.md)
- [Development environment and dependency notes](docs/开发环境与依赖说明.md)
- [Operations, deployment and load-test handbook](docs/运维部署与压测手册.md)
- [Spring Boot CRUD practice](docs/SpringBoot%20CRUD实践.md)
- [MongoDB Compass practice](docs/MongoDB%20Compass实践.md)
- [Git collaboration practice](docs/Git协作实践.md)
- [WSL and Android debugging](docs/WSL与Android调试.md)

Android 真机调试一键脚本：`scripts/flutter-android-run.ps1`。

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). New work should use a feature or fix branch, pass the relevant local checks and be submitted through a Pull Request.

## License

Apache License 2.0. See [LICENSE](LICENSE).
