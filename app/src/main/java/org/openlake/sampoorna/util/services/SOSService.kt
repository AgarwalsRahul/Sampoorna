package org.openlake.sampoorna.util.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.os.Build
import android.telephony.SmsManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import com.google.android.gms.location.FusedLocationProviderClient
import org.openlake.sampoorna.R
import org.openlake.sampoorna.presentation.MainActivity

class SOSService:LifecycleService() {
    private var latlong:String = ""
    private var sosMessage:String? =""
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var contactsListPreferences: SharedPreferences
    private lateinit var sosMessagePreferences: SharedPreferences
    companion object {
        var canSend = MutableLiveData<Boolean>()

    }
    @SuppressLint("MissingPermission", "VisibleForTests")
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startSOSForeground()
        }
        canSend.postValue(false)
        contactsListPreferences = this.getSharedPreferences("sosContacts", Context.MODE_PRIVATE)
        sosMessagePreferences = this.getSharedPreferences("sosMessage",Context.MODE_PRIVATE)
        if (contactsListPreferences.contains("contacts")) {
            fusedLocationProviderClient = FusedLocationProviderClient(this)
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                val lat = location.latitude.toString()
                val long = location.longitude.toString()
                this.latlong = "\nMy location is:\nhttp://maps.google.com/?q=$lat,$long"
            }
        if (sosMessagePreferences.contains("sosMessage")){
            sosMessage = sosMessagePreferences.getString("sosMessage","Hello , I am in danger")
        }
            canSend.observe(this, Observer {
                if (it) {
                    val contactSet: MutableSet<String>? =
                        contactsListPreferences.getStringSet("contacts", mutableSetOf())
                    val smsManager = SmsManager.getDefault()
                    contactSet?.forEach { contact ->
                        smsManager.sendTextMessage(contact, null, sosMessage+ "\n" + this.latlong, null, null)
                    }
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        val receiver = ScreenReceiver()
        registerReceiver(receiver,filter)
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startSOSForeground() {
        val notifChannelId = "Sampoorna"
        val channelName = "Background Service"
        val channel = NotificationChannel(
            notifChannelId, channelName, NotificationManager.IMPORTANCE_MIN)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE)!! as NotificationManager
        manager.createNotificationChannel(channel)
        val notificationBuild = NotificationCompat.Builder(this, notifChannelId)
        val notification = notificationBuild.setOngoing(true)
            .setContentTitle("You are protected")
            .setSmallIcon(R.mipmap.ic_womenlogo_round)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    override fun onDestroy() {
        if (MainActivity.SOSSwitch.value==true) {
            val broadcastIntent = Intent()
            broadcastIntent.action = "restart Service"
            broadcastIntent.setClass(this, ReactivateService::class.java)
            this.sendBroadcast(broadcastIntent)
        }
        super.onDestroy()
    }
}