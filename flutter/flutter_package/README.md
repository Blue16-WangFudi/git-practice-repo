# Flutter Package Sample

Small Dart package scaffold for Git practice.

## Use Locally
Add the package as a path dependency from another Flutter app:

```yaml
dependencies:
  flutter_package:
    path: ../flutter_package
```

Example usage:

```dart
import 'package:flutter_package/flutter_package.dart';

final value = Calculator().addOne(41);
```

## Develop
```bash
flutter pub get
flutter test
```
