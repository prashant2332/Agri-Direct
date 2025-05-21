package com.example.finalyearprojectwithfirebase.adapters

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.finalyearprojectwithfirebase.R
import com.example.finalyearprojectwithfirebase.databinding.ProfileproductitemBinding
import com.example.finalyearprojectwithfirebase.model.ProfileProduct
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database


class ProfileProductAdapter(
    private val productList: List<ProfileProduct>,
    val context: Context,
    private val onBidClick: () -> Unit) : RecyclerView.Adapter<ProfileProductAdapter.ProfileProductViewHolder>() {

    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid
    private val database=FirebaseDatabase.getInstance().reference

    inner class ProfileProductViewHolder(val binding: ProfileproductitemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileProductViewHolder {
        val binding = ProfileproductitemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProfileProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileProductViewHolder, position: Int) {
        val product = productList[position]

        // Set product details
        holder.binding.productname.text = product.name
        holder.binding.productvariety.text = "Variety: ${product.variety}"
        holder.binding.productunit.text = "Unit: ${product.unit}"
        holder.binding.productquantity.text ="Quantity: ${product.quantity.toString()}"
        holder.binding.productprice.text = "₹${product.price}"
        holder.binding.currentBid.text = "Current Bid: ₹${product.currenthighestbid}"

        // Handle bidding status
        val isBiddingEnabled = product.isbiddingenabled
        val minimumBidQuantity = product.minimumbidquantity
        val currentHighestBid = product.currenthighestbid

        holder.binding.bidstatus.text = if (isBiddingEnabled) "Bidding is Open" else "Bidding is Closed"

        // Place Bid Button logic
        holder.binding.placeBidButton.setOnClickListener {

            AlertDialog.Builder(context)
                .setTitle("Bidding")
                .setMessage("Do you want to Bid for this Product?")
                .setPositiveButton("Yes") { _, _ ->
                    if (isBiddingEnabled) {
                        val bidQuantityStr = holder.binding.bidQuantity.text.toString()
                        val bidInputStr = holder.binding.bidInput.text.toString()

                        if (bidQuantityStr.isNotEmpty() && bidInputStr.isNotEmpty()) {
                            val bidQuantity = bidQuantityStr.toInt()
                            val bidInput = bidInputStr.toInt()

                            if (bidQuantity >= minimumBidQuantity && bidInput > currentHighestBid && bidInput>=product.price) {
                                placeBid(product, bidInput, bidQuantity)

                            } else {
                                Toast.makeText(context, "Bid amount or quantity is too low", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Please enter both bid quantity and amount", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Bidding is disabled by Seller", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

        }

        // Load product image
        Glide.with(context)
            .load(product.image)
            .into(holder.binding.productImage)

        // Product image click listener for full-screen view
        holder.binding.productImage.setOnClickListener {
            showFullScreenDialog(product.image)
        }

        // Add to cart functionality
        holder.binding.addtocartbtn.setOnClickListener {
            if (currentUserId != null) {
                addToCart(product)
            } else {
                Toast.makeText(context, "Please log in to add to cart", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    // Place Bid in Firebase
    private fun placeBid(product: ProfileProduct, bidInput: Int, bidQuantity: Int) {
        val productRef = database.child("Bids").child(product.sellerid!!).child(product.productId!!)

        productRef.child("currenthighestbid").setValue(bidInput)
        productRef.child("currentbidderid").setValue(currentUserId)
        productRef.child("currentbidderquantity").setValue(bidQuantity)
            .addOnSuccessListener {
                Toast.makeText(context, "Bid Placed", Toast.LENGTH_SHORT).show()
                onBidClick()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Bid is not Placed", Toast.LENGTH_SHORT).show()
            }

        val bidRef = database.child("YourBids").child(currentUserId!!)

        bidRef.get().addOnSuccessListener { snapshot ->
            var bidUpdated = false
            for (child in snapshot.children) {
                val existingProductId = child.child("productId").getValue(String::class.java)
                if (existingProductId == product.productId) {
                    // Update existing bid
                    child.ref.child("sellerId").setValue(product.sellerid)
                    child.ref.child("result").setValue("false")
                    bidUpdated = true
                    Toast.makeText(context, "Updated your existing bid", Toast.LENGTH_SHORT).show()
                    break
                }
            }

            if (!bidUpdated) {
                // Push new bid if not already present
                val newBidRef = bidRef.push()
                val bidItem = mapOf(
                    "sellerId" to product.sellerid,
                    "productId" to product.productId,
                    "result" to "false"
                )
                newBidRef.setValue(bidItem)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Added to your bid section", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Cannot add to your bid section", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun addToCart(product: ProfileProduct) {

        if(!checkwhtheraddedtocartornot(product)) {
            val cartRef = currentUserId?.let {
                database.child("Cart").child(it).push()
            }
            val cartItem = mapOf(
                "sellerId" to product.sellerid,
                "productId" to product.productId
            )
            cartRef?.setValue(cartItem)
                ?.addOnSuccessListener {
                    Toast.makeText(context, "Added to your cart", Toast.LENGTH_SHORT).show()
                }
                ?.addOnFailureListener {
                    Toast.makeText(context, "Failed to add to cart", Toast.LENGTH_SHORT).show()
                }
        }
        else{
            Toast.makeText(context, "Already in Your Cart", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkwhtheraddedtocartornot(product: ProfileProduct):Boolean{
        var status=false
        if (currentUserId != null) {
            database.child("Cart")
                .child(currentUserId)
                .orderByChild("productId")
                .equalTo(product.productId)
                .addListenerForSingleValueEvent(object:ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.exists()){
                            status=true
                        }

                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Failed to access the  cart", Toast.LENGTH_SHORT).show()
                    }
                })
        }
        return status
    }

    private fun showFullScreenDialog(imageUrl: String) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialogfullscreenimage)

        val fullscreenImageView = dialog.findViewById<ImageView>(R.id.fullscreenImageView)
        val closeButton = dialog.findViewById<ImageView>(R.id.closeButton)

        if (imageUrl.isNotEmpty()) {
            Glide.with(context).load(imageUrl).into(fullscreenImageView)
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}
