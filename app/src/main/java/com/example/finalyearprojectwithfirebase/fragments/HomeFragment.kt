package com.example.finalyearprojectwithfirebase.fragments

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalyearprojectwithfirebase.R
import com.example.finalyearprojectwithfirebase.activities.ProfileActivity
import com.example.finalyearprojectwithfirebase.adapters.SearchProductAdapter
import com.example.finalyearprojectwithfirebase.databinding.FragmentHomeBinding
import com.example.finalyearprojectwithfirebase.model.SearchProduct
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.InputStreamReader

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var searchProductAdapter: SearchProductAdapter
    private val productList: MutableList<SearchProduct> = mutableListOf()
    private lateinit var state: AutoCompleteTextView
    private lateinit var district: AutoCompleteTextView
    private lateinit var statesAndDistricts: Map<String, List<String>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchProductAdapter = SearchProductAdapter(requireContext(), productList) { product ->
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            intent.putExtra("USER_ID", product.userId)
            intent.putExtra("token", "false") // Passing userId to ProfileActivity
            startActivity(intent)
        }
        binding.searchrecyclerview.layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false)
        binding.searchrecyclerview.adapter = searchProductAdapter

        state = binding.state
        district = binding.district
        district.isEnabled = false
        district.alpha = 0.5f

        val inputStream = requireContext().assets.open("statesanddistricts.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<Map<String, List<String>>>() {}.type
        statesAndDistricts = Gson().fromJson(reader, type)

        val stateNames = statesAndDistricts.keys.toList()
        val stateAdapter = ArrayAdapter(requireContext(), R.layout.dropdownmenupopupitem, stateNames)
        state.setAdapter(stateAdapter)

        state.setOnItemClickListener { _, _, position, _ ->
            val selectedState = stateNames[position]
            val districts = statesAndDistricts[selectedState] ?: emptyList()
            val districtAdapter = ArrayAdapter(requireContext(), R.layout.dropdownmenupopupitem, districts)
            district.setAdapter(districtAdapter)
            district.setText("")
            district.isEnabled = true
            district.alpha = 1f
        }
        state.addTextChangedListener {
            if (it.isNullOrEmpty()) {
                district.setText("")
                district.isEnabled = false
                district.alpha = 0.5f
            }
        }

        // Slider value change listener
        binding.seekbar.addOnChangeListener { _, value, _ ->
            binding.selectedmaxprice.text = "Selected MaxPrice: ₹${value.toInt()}"
        }





        // Search button click listener
        binding.btnSearch.setOnClickListener {

            // 1. Hide keyboard
            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)

            val productName = binding.searchProduct.text.toString().trim()
            val state = binding.state.text.toString().trim()
            val district = binding.district.text.toString().trim()
            val maxPrice = binding.seekbar.value.toInt()  // Use .value instead of .progress

            if (productName.isNotEmpty() && state.isNotEmpty() && district.isNotEmpty() && maxPrice > 0) {
                fetchProducts(productName, state, district, maxPrice)
            } else {
                Toast.makeText(context, "Don't leave any Field Empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchProducts(productName: String, state: String, district: String, maxPrice: Int) {
        val usersRef = FirebaseDatabase.getInstance().reference.child("Users")
        val productsRef = FirebaseDatabase.getInstance().reference.child("products")
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.swipe.visibility=View.VISIBLE
                val usersSnapshot = usersRef.get().await()
                productList.clear()
                val deferredList = mutableListOf<Deferred<Unit>>()
                for (userSnapshot in usersSnapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    val userState = userSnapshot.child("state").getValue(String::class.java)
                    val userDistrict = userSnapshot.child("district").getValue(String::class.java)

                    if (userId != FirebaseAuth.getInstance().currentUser?.uid && userState == state && userDistrict == district) {
                        val deferred = async {
                            val productSnapshot = productsRef.child(userId).get().await()
                            for (productData in productSnapshot.children) {
                                val product = productData.getValue(SearchProduct::class.java)
                                if (product != null &&
                                    product.name.contains(productName, ignoreCase = true) && product.price <= maxPrice
                                ) {
                                    product.userId = userId
                                    productList.add(product)
                                }
                            }
                        }
                        deferredList.add(deferred)
                    }
                }
                deferredList.awaitAll()
                searchProductAdapter.notifyDataSetChanged()
                if (productList.isEmpty()) {
                    Toast.makeText(requireContext(), "No Products Found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE

            }
        }
    }
}



