package com.matthew.sportiliapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material still chooses the shape appropriate to each native control.
val SportiliShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

object SportiliSpacing {
    val extraSmall = 4.dp
    val compact = 8.dp
    val small = 12.dp
    val standard = 16.dp
    val section = 24.dp
    val large = 32.dp
}
