package com.example.agri_direct_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.agri_direct_app.databinding.ActivityResetPasswordBinding
import com.example.agri_direct_app.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {
    private val binding: ActivitySignInBinding by lazy{
        ActivitySignInBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.signin.setOnClickListener{
            startActivity(Intent(this@SignInActivity,HomeActivity::class.java))
            finish()
        }
        binding.forgotpassbtn.setOnClickListener{
            startActivity(Intent(this@SignInActivity,ResetPassword::class.java))
            finish()
        }
    }
}