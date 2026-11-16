"""gui/ 深度自检补测：打包管线版本守卫、USB 诊断边界分支、提示音确定性、单文件页内嵌。

之前 gui/ 只有 import 副作用、打包入口和个别功能的回归；这四个都是纯逻辑或可离线跑的路径。
"""
import hashlib
import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV = {**{"PYTHONIOENCODING": "utf-8", "AAC_NO_SOUND": "1"}}


def run(*args, cwd=ROOT):
    return subprocess.run([sys.executable, *args], cwd=cwd, env={**ENV, **dict(__import__("os").environ)},
                          capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=120)


class VersionScriptTest(unittest.TestCase):
    """gui/version.py 是两条发版工作流读版本号的唯一来源（build-windows / android 都调它）。"""

    def test_prints_the_version_from_gui_app(self):
        app_src = (ROOT / "gui" / "app.py").read_text(encoding="utf-8")
        import re
        expected = re.search(r'VERSION = "([^"]+)"', app_src).group(1)
        out = run(str(ROOT / "gui" / "version.py"))
        self.assertEqual(out.returncode, 0, out.stderr)
        self.assertEqual(out.stdout.strip(), expected)


class UsbParseEdgeTests(unittest.TestCase):
    """#57-#118-#702 诊断链的边界：认不出的行跳过、字段被截断不炸。"""

    @classmethod
    def setUpClass(cls):
        sys.path.insert(0, str(ROOT / "gui"))            # app.py 顶层 `import winpreflight` 需要 gui/ 在 path 上
        spec = importlib.util.spec_from_file_location("gui_app", ROOT / "gui" / "app.py")
        cls.app = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(cls.app)

    def test_non_usb_lines_are_skipped(self):
        out = "蓝牙设备|Generic Bluetooth|OK\nUSB\\HID_1234&PID_0000|键盘|OK\n" \
              "USB\\VID_2717&PID_FF40|小米手机|OK|USBDevice|-"
        found = self.app.usb_parse(out)
        self.assertEqual(len(found), 1)                  # 只剩小米那一行
        self.assertEqual(found[0][0], "小米")

    def test_truncated_fields_default_to_empty(self):
        row = "USB\\VID_2A70&PID_0001"                   # 只有第一段：没有名字、状态、类、GUID
        found = self.app.usb_parse(row)
        self.assertEqual(found[0][1:4], ("", "", False))  # 名/状态空、不算 ADB 接口
        self.assertIsNone(found[0][5])                   # GUID 整段缺失 = 没读到（#702 口径）

    def test_empty_guid_entry_is_read_but_unregistered(self):
        found = self.app.usb_parse("USB\\VID_2A45&PID_0002|魅族|OK|USB|\n")
        self.assertEqual(found[0][5], ())                # 读到了但一个没登记 → 空元组

    def test_hw_id_rejects_wrong_bus(self):
        self.assertEqual(self.app.usb_hw_id("USB\\VID_271&PID_FF40"), "")   # VID 位数不够
        self.assertEqual(self.app.usb_hw_id("HID\\VID_2717&PID_FF40"), "")  # 总线不是 USB
        self.assertEqual(self.app.usb_hw_id(""), "")


class MakeChimeDeterminismTest(unittest.TestCase):
    """重新生成提示音必须得到同一份字节：改了音色没重跑脚本、或合成器变了都该红。"""

    def test_regenerating_produces_identical_bytes(self):
        wav = ROOT / "gui" / "web" / "chime.wav"
        self.assertTrue(wav.exists())
        before = hashlib.sha256(wav.read_bytes()).hexdigest()
        out = run(str(ROOT / "gui" / "make_chime.py"))
        self.assertEqual(out.returncode, 0, out.stderr)
        self.assertEqual(hashlib.sha256(wav.read_bytes()).hexdigest(), before,
                         "gui/web/chime.wav 与 make_chime.py 的输出不一致：改了 NOTES 要重新生成提交")


class StandaloneEmbedTest(unittest.TestCase):
    """单文件页内嵌（#669）：小图标原样 PNG、大图压 JPEG——跑一遍真实 index.html。"""

    def test_embeds_images_and_shrinks(self):
        out_html = Path(tempfile.mkdtemp()) / "standalone.html"
        out = run(str(ROOT / "gui" / "standalone.py"), str(out_html))
        self.assertEqual(out.returncode, 0, out.stderr)
        html = out_html.read_text(encoding="utf-8")
        self.assertIn("data:image/", html)               # 至少内嵌了一部分
        self.assertRegex(out.stdout, r"内嵌 \d+ 张图")
        self.assertNotIn('src="images/', html)           # 引用全部换成 data URI


if __name__ == "__main__":
    unittest.main()
