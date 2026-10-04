import 'package:flutter_test/flutter_test.dart';

import 'package:sentinel_monitor_status/main.dart';

void main() {
  testWidgets('renders the Sentinel public status page', (WidgetTester tester) async {
    await tester.pumpWidget(const SentinelStatusApp());

    expect(find.text('Sentinel Monitor'), findsOneWidget);
    expect(find.text('服务状态'), findsNWidgets(2));
    expect(find.text('订阅更新'), findsOneWidget);
  });
}
