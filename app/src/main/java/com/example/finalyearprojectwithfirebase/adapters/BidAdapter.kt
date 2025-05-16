package com.example.finalyearprojectwithfirebase.adapters


import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
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

    var viewholder:BidViewHolder?=null

    private var currentProfilePicUrl: String = ""

    inner class BidViewHolder( var binding: BiditemBinding) : RecyclerView.ViewHolder(binding.root)



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BidViewHolder {
        // Inflate the layout using ViewBinding
        val binding = BiditemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BidViewHolder(binding)
    }

    override fun getItemCount(): Int = bidList.size

    override fun onBindViewHolder(holder: BidViewHolder, position: Int) {
        val bidProduct = bidList[position]
        viewholder=holder

        // Set data to the views using ViewBinding
        holder.binding.tvCartProductName.text = bidProduct.name
        holder.binding.tvCartVariety.text = "Variety: ${bidProduct.variety}"
        holder.binding.tvCartQuantity.text = "Quantity: ${bidProduct.quantity}"
        holder.binding.tvCartUnit.text = "Unit: ${bidProduct.unit}"
        holder.binding.tvCartPrice.text = "₹ ${bidProduct.price}"

        val bidstatus=bidProduct.isbiddingenabled
        val bidresult=bidProduct.result


        var token=false
        if (bidresult.toBoolean() && !bidstatus) {
            token = true
            holder.binding.eligibleforcontact.text = "You Won the auction"

            checkuseralreadyrespondornot(bidProduct.sellerId, bidProduct.productId) { alreadyResponded ->
                if (!alreadyResponded) {
                    holder.binding.transactionComplete.isEnabled = true
                    holder.binding.transactionnotComplete.isEnabled = true
                } else {
                    holder.binding.transactionComplete.isEnabled = false
                    holder.binding.transactionnotComplete.isEnabled = false
                }
            }

        } else if (!bidresult.toBoolean() && bidstatus) {
            holder.binding.eligibleforcontact.text = "Not Declared"
        } else if (!bidresult.toBoolean() && !bidstatus) {
            holder.binding.eligibleforcontact.text = "You have lost the Auction"
        }
        else if(!bidresult.toBoolean() && bidstatus ){
            holder.binding.eligibleforcontact.text="Not Declared"
        }
        else if(!bidresult.toBoolean() && !bidstatus){
            holder.binding.eligibleforcontact.text="You have lost the Auction"
        }

        if(bidstatus){
            holder.binding.bidstatus.text="Bidding is Open"
        }
        else{
            holder.binding.bidstatus.text="Bidding is Closed"
        }



        currentProfilePicUrl=bidProduct.image

        if(currentProfilePicUrl.isNotEmpty()){
            Glide
                .with(context)
                .load(currentProfilePicUrl)
                .into(holder.binding.tvCartProductImage)
        }

        holder.binding.tvCartProductImage.setOnClickListener{
            showFullScreenDialog(currentProfilePicUrl)
        }

        // Set the click listener on the remove button
        holder.binding.removefrombidbtn.setOnClickListener {

                AlertDialog.Builder(context)
                    .setTitle("Remove Item")
                    .setMessage("Do you want to  this Item?")
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
            showRatingDialog(bidProduct.sellerId,"Successful",bidProduct.productId)
        }
        holder.binding.transactionnotComplete.setOnClickListener{
            showRatingDialog(bidProduct.sellerId,"Canceled",bidProduct.productId)
        }



        Firebase.database.reference
            .child("Bids")
            .child(bidProduct.sellerId)
            .child(bidProduct.productId)
            .addValueEventListener(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentbhishestbid=snapshot.child("currenthighestbid").value
                    holder.binding.currenthishestbid.text="Current Bid: ₹${currentbhishestbid}"
                }

                override fun onCancelled(error: DatabaseError) {
                    holder.binding.currenthishestbid.text="Not Available"
                }

            })
    }


    private fun checkuseralreadyrespondornot(sellerid: String,productId: String,callback: (Boolean) -> Unit){
        var responded = false
        val transRef = FirebaseDatabase.getInstance().getReference("Transactions")
        val sellerId = sellerid
        val buyerId = Firebase.auth.currentUser?.uid
        val targetProductId = productId

        transRef.child(sellerId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()) {
                        for (transactionSnapshot in snapshot.children) {
                            val storedBuyerId =
                                transactionSnapshot.child("buyerId").getValue(String::class.java)
                            val productId =
                                transactionSnapshot.child("productId").getValue(String::class.java)
                            val status =
                                transactionSnapshot.child("status").getValue(String::class.java)

                            if (storedBuyerId == buyerId &&
                                productId == targetProductId &&
                                (status.equals(
                                    "Successful",
                                    ignoreCase = true
                                ) || status.equals("Canceled", ignoreCase = true))
                            ) {
                                responded = true
                                break
                            }
                        }
                    }
                    callback(responded)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

    }

    private fun showRatingDialog(sellerId: String,status:String,productId:String) {
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
                    rateSellerInDatabase(sellerId, rating,status,productId)
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


    private fun rateSellerInDatabase(sellerid:String,rating:Float,status:String,productId:String){

        val currentuserid=Firebase.auth.currentUser?.uid
        val TransactionRef = sellerid.let {
            com.google.firebase.Firebase.database.reference
                .child("Transactions")
                .child(it)
                .push()
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
                viewholder?.binding?.transactionComplete?.isEnabled=false
                viewholder?.binding?.transactionnotComplete?.isEnabled=false
                viewholder?.binding?.removefrombidbtn?.isEnabled=true

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
