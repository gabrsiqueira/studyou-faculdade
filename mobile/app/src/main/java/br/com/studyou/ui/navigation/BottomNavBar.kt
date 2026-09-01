package br.com.studyou.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

private data class BottomNavTab(val screen: Screen, val label: String, val icon: ImageVector)

private val bottomNavTabs = listOf(
    BottomNavTab(Screen.Files, "Arquivos", Icons.Filled.Description),
    BottomNavTab(Screen.History, "Histórico", Icons.Filled.History),
    BottomNavTab(Screen.Profile, "Perfil", Icons.Filled.Person)
)

/** Routes that show the bottom navigation bar. */
val bottomNavRoutes = bottomNavTabs.map { it.screen.route }.toSet()

@Composable
fun StudYouBottomNavBar(currentRoute: String?, onNavigate: (Screen) -> Unit) {
    NavigationBar {
        bottomNavTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.screen.route,
                onClick = { onNavigate(tab.screen) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
