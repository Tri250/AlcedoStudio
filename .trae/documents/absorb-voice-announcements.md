# 计划：为 Ai.Clean 手机版吸收远程仓库的「语音播报」能力

## Context

对比分析（william-oy/Android-Phone-Ad-Cleaner vs 本地 Ai.Clean）后，用户同意吸收远程仓库的优点。核实结论：

- **TTS 语音播报**：远程仓库在执行期间用 `TextToSpeech` 播报进度（开始/每段/完成/停止），是适老化的关键反馈层。本地手机版完全没有语音能力（全仓无 TextToSpeech 引用），只有提示音 chime（`gui/app.py` 的 `chime()`，手机版经 `native.chime(path)` 播 wav）。这是本次要移植的能力。
- **ADB 自动开无障碍**：经核实**对本项目不适用，不移植**。远程仓库依赖自己的 AccessibilityService 读屏，才需要 ADB 帮开无障碍；本地手机版刻意不用无障碍服务，读屏走 shell 身份 UiAutomation（`AndroidScreenReader.java`，且带 `FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES` 不踢用户已有服务，#623）。它的本地等价物——用已授予的 `WRITE_SECURE_SETTINGS` 自动重开无线调试——本地已实现（[AdbShell.kt](file:///workspace/android/app/src/main/java/com/ahuamiao/adcleaner/AdbShell.kt)）。
- 本地已有的一键流程（超级 Ai.Clean）+ 大按钮 UI 已覆盖远程的适老化交互主体，不重做界面。

## 方案

### 1. Python 端：可测的播报决策 + 设置项

**[android/app/src/main/python/aac_android/view.py](file:///workspace/android/app/src/main/python/aac_android/view.py)**（手机版专用层，桌面版不引用，符合「界面判断放 view.py 便于 CI 测」约定 #646）：

- 新增纯函数 `announcements(prev, cur)`：输入两次 `poll()` 返回的 RunProgress dict，返回要说的话（字符串列表）：
  - 开始执行：「开始清理，共 N 项」
  - 步骤推进（idx/title 变化）：「第 X 项，<title>」
  - 暂停 / 继续：「已暂停」「继续清理」
  - 卡住（stalled 0→>0）：「清理好像停住了，请回来看看」
  - 结束：按 outcome 给「清理完成」「已停止」「有步骤没做成，请回来看看结果」，并附各步 summary 汇总（复用 [view.py](file:///workspace/android/app/src/main/python/aac_android/view.py) 现有 `summarize()` 的输出）
  - 同一批次结束与步进同时发生时只说结局；prev 为空只说开始
- 注册进文件末尾的 `view.EXPORTS`（现为 poll/detect/brand_guide/setup_guide/send_feedback，约 L304）

**[gui/app.py](file:///workspace/gui/app.py)** Api 类（两版共用；桌面 web 不加开关，行为不变）：

- `def voice(self)` → `self.setting("voice", False)`（复用 L76 `setting()`）
- `def set_voice(self, on)` → `self.put_setting("voice", bool(on))`（复用 L84 `put_setting()`，settings.json 已有原子写）

### 2. Kotlin 端：TTS 播报器 + 开关

**新文件 `android/app/src/main/java/com/ahuamiao/adcleaner/Voice.kt`**：

- `TextToSpeech` 包装：初始化 `Locale.SIMPLIFIED_CHINESE`，用 `Handler(mainLooper).post` 模式与 [Native.kt](file:///workspace/android/app/src/main/java/com/ahuamiao/adcleaner/Native.kt) 现有方法一致
- `speak(text)`：进度类用 `QUEUE_FLUSH` 防积压；结束类 `QUEUE_ADD`
- TTS 引擎初始化失败（个别精简 ROM 没有 TTS）：静默降级不播报，不崩溃、不重试
- 不需要任何新 Android 权限

**[ui/AppModel.kt](file:///workspace/android/app/src/main/java/com/ahuamiao/adcleaner/ui/AppModel.kt)**：

- `var voiceOn by mutableStateOf(false)`；`start()` 里 `backend.call("voice")` 读回
- `fun setVoice(on)`：改状态 + `backend.call("set_voice", on)`
- `follow()` 的 poll 循环（现每 500ms/2000ms 一次，L390-444）：voiceOn 时调 `backend.call("view.announcements", prev, cur)`，把返回的句子逐条 `Voice.speak()`；整段 `runCatching` 包裹，播报任何失败不影响执行；voiceOn=false 时不发起调用（省 IPC）

**[ui/Sheets.kt](file:///workspace/android/app/src/main/java/com/ahuamiao/adcleaner/ui/Sheets.kt)** MoreSheet（L111）：

- 新增一行「语音播报」，`trailingContent = { Switch(...) }`（参照 [ChooseScreen.kt](file:///workspace/android/app/src/main/java/com/ahuamiao/adcleaner/ui/ChooseScreen.kt) L337 的用法），点击走 `model.setVoice(!model.voiceOn)`；`rows` 计数相应 +1

### 3. 测试

- `tests/` 新增 `announcements` 纯函数用例：构造 prev/cur dict，断言开始/步进/暂停/卡住/三种结局/同批合并各场景文案；导入方式参照 [tests/test_android_shim.py](file:///workspace/tests/test_android_shim.py) 现有做法
- 运行：`python -m unittest discover -s tests`（README 既定命令，设备无关）
- 不新增 Kotlin 单测（项目 JVM 单测只覆盖 ShellServer 协议层，本次无此类逻辑）

### 4. 文档

- [CHANGELOG.md](file:///workspace/CHANGELOG.md) 顶部按现有 1.0.x 格式加一节（版本号留待发版定），写明「手机版新增语音播报开关；执行进度可语音提醒」
- [android/README.md](file:///workspace/android/README.md) 界面一节补一句语音开关
- 不新增 ADR（功能实现，非架构决策）

## 明确不做

- 不改 `tool/` 引擎、桌面 web 界面、桌面端行为
- 不移植远程的「ADB 自动开无障碍」（架构上不适用，见 Context）
- 不动版本号 `gui/app.py VERSION`

## 验证

1. `python -m unittest discover -s tests` 全绿（含新用例）
2. Kotlin 编译需要 JDK 17 + Android SDK，本沙盒无此环境——构建验证走 `android/build.sh assembleRelease` 由用户/CI 完成；Kotlin 改动保持最小（一个新文件 + 三处小改），参照现有代码风格
3. 真机验收路径：开语音 → 首页点「超级 Ai.Clean」→ 确认执行页播报「开始清理…」「第 X 项…」→ 完成页播报结果；关语音后全程无声
