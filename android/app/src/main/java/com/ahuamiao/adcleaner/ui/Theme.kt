package com.ahuamiao.adcleaner.ui

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.lightColorScheme

/** 用户在系统里关了动画：形状不再持续旋转，其余动效由 Compose 按系统比例缩短。 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** 数字等宽：进度、用时跳动时不晃。 */
const val TABULAR = "tnum"

/**
 * 强调字重。官方令牌是 500，可很多中文字体只有常规和粗体两档，500 落回常规，强调就没了。
 * 标题和大字用粗体，小一号的用 600（只有两档的字体也会落到粗体）。
 *
 * 中文字距：M3 的字距是按拉丁字母调的，汉字方块字自带间距，再叠 0.1~0.5sp 就发飘。
 * 界面全是要给中文看的，把用到的字阶字距清零；正文说明行高放宽到 18sp，长句不挤。
 */
private fun TextStyle.cn() = copy(letterSpacing = 0.sp)

private val AacTypography = Typography().run {
    copy(
        displayLargeEmphasized = displayLargeEmphasized.cn().copy(fontWeight = FontWeight.Bold),
        displayMediumEmphasized = displayMediumEmphasized.cn().copy(fontWeight = FontWeight.Bold),
        displaySmallEmphasized = displaySmallEmphasized.cn().copy(fontWeight = FontWeight.Bold),
        headlineLargeEmphasized = headlineLargeEmphasized.cn().copy(fontWeight = FontWeight.Bold),
        headlineMediumEmphasized = headlineMediumEmphasized.cn().copy(fontWeight = FontWeight.Bold),
        headlineSmallEmphasized = headlineSmallEmphasized.cn().copy(fontWeight = FontWeight.Bold),
        titleLargeEmphasized = titleLargeEmphasized.cn().copy(fontWeight = FontWeight.SemiBold),
        titleMediumEmphasized = titleMediumEmphasized.cn().copy(fontWeight = FontWeight.SemiBold),
        titleSmallEmphasized = titleSmallEmphasized.cn().copy(fontWeight = FontWeight.SemiBold),
        labelLargeEmphasized = labelLargeEmphasized.cn().copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.cn(),
        titleMedium = titleMedium.cn(),
        titleSmall = titleSmall.cn(),
        bodyLarge = bodyLarge.cn(),
        bodyMedium = bodyMedium.cn(),
        bodySmall = bodySmall.copy(letterSpacing = 0.sp, lineHeight = 18.sp),
        labelLarge = labelLarge.cn(),
        labelMedium = labelMedium.cn(),
        labelSmall = labelSmall.cn(),
    )
}

/** 品牌形状：圆角比默认更克制，列表和面板看起来像同一套产品，而不是拼起来的。 */
private val AacShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AacTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = if (dark) darkColorScheme(
        primary = Color(0xFFAFC6FF), onPrimary = Color(0xFF002C77),
        primaryContainer = Color(0xFF1A44A8), onPrimaryContainer = Color(0xFFE6EDFF),
        // 品牌第二色：Ai.Clean 的薄荷绿，只用在「已就绪 / 成功」和次级操作上
        secondary = Color(0xFF63E0C6), onSecondary = Color(0xFF00382F),
        secondaryContainer = Color(0xFF0F4F46), onSecondaryContainer = Color(0xFFC9F6EC),
        tertiary = Color(0xFFFFC85C), onTertiary = Color(0xFF3D2A00),
        tertiaryContainer = Color(0xFF5A4100), onTertiaryContainer = Color(0xFFFFE7B3),
        // 卡片比页面亮一档：深色下也看得出层次，不容易和底色糊在一起
        background = Color(0xFF0B0E14), surface = Color(0xFF191F2B),
        // 中性梯度全部带蓝味，与品牌底色同调
        onBackground = Color(0xFFE7EAF2), onSurface = Color(0xFFE7EAF2),
        surfaceVariant = Color(0xFF3D4454), onSurfaceVariant = Color(0xFFC1C7D6),
        outline = Color(0xFF8B93A7), outlineVariant = Color(0xFF3A4152),
        surfaceDim = Color(0xFF0B0E14),
        surfaceContainerLowest = Color(0xFF080A0F), surfaceContainerLow = Color(0xFF11151D),
        surfaceContainer = Color(0xFF10141C), surfaceContainerHigh = Color(0xFF1E2431),
        surfaceContainerHighest = Color(0xFF272E3D),
    ) else lightColorScheme(
        primary = Color(0xFF1F5EFF), onPrimary = Color.White,
        primaryContainer = Color(0xFFE3ECFF), onPrimaryContainer = Color(0xFF0A2A6B),
        secondary = Color(0xFF0E9F8A), onSecondary = Color.White,
        secondaryContainer = Color(0xFFD6F6EF), onSecondaryContainer = Color(0xFF003D34),
        tertiary = Color(0xFFF0A000), onTertiary = Color(0xFF3D2A00),
        tertiaryContainer = Color(0xFFFFF0D1), onTertiaryContainer = Color(0xFF3D2A00),
        background = Color(0xFFF6F8FC), surface = Color.White,
        onBackground = Color(0xFF11141B), onSurface = Color(0xFF11141B),
        surfaceVariant = Color(0xFFE7EAF2), onSurfaceVariant = Color(0xFF4B5263),
        outline = Color(0xFF767E91), outlineVariant = Color(0xFFD5DAE5),
        surfaceDim = Color(0xFFDCE1EB),
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF2F5FA),
        surfaceContainer = Color(0xFFF5F7FB), surfaceContainerHigh = Color(0xFFEBEFF7),
        surfaceContainerHighest = Color(0xFFE2E7F1),
    )
    val reduce = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    CompositionLocalProvider(LocalReduceMotion provides reduce) {
        MaterialExpressiveTheme(
            colorScheme = colors,
            motionScheme = MotionScheme.expressive(),
            shapes = AacShapes,
            typography = AacTypography,
            content = content,
        )
    }
}
