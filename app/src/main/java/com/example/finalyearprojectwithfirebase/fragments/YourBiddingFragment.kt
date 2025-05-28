package com.example.finalyearprojectwithfirebase.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalyearprojectwithfirebase.adapters.BidAdapter
import com.example.finalyearprojectwithfirebase.databinding.FragmentYourBidsBinding
import com.example.finalyearprojectwithfirebase.model.BidProduct
import com.example.finalyearprojectwithfirebase.model.CustomToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class YourBiddingFragment : Fragment() {
    private lateinit var binding: FragmentYourBidsBinding
    private lateinit var bidrecyclerView: RecyclerView
    private lateinit var bidAdapter: BidAdapter
    private val bidList = mutableListOf<BidProduct>()

    private val databaseReference = FirebaseDatabase.getInstance().reference
    private val userid= FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentYourBidsBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bidrecyclerView = binding.bidRecyclerView
        bidrecyclerView.layoutManager = LinearLayoutManager(requireContext())
        bidAdapter = BidAdapter(bidList, requireContext()) { bidProduct ->
            removeFromBidSection(bidProduct)
        }
        bidrecyclerView.adapter = bidAdapter
        binding.progressBar.visibility=View.VISIBLE
        loadBiddedProducts()
    }

    private fun loadBiddedProducts() {
        if (userid!=null) {
            databaseReference.child("YourBids").child(userid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        bidList.clear() // Clear previous cart data
                        for (bidItem in snapshot.children) {
                            val bidId = bidItem.key
                            val sellerId = bidItem.child("sellerId").getValue(String::class.java)
                            val productId = bidItem.child("productId").getValue(String::class.java)
                            val result=bidItem.child("result").getValue(String::class.java)
                            if (!sellerId.isNullOrEmpty() && !productId.isNullOrEmpty()) {
                                // Get product from Products node
                                databaseReference.child("products").child(sellerId).child(productId)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(productSnap: DataSnapshot) {
                                            val product = productSnap.getValue(BidProduct::class.java)
                                            if (product != null && bidId != null && result!=null) {
                                                product.bidid = bidId
                                                product.productId = productId
                                                product.sellerId = sellerId
                                                product.result=result
                                                bidList.add(product)
                                                bidAdapter.notifyDataSetChanged()
                                            }
                                        }
                                        override fun onCancelled(error: DatabaseError) {
                                            showErrorToast(error.message)
                                        }
                                    })
                            }
                        }
                        binding.progressBar.visibility=View.GONE
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showErrorToast(error.message)
                        binding.progressBar.visibility=View.GONE
                    }
                })
        } else {
            showErrorToast("User not logged in")
            binding.progressBar.visibility=View.GONE
        }
    }

    private fun removeFromBidSection(bidProduct: BidProduct) {

        if (userid != null) {
            // Remove product from the user's cart in Firebase
            databaseReference.child("YourBids")
                .child(userid)
                .child(bidProduct.bidid)
                .removeValue()
                .addOnSuccessListener {
                    CustomToast.show(requireContext(), "Removed")
                    bidList.remove(bidProduct)
                    bidAdapter.notifyDataSetChanged() // Update RecyclerView
                }
                .addOnFailureListener {
                    showErrorToast("Failed to remove product from Bidding section")
                }
        } else {
            showErrorToast("User not logged in")
        }
    }
    private fun showErrorToast(message: String) {
        CustomToast.show(requireContext(), message)
    }

}