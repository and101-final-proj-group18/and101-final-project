package com.example.nearbyeats

data class Restaurant (
    val name: String,
    val image_url: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val ratings: Double,
    val price: String,
    val reviews: Int
)