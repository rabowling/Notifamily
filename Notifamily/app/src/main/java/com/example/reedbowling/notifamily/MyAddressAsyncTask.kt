package com.example.reedbowling.notifamily

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import java.lang.ref.WeakReference

class MyAddressAsyncTask(private val weakContext : WeakReference<Context?>, private val delegate : LocationAsyncResponse) : AsyncTask<String, Nothing, LatLng?>() {

    interface LocationAsyncResponse {
        fun postLocationAcquired(result: LatLng?)
    }

    override fun doInBackground(vararg params: String?): LatLng? {
        val address = params[0]
        val coder = Geocoder(weakContext.get())
        val addrList : List<Address>?
        var location2 : LatLng? = null
        try {
            addrList = coder.getFromLocationName(address, 5)
            if (addrList == null || addrList.isEmpty()) {
                return null
            }
            location2 = LatLng(addrList[0].latitude, addrList[0].longitude)
        } catch (e : IOException) {
            Log.e("GET_LAT_FROM_ADDRESS", "failed to convert address to latlng", e)
        }
        return location2
    }

    override fun onPostExecute(result: LatLng?) {
        delegate.postLocationAcquired(result)
    }
}