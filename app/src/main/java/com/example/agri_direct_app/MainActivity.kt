package com.example.agri_direct_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import com.example.agri_direct_app.databinding.ActivityMainBinding
import com.example.agri_direct_app.model.User
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import org.mindrot.jbcrypt.BCrypt
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var verificationIdReceived: String? = null
    private lateinit var otpTimer: CountDownTimer
    private val OTP_TIMEOUT = 30000L

    val mauth=FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)




        binding.signUpBtn.isEnabled=false
        binding.sendOtpBtn.isEnabled = true


        binding.sendOtpBtn.setOnClickListener {
            //check whether user have entered the phone number or not
            val phoneNumber = binding.phoneNumber.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                binding.phoneNumber.error = "Please enter a phone number"
                return@setOnClickListener
            }

            // Send OTP to phone number
            sendOtp(phoneNumber)
        }

        binding.signUpBtn.setOnClickListener {
            val otp = binding.otp.text.toString()
            if (otp.isNotEmpty()) {
                verifyOtp(otp)
            } else {
                Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendOtp(phoneNumber:String) {

//        val phonenumberwithcountrycode="+91$phoneNumber"
//        PhoneAuthProvider.getInstance().verifyPhoneNumber(
//            phonenumberwithcountrycode,
//            60,
//            java.util.concurrent.TimeUnit.SECONDS,
//            this,
//            object:PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
//                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
//
//                    Toast.makeText(this@MainActivity,"OTP Verified!",Toast.LENGTH_SHORT).show()
//                    binding.signUpBtn.isEnabled=true
//                }
//
//                override fun onVerificationFailed(p0: FirebaseException) {
//
//                    Toast.makeText(this@MainActivity, "OTP Verification Failed"+p0.message, Toast.LENGTH_SHORT).show()
//                    Log.d("Error",p0.message!!)
//                }
//
//                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
//                    verificationIdReceived = verificationId
//                    Toast.makeText(this@MainActivity, "OTP Sent", Toast.LENGTH_SHORT).show()
//                    binding.sendOtpBtn.isEnabled=false
//                    startOtpTimer()
//                }
//
//            })


            val options = PhoneAuthOptions.newBuilder(mauth)
                .setPhoneNumber("+91$phoneNumber")       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS)  // Timeout duration
                .setActivity(this)                  // Activity (for callback binding)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // OTP is automatically detected and Firebase signs the user in
                       mauth.signInWithCredential(credential)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // OTP is automatically verified, enable sign up
                                    binding.signUpBtn.isEnabled = true
                                    Toast.makeText(this@MainActivity, "OTP automatically verified", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@MainActivity, "OTP verification failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Toast.makeText(this@MainActivity, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.d("error",e.message!!)
                    }

                    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                        // Store the verification ID to use it later for verifying the OTP
                        verificationIdReceived = verificationId
                        Toast.makeText(this@MainActivity, "OTP sent successfully", Toast.LENGTH_SHORT).show()

                        // Disable the send OTP button until timeout
                        binding.sendOtpBtn.isEnabled = false

                        // Start the timer to allow resend after 30 seconds
                        startOtpTimer()
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }


    private fun verifyOtp(enteredOtp:String){

        val credential = PhoneAuthProvider.getCredential(verificationIdReceived!!, enteredOtp)
        Firebase.auth.signInWithCredential(credential)
            .addOnCompleteListener(this){task->
                if(task.isSuccessful){
                   checkphonenumberandusername()

                }
                else{
                    Toast.makeText(this@MainActivity,"Incorrect OTP",Toast.LENGTH_SHORT).show()
                }
            }

    }

    private fun checkphonenumberandusername(){
        val phoneNumber = binding.phoneNumber.text.toString().trim()
        val username = binding.username.text.toString().trim()

        Firebase.database.reference.child("Users").orderByChild("phoneNumber").equalTo(phoneNumber)
            .addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
                        // Phone number already taken
                        Toast.makeText(this@MainActivity, "Account already exists for this number", Toast.LENGTH_SHORT).show()
                    }
                    else{
                        // Phone number is available, check username
                        checkUsernameAvailability(username)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                    Toast.makeText(this@MainActivity, "Error checking phone number: ${error.message}", Toast.LENGTH_SHORT).show()
                }

            })
    }

    private fun checkUsernameAvailability(username: String) {


        // Check if username already exists
        Firebase.database.reference.child("Users").orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Username already taken
                        Toast.makeText(this@MainActivity, "Username is already taken", Toast.LENGTH_SHORT).show()
                    } else {
                        // Username is available, proceed with account creation
                        createUserAccount()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                    Toast.makeText(this@MainActivity, "Error checking username: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun createUserAccount() {
        val username = binding.username.text.toString().trim()
        val name = binding.name.text.toString().trim()
        val phoneNumber = binding.phoneNumber.text.toString().trim()
        val password = binding.password.text.toString().trim()

        // Hash the password
        //val hashedPassword = hashPassword(password)



        // Create a new user object
        val user = User(
            username = username,
            name = name,
            phoneNumber = phoneNumber,
            password = hashPassword(password)
        )

        // Store user data under the user's UID in the Realtime Database
        Firebase.database.reference.child("Users").child(Firebase.auth.currentUser!!.uid).setValue(user)
            .addOnCompleteListener{task->
                if(task.isSuccessful){
                    // Successfully created user account
                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java)) // Go to HomeActivity
                    finish() // Close the SignUpActivity
                }
                else{
                    Toast.makeText(this, "Failed to create account", Toast.LENGTH_SHORT).show()
                }

            }


    }

    // Function to hash the password using BCrypt
    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt()) // Hash the password
    }

    // Timer to handle OTP validity period
    private fun startOtpTimer() {
        otpTimer = object : CountDownTimer(OTP_TIMEOUT, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update UI with remaining time (optional)
                // Example: update a TextView with remaining time
            }

            override fun onFinish() {
                // Re-enable Send OTP button after 30 seconds
                Toast.makeText(this@MainActivity,"You can now request for new OTP",Toast.LENGTH_SHORT).show()
                binding.sendOtpBtn.isEnabled = true

            }
        }
        otpTimer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        otpTimer.cancel()
    }


    override fun onStart() {
        super.onStart()

        if (Firebase.auth.currentUser != null) {
            startActivity(Intent(this@MainActivity, HomeActivity::class.java))
            finish()
        }
    }


}