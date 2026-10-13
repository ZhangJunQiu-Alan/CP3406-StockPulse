package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.MarketSession
import au.edu.jcu.assessment.utilityapp.data.model.MarketStatus
import au.edu.jcu.assessment.utilityapp.domain.UsMarketHours
import au.edu.jcu.assessment.utilityapp.ui.PreviewData
import au.edu.jcu.assessment.utilityapp.ui.format.StockFormatters
import au.edu.jcu.assessment.utilityapp.ui.theme.LocalMarketColors
import au.edu.jcu.assessment.utilityapp.ui.theme.StockPulseTheme
import java.time.Duration
import java.time.Instant

/**
 * Answers "Is the US market trading right now, and when does that change?" with the time
 * converted into the user's own time zone (useful for Singapore users watching New York).
 */
@Composable
fun MarketStatusBanner(
    status: MarketStatus,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    val title = when {
        status.holiday != null && status.session == MarketSession.CLOSED ->
            stringResource(R.string.market_holiday, status.holiday)
        status.session == MarketSession.REGULAR -> stringResource(R.string.market_open)
        status.session == MarketSession.PRE_MARKET -> stringResource(R.string.market_pre)
        status.session == MarketSession.POST_MARKET -> stringResource(R.string.market_post)
        else -> stringResource(R.string.market_closed)
    }
    val dotColor = when (status.session) {
        MarketSession.REGULAR -> LocalMarketColors.current.gain
        MarketSession.PRE_MARKET, MarketSession.POST_MARKET -> MaterialTheme.colorScheme.tertiary
        MarketSession.CLOSED -> MaterialTheme.colorScheme.outline
    }
    val detail = if (status.session == MarketSession.REGULAR) {
        val close = UsMarketHours.closingTimeOn(now)
        stringResource(
            R.string.market_closes_in,
            StockFormatters.countdown(Duration.between(now, close)),
            StockFormatters.localTime(close, now),
        )
    } else {
        val open = UsMarketHours.nextOpeningTime(now)
        stringResource(
            R.string.market_opens_in,
            StockFormatters.countdown(Duration.between(now, open)),
            StockFormatters.localTime(open, now),
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            StatusDot(color = dotColor)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape),
    )
}

@Preview
@Composable
private fun MarketStatusBannerPreview() {
    StockPulseTheme {
        MarketStatusBanner(status = MarketStatus(MarketSession.REGULAR), now = PreviewData.now)
    }
}
