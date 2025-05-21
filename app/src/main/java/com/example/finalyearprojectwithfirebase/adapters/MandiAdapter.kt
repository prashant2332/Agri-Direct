package com.example.finalyearprojectwithfirebase.adapters
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finalyearprojectwithfirebase.R
import com.example.finalyearprojectwithfirebase.databinding.BiditemBinding
import com.example.finalyearprojectwithfirebase.databinding.ItemMandiBinding
import com.example.finalyearprojectwithfirebase.model.MandiRecord

class MandiAdapter : RecyclerView.Adapter<MandiAdapter.MandiViewHolder>() {

    private val mandiRecords = mutableListOf<MandiRecord>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MandiViewHolder {
        val binding = ItemMandiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MandiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MandiViewHolder, position: Int) {
        val mandirecord = mandiRecords[position]
        holder.binding.commodity.text=mandirecord.commodity
        holder.binding.market.text="${mandirecord.market}"
        holder.binding.minprice.text="₹${mandirecord.min_price}"
        holder.binding.maxprice.text="₹${mandirecord.max_price}"
        holder.binding.avgprice.text="₹${mandirecord.modal_price}"
        holder.binding.date.text="${mandirecord.arrival_date}"
        holder.binding.details.text="State:${mandirecord.state} & District:${mandirecord.district}"
    }

    override fun getItemCount(): Int = mandiRecords.size

    fun submitList(records: List<MandiRecord>) {
        mandiRecords.clear()
        mandiRecords.addAll(records)
        notifyDataSetChanged()
    }
    class MandiViewHolder( var binding: ItemMandiBinding) : RecyclerView.ViewHolder(binding.root)
}
