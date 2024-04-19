package com.example.nearbyeats

data class Restaurant (
    val name: String,
    val image_url: String,
    val address: String,
    val ratings: String,
    val price: String,
    val reviews: String,
    val closed: Boolean,
    val phone: String
)