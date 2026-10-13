package au.edu.jcu.assessment.utilityapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import au.edu.jcu.assessment.utilityapp.ui.Trend

/** Semantic gain/loss colours that Material 3's colour scheme doesn't provide. */
@Immutable
data class MarketColors(
    val gain: Color,
    val gainContainer: Color,
    val onGainContainer: Color,
    val loss: Color,
    val lossContainer: Color,
    val onLossContainer: Color,
)

val LightMarketColors = MarketColors(
    gain = GainLight,
    gainContainer = GainContainerLight,
    onGainContainer = OnGainContainerLight,
    loss = LossLight,
    lossContainer = LossContainerLight,
    onLossContainer = OnLossContainerLight,
)

val DarkMarketColors = MarketColors(
    gain = GainDark,
    gainContainer = GainContainerDark,
    onGainContainer = OnGainContainerDark,
    loss = LossDark,
    lossContainer = LossContainerDark,
    onLossContainer = OnLossContainerDark,
)

val LocalMarketColors = staticCompositionLocalOf { LightMarketColors }

/** Line / text colour for a price movement. */
@Composable
@ReadOnlyComposable
fun Trend.contentColor(): Color = when (this) {
    Trend.UP -> LocalMarketColors.current.gain
    Trend.DOWN -> LocalMarketColors.current.loss
    Trend.FLAT -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Background and foreground pair for a price-change badge. */
@Composable
@ReadOnlyComposable
fun Trend.badgeColors(): Pair<Color, Color> = when (this) {
    Trend.UP -> LocalMarketColors.current.gainContainer to LocalMarketColors.current.onGainContainer
    Trend.DOWN -> LocalMarketColors.current.lossContainer to LocalMarketColors.current.onLossContainer
    Trend.FLAT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
}
