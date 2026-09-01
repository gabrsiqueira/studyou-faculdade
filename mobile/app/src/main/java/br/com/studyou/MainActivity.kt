package br.com.studyou

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import br.com.studyou.di.AppContainer
import br.com.studyou.ui.di.LocalAppContainer
import br.com.studyou.ui.navigation.StudYouNavHost
import br.com.studyou.ui.theme.StudyouTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyouTheme {
                CompositionLocalProvider(LocalAppContainer provides AppContainer.getInstance(applicationContext)) {
                    StudYouNavHost()
                }
            }
        }
    }
}