package br.com.studyou.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val keyboardRows = listOf(
    "QWERTYUIOP",
    "ASDFGHJKL",
    "ZXCVBNM"
)

/**
 * A QWERTY on-screen keyboard in the style of Termo: the last row carries ENTER on the
 * left and backspace on the right, flanking the remaining letters.
 */
@Composable
fun OnScreenKeyboard(
    letterColors: Map<Char, Color> = emptyMap(),
    onLetterClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    onEnterClick: (() -> Unit)? = null,
    enterEnabled: Boolean = true
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        keyboardRows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val isLastRow = rowIndex == keyboardRows.lastIndex
                if (isLastRow && onEnterClick != null) {
                    // Highlighted like a primary action once the word is complete, and wide
                    // enough that "ENTER" never wraps onto two lines.
                    KeyboardKey(
                        modifier = Modifier.weight(2f),
                        containerColor = if (enterEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        contentColor = if (enterEnabled) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        enabled = enterEnabled,
                        onClick = onEnterClick
                    ) {
                        Text(
                            "ENTER",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                row.forEach { letter ->
                    KeyboardKey(
                        modifier = Modifier.weight(1f),
                        containerColor = letterColors[letter] ?: MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (letter in letterColors) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { onLetterClick(letter) }
                    ) {
                        Text(letter.toString(), fontWeight = FontWeight.SemiBold)
                    }
                }
                if (isLastRow) {
                    KeyboardKey(
                        modifier = Modifier.weight(1.5f),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = onBackspaceClick
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Apagar")
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardKey(
    modifier: Modifier,
    containerColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable () -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)) {
            content()
        }
    }
}
