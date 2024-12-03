package com.example.agri_direct_app.model

class User {
    var username: String = ""
    var name: String = ""
    var phoneNumber: String = ""
    var password: String = ""

    constructor()

    constructor(username: String, name: String, phoneNumber: String, password: String) {
        this.username = username
        this.name = name
        this.phoneNumber = phoneNumber
        this.password = password
    }


}