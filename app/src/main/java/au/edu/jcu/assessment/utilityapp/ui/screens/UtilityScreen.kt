package au.edu.jcu.assessment.utilityapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.ui.PreviewData
import au.edu.jcu.assessment.utilityapp.ui.StockViewModel
import au.edu.jcu.assessment.utilityapp.ui.WatchlistContent
import au.edu.jcu.assessment.utilityapp.ui.WatchlistUiState
import au.edu.jcu.assessment.utilityapp.ui.components.BigMoveAlertCard
import au.edu.jcu.assessment.utilityapp.ui.components.EmptyWatchlist
import au.edu.jcu.assessment.utilityapp.ui.components.FullScreenError
import au.edu.jcu.assessment.utilityapp.ui.components.InlineMessage
import au.edu.jcu.assessment.utilityapp.ui.components.LoadingState
import au.edu.jcu.assessment.utilityapp.ui.components.MarketStatusBanner
import au.edu.jcu.assessment.utilityapp.ui.components.StockRow
import au.edu.jcu.assessment.utilityapp.ui.components.WatchlistSummaryCard
import au.edu.jcu.assessment.utilityapp.ui.components.message
import au.edu.jcu.assessment.utilityapp.ui.theme.StockPulseTheme
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * Main utility screen. Collecting with lifecycle awareness means the ViewModel's polling stops
 * when this screen isn't visible.
 */
@Composable
fun UtilityScreen(
    viewModel: StockViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val now by rememberCurrentInstant()
    UtilityScreenContent(
        uiState = uiState,
        now = now,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

/** Stateless version of [UtilityScreen], so it can be previewed and tested with any state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityScreenContent(
    uiState: WatchlistUiState,
    now: Instant,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = uiState.content
    when {
        content == null && uiState.error != null ->
            FullScreenError(error = uiState.error, onRetry = onRefresh, modifier = modifier)

        content == null -> LoadingState(modifier)

        else -> PullToRefreshBox(
            isRefreshing = uiState.isUserRefreshing,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize(),
        ) {
            WatchlistList(content = content, uiState = uiState, now = now)
        }
    }
}

@Composable
private fun WatchlistList(
    content: WatchlistContent,
    uiState: WatchlistUiState,
    now: Instant,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "market-status") {
            MarketStatusBanner(status = content.marketStatus, now = now)
        }
        uiState.error?.let { error ->
            item(key = "error") { InlineMessage(text = error.message()) }
        }
        if (content.rows.isEmpty()) {
            item(key = "empty") { EmptyWatchlist() }
        } else {
            item(key = "summary") { WatchlistSummaryCard(content = content, now = now) }
        }
        if (content.bigMovers.isNotEmpty()) {
            item(key = "big-moves") {
                BigMoveAlertCard(
                    movers = content.bigMovers,
                    thresholdPercent = content.bigMoveThresholdPercent,
                )
            }
        }
        // Keys + animateItem() make rows slide smoothly when the sort order changes.
        items(content.rows, key = { it.symbol }) { stock ->
            StockRow(stock = stock, modifier = Modifier.animateItem())
        }
        if (content.unknownSymbols.isNotEmpty()) {
            item(key = "unknown") {
                InlineMessage(
                    text = stringResource(R.string.unknown_symbols, content.unknownSymbols.joinToString()),
                )
            }
        }
        item(key = "disclaimer") {
            Text(
                text = stringResource(R.string.data_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The current time, updated every [periodMillis] so countdowns stay accurate between refreshes. */
@Composable
private fun rememberCurrentInstant(periodMillis: Long = 30_000): State<Instant> =
    produceState(initialValue = Instant.now()) {
        while (true) {
            delay(periodMillis)
            value = Instant.now()
        }
    }

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun UtilityScreenPreview() {
    StockPulseTheme {
        UtilityScreenContent(
            uiState = WatchlistUiState(content = PreviewData.content),
            now = PreviewData.now,
            onRefresh = {},
        )
    }
}
