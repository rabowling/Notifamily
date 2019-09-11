package com.example.reedbowling.notifamily

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.lang.Exception
import java.lang.IllegalArgumentException

class MyLocationService : Service() {

    private val mAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private val LOCATION_INTERVAL : Long = 1000
    private val LOCATION_DISTANCE = 10f
    private var mLocationManager: LocationManager? = null
    private val TAG = "MY_LOCATION_SERVICE"

    private val mLocationListeners = arrayOf(
            MyLocationListener(LocationManager.GPS_PROVIDER),
            MyLocationListener(LocationManager.NETWORK_PROVIDER))

    inner class MyLocationListener(provider: String) : LocationListener {

        private var mLastLocation = Location(provider)

        override fun onLocationChanged(location: Location?) {
            Log.e(TAG, "New location update: $location")
            mLastLocation.set(location)
            if (mAuth.currentUser != null) {
                val lastLocation = LatLng(mLastLocation.latitude, mLastLocation.longitude)
                val updates = mutableMapOf<String, Any>()
                updates["lastLocation"] = lastLocation
                database.getReference("users").child(mAuth.currentUser!!.uid).updateChildren(updates)
            }
        }

        override fun onProviderDisabled(provider: String?) {
        }

        override fun onProviderEnabled(provider: String?) {
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        initLocationManager()

        try {
            mLocationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_INTERVAL,
                    LOCATION_DISTANCE,
                    mLocationListeners[1]
            )
        } catch (e : SecurityException) {
            Log.e(TAG, "fail to request location update", e)
        } catch (e : IllegalArgumentException) {
            Log.e(TAG, "network provider doesn't exist", e)
        }

        try {
            mLocationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_INTERVAL,
                    LOCATION_DISTANCE,
                    mLocationListeners[0]
            )
        } catch (e : SecurityException) {
            Log.e(TAG, "fail to request location update", e)
        } catch (e : IllegalArgumentException) {
            Log.e(TAG, "network provider doesn't exist", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mLocationManager != null) {
            for (listener in mLocationListeners) {
                try {
                    mLocationManager?.removeUpdates(listener)
                } catch (e : Exception) {
                    Log.e(TAG, "failed to remove listeners", e)
                }
            }
        }
    }

    private fun initLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
    }
}
