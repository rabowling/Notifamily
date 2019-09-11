package com.example.reedbowling.notifamily

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.example.reedbowling.notifamily.NotificationListFragment.OnNotificationListFragmentInteractionListener
import com.google.android.gms.maps.model.LatLng

import kotlinx.android.synthetic.main.fragment_notification_item.view.*
import java.io.IOException
import java.lang.StringBuilder
import java.util.*

/**
 * [RecyclerView.Adapter] that can display a [NotificationItem] and makes a call to the
 * specified [OnNotificationListFragmentInteractionListener].
 */
class MyNotificationRecyclerViewAdapter(
        private val mValues: List<NotificationItem>?,
        private val mListener: OnNotificationListFragmentInteractionListener?,
        private val mContext: Context)
    : RecyclerView.Adapter<MyNotificationRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as NotificationItem
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onNotificationListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_notification_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues?.get(position)
        holder.mNotifContent.text = item?.content
        holder.mNotifTime.text = mContext.getString(R.string.notification_time, item?.time.toString())
        holder.mNotifCompleted.visibility = if (item?.completed == true) View.VISIBLE else View.GONE
        holder.mNotifNotified.text = mContext.getString(R.string.notification_notified, if (item?.notified == true) "Yes" else "No")
        holder.mNotifLocation.text = mContext.getString(R.string.notification_location, if (item?.location != null) getAddressFromLatLng(item.location) else "None")

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    private fun getAddressFromLatLng(latlng : LatLng) : String? {
        val coder = Geocoder(mContext, Locale.US)
        val addrList : List<Address>
        var addressString = ""
        try {
            addrList = coder.getFromLocation(latlng.latitude, latlng.longitude, 5)
            if (addrList == null || addrList.isEmpty()) {
                return null
            }
            val sb = StringBuilder()
            val address = addrList[0]
            for (i in 0..address.maxAddressLineIndex) {
                sb.append(address.getAddressLine(i)).append("\n")
            }
            addressString = sb.toString()
        } catch (e: IOException) {
            Log.e("GET_ADDRESS_FROM_LAT", "failed to convert latlng to address", e)
        }
        return addressString
    }

    override fun getItemCount(): Int = mValues!!.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mNotifContent: TextView = mView.notifContent
        val mNotifTime: TextView = mView.notifTime
        val mNotifLocation: TextView = mView.notifLocation
        val mNotifCompleted: ImageView = mView.imageView
        val mNotifNotified: TextView = mView.notifNotified

        override fun toString(): String {
            return super.toString() + " '" + mNotifTime.text + "'"
        }
    }
}
