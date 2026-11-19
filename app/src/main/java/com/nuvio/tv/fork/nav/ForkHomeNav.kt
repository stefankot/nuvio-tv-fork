package com.nuvio.tv.fork.nav

import androidx.navigation.NavHostController
import com.nuvio.tv.ui.navigation.Screen

/**
 * Fork 19.11 (zgłoszenie 1: „Home w pasku nie działa”). Pasek nawiguje do zakładki przez
 * popUpTo(start){saveState} + restoreState. Home = start, więc z ekranu otwartego nad Home (szczegóły,
 * Historia, ekrany forka) zdjęty stos jest zapisywany pod Home i od razu przywracany — wraca ten sam ekran.
 * Home: zdejmij stos do Home i porzuć zapis. Zwraca true, gdy obsłużone.
 */
object ForkHomeNav {
    fun handle(nav: NavHostController, currentRoute: String?, targetRoute: String): Boolean {
        if (targetRoute != Screen.Home.route || currentRoute == targetRoute) return false
        val hasHome = runCatching { nav.getBackStackEntry(Screen.Home.route) }.isSuccess
        if (!hasHome) return false
        android.util.Log.i("ForkHomeNav", "home from $currentRoute")
        nav.clearBackStack(Screen.Home.route)
        return nav.popBackStack(Screen.Home.route, inclusive = false)
    }
}
