import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

const apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://127.0.0.1:8080',
);

void main() {
  runApp(const SentinelStatusApp());
}

class SentinelStatusApp extends StatelessWidget {
  const SentinelStatusApp({super.key});

  @override
  Widget build(BuildContext context) {
    const green = Color(0xff20b486);
    return MaterialApp(
      title: 'Sentinel Monitor Status',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: green),
        scaffoldBackgroundColor: const Color(0xfff7f7f8),
        fontFamily: 'Arial',
      ),
      home: const StatusPage(),
    );
  }
}

class StatusPage extends StatefulWidget {
  const StatusPage({super.key});

  @override
  State<StatusPage> createState() => _StatusPageState();
}

class _StatusPageState extends State<StatusPage> {
  StatusPageData? data;
  String? error;
  bool loading = false;
  Timer? refreshTimer;

  @override
  void initState() {
    super.initState();
    loadStatus();
    refreshTimer = Timer.periodic(const Duration(seconds: 15), (_) => loadStatus());
  }

  @override
  void dispose() {
    refreshTimer?.cancel();
    super.dispose();
  }

  Future<void> loadStatus() async {
    if (loading) return;
    setState(() => loading = true);
    try {
      final response = await http.get(Uri.parse('$apiBaseUrl/api/v1/status'));
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw Exception('API 返回 ${response.statusCode}');
      }
      setState(() {
        data = StatusPageData.fromJson(jsonDecode(response.body) as Map<String, dynamic>);
        error = null;
      });
    } catch (exception) {
      setState(() => error = exception.toString());
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> subscribe(String email) async {
    final response = await http.post(
      Uri.parse('$apiBaseUrl/api/v1/status/subscriptions'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'email': email}),
    );
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception('订阅失败：API 返回 ${response.statusCode}');
    }
  }

  @override
  Widget build(BuildContext context) {
    final current = data ?? StatusPageData.empty();
    final isDegraded = current.overallStatus != 'operational' || error != null;
    final status = isDegraded ? StatusInfo.degraded : StatusInfo.operational;

    return Scaffold(
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: loadStatus,
          child: ListView(
            padding: const EdgeInsets.only(bottom: 48),
            children: [
              _Header(onSubscribe: () => _showSubscribeDialog(context)),
              ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 960),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 34),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _Hero(status: status, loading: loading, onRefresh: loadStatus),
                      const SizedBox(height: 28),
                      _Notice(status: status, error: error),
                      if (isDegraded) ...[
                        const SizedBox(height: 18),
                        _IncidentCard(),
                      ],
                      const SizedBox(height: 28),
                      _ServicesCard(groups: current.groups),
                      const SizedBox(height: 16),
                      Text(
                        '数据每 15 秒自动刷新  ·  当前监测 ${current.totalServers} 台服务器',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.grey.shade500, fontSize: 12),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _showSubscribeDialog(BuildContext context) async {
    final controller = TextEditingController();
    var submitting = false;
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('订阅服务状态更新'),
          content: TextField(
            controller: controller,
            keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(
              labelText: '邮箱地址',
              hintText: 'you@example.com',
              border: OutlineInputBorder(),
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(dialogContext), child: const Text('取消')),
            FilledButton(
              onPressed: submitting
                  ? null
                  : () async {
                      if (controller.text.trim().isEmpty) return;
                      setDialogState(() => submitting = true);
                      try {
                        await subscribe(controller.text.trim());
                        if (dialogContext.mounted) Navigator.pop(dialogContext);
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('订阅已保存')),
                          );
                        }
                      } catch (exception) {
                        setDialogState(() => submitting = false);
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text(exception.toString())),
                          );
                        }
                      }
                    },
              child: Text(submitting ? '保存中...' : '确认订阅'),
            ),
          ],
        ),
      ),
    );
    controller.dispose();
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.onSubscribe});

  final VoidCallback onSubscribe;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 22, 20, 0),
      child: Row(
        children: [
          const _BrandMark(),
          const SizedBox(width: 10),
          const Text('Sentinel Monitor', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
          const Spacer(),
          FilledButton(onPressed: onSubscribe, child: const Text('订阅更新')),
        ],
      ),
    );
  }
}

