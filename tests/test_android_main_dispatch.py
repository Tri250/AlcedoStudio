"""手机版调度层（aac_android/main.py）：call() 分发、Session 计数（#662）、in_front 通知三结局。

same 模式见 test_android_view.py：import 真实模块，替身化 bridge / native / screen 三处边界。
"""
import json
import os
os.environ.setdefault("AAC_NO_SOUND", "1")     # 客户端提示音：测试里不出声
import sys
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "android" / "app" / "src" / "main" / "python"))

from aac_android import main  # noqa: E402


class FakeBridge:
    """替身通道：记录 exec 的命令，返回固定应答。"""

    def __init__(self, code=0):
        self.calls, self.code = [], code

    def exec(self, cmd, cb=None, timeout=None):
        self.calls.append(cmd)
        return Mock(code=self.code)


class FakeNative:
    """替身 Native：记录 busy / done / capsule 调用（属性名不能和方法同名，会遮蔽）。"""

    def __init__(self):
        self.busy_log, self.done_log, self.capsule_log = [], [], []
        self.bring_count = 0

    def busy(self, on, background):
        self.busy_log.append((on, background))

    def done(self, title, text):
        self.done_log.append((title, text))

    def capsule(self, on, text):
        self.capsule_log.append((on, text))

    def bringToFront(self):
        self.bring_count += 1


def fresh_module():
    """每例一个干净的 Session / bridge / native：Session 的计数跨例会串。"""
    m = main
    m.SESSION = main.Session()
    m._ready.set()
    return m


def fake_screen(background=True, started=None):
    screen = Mock()
    screen.active = object() if background else None
    screen.starting = False
    screen.start.return_value = started if started is not None else ("display" if background else None)
    screen.release = Mock()
    return screen


class CallDispatchTests(unittest.TestCase):
    def setUp(self):
        self.m = fresh_module()
        self.m.API = Mock()
        self.m.API.detect = Mock(return_value={"devices": 1})

    def test_plain_api_method_returns_ok_envelope(self):
        out = json.loads(self.m.call("detect", "[]"))
        self.assertEqual(out, {"ok": {"devices": 1}})

    def test_view_method_routes_to_exports(self):
        self.m.API.status = Mock(return_value={"steps": ["桌面"]})
        self.m.view = Mock()
        self.m.view.EXPORTS = {"poll": lambda api, since=0: {"log": since}}
        out = json.loads(self.m.call("view.poll", json.dumps([7])))
        self.assertEqual(out, {"ok": {"log": 7}})

    def test_view_method_unknown_name_is_error_envelope(self):
        self.m.view = Mock()
        self.m.view.EXPORTS = {}
        out = json.loads(self.m.call("view.nope", "[]"))
        self.assertIn("error", out)
        self.assertIn("KeyError", out["error"])

    def test_underscore_method_rejected(self):
        out = json.loads(self.m.call("_child_env", "[]"))
        self.assertIn("error", out)
        self.assertIn("AttributeError", out["error"])

    def test_api_none_is_error_envelope(self):
        self.m.API = None
        out = json.loads(self.m.call("detect", "[]"))
        self.assertIn("error", out)
        self.assertIn("AttributeError", out["error"])

    def test_args_default_to_empty_list(self):
        self.m.call("detect", "")
        self.m.API.detect.assert_called_once_with()

    def test_exception_becomes_error_envelope_not_crash(self):
        self.m.API.detect = Mock(side_effect=RuntimeError("boom"))
        out = json.loads(self.m.call("detect", "[]"))
        self.assertIn("error", out)
        self.assertIn("RuntimeError: boom", out["error"])


