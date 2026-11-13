"""fast.py 单步命令（AGENTS.md §4）：ls / tap / off / scan / only / run / check 与参数解析、错误出口。

同一模式见 test_ui_session.py：替身化读屏连接后调 fast.main()，断言打印与退出码。
"""
import os
os.environ.setdefault("AAC_NO_SOUND", "1")     # 客户端提示音：测试里不出声
import contextlib
import io
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tool"))
import fast


def phone():
    """连上替身读屏的 Phone：返回前已过 __init__ 的全部设备探针。"""
    d = Mock()
    d.settings = {}
    d.window_size.return_value = (1080, 2400)
    with patch.object(fast, "connect", return_value=d):
        return fast.Phone("test")


def cli(p, argv):
    """跑一次 fast.main()：main() 内部自建 Phone，这里把构造换成预备好的 p。

    返回 (退出码, stdout, stderr)：check 的失败路径用 sys.exit(字符串)，原因收进 stderr。
    """
    out, err = io.StringIO(), io.StringIO()
    with patch.object(fast, "Phone", return_value=p):
        with patch.object(sys, "argv", ["fast.py"] + argv), \
             contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            try:
                fast.main()
                return 0, out.getvalue(), err.getvalue()
            except SystemExit as e:
                if isinstance(e.code, str):        # sys.exit("人话")：原因在退出码里
                    err.write(e.code + "\n")
                    return 1, out.getvalue(), err.getvalue()
                return e.code or 0, out.getvalue(), err.getvalue()


def cli_raising(exception, argv):
    """Phone 构造就失败的场景：不预备实例，让 fast.Phone 直接抛。"""
    out, err = io.StringIO(), io.StringIO()
    with patch.object(fast, "Phone", side_effect=exception), \
         patch.object(sys, "argv", ["fast.py"] + argv), \
         contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
        try:
            fast.main()
            return 0, out.getvalue(), err.getvalue()
        except SystemExit as e:
            code = e.code if isinstance(e.code, int) else 1
            return code, out.getvalue(), err.getvalue()


class ParseTests(unittest.TestCase):
    def test_no_command_prints_help_and_exits_zero(self):
        code, out, _ = cli(phone(), [])
        self.assertEqual(code, 0)
        self.assertIn("fast.py", out)

    def test_help_flag_prints_usage(self):
        for flag in (["--help"], ["-h"]):
            code, out, _ = cli(phone(), flag)
            self.assertEqual(code, 0)
            self.assertIn("fast.py", out)

    def test_missing_serial_exits_one_with_help(self):
        code, out, _ = cli(phone(), ["ls"])
        self.assertEqual(code, 1)
        self.assertIn("fast.py", out)

    def test_unknown_command_prints_help(self):
        p = phone()
        with patch.object(fast, "Phone", return_value=p), \
             patch.object(sys, "argv", ["fast.py", "-s", "S", "frobnicate"]), \
             contextlib.redirect_stdout(io.StringIO()) as out:
            fast.main()
        self.assertIn("fast.py", out.getvalue())


class CheckCommandTests(unittest.TestCase):
    def test_check_passes_on_unlocked_screen_with_nodes(self):
        p = phone()
        with patch.object(p, "locked", return_value=False), \
             patch.object(p, "tree", return_value=fast.ET.fromstring("<h><node/></h>")):
            code, out, _ = cli(p, ["-s", "S", "check"])
        self.assertEqual(code, 0)
        self.assertIn("读屏检查通过", out)
        self.assertIn("1080 × 2400", out)

    def test_check_stops_when_phone_is_locked(self):
        p = phone()
        with patch.object(p, "locked", return_value=True):
            code, _, err = cli(p, ["-s", "S", "check"])
        self.assertEqual(code, 1)
        self.assertIn("手机锁屏了", err)

    def test_check_stops_when_no_nodes_readable(self):
        p = phone()
        with patch.object(p, "locked", return_value=False), \
             patch.object(p, "tree", return_value=fast.ET.fromstring("<h/>")):
            code, _, err = cli(p, ["-s", "S", "check"])
        self.assertEqual(code, 1)
        self.assertIn("没有读到页面节点", err)


