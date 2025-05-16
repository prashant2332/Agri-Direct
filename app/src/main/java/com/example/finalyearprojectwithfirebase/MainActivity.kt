package com.example.finalyearprojectwithfirebase



import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.finalyearprojectwithfirebase.activities.LoginActivity
import com.example.finalyearprojectwithfirebase.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy{
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        drawerLayout=binding.drawerLayout
        navigationView=binding.navigationView
        toolbar=findViewById(R.id.toolbar)

        //step 1
        setSupportActionBar(toolbar)

        //step 2
        val toggle= ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.OpenDrawer,R.string.CloseDrawer)

        val drawable = toggle.drawerArrowDrawable
        drawable.color = ContextCompat.getColor(this, R.color.white)

        //step 3
        drawerLayout.addDrawerListener(toggle)

        //step 4
        toggle.syncState()



        val navController = findNavController(R.id.fragmentcontainer)
        val nav = binding.navigationView
        nav.setupWithNavController(navController)

        nav.setNavigationItemSelectedListener { item ->
            // Clear all selections and highlight the selected item
            for (i in 0 until nav.menu.size()) {
                nav.menu.getItem(i).isChecked = false
            }
            item.isChecked = true

            val currentDestination = navController.currentDestination?.id

            when (item.itemId) {
                R.id.home -> {

                    if (currentDestination != R.id.home) {
                        navController.navigate(R.id.home)
                    }
                }
                R.id.profile -> {
                    if (currentDestination != R.id.profile) {
                        navController.navigate(R.id.profile)
                    }
                }
                R.id.sell -> {
                    if (currentDestination != R.id.sell) {
                        navController.navigate(R.id.sell)
                    }
                }
                R.id.buy -> {
                    if (currentDestination != R.id.buy) {
                        navController.navigate(R.id.buy)
                    }
                }
                R.id.mandibhav -> {
                    if (currentDestination != R.id.mandibhav) {
                        navController.navigate(R.id.mandibhav)
                    }
                }
                R.id.yourbids -> {
                    if (currentDestination != R.id.yourbids) {
                        navController.navigate(R.id.yourbids)
                    }
                }
                R.id.logout -> {
                    Firebase.auth.signOut()
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }




        onBackPressedDispatcher.addCallback(this,object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                if(drawerLayout.isDrawerOpen(GravityCompat.START)){
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
               else{
                    val navcontroller = findNavController(R.id.fragmentcontainer)
                    if (!navcontroller.popBackStack()) {
                        finish()
                    }
                }
            }

        })

    }
}