# 对比分析：Ai.Clean v1.1.1-1113v2 ↔ rddmwmddr/android-ad-cleaner v0.4.4

> 对比基准：本仓 tag `v1.1.1-1113v2`（7720236，含其后的 gui 自检提交 a741fa0）↔ 外部仓 tag `v0.4.4`（2026-11-13 完整克隆）
> 覆盖：谱系 / 规模 / 功能 / 引擎 / 测试 / CI / 发布 / 治理 全维度。仅「对方 Release 资产清单」一项因 API 限流+token 失效未能核验，如实标注。
>
> **一句话结论：同源分支，各自演化。对方在工程化、真机验证、社区治理上已明显反超；我们在语音播报、品牌成品、最新自检体系上独有。整体对方规模 +46%。**

---

## 一、谱系考证（fork 实锤，四条硬证据）

对方仓库是**从本仓 1.0.7 · 1109v5 时代 fork 出去的分支**，随后回退品牌、重置版本号独立演进：

| 证据 | 内容 |
|---|---|
| README 残留 | 其 `README.md` 第 5 行原文：「**1.0.7 · 1109v5** 已提供 Windows 安装版与便携版、Android arm64 签名版。下载只认本仓库 [Releases](https://github.com/Tri250/AlcedoStudio/releases)」——把**本仓 URL 写成他们的「本仓库」** |
| 文档残留 | 其 `docs/02-用AI智能体.md`：「请克隆 https://github.com/Tri250/AlcedoStudio，读仓库里的 AGENTS.md」 |
| 版本错位 | 其代码 `gui/app.py` VERSION = 0.4.4，但 README 停在本仓 1.0.7 时代的版本摘要 → fork 后重置了版本线 |
| 品牌回退 | 改回「肉呆去广告」（本仓 0.4.0 时代的旧名），本仓则演进为 Ai.Clean 1.x |

对其利弊：品牌/版本重置让他们摆脱 1.x 惯性，但 README/docs 的脏引用直接失分（用户点「本仓库 Releases」会跳到我们仓库）。对 README 已无 aigoodbro/官网跳转痕迹（品牌清除比我们更彻底）。

## 二、量化规模对比

| 维度 | 本仓 v1.1.1 | 对方 v0.4.4 | 差值 |
|---|---|---|---|
| Python 代码行 | 51,174 | **74,904** | 对方 +46% |
| Kotlin+Java 行 | **6,702** | 5,363 | 本仓 +25% |
| 机型脚本（目录） | 41 | **44** | 对方 +3 |
| Python 测试文件 | **153** | 146 | 本仓 +7 |
| 设备无关回归（自报） | 2,059（本轮实测） | **3,044**（其 CHANGELOG） | 对方 +985 |
| 前端用例文件 | 5 | 6 | 对方 +1 |
| gui 模块 | 12 | **16** | 对方 +4（鸿蒙×3、hdc_fetch、更新×2） |
| GitHub 工作流 | 3 | **9** | 对方 +6 |
| 静态类型检查 | 无 | **mypy 通过** | 对方 |
| 真机冒烟 | 无 | **有**（一加16 27屏逐屏+还原） | 对方 |
| 社区 issue 编号 | #870 级 | **#1235 级** | 对方大一个量级 |

## 三、功能矩阵（100% 覆盖逐项）

### 本仓独有（对方没有）

| 功能 | 证据 |
|---|---|
| **语音播报 TTS**（1.1.0） | 对方 android 源码 grep 无 TextToSpeech；本仓 view.py `announcements()` + Voice.kt + 更多面板开关 |
| 官网自动跳转移除（1.1.0） | 对方谱系停留在有该行为的年代，已在本仓移除 |
| Ai.Clean 品牌成品链 | 官网 index.html、下载区、品牌词库、更新说明全套 |
| 三轮同口径深度自检 | tool/（92%）→ android/（83%）→ gui/（88%），覆盖率量化+缺口分类方法成体系 |
| 手机版调度层/AdbBridge 专项测试 | Session 保活计数、结束通知三结局、电脑调试通道 17 项 |
| 确认页底栏修复（1.0.9） | OPPO Find X9 Pro 实测问题，`HorizontalFloatingToolbar` 替换 |

### 对方独有（本仓没有）

| 功能 | 证据 | 对我们的价值 |
|---|---|---|
| **鸿蒙 HarmonyOS 通道** | `harmony/` 客户端（desktop/entry/recipes.json）+ gui 的 harmony_bridge/client/engine/vocab + hdc_fetch | 远期；若有鸿蒙用户群再评估 |
| **客户端自更新安装** | gui/update_install.py + update_manifest.py | 中；我们目前只提示不自动装 |
| **失败现场存档与回放** | tool/captures.py：失败段 dump 落 `captures/`，`run.py captures [目录]` 只读查看占用（#1126） | **高**：用户报障质量跃升，诊断闭环 |
| **off() 先回页顶再拨** | 引擎改 `off()` 拨前回页顶、`top()` 翻到没有新文字为止（#1094） | **高**：长列表/旋转屏漏开关的真实修复 |
| 验证台账两维 + status-json | #1124，可机读验证状态 | 中 |
| 结果页验证覆盖度提示 | #1129/#1130：本次跑的脚本验证覆盖到什么程度 | 中 |
| 机型脚本模块化工具链 | recipe_meta/nav/source/steps + tool/phone/ 子目录 | 中 |
| **REL↔Release 对账工具** | tool/release_assets.py：文件名/大小/sha 三级核对，退出码区分不一致与网络抖动（#1136） | **高**：我们 index.html REL 同步纯手工 |
| 社区治理工作流 ×6 | stale、ticket-checks、triage-audit、commit-checks、DCO、conventional、dependency-audit/review | 低-中（我们 issue 量级小） |
| **真机冒烟流程** | 一加16（PYB110）：覆盖安装+启动+27屏逐屏+还原通过，397 秒整轮脚本 | **高**：可复制的真机验收清单 |
| mypy 全量通过 | 其 CHANGELOG 自报 | 中（增量接入） |

### 两边持平

MIT 许可、AGENTS.md 产品契约骨架、adb 快速操作工具、通知渠道扫描、快应用处理、备份还原、安全模式、41+ 机型脚本主体、Compose 手机版、pywebview 桌面版、jsdom 前端测试。

## 四、发布产物对比

| 项 | 本仓 v1.1.1（已核验） | 对方 v0.4.4（未能核验*） |
|---|---|---|
| 产物 | Windows 安装版 30.6MB + 便携版 45.5MB + 安卓签名 APK 52.8MB | 未知 |
| 校验 | SHA256SUMS-android.txt 上传且回读核对 OK | 未知 |
| 签名 | 固定证书 CN=Ai.Clean Release（CI 缺 secret，本地重签流程） | 未知 |
| 对账 | REL 同步手工 | 有 release_assets.py 自动对账 |

\* 对方 Release 资产清单因 GitHub API 匿名限流 + token 失效无法核验，此行如实空缺。

## 五、质量口径的诚实说明

- 对方「3044 项回归」取自其 CHANGELOG 自报，未复跑；本仓 2059 为本轮 discover 实测。两者口径细节（是否含参数化展开）不同，只作量级参考。
- 对方有真机冒烟记录，本仓自 0.4.0 后无真机整轮——**这是对方与我们最实质的差距项**，本环境结构性无法补。
- 对方 mypy 全量通过属自报；本仓 ruff 双规则+CI 基线每轮实测。

## 六、建议行动清单（按价值排序）

1. **立即吸收（纯逻辑、可测、低成本）**：
   - captures.py 失败现场存档/回放机制
   - off() 回页顶 + top() 终止条件修复（长列表可靠性）
   - release_assets.py 的 REL↔Release 对账（保护我们的发版链）
2. **待决策**：mypy 增量接入、治理类工作流（stale/dependency-audit）、结果页验证覆盖度提示
3. **真机缺口**：抄对方的「27 屏逐屏 + 覆盖安装 + 还原」冒烟清单，作为有真机时的验收脚本模板
4. **保持领先**：语音播报、品牌链路、自检方法体系
5. 远期：鸿蒙通道（有用户需求再评估）

## 七、对方失分项（供参考，不做处理）

- README/docs 多处残留指向本仓库的 URL（用户会被引流）
- 版本摘要与代码版本错位（README 停在 1.0.7 时代）
