package com.example.finalyearprojectwithfirebase.model

data class Product(
    val name: String = "",
    val variety:String="",
    val unit:String="",
    val quantity:Int?=null,
    val price: Int?=null,
    val image:String="",
    val minimumbidquantity:Int=0,
    val isbiddingenabled:Boolean=true
)

