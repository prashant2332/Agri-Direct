package com.example.finalyearprojectwithfirebase.model


import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.example.finalyearprojectwithfirebase.R

object CustomToast {

    fun show(context: Context, message: String) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast,null)

        val text = layout.findViewById<TextView>(R.id.message)
        text.text = message
        val toast = Toast(context.applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
        toast.show()
    }
}