class _BrandMark extends StatelessWidget {
  const _BrandMark();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 36,
      height: 36,
      alignment: Alignment.center,
      decoration: const BoxDecoration(color: Color(0xff202124), shape: BoxShape.circle),
      child: const Text('S', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w700)),
    );
  }
}

class _Hero extends StatelessWidget {
  const _Hero({required this.status, required this.loading, required this.onRefresh});

  final StatusInfo status;
  final bool loading;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 56,
          height: 56,
          alignment: Alignment.center,
          decoration: BoxDecoration(color: status.color, shape: BoxShape.circle),
          child: Icon(status.icon, color: Colors.white, size: 30),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('SENTINEL MONITOR STATUS', style: TextStyle(color: Colors.grey.shade600, fontSize: 11, fontWeight: FontWeight.w800, letterSpacing: 1.3)),
              const SizedBox(height: 5),
              const Text('服务状态', style: TextStyle(fontSize: 30, fontWeight: FontWeight.w800)),
              const SizedBox(height: 4),
              Text(status.description, style: TextStyle(color: Colors.grey.shade600)),
            ],
          ),
        ),
        TextButton(onPressed: loading ? null : onRefresh, child: Text(loading ? '刷新中...' : '立即刷新')),
      ],
    );
  }
}

class _Notice extends StatelessWidget {
  const _Notice({required this.status, required this.error});

  final StatusInfo status;
  final String? error;

  @override
  Widget build(BuildContext context) {
    final background = status == StatusInfo.operational ? const Color(0xffdbf7eb) : const Color(0xfffff2c6);
    return Container(
      decoration: BoxDecoration(color: Colors.white, border: Border.all(color: status.color.withAlpha(150)), borderRadius: BorderRadius.circular(13)),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(18),
            color: background,
            child: Row(children: [Icon(status.icon, color: status.color), const SizedBox(width: 10), Text(status.title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700))]),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 18, 20, 8),
            child: Text(error == null ? status.description : '暂时无法连接后端，页面会继续重试。', style: const TextStyle(fontSize: 14)),
          ),
          Padding(padding: const EdgeInsets.fromLTRB(20, 0, 20, 17), child: Text('实时状态页', style: TextStyle(color: Colors.grey.shade500, fontSize: 12))),
        ],
      ),
    );
  }
}

class _IncidentCard extends StatelessWidget {
  const _IncidentCard();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(color: const Color(0xfffffef3), border: Border.all(color: const Color(0xfff1c84b)), borderRadius: BorderRadius.circular(13)),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.circle, size: 11, color: Color(0xfff0ad00)),
          SizedBox(width: 12),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('部分服务需要关注', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700)), SizedBox(height: 5), Text('系统正在持续检查服务状态。')]))
        ],
      ),
    );
  }
}

class _ServicesCard extends StatelessWidget {
  const _ServicesCard({required this.groups});

  final List<ServiceGroup> groups;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.only(top: 22),
        child: Column(
          children: [
            const Padding(
              padding: EdgeInsets.fromLTRB(22, 0, 22, 18),
              child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [Text('服务状态', style: TextStyle(fontSize: 21, fontWeight: FontWeight.w800)), Text('最近 60 次检查', style: TextStyle(color: Colors.grey, fontSize: 12))]),
            ),
            for (final group in groups) _ServiceGroupTile(group: group),
            if (groups.isEmpty) const Padding(padding: EdgeInsets.all(28), child: Text('等待监控数据接入')),
          ],
        ),
      ),
    );
  }
}

class _ServiceGroupTile extends StatefulWidget {
  const _ServiceGroupTile({required this.group});

  final ServiceGroup group;

