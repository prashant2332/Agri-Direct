package com.example.finalyearprojectwithfirebase.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finalyearprojectwithfirebase.MainActivity
import com.example.finalyearprojectwithfirebase.R
import com.example.finalyearprojectwithfirebase.databinding.ActivityLoginBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.registerRedirect.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        binding.forgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun loginUser() {

        val email = binding.loginEmail.text.toString().trim()
        val password = binding.loginPassword.text.toString().trim()

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null

        if (!isValidEmail(email)) {
            binding.emailInputLayout.error = "Enter a valid email"
            binding.loginEmail.requestFocus()
            return
        }

        if (password.length < 6) {
            binding.passwordInputLayout.error = "Password must be at least 6 characters"
            binding.loginPassword.requestFocus()
            return
        }

        binding.btnLogin.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch{
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT)
                    .show()
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity,e.message, Toast.LENGTH_LONG)
                    .show()
            } finally {
                binding.btnLogin.isEnabled = true
                binding.progressBar.visibility=View.GONE
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.forgot_password_dialog, null)
        val emailEditText = dialogView.findViewById<TextInputEditText>(R.id.forgot_email_edit_text)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Send", null)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            val sendButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            sendButton.setOnClickListener {
                val email = emailEditText?.text?.toString()?.trim()
                if (email.isNullOrEmpty()) {
                    emailEditText?.error = "Please enter your email"
                } else if (!isValidEmail(email)) {
                    emailEditText.error = "Enter a valid email"
                } else {
                    dialog.dismiss()
                    sendPasswordResetEmail(email)
                }
            }
        }

        dialog.show()
    }

    private fun sendPasswordResetEmail(email: String) {
        lifecycleScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                AlertDialog.Builder(this@LoginActivity)
                    .setTitle("Email Sent")
                    .setMessage("A password reset email has been sent to $email.")
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
