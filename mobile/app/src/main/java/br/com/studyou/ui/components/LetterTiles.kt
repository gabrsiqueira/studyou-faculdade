package br.com.studyou.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.studyou.ui.theme.AprendixColors

private const val LETTER_TILE_COLUMNS = 4

/**
 * A grid of tappable letter tiles, like the letter wheel of Palavras Cruzadas: the player
 * taps letters in sequence to build a word.
 *
 * Deliberately not a LazyVerticalGrid: it sits inside screens that already scroll
 * vertically, and nesting a lazy (scrollable) grid there crashes with an "infinity maximum
 * height constraints" error. There are only a handful of letters, so laziness buys nothing.
 */
@Composable
fun LetterTileGrid(
    letters: List<Char>,
    selectedIndices: List<Int>,
    onLetterTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier.fillMaxWidth()) {
        letters.indices.chunked(LETTER_TILE_COLUMNS).forEach { rowIndices ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                rowIndices.forEach { index ->
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                        LetterTile(
                            letter = letters[index],
                            selected = index in selectedIndices,
                            onClick = { onLetterTap(index) }
                        )
                    }
                }
                repeat(LETTER_TILE_COLUMNS - rowIndices.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun LetterTile(letter: Char, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = CircleShape,
        color = containerColor,
        onClick = onClick,
        modifier = modifier.size(64.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(letter.toString(), color = contentColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Shows the word currently being built from tapped letters, highlighted in red on a wrong guess. */
@Composable
fun CurrentWordBanner(word: String, placeholder: String, isError: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = word.ifEmpty { placeholder },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                isError -> AprendixColors.StatusDanger
                word.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}
