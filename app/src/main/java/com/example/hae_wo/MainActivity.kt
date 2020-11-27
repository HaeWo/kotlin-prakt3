package com.example.hae_wo

import  android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(),SensorEventListener {

    //Managers
    private lateinit var sensorManager : SensorManager
    private lateinit var locationManager : LocationManager

    //JSON
    private var jsonArray = JSONArray()
    private var jsonObjectGrv = JSONObject()
    private var jsonObjectAcc = JSONObject()
    private var jsonObjectGyro = JSONObject()
    private var jsonObjectLit = JSONObject()
    private var jsonObjectPre = JSONObject()

    //TextViews
    private lateinit var acc: TextView
    private lateinit var grv: TextView
    private lateinit var gyr: TextView
    private lateinit var lit: TextView
    private lateinit var pre: TextView
    private lateinit var loc: TextView

    //EditText
    private lateinit var inputTime: EditText

    //Buttons
    private lateinit var btnStart: Button
    private lateinit var sendToMap: Button
    private lateinit var btnHTTP: Button
    private lateinit var showMap: Button

    private var start = false

    //RadioButtons
    private lateinit var rbHigh: RadioButton
    private lateinit var rbBalanced: RadioButton
    private lateinit var rbLow: RadioButton

    private lateinit var rg: RadioGroup

    private var accuracy: Int = 1
    
    //Checkboxes
    private lateinit var grvCB: CheckBox
    private lateinit var gyrCB: CheckBox
    private lateinit var accCB: CheckBox
    private lateinit var litCB: CheckBox
    private lateinit var preCB: CheckBox
    private lateinit var locCB: CheckBox

    //Coroutines
    private var scope = MainScope()

    //GraphView
    private lateinit var graph : GraphView

    //Lat und Lng Values
    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var latlngDoubleArray: ArrayList<Double> = arrayListOf()

    private var distances: ArrayList<Double> = arrayListOf()
    private var accuracies: ArrayList<Float> = arrayListOf()
    private var location: Location = Location("dummyprovider")

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //initialize the variable and check for permission
        init()
        checkPermission()
        inputTime.setText("1000")

        rbHigh.setOnClickListener{
            accuracy = 1
        }
        rbBalanced.setOnClickListener{
            accuracy = 2
        }
        rbLow.setOnClickListener{
            accuracy = 3
        }

        //The start/stop button
        btnStart.setOnClickListener{
            if(!start){
                start = true
                btnHTTP.isEnabled = false
                sendToMap.isEnabled = false
                registerListener()
                startUpdates(inputTime.text.toString().toLong())
                btnStart.text = "Stop and Save"
            }else{
                start = false
                btnHTTP.isEnabled = true
                sendToMap.isEnabled = true
                unregisterListener()
                saveFile()
                stopUpdates()
                //drawOnGraph()
                btnStart.text = "Start"
            }
        }

        sendToMap.setOnClickListener{
            sendToMap()
        }

        btnHTTP.setOnClickListener{
            sendToHttp()
        }

        showMap.setOnClickListener{
            startActivity(Intent(this, ShowMapsActivity::class.java))
        }
    }

    private fun init(){
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        graph = findViewById<View>(R.id.graph) as GraphView

        acc = findViewById(R.id.ax)
        grv = findViewById(R.id.gx)
        gyr = findViewById(R.id.sx)
        lit = findViewById(R.id.lx)
        pre = findViewById(R.id.px)
        loc = findViewById(R.id.loc)

        grvCB = findViewById(R.id.grv_checkBox)
        gyrCB = findViewById(R.id.gyr_checkBox)
        accCB = findViewById(R.id.acc_checkBox)
        litCB = findViewById(R.id.light_checkBox)
        preCB = findViewById(R.id.pressure_checkBox)
        locCB = findViewById(R.id.location_checkBox)

        rbHigh = findViewById(R.id.rb_high)
        rbBalanced = findViewById(R.id.rb_balanced)
        rbLow = findViewById(R.id.rb_low)
        rg = findViewById(R.id.rg)

        inputTime = findViewById(R.id.time)

        btnStart = findViewById(R.id.start)
        sendToMap = findViewById(R.id.sendToMap)
        btnHTTP = findViewById(R.id.sendHttp)
        showMap = findViewById(R.id.showMap)
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
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1340,
            )
        }
    }

    private fun registerListener(){
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    private fun unregisterListener(){
        sensorManager.unregisterListener(this)
    }

/*
    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLocationListener(){
        if(loc_cb.isChecked) {
            var geocoder = Geocoder(this)
            val locationListener = LocationListener { location ->
                val geocodeResults =
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)

                if (geocodeResults.isNotEmpty()) {
                    loc.text = "Latitude: ${location.latitude}\n" +
                            "Longitude: ${location.longitude}\n" +
                            "Adresse: ${geocodeResults[0].getAddressLine(0)}\n"
                } else {
                    loc.text = "Latitude: ${location.latitude}\nLongitude: ${location.longitude}"

                }

                lat= location.latitude
                lng = location.longitude
            }
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1L,
                1f,
                locationListener
            )

            // Saving the location data into JSON file
            var jsonObjectLoc = JSONObject()
            jsonObjectLoc.put("Latitude", lat)
            jsonObjectLoc.put("Longitude", lng)
            jsonObjectLoc.put("Time", System.currentTimeMillis())
            if (jsonObjectLoc["Latitude"] != 0.0 && jsonObjectLoc["Longitude"] != 0.0) {
                jsonToArray(jsonObjectLoc)
            }
            if(lat != 0.0)
                latlngDoubleArray.add(lat)
            if(lng != 0.0)
                latlngDoubleArray.add(lng)
           // Log.e("Debug:", "Location got updated!")
        }
    }
*/

    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLocationCallback() {
        var tempAccuracy = 0.0F
        val geocoder = Geocoder(this)

        if (locCB.isChecked) {
            var locationRequest: LocationRequest = LocationRequest().setFastestInterval(20000).setInterval(60000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

            if(accuracy == 2){
                locationRequest = LocationRequest().setFastestInterval(20000).setInterval(60000).setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                Log.e("accuracy:", "PRIORITY_BALANCED_POWER_ACCURACY")
            } else if (accuracy == 3){
                locationRequest = LocationRequest().setFastestInterval(20000).setInterval(60000).setPriority(LocationRequest.PRIORITY_LOW_POWER)
                Log.e("accuracy:", "PRIORITY_LOW_POWER")
            }

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(location_result: LocationResult?) {
                    val geocodeResults = geocoder.getFromLocation(
                        location_result?.lastLocation!!.latitude,
                        location_result.lastLocation!!.longitude,
                        1,
                    )
                    super.onLocationResult(location_result)
                    loc.text = "Latitude: ${location_result.lastLocation.latitude}\n" +
                            "Longitude: ${location_result.lastLocation?.longitude}\n" +
                            "Speed: ${location_result.lastLocation?.speed}\n" +
                            "Accuracy: ${location_result.lastLocation?.accuracy}\n" +
                            "Address: ${geocodeResults[0].getAddressLine(0)}\n"

                    Log.e("lastLocation:", location_result.lastLocation.toString())
                    Log.e("location before:", location.toString())

                    distances.add(location_result.lastLocation.distanceTo(location).toDouble())
                    Log.e("distances:", location_result.lastLocation.distanceTo(location).toString())
                    location = location_result.lastLocation

                    lat = location_result.lastLocation.latitude
                    lng = location_result.lastLocation.longitude
                    tempAccuracy = location_result.lastLocation.accuracy
                    Log.e("tempAccuracy:", location_result.lastLocation.accuracy.toString())
                }
            }
            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )

            // Saving the location data into JSON file
            if (lat != 0.0 && lng != 0.0) {
                val jsonObjectLoc = JSONObject()
                jsonObjectLoc.put("Latitude", lat)
                jsonObjectLoc.put("Longitude", lng)
                jsonObjectLoc.put("Time", System.currentTimeMillis())
                //jsonToArray(jsonObjectLoc)
                latlngDoubleArray.add(lat)
                latlngDoubleArray.add(lng)
                accuracies.add(tempAccuracy)
                //Log.e("Debug:", "Location got updated!")
            }
        }
    }

    private fun sendToMap(){
        val intent = Intent(this, SendToMapsActivity::class.java)
        intent.putExtra("LENGTH", latlngDoubleArray.size)
        for((x, i) in latlngDoubleArray.withIndex()){
            intent.putExtra("L${x}", i)
        }
        Log.e("Debug:", latlngDoubleArray.toString())
        startActivity(intent)
    }


    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent?) {

        jsonObjectGrv = JSONObject()
        jsonObjectAcc = JSONObject()
        jsonObjectGyro = JSONObject()
        jsonObjectLit = JSONObject()
        jsonObjectPre = JSONObject()

        if(grvCB.isChecked) {
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

        if(accCB.isChecked) {
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

        if(gyrCB.isChecked) {
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

        if(preCB.isChecked) {
            when (event?.sensor?.type) {
                Sensor.TYPE_PRESSURE -> {
                    pre.text = "${"%.2f".format(event.values[0])} hPa"
                    jsonObjectPre.put("Sensor", "Pressure")
                    jsonObjectPre.put("Value", event.values[0])
                    jsonObjectPre.put("Time", System.currentTimeMillis())
                }
            }
        }

        if(litCB.isChecked) {
            when (event?.sensor?.type) {
                Sensor.TYPE_LIGHT -> {
                    lit.text = "${"%.2f".format(event.values[0])} lx"
                    jsonObjectLit.put("Sensor", "Light")
                    jsonObjectLit.put("Value", event.values[0])
                    jsonObjectLit.put("Time", System.currentTimeMillis())
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateSensorsData() {
        if(!jsonObjectGrv.isNull("X")&&!jsonObjectGrv.isNull("Y")&&!jsonObjectGrv.isNull("Z")){
            jsonToArray(jsonObjectGrv)
        }

        if(!jsonObjectAcc.isNull("X")&&!jsonObjectAcc.isNull("Y")&&!jsonObjectAcc.isNull("Z")){
            jsonToArray(jsonObjectAcc)
        }

        if(!jsonObjectGyro.isNull("X")&&!jsonObjectGyro.isNull("Y")&&!jsonObjectGyro.isNull("Z")){
            jsonToArray(jsonObjectGyro)
        }

        if(!jsonObjectPre.isNull("Value")){
            jsonToArray(jsonObjectPre)
        }

        if(!jsonObjectLit.isNull("Value")){
            jsonToArray(jsonObjectLit)
        }

        //Log.e("Debug:", "Sensors data got updated!")
    }

    private fun startUpdates(time: Long){
        scope.launch {
            while(true){
                //getLocationListener()
                getLocationCallback()
                updateSensorsData()
                delay(time)
                Log.e("Time:", System.currentTimeMillis().toString())
            }
        }
    }

    private fun stopUpdates(){
        scope.cancel()
        scope = MainScope()
    }

    private fun jsonToArray(jsonObject: JSONObject){
        jsonArray.put(jsonObject)
    }

   /* private fun calculateDistance(): FloatArray{
        distances = FloatArray(latlngDoubleArray.size/2)
        Log.e("doubleArray", latlngDoubleArray.toString())
        for(i in 0..latlngDoubleArray.size-4 step 2){
            Location.distanceBetween(
                latlngDoubleArray[i],
                latlngDoubleArray[i + 1],
                latlngDoubleArray[i + 2],
                latlngDoubleArray[i + 3],
                distances
            )
            Log.e("i", latlngDoubleArray[i].toString())
            Log.e("i + 1", latlngDoubleArray[i + 1].toString())
            Log.e("i + 2", latlngDoubleArray[i + 2].toString())
            Log.e("i + 3", latlngDoubleArray[i + 3].toString())

        }
        Log.e("distances", distances.contentToString())
        return distances
    }*/

    private fun drawOnGraph(){
        val dataPoints = arrayOfNulls<DataPoint>(distances.size)

        Log.e("distances.size", distances.size.toString())
        Log.e("accuracies.size", accuracies.size.toString())

        for(i in 0 until distances.size){
            //dataPoints[i] = DataPoint(distances[i], distances[i])
            Log.e("distances", distances[i].toString())
        }

        val highAccuracy = LineGraphSeries(dataPoints)
        //graph.addSeries(highAccuracy)
    }

    private fun saveFile(){

        val myExternalFile = File(getExternalFilesDir(""), "Output.json")

        try {
            val fileOutPutStream = FileOutputStream(myExternalFile)
            fileOutPutStream.write(jsonArray.toString().toByteArray())
            fileOutPutStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Toast.makeText(applicationContext, "data saved", Toast.LENGTH_SHORT).show()
    }

    private fun sendToHttp(){
        val client = OkHttpClient()
        val postBody = jsonArray.toString()
        val request = Request.Builder().url("https://haewo.free.beeceptor.com").post(postBody.toRequestBody()).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) throw IOException("Fehler: $response")
                Log.e("Res", response.body!!.string())
            }
        })
    }

    private suspend fun setTextOnMainThread(input: String){
        withContext(Dispatchers.Main){
            loc.text = input
        }
    }

}