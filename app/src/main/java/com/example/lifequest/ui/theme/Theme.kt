package com.example.lifequest.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Tech Noir Spec: Dark Mode Only
private val TechNoirColorScheme = darkColorScheme(
    primary = TechNoirPrimary,
    onPrimary = TechNoirOnPrimary,
    primaryContainer = TechNoirPrimaryContainer,
    onPrimaryContainer = TechNoirOnPrimaryContainer,
    secondary = TechNoirSecondary,
    onSecondary = TechNoirOnSecondary,
    secondaryContainer = TechNoirSecondaryContainer,
    onSecondaryContainer = TechNoirOnSecondaryContainer,
    tertiary = TechNoirTertiary,
    onTertiary = TechNoirOnTertiary,
    tertiaryContainer = TechNoirTertiaryContainer,
    onTertiaryContainer = TechNoirOnTertiaryContainer,
    background = TechNoirBackground,
    onBackground = TechNoirOnSurface,
    surface = TechNoirSurface,
    onSurface = TechNoirOnSurface,
    surfaceVariant = TechNoirSecondary, // カード等のVariant
    onSurfaceVariant = TechNoirOnSecondary,
    outline = TechNoirOutline,
    outlineVariant = TechNoirOutlineVariant,
    error = TechNoirError,
    onError = TechNoirOnError,
    errorContainer = TechNoirErrorContainer,
    onErrorContainer = TechNoirOnErrorContainer
)

// デザイン仕様: 角丸は 8dp〜12dp と小さめに設定 (ハードウェア感)
val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp), // カード等の基本形状
    large = RoundedCornerShape(12.dp)  // ダイアログ等
)

@Composable
fun LifeQuestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Tech Noirは常にダークテーマのような外観を持つため、システム設定に関わらずDark配色をベースにする
    // ただしDynamic Colorはアクセントとして取り入れる
    dynamicColor: Boolean = false, // デザインフィロソフィー優先のためOFF推奨
    content: @Composable () -> Unit
) {
    val colorScheme = TechNoirColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb() // ステータスバーも背景色に合わせる
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false // 常に白文字
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
//package com.example.lifequest.ui.theme
//
//import android.os.Build
//import androidx.compose.foundation.isSystemInDarkTheme
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.darkColorScheme
//import androidx.compose.material3.dynamicDarkColorScheme
//import androidx.compose.material3.dynamicLightColorScheme
//import androidx.compose.material3.lightColorScheme
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.platform.LocalContext
//
//private val DarkColorScheme = darkColorScheme(
//    primary = Purple80,
//    secondary = PurpleGrey80,
//    tertiary = Pink80
//)
//
//private val LightColorScheme = lightColorScheme(
//    primary = Purple40,
//    secondary = PurpleGrey40,
//    tertiary = Pink40
//
//    /* Other default colors to override
//    background = Color(0xFFFFFBFE),
//    surface = Color(0xFFFFFBFE),
//    onPrimary = Color.White,
//    onSecondary = Color.White,
//    onTertiary = Color.White,
//    onBackground = Color(0xFF1C1B1F),
//    onSurface = Color(0xFF1C1B1F),
//    */
//)
//
//@Composable
//fun LifeQuestTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    // Dynamic color is available on Android 12+
//    dynamicColor: Boolean = true,
//    content: @Composable () -> Unit
//) {
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }
//
//    MaterialTheme(
//        colorScheme = colorScheme,
//        typography = Typography,
//        content = content
//    )
//}
