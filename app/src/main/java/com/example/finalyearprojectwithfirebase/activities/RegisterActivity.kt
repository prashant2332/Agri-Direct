package com.example.finalyearprojectwithfirebase.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.finalyearprojectwithfirebase.MainActivity
import com.example.finalyearprojectwithfirebase.R
import com.example.finalyearprojectwithfirebase.databinding.ActivityRegisterBinding
import com.example.finalyearprojectwithfirebase.model.CustomToast
import com.example.finalyearprojectwithfirebase.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class RegisterActivity : AppCompatActivity() {

    private val databaseReference = FirebaseDatabase.getInstance().reference
    private val auth=FirebaseAuth.getInstance()
    private val userid=auth.currentUser?.uid

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var statename: AutoCompleteTextView
    private lateinit var district: AutoCompleteTextView
    private lateinit var statesAndDistricts: Map<String, List<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        statename = binding.registerState
        district = binding.registerDistrict
        district.isEnabled = false
        district.alpha = 0.5f

        // Load state and district data from assets
        val inputStream = assets.open("statesanddistricts.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<Map<String, List<String>>>() {}.type
        statesAndDistricts = Gson().fromJson(reader, type)

        // Populate state dropdown
        val stateNames = statesAndDistricts.keys.toList()
        val stateAdapter = ArrayAdapter(this, R.layout.dropdownmenupopupitem, stateNames)
        statename.setAdapter(stateAdapter)

        statename.setOnItemClickListener { _, _, position, _ ->
            val selectedState = stateNames[position]
            val districts = statesAndDistricts[selectedState] ?: emptyList()
            val districtAdapter = ArrayAdapter(this, R.layout.dropdownmenupopupitem, districts)
            district.setAdapter(districtAdapter)
            district.setText("")
            district.isEnabled = true
            district.alpha = 1f
        }
        statename.addTextChangedListener {
            if (it.isNullOrEmpty()) {
                district.setText("")
                district.isEnabled = false
                district.alpha = 0.5f
            }
        }

        binding.loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnRegister.setOnClickListener {

            binding.progressBar.visibility= View.VISIBLE

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
                binding.email.error = "Enter a valid email"
                binding.registerEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.password.error = "Password must be at least 6 characters"
                binding.registerPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.confirmpassword.error = "Password does not match"
                binding.registerConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (phoneNumber.isEmpty()) {
                binding.phone.error = "Phone number required"
                binding.registerPhonenumber.requestFocus()
                return@setOnClickListener
            }

            if (username.isEmpty()) {
                binding.name.error = "Username required"
                binding.registerUsername.requestFocus()
                return@setOnClickListener
            }

            if (state.isEmpty()) {
                binding.state.error = "State required"
                binding.registerState.requestFocus()
                return@setOnClickListener
            }

            if (district.isEmpty()) {
                binding.districtlayout.error = "District required"
                binding.registerDistrict.requestFocus()
                return@setOnClickListener
            }

            if (localAddress.isEmpty()) {
                binding.localeaddress.error = "Address required"
                binding.registerLocaleaddress.requestFocus()
                return@setOnClickListener
            }

            auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { checkTask ->
                val signInMethods = checkTask.result?.signInMethods

                if (!signInMethods.isNullOrEmpty()) {

                    CustomToast.show(this@RegisterActivity,"This email is already registered")
                } else {
                    checkRegistryOfNumber(phoneNumber) { numberExists ->
                        if (numberExists) {

                            CustomToast.show(this,"Mobile Number is already registered")
                        } else {
                            // Create user
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {

                                        CustomToast.show(this@RegisterActivity,"Account Created")
                                        val user = User(email, phoneNumber, username, state, district, localAddress)

                                        if (userid != null) {
                                            databaseReference.child("Users").child(userid).setValue(user)
                                                .addOnSuccessListener {
                                                    binding.progressBar.visibility=View.GONE
                                                    startActivity(Intent(this, MainActivity::class.java))
                                                    finish()
                                                }
                                                .addOnFailureListener {
                                                    binding.progressBar.visibility=View.GONE
                                                    CustomToast.show(this@RegisterActivity,it.message!!)
                                                }
                                        }
                                    } else {
                                        binding.progressBar.visibility=View.GONE
                                        CustomToast.show(this@RegisterActivity,task.exception?.message!!)
                                    }
                                }
                        }
                    }
                }
            }

        }
    }

    private fun checkRegistryOfNumber(phoneNumber: String, callback: (Boolean) -> Unit) {
        databaseReference.child("Users")
            .orderByChild("phonenumber").equalTo(phoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.exists())
                }
                override fun onCancelled(error: DatabaseError) {
                    CustomToast.show(this@RegisterActivity,error.message)
                    callback(false)
                }
            })
    }
}
