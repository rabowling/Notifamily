package com.example.reedbowling.notifamily

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.dialogfragment_create_notification.*
import java.lang.ref.WeakReference
import java.util.*

class AddNotificationDialogFragment : DialogFragment() {

    private var myDate : MyDate? = null
    private var content : String? = null
    private var location : LatLng? = null
    private lateinit var userId : String
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialogfragment_create_notification, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            userId = it.getString("USER")!!
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        add_time.setOnClickListener {
            val cal = Calendar.getInstance()
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val currentMinute = cal.get(Calendar.MINUTE)
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)
            val picker = TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                val picker2 = DatePickerDialog(context!!, DatePickerDialog.OnDateSetListener { _, year, month, day ->
                    myDate = MyDate(month, day, year, hour, minute)
                    add_time.setText(myDate.toString())
                }, currentYear, currentMonth, currentDay)
                picker2.setTitle("Select Date")
                picker2.show()
            }, currentHour, currentMinute, false)
            picker.setTitle("Select Time")
            picker.show()
        }

        add_content.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                content = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        add_location.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && add_location.text.isNotEmpty()) {
                MyAddressAsyncTask(WeakReference(context), object : MyAddressAsyncTask.LocationAsyncResponse {
                    override fun postLocationAcquired(result: LatLng?) {
                        location = result
                    }
                }).execute(add_location.text.toString())
            }
        }

        add_submit.setOnClickListener {
            if (myDate != null && content != null) {
                val ref = database.getReference("notifications").push()
                val newNotification = NotificationItem(ref.key!!, userId, content!!, location, myDate!!, false, false, false)
                ref.setValue(newNotification)
                database.getReference("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(UserReceiver::class.java)?.mapToUser()
                        user!!.num++
                        database.getReference("users").child(userId).setValue(user)
                        dismiss()
                    }

                    override fun onCancelled(p0: DatabaseError) {
                    }
                })
            } else {
                if (myDate == null) {
                    add_time.error = getString(R.string.error_field_required)
                }
                if (content == null) {
                    add_content.error = getString(R.string.error_field_required)
                }
            }
        }

        add_cancel.setOnClickListener {
            dismiss()
        }
    }
}