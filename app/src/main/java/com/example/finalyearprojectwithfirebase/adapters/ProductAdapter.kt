package com.example.finalyearprojectwithfirebase.adapters

import android.R
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.finalyearprojectwithfirebase.databinding.ProductItemBinding

import com.example.finalyearprojectwithfirebase.model.StockProduct
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class ProductAdapter(private val context: Context,
    private val productList: List<StockProduct>,
    private val onUpdateClick: (StockProduct) -> Unit,
    private val onDeleteClick: (StockProduct) -> Unit,
    private val ontogglebidding: (StockProduct,Boolean) ->Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    var sellerstatus=""
    var viewholder:ProductViewHolder?=null
    var productid=""

    inner class ProductViewHolder(val binding: ProductItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ProductItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun getItemCount(): Int = productList.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        viewholder=holder
        productid=product.productid

        holder.binding.productName.text = product.name
        holder.binding.productPrice.text = "₹${product.price}"
        holder.binding.productVariety.text = "Variety: ${product.variety}"
        holder.binding.productQuantity.text = "Quantity: ${product.quantity}"
        holder.binding.productUnit.text = "Unit: ${product.unit}"

        val currenthighestbid=product.currenthighestbid

        val currentbidderid=product.currentbidderid
        if(currentbidderid.isNotEmpty()){
            holder.binding.btnDelete.isEnabled=false
            holder.binding.btnUpdate.isEnabled=false
        }

        if(currenthighestbid>0) {
            holder.binding.highestBidTextView.text = "Current Bid: ₹${currenthighestbid}"
        }

        val biddingenabled=product.isbiddingenabled
        if(biddingenabled){
            holder.binding.enabledisable.text="Disable"
            holder.binding.bidstatus.text="Bidding is Open"
        }
        else{
            holder.binding.enabledisable.isEnabled=false
            holder.binding.enabledisable.text="Disabled"
            holder.binding.bidstatus.text="Bidding is Closed"
        }


        holder.binding.enabledisable.setOnClickListener{
                ontogglebidding(product,biddingenabled)
                val userId = product.currentbidderid // replace with actual user ID
                val targetSellerId = Firebase.auth.currentUser?.uid!! // the sellerId you are searching for
                val targetProductId = product.productid // the productId you are searching for

                val yourBidsRef = FirebaseDatabase.getInstance().getReference("YourBids").child(userId)

                yourBidsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (bidSnapshot in snapshot.children) {
                            val sellerId = bidSnapshot.child("sellerId").getValue(String::class.java)
                            val productId = bidSnapshot.child("productId").getValue(String::class.java)

                            if (sellerId == targetSellerId && productId == targetProductId) {
                                val matchingBidId = bidSnapshot.key

                                yourBidsRef
                                    .child(matchingBidId!!)
                                    .child("result")
                                    .setValue("true")

                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Database error: ${error.message}")
                    }
                })

                holder.binding.enabledisable.isEnabled=false



        }

        Firebase.auth.currentUser?.uid?.let {
            Firebase.database.reference
                .child("Transactions")
                .child(it)
                .get().addOnSuccessListener {
                        snapshot ->
                    for (child in snapshot.children) {
                        val transactionBuyerId = child.child("buyerId").getValue(String::class.java)
                        val transactionProductId = child.child("productId").getValue(String::class.java)

                        if (transactionBuyerId == product.currentbidderid && transactionProductId == product.productid) {
                            val status = child.child("status").getValue(String::class.java)

                            if(status?.equals("Successful") == true){
                                holder.binding.transactionstatus.text="Transaction Successful"
                                holder.binding.btnDelete.isEnabled=true
                                sellerstatus="Successful"
                                call(product)
                            }
                            else if(status?.equals("Canceled")==true){
                                holder.binding.transactionstatus.text="Transaction Canceled"
                                holder.binding.btnDelete.isEnabled=true
                                sellerstatus="Canceled"
                                call(product)
                            }
                            break
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(context, "Failed to retrieve transactions", Toast.LENGTH_SHORT).show()

                }
        }


        val currentProfilePicUrl=product.image
        if(currentProfilePicUrl.isNotEmpty()){
            Glide
                .with(context)
                .load(currentProfilePicUrl)
                .into(holder.binding.productImage)
        }

        holder.binding.btnUpdate.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Update Product")
                    .setMessage("Do you want to Update this product?")
                    .setPositiveButton("Yes") { _, _ ->
                        onUpdateClick(product)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
        }
        holder.binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Delete Product")
                .setMessage("Do you want to Delete this product?")
                .setPositiveButton("Yes") { _, _ ->
                    onDeleteClick(product)
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        holder.binding.productImage.setOnClickListener{
            showFullScreenDialog(currentProfilePicUrl)
        }



        holder.binding.btnratebuyer.setOnClickListener{
            showRatingDialog(product.currentbidderid,sellerstatus,product.productid)
            holder.binding.btnratebuyer.isEnabled=false
        }



    }

    private fun call(product:StockProduct){
        if(!product.isbiddingenabled && product.currentbidderid.isNotEmpty() && sellerstatus.isNotEmpty()) {
            checkuseralreadyratedornot(productid, product.currentbidderid)
        }
    }

    private fun checkuseralreadyratedornot(productid: String, buyerid: String) {
        val transactionRef = FirebaseDatabase.getInstance().reference
            .child("Transactions")
            .child(buyerid)

        transactionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                    var alreadyRated = false
                    for (transactionSnapshot in snapshot.children) {
                        val sellerId = transactionSnapshot.child("sellerId").getValue(String::class.java)
                        val productIdSnapshot = transactionSnapshot.child("productId").getValue(String::class.java)

                        if (sellerId == Firebase.auth.currentUser?.uid && productIdSnapshot == productid) {
                            alreadyRated = true
                            break // no need to check further
                        }
                    }

                    // Enable or disable button based on match result
                    viewholder?.binding?.btnratebuyer?.isEnabled = !alreadyRated

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE_ERROR", "Failed to fetch data: ${error.message}")
            }
        })
    }


    fun showRatingDialog(buyerId: String,status:String,productId:String) {
        val dialog = Dialog(context)
        dialog.setContentView(com.example.finalyearprojectwithfirebase.R.layout.dialog_rate_buyer)
        dialog.setCancelable(false)

        val ratingEditText = dialog.findViewById<EditText>(com.example.finalyearprojectwithfirebase.R.id.ratingEditText)
        val rateButton = dialog.findViewById<Button>(com.example.finalyearprojectwithfirebase.R.id.rateButton)
        val cancelButton = dialog.findViewById<Button>(com.example.finalyearprojectwithfirebase.R.id.cancelButton)

        rateButton.setOnClickListener {
            val ratingStr = ratingEditText.text.toString()
            if (ratingStr.isNotEmpty()) {
                val rating = ratingStr.toFloatOrNull()
                if (rating != null && rating in 0.0..5.0) {
                    rateBuyerInDatabase(buyerId, rating,status,productId)
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

    private fun rateBuyerInDatabase(buyerId:String,rating:Float,status:String,productId:String){

        val currentuserid= Firebase.auth.currentUser?.uid
        val TransactionRef = buyerId.let {
            Firebase.database.reference
                .child("Transactions")
                .child(it)
                .push()
        }


        val transactionitem = mapOf(
            "buyerId" to buyerId,
            "productId" to productId,
            "sellerId" to currentuserid,
            "status" to status,
            "rating" to rating
        )

        TransactionRef.setValue(transactionitem)
            .addOnSuccessListener {
                Toast.makeText(context, "Thank u for  rating the buyer", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update the status", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showFullScreenDialog(imageUrl: String) {
        val dialog = Dialog(context, R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(com.example.finalyearprojectwithfirebase.R.layout.dialogfullscreenimage)

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
