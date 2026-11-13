"""电脑调试通道（aac_android/bridge.py 的 AdbBridge）：ready / exec / daemon / dump / size 与 get() 选路。

subprocess 边界用 patch 换掉 bridge._run / _popen（模块在导入时就抓了改写前的那份，patch 模块属性即可）。
"""
import os
os.environ.setdefault("AAC_NO_SOUND", "1")     # 客户端提示音：测试里不出声
import subprocess
import sys
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "android" / "app" / "src" / "main" / "python"))

from aac_android import bridge  # noqa: E402


def result(code=0, out="", err=""):
    """替身 subprocess.CompletedProcess：真 _run 把流 decode 成 UTF-8 文本，这里照做。"""
    return Mock(returncode=code, stdout=out.encode(), stderr=err.encode())


class ReadyTests(unittest.TestCase):
    """ready() 自己调 _run 且 text=True：流是文本，与 exec 的字节透传不同。"""

    def test_device_state_is_ready(self):
        b = bridge.AdbBridge("/adb", "S")
        with patch.object(bridge, "_run", return_value=Mock(returncode=0, stdout="device\n", stderr="")):
            self.assertTrue(b.ready())
            self.assertTrue(b.ensure())

    def test_other_state_is_not_ready(self):
        b = bridge.AdbBridge("/adb", "S")
        with patch.object(bridge, "_run", return_value=Mock(returncode=0, stdout="offline", stderr="")):
            self.assertFalse(b.ready())
            self.assertFalse(b.ensure())


class ExecTests(unittest.TestCase):
    def test_passes_through_code_and_streams(self):
        b = bridge.AdbBridge("/adb", "S")
        with patch.object(bridge, "_run", return_value=result(3, "out\n", "err\n")) as run:
            r = b.exec("pm list packages")
        self.assertEqual((r.code, r.out, r.err, r.timed_out), (3, b"out\n", b"err\n", False))
        self.assertEqual(run.call_args.args[0][:3], ["/adb", "-s", "S"])

    def test_shell_v2_not_exec_out(self):
        """引擎要看退出码：用 shell（分开 stderr 并带回退出码），不用 exec-out。"""
        b = bridge.AdbBridge("/adb", "S")
        with patch.object(bridge, "_run", return_value=result()) as run:
            b.exec("getprop ro.x")
        self.assertEqual(run.call_args.args[0][3], "shell")

    def test_timeout_becomes_code_124_with_flag(self):
        b = bridge.AdbBridge("/adb", "S")
        with patch.object(bridge, "_run", side_effect=subprocess.TimeoutExpired("adb", 5)):
            r = b.exec("slow cmd")
        self.assertEqual((r.code, r.timed_out), (124, True))

    def test_default_timeout_is_ten_minutes(self):
        b = bridge.AdbBridge("/adb", "S")
        with patch.object(bridge, "_run", return_value=result()) as run:
            b.exec("long run")
        self.assertEqual(run.call_args.kwargs["timeout"], 600)


class DaemonTests(unittest.TestCase):
    def test_second_call_reuses_live_daemon(self):
        b = bridge.AdbBridge("/adb", "S")
        alive = Mock()
        alive.poll.return_value = None
        with patch.object(bridge, "_popen", return_value=alive) as popen:
            self.assertTrue(b.daemon("ui", "uiautomator"))
            self.assertFalse(b.daemon("ui", "uiautomator"))
        self.assertEqual(popen.call_count, 1)

    def test_dead_daemon_is_restarted(self):
        b = bridge.AdbBridge("/adb", "S")
        alive, dead = Mock(), Mock()
        alive.poll.return_value = None
        dead.poll.return_value = 1
        with patch.object(bridge, "_popen", side_effect=[alive, alive]):
            b.daemon("ui", "cmd")
            b.daemon("ui", "cmd")
        b.daemons["ui"] = dead
        with patch.object(bridge, "_popen", return_value=alive) as popen:
            self.assertTrue(b.daemon("ui", "cmd"))
        self.assertEqual(popen.call_count, 1)

    def test_stop_daemon_kills_and_forgets(self):
        b = bridge.AdbBridge("/adb", "S")
        alive = Mock()
        alive.poll.return_value = None
        with patch.object(bridge, "_popen", return_value=alive):
            b.daemon("ui", "cmd")
            b.stop_daemon("ui")
        alive.kill.assert_called_once()
        self.assertNotIn("ui", b.daemons)

    def test_stop_unknown_daemon_is_noop(self):
        b = bridge.AdbBridge("/adb", "S")
        b.stop_daemon("nope")          # 不抛即通过


class DumpSizeTests(unittest.TestCase):
    def test_dump_reads_the_written_file(self):
        b = bridge.AdbBridge("/adb", "S")
        with patch.object(bridge, "_run", return_value=result(0, "<h/>", "")) as run:
            r = b.dump(0)
        self.assertEqual((r.code, r.out), (0, b"<h/>"))
        self.assertIn("/data/local/tmp/aac-dump.xml", run.call_args.args[0][-1])

    def test_size_returns_width_height(self):
        b = bridge.AdbBridge("/adb", "S")
        with patch.object(bridge, "_run", return_value=result(0, "Physical size: 1080x2400\n")):
            r = b.size(0)
        self.assertEqual((r.code, r.out), (0, b"1080 2400"))

    def test_size_takes_last_match_when_override_active(self):
        b = bridge.AdbBridge("/adb", "S")
        with patch.object(bridge, "_run", return_value=result(0, "Physical size: 1080x2400\nOverride size: 720x1280\n")):
            r = b.size(0)
        self.assertEqual(r.out, b"720 1280")

    def test_size_without_dimensions_is_error(self):
        b = bridge.AdbBridge("/adb", "S")
        with patch.object(bridge, "_run", return_value=result(0, "")):
            r = b.size(0)
        self.assertEqual((r.code, r.err), (1, b"wm size returned nothing"))


class ClipboardTests(unittest.TestCase):
    def test_clipboard_prints_on_desktop_debug_path(self):
        b = bridge.AdbBridge("/adb", "S")
        with patch("sys.__stderr__") as err:
            b.set_clipboard("hello")
        self.assertIn("hello", str(err.write.call_args_list))


class GetSelectionTests(unittest.TestCase):
    def test_bridge_adb_env_selects_adb_bridge_and_caches(self):
        saved = bridge._bridge
        try:
            bridge._bridge = None
            with patch.dict(os.environ, {"AAC_BRIDGE_ADB": "/adb", "AAC_BRIDGE_SERIAL": "S"}):
                first = bridge.get()
                second = bridge.get()
            self.assertIsInstance(first, bridge.AdbBridge)
            self.assertIs(first, second)                 # 同一个实例，不再重建
            self.assertEqual((first.adb, first.serial), ("/adb", "S"))
        finally:
            bridge._bridge = saved

    def test_without_bridge_adb_needs_java(self):
        """没设 AAC_BRIDGE_ADB 时走 ShizukuBridge：电脑上没有 java 模块，报 ImportError 而不是静默换路。"""
        saved = bridge._bridge
        try:
            bridge._bridge = None
            env = {k: v for k, v in os.environ.items() if k != "AAC_BRIDGE_ADB"}
            with patch.dict(os.environ, env, clear=True):
                with self.assertRaises(ImportError):
                    bridge.get()
        finally:
            bridge._bridge = saved


if __name__ == "__main__":
    unittest.main()
