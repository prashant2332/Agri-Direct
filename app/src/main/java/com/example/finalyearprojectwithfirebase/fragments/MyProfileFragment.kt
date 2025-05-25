package com.example.finalyearprojectwithfirebase.fragments

import android.Manifest
import android.app.Dialog
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
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.finalyearprojectwithfirebase.R
import com.example.finalyearprojectwithfirebase.databinding.FragmentMyProfileBinding
import com.example.finalyearprojectwithfirebase.network.FileUtils
import com.example.finalyearprojectwithfirebase.network.RetrofitClient
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.InputStreamReader

class MyProfileFragment : Fragment() {

    private val binding:FragmentMyProfileBinding by lazy {
        FragmentMyProfileBinding.inflate(layoutInflater)
    }
    private var cameraImageUri: Uri? = null
    private var currentProfilePicUrl: String = ""

    private val databaserefernce=FirebaseDatabase.getInstance().reference
    private val userid=FirebaseAuth.getInstance().currentUser?.uid

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        fetchuserdetails()

        binding.editprofiledetails.setOnClickListener{
            showEditProfileDialog()
        }
        binding.profilePhoto.setOnClickListener{
            showFullScreenDialog(currentProfilePicUrl)
        }
        binding.editprofilephoto.setOnClickListener{
            showImagePickDialog()
        }
        binding.removeprofilephoto.setOnClickListener{
            context?.let { context ->
                AlertDialog.Builder(context)
                    .setTitle("Remove Profile Photo")
                    .setMessage("Do you really want to remove your profile photo?")
                    .setPositiveButton("Yes") { _, _ ->
                        removeprofilepic()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
        requestcamerapermissions()
        return binding.root
    }
    private fun requestcamerapermissions(){
        // Request camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    private fun removeprofilepic(){
        userid?.let {
            databaserefernce.child("Users").child(it).child("profilepic").setValue("")
        }
        fetchuserdetails()
    }

    private fun fetchuserdetails(){
        if (userid != null) {

            databaserefernce.child("Users").child(userid)
                .get()
                .addOnSuccessListener { snapshot ->

                    binding.email.text = snapshot.child("email").value?.toString() ?: "NA"
                    binding.phonenumber.text = snapshot.child("phonenumber").value?.toString() ?: "NA"
                    binding.state.text = snapshot.child("state").value?.toString() ?: "NA"
                    binding.district.text = snapshot.child("district").value?.toString() ?: "NA"
                    binding.address.text = snapshot.child("localeaddress").value?.toString() ?: "NA"
                    binding.profileUsername.text = snapshot.child("username").value?.toString() ?: "NA"
                    val profilepicurl=snapshot.child("profilepic").value?.toString() ?: ""

                    currentProfilePicUrl = profilepicurl
                    if(profilepicurl.isNotEmpty()){
                        Glide.with(requireContext()).load(profilepicurl).into(binding.profilePhoto)
                    }
                    else{
                        binding.profilePhoto.setImageResource(R.drawable.profile)
                    }
                    binding.progressBar.visibility=View.GONE
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "could not fetch data", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility=View.GONE
                }
        }

        val transactionRef = userid?.let { databaserefernce.child("Transactions").child(it) }

        transactionRef?.get()?.addOnSuccessListener { snapshot ->
            var count = 0
            for (child in snapshot.children) {
                val status = child.child("status").getValue(String::class.java)
                if (status=="Successful") {
                    count++
                }
            }
            binding.succesfultransaction.text="${count}"
        }?.addOnFailureListener {
            Toast.makeText(context, "Failed to retrieve transactions", Toast.LENGTH_SHORT).show()
        }
        if (userid != null) {
            calculateAverageRating(userid) { averageRating ->
                if(averageRating>0) {
                    binding.myrating.text = "%.1f".format(averageRating)
                }
                else{
                    binding.myrating.text = "No Ratings Found"
                }
            }
        }
    }

    fun calculateAverageRating(userId: String, onResult: (Float) -> Unit) {
        val ref = databaserefernce.child("Transactions").child(userId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalRating = 0f
                var count = 0
                for (transactionSnapshot in snapshot.children) {
                    val rating = transactionSnapshot.child("rating").getValue(Float::class.java)
                    if (rating != null) {
                        totalRating += rating
                        count++
                    }
                }
                if (count > 0) {
                    val average = totalRating / count
                    onResult(average)
                } else {
                    onResult(0f) // No ratings found
                }
            }
            override fun onCancelled(error: DatabaseError) {
                onResult(0f)
            }
        })
    }

