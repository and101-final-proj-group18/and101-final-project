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
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.RequestHeaders
import com.codepath.asynchttpclient.RequestParams
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import com.google.android.gms.location.*
import okhttp3.Headers


class HomeActivity : AppCompatActivity() {

    private lateinit var searchBar : EditText

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private val PERMISSION_ID = 42

    private var lat = 0.0
    private var long = 0.0
    private var category = ""

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

                getRestaurants(category)

                return@setOnEditorActionListener true
            }

            false
        }


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
        params.put("limit", 5)
        params.put("latitude", "$lat")
        params.put("longitude", "$long")
        params.put("categories", "$category")
        params.put("sort_by", "best_match")
        val requestHeaders = RequestHeaders()
        requestHeaders.put("Authorization", "Bearer ${BuildConfig.api_key}")
        requestHeaders.put("accept", "application/json")

        client.get("https://api.yelp.com/v3/businesses/search", requestHeaders, params, object : JsonHttpResponseHandler() {

            override fun onSuccess(statusCode: Int, headers: Headers, json: JsonHttpResponseHandler.JSON) {
                Log.d("response", "$json")
            }

            override fun onFailure(
                statusCode: Int,
                headers: Headers?,
                errorResponse: String,
                throwable: Throwable?
            ) {
                Log.d("Error", errorResponse)
            }
        })

    }
}
