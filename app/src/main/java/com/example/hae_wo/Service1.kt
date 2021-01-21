package com.example.hae_wo

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException


class Service1 : Service() {
    private var ACTION_STOP_SERVICE: String = "STOPME"
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private var fusedLocation: FusedLocationProviderClient? = null
    private var type = "";
    private var data0 = 15;
    private var data1 = 10;

    override fun onBind(intent: Intent): IBinder? {
        println("Some component want to bind with the service")
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ACTION_STOP_SERVICE == intent?.action) {
            println("called to cancel service");
            stopService()
            stopSelf()
            return START_NOT_STICKY
        }

        println("onStartCommand executed with startId: $startId")
        if (intent != null) {
            type = intent.extras?.getString("TYPE")!!
            data0 = intent.extras?.getInt("DATA0")!!
            data1 = intent.extras?.getInt("DATA1")!!

            val action = intent.action
            println("using an intent with action $action")
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> println("This should never happen. No action in the received intent")
            }
        } else {
            println(
                "with a null intent. It has been probably restarted by the system."
            )
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        println("The service has been created".toUpperCase())
        startForeground(1, createNotification())
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
    }

    private fun startService() {
        if (isServiceStarted) return
        println("Starting the foreground service task")
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Service1::lock").apply {
                    acquire()
                }
            }

        // we're starting a loop in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                launch(Dispatchers.IO) {
                    when (type) {
                        ServiceType.PERIODIC.name -> periodic()
                        ServiceType.DISTANCE.name -> distance()
                        ServiceType.SPEED.name -> speed()
                        ServiceType.SLEEP_AWARE.name -> sleepAware()
                        else -> { // Note the block
                            stopService()
                        }
                    }
                }
                delay(data0.toLong() * 1000)
            }
            println("End of the loop for the service")
        }
    }


    @SuppressLint("MissingPermission")
    private fun periodic() {
        println("periodic()")
        val currentLocation = fusedLocation?.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
        currentLocation?.addOnCompleteListener { l -> sendToHttp(l.result) }
        currentLocation?.addOnFailureListener { println("FAILED!") }
    }

    private fun distance() {

    }

    private fun speed() {

    }

    private fun sleepAware() {

    }

    private fun sendToHttp(loc: Location?) {
        println("Send HTTP")
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
                println("failed http")
            }

            override fun onResponse(call: Call, response: Response) {
                println("ok http")
                if (!response.isSuccessful) throw IOException("Fehler: $response")
                Log.e("Res", response.body!!.string())
            }
        })
    }

    private fun stopService() {
        println("Stopping the foreground service")
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            println("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "Hae!Wo?_SERVICE_CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        }

        val stopSelf = Intent(this, Service1::class.java)
        stopSelf.action = ACTION_STOP_SERVICE
        val pendingIntent: PendingIntent = PendingIntent.getService(
            this,
            0,
            stopSelf,
            PendingIntent.FLAG_CANCEL_CURRENT
        );

        val builder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                this,
                notificationChannelId
            ) else Notification.Builder(this)

        return builder
            .setContentTitle("Hae!Wo?")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_new)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()
    }
}