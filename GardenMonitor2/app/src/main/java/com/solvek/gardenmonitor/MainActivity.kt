package com.solvek.gardenmonitor

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
import com.solvek.gardenmonitor.ui.theme.GardenMonitorTheme
import timber.log.Timber


class MainActivity : ComponentActivity() {
    private val viewModel: CalibrateViewModel by viewModels { CalibrateViewModel.Factory }

//    private lateinit var oneTapClient: SignInClient
//    private lateinit var signInRequest: BeginSignInRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
//        requestGoogleAuth()
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

//    private fun requestGoogleAuth() {
//        oneTapClient = Identity.getSignInClient(this)
//        signInRequest = BeginSignInRequest.builder()
//            .setPasswordRequestOptions(
//                BeginSignInRequest.PasswordRequestOptions.builder()
//                    .setSupported(true)
//                    .build()
//            )
//            .setGoogleIdTokenRequestOptions(
//                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
//                    .setSupported(true)
//                    // Your server's client ID, not your Android client ID.
//                    .setServerClientId(Config.GOOGLE_SERVER_CLIENT_ID)
//                    // Only show accounts previously used to sign in.
//                    .setFilterByAuthorizedAccounts(false)
//                    .build()
//            )
//            // Automatically sign in when exactly one credential is retrieved.
//            .setAutoSelectEnabled(true)
//            .build()
//
//        oneTapClient.beginSignIn(signInRequest)
//            .addOnSuccessListener(
//                this
//            ) { result ->
//                val intentSenderRequest = IntentSenderRequest
//                    .Builder(result.pendingIntent)
//                    .build()
//                signInHandler.launch(intentSenderRequest)
//            }
//            .addOnFailureListener(this) { e -> // No saved credentials found. Launch the One Tap sign-up flow, or
//                // do nothing and continue presenting the signed-out UI.
//                Timber.tag(TAG).d(e.localizedMessage)
//            }
//    }
//
//    private val signInHandler =
//        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
//            if (it.resultCode != Activity.RESULT_OK) {
//                Timber.tag(TAG).e("User cancelled sign in")
//                return@registerForActivityResult
//            }
//            try {
//                val googleCredential = oneTapClient.getSignInCredentialFromIntent(it.data)
//                val account = Account(googleCredential.id, packageName)
//                viewModel.setGoogleAccount(account)
//
////                val idToken = googleCredential.googleIdToken
////                val username = credential.id
////                val password = credential.password
////                if (idToken != null) {
////                    // Got an ID token from Google. Use it to authenticate
////                    // with your backend.
////                    Timber.tag(TAG).d("Got ID token.")
////                } else if (password != null) {
////                    // Got a saved username and password. Use them to authenticate
////                    // with your backend.
////                    Timber.tag(TAG).d("Got password.")
////                }
//            } catch (e: ApiException) {
//                Timber.tag(TAG).e(e, "Cannot get credentials")
//            }
//        }

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