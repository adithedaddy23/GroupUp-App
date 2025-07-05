package com.example.studygroupfinder.navigation

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector


sealed class IconType {
    data class Vector(val imageVector: ImageVector) : IconType()
    data class Resource(val painter: Painter) : IconType()
}

data class BottomNavItem(
    val label: String,
    val icon: IconType,
    val route: String,
    val selectedIcon: IconType? = null
)