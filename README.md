# Git Practice Repository

This repo is a sandbox for practicing Git and GitHub workflows across a few
small sample projects. Each subproject is intentionally minimal and can be used
independently.

## Repository Layout
- `springboot/`: Spring Boot 4 starter (Java 21, Maven wrapper).
- `flutter/flutter_application/`: Flutter app template.
- `flutter/flutter_package/`: Flutter package template.
- `flutter/flutter_plugin/`: Flutter plugin template with an example app.
- `docs/`: Small notes.

## Prerequisites
- Java 21 for the Spring Boot project.
- Flutter SDK for the Flutter projects (`flutter doctor` should be clean).

## Usage

### Spring Boot sample
```bash
cd springboot
./mvnw spring-boot:run
```

Windows:
```powershell
mvnw.cmd spring-boot:run
```

Optional tests:
```bash
./mvnw test
```

Note: no controllers are defined yet; add one under
`springboot/src/main/java` if you want endpoints.

### Flutter application
```bash
cd flutter/flutter_application
flutter pub get
flutter run
```

### Flutter package
```bash
cd flutter/flutter_package
flutter pub get
flutter test
```

### Flutter plugin
```bash
cd flutter/flutter_plugin
flutter pub get
flutter test
cd example
flutter run
```

## More Details
- `springboot/README.md`
- `flutter/flutter_application/README.md`
- `flutter/flutter_package/README.md`
- `flutter/flutter_plugin/README.md`

## Contributing
See `CONTRIBUTING.md`.

## License
Apache License 2.0. See `LICENSE`.
