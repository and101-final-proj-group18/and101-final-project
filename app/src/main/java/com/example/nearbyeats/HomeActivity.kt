package com.example.nearbyeats

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.RequestHeaders
import com.codepath.asynchttpclient.RequestParams
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import com.google.android.gms.common.internal.FallbackServiceBroker
import com.google.android.gms.location.*
import okhttp3.Headers
import okhttp3.internal.http2.Header
import kotlin.math.log


class HomeActivity : AppCompatActivity() {
    private lateinit var restaurantList : MutableList<Restaurant>
    private lateinit var rvRestaurant: RecyclerView

    private lateinit var searchBar : EditText

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private val PERMISSION_ID = 42

    private var lat = 0.0
    private var long = 0.0
    private var category = ""

    private var restaurantName: String = ""
    private var restaurantImageURL: String = ""
    private var restaurantAddress: String = ""
    private var restaurantLat: Double = 0.0
    private var restaurantLong: Double = 0.0
    private var restaurantRating: Double = 0.0
    private var restaurantPrice: String = ""
    private var restaurantReview: Int = 0
    private var restaurantClosed: Boolean = false
    private var restaurantPhone: String = ""

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), 0
        )

        setContentView(R.layout.activity_home)

        val toolbar : Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        searchBar = findViewById(R.id.search_input)
        val drawableLeft = ContextCompat.getDrawable(this, R.drawable.icon_start)
        val drawableRight = ContextCompat.getDrawable(this, R.drawable.icon_end)

        searchBar.setCompoundDrawablesWithIntrinsicBounds(drawableLeft, null, drawableRight, null)

        searchBar.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                searchBar.setCursorVisible(true)
                if (event.rawX <= searchBar.left + searchBar.compoundDrawables[0].bounds.width()) {
                    if (searchBar.hasFocus()) {
                        goBack()
                    }
                    return@setOnTouchListener true
                }

                if (event.rawX >= searchBar.right - searchBar.compoundDrawables[2].bounds.width()) {
                    if (searchBar.hasFocus()) {
                        searchBar.text.clear()
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }

        searchBar.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                category = searchBar.text.toString()

                rvRestaurant = findViewById(R.id.recycler_view)

                getRestaurants(category)


                return@setOnEditorActionListener true
            }

            false
        }

        restaurantList = mutableListOf()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLastLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        lat = location.latitude
                        long = location.longitude
                    }
                }
            } else {
                Toast.makeText(this, "Please turn on your location...", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000
            fastestInterval = 0
            numUpdates = 1
        }

        fusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
           return
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_ID
        )
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkPermissions()) {
            getLastLocation()
        }
    }

    private fun goBack() {
        searchBar.text.clear()
        searchBar.setFocusableInTouchMode(false)
        searchBar.setFocusable(false)
        searchBar.setFocusableInTouchMode(true)
        searchBar.setFocusable(true)
        searchBar.setCursorVisible(false)
    }
    private fun getRestaurants(category: String) {

        val client = AsyncHttpClient()

        client.setReadTimeout(10)
        client.setConnectTimeout(10)
        val params = RequestParams()
        params.put("limit", 10)
        params.put("latitude", "$lat")
        params.put("longitude", "$long")
        params.put("categories", "$category")
        params.put("sort_by", "best_match")
        val requestHeaders = RequestHeaders()
        requestHeaders.put("Authorization", "bearer your_api_key")  //"bearer $ {BuildConfig.api_key}"
        requestHeaders.put("accept", "application/json")

        client.get("https://api.yelp.com/v3/businesses/search", requestHeaders, params, object : JsonHttpResponseHandler() {

            override fun onSuccess(statusCode: Int, headers: Headers, json: JsonHttpResponseHandler.JSON) {
                Log.d("response", "$json")

                val jsonArray = json.jsonObject.getJSONArray("businesses")

                restaurantList.clear()

                for(i in 0 until jsonArray.length()){
                    val businessJson = jsonArray.getJSONObject(i)

                    restaurantName = businessJson.getString("name")
                    restaurantImageURL = businessJson.getString("image_url")
                    restaurantAddress = businessJson.getJSONObject("location").getString("address1")
                    restaurantLat = businessJson.getJSONObject("coordinates").getDouble("latitude")
                    restaurantLong = businessJson.getJSONObject("coordinates").getDouble("longitude")
                    restaurantRating = businessJson.getDouble("rating")
                    restaurantPrice = if(businessJson.has("price")) businessJson.getString("price") else ""
                    restaurantReview = businessJson.getInt("review_count")
                    restaurantClosed = businessJson.getBoolean("is_closed")
                    restaurantPhone = businessJson.getString("display_phone")

                    val restaurantInfo = Restaurant(restaurantName, restaurantImageURL, restaurantAddress, restaurantLat, restaurantLong, restaurantRating, restaurantPrice, restaurantReview, restaurantClosed, restaurantPhone)
                    restaurantList.add(restaurantInfo)
                }

                val adapter = RestaurantAdapter(restaurantList)
                rvRestaurant.adapter = adapter
                rvRestaurant.layoutManager = LinearLayoutManager(this@HomeActivity)
            }

            override fun onFailure(
                statusCode: Int,
                headers: Headers?,
                errorResponse: String,
                throwable: Throwable?
            ) {
                Log.d("response", errorResponse)
            }
        })

    }
}