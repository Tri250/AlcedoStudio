"""手机版语音播报的句子怎么定：view.announcements 的用例（不需要设备）。"""
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "android" / "app" / "src" / "main" / "python"))

from aac_android import view


def run(**kw):
    base = {"running": True, "idx": 1, "total": 4, "title": "备份改动", "paused": False, "pause": False,
            "stalled": 0, "results": [], "cancel": False, "outcome": "ok"}
    base.update(kw)
    return base


class VoiceAnnouncements(unittest.TestCase):
    def setUp(self):
        self.say = lambda prev, cur: view.announcements(None, prev, cur)

    def test_in_exports(self):
        self.assertIs(view.EXPORTS["announcements"], view.announcements)

    def test_start(self):
        self.assertEqual(self.say(None, run()), ["开始清理，共 4 项"])

    def test_start_without_total(self):
        self.assertEqual(self.say(None, run(total=0)), ["开始清理"])

    def test_resume_into_middle_after_reconnect(self):
        self.assertEqual(self.say(None, run(idx=3, title="广告组件")), ["继续清理，第 3 项，广告组件"])

    def test_step_advance(self):
        self.assertEqual(self.say(run(), run(idx=2, title="营销通知")), ["第 2 项，营销通知"])

    def test_same_step_is_quiet(self):
        self.assertEqual(self.say(run(), run()), [])

    def test_pause_and_resume(self):
        self.assertEqual(self.say(run(), run(paused=True)), ["已暂停"])
        self.assertEqual(self.say(run(paused=True), run()), ["继续清理"])

    def test_stalled_spoken_once(self):
        self.assertEqual(self.say(run(), run(stalled=75)), ["清理好像停住了，请回来看看"])
        self.assertEqual(self.say(run(stalled=75), run(stalled=90)), [])

    def test_finish_ok_skips_backup_and_blank(self):
        cur = run(running=False, idx=4, results=[
            {"kind": "backup", "title": "备份改动", "rc": 0, "summary": "已保存", "skipped": False},
            {"kind": "recipe", "title": "机型脚本", "rc": 0, "summary": "关了 30 个开关", "skipped": False},
            {"kind": "channels", "title": "营销通知", "rc": 0, "summary": "关了 5 个渠道", "skipped": False},
            {"kind": "quickapp", "title": "广告组件", "rc": 0, "summary": "", "skipped": True},
        ])
        self.assertEqual(self.say(run(), cur),
                         ["清理完成：机型脚本，关了 30 个开关；营销通知，关了 5 个渠道"])

    def test_finish_ok_speaks_at_most_four_parts(self):
        cur = run(running=False, idx=4, results=[
            {"kind": "recipe", "title": f"步骤{n}", "rc": 0, "summary": f"关了 {n} 个开关", "skipped": False}
            for n in range(6)
        ])
        said = self.say(run(), cur)[0]
        self.assertTrue(said.startswith("清理完成："))
        self.assertEqual(said.count("；"), 3)

    def test_finish_failed(self):
        cur = run(running=False, idx=4, outcome="failed", results=[
            {"kind": "recipe", "title": "机型脚本", "rc": 2, "summary": "1 段未完成", "skipped": False},
        ])
        self.assertEqual(self.say(run(), cur), ["清理没做完，有步骤没成功，请回来看看结果"])

    def test_finish_stopped(self):
        self.assertEqual(self.say(run(), run(running=False, cancel=True, outcome="stopped")), ["已停止清理"])

    def test_finish_wins_over_step_change(self):
        cur = run(running=False, idx=4, outcome="ok", results=[
            {"kind": "recipe", "title": "机型脚本", "rc": 0, "summary": "关了 30 个开关", "skipped": False},
        ])
        said = self.say(run(), cur)
        self.assertEqual(len(said), 1)
        self.assertTrue(said[0].startswith("清理完成"))


if __name__ == "__main__":
    unittest.main()
