package com.tasp1.pocketpal.ui.components.claude

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun ClaudeHomeEmpty(
    userName: String = "there",
    modifier: Modifier = Modifier,
) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Hello"
        }
    }
    val starColor = Color(0xFFC4785A)

    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(56.dp)) {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = size.minDimension / 2.2f
            // soft starburst (Claude-like mark)
            for (i in 0 until 12) {
                val a = (i * 30f) * (Math.PI / 180f).toFloat()
                drawLine(
                    color = starColor,
                    start = Offset(cx + kotlin.math.cos(a) * r * 0.25f, cy + kotlin.math.sin(a) * r * 0.25f),
                    end = Offset(cx + kotlin.math.cos(a) * r, cy + kotlin.math.sin(a) * r),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(starColor.copy(alpha = 0.15f), radius = r * 0.35f, center = Offset(cx, cy))
        }
        Text(
            "$greeting, $userName",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground,
            ),
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            "Agent: type $ ls or /shell pwd · Shell chip → Render",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
