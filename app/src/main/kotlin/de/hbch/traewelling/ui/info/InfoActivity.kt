package de.hbch.traewelling.ui.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import de.hbch.traewelling.BuildConfig
import de.hbch.traewelling.theme.MainTheme

class InfoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainTheme {
                InfoScreen(
                    showProjectRepo = { showProjectRepo() },
                    showLegalInfo = { showLegalInfo() },
                    backPressed = { finish() }
                )
            }
        }
    }

    private fun showProjectRepo() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(BuildConfig.REPO_URL)
        startActivity(intent)
    }

    private fun showLegalInfo() {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(false)
            .build()

        intent.launchUrl(this, Uri.parse(BuildConfig.PRIVACY_URL))
    }
}
