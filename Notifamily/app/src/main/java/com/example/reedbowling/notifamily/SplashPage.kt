package com.example.reedbowling.notifamily

import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_splash_page.*

class SplashPage : AppCompatActivity() {

    private val mAuth = FirebaseAuth.getInstance()
    private val myApplication = MyApplication()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_page)

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        if (intent.hasExtra("NOTIF_ID")) {
            exitSplashPage(true)
        }

        splashLayout.setOnClickListener { exitSplashPage(false) }

        Handler().postDelayed({
            fadeInButton()
        }, 4000)
    }

    private fun fadeInButton() {
        fadeText.animate().alpha(1f).setDuration(2000).start()
    }

    private fun exitSplashPage(fromNotif : Boolean) {
        var mIntent: Intent
        val notifId = if (fromNotif) intent.getStringExtra("NOTIF_ID") else ""
        if (mAuth.currentUser == null) {
            Log.e("SPLASH_PAGE", "Null user")
            mIntent = Intent(this, LoginActivity::class.java)
            if (fromNotif) {
                mIntent.putExtra("NOTIF_ID", notifId)
            }
            startActivity(mIntent)
        } else {
            Log.e("SPLASH_PAGE", "Non-null user")
            val userRef = database.getReference("users").child(mAuth.currentUser!!.uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {
                    val user = p0.getValue(UserReceiver::class.java)?.mapToUser()
                    myApplication.setUser(user!!)
                    mIntent = Intent(this@SplashPage, MainActivity::class.java).apply {
                        putExtra("IS_PARENT", myApplication.isParent())
                        if (fromNotif) putExtra("NOTIF_ID", notifId)
                    }
                    startActivity(mIntent)
                }

                override fun onCancelled(p0: DatabaseError) {
                    Log.e("DATABASE_TRANSACTION", "Failed database transaction:", p0.toException())
                }
            })
        }
    }
}
