package com.example.finalyearprojectwithfirebase



import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
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
    private lateinit var toolbarTitleTextView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        drawerLayout=binding.drawerLayout
        navigationView=binding.navigationView
        toolbar=findViewById(R.id.toolbar)
        toolbarTitleTextView = toolbar.findViewById(R.id.toolbartitletextview)


        //step 1
        setSupportActionBar(toolbar)
        //step 2
        setupDrawerToggle()

        val navController = findNavController(R.id.fragmentcontainer)
        val nav = binding.navigationView
        nav.setupWithNavController(navController)

        nav.setNavigationItemSelectedListener { item ->

            for (i in 0 until nav.menu.size()) {
                nav.menu.getItem(i).isChecked = false
            }
            item.isChecked = true


            when (item.itemId) {
                R.id.home -> navigateIfNotCurrent(navController, R.id.home)
                R.id.profile -> navigateIfNotCurrent(navController, R.id.profile)
                R.id.sell -> navigateIfNotCurrent(navController, R.id.sell)
                R.id.wishlist -> navigateIfNotCurrent(navController, R.id.wishlist)
                R.id.mandibhav -> navigateIfNotCurrent(navController, R.id.mandibhav)
                R.id.yourbids -> navigateIfNotCurrent(navController, R.id.yourbids)
                R.id.logout -> {
                    Firebase.auth.signOut()
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        setupBackButtonHandler()
        // 🔥 Add this block to change toolbar title dynamically
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val title = when (destination.id) {
                R.id.home -> "Home"
                R.id.profile -> "Profile"
                R.id.sell -> "Product Stock"
                R.id.wishlist -> "Wishlist"
                R.id.mandibhav -> "Mandi Bhav"
                R.id.yourbids -> "Your Bids"
                else -> "Agridirect"
            }
            toolbarTitleTextView.text = title
        }

    }

    private fun navigateIfNotCurrent(navController: NavController, destinationId: Int) {
        if (navController.currentDestination?.id != destinationId) {
            navController.navigate(destinationId)
        }
    }

    private fun setupBackButtonHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    val navcontroller = findNavController(R.id.fragmentcontainer)
                    if (!navcontroller.popBackStack()) finish()
                }
            }
        })
    }

    private fun setupDrawerToggle() {
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.OpenDrawer, R.string.CloseDrawer)
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.white)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

}