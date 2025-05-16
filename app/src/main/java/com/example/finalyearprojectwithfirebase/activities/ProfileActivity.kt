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

    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("Users")
    private lateinit var userId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get user ID from intent
        userId = intent.getStringExtra("USER_ID") ?: ""

        val token=intent.getStringExtra("token")

        if(token.toBoolean()){
            binding.callButton.isEnabled=true
            binding.messageButton.isEnabled=true
        }


        // Set up RecyclerView
        productAdapter = ProfileProductAdapter(productList,
            this@ProfileActivity,
            onBidClick = { fetchUserProducts() })
        binding.productsRecyclerView.layoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        binding.productsRecyclerView.adapter = productAdapter

        fetchUserDetails()
        fetchUserProducts()
    }

    private fun fetchUserDetails() {
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                val address = snapshot.child("localeaddress").getValue(String::class.java)
                val phone = snapshot.child("phonenumber").getValue(String::class.java)
                val profilepic=snapshot.child("profilepic").getValue(String::class.java)

                binding.username.text = username ?: "N/A"
                binding.email.text = email ?: "N/A"
                binding.address.text = address ?: "N/A"

                if(profilepic?.isNotEmpty() == true) {
                    Glide.with(this@ProfileActivity)
                        .load(profilepic)
                        .into(binding.profileImageView)
                }
                else{
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
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, "Failed to fetch user details", Toast.LENGTH_SHORT).show()
            }
        })

        val transactionRef = Firebase.database.reference.child("Transactions").child(userId)
        transactionRef.get().addOnSuccessListener { snapshot ->
            var count = 0
            for (child in snapshot.children) {
                val status = child.child("status").getValue(String::class.java)
                if (status == "Successful") {
                    count++
                }
            }

            binding.succesfultransaction.text="Succesful Sales ${count}"
        }.addOnFailureListener {
            Toast.makeText(this@ProfileActivity, "Failed to retrieve transactions", Toast.LENGTH_SHORT).show()
        }


        calculateAverageRating(userId) { averageRating ->
            if(averageRating>0) {
                binding.rating.text = "Rating: %.1f".format(averageRating)
            }
            else{
                binding.rating.text = "No Ratings"
            }
        }

    }

    fun calculateAverageRating(userId: String, onResult: (Float) -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("Transactions").child(userId)

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



    private fun fetchUserProducts() {
        FirebaseDatabase.getInstance().reference
            .child("products")
            .child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    productList.clear()

                    for (productSnapshot in snapshot.children) {
                        val productId = productSnapshot.key
                        val product = productSnapshot.getValue(ProfileProduct::class.java)

                        if (product != null && productId != null) {
                            Firebase.database.reference
                                .child("Bids")
                                .child(userId)
                                .child(productId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(bidSnapshot: DataSnapshot) {
                                        val bidValue = bidSnapshot.child("currenthighestbid").value?.toString()
                                        val currenthighestbid = if (!bidValue.isNullOrEmpty()) bidValue.toInt() else 0

                                        // Add product only after bid is loaded
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

                                        productAdapter.notifyDataSetChanged()
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Toast.makeText(this@ProfileActivity, "Failed to fetch bid", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProfileActivity, "Failed to fetch products", Toast.LENGTH_SHORT).show()
                }
            })
    }

}