    // Show Dialog for Editing Profile
    private fun showEditProfileDialog() {

        // Inflate the dialog's layout with EditTexts for user input
        val dialogView = layoutInflater.inflate(R.layout.editprofiledetails, null)
        val statesAndDistricts: Map<String, List<String>>

        // Find EditTexts in dialog layout
        val usernameEditText = dialogView.findViewById<EditText>(R.id.editUsername)
        val phoneEditText = dialogView.findViewById<EditText>(R.id.editPhone)
        val stateEditText = dialogView.findViewById<AutoCompleteTextView>(R.id.editState)
        val districtEditText = dialogView.findViewById<AutoCompleteTextView>(R.id.editDistrict)
        val addressEditText = dialogView.findViewById<EditText>(R.id.editAddress)

        districtEditText.isEnabled = false
        districtEditText.alpha = 0.5f

        // Load state and district data from assets
        val inputStream = requireContext().assets.open("statesanddistricts.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<Map<String, List<String>>>() {}.type
        statesAndDistricts = Gson().fromJson(reader, type)

        // Populate state dropdown
        val stateNames = statesAndDistricts.keys.toList()
        val stateAdapter = ArrayAdapter(requireContext(), R.layout.dropdownmenupopupitem, stateNames)
        stateEditText.setAdapter(stateAdapter)

        stateEditText.setOnItemClickListener { _, _, position, _ ->
            val selectedState = stateNames[position]
            val districts = statesAndDistricts[selectedState] ?: emptyList()
            val districtAdapter = ArrayAdapter(requireContext(), R.layout.dropdownmenupopupitem, districts)
            districtEditText.setAdapter(districtAdapter)
            districtEditText.setText("")
            districtEditText.isEnabled = true
            districtEditText.alpha = 1f
        }
        stateEditText.addTextChangedListener {
            if (it.isNullOrEmpty()) {
                districtEditText.setText("")
                districtEditText.isEnabled = false
                districtEditText.alpha = 0.5f
            }
        }

        // Set current values in the dialog EditTexts
        usernameEditText.setText(binding.profileUsername.text)
        phoneEditText.setText(binding.phonenumber.text)
        stateEditText.setText(binding.state.text)
        districtEditText.setText(binding.district.text)
        addressEditText.setText(binding.address.text)

        // Create and show the AlertDialog
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                // When Save is clicked, get the values from EditTexts
                val updatedUsername = usernameEditText.text.toString()
                val updatedPhone = phoneEditText.text.toString()
                val updatedState = stateEditText.text.toString()
                val updatedDistrict = districtEditText.text.toString()
                val updatedAddress = addressEditText.text.toString()

                // Validate input and update Firebase Database
                if (updatedUsername.isNotEmpty()
                    && updatedPhone.isNotEmpty()
                    && updatedState.isNotEmpty()
                    && updatedDistrict.isNotEmpty()
                    && updatedAddress.isNotEmpty()) {
                    updateProfileInDatabase(updatedUsername, updatedPhone, updatedState, updatedDistrict, updatedAddress)
                    fetchuserdetails()
                } else {
                    Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
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
                Firebase.auth.currentUser?.uid?.let {
                    Firebase.database.reference
                        .child("Users")
                        .child(it)
                        .child("profilepic")
                        .setValue(imageUrl)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Updated Successfully", Toast.LENGTH_SHORT).show()
                            fetchuserdetails()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Update Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Update Profile in Firebase Database
    private fun updateProfileInDatabase(username: String, phone: String, state: String, district: String, address: String) {
        val userRef = userid?.let { databaserefernce.child("Users").child(it) }
        val updatedUserData = mapOf(
            "username" to username,
            "phone" to phone,
            "state" to state,
            "district" to district,
            "localeaddress" to address
        )
        userRef?.updateChildren(updatedUserData)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFullScreenDialog(imageUrl: String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialogfullscreenimage)

        val fullscreenImageView = dialog.findViewById<ImageView>(R.id.fullscreenImageView)
        val closeButton = dialog.findViewById<ImageView>(R.id.closeButton)
        if (imageUrl.isNotEmpty()){
            Glide.with(requireContext()).load(imageUrl).into(fullscreenImageView)
        }
        else{
            fullscreenImageView.setImageResource(R.drawable.profile)
        }
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}
