@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.ahuamiao.adcleaner.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.ahuamiao.adcleaner.R

@Composable
fun BootScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ContainedLoadingIndicator(Modifier.size(72.dp))
    }
}

@Composable
fun FailedScreen(reason: String) {
    Page {
        // 报错可能很长：限在剩余高度里滚动，横屏也不会顶出屏幕
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("启动失败", style = MaterialTheme.typography.headlineMediumEmphasized)
                Spacer(Modifier.height(12.dp))
                SelectionContainer {
                    Text(
                        reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 首页主视觉：品牌渐变圆角徽标，替代原来的彩色多瓣形状。 */
@Composable
private fun BrandMark(size: Dp, onTap: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(percent = 34)
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "brand-mark-press",
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.primary, colors.secondary)))
            .semantics { contentDescription = "关于 Ai.Clean" }
            .clickable(interactionSource = press, indication = null, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        val ink = colors.onPrimary
        Box(Modifier.fillMaxSize().border(1.dp, ink.copy(alpha = 0.22f), shape))
        Box(Modifier.fillMaxSize().padding(size * 0.12f).border(1.dp, ink.copy(alpha = 0.12f), shape))
        CompositionLocalProvider(LocalContentColor provides ink) {
            Glyph(R.drawable.ic_ad_off, Modifier.size(size * 0.34f))
        }
    }
}

@Composable
fun HomeScreen(model: AppModel) {
    val haptic = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val device = model.device
    val actionHeight = ButtonDefaults.MediumContainerHeight

    HeroPage(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painterResource(R.drawable.ic_aiclean_avatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text("Ai.Clean", style = MaterialTheme.typography.titleMediumEmphasized)
                }
                Box {
                    IconButton(onClick = { model.sheet = Sheet.More }, shapes = IconButtonDefaults.shapes()) {
                        Glyph(R.drawable.ic_more_horiz, label = "更多")
                    }
                    if (model.newVersion != null) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.error),
                        )
                    }
                }
            }
        },
        middle = { maxWidth, maxHeight ->
            // 扣掉下面标题和状态胶囊，别让徽标在矮屏上把文字顶出屏幕
            val size = min(maxWidth * 0.50f, (maxHeight - 150.dp).coerceAtLeast(88.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BrandMark(
                    size = size,
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        model.sheet = Sheet.About
                    },
                )
                Spacer(Modifier.height(24.dp))
                val title = device?.title.orEmpty()
                if (title.isNotEmpty()) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                StatusPill("已就绪", color = colors.secondary, contentColor = colors.onSecondaryContainer)
            }
        },
        actions = {
            SizedButton(
                text = "超级 Ai.Clean",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    model.confirmSuperClean()
                },
                modifier = Modifier.fillMaxWidth(),
                height = actionHeight,
                icon = R.drawable.ic_bolt,
            )
            Spacer(Modifier.height(12.dp))
            SizedButton(
                text = "手动选择去广告",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    model.oneTap()
                },
                modifier = Modifier.fillMaxWidth(),
                height = actionHeight,
                icon = R.drawable.ic_shield,
                tonal = true,
            )
            Spacer(Modifier.height(12.dp))
            SizedButton(
                text = "还原改动",
                onClick = model::openRestore,
                modifier = Modifier.fillMaxWidth(),
                height = actionHeight,
                icon = R.drawable.ic_history,
                outlined = true,
            )
            Spacer(Modifier.height(16.dp))
        },
    )
}

// ---------- 首次设置 ---------- //

@Composable
fun SetupScreen(model: AppModel, state: String, host: HostActions) {
    val at: Int? = model.setup.at[state]
    val steps = model.setup.steps
    when {
        // 三步清单还没从 view.py 取到：还不知道这一步该说什么（只有演示路径会碰上）
        at == null -> if (steps.isEmpty()) BootScreen() else StatusScreen(state, host)
        at > steps.lastIndex -> BootScreen()
        else -> SetupSteps(model, at, host)
    }
}

/** 三步的按钮落到哪个系统动作；文案不在这里（view.py 的 setup_guide，#646）。 */
private fun runStep(step: SetupStep, host: HostActions) = when (step.key) {
    "about" -> host.openSettings("about")
    "dev" -> host.openSettings("dev")
    "pair" -> host.startPairing()
    else -> Unit
}

@Composable
private fun SetupSteps(model: AppModel, at: Int, host: HostActions) {
    val steps = model.setup.steps
    val list = rememberLazyListState()
    val reduce = LocalReduceMotion.current
    // 从系统设置回到 App 时定位到当前步（#646）；换一步时也滚过去。高亮由 SetupRow 按 i == at 画。
    LaunchedEffect(at, model.visible) {
        if (model.visible) {
            if (reduce) list.scrollToItem(at) else list.animateScrollToItem(at)
        }
    }
    val bar = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colors = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.nestedScroll(bar.nestedScrollConnection),
        containerColor = colors.surfaceContainer,
        topBar = {
            CenteredBar {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            "先做 3 个设置",
                            style = MaterialTheme.typography.displaySmallEmphasized,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    subtitle = { Text("只做一次，约 1 分钟", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = pageBarColors(),
                    scrollBehavior = bar,
                )
            }
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .widthIn(max = PageWidth)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SizedButton(
                        text = steps[at].action,
                        onClick = { runStep(steps[at], host) },
                        modifier = Modifier.fillMaxWidth(),
                        height = ctaHeight(),
                    )
                    TextButton(onClick = host::requestShizuku, shapes = ButtonDefaults.shapes()) {
                        Text("改用 Shizuku")
                    }
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = list,
                modifier = Modifier
                    .widthIn(max = PageWidth)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                itemsIndexed(steps) { i, step -> SetupRow(i, steps.size, step, i < at, i == at) }
            }
        }
    }
}

