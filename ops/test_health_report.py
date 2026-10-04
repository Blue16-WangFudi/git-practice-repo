"""Offline tests: never contact a real API, mail server or Feishu group."""

import contextlib
import importlib.util
import io
import json
import os
import smtplib
import ssl
import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch
from urllib.error import URLError

spec = importlib.util.spec_from_file_location("health_report", Path(__file__).with_name("health-report.py"))
report = importlib.util.module_from_spec(spec)
spec.loader.exec_module(report)


class ReportTests(unittest.TestCase):
    def setUp(self):
        self.environment = patch.dict(os.environ, {}, clear=True)
        self.environment.start()
        self.addCleanup(self.environment.stop)
        # Fail closed if a test forgets to mock either transport or Docker.
        self.http = patch.object(report.urllib.request, "urlopen", side_effect=AssertionError("Network forbidden"))
        self.smtp = patch.object(report.smtplib, "SMTP", side_effect=AssertionError("SMTP forbidden"))
        self.smtp_ssl = patch.object(report.smtplib, "SMTP_SSL", side_effect=AssertionError("SMTP forbidden"))
        self.process = patch.object(report.subprocess, "run", side_effect=AssertionError("Docker forbidden"))
        for guard in (self.http, self.smtp, self.smtp_ssl, self.process):
            guard.start()
            self.addCleanup(guard.stop)

    def snapshot(self):
        with patch.object(report, "get_json", side_effect=[
            {"status": "UP"},
            {"overallStatus": "operational", "overview": {"onlineServers": 1, "totalServers": 1, "offlineServers": 0},
             "incidents": []},
            {"requestCount": 30, "qps": 0.1, "serverErrorCount": 0, "latency": {"p50Ms": 5, "p999Ms": 20}},
            {"hits": 8, "misses": 2, "hitRate": 80},
        ]), patch.object(report, "docker_stats", return_value=[]):
            return report.build_report_data("http://localhost:8088/")

    def webhook_response(self, data, status=200):
        response = MagicMock()
        response.status = status
        response.read.return_value = json.dumps(data).encode()
        context = MagicMock()
        context.__enter__.return_value = response
        return context

    def smtp_config(self, security="starttls"):
        os.environ.update(SMTP_HOST="mail.example.com", SMTP_FROM="Status <status@example.com>",
                          SMTP_TO="alice@example.com,bob@example.com", SMTP_SECURITY=security,
                          SMTP_USER="status@example.com", SMTP_PASSWORD="example-only")

    def test_snapshot_success(self):
        data = self.snapshot()
        self.assertEqual(data["collectionErrors"], [])
        self.assertEqual(data["service"]["activeIncidentCount"], 0)
        self.assertEqual(data["performance"]["requestCount"], 30)
        self.assertIn("命中率 80%", report.format_text(data))

    def test_api_outage_still_generates_report(self):
        with patch.object(report, "get_json", side_effect=URLError("secret-url")), \
                patch.object(report, "docker_stats", return_value=[{"error": "Docker 不可用"}]):
            data = report.build_report_data("http://localhost:8088")
        text = report.format_text(data)
        self.assertEqual(len(data["collectionErrors"]), 4)
        self.assertIsNone(data["service"]["activeIncidentCount"])
        self.assertIn("[采集异常]", text)
        self.assertIn("QPS：未知", text)
        self.assertNotIn("secret-url", text)
        self.assertIn("Docker 不可用", text)

    def test_partial_failure_keeps_successful_metrics(self):
        with patch.object(report, "get_json", side_effect=[{}, URLError("timeout"), {"qps": 42}, {}]), \
                patch.object(report, "docker_stats", return_value=[]):
            data = report.build_report_data("http://localhost:8088")
        self.assertEqual(data["performance"]["qps"], 42)
        self.assertEqual(data["collectionErrors"][0]["source"], "status")

    def test_invalid_json_types_become_collection_errors(self):
        for value in ([], {"overview": None}, {"latency": []}, {"incidents": "invalid"}):
            with self.subTest(value=value), patch.object(report, "get_json", return_value=value), \
                    patch.object(report, "docker_stats", return_value=[]):
                data = report.build_report_data("http://localhost:8088")
                self.assertEqual(len(data["collectionErrors"]), 4)
                self.assertIn("采集异常", report.format_text(data))

    def test_docker_failure_is_reported(self):
        with patch.object(report.subprocess, "run", side_effect=FileNotFoundError("docker")):
            self.assertIn("error", report.docker_stats()[0])

    def test_docker_stats_are_parsed(self):
        result = MagicMock(returncode=0, stdout="api|1%|100MiB / 512MiB\ninvalid\n", stderr="")
        with patch.object(report.subprocess, "run", return_value=result):
            self.assertEqual(report.docker_stats(), [{"name": "api", "cpu": "1%", "memory": "100MiB / 512MiB"}])

    def test_feishu_signature_reference_vector(self):
        # Independent vector calculated with Node.js crypto, not this function.
        self.assertEqual(report.feishu_signature("1700000000", "example-secret"),
                         "Gs3YRutIJpVN2FvEXKXHU6bEj2pVdSriQ0niig9EbUk=")

    def test_feishu_signed_payload(self):
        os.environ.update(FEISHU_WEBHOOK_URL="https://open.feishu.cn/example", FEISHU_SECRET="example-secret")
        with patch.object(report.time, "time", return_value=1700000000), \
                patch.object(report.urllib.request, "urlopen", return_value=self.webhook_response({"code": 0})) as send:
            self.assertTrue(report.send_feishu("日报"))
        payload = json.loads(send.call_args.args[0].data)
        self.assertEqual(payload["timestamp"], "1700000000")
        self.assertEqual(payload["sign"], "Gs3YRutIJpVN2FvEXKXHU6bEj2pVdSriQ0niig9EbUk=")
        self.assertEqual(payload["content"]["text"], "日报")

    def test_feishu_legacy_success(self):
        os.environ["FEISHU_WEBHOOK_URL"] = "https://open.feishu.cn/example"
        with patch.object(report.urllib.request, "urlopen", return_value=self.webhook_response({"StatusCode": 0})):
            self.assertTrue(report.send_feishu("report"))

    def test_feishu_http_success_is_not_business_success(self):
        os.environ["FEISHU_WEBHOOK_URL"] = "https://open.feishu.cn/example"
        for response in ({"code": 19021}, {}, [], {"code": 1, "StatusCode": 0}, {"code": False}, {"code": "0"}):
            with self.subTest(response=response), patch.object(report.urllib.request, "urlopen",
                                                               return_value=self.webhook_response(response)):
                with self.assertRaises(RuntimeError):
                    report.send_feishu("report")

    def test_feishu_missing_url_fails_before_network(self):
        with self.assertRaises(RuntimeError):
            report.send_feishu("report")

    def test_feishu_rejects_plaintext_transport(self):
        os.environ["FEISHU_WEBHOOK_URL"] = "http://open.feishu.cn/example"
        with self.assertRaises(RuntimeError):
            report.send_feishu("report")

    def test_email_starttls_and_envelope(self):
        self.smtp_config()
        with patch.object(report.smtplib, "SMTP") as factory:
            client = factory.return_value.__enter__.return_value
            client.send_message.return_value = {}
            self.assertTrue(report.send_email("中文日报"))
            factory.assert_called_once_with("mail.example.com", 587, timeout=15)
            client.starttls.assert_called_once()
            tls = client.starttls.call_args.kwargs["context"]
            self.assertEqual(tls.verify_mode, ssl.CERT_REQUIRED)
            self.assertTrue(tls.check_hostname)
            self.assertEqual([call[0] for call in client.mock_calls],
                             ["ehlo", "starttls", "ehlo", "login", "send_message"])
            client.login.assert_called_once_with("status@example.com", "example-only")
            args = client.send_message.call_args
            self.assertEqual(args.kwargs["from_addr"], "status@example.com")
            self.assertEqual(args.kwargs["to_addrs"], ["alice@example.com", "bob@example.com"])
            self.assertIn("中文日报", args.args[0].get_content())

    def test_email_ssl(self):
        self.smtp_config("ssl")
        with patch.object(report.smtplib, "SMTP_SSL") as factory:
            client = factory.return_value.__enter__.return_value
            client.send_message.return_value = {}
            report.send_email("report")
            self.assertEqual(factory.call_args.args, ("mail.example.com", 465))
            self.assertTrue(factory.call_args.kwargs["context"].check_hostname)
            client.starttls.assert_not_called()

    def test_email_invalid_config_fails_before_network(self):
        cases = [{"SMTP_HOST": ""}, {"SMTP_FROM": ""}, {"SMTP_TO": ""}, {"SMTP_SECURITY": "none"},
                 {"SMTP_PORT": "abc"}, {"SMTP_PORT": "0"}, {"SMTP_PORT": "65536"},
                 {"SMTP_USER": ""}, {"SMTP_PASSWORD": ""}, {"SMTP_TO": "not-an-address"},
                 {"SMTP_TO": "a@example.com\nBcc:b@example.com"},
                 {"SMTP_FROM": "a@example.com,b@example.com"}]
        for changes in cases:
            with self.subTest(changes=changes):
                self.smtp_config()
                with patch.dict(os.environ, changes), self.assertRaises(RuntimeError):
                    report.send_email("report")

    def test_email_partial_refusal_is_not_success(self):
        self.smtp_config()
        with patch.object(report.smtplib, "SMTP") as factory:
            factory.return_value.__enter__.return_value.send_message.return_value = {"alice@example.com": (550, b"rejected")}
            with self.assertRaises(RuntimeError):
                report.send_email("report")

    def test_tls_failure_does_not_login_or_send(self):
        self.smtp_config()
        with patch.object(report.smtplib, "SMTP") as factory:
            client = factory.return_value.__enter__.return_value
            client.starttls.side_effect = smtplib.SMTPNotSupportedError("STARTTLS unavailable")
            with self.assertRaises(smtplib.SMTPNotSupportedError):
                report.send_email("report")
            client.login.assert_not_called()
            client.send_message.assert_not_called()

    def run_cli(self, arguments):
        output, errors = io.StringIO(), io.StringIO()
        with patch.object(report, "build_report_data", return_value=self.snapshot()), \
                contextlib.redirect_stdout(output), contextlib.redirect_stderr(errors):
            code = report.main(arguments)
        return code, output.getvalue(), errors.getvalue()

    def test_cli_default_never_sends_even_with_credentials(self):
        self.smtp_config()
        os.environ["FEISHU_WEBHOOK_URL"] = "https://open.feishu.cn/example"
        with patch.object(report, "send_email") as mail, patch.object(report, "send_feishu") as feishu:
            code, output, _ = self.run_cli(["--format", "json"])
            self.assertEqual(code, 0)
            self.assertEqual(json.loads(output)["service"]["apiStatus"], "UP")
            mail.assert_not_called()
            feishu.assert_not_called()

    def test_cli_individual_send_flags(self):
        for flag, expected in (("--send-email", "email"), ("--send-feishu", "feishu")):
            with self.subTest(flag=flag), patch.object(report, "send_email") as mail, \
                    patch.object(report, "send_feishu") as feishu:
                code, _, _ = self.run_cli([flag])
                self.assertEqual(code, 0)
                self.assertEqual(mail.call_count, int(expected == "email"))
                self.assertEqual(feishu.call_count, int(expected == "feishu"))

    def test_cli_failure_does_not_block_other_destination_or_leak_secrets(self):
        with patch.object(report, "send_feishu", side_effect=URLError("secret-webhook-token")), \
                patch.object(report, "send_email", return_value=True) as mail:
            code, _, errors = self.run_cli(["--send-feishu", "--send-email"])
            self.assertEqual(code, 1)
            mail.assert_called_once()
            self.assertNotIn("secret-webhook-token", errors)
            self.assertIn("URLError", errors)


if __name__ == "__main__":
    unittest.main()
