package com.devoid.menumate.prsentation.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import com.devoid.menumate.R
import com.devoid.menumate.domain.repository.LoginRepository
import com.devoid.menumate.prsentation.state.LoginUiState
import com.devoid.menumate.prsentation.ui.BottomSheetDialog
import com.devoid.menumate.prsentation.ui.LoginActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val loginRepository: LoginRepository) : ViewModel() {

    private val randomNonce: String
        get() {
            try {
                val uid = UUID.randomUUID().toString()
                val bytes = uid.toByteArray()
                var md: MessageDigest? = null

                md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                return getHexValue(digest)
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }
        }
    var lastPassResetEmailSent = 0L

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

   suspend fun loginGoogle(context: Context){
        try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(R.string.web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(true)
                .setNonce(randomNonce)
                .build()
            val credentialRequest: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val credential = credentialManager.getCredential(
                context,
                credentialRequest
            ).credential
           loginRepository.loginGoogle(credential,::onResult)
        } catch (e: Exception) {
            _uiState.value=LoginUiState.Failure(e)
        }
    }

    fun loginTwitter(activity: Activity){
            val provider = OAuthProvider.newBuilder("twitter.com")
            provider.addCustomParameter("oauth_nonce", randomNonce)
            val pendingResultTask = Firebase.auth.pendingAuthResult
            val onCompleteListener = OnCompleteListener<AuthResult?> { task ->
                task.exception?.let {
                    onResult(task.exception, null)
                    return@OnCompleteListener
                }
                onResult(null, task.result.user)
            }
            // There's something already here! Finish the sign-in
            pendingResultTask?.addOnCompleteListener(onCompleteListener) ?: Firebase.auth
                .startActivityForSignInWithProvider(activity, provider.build())
                .addOnCompleteListener(onCompleteListener)
    }

    fun login(email: String, password: String){
        loginRepository.loginEmailPassword(email,password,::onResult)
    }

    fun signUp(uName: String, email: String, password: String){
        loginRepository.signUp(uName,email,password,::onResult)
    }

    fun sendPasswordResetEmail(email:String){
        loginRepository.sendPassResetEmail(email){e->
            e?.let {
                _uiState.value = LoginUiState.Failure(e)
                return@sendPassResetEmail
            }
            lastPassResetEmailSent = System.currentTimeMillis()
            _uiState.value = LoginUiState.ResetPassEmailSent
        }
    }

    private fun onResult(e:Exception?,user: FirebaseUser?){
        e?.let {
            _uiState.value = LoginUiState.Failure(e)
            return
        }
        loginRepository.updateUserOnDb(user!!){e2->
            e2?.let {
                _uiState.value = LoginUiState.Failure(Exception("Failed to update database!"))
            }
            _uiState.value = LoginUiState.Success
        }
    }



    private fun getHexValue(byteArray: ByteArray): String {
        var hex = ""

        // Iterating through each byte in the array
        for (i in byteArray) {
            hex += String.format("%02X", i)
        }

        return hex
    }
}

fun showLogoutDialog(activity: AppCompatActivity) {
    val dialog = BottomSheetDialog()
    dialog.apply {
        title = "LogOut?"
        secondaryText = "Are you sure You want to LogOut?"
        icon = R.drawable.logout
    }
    dialog.listener = BottomSheetDialog.OnShowListener {
        dialog.primaryBtn("LogOut") {
            Firebase.auth.signOut()
            activity.startActivity(Intent(activity, LoginActivity::class.java))
            activity.finish()
        }
        dialog.secondaryBtn("Cancel") {
            dialog.dismiss()
        }
    }
    dialog.show(activity.supportFragmentManager, "LogoutDialog")
}