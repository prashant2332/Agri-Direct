package com.example.finalyearprojectwithfirebase.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalyearprojectwithfirebase.R
import com.example.finalyearprojectwithfirebase.adapters.MandiAdapter
import com.example.finalyearprojectwithfirebase.databinding.FragmentMandiBhavBinding
import com.example.finalyearprojectwithfirebase.model.MandiResponse
import com.example.finalyearprojectwithfirebase.network.MandiApiService
import com.example.finalyearprojectwithfirebase.network.MandiClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.io.InputStreamReader
import java.util.Date
import java.util.Locale

class MandiBhavFragment : Fragment() {

    private lateinit var binding: FragmentMandiBhavBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var mandiAdapter: MandiAdapter
    private lateinit var apiService: MandiApiService
    private lateinit var statename: AutoCompleteTextView
    private lateinit var district: AutoCompleteTextView
    private lateinit var statesAndDistricts: Map<String, List<String>>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMandiBhavBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = binding.mandirecyclerview
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        mandiAdapter = MandiAdapter()
        recyclerView.adapter = mandiAdapter

        statename = binding.statename
        district = binding.district
        district.isEnabled = false
        district.alpha = 0.5f

        // Load state and district data from assets
        val inputStream = requireContext().assets.open("statesanddistricts.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<Map<String, List<String>>>() {}.type
        statesAndDistricts = Gson().fromJson(reader, type)

        // Populate state dropdown
        val stateNames = statesAndDistricts.keys.toList()
        val stateAdapter = ArrayAdapter(requireContext(), R.layout.dropdownmenupopupitem, stateNames)
        statename.setAdapter(stateAdapter)

        statename.setOnItemClickListener { _, _, position, _ ->
            val selectedState = stateNames[position]
            val districts = statesAndDistricts[selectedState] ?: emptyList()
            val districtAdapter = ArrayAdapter(requireContext(), R.layout.dropdownmenupopupitem, districts)
            district.setAdapter(districtAdapter)
            district.setText("")
            district.isEnabled = true
            district.alpha = 1f
        }
        statename.addTextChangedListener {
            if (it.isNullOrEmpty()) {
                district.setText("")
                district.isEnabled = false
                district.alpha = 0.5f
            }
        }

        apiService = MandiClient.instance

        binding.searchpricebtn.setOnClickListener {

            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)

            val productname = binding.productname.text.toString()
            val statename = binding.statename.text.toString()
            val district = binding.district.text.toString()

            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val todaydate = formatter.format(Date())

            when {
                productname.isNotEmpty() && statename.isNotEmpty() && district.isNotEmpty() -> {
                    fetchMandiData(statename, district, productname, todaydate)
                }
                productname.isNotEmpty() && statename.isNotEmpty() && district.isEmpty() -> {
                    fetchMandiDataWithProductAndState(statename, productname, todaydate)
                }
                productname.isNotEmpty() && statename.isEmpty() && district.isEmpty() -> {
                    fetchMandiDataWithProductName(productname, todaydate)
                }
                else -> {
                    Toast.makeText(requireContext(), "Search parameter is missing", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchData(apiCall: Call<MandiResponse>) {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipe.visibility=View.VISIBLE

        apiCall.enqueue(object : Callback<MandiResponse> {
            override fun onResponse(call: Call<MandiResponse>, response: Response<MandiResponse>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val mandiResponse = response.body()
                    if (mandiResponse != null && mandiResponse.records.isNotEmpty()) {
                        mandiAdapter.submitList(mandiResponse.records)
                        recyclerView.apply {
                            visibility = View.VISIBLE
                            alpha = 0f
                            animate().alpha(1f).setDuration(500).start()
                        }
                    } else {
                        Toast.makeText(requireContext(), "No records found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Unexpected response", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<MandiResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                binding.swipe.visibility=View.GONE
                Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun fetchMandiData(state: String, district: String, commodity: String, arrivalDate: String) {
        val call = apiService.getMandiData(state = state, district = district, commodity = commodity,
            arrivalDate = arrivalDate)
        fetchData(call)
    }
    private fun fetchMandiDataWithProductName(commodity: String, arrivalDate: String) {
        val call = apiService.getMandiDatawithproductname(commodity = commodity,
            arrivalDate = arrivalDate)
        fetchData(call)
    }
    private fun fetchMandiDataWithProductAndState(state: String, commodity: String, arrivalDate: String) {
        val call = apiService.getMandiDatawithproductnameandstate(state = state, commodity = commodity,
            arrivalDate = arrivalDate)
        fetchData(call)
    }
}



