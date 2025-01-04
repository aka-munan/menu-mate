package com.devoid.menumate.data.reository

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.devoid.menumate.domain.repository.LoginRepository
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import javax.inject.Inject

class LoginRepositoryImpl @Inject constructor() : LoginRepository {

    override fun loginGoogle(
        credential: Credential,
        onResult: (Exception?, FirebaseUser?) -> Unit
    ) {
        if (credential is CustomCredential) {
            // GoogleIdToken credentialt
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential: GoogleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(credential.data)
                val authCredential =
                    GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                Firebase.auth.signInWithCredential(authCredential)
                    .addOnCompleteListener { task ->
                        task.exception?.let {
                            onResult(task.exception, null)
                            return@addOnCompleteListener
                        }
                        onResult(null, task.result.user)
                    }
            }
        }
    }

    override fun loginEmailPassword(
        email: String,
        password: String,
        onResult: (Exception?, FirebaseUser?) -> Unit
    ) {
        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                task.exception?.let {
                    onResult(task.exception, null)
                    return@addOnCompleteListener
                }
                onResult(null, task.result.user)
            }
    }

    override fun signUp(
        uName: String,
        email: String,
        password: String,
        onResult: (Exception?, FirebaseUser?) -> Unit
    ) {
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                task.exception?.let {
                    onResult(task.exception, null)
                    return@addOnCompleteListener
                }
                val user = task.result.user!!
                val profileUpdates =
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(uName)
                        .build()
                user.updateProfile(profileUpdates)
                onResult(null, user)
            }
    }



    override fun sendPassResetEmail(email: String, onResult: (Exception?) -> Unit) {
        Firebase.auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task: Task<Void?> ->
                task.exception?.let {
                    onResult(task.exception)
                    return@addOnCompleteListener
                }
                onResult(null)
            }
    }

    override fun sendVerificationEmail(
        user: FirebaseUser,
        onResult: (Exception?) -> Unit
    ) {
        user.sendEmailVerification()
            .addOnCompleteListener { task: Task<Void?> ->
                task.exception?.let {
                    onResult(task.exception)
                    return@addOnCompleteListener
                }
                onResult(null)
            }
    }

    override fun updateUserOnDb(user: FirebaseUser, onResult: (Exception?) -> Unit) {
        val userMap = hashMapOf(
            "UName" to user.displayName,
            "profileUrl" to user.photoUrl.toString(),
            "email" to user.email
        )
        Firebase.database.getReference("users/${user.uid}").updateChildren(userMap.toMap())
            .addOnCompleteListener { task->
                task.exception?.let{
                    onResult(it)
                    return@addOnCompleteListener
                }
                onResult(null)
            }
    }

}