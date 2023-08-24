package com.solvek.gardenmonitor

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.solvek.gardenmonitor.ui.theme.GardenMonitorTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private val viewModel: CalibrateViewModel by viewModels { CalibrateViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        requestSignIn()
        setContent {
            GardenMonitorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    CalibrateScreen(viewModel)
                }
            }
        }
    }

    private val signInHandler =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) {
                Timber.tag(TAG).e("User cancelled sign in")
                return@registerForActivityResult
            }
            GoogleSignIn.getSignedInAccountFromIntent(it.data)
                .addOnSuccessListener { account ->
//                    val scopes = listOf(SheetsScopes.SPREADSHEETS)
//                    val credential = GoogleAccountCredential.usingOAuth2(this, scopes)
//                    credential.selectedAccount = account.account
//                    val jsonFactory = JacksonFactory.getDefaultInstance()
//                    // GoogleNetHttpTransport.newTrustedTransport()
//                    val httpTransport =  AndroidHttp.newCompatibleTransport()
//                    val service = Sheets.Builder(httpTransport, jsonFactory, credential)
//                        .setApplicationName(getString(R.string.app_name))
//                        .build()
                }
                .addOnFailureListener { e ->
                    Timber.tag(TAG).e(e, "Failed to get sign in information")
                }
        }

    private fun requestSignIn() {
        /*
        GoogleSignIn.getLastSignedInAccount(context)?.also { account ->
            Timber.d("account=${account.displayName}")
        }
         */
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // .requestEmail()
            // .requestScopes(Scope(SheetsScopes.SPREADSHEETS_READONLY))
//            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
            .requestScopes(Scope("https://www.googleapis.com/auth/spreadsheets"))
            .build()
        val client = GoogleSignIn.getClient(this, signInOptions)
        signInHandler.launch(client.signInIntent)
    }


    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }

        val permission = android.Manifest.permission.BLUETOOTH_CONNECT

        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED){
            return
        }

        requestMultiplePermissions.launch(arrayOf(permission))
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Timber.tag(TAG).d("${it.key} = ${it.value}")
            }
        }

    companion object {
        private const val TAG = "MainActivity"
    }
}