class SingleStepTests(unittest.TestCase):
    def test_ls_prints_current_page(self):
        p = phone()
        with patch.object(p, "ls") as ls:
            code, _, _ = cli(p, ["-s", "S", "ls"])
        self.assertEqual(code, 0)
        ls.assert_called_once()

    def test_tap_clicks_each_text_then_lists(self):
        p = phone()
        with patch.object(p, "tap") as tap, patch.object(p, "ls") as ls:
            code, _, _ = cli(p, ["-s", "S", "tap", "隐私", "广告"])
        self.assertEqual(code, 0)
        self.assertEqual([c.args for c in tap.call_args_list], [("隐私",), ("广告",)])
        ls.assert_called_once()

    def test_off_reports_count(self):
        p = phone()
        with patch.object(p, "off", return_value=2):
            code, out, _ = cli(p, ["-s", "S", "off"])
        self.assertEqual(code, 0)
        self.assertIn("关了 2 个", out)

    def test_off_keeps_named_titles(self):
        p = phone()
        with patch.object(p, "off", return_value=1) as off:
            cli(p, ["-s", "S", "off", "帮助,安全"])
        self.assertEqual(off.call_args.kwargs["keep"], ["帮助", "安全"])

    def test_only_reports_count(self):
        p = phone()
        with patch.object(p, "off", return_value=1):
            code, out, _ = cli(p, ["-s", "S", "only", "个性化推荐,开屏广告"])
        self.assertEqual(code, 0)
        self.assertIn("关了 1 个", out)

    def test_scan_lists_every_switch_and_summary(self):
        p = phone()
        rows = [("内容推荐", True, (100, 200)), ("个性化广告", False, (100, 400))]
        with patch.object(p, "switches", return_value=rows), \
             patch.object(p, "scroll_more", return_value=False):
            code, out, _ = cli(p, ["-s", "S", "scan"])
        self.assertEqual(code, 0)
        self.assertIn("[开] 内容推荐", out)
        self.assertIn("[关] 个性化广告", out)
        self.assertIn("2 个开关，开着 1 个", out)

    def test_scan_scrolls_until_no_more(self):
        p = phone()
        calls = []
        def switches():
            calls.append("read")
            return [("开关A", True, (100, 200))]
        with patch.object(p, "switches", side_effect=switches), \
             patch.object(p, "scroll_more", side_effect=[True, True, False]):
            code, out, _ = cli(p, ["-s", "S", "scan"])
        self.assertEqual(code, 0)
        self.assertEqual(len(calls), 3)          # 翻到底为止，不空转 60 轮
        self.assertIn("1 个开关", out)

    def test_scan_with_nothing_found_falls_back_to_top_listing(self):
        p = phone()
        with patch.object(p, "switches", return_value=[]), \
             patch.object(p, "scroll_more", return_value=False), \
             patch.object(p, "top") as top, patch.object(p, "ls") as ls:
            code, out, _ = cli(p, ["-s", "S", "scan"])
        self.assertEqual(code, 0)
        self.assertIn("0 个开关", out)
        top.assert_called_once()
        ls.assert_called_once()

    def test_ls_body_prints_focus_texts_and_switches(self):
        """ls 实体：前台包、整页文字、逐个开关带状态与坐标（AGENTS.md §4 的第一步就看它）。"""
        p = phone()
        root = fast.ET.fromstring("<h/>")
        with patch.object(p, "tree", return_value=root), \
             patch.object(p, "focus", return_value="com.android.settings"), \
             patch.object(p, "texts", return_value=[("设置", None, (0, 0)), ("蓝牙", None, (0, 100))]), \
             patch.object(p, "switches", return_value=[("开放设备", True, (900, 400))]):
            code, out, _ = cli(p, ["-s", "S", "ls"])
        self.assertEqual(code, 0)
        self.assertIn("· com.android.settings", out)
        self.assertIn("设置 / 蓝牙", out)
        self.assertIn("[开] 开放设备  @(900, 400)", out)


