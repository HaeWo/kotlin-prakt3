package com.example.praktikum1

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity(),SensorEventListener {

    //Location variables
    private lateinit var sensorManager : SensorManager
    private lateinit var locationManager : LocationManager
    private lateinit var locationListener: LocationListener

    //Sensor variables
    private var gravityData: SensorData? = null
    private var accelerometerData: SensorData? = null
    private var gyroscopeData: SensorData? = null

    //TextViews
    private lateinit var acc: TextView
    private lateinit var grv: TextView
    private lateinit var gyr: TextView
    private lateinit var lit: TextView
    private lateinit var pre: TextView
    private lateinit var loc: TextView

    //Buttons
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    //Time variables
    private var dt:Long = 1000
    private var counter:Long = 0

    private var start = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //initialize the variable
        init()

        //check for permission
        checkPermission()

        //The start button
        btnStart.setOnClickListener{
            getLocation()
            start = true
            registerListener()
            btnStart.isEnabled = false
            btnStop.isEnabled = true
        }

        //The stop button
        btnStop.setOnClickListener{
            start = false
            unregisterListener()
            saveFile()
            btnStart.isEnabled = true
            btnStop.isEnabled = false
        }
    }

    private fun init(){
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        acc = findViewById(R.id.ax)
        grv = findViewById(R.id.gx)
        gyr = findViewById(R.id.sx)
        lit = findViewById(R.id.lx)
        pre = findViewById(R.id.px)
        loc = findViewById(R.id.loc)

        btnStart = findViewById<Button>(R.id.start)
        btnStop = findViewById<Button>(R.id.stop)

        //disabling the stop button at start
        btnStop.isEnabled = false
    }

    private fun checkPermission(){
        //checks if the app has access to the needed permission, if not askes for it
        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) !=  PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=  PackageManager.PERMISSION_GRANTED

        ) {
            ActivityCompat.requestPermissions(
                    this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ),
                    1340,
            )
        }
    }

    private fun registerListener(){
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun unregisterListener(){
        sensorManager.unregisterListener(this)
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(){
        locationListener = LocationListener { location ->
            if(start){
                loc.text = "Latitude: ${location.latitude}\nLongitude: ${location.longitude}"
            }
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1L, 1f, locationListener)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        //if statement to update the values every 100ms
        if(System.currentTimeMillis() - counter >= dt) {
            /*update the values of the sensors when they change
            and writing them into an object.
            The if statement ensure to create an object at first
            then update the values of the already created object and NOT
            creating new objects*/
            when (event?.sensor?.type) {
                Sensor.TYPE_ACCELEROMETER -> if (accelerometerData == null) {
                    accelerometerData = SensorData(
                        event.values[0],
                        event.values[1],
                        event.values[2],
                        event.timestamp
                    )
                } else {
                    accelerometerData!!.x = event.values[0]
                    accelerometerData!!.y = event.values[1]
                    accelerometerData!!.z = event.values[2]
                    accelerometerData!!.timestap = event.timestamp
                }
            }
            when (event?.sensor?.type) {
                Sensor.TYPE_GYROSCOPE -> if (gyroscopeData == null) {
                    gyroscopeData = SensorData(
                        event.values[0],
                        event.values[1],
                        event.values[2],
                        event.timestamp
                    )
                } else {
                    gyroscopeData!!.x = event.values[0]
                    gyroscopeData!!.y = event.values[1]
                    gyroscopeData!!.z = event.values[2]
                    gyroscopeData!!.timestap = event.timestamp
                }
            }
            when (event?.sensor?.type) {
                Sensor.TYPE_GRAVITY -> if (gravityData == null) {
                    gravityData = SensorData(
                        event.values[0],
                        event.values[1],
                        event.values[2],
                        event.timestamp
                    )
                } else {
                    gravityData!!.x = event.values[0]
                    gravityData!!.y = event.values[1]
                    gravityData!!.z = event.values[2]
                    gravityData!!.timestap = event.timestamp
                }
            }
            //reset the value to count a new 100ms
            counter = System.currentTimeMillis()
        }

        //read the values from the created object and show it on screen
        getData()

        /*the pressure and light sensors will not be written into an object
        and will not be saved into file later, they will only be shown at the screen*/
        when (event?.sensor?.type) {
            Sensor.TYPE_PRESSURE -> pre.text = "${"%.2f".format(event.values[0])} hPa"
        }
        when (event?.sensor?.type) {
                Sensor.TYPE_LIGHT -> lit.text = "${"%.2f".format(event.values[0])} lx"
            }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun getData(){
        if(gravityData != null) {
                grv.text = "X: ${"%.2f".format(gravityData!!.x)} m/s² \n" +
                        " Y: ${"%.2f".format(gravityData!!.y)} m/s² \n" +
                        " Z: ${"%.2f".format(gravityData!!.z)} m/s² "
        }
        if(accelerometerData != null){
            acc.text = " X: ${"%.2f".format(accelerometerData!!.x)} m/s²" +
                    "\n Y: ${"%.2f".format(accelerometerData!!.y)} m/s² \n" +
                    " Z: ${"%.2f".format(accelerometerData!!.z)} m/s² "
        }
        if(gyroscopeData != null){
            gyr.text = "X: ${"%.2f".format(gyroscopeData!!.x)} m/s² \n" +
                    " Y: ${"%.2f".format(gyroscopeData!!.y)} m/s² \n" +
                    " Z: ${"%.2f".format(gyroscopeData!!.z)} m/s² "
        }
    }

    private fun saveFile(){

        val fileData = "Gravity: "+gravityData.toString()+"\nAcceleration: "+accelerometerData.toString()+"\nGyroscope: "+gyroscopeData.toString()
        var myExternalFile = File(getExternalFilesDir(""), "Output.txt")

        try {
            val fileOutPutStream = FileOutputStream(myExternalFile)
            fileOutPutStream.write(fileData.toByteArray())
            fileOutPutStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Toast.makeText(applicationContext,"data saved",Toast.LENGTH_SHORT).show()
    }

}