  @override
  State<_ServiceGroupTile> createState() => _ServiceGroupTileState();
}

class _ServiceGroupTileState extends State<_ServiceGroupTile> {
  bool expanded = false;

  @override
  Widget build(BuildContext context) {
    final group = widget.group;
    final status = group.status == 'operational' ? StatusInfo.operational : StatusInfo.degraded;
    return InkWell(
      onTap: () => setState(() => expanded = !expanded),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(22, 18, 22, 18),
        child: Column(
          children: [
            Row(children: [
              Icon(status.icon, color: status.color, size: 24),
              const SizedBox(width: 10),
              Expanded(child: Text(group.name, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700))),
              Text(group.uptime, style: TextStyle(color: Colors.grey.shade700)),
              const SizedBox(width: 4),
              Icon(expanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down, color: Colors.grey),
            ]),
            const SizedBox(height: 12),
            Row(children: [for (final value in group.history) Expanded(child: Container(height: 12, margin: const EdgeInsets.only(right: 2), decoration: BoxDecoration(color: _historyColor(value), borderRadius: BorderRadius.circular(2))))]),
            if (expanded) ...[
              const SizedBox(height: 12),
              for (final child in group.children) Padding(padding: const EdgeInsets.symmetric(vertical: 4), child: Row(children: [const Icon(Icons.check_circle, size: 16, color: Color(0xff20b486)), const SizedBox(width: 8), Text(child), const Spacer(), Text(status.shortLabel, style: TextStyle(color: status.color, fontSize: 12))])),
            ],
          ],
        ),
      ),
    );
  }

  Color _historyColor(String value) {
    if (value == 'operational') return const Color(0xff32ba91);
    if (value == 'degraded') return const Color(0xffffbc2b);
    if (value == 'major_outage') return const Color(0xffe66a6f);
    return const Color(0xffe7e7e9);
  }
}

class StatusInfo {
  const StatusInfo(this.title, this.description, this.shortLabel, this.icon, this.color);

  final String title;
  final String description;
  final String shortLabel;
  final IconData icon;
  final Color color;

  static const operational = StatusInfo('正常运行', '所有服务运行正常。', '正常', Icons.check, Color(0xff20b486));
  static const degraded = StatusInfo('部分服务性能下降', '部分服务响应时间较长，正在持续观察。', '性能下降', Icons.priority_high, Color(0xffe1a900));
}

class StatusPageData {
  const StatusPageData({required this.overallStatus, required this.totalServers, required this.groups});

  final String overallStatus;
  final int totalServers;
  final List<ServiceGroup> groups;

  factory StatusPageData.fromJson(Map<String, dynamic> json) {
    final overview = (json['overview'] as Map<String, dynamic>?) ?? const {};
    return StatusPageData(
      overallStatus: json['overallStatus'] as String? ?? 'operational',
      totalServers: (overview['totalServers'] as num?)?.toInt() ?? 0,
      groups: ((json['groups'] as List<dynamic>?) ?? const [])
          .map((value) => ServiceGroup.fromJson(value as Map<String, dynamic>))
          .toList(),
    );
  }

  factory StatusPageData.empty() => const StatusPageData(overallStatus: 'operational', totalServers: 0, groups: []);
}

class ServiceGroup {
  const ServiceGroup({required this.name, required this.status, required this.uptime, required this.history, required this.children});

  final String name;
  final String status;
  final String uptime;
  final List<String> history;
  final List<String> children;

  factory ServiceGroup.fromJson(Map<String, dynamic> json) {
    return ServiceGroup(
      name: json['name'] as String? ?? '未命名服务',
      status: json['status'] as String? ?? 'unknown',
      uptime: json['uptime'] as String? ?? '-',
      history: ((json['history'] as List<dynamic>?) ?? const []).map((value) => value.toString()).toList(),
      children: ((json['children'] as List<dynamic>?) ?? const []).map((value) => value.toString()).toList(),
    );
  }
}
