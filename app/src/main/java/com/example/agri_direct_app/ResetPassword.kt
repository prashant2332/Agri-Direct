package com.example.agri_direct_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.agri_direct_app.databinding.ActivityResetPasswordBinding

class ResetPassword : AppCompatActivity() {

    private val binding:ActivityResetPasswordBinding by lazy{
        ActivityResetPasswordBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.resetotpbtn.setOnClickListener{
            binding.resetpasslinearlayout.visibility=View.VISIBLE
        }
        binding.resetbtn.setOnClickListener{
            binding.resetpasslinearlayout.visibility=View.GONE
            startActivity(Intent(this@ResetPassword,HomeActivity::class.java))
            finish()
        }

    }
}