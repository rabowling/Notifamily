package com.example.reedbowling.notifamily

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MyViewModel : ViewModel() {
    private lateinit var childs : MutableLiveData<List<User>>
    private lateinit var notifications : MutableLiveData<List<NotificationItem>>
    private var mAuth = FirebaseAuth.getInstance()

    private val database = FirebaseDatabase.getInstance()

    fun getChildren() : LiveData<List<User>> {
        if (!::childs.isInitialized) {
            childs = MutableLiveData()
            makeChildDatabaseConnection()
        }
        return childs
    }

    fun getNotifications() : LiveData<List<NotificationItem>> {
        if (!::notifications.isInitialized) {
            notifications = MutableLiveData()
            makeNotifDatabaseConnection()
        }
        return notifications
    }

    private fun makeChildDatabaseConnection() {
        val usersRef = database.getReference("users")
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.run {
                    val userFromFirebase = children.mapNotNull {
                        it.getValue(UserReceiver::class.java)
                    }
                    childs.postValue(userFromFirebase.map(UserReceiver::mapToUser).filter {
                        it.parentId != null && it.parentId == mAuth.currentUser?.uid
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE TRANSACTION", "Failed transaction", error.toException())
            }
        })
    }

    private fun makeNotifDatabaseConnection() {
        val notificationsRef = database.getReference("notifications")
        notificationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.run {
                    val notificationsFromFirebase = children.mapNotNull {
                        it.getValue(NotificationItemReceiver::class.java)
                    }
                    notifications.postValue(notificationsFromFirebase.map(NotificationItemReceiver::mapToNotificationItem).reversed())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE TRANSACTION", "Failed transaction", error.toException())
            }
        })
    }
}