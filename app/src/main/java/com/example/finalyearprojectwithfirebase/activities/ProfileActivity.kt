package com.example.finalyearprojectwithfirebase.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.finalyearprojectwithfirebase.R
import com.example.finalyearprojectwithfirebase.databinding.ActivityProfileBinding
import com.example.finalyearprojectwithfirebase.adapters.ProfileProductAdapter
import com.example.finalyearprojectwithfirebase.model.ProfileProduct
import com.google.firebase.Firebase
import com.google.firebase.database.*
import kotlin.properties.Delegates

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var productAdapter: ProfileProductAdapter
    private val productList: MutableList<ProfileProduct> = mutableListOf()

    private val database = FirebaseDatabase.getInstance().reference
    private val usersRef = database.child("Users")
    private lateinit var userId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get user ID from intent
        userId = intent.getStringExtra("USER_ID") ?: ""

        binding.progressBar.visibility = View.VISIBLE


        val token = intent.getStringExtra("token")

        if (token.toBoolean()) {
            binding.callButton.isEnabled = true
            binding.messageButton.isEnabled = true
        }


        // Set up RecyclerView
        productAdapter = ProfileProductAdapter(productList,
            this@ProfileActivity,
            onBidClick = { fetchUserProducts{} })
        binding.productsRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.productsRecyclerView.adapter = productAdapter

        // 👇 Combine both async fetches before hiding progress bar
        var tasksRemaining = 2
        fun taskDone() {
            tasksRemaining--
            if (tasksRemaining == 0) {
                binding.progressBar.visibility = View.GONE
            }
        }

        fetchUserDetails {
            taskDone()
        }

        fetchUserProducts {
            taskDone()
        }

}

    private fun fetchUserDetails(onComplete: () -> Unit) {
        var tasksRemaining = 3

        fun taskDone() {
            tasksRemaining--
            if (tasksRemaining == 0) {
                onComplete()
            }
        }

        // --- 1. Fetch user info ---
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                val address = snapshot.child("localeaddress").getValue(String::class.java)
                val phone = snapshot.child("phonenumber").getValue(String::class.java)
                val profilepic = snapshot.child("profilepic").getValue(String::class.java)

                binding.username.text = username ?: "N/A"
                binding.email.text = email ?: "N/A"
                binding.address.text = address ?: "N/A"

                if (profilepic?.isNotEmpty() == true) {
                    Glide.with(this@ProfileActivity)
                        .load(profilepic)
                        .into(binding.profileImageView)
                } else {
                    binding.profileImageView.setImageResource(R.drawable.profile)
                }

                if (!phone.isNullOrEmpty()) {
                    binding.callButton.setOnClickListener {
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        startActivity(callIntent)
                    }
                    binding.messageButton.setOnClickListener {
                        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
                        startActivity(smsIntent)
                    }
                } else {
                    binding.callButton.isEnabled = false
                    binding.messageButton.isEnabled = false
                }

                taskDone() // ✅ user info task done
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, "Failed to fetch user details", Toast.LENGTH_SHORT).show()
                taskDone() // still call to avoid hanging the ProgressBar
            }
        })

        // --- 2. Transactions task ---
        val transactionRef = database.child("Transactions").child(userId)
        transactionRef.get().addOnSuccessListener { snapshot ->
            var count = 0
            for (child in snapshot.children) {
                val status = child.child("status").getValue(String::class.java)
                if (status == "Successful") {
                    count++
                }
            }

            binding.succesfultransaction.text = "Successful Sales $count"
            taskDone() // ✅ transactions done
        }.addOnFailureListener {
            Toast.makeText(this@ProfileActivity, "Failed to retrieve transactions", Toast.LENGTH_SHORT).show()
            taskDone() // still call to avoid hanging the ProgressBar
        }

        // --- 3. Ratings task ---
        calculateAverageRating(userId) { averageRating ->
            if (averageRating > 0) {
                binding.rating.text = "Rating: %.1f".format(averageRating)
            } else {
                binding.rating.text = "No Ratings"
            }
            taskDone() // ✅ rating done
        }
    }

    private fun calculateAverageRating(userId: String, onResult: (Float) -> Unit) {
        val ref = database.child("Transactions").child(userId)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalRating = 0f
                var count = 0

                for (transactionSnapshot in snapshot.children) {
                    val rating = transactionSnapshot.child("rating").getValue(Float::class.java)
                    if (rating != null) {
                        totalRating += rating
                        count++
                    }
                }

                if (count > 0) {
                    val average = totalRating / count
                    onResult(average)
                } else {
                    onResult(0f) // No ratings found
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to fetch ratings: ${error.message}")
                onResult(0f)
            }
        })
    }
    private fun fetchUserProducts(onComplete: () -> Unit) {
        database
            .child("products")
            .child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    productList.clear()
                    var itemsProcessed = 0
                    val totalItems = snapshot.children.count()

                    if (totalItems == 0) {
                        productList.clear()
                        productAdapter.notifyDataSetChanged()
                        onComplete()
                        return
                    }

                    for (productSnapshot in snapshot.children) {
                        val productId = productSnapshot.key
                        val product = productSnapshot.getValue(ProfileProduct::class.java)

                        if (product != null && productId != null) {
                            database
                                .child("Bids")
                                .child(userId)
                                .child(productId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(bidSnapshot: DataSnapshot) {
                                        val bidValue = bidSnapshot.child("currenthighestbid").value?.toString()
                                        val currenthighestbid = if (!bidValue.isNullOrEmpty()) bidValue.toInt() else 0

                                        productList.add(
                                            ProfileProduct(
                                                userId,
                                                productId,
                                                product.name,
                                                product.variety,
                                                product.unit,
                                                product.quantity,
                                                product.price,
                                                product.image,
                                                product.minimumbidquantity,
                                                product.isbiddingenabled,
                                                currenthighestbid
                                            )
                                        )

                                        itemsProcessed++
                                        if (itemsProcessed == totalItems) {
                                            productAdapter.notifyDataSetChanged()
                                            onComplete()
                                        }

                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Toast.makeText(this@ProfileActivity, "Failed to fetch bid", Toast.LENGTH_SHORT).show()
                                        onComplete()
                                    }

                                })
                        } else {
                            itemsProcessed++
                            if (itemsProcessed == totalItems) {
                                productAdapter.notifyDataSetChanged()
                                onComplete()

                            }
                        }
                    }
                }


                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProfileActivity, "Failed to fetch products", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            })
    }

}