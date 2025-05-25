package com.example.finalyearprojectwithfirebase.adapters


import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.finalyearprojectwithfirebase.R
import com.example.finalyearprojectwithfirebase.activities.ProfileActivity
import com.example.finalyearprojectwithfirebase.databinding.BiditemBinding
import com.example.finalyearprojectwithfirebase.model.BidProduct
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class BidAdapter(
    private val bidList: List<BidProduct>,
    val context: Context,
    private val onDeleteClick: (BidProduct) -> Unit
) : RecyclerView.Adapter<BidAdapter.BidViewHolder>() {

    private val database = FirebaseDatabase.getInstance().reference
    private val userid=FirebaseAuth.getInstance().currentUser?.uid
    private var currentProductPicUrl: String = ""

    inner class BidViewHolder( var binding: BiditemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BidViewHolder {
        // Inflate the layout using ViewBinding
        val binding = BiditemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BidViewHolder(binding)
    }

    override fun getItemCount(): Int = bidList.size

    override fun onBindViewHolder(holder: BidViewHolder, position: Int) {
        val bidProduct = bidList[position]

        // Set data to the views using ViewBinding
        holder.binding.tvCartProductName.text = bidProduct.name
        holder.binding.tvCartVariety.text = "Variety: ${bidProduct.variety}"
        holder.binding.tvCartQuantity.text = "Quantity: ${bidProduct.quantity}"
        holder.binding.tvCartUnit.text = "Unit: ${bidProduct.unit}"
        holder.binding.tvCartPrice.text = "₹ ${bidProduct.price}"

        val bidstatus=bidProduct.isbiddingenabled
        val bidresult=bidProduct.result


        var token=false
        if (bidresult == "true" && !bidstatus){
            token = true
            holder.binding.eligibleforcontact.text = "You Won the auction"

            holder.binding.transactionComplete.isEnabled = false
            holder.binding.transactionnotComplete.isEnabled = false

            loadbtnstatus(bidProduct, holder)

        } else if (bidresult == "false" && !bidstatus) {
            holder.binding.eligibleforcontact.text = "You have lost the Auction"
        }

        if(bidstatus){
            holder.binding.bidstatus.text="Bidding is Open"
        }
        else{
            holder.binding.bidstatus.text="Bidding is Closed"
        }

        loadcurrentbid(bidProduct,holder)

        currentProductPicUrl=bidProduct.image
        if(currentProductPicUrl.isNotEmpty()){
            Glide
                .with(context)
                .load(currentProductPicUrl)
                .into(holder.binding.tvCartProductImage)
        }
        holder.binding.tvCartProductImage.setOnClickListener{
            showFullScreenDialog(currentProductPicUrl)
        }
        // Set the click listener on the remove button
        holder.binding.removefrombidbtn.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Remove Item")
                    .setMessage("Do you want to remove this Item?")
                    .setPositiveButton("Yes") { _, _ ->
                        onDeleteClick(bidProduct)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
        }

        holder.binding.root.setOnClickListener{
                val intent = Intent(context, ProfileActivity::class.java)
                intent.putExtra("USER_ID", bidProduct.sellerId)
                intent.putExtra("token",token.toString())
                context.startActivity(intent)
        }

        holder.binding.transactionComplete.setOnClickListener{
            showRatingDialog(bidProduct.sellerId,"Successful",bidProduct.productId,holder)
            holder.binding.transactionComplete.isEnabled=false
            holder.binding.removefrombidbtn.isEnabled=true
        }
        holder.binding.transactionnotComplete.setOnClickListener{
            showRatingDialog(bidProduct.sellerId,"Canceled",bidProduct.productId,holder)
            holder.binding.transactionnotComplete.isEnabled=false
            holder.binding.removefrombidbtn.isEnabled=true
        }

    }

    private fun loadcurrentbid(bidProduct:BidProduct,holder:BidViewHolder){
        database.child("Bids").child(bidProduct.sellerId).child(bidProduct.productId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentHighestBid = snapshot.child("currenthighestbid").value
                    holder.binding.currenthishestbid.text = "Current Bid: ₹${currentHighestBid}"
                }

                override fun onCancelled(error: DatabaseError) {
                    holder.binding.currenthishestbid.text = "Current Bid: ₹0"
                }
            })
    }


    private fun loadbtnstatus(product:BidProduct,holder: BidViewHolder){
        checkuseralreadyratedornot(product.productId, product.sellerId) { alreadyRated ->
            if(!alreadyRated){
                holder.binding.transactionComplete.isEnabled=true
                holder.binding.transactionnotComplete.isEnabled=true
            }
            else{
                holder.binding.removefrombidbtn.isEnabled=true
            }
        }
    }

    private fun checkuseralreadyratedornot(productid: String, sellerid: String, callback: (Boolean) -> Unit) {
        var rated = false

        val transactionsRef = database.child("Transactions").child(sellerid)

        transactionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()) {
                    for (data in snapshot.children) {
                        val buyerfromdb = data.child("buyerId").getValue(String::class.java)
                        val productFromDb = data.child("productId").getValue(String::class.java)
                        if (buyerfromdb == userid && productFromDb == productid) {
                            rated = true
                            break
                        }
                    }
                }
                callback(rated)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Database error: ${error.message}")
                callback(rated)
            }
        })
    }

    private fun showRatingDialog(sellerId: String,status:String,productId:String,holder:BidViewHolder) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_rate_seller)
        dialog.setCancelable(false)

        val ratingEditText = dialog.findViewById<EditText>(R.id.ratingEditText)
        val rateButton = dialog.findViewById<Button>(R.id.rateButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        rateButton.setOnClickListener {
            val ratingStr = ratingEditText.text.toString()
            if (ratingStr.isNotEmpty()) {
                val rating = ratingStr.toFloatOrNull()
                if (rating != null && rating in 0.0..5.0) {
                    rateSellerInDatabase(sellerId, rating,status,productId,holder)
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Please enter a valid rating between 0 and 5", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Rating cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }


    private fun rateSellerInDatabase(sellerid:String,rating:Float,status:String,productId:String,holder:BidViewHolder){

        val currentuserid=Firebase.auth.currentUser?.uid
        val TransactionRef = sellerid.let {
            database.child("Transactions").child(it).push()
        }

        val transactionitem = mapOf(
            "buyerId" to currentuserid,
            "productId" to productId,
            "sellerId" to sellerid,
            "status" to status,
            "rating" to rating
        )
        TransactionRef.setValue(transactionitem)
            .addOnSuccessListener {
                Toast.makeText(context, "Thank u For the status update", Toast.LENGTH_SHORT).show()
                holder.binding.transactionComplete.isEnabled=false
                holder.binding.transactionnotComplete.isEnabled=false
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update the status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showFullScreenDialog(imageUrl: String) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialogfullscreenimage)

        val fullscreenImageView = dialog.findViewById<ImageView>(com.example.finalyearprojectwithfirebase.R.id.fullscreenImageView)
        val closeButton = dialog.findViewById<ImageView>(com.example.finalyearprojectwithfirebase.R.id.closeButton)

        if (imageUrl.isNotEmpty()){
            Glide.with(context).load(imageUrl).into(fullscreenImageView)
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}
