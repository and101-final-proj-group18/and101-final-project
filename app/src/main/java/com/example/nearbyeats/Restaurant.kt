package com.example.nearbyeats

data class Restaurant (
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val ratings: Double,
    val reviews: Int,
    val image_url: String,
    val address: String
)