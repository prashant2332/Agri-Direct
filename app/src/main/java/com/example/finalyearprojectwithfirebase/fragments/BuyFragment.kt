package com.example.finalyearprojectwithfirebase.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalyearprojectwithfirebase.adapters.CartAdapter
import com.example.finalyearprojectwithfirebase.databinding.FragmentBuyBinding
import com.example.finalyearprojectwithfirebase.model.CartProduct
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.FirebaseDatabase

class BuyFragment : Fragment() {

    private lateinit var binding: FragmentBuyBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var cartAdapter: CartAdapter
    private val cartList = mutableListOf<CartProduct>()


    private val databaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Initialize view binding
        binding = FragmentBuyBinding.inflate(inflater, container, false)



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize RecyclerView and adapter
        recyclerView = binding.cartRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        cartAdapter = CartAdapter(cartList, requireContext()) { cartProduct ->
            removeFromCart(cartProduct)
        }
        recyclerView.adapter = cartAdapter

        // Load cart products
        loadCartProducts()
    }

    private fun loadCartProducts() {
        val currentUserId = Firebase.auth.currentUser?.uid
        if (currentUserId != null) {
            // Reference to Cart node under current user
            databaseReference.child("Cart").child(currentUserId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        cartList.clear() // Clear previous cart data

                        for (cartItem in snapshot.children) {
                            val cartId = cartItem.key
                            val sellerId = cartItem.child("sellerId").getValue(String::class.java)
                            val productId = cartItem.child("productId").getValue(String::class.java)


                            if (!sellerId.isNullOrEmpty() && !productId.isNullOrEmpty()) {
                                // Get product from Products node
                                databaseReference.child("products").child(sellerId).child(productId)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(productSnap: DataSnapshot) {
                                            val product = productSnap.getValue(CartProduct::class.java)
                                            if (product != null && cartId != null) {
                                                product.cartid = cartId
                                                product.productId = productId
                                                product.sellerId = sellerId
                                                cartList.add(product)
                                                cartAdapter.notifyDataSetChanged()
                                            }

                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            showErrorToast(error.message)
                                        }
                                    })
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showErrorToast(error.message)
                    }
                })
        } else {
            showErrorToast("User not logged in")
        }
    }


    private fun removeFromCart(cartProduct: CartProduct) {
        val currentUserId = Firebase.auth.currentUser?.uid
        if (currentUserId != null) {
            // Remove product from the user's cart in Firebase
            databaseReference.child("Cart")
                .child(currentUserId)
                .child(cartProduct.cartid)
                .removeValue()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Product removed from cart", Toast.LENGTH_SHORT).show()
                    cartList.remove(cartProduct)
                    cartAdapter.notifyDataSetChanged() // Update RecyclerView
                }
                .addOnFailureListener {
                    showErrorToast("Failed to remove product from cart")
                }
        } else {
            showErrorToast("User not logged in")
        }
    }

    // Helper method to show error messages in a Toast
    private fun showErrorToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