@Composable
private fun SetupRow(index: Int, count: Int, step: SetupStep, done: Boolean, current: Boolean) {
    val colors = MaterialTheme.colorScheme
    val reduce = LocalReduceMotion.current
    val badgeColor = when {
        done -> colors.secondary
        current -> colors.primary
        else -> colors.surfaceContainerHigh
    }
    val badgeContent = when {
        done -> colors.onSecondary
        current -> colors.onPrimary
        else -> colors.onSurfaceVariant
    }
    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(index, count),
        modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
        colors = if (current) {
            ListItemDefaults.segmentedColors(
                containerColor = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
                supportingContentColor = colors.onSecondaryContainer,
                leadingContentColor = colors.onSecondaryContainer,
            )
        } else {
            cardRowColors()
        },
        verticalAlignment = Alignment.Top,
        leadingContent = {
            ShapeBadge(MaterialShapes.Circle, badgeColor, Modifier.size(40.dp)) {
                CompositionLocalProvider(LocalContentColor provides badgeContent) {
                    // 完成瞬间：数字→对勾缩放+淡入，不硬切；步进时数字换号保持瞬时
                    val motion = MaterialTheme.motionScheme
                    AnimatedContent(
                        done,
                        transitionSpec = {
                            if (reduce) {
                                fadeIn(motion.fastEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())
                            } else {
                                (scaleIn(motion.fastEffectsSpec(), initialScale = 0.6f) + fadeIn(motion.fastEffectsSpec())) togetherWith
                                    fadeOut(motion.fastEffectsSpec())
                            }
                        },
                        label = "setup-badge",
                    ) { checked ->
                        if (checked) Glyph(R.drawable.ic_check, Modifier.size(20.dp), label = "已完成")
                        else Text("${index + 1}", style = MaterialTheme.typography.titleMediumEmphasized)
                    }
                }
            }
        },
        supportingContent = if (current) {
            {
                Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    step.lines.forEachIndexed { k, line ->
                        Row {
                            Text(
                                "${k + 1}",
                                style = MaterialTheme.typography.labelLargeEmphasized,
                                // 最小宽度而不是固定宽度：系统字号调大时序号还能完整显示
                                modifier = Modifier.widthIn(min = 18.dp),
                            )
                            Text(line, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (step.note.isNotEmpty()) {
                        Text(step.note, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else null,
    ) {
        Text(
            step.title,
            style = if (current) MaterialTheme.typography.titleMediumEmphasized else MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = if (current) 0.dp else 8.dp),
        )
    }
}

// ---------- Shizuku 与读不到手机：一句话 + 一个按钮 ---------- //

private data class StatusCopy(val title: String, val text: String, val action: String, val run: (HostActions) -> Unit)

private fun statusCopy(state: String) = when (state) {
    "old" -> StatusCopy("安卓版本太低", "安卓 11 以下没有无线调试，要装 Shizuku，按它的说明连一次电脑启动。", "打开 Shizuku") { it.openShizuku() }
    "stopped" -> StatusCopy("Shizuku 还没启动", "打开 Shizuku 按说明启动。手机重启后要再启动一次。", "打开 Shizuku") { it.openShizuku() }
    "ask" -> StatusCopy("允许本 App 使用 Shizuku", "点下面的按钮，在弹窗里选「始终允许」。", "授权") { it.requestShizuku() }
    "denied" -> StatusCopy("之前拒绝了 Shizuku 授权", "打开 Shizuku →「已授权的应用」，打开「Ai.Clean」，再回到这里。", "打开 Shizuku") { it.openShizuku() }
    else -> StatusCopy("读不到这台手机", "到「开发者选项」把「无线调试」关掉再打开，然后回到这里。", "打开开发者选项") { it.openSettings("dev") }
}

@Composable
private fun StatusScreen(state: String, host: HostActions) {
    val copy = statusCopy(state)
    val colors = MaterialTheme.colorScheme
    Page {
        // 内容放得下时居中，横屏或大字号放不下时整块可滚动，不会顶到下面的按钮
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 横屏和矮屏收小主视觉，给说明文字和按钮留位置
                val short = LocalConfiguration.current.screenHeightDp < 600
                Hero(MaterialShapes.Circle, colors.primaryContainer, Modifier.size(if (short) 112.dp else 160.dp)) {
                    Glyph(R.drawable.ic_shield, Modifier.size(if (short) 48.dp else 64.dp))
                }
                Spacer(Modifier.height(32.dp))
                Text(copy.title, style = MaterialTheme.typography.headlineMediumEmphasized, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    copy.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        SizedButton(
            text = copy.action,
            onClick = { copy.run(host) },
            modifier = Modifier.fillMaxWidth(),
            height = ctaHeight(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** MainActivity 提供的系统动作。 */
interface HostActions {
    fun startPairing()
    fun openSettings(which: String)
    fun requestShizuku()
    fun openShizuku()
}
