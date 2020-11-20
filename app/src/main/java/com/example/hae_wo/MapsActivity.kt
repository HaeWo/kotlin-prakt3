package com.example.hae_wo

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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

        // Add a marker to your current location
        val lat = intent.getDoubleExtra("LAT",0.0)
        val lng = intent.getDoubleExtra("LNG", 0.0)
        val Daraa = LatLng(lat, lng)
        mMap.addMarker(MarkerOptions().position(Daraa).title("Home @ Daraa"))
        // Add a circle to your current location
        mMap.addCircle(
            CircleOptions().center(Daraa).radius(2.0).strokeColor(Color.BLACK).fillColor(
                Color.RED))
        // Move the camera to your current location
        val cameraUpdate= CameraUpdateFactory.newLatLngZoom(Daraa, 17f)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Daraa))
        mMap.animateCamera(cameraUpdate)
    }
}