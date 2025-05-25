package com.example.finalyearprojectwithfirebase.adapters

import android.R
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.finalyearprojectwithfirebase.databinding.ProductItemBinding

import com.example.finalyearprojectwithfirebase.model.StockProduct
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
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

    private val userid=FirebaseAuth.getInstance().currentUser?.uid
    private val database=FirebaseDatabase.getInstance().reference

    inner class ProductViewHolder(val binding: ProductItemBinding,var status:String="") :
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
        val currenthighestbid=product.currenthighestbid
        val currentbidderid=product.currentbidderid
        val biddingenabled=product.isbiddingenabled
        val currentProductPicUrl=product.image

        holder.binding.productName.text = product.name
        holder.binding.productPrice.text = "₹${product.price}"
        holder.binding.productVariety.text = "Variety: ${product.variety}"
        holder.binding.productQuantity.text = "Quantity: ${product.quantity}"
        holder.binding.productUnit.text = "Unit: ${product.unit}"

        if(currentProductPicUrl.isNotEmpty()){
            Glide
                .with(context)
                .load(currentProductPicUrl)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(24)))
                .into(holder.binding.productImage)
        }

        if(currentbidderid.isNotEmpty()){
            holder.binding.btnDelete.isEnabled=false
            holder.binding.btnUpdate.isEnabled=false
        }
        if(currenthighestbid>0) {
            holder.binding.highestBidTextView.text = "Current Bid: ₹${currenthighestbid}"
        }
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
                val userId = product.currentbidderid
                val targetProductId = product.productid
            if(userId.isNotEmpty()) {
                val yourBidsRef =
                    database.child("YourBids").child(userId)

                yourBidsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (bidSnapshot in snapshot.children) {
                            val sellerId =
                                bidSnapshot.child("sellerId").getValue(String::class.java)
                            val productId =
                                bidSnapshot.child("productId").getValue(String::class.java)

                            if (sellerId == userid && productId == targetProductId) {
                                val matchingBidId = bidSnapshot.key
                                yourBidsRef.child(matchingBidId!!).child("result").setValue("true")
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Database error: ${error.message}")
                    }
                })
            }
            else{
                Toast.makeText(context,"No bidder is available",Toast.LENGTH_SHORT).show()
            }
            holder.binding.enabledisable.isEnabled = false
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
            showFullScreenDialog(currentProductPicUrl)
        }

        if(!biddingenabled && currentbidderid.isNotEmpty()) {
            loadstatus(holder, product)
        }

        holder.binding.btnratebuyer.setOnClickListener{
            showRatingDialog(product.currentbidderid,holder.status,product.productid)
            holder.binding.btnratebuyer.isEnabled=false
        }
    }

    private fun loadstatus(holder:ProductViewHolder,product:StockProduct){
        userid?.let {
            database.child("Transactions").child(it)
                .addValueEventListener(object:ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.exists()) {
                            for (data in snapshot.children) {
                                val transactionBuyerId = data.child("buyerId").getValue(String::class.java)
                                val transactionProductId = data.child("productId").getValue(String::class.java)

                                if (transactionBuyerId == product.currentbidderid && transactionProductId == product.productid) {
                                    val status = data.child("status").getValue(String::class.java)

                                    if(status?.equals("Successful") == true){
                                        holder.binding.transactionstatus.text="Transaction Status: Successful"
                                        holder.binding.btnDelete.isEnabled=true
                                        val transactionstatus="Successful"
                                        call(product,transactionstatus,holder)
                                    }
                                    else if(status?.equals("Canceled")==true){
                                        holder.binding.transactionstatus.text="Transaction Status: Canceled"
                                        holder.binding.btnDelete.isEnabled=true
                                        val transactionstatus="Canceled"
                                        call(product,transactionstatus,holder)
                                    }
                                    break
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun call(product: StockProduct, transactionstatus: String,holder:ProductViewHolder) {
        checkuseralreadyratedornot(product.productid, product.currentbidderid) { alreadyRated ->
            Log.d("RatingCheck", "Already rated: $alreadyRated")
            if (!alreadyRated) {
                holder.status=transactionstatus
                holder.binding.btnratebuyer.isEnabled=true
            } else {
                Log.d("RatingCheck", "User already rated")
            }
        }
    }

    private fun checkuseralreadyratedornot(productid: String, buyerid: String, callback: (Boolean) -> Unit) {
        var rated = false
        val transactionsRef = database.child("Transactions").child(buyerid)

        transactionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()) {
                    for (data in snapshot.children) {
                        val sellerFromDb = data.child("sellerId").getValue(String::class.java)
                        val productFromDb = data.child("productId").getValue(String::class.java)
                        if (sellerFromDb == userid && productFromDb == productid) {
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
        val currentuserid= userid
        val TransactionRef = buyerId.let {
            database.child("Transactions").child(it).push()
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
