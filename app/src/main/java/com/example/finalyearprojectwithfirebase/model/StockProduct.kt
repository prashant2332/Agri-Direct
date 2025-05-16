package com.example.finalyearprojectwithfirebase.model

data class StockProduct(
    var productid:String="",
    val name: String = "",
    val price: Int?=null,
    val quantity:Int?=null,
    val unit:String="",
    val variety:String="",
    val image:String="",
    val isbiddingenabled:Boolean=false,
    var currenthighestbid:Int=0,
    var currentbidderquantity:Int=0,
    var currentbidderid:String=""
)
