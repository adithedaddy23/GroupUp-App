package com.example.studygroupfinder.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.example.studygroupfinder.R

@Composable
fun BottomNavIcon(iconType: IconType, contentDescription: String) {
    when (iconType) {
        is IconType.Vector -> Icon(
            imageVector = iconType.imageVector,
            contentDescription = contentDescription
        )
        is IconType.Resource -> Icon(
            painter = iconType.painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp).padding(2.dp)
        )
    }
}

@Composable
fun getBottomNavItems(): List<BottomNavItem>{
    return listOf(
        BottomNavItem(
            label = "Explore",
            icon = IconType.Resource(painterResource(R.drawable.compass_)),
            route = "home"
        ),
        BottomNavItem(
            label = "My Events",
            icon = IconType.Resource(painterResource(R.drawable.list)),
            route = "myEvents"
        ),
        BottomNavItem(
            label = "Chats",
            icon = IconType.Resource(painterResource(R.drawable.chat_)),
            route = "chat"
        ),
        BottomNavItem(
            label = "Profile",
            icon = IconType.Vector(Icons.Default.Person),
            route = "profile"
        )

    )
}