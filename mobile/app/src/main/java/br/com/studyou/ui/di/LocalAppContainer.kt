package br.com.studyou.ui.di

import androidx.compose.runtime.staticCompositionLocalOf
import br.com.studyou.di.AppContainer

/** Exposes the [AppContainer] to the composable tree so screens can build their ViewModels. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("No AppContainer provided")
}
