package com.devoid.menumate.domain.repository

import android.app.Activity
import android.content.Context
import androidx.credentials.Credential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth

interface LoginRepository {
    fun loginGoogle(credential: Credential,onResult:(Exception?,FirebaseUser?)->Unit)
    fun loginEmailPassword(email:String,password:String,onResult:(Exception?,FirebaseUser?)->Unit)
    fun signUp(uName:String,email:String,password:String,onResult:(Exception?,FirebaseUser?)->Unit)
    fun sendPassResetEmail(email: String,onResult: (Exception?) -> Unit)
    fun sendVerificationEmail(user: FirebaseUser,onResult: (Exception?) -> Unit)
    fun updateUserOnDb(user: FirebaseUser,onResult: (Exception?) -> Unit)
}