package com.example.hae_wo


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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
    private lateinit var btnMap: Button
    private lateinit var btnHTTP: Button

    private var start = false
    
    //Checkboxes
    private lateinit var grv_cb: CheckBox
    private lateinit var gyr_cb: CheckBox
    private lateinit var acc_cb: CheckBox
    private lateinit var lit_cb: CheckBox
    private lateinit var pre_cb: CheckBox
    private lateinit var loc_cb: CheckBox

    //Coroutines
   private var scope = MainScope()

    //Lat und Lng Values
    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var latlngArray: ArrayList<Double> = arrayListOf()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //initialize the variable and check for permission
        init()
        checkPermission()
        inputTime.setText("1000")

        //The start/stop button
        btnStart.setOnClickListener{
            if(!start){
                start = true
                registerListener()
                startUpdates(inputTime.text.toString().toLong())
                btnStart.text = "Stop and Save"
            }else{
                start = false
                unregisterListener()
                saveFile()
                stopUpdates()
                btnStart.text = "Start"
            }
        }

        btnMap.setOnClickListener{
            showMap()
        }

        btnHTTP.setOnClickListener{
            sendToHttp()
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

        inputTime = findViewById(R.id.time)

        btnStart = findViewById(R.id.start)
        btnMap = findViewById(R.id.showMap)
        btnHTTP = findViewById(R.id.sendHttp)
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

    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLocation(){
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
                latlngArray.add(lat)
            if(lng != 0.0)
                latlngArray.add(lng)
            Log.e("Debug:", "Location got updated!")
        }
    }

    private fun showMap(){
        val intent = Intent(this, MapsActivity::class.java)
        intent.putExtra("LENGTH", latlngArray.size)
        for((x, i) in latlngArray.withIndex()){
            intent.putExtra("L${x}", i)
        }
        Log.e("Debug:", latlngArray.toString())
        startActivity(intent)
    }

    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent?) {

        jsonObjectGrv = JSONObject()
        jsonObjectAcc = JSONObject()
        jsonObjectGyro = JSONObject()
        jsonObjectLit = JSONObject()
        jsonObjectPre = JSONObject()

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

        Log.e("Debug:","Sensors data got updated!")
    }

    private fun startUpdates(time : Long){
        scope.launch {
            while(true){
                getLocation()
                updateSensorsData()
                delay(time)
                Log.e("Time:",System.currentTimeMillis().toString())
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
                Log.e("Res",response.body!!.string())
            }
        })
    }

    private suspend fun setTextOnMainThread(input: String){
        withContext(Dispatchers.Main){
            loc.text = input
        }
    }

}