class SessionCountingTests(unittest.TestCase):
    """#662：三样保活按进入次数计数，最后一个离开的才收；半路失败不能把计数留在 1。"""

    def test_second_enter_reuses_first_background_without_restarting(self):
        m = fresh_module()
        screen = fake_screen(background=True)
        bridge, native = FakeBridge(), FakeNative()
        with patch.object(m, "screen", screen), \
             patch.object(m, "bridge", Mock(get=Mock(return_value=bridge))), \
             patch.object(m, "native", native):
            first = m.SESSION.enter("com.aigoodbro.qingjing", "去广告")
            second = m.SESSION.enter("com.aigoodbro.qingjing", "还原")
        self.assertTrue(first)
        self.assertTrue(second)                       # 沿用第一块的屏
        self.assertEqual(screen.start.call_count, 1)  # 没有建第二块
        self.assertEqual(native.busy_log, [(True, True)])

    def test_main_screen_mode_when_background_unavailable(self):
        m = fresh_module()
        screen = fake_screen(background=False, started=None)
        bridge, native = FakeBridge(), FakeNative()
        with patch.object(m, "screen", screen), \
             patch.object(m, "bridge", Mock(get=Mock(return_value=bridge))), \
             patch.object(m, "native", native):
            bg = m.SESSION.enter("com.aigoodbro.qingjing", "去广告")
        self.assertFalse(bg)
        self.assertEqual(native.capsule_log[-1], (True, "Ai.Clean去广告中"))

    def test_leave_keeps_alive_until_last(self):
        m = fresh_module()
        screen = fake_screen()
        with patch.object(m, "screen", screen), \
             patch.object(m, "bridge", Mock(get=Mock(return_value=FakeBridge()))), \
             patch.object(m, "native", FakeNative()):
            m.SESSION.enter("pkg", "去广告")
            m.SESSION.enter("pkg", "还原")
            first = m.SESSION.leave("pkg")
            second = m.SESSION.leave("pkg")
        self.assertFalse(first)
        self.assertTrue(second)
        screen.release.assert_called_once()

    def test_enter_failure_rolls_back_count_and_keeps_alive_removed(self):
        """建屏半路失败：计数退回 0，保活与胶囊仍撤掉（#746），之后还能重进。"""
        m = fresh_module()
        screen = fake_screen()
        screen.start = Mock(side_effect=RuntimeError("断线"))
        native = FakeNative()
        with patch.object(m, "screen", screen), \
             patch.object(m, "bridge", Mock(get=Mock(return_value=FakeBridge()))), \
             patch.object(m, "native", native):
            with self.assertRaises(RuntimeError):
                m.SESSION.enter("pkg", "去广告")
            self.assertEqual(m.SESSION.count, 0)
            self.assertIn((False, False), native.busy_log)
            screen.start.side_effect = None              # 通道恢复：重进要走得通
            bg = m.SESSION.enter("pkg", "去广告")
        self.assertTrue(bg)                              # 重进走得通，计数没卡死


class InFrontNotificationTests(unittest.TestCase):
    """结束通知三结局：做完 / 用户停止 / 失败文案不同（停止与失败不能看起来像成功）。"""

    def _run(self, m, fn, stopped=None, screen=None, bridge=None):
        screen = screen or fake_screen()
        native = FakeNative()
        with patch.object(m, "screen", screen), \
             patch.object(m, "bridge", Mock(get=Mock(return_value=bridge or FakeBridge()))), \
             patch.object(m, "native", native):
            out = m.in_front(fn, "pkg", "去广告", stopped)()
        return out, native, screen

    def test_success_notifies_done_in_background(self):
        m = fresh_module()
        _, native, _ = self._run(m, lambda: 42)
        self.assertEqual(native.done_log, [("去广告做完了", "点这里查看结果")])

    def test_stopped_notifies_stopped_not_done(self):
        m = fresh_module()
        _, native, _ = self._run(m, lambda: None, stopped=lambda: True)
        self.assertEqual(native.done_log, [("已停止", "点这里查看结果")])

    def test_failure_notifies_failure_not_done(self):
        m = fresh_module()
        native = FakeNative()
        with patch.object(m, "screen", fake_screen()), \
             patch.object(m, "bridge", Mock(get=Mock(return_value=FakeBridge()))), \
             patch.object(m, "native", native):
            with self.assertRaises(RuntimeError):
                m.in_front(lambda: (_ for _ in ()).throw(RuntimeError("boom")), "pkg", "去广告")()
        self.assertEqual(native.done_log, [("去广告失败", "点这里查看原因")])

    def test_main_screen_mode_returns_to_app_and_keeps_alive_removed(self):
        m = fresh_module()
        screen = fake_screen(background=False, started=None)
        bridge = FakeBridge()
        _, native, _ = self._run(m, lambda: 1, screen=screen, bridge=bridge)
        self.assertEqual(native.done_log, [])                    # 主屏不发通知
        self.assertIn("am start -n pkg/com.ahuamiao.adcleaner.MainActivity", bridge.calls)
        self.assertEqual(native.busy_log[-1], (False, False))    # 屏幕锁照撤

    def test_return_home_failure_falls_back_to_bring_to_front(self):
        m = fresh_module()
        screen = fake_screen(background=False, started=None)
        bridge = FakeBridge(code=1)
        _, native, _ = self._run(m, lambda: 1, screen=screen, bridge=bridge)
        self.assertEqual(native.bring_count, 1)


class NativeFallbackTests(unittest.TestCase):
    def test_java_absent_falls_back_to_printing(self):
        """电脑上调试（没有 Java）：native.xxx() 退化成打印，不 ImportError 崩掉界面。"""
        with patch("builtins.__import__", side_effect=ImportError("no java")):
            native = main._Native()
            native.capsule(True, "测试")
        # 不抛即通过；lambda 只往 inproc._fallback 打印


if __name__ == "__main__":
    unittest.main()
