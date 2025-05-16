package com.example.finalyearprojectwithfirebase.model

data class ProfileProduct(
    val sellerid:String?=null,
    val productId: String? = null,
    val name: String? = null,
    val variety:String?=null,
    val unit:String?=null,
    val quantity:Int=0,
    val price: Int=0,
    val image:String="",
    val minimumbidquantity:Int=0,
    val isbiddingenabled:Boolean=false,
    var currenthighestbid:Int=0
)

