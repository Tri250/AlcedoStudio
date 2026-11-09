@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.ahuamiao.adcleaner.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ahuamiao.adcleaner.R
import org.json.JSONObject

// 「使用说明」指向用户指定的项目仓库教程文档链接
private const val GUIDE_URL =
    "https://github.com/Tri250/AlcedoStudio/blob/main/docs/%E6%89%8B%E6%9C%BA%E7%89%88%E6%95%99%E7%A8%8B/brand-taps.json"

@Composable
fun SheetHost(model: AppModel) {
    val sheet = model.sheet ?: return
    key(sheet::class) {
        val state = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
        ModalBottomSheet(onDismissRequest = { model.sheet = null }, sheetState = state) {
            when (sheet) {
                is Sheet.More -> MoreSheet(model)
                is Sheet.About -> AboutSheet(model)
                is Sheet.Support -> SupportSheet()
                is Sheet.Restore -> RestoreSheet(model)
                is Sheet.Feedback -> FeedbackSheet(model, sheet)
                is Sheet.Report -> ReportSheet(model, sheet.payload)
                is Sheet.Log -> LogSheet(model)
            }
        }
    }
}

@Composable
private fun SheetBody(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        content = content,
    )
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmallEmphasized,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

// ---------- 更多 ---------- //

@Composable
private fun MoreSheet(model: AppModel) {
    val newer = model.newVersion
    val rows = if (newer != null) 6 else 5
    val colors = MaterialTheme.colorScheme
    var i = 0
    SheetBody {
        Segments {
            if (newer != null) {
                SegmentedListItem(
                    onClick = { model.openUrl(newer.url) },
                    shapes = ListItemDefaults.segmentedShapes(i++, rows),
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                    ),
                    leadingContent = {
                        IconTile(
                            R.drawable.ic_system_update,
                            container = colors.surface.copy(alpha = 0.72f),
                            content = colors.primary,
                        )
                    },
                ) { Text("有新版本 ${newer.version}") }
            }
            SegmentedListItem(
                onClick = model::openRestore,
                shapes = ListItemDefaults.segmentedShapes(i++, rows),
                colors = cardRowColors(),
                leadingContent = { IconTile(R.drawable.ic_history, container = colors.surfaceContainerHigh, content = colors.onSurfaceVariant) },
            ) { Text("还原改动") }
            SegmentedListItem(
                onClick = { model.openFeedback() },
                shapes = ListItemDefaults.segmentedShapes(i++, rows),
                colors = cardRowColors(),
                leadingContent = { IconTile(R.drawable.ic_chat_bubble, container = colors.surfaceContainerHigh, content = colors.onSurfaceVariant) },
            ) { Text("反馈问题") }
            SegmentedListItem(
                onClick = { model.sheet = Sheet.Support },
                shapes = ListItemDefaults.segmentedShapes(i++, rows),
                colors = cardRowColors(),
                leadingContent = { IconTile(R.drawable.ic_headset, container = colors.surfaceContainerHigh, content = colors.onSurfaceVariant) },
            ) { Text("开发者支持") }
            SegmentedListItem(
                onClick = { model.openUrl(GUIDE_URL) },
                shapes = ListItemDefaults.segmentedShapes(i++, rows),
                colors = cardRowColors(),
                leadingContent = { IconTile(R.drawable.ic_menu_book, container = colors.surfaceContainerHigh, content = colors.onSurfaceVariant) },
                trailingContent = { Glyph(R.drawable.ic_open_in_new) },
            ) { Text("使用说明") }
            SegmentedListItem(
                onClick = { model.sheet = Sheet.About },
                shapes = ListItemDefaults.segmentedShapes(i, rows),
                colors = cardRowColors(),
                leadingContent = { IconTile(R.drawable.ic_shield, container = colors.surfaceContainerHigh, content = colors.onSurfaceVariant) },
            ) { Text("关于清静") }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "版本 ${model.version}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

// ---------- 关于 ---------- //

@Composable
private fun AboutSheet(model: AppModel) {
    SheetBody(Modifier.verticalScroll(rememberScrollState())) {
        SheetTitle("清静 · AiGoodBro")
        Text("版本 ${model.version} · 安卓版", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "帮你把安卓手机里的广告、推荐和营销推送关掉，让手机安静一点。改动前逐项确认，正常功能不受影响，随时可一键还原。",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Text("它能做什么", style = MaterialTheme.typography.titleMediumEmphasized)
        Text(
            "· 系统推荐与广告开关：按机型适配，逐项确认后关闭",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "· 营销通知：精准关闭推送渠道，快递、更新等正常通知保留",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "· 快应用与广告组件：挂起不卸载，随时恢复",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { model.sheet = Sheet.Support }, shapes = ButtonDefaults.shapes()) {
            Text("开发者支持")
        }
    }
}

/** 开发者抖音主页直达链接与分享口令（口令必须整段复制才有效）。 */
private const val DouyinUrl = "https://v.douyin.com/MMTUX7QHyO4/"
private const val DouyinCode =
    "8- 长按复制此条消息，打开抖音搜索，查看TA的更多作品。 https://v.douyin.com/MMTUX7QHyO4/ 3@8.com :7pm"

@Composable
private fun SupportSheet() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val uri = LocalUriHandler.current
    SheetBody(Modifier.verticalScroll(rememberScrollState())) {
        SheetTitle("开发者支持")
        Text("开发者：某厂大牛马 · 带娃的小陈工", style = MaterialTheme.typography.bodyMedium)
        Text(
            "抖音号：72690415302",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            SelectionContainer {
                Text(
                    DouyinCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                onClick = {
                    clipboard.setText(AnnotatedString(DouyinCode))
                    Toast.makeText(context, "口令已复制，打开抖音即可查看主页", Toast.LENGTH_SHORT).show()
                },
                shapes = ButtonDefaults.shapes(),
            ) { Text("复制口令") }
            TextButton(onClick = { uri.openUri(DouyinUrl) }, shapes = ButtonDefaults.shapes()) {
                Text("打开抖音主页")
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "复制整段口令后打开抖音，会自动弹出主页卡片；手机上也可以直接点「打开抖音主页」。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------- 还原 ---------- //

private data class RestoreItem(
    val key: String,
    val label: String,
    val icon: Int,
    val note: String,
)

private val RestoreItems = listOf(
    RestoreItem("quickapp", "恢复快应用", R.drawable.ic_bolt, "重新启用被关闭的快应用组件"),
    RestoreItem("suspend", "恢复挂起的广告组件", R.drawable.ic_extension, "重新启用被挂起的系统广告服务"),
    RestoreItem("channels", "重新打开营销通知", R.drawable.ic_notifications_off, "恢复被关闭的营销通知渠道"),
)

@Composable
private fun RestoreSheet(model: AppModel) {
    val colors = MaterialTheme.colorScheme
    SheetBody {
        SheetTitle("还原改动")
        Text(
            "只恢复清静做过的改动，不影响系统更新和正常功能。",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Segments {
            RestoreItems.forEachIndexed { i, item ->
                val tone = when (i) {
                    0 -> colors.tertiaryContainer to colors.onTertiaryContainer
                    1 -> colors.primaryContainer to colors.onPrimaryContainer
                    else -> colors.secondaryContainer to colors.onSecondaryContainer
                }
                SegmentedListItem(
                    onClick = { model.restore(item.key) },
                    enabled = !model.restoring,
                    shapes = ListItemDefaults.segmentedShapes(i, RestoreItems.size),
                    colors = cardRowColors(),
                    leadingContent = {
                        IconTile(item.icon, container = tone.first, content = tone.second)
                    },
                    supportingContent = { Text(item.note) },
                ) { Text(item.label) }
            }
        }
        Text(
            "App 里的开关要按手动清单打开。",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        AnimatedVisibility(model.restoring) {
            LinearWavyProgressIndicator(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
        }
        if (model.restoreResult.isNotEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = colors.surfaceContainerHigh,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("还原结果", style = MaterialTheme.typography.titleSmallEmphasized)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        model.restoreResult,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
    }
}

// ---------- 反馈 ---------- //

private val FeedbackKinds = listOf("bug" to "遇到问题", "idea" to "功能建议", "device" to "适配新机型")

@Composable
private fun FeedbackSheet(model: AppModel, sheet: Sheet.Feedback) {
    var kind by remember { mutableStateOf(sheet.kind) }
    var text by remember { mutableStateOf("") }
    var attach by remember { mutableStateOf(true) }
    var showDiag by remember { mutableStateOf(false) }
    var diag by remember { mutableStateOf(sheet.diag) }
    var missing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    SheetBody(
        Modifier
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        SheetTitle("反馈问题")
        TextButton(onClick = { model.sheet = Sheet.Support }, shapes = ButtonDefaults.shapes()) { Text("开发者支持") }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            FeedbackKinds.forEachIndexed { i, (key, label) ->
                ToggleButton(
                    checked = kind == key,
                    onCheckedChange = { kind = key },
                    // 和页面主按钮同高，表单里的控件不再忽高忽低
                    modifier = Modifier
                        .weight(1f)
                        .height(ButtonDefaults.MediumContainerHeight),
                    shapes = when (i) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        FeedbackKinds.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                missing = false
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            isError = missing,
            // 输入框和卡片用同一档圆角与正文规格，不再各写各的
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = { Text("发生了什么？你希望怎样？", style = MaterialTheme.typography.bodyMedium) },
            supportingText = if (missing) {
                { Text("写几句再提交") }
            } else null,
        )
        Spacer(Modifier.height(12.dp))
        Segments {
            SegmentedListItem(
                checked = attach,
                onCheckedChange = { attach = it },
                shapes = ListItemDefaults.segmentedShapes(0, 2),
                colors = toggleRowColors(),
                trailingContent = { Switch(attach, onCheckedChange = null) },
                supportingContent = { Text("版本、手机型号、执行结果和最近的日志。序列号已替换，不含应用列表和通知内容。") },
            ) { Text("附带诊断信息") }
            SegmentedListItem(
                onClick = { showDiag = !showDiag },
                enabled = attach,
                shapes = ListItemDefaults.segmentedShapes(1, 2),
                colors = cardRowColors(),
            ) { Text(if (showDiag) "收起" else "查看 / 修改诊断信息") }
        }
        AnimatedVisibility(showDiag && attach) {
            OutlinedTextField(
                value = diag,
                onValueChange = { diag = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .heightIn(max = 240.dp),
                shape = MaterialTheme.shapes.medium,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        }
        if (result.isNotEmpty()) {
            Text(
                result,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        SizedButton(
            text = "提交到 GitHub",
            onClick = {
                val trimmedText = text.trim()
                if (trimmedText.isEmpty()) {
                    missing = true
                } else {
                    model.sendFeedback(kind, trimmedText, if (attach) diag else "") { trimmed ->
                        result = if (trimmed) "网址里带了诊断摘要，完整诊断已复制，可以粘贴到打开的页面末尾。"
                        else "已打开 GitHub。登录后检查内容，点「Submit new issue」提交。"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            icon = R.drawable.ic_open_in_new,
        )
        TextButton(
            onClick = model::openReport,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text("查看 / 复制错误日志") }
    }
}

// ---------- 错误日志：仅在本机预览和复制 ---------- //

@Composable
private fun ReportSheet(model: AppModel, payload: JSONObject) {
    var result by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    SheetBody {
        SheetTitle("错误日志")
        Text(
            "下面是本机生成的诊断内容，序列号等敏感信息已替换。不会上传到服务器；你可以先检查，再复制并自行分享。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            SelectionContainer {
                Text(
                    remember(payload) { payload.toString(2) },
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                )
            }
        }
        if (result.isNotEmpty()) {
            Text(
                result,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        SizedButton(
            text = when {
                sent -> "已复制"
                sending -> "复制中"
                result.isNotEmpty() -> "重试"
                else -> "复制日志"
            },
            onClick = {
                sending = true
                model.copyReport(payload) { message, ok ->
                    result = message
                    sent = ok
                    sending = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !sent && !sending,
        )
    }
}

// ---------- 详细记录 ---------- //

@Composable
private fun LogSheet(model: AppModel) {
    val list = rememberLazyListState()
    LaunchedEffect(model.log.size) {
        if (model.log.isNotEmpty()) list.scrollToItem(model.log.lastIndex)
    }
    SheetBody(Modifier.fillMaxHeight(0.8f)) {
        SheetTitle("详细记录")
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            SelectionContainer {
                LazyColumn(
                    state = list,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    items(model.log) { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        )
                    }
                }
            }
        }
    }
}

// ---------- 对话框 ---------- //

@Composable
fun NoticeHost(model: AppModel) {
    val notice = model.notice ?: return
    AlertDialog(
        onDismissRequest = { model.notice = null },
        title = { Text(notice.title) },
        text = if (notice.text.isNotEmpty() || notice.detail.isNotEmpty()) {
            {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (notice.text.isNotEmpty()) Text(notice.text)
                    if (notice.detail.isNotEmpty()) {
                        Text(
                            notice.detail,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        )
                    }
                }
            }
        } else null,
        confirmButton = {
            TextButton(
                onClick = {
                    model.notice = null
                    notice.onConfirm?.invoke()
                },
                shapes = ButtonDefaults.shapes(),
            ) { Text(notice.confirm) }
        },
        dismissButton = notice.dismiss?.let { label ->
            {
                TextButton(onClick = { model.notice = null }, shapes = ButtonDefaults.shapes()) { Text(label) }
            }
        },
    )
}