class RunRecipeTests(unittest.TestCase):
    def _recipe(self, directory, body="pass"):
        path = Path(directory, "recipe.py")
        path.write_text(body, encoding="utf-8")
        return str(path)

    def test_run_executes_recipe_and_reports_changes(self):
        p = phone()
        with tempfile.TemporaryDirectory() as directory:
            src = self._recipe(directory)
            with patch.object(p, "wait_unlocked", return_value=True):
                code, out, _ = cli(p, ["-s", "S", "run", src])
        self.assertEqual(code, 0)
        self.assertIn("机型脚本跑完", out)

    def test_run_stops_when_phone_stays_locked(self):
        p = phone()
        with tempfile.TemporaryDirectory() as directory:
            src = self._recipe(directory)
            with patch.object(p, "wait_unlocked", return_value=False):
                code, out, _ = cli(p, ["-s", "S", "run", src])
        self.assertEqual(code, 2)
        self.assertIn("手机一直锁屏", out)

    def test_run_records_changed_switches_to_log(self):
        p = phone()
        with tempfile.TemporaryDirectory() as directory:
            src = self._recipe(directory)
            p.changed = [("设置", "内容推荐")]
            with patch.object(p, "wait_unlocked", return_value=True):
                code, _, _ = cli(p, ["-s", "S", "run", src])
            self.assertEqual(code, 0)
            log = Path(directory, "run-log.txt")
            self.assertTrue(log.exists())
            self.assertIn("设置\t内容推荐", log.read_text(encoding="utf-8"))

    def test_run_version_mismatch_falls_back_to_safe_mode(self):
        p = phone()
        with tempfile.TemporaryDirectory() as directory:
            src = self._recipe(directory, 'BUILD = "OS9.9.9.9"\npass\n')
            # 前导把 reader.shell 换成 _resilient_shell；getprop 的应答从替身 reader 来
            p.reader.shell = Mock(return_value=Mock(output="OS1.0.1.0"))
            with patch.object(p, "wait_unlocked", return_value=True):
                code, out, _ = cli(p, ["-s", "S", "run", src])
        self.assertEqual(code, 0)
        self.assertIn("安全模式", out)
        self.assertTrue(p.safe_mode)

    def test_run_keeps_safe_mode_for_reference_script(self):
        p = phone()
        with tempfile.TemporaryDirectory() as directory:
            src = self._recipe(directory)
            with patch.object(p, "wait_unlocked", return_value=True):
                code, _, _ = cli(p, ["-s", "S", "run", "--ref", src])
        self.assertEqual(code, 0)
        self.assertTrue(p.safe_mode)


class ErrorPathTests(unittest.TestCase):
    def test_runtime_error_prints_reason_without_traceback(self):
        code, out, _ = cli_raising(RuntimeError("读屏服务被占用"), ["-s", "S", "check"])
        self.assertEqual(code, 1)
        self.assertIn("✗ 读屏服务被占用", out)
        self.assertNotIn("Traceback", out)

    def test_device_lost_becomes_plain_message(self):
        import adb_link
        code, out, _ = cli_raising(adb_link.DeviceLost("连接已断开"), ["-s", "S", "check"])
        self.assertEqual(code, 1)
        self.assertIn("✗ 连接已断开", out)
        self.assertNotIn("Traceback", out)

    def test_unrecognized_exception_reraises_after_swallowing_message(self):
        """failure_message 认不出的异常：原样抛出，不伪装成已收场（防御分支）。"""
        import adb_link
        p = phone()
        out, err = io.StringIO(), io.StringIO()
        with patch.object(fast, "Phone", return_value=p), \
             patch.object(adb_link, "failure_message", return_value=None), \
             patch.object(fast, "execute", side_effect=ValueError("boom")), \
             patch.object(sys, "argv", ["fast.py", "-s", "S", "check"]), \
             contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            with self.assertRaises(ValueError):
                fast.main()


if __name__ == "__main__":
    unittest.main()
