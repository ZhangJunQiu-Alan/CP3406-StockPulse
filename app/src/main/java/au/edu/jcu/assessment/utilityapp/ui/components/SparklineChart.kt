package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A minimal line chart drawn directly on a [Canvas], scaled to the series' own min/max so the
 * shape of the movement is visible at a glance. Draws nothing if there are fewer than 2 points.
 */
@Composable
fun SparklineChart(
    points: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) {
        Spacer(modifier) // keep the row layout stable when no chart data is available
        return
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 1.5.dp.toPx()
        val min = points.min()
        val range = (points.max() - min).takeIf { it > 0 } ?: 1.0
        val usableHeight = size.height - strokeWidth
        val stepX = size.width / (points.size - 1)

        fun yFor(value: Double): Float =
            strokeWidth / 2 + usableHeight - ((value - min) / range * usableHeight).toFloat()

        val line = Path().apply {
            points.forEachIndexed { index, value ->
                if (index == 0) moveTo(0f, yFor(value)) else lineTo(index * stepX, yFor(value))
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = area,
            brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)),
        )
        drawPath(
            path = line,
            color = lineColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SparklineChartPreview() {
    SparklineChart(
        points = listOf(10.0, 10.4, 10.1, 10.9, 11.3, 11.0, 11.8),
        lineColor = Color(0xFF1B7F3B),
        modifier = Modifier.size(width = 120.dp, height = 40.dp),
    )
}
