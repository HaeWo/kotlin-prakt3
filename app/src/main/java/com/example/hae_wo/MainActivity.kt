package com.example.hae_wo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity(),SensorEventListener {

    //Location variables
    private lateinit var sensorManager : SensorManager
    private lateinit var locationManager : LocationManager
    private lateinit var locationListener: LocationListener

    //JSON
    var jsonArray = JSONArray()

    //TextViews
    private lateinit var acc: TextView
    private lateinit var grv: TextView
    private lateinit var gyr: TextView
    private lateinit var lit: TextView
    private lateinit var pre: TextView
    private lateinit var loc: TextView

    //Buttons
    private lateinit var btnStart: Button
    
    //Checkboxes
    private lateinit var grv_cb: CheckBox
    private lateinit var gyr_cb: CheckBox
    private lateinit var acc_cb: CheckBox
    private lateinit var lit_cb: CheckBox
    private lateinit var pre_cb: CheckBox
    private lateinit var loc_cb: CheckBox

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

        //The start/stop button
        btnStart.setOnClickListener{
            if(!start){
                start = true
                //getLocation()
                registerListener()
                btnStart.text = "Stop"
            }else{
                start = false
                unregisterListener()
                saveFile()
                btnStart.text = "Start"
            }
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
        grv_cb = findViewById(R.id.grv_checkBox)
        gyr_cb = findViewById(R.id.gyr_checkBox)
        acc_cb = findViewById(R.id.acc_checkBox)
        lit_cb = findViewById(R.id.light_checkBox)
        pre_cb = findViewById(R.id.pressure_checkBox)
        loc_cb = findViewById(R.id.location_checkBox)

        btnStart = findViewById<Button>(R.id.start)
    }

    private fun checkPermission(){
        //checks if the app has access to the needed permission, if not askes for it
        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=  PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=  PackageManager.PERMISSION_GRANTED

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
            loc.text = "Latitude: ${location.latitude}\n" +
                    "Longitude: ${location.longitude}"
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1L,
            1f,
            locationListener
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {

        var jsonObjectGrv = JSONObject()
        var jsonObjectAcc = JSONObject()
        var jsonObjectGyro = JSONObject()
        var jsonObjectLit = JSONObject()
        var jsonObjectPre = JSONObject()
        var jsonObjectLoc = JSONObject()

        if(grv_cb.isChecked) {
            when (event?.sensor?.type) {
                Sensor.TYPE_GRAVITY -> {
                    grv.text = "X: ${"%.2f".format(event.values[0])} m/s² \n" +
                            " Y: ${"%.2f".format(event.values[1])} m/s² \n" +
                            " Z: ${"%.2f".format(event.values[2])} m/s² "
                    jsonObjectGrv.put("Sensor", "Gravity")
                    jsonObjectGrv.put("X", event.values?.get(0))
                    jsonObjectGrv.put("Y", event.values?.get(1))
                    jsonObjectGrv.put("Z", event.values?.get(2))
                    jsonObjectGrv.put("Time", System.currentTimeMillis())
                }
            }
        }

        if(acc_cb.isChecked) {
            when (event?.sensor?.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    acc.text = "X: ${"%.2f".format(event.values[0])} m/s² \n" +
                            " Y: ${"%.2f".format(event.values[1])} m/s² \n" +
                            " Z: ${"%.2f".format(event.values[2])} m/s² "
                    jsonObjectAcc.put("Sensor", "Accelerometer")
                    jsonObjectAcc.put("X", event.values?.get(0))
                    jsonObjectAcc.put("Y", event.values?.get(1))
                    jsonObjectAcc.put("Z", event.values?.get(2))
                    jsonObjectAcc.put("Time", System.currentTimeMillis())
                }
            }
        }

        if(gyr_cb.isChecked) {
            when (event?.sensor?.type) {
                Sensor.TYPE_GYROSCOPE -> {
                    gyr.text = "X: ${"%.2f".format(event.values[0])} m/s² \n" +
                            " Y: ${"%.2f".format(event.values[1])} m/s² \n" +
                            " Z: ${"%.2f".format(event.values[2])} m/s² "
                    jsonObjectGyro.put("Sensor", "Gyroscope")
                    jsonObjectGyro.put("X", event.values?.get(0))
                    jsonObjectGyro.put("Y", event.values?.get(1))
                    jsonObjectGyro.put("Z", event.values?.get(2))
                    jsonObjectGyro.put("Time", System.currentTimeMillis())
                }
            }
        }

        if(pre_cb.isChecked) {
            when (event?.sensor?.type) {
                Sensor.TYPE_PRESSURE -> {
                    pre.text = "${"%.2f".format(event.values[0])} hPa"
                    jsonObjectPre.put("Sensor", "Pressure")
                    jsonObjectPre.put("Value", event.values[0])
                    jsonObjectPre.put("Time", System.currentTimeMillis())
                }
            }
        }

        if(lit_cb.isChecked) {
            when (event?.sensor?.type) {
                Sensor.TYPE_LIGHT -> {
                    lit.text = "${"%.2f".format(event.values[0])} lx"
                    jsonObjectLit.put("Sensor", "Light")
                    jsonObjectLit.put("Value", event.values[0])
                    jsonObjectLit.put("Time", System.currentTimeMillis())
                }
            }
        }

        if(System.currentTimeMillis() - counter >= dt) {
            if(!jsonObjectGrv.isNull("X")&&!jsonObjectGrv.isNull("Y")&&!jsonObjectGrv.isNull("Z")){
                jsonArray.put(jsonObjectGrv)
            }

            if(!jsonObjectAcc.isNull("X")&&!jsonObjectAcc.isNull("Y")&&!jsonObjectAcc.isNull("Z")){
                jsonArray.put(jsonObjectAcc)
            }

            if(!jsonObjectGyro.isNull("X")&&!jsonObjectGyro.isNull("Y")&&!jsonObjectGyro.isNull("Z")){
                jsonArray.put(jsonObjectGyro)
            }

            if(!jsonObjectPre.isNull("Value")){
                jsonArray.put(jsonObjectPre)
            }

            if(!jsonObjectLit.isNull("Value")){
                jsonArray.put(jsonObjectLit)
            }

            counter = System.currentTimeMillis()
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveFile(){

        var myExternalFile = File(getExternalFilesDir(""), "Output.json")

        try {
            val fileOutPutStream = FileOutputStream(myExternalFile)
            fileOutPutStream.write(jsonArray.toString().toByteArray())
            fileOutPutStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Toast.makeText(applicationContext,"data saved",Toast.LENGTH_SHORT).show()
    }

}