package com.example.radiyo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.radiyo.data.model.UserRole
import com.example.radiyo.data.repository.UserRepository
import com.example.radiyo.notification.InAppNotificationManager
import com.example.radiyo.ui.components.InAppNotificationBanner
import com.example.radiyo.ui.screens.LoginScreen
import com.example.radiyo.ui.screens.ModeratorDashboardScreen
import com.example.radiyo.ui.screens.NowPlayingScreen
import com.example.radiyo.ui.screens.PlaylistsScreen
import com.example.radiyo.ui.screens.ProfileScreen
import com.example.radiyo.ui.screens.RequestsScreen

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val moderatorOnly: Boolean = false,
    val listenerOnly: Boolean = false
) {
    data object NowPlaying : Screen(
        route = "now_playing",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Requests : Screen(
        route = "requests",
        title = "Anfragen",
        selectedIcon = Icons.Filled.MusicNote,
        unselectedIcon = Icons.Outlined.MusicNote,
        listenerOnly = true
    )

    data object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        moderatorOnly = true
    )

    data object Playlists : Screen(
        route = "playlists",
        title = "Playlists",
        selectedIcon = Icons.AutoMirrored.Filled.QueueMusic,
        unselectedIcon = Icons.AutoMirrored.Outlined.QueueMusic,
        moderatorOnly = true
    )

    data object Profile : Screen(
        route = "profile",
        title = "Profil",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    data object Login : Screen(
        route = "login",
        title = "Anmelden",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

@Composable
fun RadiyoApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val scope = rememberCoroutineScope()

    val userRepository = UserRepository.getInstance()
    val currentUser by userRepository.currentUser.collectAsState()
    val isAuthenticated by userRepository.isAuthenticated.collectAsState()

    val isModerator = currentUser?.role == UserRole.MODERATOR

    val notificationManager = InAppNotificationManager.getInstance()
    val currentNotification by notificationManager.currentNotification.collectAsState()

    LaunchedEffect(isAuthenticated, isModerator) {
        if (isAuthenticated && isModerator) {
            notificationManager.startListening()
        } else {
            notificationManager.reset()
        }
    }

    val bottomNavItems = buildList {
        add(Screen.NowPlaying)
        if (!isModerator) {
            add(Screen.Requests)
        }
        if (isModerator) {
            add(Screen.Dashboard)
            add(Screen.Playlists)
        }
        add(Screen.Profile)
    }

    if (!isAuthenticated) {
        LoginScreen(
            onLoginSuccess = { }
        )
        return
    }

    val startDestination = if (isModerator) Screen.Dashboard.route else Screen.NowPlaying.route

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.NowPlaying.route) {
                    NowPlayingScreen()
                }
                composable(Screen.Requests.route) {
                    RequestsScreen()
                }
                composable(Screen.Dashboard.route) {
                    ModeratorDashboardScreen()
                }
                composable(Screen.Playlists.route) {
                    PlaylistsScreen()
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onLogout = {
                            notificationManager.reset()
                            navController.navigate(Screen.NowPlaying.route) {
                                popUpTo(0)
                            }
                        }
                    )
                }
            }
        }

        if (isModerator) {
            InAppNotificationBanner(
                notification = currentNotification,
                onDismiss = { notificationManager.dismissNotification() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
