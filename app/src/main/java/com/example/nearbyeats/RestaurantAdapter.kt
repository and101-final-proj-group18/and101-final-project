package com.example.nearbyeats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RestaurantAdapter(private val restaurantList: List<Restaurant>) : RecyclerView.Adapter<RestaurantAdapter.ViewHolder>(){
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val restaurantImage: ImageView
        val restaurantName: TextView
        val restaurantDirection : TextView
        val restaurantRating : TextView
        val restaurantPrice : TextView

        init {
            // Find our RecyclerView item's ImageView for future use
            restaurantImage = view.findViewById(R.id.featured_image)
            restaurantName = view.findViewById(R.id.restaurant_name)
            restaurantDirection = view.findViewById(R.id.direction)
            restaurantRating = view.findViewById(R.id.ratings_reviews)
            restaurantPrice = view.findViewById(R.id.price_range)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return restaurantList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        fun hasDigit(input: String): Boolean {
            return input.any { it.isDigit() }
        }

        val restaurantItem = restaurantList[position]
        Glide.with(holder.itemView)
            .load(restaurantItem.image_url)
            .centerCrop()
            .into(holder.restaurantImage)

        holder.restaurantName.text = restaurantItem.name
        holder.restaurantDirection.text = restaurantItem.address
        holder.restaurantRating.text = "${restaurantItem.ratings} (${restaurantItem.reviews})"
        holder.restaurantPrice.text = if (hasDigit(restaurantItem.price)) {
            restaurantItem.price
        }
        else {
            "Price unavailable"
        }

        holder.restaurantName.setOnClickListener{
            val context = holder.itemView.context
            val phoneNumber = restaurantItem.phone
            var closed = "closed"
            closed = if(!restaurantItem.closed){
                "closed"
            } else{
                "open"
            }
            Toast.makeText(context, "Restaurant is currently $closed, phone number is $phoneNumber", Toast.LENGTH_LONG).show()
        }
    }
}

