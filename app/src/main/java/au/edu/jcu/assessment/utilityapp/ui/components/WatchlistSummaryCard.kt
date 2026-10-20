package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.DisplayCurrency
import au.edu.jcu.assessment.utilityapp.ui.PreviewData
import au.edu.jcu.assessment.utilityapp.ui.Trend
import au.edu.jcu.assessment.utilityapp.ui.WatchlistContent
import au.edu.jcu.assessment.utilityapp.ui.format.StockFormatters
import au.edu.jcu.assessment.utilityapp.ui.theme.StockPulseTheme
import au.edu.jcu.assessment.utilityapp.ui.theme.contentColor
import java.time.Instant

/**
 * The "at a glance" headline: one big number (average move of the watchlist today),
 * followed by how many stocks are up/down and the best and worst performers.
 */
@Composable
fun WatchlistSummaryCard(
    content: WatchlistContent,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    val summary = content.summary
    val averageTrend = Trend.fromChange(summary.averageChangePercent)
    val showIllustration = LocalConfiguration.current.screenWidthDp >= 360 &&
        LocalDensity.current.fontScale <= 1.3f

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.summary_average_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = StockFormatters.signedPercent(summary.averageChangePercent),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (averageTrend == Trend.FLAT) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            averageTrend.contentColor()
                        },
                    )
                }
                if (showIllustration) {
                    Image(
                        painter = painterResource(R.drawable.illustration_market),
                        contentDescription = null,
                        modifier = Modifier.size(width = 80.dp, height = 60.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.summary_up_down, summary.upCount, summary.downCount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            val best = summary.bestPerformer
            val worst = summary.worstPerformer
            if (best != null && worst != null && best != worst) {
                Text(
                    text = stringResource(
                        R.string.summary_best_worst,
                        best.symbol,
                        StockFormatters.signedPercent(best.changePercent),
                        worst.symbol,
                        StockFormatters.signedPercent(worst.changePercent),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Text(
                text = footerText(content, now),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun footerText(content: WatchlistContent, now: Instant): String {
    val updated = stringResource(
        R.string.summary_updated,
        StockFormatters.localTime(content.lastUpdated, now),
    )
    val currencyNote = when {
        content.isCurrencyFallback -> stringResource(R.string.summary_currency_fallback)
        content.currency == DisplayCurrency.SGD && content.usdToSgdRate != null ->
            stringResource(R.string.summary_fx_rate, StockFormatters.exchangeRate(content.usdToSgdRate))
        else -> null
    }
    return listOfNotNull(updated, currencyNote).joinToString(" · ")
}

@Preview
@Composable
private fun WatchlistSummaryCardPreview() {
    StockPulseTheme {
        WatchlistSummaryCard(content = PreviewData.content, now = PreviewData.now)
    }
}
