package com.example.finalyearprojectwithfirebase.fragments

import android.Manifest
import android.R
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalyearprojectwithfirebase.adapters.ProductAdapter
import com.example.finalyearprojectwithfirebase.databinding.FragmentSellBinding
import com.example.finalyearprojectwithfirebase.databinding.NewproductBinding
import com.example.finalyearprojectwithfirebase.databinding.UpdateproductBinding
import com.example.finalyearprojectwithfirebase.model.Product
import com.example.finalyearprojectwithfirebase.model.StockProduct
import com.example.finalyearprojectwithfirebase.network.FileUtils
import com.example.finalyearprojectwithfirebase.network.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Calendar


class SellFragment : Fragment() {

    private var _binding: FragmentSellBinding? = null
    private val binding get() = _binding!!

    private val databasereference=FirebaseDatabase.getInstance().reference
    private val auth=FirebaseAuth.getInstance()
    val userid=auth.currentUser?.uid

    private var dialogBinding: NewproductBinding? = null
    private var cameraImageUri: Uri? = null

    private val pickFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                uploadImageToCloudinary(it)
            }
        }

    private val takePhotoFromCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                uploadImageToCloudinary(cameraImageUri!!)
            }
        }

    private lateinit var adapter: ProductAdapter
    private var productList = ArrayList<StockProduct>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSellBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.userproductsrecyclerview.layoutManager = LinearLayoutManager(requireContext())
        adapter = ProductAdapter(requireContext(),productList,
            onUpdateClick = { product -> navigateToUpdate(product) },
            onDeleteClick = { product -> deleteProduct(product) },
            ontogglebidding = {product,isbidddingenabled ->togglebiddingstatus(product,isbidddingenabled)}
        )
        binding.userproductsrecyclerview.adapter = adapter

        binding.addproduct.setOnClickListener {
            addProduct()
        }

        requestcamerapermission()
        fetchUserProducts()
    }

    private fun requestcamerapermission(){
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    private fun togglebiddingstatus(product:StockProduct,isbiddingenabled:Boolean){
        userid?.let {
            databasereference
                .child("products")
                .child(it)
                .child(product.productid)
                .child("isbiddingenabled")
                .setValue(!isbiddingenabled)
        }

        fetchUserProducts()
    }

    private fun fetchUserProducts() {
        binding.progressBar.visibility=View.VISIBLE
        if (userid != null) {
            databasereference
                .child("products")
                .child(userid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        productList.clear()
                        val tempList = mutableListOf<StockProduct>()
                        var loadedCount = 0
                        val totalProducts = snapshot.children.count()
                        if (totalProducts == 0) {
                            adapter.notifyDataSetChanged()
                            binding.progressBar.visibility=View.GONE
                            return
                        }
                        for (data in snapshot.children) {
                            val productId = data.key ?: continue
                            val product = data.getValue(StockProduct::class.java)
                            product?.productid = productId
                            if (product != null) {
                                databasereference
                                    .child("Bids")
                                    .child(userid)
                                    .child(productId)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(bidSnapshot: DataSnapshot) {
                                            product.currenthighestbid = bidSnapshot.child("currenthighestbid").value?.toString()?.toIntOrNull() ?: 0
                                            product.currentbidderquantity = bidSnapshot.child("currentbidderquantity").value?.toString()?.toIntOrNull() ?: 0
                                            product.currentbidderid = bidSnapshot.child("currentbidderid").value?.toString() ?: ""
                                            tempList.add(product)
                                            loadedCount++
                                            if (loadedCount == totalProducts) {
                                                productList.clear()
                                                productList.addAll(tempList)
                                                adapter.notifyDataSetChanged()
                                                binding.progressBar.visibility=View.GONE
                                            }
                                        }
                                        override fun onCancelled(error: DatabaseError) {

                                            loadedCount++
                                            if (loadedCount == totalProducts) {
                                                productList.clear()
                                                productList.addAll(tempList)
                                                adapter.notifyDataSetChanged()
                                            }
                                        }
                                    })
                            } else {
                                loadedCount++
                                if (loadedCount == totalProducts) {
                                    productList.clear()
                                    productList.addAll(tempList)
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }
                        binding.progressBar.visibility=View.GONE
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Failed to load your products", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility=View.GONE
                    }
                })
        }
    }

    private fun deleteProduct(product: StockProduct) {
        val productId = product.productid

        // 1. Delete from Products
       if (userid != null) {
           databasereference.child("products").child(userid).child(productId)
               .removeValue()
               .addOnSuccessListener {
                   // 2. Delete from all users' Cart
                   databasereference.child("Cart").get().addOnSuccessListener { cartSnapshot ->
                       for (userSnap in cartSnapshot.children) {
                           for (cartItemSnap in userSnap.children) {
                               val pid = cartItemSnap.child("productId").getValue(String::class.java)
                               if (pid == productId) {
                                   cartItemSnap.ref.removeValue()
                               }
                           }
                       }
                   }
                   // 3. Delete from all users' Bids
                   databasereference.child("Bids").get().addOnSuccessListener { bidsSnapshot ->
                       for (userSnap in bidsSnapshot.children) {
                           if (userSnap.hasChild(productId)) {
                               userSnap.child(productId).ref.removeValue()
                           }
                       }
                   }
                   // 4. Delete from all users' YourBids
                   databasereference.child("YourBids").get().addOnSuccessListener { yourBidsSnapshot ->
                       for (userSnap in yourBidsSnapshot.children) {
                           for (bidSnap in userSnap.children) {
                               val pid = bidSnap.child("productId").getValue(String::class.java)
                               if (pid == productId) {
                                   bidSnap.ref.removeValue()
                               }
                           }
                       }
                   }

                   Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                   fetchUserProducts()
               }
               .addOnFailureListener {
                   Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
               }
       }
    }
    private fun addProduct() {
        dialogBinding = NewproductBinding.inflate(LayoutInflater.from(requireContext()))
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding?.root)
            .create()

        val units = listOf("Quintal", "Kilogram")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, units)
        dialogBinding?.etUnit?.setAdapter(adapter)

        dialogBinding?.btnUploadImage?.setOnClickListener{
            showImagePickDialog()
        }
        dialogBinding?.btnAddProduct?.setOnClickListener {
            val name = dialogBinding?.etProductName?.text.toString()
            val variety = dialogBinding?.etVariety?.text.toString()
            val quantity = dialogBinding?.etQuantity?.text.toString().toIntOrNull() ?: 0
            val unit = dialogBinding?.etUnit?.text.toString()
            val price = dialogBinding?.etPrice?.text.toString().toIntOrNull() ?: 0
            val image=dialogBinding?.imagetakenornot?.text.toString()
            val minimumquantitytobid=dialogBinding?.etminimumquantity?.text.toString().toIntOrNull()?:0

            var isValid = true

            if (name.isEmpty()) {
                dialogBinding?.etProductName?.error = "Product name required"
                isValid = false
            }
            if (variety.isEmpty()) {
                dialogBinding?.etVariety?.error = "Variety required"
                isValid = false
            }
            if (quantity <= 0) {
                dialogBinding?.etQuantity?.error = "Enter a valid quantity"
                isValid = false
            }
            if (unit.isEmpty()) {
                dialogBinding?.etUnit?.error = "Unit required"
                isValid = false
            }
            if (price <= 0) {
                dialogBinding?.etPrice?.error = "Enter a valid price"
                isValid = false
            }
            if (image.isEmpty()) {
                Toast.makeText(requireContext(), "Please upload an image", Toast.LENGTH_SHORT).show()
                isValid = false
            }
            if (minimumquantitytobid <= 0) {
                dialogBinding?.etminimumquantity?.error = "Enter a valid minimum quantity to bid"
                isValid = false
            }
            if (!isValid) {
                return@setOnClickListener
            }
            else{
                val product = Product(name, variety, unit,quantity, price,image,minimumquantitytobid)
                userid?.let { uid ->
                    databasereference.child("products").child(uid).push().setValue(product)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Product added", Toast.LENGTH_SHORT).show()
                            fetchUserProducts()
                            alertDialog.dismiss()
                            dialogBinding = null
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to add product", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
        alertDialog.show()
    }
    private fun navigateToUpdate(product: StockProduct) {
        val dialogBinding = UpdateproductBinding.inflate(LayoutInflater.from(requireContext()))
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.etUpdatePrice.setText(product.price.toString())
        dialogBinding.etUpdateQuantity.setText(product.quantity.toString())

        dialogBinding.btnUpdateProduct.setOnClickListener {
            val updatedPrice = dialogBinding.etUpdatePrice.text.toString().toIntOrNull() ?: product.price
            val updatedQuantity = dialogBinding.etUpdateQuantity.text.toString().toIntOrNull() ?: product.quantity

            val productRef = userid?.let { uid ->
                databasereference.child("products").child(uid).child(product.productid)
            }

            productRef?.child("price")?.setValue(updatedPrice)
            productRef?.child("quantity")?.setValue(updatedQuantity)
                ?.addOnSuccessListener {
                    Toast.makeText(requireContext(), "Update Sucessful", Toast.LENGTH_SHORT).show()
                    fetchUserProducts()
                    alertDialog.dismiss()
                }
                ?.addOnFailureListener {
                    Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
                }
        }

        alertDialog.show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun showImagePickDialog() {
        val options = arrayOf("Camera", "Gallery")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Image From")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        openCamera()
                    }
                    1 -> pickFromGallery.launch("image/*")
                }
            }
            .show()
    }
    private fun openCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Picture")
            put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        }
        cameraImageUri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )
        takePhotoFromCamera.launch(cameraImageUri)
    }
    private fun uploadImageToCloudinary(imageUri: Uri) {
        val path = FileUtils.getPath(requireContext(), imageUri) ?: return
        val file = File(path)
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val preset = "my_unsigned_preset".toRequestBody("text/plain".toMediaTypeOrNull())

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.uploadImage(body, preset)
                val imageUrl = response.secure_url
                dialogBinding?.imagetakenornot?.text = imageUrl
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Upload Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
