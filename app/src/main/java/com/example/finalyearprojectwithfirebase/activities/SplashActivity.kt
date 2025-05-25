
        package com.example.finalyearprojectwithfirebase.activities

        import android.content.Intent
        import android.os.Bundle
        import androidx.appcompat.app.AppCompatActivity
        import androidx.lifecycle.lifecycleScope
        import com.example.finalyearprojectwithfirebase.MainActivity
        import com.example.finalyearprojectwithfirebase.databinding.ActivitySplashBinding
        import com.google.firebase.Firebase
        import com.google.firebase.auth.auth
        import kotlinx.coroutines.delay
        import kotlinx.coroutines.launch

        class SplashActivity : AppCompatActivity() {

            private lateinit var binding: ActivitySplashBinding
            private val SPLASH_DELAY: Long = 3000
            private var hasNavigated = false

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)

                binding = ActivitySplashBinding.inflate(layoutInflater)
                setContentView(binding.root)

                val versionName = packageManager.getPackageInfo(packageName, 0).versionName
                binding.appVersion.text = "v${versionName}"

                lifecycleScope.launch {

                    delay(SPLASH_DELAY)
                    val currentUser = Firebase.auth.currentUser

                    if (!hasNavigated) {
                        hasNavigated = true

                        val intent = if (currentUser != null) {
                            Intent(this@SplashActivity, MainActivity::class.java)
                        }
                        else {
                            Intent(this@SplashActivity, LoginActivity::class.java)
                        }

                        startActivity(intent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()

                    }
                }
            }
        }




