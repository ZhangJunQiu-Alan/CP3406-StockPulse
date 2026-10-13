package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.ui.PreviewData
import au.edu.jcu.assessment.utilityapp.ui.StockRowUiModel
import au.edu.jcu.assessment.utilityapp.ui.Trend
import au.edu.jcu.assessment.utilityapp.ui.format.StockFormatters
import au.edu.jcu.assessment.utilityapp.ui.theme.StockPulseTheme
import au.edu.jcu.assessment.utilityapp.ui.theme.contentColor

/**
 * One watchlist entry: logo, symbol/name, sparkline, price and change badge.
 * The whole row is read by TalkBack as a single sentence, e.g.
 * "NVDA, NVIDIA Corp. Price S$233.51. Up 3.52% today."
 */
@Composable
fun StockRow(
    stock: StockRowUiModel,
    modifier: Modifier = Modifier,
) {
    val description = stockRowDescription(stock)
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            CompanyLogo(logoUrl = stock.logoUrl, symbol = stock.symbol)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stock.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stock.companyName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            SparklineChart(
                points = stock.sparkline,
                lineColor = stock.sparklineTrend.contentColor(),
                modifier = Modifier.size(width = 64.dp, height = 32.dp),
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = StockFormatters.price(stock.price, stock.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ChangeBadge(changePercent = stock.changePercent, trend = stock.trend)
            }
        }
    }
}

@Composable
private fun stockRowDescription(stock: StockRowUiModel): String {
    val percent = StockFormatters.unsignedPercent(stock.changePercent)
    val movement = when (stock.trend) {
        Trend.UP -> stringResource(R.string.trend_up_description, percent)
        Trend.DOWN -> stringResource(R.string.trend_down_description, percent)
        Trend.FLAT -> stringResource(R.string.trend_flat_description)
    }
    return stringResource(
        R.string.stock_row_description,
        stock.symbol,
        stock.companyName,
        StockFormatters.price(stock.price, stock.currency),
        movement,
    )
}

@Preview
@Composable
private fun StockRowPreview() {
    StockPulseTheme {
        StockRow(stock = PreviewData.content.rows.first())
    }
}
