// ==========================================
// FILE: presentation/MainActivity.kt
// DESCRIPTION: App entry point and Compose navigation host with Google Sign-In.
// ==========================================
package com.journal.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.journal.app.JournalApplication
import com.journal.app.presentation.screens.HomeScreen

/**
 * SECTION: Main Activity
 * Hosts the single-activity Compose UI and handles OAuth / permission flows.
 */
class MainActivity : ComponentActivity() {

    private lateinit var journalViewModel: JournalViewModel

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(Exception::class.java)
            account?.email?.let { email ->
                journalViewModel.onSignedIn(email)
            }
        } catch (_: Exception) {
            // Sign-in cancelled or failed; no action required.
        }
    }

    private val recordPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            journalViewModel.startVoiceRecording()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as JournalApplication
        val factory = JournalViewModelFactory(app, app.repository)
        journalViewModel = ViewModelProvider(this, factory)[JournalViewModel::class.java]

        restoreExistingSignIn(app)

        setContent {
            MaterialTheme {
                Surface {
                    val hasRecordPermission = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    HomeScreen(
                        viewModel = journalViewModel,
                        onRequestSignIn = { launchGoogleSignIn() },
                        onRequestRecordPermission = {
                            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        hasRecordPermission = hasRecordPermission
                    )
                }
            }
        }
    }

    private fun restoreExistingSignIn(app: JournalApplication) {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        account?.email?.let { email ->
            app.repository.setSignedInAccountEmail(email)
            journalViewModel.onSignedIn(email, showMessage = false)
        }
    }

    private fun launchGoogleSignIn() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        val client = GoogleSignIn.getClient(this, signInOptions)
        googleSignInLauncher.launch(client.signInIntent)
    }
}
