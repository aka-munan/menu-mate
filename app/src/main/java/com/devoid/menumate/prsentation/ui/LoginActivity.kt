package com.devoid.menumate.prsentation.ui

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.TransitionManager
import com.devoid.menumate.R
import com.devoid.menumate.databinding.LoginMainBinding
import com.devoid.menumate.prsentation.state.LoginUiState
import com.devoid.menumate.prsentation.viewmodel.LoginViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class LoginActivity : AppCompatActivity(){
    private lateinit var binding: LoginMainBinding
    private val viewModel by viewModels<LoginViewModel>()
    private val TAG = LoginActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observeViewModel()
        setUpButtons()
    }


    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Failure -> {
                            setButtonsEnabled(true)
                            parseException(state.e)
                        }

                        LoginUiState.Idle -> {}

                        LoginUiState.ResetPassEmailSent -> {
                            Toast.makeText(this@LoginActivity, "Password reset email sent to your Email.", Toast.LENGTH_SHORT).show()
                        }

                        LoginUiState.Success -> {
                            setButtonsEnabled(true)
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun parseException(e: Exception) {
        if (e is GetCredentialCancellationException)
            return
        var errMsg = e.message
        when (e) {
            is FirebaseAuthWeakPasswordException ->
                errMsg = "Weak passward"

            is FirebaseAuthUserCollisionException ->
                errMsg = "User already exists"

            is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException ->
                errMsg = "Invalid Credentials entered"

            else -> {
                Log.e(TAG, "onLogInFailure: ", e);
            }
        }
        Toast.makeText(
            this,
            "Authentication failed : $errMsg",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setUpButtons() {
        binding.signupTextview.setOnClickListener {
            binding.signupTextview.text =
                if (binding.usernameLayout.isVisible) "Sign up" else "LogIn"
            binding.singupHint.text =
                if (binding.usernameLayout.isVisible) "Don't have an account?" else "Already have an account?"
            TransitionManager.beginDelayedTransition(binding.root)
            binding.usernameLayout.visibility =
                if (binding.usernameLayout.isVisible) View.GONE else View.VISIBLE
        }
        binding.btnLogin.setOnClickListener {
            binding.apply {
                val uName = username.text.toString()
                val email = email.text.toString()
                val password = password.text.toString()
                if (usernameLayout.visibility == View.VISIBLE) {//sign up
                    if (isNotFormat(chkUser = true, chkPas = true))
                        return@setOnClickListener
                    viewModel.signUp(uName, email, password)
                } else {
                    if (isNotFormat(chkUser = false, chkPas = true))
                        return@setOnClickListener
                    viewModel.login(uName, password)
                }
            }
        }
        binding.googleBtn.setOnClickListener {
            it.isEnabled = false
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    viewModel.loginGoogle(this@LoginActivity)
                }
            }
        }
        binding.twitterBtn.setOnClickListener {
            viewModel.loginTwitter(this@LoginActivity)
        }
        binding.forgotPassTxt.setOnClickListener {
            if (isNotFormat(chkUser = false, chkPas = false))
                return@setOnClickListener
            showPassResetDialog()

        }
    }

    private fun showPassResetDialog() {
        val dialog: AlertDialog = MaterialAlertDialogBuilder(this@LoginActivity)
            .setCancelable(true)
            .setTitle("Forgot Password!")
            .setMessage(getString(R.string.forgot_password_message))
            .setPositiveButton("Ok") { _: DialogInterface?, _: Int ->
            }
            .setNegativeButton(
                "Send"
            ) { _, _ ->
                viewModel.sendPasswordResetEmail(binding.email.text.toString())
            }.create()
        dialog.show()
        val timePassed: Long =
            System.currentTimeMillis() - viewModel.lastPassResetEmailSent
        if (1000 * 60 > timePassed /*less then 1 minute passed*/) {
            val negButton: Button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negButton.isEnabled = false
            negButton.text = "ReSend in ${(60000 - timePassed) / 1000} s"
        } else
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isVisible = false
    }

    private fun isNotFormat(chkUser: Boolean, chkPas: Boolean): Boolean {
        // check if format is correct
        //return true if incorrect format and sets error respective edit texts
        if (chkUser) {
            //check username format
            if (binding.username.text.toString().trim().isEmpty()) {
                binding.username.error = "UserName can't be empty"
                return true
            }
            if (binding.username.text.toString().replace("[^\\w\\s]", "").isEmpty()) {
                binding.username.error = "UserName mustContain a letter"
                return true
            }
        }
        //check email format
        if (binding.email.getText().toString().trim().isEmpty()) {
            binding.email.error = "email can't be empty"
            return true
        }
        if (binding.email.text.toString().replace("[^\\w\\s]", "").isEmpty()
            || !binding.email.text.toString().trim().contains("@")
        ) {
            binding.email.error = "Enter a Valid EMAIL"
            return true
        }
        if (chkPas) {
            //check password format
            if (binding.password.text.toString().trim().length <= 8) {
                binding.password.error = "Password must be more then 8 characters"
                return true
            }
            if (binding.password.getText().toString().replace("[^\\w\\s]", "").isEmpty()) {
                binding.password.error = "Password must contain a letter"
                return true
            }
        }
        //remove errors from editText
        binding.email.error = null
        binding.password.error = null
        binding.username.error = null
        return false
    }



    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnLogin.setEnabled(enabled)
        binding.googleBtn.setEnabled(enabled)
        binding.twitterBtn.setEnabled(enabled)
    }


    override fun onStart() {
        super.onStart()
        if (Firebase.auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}