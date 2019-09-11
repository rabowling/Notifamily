package com.example.reedbowling.notifamily

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.dialogfragment_notification_info.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.IOException
import java.lang.StringBuilder
import java.lang.ref.WeakReference
import java.util.*

class NotificationInfoDialogFragment : DialogFragment() {

    private var isParent = false
    private var userId = ""
    private var notificationId = ""
    private var notificationContent = ""
    private lateinit var notificationTime : MyDate
    private var notificationLocation : LatLng? = null
    private var notificationCompleted = false
    private var notificationNotified = false
    private var parentNotifed = false
    private var prevChecked = false

    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialogfragment_notification_info, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            userId = it.getString("USER_ID", "")
            isParent = it.getBoolean("IS_PARENT", false)
            notificationId = it.getString("NOTIF_ID", "")
            notificationCompleted = it.getBoolean("NOTIF_COMPLETED", false)
            notificationTime = it.getSerializable("NOTIF_TIME") as MyDate
            notificationContent = it.getString("NOTIF_CONTENT", "")
            notificationNotified = it.getBoolean("NOTIF_NOTIFIED", false)
            parentNotifed = it.getBoolean("NOTIF_PARENT_NOTIFIED", false)
            if (it.containsKey("NOTIF_LOCATION")) {
                notificationLocation = it.getParcelable("NOTIF_LOCATION")
            }
        }

        prevChecked = notificationCompleted
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isParent) {
            parentInfoView.visibility = View.VISIBLE
            childInfoView.visibility = View.GONE
            infoContent.setText(notificationContent)
            infoTime.setText(notificationTime.toString())
            infoLocation.setText(if (notificationLocation != null) getAddressFromLatLng(notificationLocation!!) else "")
            infoMarkComplete.isChecked = notificationCompleted

            infoTime.setOnClickListener {
                val cal = Calendar.getInstance()
                val currentHour = cal.get(Calendar.HOUR_OF_DAY)
                val currentMinute = cal.get(Calendar.MINUTE)
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                val currentMonth = cal.get(Calendar.MONTH)
                val currentYear = cal.get(Calendar.YEAR)
                val picker = TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                    val picker2 = DatePickerDialog(context!!, DatePickerDialog.OnDateSetListener { _, year, month, day ->
                        notificationTime = MyDate(month, day, year, hour, minute)
                        infoTime.setText(notificationTime.toString())
                    }, currentYear, currentMonth, currentDay)
                    picker2.setTitle("Select Date")
                    picker2.show()
                }, currentHour, currentMinute, false)
                picker.setTitle("Select Time")
                picker.show()
            }

            infoLocation.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    MyAddressAsyncTask(WeakReference(context), object : MyAddressAsyncTask.LocationAsyncResponse {
                        override fun postLocationAcquired(result: LatLng?) {
                            notificationLocation = result
                        }
                    })
                }
            }

            infoSubmit.setOnClickListener {
                val newNotification = NotificationItem(notificationId, userId, infoContent.text.toString(), notificationLocation, notificationTime, infoMarkComplete.isChecked, notificationNotified, parentNotifed)
                database.getReference("notifications").child(newNotification.id).setValue(newNotification)
                Toast.makeText(this@NotificationInfoDialogFragment.context, "Updated notification", Toast.LENGTH_SHORT).show()
                dismiss()
            }

            infoDiscard.setOnClickListener {
                dismiss()
            }

            infoDelete.setOnClickListener {
                database.getReference("notifications").child(notificationId).removeValue()
                Toast.makeText(this@NotificationInfoDialogFragment.context, "Deleted notification", Toast.LENGTH_SHORT).show()
                database.getReference("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(p0: DataSnapshot) {
                        val user = p0.getValue(UserReceiver::class.java)?.mapToUser()
                        user!!.num--
                        database.getReference("users").child(userId).setValue(user)
                        dismiss()
                    }

                    override fun onCancelled(p0: DatabaseError) {
                        dismiss()
                    }
                })
            }
        } else {
            parentInfoView.visibility = View.GONE
            childInfoView.visibility = View.VISIBLE
            childInfoContent.text = notificationContent
            childInfoTime.text = getString(R.string.notification_time, notificationTime.toString())
            childInfoLocation.text = getString(R.string.notification_location, if (notificationLocation != null) getAddressFromLatLng(notificationLocation!!) else "None")
            childInfoMarkComplete.isChecked = notificationCompleted

            childInfoDiscard.setOnClickListener {
                dismiss()
            }

            childInfoSubmit.setOnClickListener {
                val newNotif = NotificationItem(notificationId, userId, notificationContent, notificationLocation, notificationTime, childInfoMarkComplete.isChecked, notificationNotified, parentNotifed)
                database.getReference("notifications").child(notificationId).setValue(newNotif)
                if (prevChecked != childInfoMarkComplete.isChecked) {
                    database.getReference("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(UserReceiver::class.java)?.mapToUser()
                            if (prevChecked) user!!.completed-- else user!!.completed++
                            database.getReference("users").child(userId).setValue(user)
                        }

                        override fun onCancelled(p0: DatabaseError) {
                        }
                    })
                }
                Toast.makeText(this@NotificationInfoDialogFragment.context, if (childInfoMarkComplete.isChecked) "Marked as completed" else "Marked as incomplete", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    private fun getAddressFromLatLng(latlng : LatLng) : String? {
        val coder = Geocoder(context, Locale.US)
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
        return addressString.trim()
    }
}