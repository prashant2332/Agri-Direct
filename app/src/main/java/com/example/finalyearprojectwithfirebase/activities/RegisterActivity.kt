package com.example.finalyearprojectwithfirebase.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.example.finalyearprojectwithfirebase.MainActivity
import com.example.finalyearprojectwithfirebase.R
import com.example.finalyearprojectwithfirebase.databinding.ActivityRegisterBinding
import com.example.finalyearprojectwithfirebase.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val email = binding.registerEmail.text.toString().trim()
            val password = binding.registerPassword.text.toString().trim()
            val confirmPassword = binding.registerConfirmPassword.text.toString().trim()
            val phoneNumber = binding.registerPhonenumber.text.toString().trim()
            val username = binding.registerUsername.text.toString().trim()
            val state = binding.registerState.text.toString().trim()
            val district = binding.registerDistrict.text.toString().trim()
            val localAddress = binding.registerLocaleaddress.text.toString().trim()


            // Input validations
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.registerEmail.error = "Enter a valid email"
                binding.registerEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.registerPassword.error = "Password must be at least 6 characters"
                binding.registerPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.registerConfirmPassword.error = "Password does not match"
                binding.registerConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (phoneNumber.isEmpty()) {
                binding.registerPhonenumber.error = "Phone number required"
                binding.registerPhonenumber.requestFocus()
                return@setOnClickListener
            }

            if (username.isEmpty()) {
                binding.registerUsername.error = "Username required"
                binding.registerUsername.requestFocus()
                return@setOnClickListener
            }

            if (state.isEmpty()) {
                binding.registerState.error = "State required"
                binding.registerState.requestFocus()
                return@setOnClickListener
            }

            if (district.isEmpty()) {
                binding.registerDistrict.error = "District required"
                binding.registerDistrict.requestFocus()
                return@setOnClickListener
            }

            if (localAddress.isEmpty()) {
                binding.registerLocaleaddress.error = "Address required"
                binding.registerLocaleaddress.requestFocus()
                return@setOnClickListener
            }

            // Check if email already exists
            Firebase.auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { checkTask ->
                val signInMethods = checkTask.result?.signInMethods
                if (!signInMethods.isNullOrEmpty()) {
                    Toast.makeText(this, "This email is already registered", Toast.LENGTH_LONG)
                        .show()
                } else {
                    // Create account
                    if (!checkregistryofnumber(phoneNumber)) {
                        Firebase.auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        this,
                                        "Account created successfully",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    val user = User(
                                        email,
                                        phoneNumber,
                                        username,
                                        state,
                                        district,
                                        localAddress
                                    )
                                    Firebase.database.reference.child("Users")
                                        .child(Firebase.auth.currentUser!!.uid)
                                        .setValue(user)
                                        .addOnSuccessListener {
                                            startActivity(Intent(this, MainActivity::class.java))
                                            finish()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                this,
                                                "Failed to save user: ${it.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            it.message?.let { it1 -> Log.d("error", it1) }
                                        }

                                } else {
                                    Toast.makeText(
                                        this,
                                        "Registration failed: ${task.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }


                    }
                    else{
                        Toast.makeText(this@RegisterActivity,"Mobile Number is already Registered",Toast.LENGTH_SHORT).show()
                    }
                }
            }

            binding.loginRedirect.setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun checkregistryofnumber(phonenumber:String):Boolean{
        var status =false

        Firebase.database.reference.child("Users")
        .orderByChild("phonenumber").equalTo(phonenumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Phone number exists
                        status=true
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                   Toast.makeText(this@RegisterActivity,error.message,Toast.LENGTH_SHORT).show()
                }
            })


        return status
    }
}
