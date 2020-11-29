package com.example.hae_wo

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.ArrayList

class ShowMapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var addLocBTN: FloatingActionButton
    private lateinit var changeBTN: FloatingActionButton
    private lateinit var setHighBTN: FloatingActionButton
    private lateinit var setBalancedBTN: FloatingActionButton
    private lateinit var setLowBTN: FloatingActionButton
    private val rotateOpen: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.rotate_open
        )}
    private val rotateClose: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.rotate_close
        )}
    private val fromBottom: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.from_bottom
        )}
    private val toBottom: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.to_bottom)}
    private var clicked = false
    private var accuracy = "HIGH"
    private var locationsHigh: ArrayList<Location> = arrayListOf()
    private var locationsBalanced: ArrayList<Location> = arrayListOf()
    private var locationsLow: ArrayList<Location> = arrayListOf()
    private lateinit var currentLocation: Location
    private lateinit var lastLocation: Location
    private var firstPoint = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        currentLocation = getLocationCallback()
        lastLocation = getLocationCallback()

        addLocBTN = findViewById(R.id.fabAdd)
        setHighBTN = findViewById(R.id.fabHigh)
        setBalancedBTN = findViewById(R.id.fabBalanced)
        setLowBTN = findViewById(R.id.fabLow)
        changeBTN = findViewById(R.id.fabRefresh)

        //Buttons on Click Listeners
        addLocBTN.setOnClickListener{
            getLocationCallback()
            mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(currentLocation.latitude,currentLocation.longitude)))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentLocation.latitude,currentLocation.longitude), 19F))

            if(accuracy == "BALANCED") {
                locationsBalanced.add(currentLocation)
                mMap.addCircle(
                    CircleOptions().center(
                        LatLng(
                            currentLocation.latitude,
                            currentLocation.longitude
                        )
                    )
                        .radius(1.0)
                        .strokeColor(Color.BLUE)
                        .fillColor(Color.BLUE)
                )
                if (!firstPoint) {
                    mMap.addPolyline(
                        PolylineOptions().add(
                            LatLng(
                                lastLocation.latitude,
                                lastLocation.longitude
                            )
                        ).add(LatLng(currentLocation.latitude, currentLocation.longitude))
                            .color(Color.BLUE)
                    )
                }
            }else if(accuracy == "LOW"){
                locationsLow.add(currentLocation)
                mMap.addCircle(
                    CircleOptions().center(
                        LatLng(
                            currentLocation.latitude,
                            currentLocation.longitude
                        )
                    )
                        .radius(1.0)
                        .strokeColor(Color.GREEN)
                        .fillColor(Color.GREEN)
                )
                if (!firstPoint) {
                    mMap.addPolyline(
                        PolylineOptions().add(
                            LatLng(
                                lastLocation.latitude,
                                lastLocation.longitude
                            )
                        ).add(LatLng(currentLocation.latitude, currentLocation.longitude))
                            .color(Color.GREEN)
                    )
                }
            }else {
                locationsHigh.add(currentLocation)
                mMap.addCircle(
                CircleOptions().center(
                    LatLng(
                        currentLocation.latitude,
                        currentLocation.longitude
                    )
                )
                    .radius(1.0)
                    .strokeColor(Color.RED)
                    .fillColor(Color.RED)
                )
                if (!firstPoint) {
                    mMap.addPolyline(
                        PolylineOptions().add(
                            LatLng(
                                lastLocation.latitude,
                                lastLocation.longitude
                             )
                        ).add(LatLng(currentLocation.latitude, currentLocation.longitude))
                            .color(Color.RED)
                    )
                }
            }

            firstPoint = false
            Log.e("lastLocation", lastLocation.toString())
            Log.e("currentLocation", currentLocation.toString())
            lastLocation = currentLocation
        }

        changeBTN.setOnClickListener{
            onAddButtonClicked()
        }

        setHighBTN.setOnClickListener{
            accuracy = "HIGH"
            changeBTN.setImageResource(R.drawable.high)
        }

        setBalancedBTN.setOnClickListener{
            accuracy = "BALANCED"
            changeBTN.setImageResource(R.drawable.balanced)
        }

        setLowBTN.setOnClickListener {
            accuracy = "LOW"
            changeBTN.setImageResource(R.drawable.low)
        }
    }

    private fun onAddButtonClicked() {
        setVisibility(clicked)
        setAnimation(clicked)
        clicked = !clicked
    }

    private fun setAnimation(clicked: Boolean) {
        if(!clicked){
            setHighBTN.visibility = View.VISIBLE
            setBalancedBTN.visibility = View.VISIBLE
            setLowBTN.visibility = View.VISIBLE
        }else{
            setHighBTN.visibility = View.INVISIBLE
            setBalancedBTN.visibility = View.INVISIBLE
            setLowBTN.visibility = View.INVISIBLE
        }
    }

    private fun setVisibility(clicked: Boolean) {
        if(!clicked){
            setHighBTN.startAnimation(fromBottom)
            setBalancedBTN.startAnimation(fromBottom)
            setLowBTN.startAnimation(fromBottom)
            changeBTN.startAnimation(rotateOpen)
        }else{
            setHighBTN.startAnimation(toBottom)
            setBalancedBTN.startAnimation(toBottom)
            setLowBTN.startAnimation(toBottom)
            changeBTN.startAnimation(rotateClose)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationCallback(): Location{

        var tempLocation = Location(LocationManager.NETWORK_PROVIDER)

        var locationRequest: LocationRequest = LocationRequest().setFastestInterval(20000).setInterval(60000).setPriority(
            LocationRequest.PRIORITY_HIGH_ACCURACY)
        if(accuracy == "BALANCED"){
            locationRequest = LocationRequest().setFastestInterval(20000).setInterval(60000).setPriority(
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            Log.e("accuracy:", "PRIORITY_BALANCED_POWER_ACCURACY")
        } else if (accuracy == "LOW"){
            locationRequest = LocationRequest().setFastestInterval(20000).setInterval(60000).setPriority(
                LocationRequest.PRIORITY_LOW_POWER)
            Log.e("accuracy:", "PRIORITY_LOW_POWER")
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(location_result: LocationResult) {
                super.onLocationResult(location_result);
                currentLocation = location_result.lastLocation
                tempLocation = location_result.lastLocation
            }
        }

        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
        return tempLocation
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        //mMap.addMarker(MarkerOptions().position(LatLng(currentLocation.latitude,currentLocation.longitude)))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(currentLocation.latitude,currentLocation.longitude)))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentLocation.latitude,currentLocation.longitude), 2f))
    }
}