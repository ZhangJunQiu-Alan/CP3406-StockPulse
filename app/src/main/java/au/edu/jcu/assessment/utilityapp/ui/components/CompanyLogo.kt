package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

/**
 * Circular company logo loaded with Coil. Falls back to the symbol's first letter while the
 * image loads, if it fails, or if Finnhub has no logo for the company.
 * Decorative only (no content description): the row already announces the company name.
 */
@Composable
fun CompanyLogo(
    logoUrl: String?,
    symbol: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val circleModifier = modifier.size(size).clip(CircleShape)

    if (logoUrl == null) {
        LetterAvatar(symbol, circleModifier)
    } else {
        SubcomposeAsyncImage(
            model = logoUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = circleModifier.background(Color.White),
            loading = { LetterAvatar(symbol) },
            error = { LetterAvatar(symbol) },
        )
    }
}

@Composable
private fun LetterAvatar(symbol: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol.take(1),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Preview
@Composable
private fun CompanyLogoFallbackPreview() {
    CompanyLogo(logoUrl = null, symbol = "NVDA")
}
