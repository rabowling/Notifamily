package com.example.reedbowling.notifamily

import android.app.Application
import android.content.Context
import com.google.firebase.database.FirebaseDatabase

class MyApplication : Application() {
    private lateinit var thisUser : User

    private lateinit var mContext: Context

    override fun onCreate() {
        super.onCreate()
        mContext = this
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }

    fun setUser(user: User) {
        thisUser = user
        println(thisUser.type)
    }

    fun isParent() = thisUser.type == "Parent"
}