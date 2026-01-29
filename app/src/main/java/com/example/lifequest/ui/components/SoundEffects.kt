package com.example.lifequest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.example.lifequest.logic.LocalSoundManager

// --- 1. 音付きクリック Modifier (Card, Row, Box用) ---
fun Modifier.soundClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val soundManager = LocalSoundManager.current
    val interactionSource = remember { MutableInteractionSource() }

    this.clickable(
        interactionSource = interactionSource,
        indication = ripple(),
        enabled = enabled
    ) {
        if (enabled) {
            soundManager.playClick()
        }
        onClick()
    }
}

// --- 2. 音付きボタン (Button用) ---
@Composable
fun SoundButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val soundManager = LocalSoundManager.current
    Button(
        onClick = {
            soundManager.playClick()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content
    )
}

// --- 3. 音付きアイコンボタン (IconButton用) ---
@Composable
fun SoundIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit
) {
    val soundManager = LocalSoundManager.current
    IconButton(
        onClick = {
            soundManager.playClick()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content
    )
}

// --- 4. 音付きテキストボタン (TextButton用) ---
@Composable
fun SoundTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val soundManager = LocalSoundManager.current
    TextButton(
        onClick = {
            soundManager.playClick()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content
    )
}