package com.example.studygroupfinder.screens

import android.util.Log
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studygroupfinder.R
import com.example.studygroupfinder.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onSignIn: () -> Unit,
    isLoading: Boolean,
    errorMessage: String? = null,
    onClearError: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animatedColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        targetValue = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "background_color"
    )

    // Floating animation for decorative elements
    val floatOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1"
    )

    val floatOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float2"
    )

    // Extract theme colors outside Canvas
    val primaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    val secondaryColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.04f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedColor,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    radius = 1200f,
                    center = Offset(0.3f, 0.2f)
                )
            )
    ) {
        // Enhanced background decoration with theme colors
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Large circle top right with floating animation
            drawCircle(
                color = primaryColor,
                radius = 180.dp.toPx(),
                center = Offset(
                    canvasWidth * 0.85f,
                    canvasHeight * 0.15f + floatOffset1
                )
            )

            // Medium circle bottom left
            drawCircle(
                color = secondaryColor,
                radius = 140.dp.toPx(),
                center = Offset(
                    canvasWidth * 0.15f,
                    canvasHeight * 0.85f + floatOffset2
                )
            )

            // Small circle top left
            drawCircle(
                color = tertiaryColor,
                radius = 90.dp.toPx(),
                center = Offset(
                    canvasWidth * 0.1f,
                    canvasHeight * 0.25f - floatOffset1 * 0.5f
                )
            )

            // Additional accent circle middle right
            drawCircle(
                color = primaryContainerColor,
                radius = 60.dp.toPx(),
                center = Offset(
                    canvasWidth * 0.9f,
                    canvasHeight * 0.6f + floatOffset2 * 0.7f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Top section with enhanced app branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Enhanced App Icon with glassmorphism effect
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(1f, 1f)
                            )
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            RoundedCornerShape(32.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Study Group Finder",
                        modifier = Modifier.size(65.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Study Group Finder",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Connect • Learn • Succeed Together",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.2.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Middle section with enhanced glassmorphism cards
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (isLoading) {
                    // Enhanced loading state with glassmorphism
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(52.dp),
                                strokeWidth = 5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Signing you in...",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = (-0.2).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Please wait while we authenticate your account",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Welcome content with enhanced styling
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = "Welcome Back!",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Sign in to discover study groups, connect with peers, and accelerate your learning journey",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 26.sp,
                                letterSpacing = 0.1.sp
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Enhanced error message with better theming
                        errorMessage?.let { error ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Sign in failed",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    letterSpacing = (-0.1).sp
                                                ),
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = error,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    lineHeight = 20.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = onClearError,
                                        modifier = Modifier.align(Alignment.End),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = ButtonDefaults.buttonElevation(
                                            defaultElevation = 4.dp
                                        )
                                    ) {
                                        Text(
                                            text = "Dismiss",
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 0.1.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Premium Google Sign In Button with enhanced theming
                        Button(
                            onClick = onSignIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSystemInDarkTheme()) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    Color.White
                                },
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(20.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 12.dp,
                                pressedElevation = 16.dp,
                                hoveredElevation = 14.dp
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable._123025_logo_google_g_icon),
                                    contentDescription = "Google",
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.Unspecified
                                )
                                Text(
                                    text = "Continue with Google",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.1.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(100.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}


