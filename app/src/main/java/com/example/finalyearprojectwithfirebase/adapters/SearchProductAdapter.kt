package com.example.finalyearprojectwithfirebase.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.finalyearprojectwithfirebase.databinding.ItemProductBinding
import com.example.finalyearprojectwithfirebase.model.SearchProduct

class SearchProductAdapter(
    private val context: Context,
    private val products: List<SearchProduct>,
    private val onItemClick: (SearchProduct) -> Unit
) : RecyclerView.Adapter<SearchProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.binding.productName.text = product.name
        holder.binding.productPrice.text = "₹${product.price}"
        holder.binding.productVariety.text="Variety: ${product.variety}"
        holder.binding.productUnit.text="Unit: ${product.unit}"
        holder.binding.productQuantity.text="Quantity: ${product.quantity.toString()}"



        val bidstatus=product.isbiddingenabled
        if(bidstatus){
            holder.binding.bidstatus.text="Bidding is Open"
        }
        else{
            holder.binding.bidstatus.text="Bidding is Closed"
        }

        val image=product.image
        if(image.isNotEmpty()){
            Glide
                .with(context)
                .load(image)
                .into(holder.binding.productImage)
        }
        // Set on click listener for each product item
        holder.binding.root.setOnClickListener {
            onItemClick(product)
        }
    }

    override fun getItemCount(): Int = products.size

    class ProductViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)
}
