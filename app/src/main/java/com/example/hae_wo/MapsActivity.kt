package com.example.hae_wo

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

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
    override fun onMapReady(googleMap: GoogleMap) {"Added polyline between locations to maps 🗺️"
        mMap = googleMap

        // Saving the number of received variables into length
        var length = intent.getIntExtra("LENGTH",0)
        var latlngArray: ArrayList<LatLng> = arrayListOf()
        var lat: Double
        var lng: Double


        // Reading the sent data from MainActivity and converting to into a latlngArray
        for(i in 0..length-2 step 2){
            lat = intent.getDoubleExtra("L${i}",0.0)
            lng = intent.getDoubleExtra("L${i+1}",0.0)

            Log.e("lat:", lat.toString())
            Log.e("lng:", lng.toString())

            if(lng != 0.0 && lat != 0.0)
                latlngArray.add(LatLng(lat,lng))
        }
        Log.e("latlngArray:", latlngArray.toString())

        // Current location should be the last saved location in the array
        val myLocation = latlngArray[latlngArray.size-1]
        Log.e("myLocation:", myLocation.toString())
        mMap.addMarker(MarkerOptions().position(myLocation).title("I'm here"))

        // Add a circle to the current location
        mMap.addCircle(
            CircleOptions().center(myLocation).radius(2.0).strokeColor(Color.BLUE).fillColor(
                Color.BLUE))

        // Move the camera to the current location
        val cameraUpdate= CameraUpdateFactory.newLatLngZoom(myLocation, 17f)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation))
        mMap.animateCamera(cameraUpdate)

        // Draw a red poly line between all the locations in the array
        var lengthNew = latlngArray.size
        Log.e("length:", lengthNew.toString())
        for(i in 0..lengthNew-2 step 2){
            Log.e("i:", i.toString())
            mMap.addCircle(CircleOptions().center(latlngArray[i]).radius(1.0).strokeColor(Color.BLUE).fillColor(Color.BLUE))
            mMap.addCircle(CircleOptions().center(latlngArray[i+1]).radius(1.0).strokeColor(Color.BLUE).fillColor(Color.BLUE))
            mMap.addPolyline(PolylineOptions().add(latlngArray[i]).add(latlngArray[i+1]).color(Color.RED))
        }
    }
}