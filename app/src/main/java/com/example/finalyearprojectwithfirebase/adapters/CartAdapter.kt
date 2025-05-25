package com.example.finalyearprojectwithfirebase.adapters

import android.R
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.finalyearprojectwithfirebase.activities.ProfileActivity
import com.example.finalyearprojectwithfirebase.databinding.CartitemBinding
import com.example.finalyearprojectwithfirebase.model.CartProduct
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class CartAdapter(
    private val cartList: List<CartProduct>,
    val context: Context,
    private val onDeleteClick: (CartProduct) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private var currentProductPicUrl: String = ""
    private val database=FirebaseDatabase.getInstance().reference

    inner class CartViewHolder( var binding: CartitemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        // Inflate the layout using ViewBinding
        val binding = CartitemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun getItemCount(): Int = cartList.size

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartProduct = cartList[position]

        // Set data to the views using ViewBinding
        holder.binding.tvCartProductName.text =cartProduct.name
        holder.binding.tvCartVariety.text = "Variety: ${cartProduct.variety}"
        holder.binding.tvCartQuantity.text = "Quantity: ${cartProduct.quantity}"
        holder.binding.tvCartUnit.text = "Unit: ${cartProduct.unit}"
        holder.binding.tvCartPrice.text = "₹${cartProduct.price}"

        val bidstatus=cartProduct.isbiddingenabled
        database.child("Bids").child(cartProduct.sellerId).child(cartProduct.productId)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentbhighestbid=snapshot.child("currenthighestbid").value
                    holder.binding.currenthighestbid.text="Current Bid: ₹${currentbhighestbid}"
                }
                override fun onCancelled(error: DatabaseError) {
                    holder.binding.currenthighestbid.text="Current Bid: ₹0"
                }
            })

        if(bidstatus){
            holder.binding.bidstatus.text="Bidding is Open"
        }
        else{
            holder.binding.bidstatus.text="Bidding is Closed"
        }

        currentProductPicUrl=cartProduct.image
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
        holder.binding.removefromcartbtn.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Delete Cart Item")
                    .setMessage("Do you want to delete this Item?")
                    .setPositiveButton("Yes") { _, _ ->
                        onDeleteClick(cartProduct)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
        }
        // Set the click listener on whole item
        holder.binding.root.setOnClickListener{
            val intent= Intent(context,ProfileActivity::class.java)
            intent.putExtra("USER_ID",cartProduct.sellerId)
            context.startActivity(intent)
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
