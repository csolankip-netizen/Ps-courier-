package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CarrierPartner
import com.example.data.model.DeliveryZone
import com.example.data.model.ShipmentStatus
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.IndiaPostRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ShadowfaxTeal

@Composable
fun CarrierBadge(carrier: CarrierPartner, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label, icon) = when (carrier) {
        CarrierPartner.SHADOWFAX -> Quadruple(
            ShadowfaxTeal.copy(alpha = 0.12f),
            ShadowfaxTeal,
            "Shadowfax (Primary Partner)",
            Icons.Default.Speed
        )
        CarrierPartner.INDIAN_POST -> Quadruple(
            IndiaPostRed.copy(alpha = 0.12f),
            IndiaPostRed,
            "Indian Post (Fallback Route)",
            Icons.Default.Mail
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ZoneBadge(zone: DeliveryZone, modifier: Modifier = Modifier) {
    Surface(
        color = PrimaryNavy.copy(alpha = 0.10f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = "${zone.code} • ₹${zone.flatRate.toInt()}",
            color = PrimaryNavy,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatusBadge(status: ShipmentStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status) {
        ShipmentStatus.BOOKED -> Pair(Color(0xFFE0E7FF), Color(0xFF3730A3))
        ShipmentStatus.PICKUP_SCHEDULED -> Pair(Color(0xFFFEF3C7), Color(0xFF92400E))
        ShipmentStatus.PICKED_UP -> Pair(Color(0xFFE0F2FE), Color(0xFF0369A1))
        ShipmentStatus.IN_TRANSIT -> Pair(Color(0xFFF3E8FF), Color(0xFF6B21A8))
        ShipmentStatus.OUT_FOR_DELIVERY -> Pair(Color(0xFFFFEDD5), Color(0xFFC2410C))
        ShipmentStatus.DELIVERED -> Pair(Color(0xFFDCFCE7), Color(0xFF166534))
        ShipmentStatus.RTO_INITIATED -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
        ShipmentStatus.RETURNED_TO_ORIGIN -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
        ShipmentStatus.CANCELLED -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = status.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * High-contrast Code-128 style Barcode Canvas renderer for shipping labels
 */
@Composable
fun BarcodeGraphic(
    code: String,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    barColor: Color = Color.Black
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val width = size.width
            val totalBars = 65
            val barWidth = width / (totalBars * 1.5f)

            val seed = code.hashCode()
            var currentX = (width - (totalBars * barWidth * 1.3f)) / 2f

            for (i in 0 until totalBars) {
                val isThick = ((seed shr (i % 24)) and (1 shl (i % 7))) != 0
                val skip = (i % 5 == 0 && i % 3 != 0)
                val w = if (isThick) barWidth * 1.8f else barWidth * 0.9f

                if (!skip && currentX < width) {
                    drawRect(
                        color = barColor,
                        topLeft = Offset(currentX, 0f),
                        size = Size(w, size.height)
                    )
                }
                currentX += w + (barWidth * 0.5f)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = barColor
        )
    }
}

/**
 * 2D Matrix / QR Code simulation canvas
 */
@Composable
fun QrCodeGraphic(
    data: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .border(1.5.dp, Color.Black, RoundedCornerShape(4.dp))
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size - 6.dp)) {
            val s = this.size.width
            val grid = 11
            val cellSize = s / grid
            val seed = data.hashCode()

            for (r in 0 until grid) {
                for (c in 0 until grid) {
                    val isCornerFinder = (r < 3 && c < 3) || (r < 3 && c >= grid - 3) || (r >= grid - 3 && c < 3)
                    val isCenterDot = isCornerFinder && ((r in 0..2 && c in 0..2 && (r == 1 && c == 1)) ||
                            (r in 0..2 && c >= grid - 3 && (r == 1 && c == grid - 2)) ||
                            (r >= grid - 3 && c in 0..2 && (r == grid - 2 && c == 1)))

                    val isFinderBorder = isCornerFinder && (r == 0 || r == 2 || c == 0 || c == 2 ||
                            r == grid - 1 || r == grid - 3 || c == grid - 1 || c == grid - 3)

                    val isRandomPixel = ((seed shr ((r * grid + c) % 28)) and 1) == 1

                    if (isFinderBorder || isCenterDot || (!isCornerFinder && isRandomPixel)) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.95f, cellSize * 0.95f)
                        )
                    }
                }
            }
        }
    }
}
