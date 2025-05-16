package com.example.finalyearprojectwithfirebase.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.finalyearprojectwithfirebase.MainActivity
import com.example.finalyearprojectwithfirebase.databinding.ActivitySplashBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Delay and redirect logic
        Handler(Looper.getMainLooper()).postDelayed({
            if (Firebase.auth.currentUser != null) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }, 3000)
    }
}
