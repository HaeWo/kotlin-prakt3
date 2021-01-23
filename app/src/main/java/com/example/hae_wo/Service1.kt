package com.example.hae_wo

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.hardware.*
import android.location.Location
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.math.sqrt


class Service1 : Service() {

    // General needed objects
    private var isServiceStarted = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var fusedLocation: FusedLocationProviderClient? = null

    // Itent Stop Key
    private var ACTION_STOP_SERVICE: String = "STOPME"

    // Intent Data
    private var type = "";
    private var data0 = 15;
    private var data1 = 10;

    // Distance Aware
    private var lastLocation: Location? = null

    // Sleep Aware
    private var lastMovement = 0L

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ACTION_STOP_SERVICE == intent?.action) {
            stopService()
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent != null) {
            type = intent.extras?.getString("TYPE")!!
            data0 = intent.extras?.getInt("DATA0")!!
            data1 = intent.extras?.getInt("DATA1")!!

            val action = intent.action
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
            }
        } else {
            Log.d(
                "onStartCommand",
                "with a null intent. It has been probably restarted by the system."
            )
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun startService() {
        if (isServiceStarted) return
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Service1::lock").apply {
                    acquire(10 * 60 * 1000L /*10 minutes*/)
                }
            }

        if (type == ServiceType.SLEEP_AWARE.name || type == ServiceType.SLEEP_AWARE_MOTION.name) {
            val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            if (type == ServiceType.SLEEP_AWARE.name) {
                val aSensor: Sensor? =
                    sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
                if (aSensor == null) {
                    Toast.makeText(this, "LINEAR_ACCELERATION not supported", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val eHandler = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent?) {
                            // https://stackoverflow.com/a/14574992/5605489
                            val x = event?.values!![0]
                            val y = event.values[1]
                            val z = event.values[2]

                            val mAccelCurrent: Float = sqrt(x * x + y * y + z * z)
                            if (mAccelCurrent > 6) {
                                lastMovement = System.currentTimeMillis()
                                Log.d("onSensorChanged", mAccelCurrent.toString())
                            }
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                        }

                    }
                    sensorManager.registerListener(
                        eHandler,
                        aSensor,
                        SensorManager.SENSOR_DELAY_UI
                    )
                }
            } else if (type == ServiceType.SLEEP_AWARE_MOTION.name) {
                // https://developer.android.com/guide/topics/sensors/sensors_motion#sensors-motion-significant
                /*
                The significant motion sensor triggers an event each time significant motion is detected
                and then it disables itself. A significant motion is a motion that might lead to a change
                in the user's location; for example walking, biking, or sitting in a moving car.
                 */
                val mSensor: Sensor? =
                    sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)

                if (mSensor == null) {
                    Toast.makeText(this, "SIGNIFICANT_MOTION not supported", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val triggerEventListener = object : TriggerEventListener() {
                        override fun onTrigger(event: TriggerEvent?) {
                            lastMovement = System.currentTimeMillis()
                            sensorManager.requestTriggerSensor(this, mSensor)
                        }
                    }
                    mSensor.also { sensor ->
                        sensorManager.requestTriggerSensor(triggerEventListener, sensor)
                    }
                }
            }
        }

        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                val x = when (type) {
                    ServiceType.PERIODIC.name -> periodic()
                    ServiceType.DISTANCE.name -> distance()
                    ServiceType.SPEED.name -> distance()
                    ServiceType.SLEEP_AWARE.name -> sleepAware()
                    ServiceType.SLEEP_AWARE_MOTION.name -> sleepAware()
                    else -> stopService()
                }

                val delayTime: Long =
                    // data0 = Sleep Time
                    if (type != ServiceType.DISTANCE.name) data0.toLong()
                    // SPEED Awware: data0 = Max Speed filter: data1 = Distance filter
                    else ((data1.toLong() / 1000) / data0.toLong()) * 3600
                if (x) delay(delayTime * 1000)
                else delay(1 * 1000)
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun periodic(): Boolean {
        Log.d("periodic()", "call")
        val currentLocation =
            fusedLocation?.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
        currentLocation?.addOnCompleteListener { l -> sendToHttp(l.result) }
        currentLocation?.addOnFailureListener { Log.d("periodic()", "failed to fetch location") }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun distance(): Boolean {
        Log.d("distance()", "call")
        val currentLocation =
            fusedLocation?.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
        currentLocation?.addOnCompleteListener { l ->
            if (lastLocation != null) Log.d(
                "distance()",
                "distance to last messurement is " + l.result.distanceTo(
                    lastLocation
                )
            )
            if (lastLocation == null) {
                lastLocation = l.result
                sendToHttp(l.result)
            } else if (l.result.distanceTo(lastLocation) >= data1) {
                lastLocation = l.result
                sendToHttp(l.result)
            }
        }
        currentLocation?.addOnFailureListener { Log.d("distance()", "failed to fetch location") }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun sleepAware(): Boolean {
        Log.d("sleepAware()", "call")
        if (System.currentTimeMillis() - lastMovement > 20000) return false
        val currentLocation =
            fusedLocation?.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
        currentLocation?.addOnCompleteListener { l ->
            if (lastLocation != null) Log.d(
                "sleepAware()",
                "distance to last messurement is " + l.result.distanceTo(
                    lastLocation
                )
            )
            if (lastLocation == null) {
                lastLocation = l.result
                sendToHttp(l.result)
            } else if (l.result.distanceTo(lastLocation) >= data1) {
                lastLocation = l.result
                sendToHttp(l.result)
            }
        }
        currentLocation?.addOnFailureListener { Log.d("sleepAware()", "failed to fetch location") }
        return true
    }

    private fun sendToHttp(loc: Location?) {
        val client = OkHttpClient()
        val jsonData = JSONObject()
        if (loc != null) {
            jsonData.put("lat", loc.latitude)
            jsonData.put("lng", loc.longitude)
            jsonData.put("accuracy", loc.accuracy)
            jsonData.put("bearing", loc.bearing)
            jsonData.put("speed", loc.speed)
            jsonData.put("provider", loc.provider)
        }
        val request =
            Request.Builder().url("https://api.sensormap.ga/haewo/niklas/$type")
                .post(jsonData.toString().toRequestBody())
                .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("sendToHttp()", "Status: " + response.code)
                if (!response.isSuccessful) throw IOException("http error $response")
            }
        })
    }

    private fun stopService(): Boolean {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.d("stopService()", "Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
        return true
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "Hae!Wo?_SERVICE_CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
        val channel = NotificationChannel(
            notificationChannelId,
            "Hae!Wo? Service",
            NotificationManager.IMPORTANCE_HIGH
        ).let {
            it.description = "Hae!Wo? Service Channel"
            it.enableLights(true)
            it.lightColor = Color.RED
            it.enableVibration(true)
            it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            it
        }
        notificationManager.createNotificationChannel(channel)

        val stopSelf = Intent(this, Service1::class.java)
        stopSelf.action = ACTION_STOP_SERVICE
        val pendingIntent: PendingIntent = PendingIntent.getService(
            this,
            0,
            stopSelf,
            PendingIntent.FLAG_CANCEL_CURRENT
        );

        val builder: Notification.Builder =
            Notification.Builder(
                this,
                notificationChannelId
            )

        return builder
            .setContentTitle("Hae!Wo?")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_new)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()
    }
}