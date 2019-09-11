package com.example.reedbowling.notifamily

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_map.view.*
import java.io.IOException

class MapFragment : Fragment(), GoogleMap.OnInfoWindowClickListener {
    private lateinit var googleMap : GoogleMap
    private val database = FirebaseDatabase.getInstance()
    private lateinit var notificationList : MutableMap<Marker, NotificationItem?>
    private lateinit var currentIds : List<String>
    private var isParent = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_map, container, false)
        val mapView = rootView.mapView

        mapView.onCreate(savedInstanceState)
        mapView.onResume()

        arguments?.let {
            currentIds = it.getStringArrayList("CURRENT_IDS")!!
            isParent = it.getBoolean("IS_PARENT", false)
        }

        try {
            MapsInitializer.initialize(activity?.applicationContext)
        } catch (e : Exception) {
            println(e)
        }

        mapView.getMapAsync(MyMapCallback())

        return rootView
    }

    override fun onInfoWindowClick(marker: Marker?) {
        val item = notificationList[marker]
        if (item != null) {
            val fragment = NotificationInfoDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("USER_ID", item.user)
                    putBoolean("IS_PARENT", isParent)
                    putString("NOTIF_ID", item.id)
                    putString("NOTIF_CONTENT", item.content)
                    putSerializable("NOTIF_TIME", item.time)
                    putBoolean("NOTIF_COMPLETED", item.completed)
                    putBoolean("NOTIF_NOTIFIED", item.notified)
                    if (item.location != null) {
                        putParcelable("NOTIF_LOCATION", item.location)
                    }
                }
            }
            fragment.show(fragmentManager, "fragment_notification_info")
        }
    }

    inner class MyMapCallback : OnMapReadyCallback {
        override fun onMapReady(p0: GoogleMap) {
            googleMap = p0

            googleMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
                override fun getInfoContents(marker: Marker?): View {
                    val layout = LinearLayout(context)
                    layout.orientation = LinearLayout.VERTICAL

                    val title = TextView(activity?.applicationContext)
                    title.text = marker?.title
                    title.gravity = Gravity.START
                    title.typeface = Typeface.DEFAULT_BOLD

                    val snippet = TextView(activity?.applicationContext)
                    snippet.setTextColor(Color.GRAY)
                    snippet.text = marker?.snippet

                    layout.addView(title)
                    layout.addView(snippet)

                    return layout
                }

                override fun getInfoWindow(p0: Marker?): View? {
                    return null
                }
            })

            if (currentIds.isNotEmpty()) {
                notificationList = mutableMapOf()
                database.getReference("notifications").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.run {
                            val notificationItems = children.mapNotNull {
                                it.getValue(NotificationItemReceiver::class.java)
                            }
                            var avgLat = 0.0
                            var avgLng = 0.0
                            var count = 0
                            for (notificationItemRec in notificationItems) {
                                val notificationItem = notificationItemRec.mapToNotificationItem()
                                if (currentIds.contains(notificationItem.user) && notificationItem.location != null) {
                                    val location = notificationItem.location
                                    count++
                                    avgLat += location.latitude
                                    avgLng += location.longitude
                                    val marker = MarkerOptions()
                                            .position(location)
                                            .title(notificationItem.content)
                                            .snippet("Time: ${notificationItem.time}\nNotified: ${notificationItem.notified}")
                                    notificationList[googleMap.addMarker(marker)] =  notificationItem
                                }
                            }
                            if (count > 0) {
                                avgLat /= count
                                avgLng /= count
                                val camPos = CameraPosition(LatLng(avgLat, avgLng), 9.875779f, 0.0f, 0.0f)
                                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos))
                            } else {
                                val camPos = CameraPosition(getLatLngFromAddress(getString(R.string.default_location)), 9.875779f, 0.0f, 0.0f)
                                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos))
                            }
                        }
                    }

                    override fun onCancelled(p0: DatabaseError) {
                        Log.e("DATABASE TRANSACTION", "Failed database transaction", p0.toException())
                    }
                })
            }
            googleMap.setOnInfoWindowClickListener(this@MapFragment)

            if (context?.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.isMyLocationEnabled = true
            }
        }
    }

    private fun getLatLngFromAddress(address : String) : LatLng? {
        val coder = Geocoder(context)
        val addrList : List<Address>?
        var location : LatLng? = null
        try {
            addrList = coder.getFromLocationName(address, 5)
            if (addrList == null || addrList.isEmpty()) {
                return null
            }
            location = LatLng(addrList[0].latitude, addrList[0].longitude)
        } catch (e : IOException) {
            Log.e("GET_LAT_FROM_ADDRESS", "failed to convert address to latlng", e)
        }
        return location
    }
}