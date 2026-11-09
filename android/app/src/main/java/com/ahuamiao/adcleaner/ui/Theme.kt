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
        // 深空黑 + 青墨绿：底色是带青绿底味的深空黑，主色是深空黑上可读的青墨绿亮档
        primary = Color(0xFF58CDA6), onPrimary = Color(0xFF003826),
        primaryContainer = Color(0xFF0D5B44), onPrimaryContainer = Color(0xFFC3F2E0),
        // 品牌第二色：薄荷青，只用在「已就绪 / 成功」和次级操作上，与主色同属青绿家族
        secondary = Color(0xFF63E0C6), onSecondary = Color(0xFF00382B),
        secondaryContainer = Color(0xFF0E4F41), onSecondaryContainer = Color(0xFFC7F3E6),
        tertiary = Color(0xFFFFC85C), onTertiary = Color(0xFF3D2A00),
        tertiaryContainer = Color(0xFF5A4100), onTertiaryContainer = Color(0xFFFFE7B3),
        // 卡片比页面亮一档：深色下也看得出层次，不容易和底色糊在一起
        background = Color(0xFF060908), surface = Color(0xFF131916),
        // 中性梯度全部带青绿味，与品牌底色同调
        onBackground = Color(0xFFE2E8E3), onSurface = Color(0xFFE2E8E3),
        surfaceVariant = Color(0xFF3A463F), onSurfaceVariant = Color(0xFFBFCBC3),
        outline = Color(0xFF89978E), outlineVariant = Color(0xFF3B463F),
        surfaceDim = Color(0xFF060908),
        surfaceContainerLowest = Color(0xFF040605), surfaceContainerLow = Color(0xFF0F1411),
        surfaceContainer = Color(0xFF0D120F), surfaceContainerHigh = Color(0xFF1C231E),
        surfaceContainerHighest = Color(0xFF262E28),
    ) else lightColorScheme(
        primary = Color(0xFF0E7C5F), onPrimary = Color.White,
        primaryContainer = Color(0xFFD3F1E4), onPrimaryContainer = Color(0xFF00382A),
        secondary = Color(0xFF12A184), onSecondary = Color.White,
        secondaryContainer = Color(0xFFCFF0E3), onSecondaryContainer = Color(0xFF00402F),
        tertiary = Color(0xFFF0A000), onTertiary = Color(0xFF3D2A00),
        tertiaryContainer = Color(0xFFFFF0D1), onTertiaryContainer = Color(0xFF3D2A00),
        background = Color(0xFFF4F8F5), surface = Color.White,
        onBackground = Color(0xFF101713), onSurface = Color(0xFF101713),
        surfaceVariant = Color(0xFFE2EAE5), onSurfaceVariant = Color(0xFF44544B),
        outline = Color(0xFF6E7F75), outlineVariant = Color(0xFFD2DED6),
        surfaceDim = Color(0xFFDBE3DD),
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF1F5F1),
        surfaceContainer = Color(0xFFF4F7F4), surfaceContainerHigh = Color(0xFFE9EFEA),
        surfaceContainerHighest = Color(0xFFDFE8E1),
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
