package com.example.finalyearprojectwithfirebase.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finalyearprojectwithfirebase.MainActivity
import com.example.finalyearprojectwithfirebase.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.loginEmail.error = "Enter valid email"
                binding.loginEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty() || password.length < 6) {
                binding.loginPassword.error = "Enter valid password"
                binding.loginPassword.requestFocus()
                return@setOnClickListener
            }


                    Firebase.auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { loginTask ->
                            if (loginTask.isSuccessful) {
                                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Login failed: ${loginTask.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                        }
        }

        binding.registerRedirect.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }
}
