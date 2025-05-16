package com.example.finalyearprojectwithfirebase.model

data class CartProduct(
    var cartid:String="",
    var productId: String = "",
    var sellerId: String = "",
    val name: String = "",
    val variety: String = "",
    val quantity: Int=0,
    val unit: String = "",
    val price: Int=0,
    val image:String="",
    val isbiddingenabled:Boolean=false
)

