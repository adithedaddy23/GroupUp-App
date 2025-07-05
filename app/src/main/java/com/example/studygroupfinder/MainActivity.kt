package com.example.studygroupfinder

import HomeScreen
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.studygroupfinder.screens.MyEventsScreen
import com.example.studygroupfinder.screens.ProfileScreen
import com.example.studygroupfinder.ui.theme.StudyGroupFinderTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.studygroupfinder.firestore.FirestoreRepository
import com.example.studygroupfinder.googleSignIn.GoogleSignInClient
import com.example.studygroupfinder.navigation.BottomNavIcon
import com.example.studygroupfinder.navigation.getBottomNavItems
import com.example.studygroupfinder.screens.ChatListScreen
import com.example.studygroupfinder.screens.JoinedEvents
import com.example.studygroupfinder.screens.LoadingScreen
import com.example.studygroupfinder.screens.LoginScreen
import com.example.studygroupfinder.screens.MessageScreen
import com.example.studygroupfinder.screens.NewEventScreen
import com.example.studygroupfinder.screens.ParticipantsScreen
import com.example.studygroupfinder.viewmodel.AuthState
import com.example.studygroupfinder.viewmodel.AuthViewModel
import com.example.studygroupfinder.viewmodel.AuthViewModelFactory
import com.example.studygroupfinder.viewmodel.ChatViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)

        // Now that chatDao is initialized, you can use it
        googleSignInClient = GoogleSignInClient(this) // Assuming GoogleSignInClient needs it
        val factory = AuthViewModelFactory(googleSignInClient)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        setContent {
            StudyGroupFinderTheme {
                // Pass the initialized chatDao
                AppNavigation(authViewModel = authViewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.observeAsState()
    val isLoading by authViewModel.isLoading.observeAsState(false)
    val errorMessage by authViewModel.errorMessage.observeAsState()

    // Create repository instance to pass to chat screens
    val repository = remember { FirestoreRepository() }

    // Show loading screen only during initial auth check
    if (authState == null && isLoading) {
        LoadingScreen()
        return
    }

    // Determine start destination based on auth state
    val startDestination = when (authState) {
        AuthState.Authenticated -> "home"
        AuthState.NotAuthenticated -> "auth"
        null -> "auth" // Default to auth if state is still null
    }

    Scaffold(
        bottomBar = {
            if (authState == AuthState.Authenticated) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("auth") {
                // Navigate to home if user becomes authenticated while on auth screen
                LaunchedEffect(authState) {
                    Log.d("AuthScreen", "Auth state changed to: $authState")
                    if (authState == AuthState.Authenticated) {
                        Log.d("AuthScreen", "Navigating to home")
                        navController.navigate("home") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                }

                LoginScreen(
                    onSignIn = {
                        Log.d("AuthScreen", "onSignIn called")
                        authViewModel.signIn()
                    },
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onClearError = { authViewModel.clearError() }
                )
            }
            composable("home") {
                // Navigate to auth if user becomes unauthenticated while on home screen
                LaunchedEffect(authState) {
                    if (authState == AuthState.NotAuthenticated) {
                        navController.navigate("auth") {
                            popUpTo(0)
                        }
                    }
                }

                HomeScreen(
                    authViewModel = authViewModel,
                    navController = navController,
                )
            }
            composable("myEvents") {
                // Navigate to auth if user becomes unauthenticated
                LaunchedEffect(authState) {
                    if (authState == AuthState.NotAuthenticated) {
                        navController.navigate("auth") {
                            popUpTo(0)
                        }
                    }
                }

                MyEventsScreen(
                    onNewEventClick = {
                        navController.navigate("newEvents")
                    },
                    authViewModel = authViewModel,
                    onEventClick = { eventId ->
                        // Handle event click here (e.g., navigate to detail screen if needed)
                        Log.d("MyEventsScreen", "Event clicked: $eventId")
                    },
                    repository = repository,
                    navController = navController,
                )
            }
            composable("profile") {
                // Navigate to auth if user becomes unauthenticated
                LaunchedEffect(authState) {
                    if (authState == AuthState.NotAuthenticated) {
                        navController.navigate("auth") {
                            popUpTo(0)
                        }
                    }
                }

                ProfileScreen(
                    authViewModel = authViewModel,
                    onSignOut = { authViewModel.signOut() }
                )
            }

            // Chat screens
            composable("chat") {
                // Navigate to auth if user becomes unauthenticated
                LaunchedEffect(authState) {
                    if (authState == AuthState.NotAuthenticated) {
                        navController.navigate("auth") {
                            popUpTo(0)
                        }
                    }
                }

                ChatListScreen(
                    viewModel = remember { ChatViewModel(repository) },
                    onChatClick = { chatId ->
                        navController.navigate("message_screen/$chatId")
                    }
                )
            }

            composable(
                route = "message_screen/{chatId}",
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""

                // Navigate to auth if user becomes unauthenticated
                LaunchedEffect(authState) {
                    if (authState == AuthState.NotAuthenticated) {
                        navController.navigate("auth") {
                            popUpTo(0)
                        }
                    }
                }

                MessageScreen(
                    chatId = chatId,
                    viewModel = remember { ChatViewModel(repository) },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "participantScreen/{eventId}/{hostId}",
                arguments = listOf(
                    navArgument("eventId") { type = NavType.StringType },
                    navArgument("hostId") { type = NavType.StringType }
                )
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                val hostId = it.arguments?.getString("hostId") ?: ""

                // Navigate to auth if user becomes unauthenticated
                LaunchedEffect(authState) {
                    if (authState == AuthState.NotAuthenticated) {
                        navController.navigate("auth") {
                            popUpTo(0)
                        }
                    }
                }

                ParticipantsScreen(
                    eventId = eventId,
                    hostId = hostId,
                    repository = repository,
                    navController = navController,
                )
            }

            composable ("joinedEvents") {
                JoinedEvents()
            }

            composable("newEvents") {
                // Navigate to auth if user becomes unauthenticated
                LaunchedEffect(authState) {
                    if (authState == AuthState.NotAuthenticated) {
                        navController.navigate("auth") {
                            popUpTo(0)
                        }
                    }
                }

                NewEventScreen(
                    onBackClick = { navController.popBackStack() },
                    repository = repository,
                    navController = navController,
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current
    val bottomNavItems = getBottomNavItems() // Get items from composable function

    if (currentRoute in listOf("home", "myEvents", "profile", "chat")) {
        NavigationBar(modifier = modifier) {
            bottomNavItems.forEach { navItem -> // Use the composable function result
                val selected = currentRoute == navItem.route

                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.2f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "iconScaleAnimation"
                )

                val iconOffsetY by animateDpAsState(
                    targetValue = if (selected) 0.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "iconOffsetYAnimation"
                )

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        navController.navigate(navItem.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        // Use the BottomNavIcon composable with animations
                        Box(
                            modifier = Modifier
                                .scale(iconScale)
                                .offset(y = iconOffsetY)
                        ) {
                            BottomNavIcon(
                                iconType = navItem.icon,
                                contentDescription = navItem.label,
                            )
                        }
                    },
                    label = { Text(text = navItem.label) },
                    alwaysShowLabel = false
                )
            }
        }
    }
}
