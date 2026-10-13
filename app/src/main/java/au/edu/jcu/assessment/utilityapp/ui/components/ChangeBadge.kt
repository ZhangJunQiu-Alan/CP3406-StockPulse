package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.ui.Trend
import au.edu.jcu.assessment.utilityapp.ui.format.StockFormatters
import au.edu.jcu.assessment.utilityapp.ui.theme.StockPulseTheme
import au.edu.jcu.assessment.utilityapp.ui.theme.badgeColors

/**
 * Coloured pill showing today's % change. An arrow is included as well as colour so the
 * direction is still clear for colour-blind users.
 */
@Composable
fun ChangeBadge(
    changePercent: Double,
    trend: Trend,
    modifier: Modifier = Modifier,
) {
    val (container, content) = trend.badgeColors()
    val arrow = when (trend) {
        Trend.UP -> "▲"
        Trend.DOWN -> "▼"
        Trend.FLAT -> "•"
    }
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text = "$arrow ${StockFormatters.unsignedPercent(changePercent)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Preview
@Composable
private fun ChangeBadgePreview() {
    StockPulseTheme {
        ChangeBadge(changePercent = 2.31, trend = Trend.UP)
    }
}
