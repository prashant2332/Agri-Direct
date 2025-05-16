package com.example.finalyearprojectwithfirebase.model

data class SearchProduct(
    val name: String = "",
    val variety:String="",
    val unit:String="",
    val quantity:Int=0,
    val price: Int = 0,
    var userId: String = "",
    val image:String="",
    val isbiddingenabled:Boolean=false
)


