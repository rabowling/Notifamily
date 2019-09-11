package com.example.reedbowling.notifamily

import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessageService : FirebaseMessagingService() {

    private val mAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onNewToken(token: String?) {
        super.onNewToken(token)

        val user = mAuth.currentUser
        Log.e("FIREBASE_MESSAGE_SERVICE", "New token: $token")
        if (user != null) {
            updateUserDeviceToken(user, token)
        }
    }

    private fun updateUserDeviceToken(user: FirebaseUser, token: String?) {
        val updates = mutableMapOf<String, Any?>()
        updates["deviceToken"] = token
        database.getReference("users").child(user.uid).updateChildren(updates)
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        super.onMessageReceived(message)

        val id = message?.data?.get("NOTIF_ID")

        Log.e("NOTIF_ID", id)

        if (id != null) {
            val intent = Intent()
            intent.putExtra("NOTIF_ID", id)
            intent.putExtra("NOTIF_MESSAGE", message.notification?.body)
            intent.action = "NEW_MESSAGE"
            sendBroadcast(intent)
        }
    }
}