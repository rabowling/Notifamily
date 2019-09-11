package com.example.reedbowling.notifamily

import com.google.android.gms.maps.model.LatLng
import java.io.Serializable

data class User(
        val type: String = "Child",
        val id: String = "",
        val name: String = "",
        var num: Int = 0,
        val parentId: String? = null,
        var completed: Int = 0,
        var deviceToken: String? = null,
        var lastLocation: LatLng? = null)

data class UserReceiver(
        val type: String = "Child",
        val id: String = "",
        val name: String = "",
        var num: Int = 0,
        val parentId: String? = null,
        var completed: Int = 0,
        var deviceToken: String? = null,
        var lastLocation: MyLatLng? = null)

fun UserReceiver.mapToUser() = User(type, id, name, num, parentId, completed, deviceToken, lastLocation?.toLatLng())

data class NotificationItem(
        val id: String,
        val user: String,
        val content: String,
        val location: LatLng?,
        val time: MyDate,
        val completed: Boolean,
        val notified: Boolean,
        var parentNotified: Boolean)

data class NotificationItemReceiver(
        val id: String = "",
        val user: String = "",
        val content: String = "",
        val location: MyLatLng? = null,
        val time: MyDate = MyDate(),
        val completed: Boolean = false,
        val notified: Boolean = false,
        var parentNotified: Boolean = false)

fun NotificationItemReceiver.mapToNotificationItem() =
        NotificationItem(id, user, content, location?.toLatLng(), time, completed, notified, parentNotified)

data class MyDate(val month: Int, val day: Int, val year: Int, val hour: Int, val minute: Int) : Serializable {

    constructor() : this(0,0,0,0,0)

    override fun toString(): String {
        val monthStr = (month + 1).toString()
        val yearStr = year.toString()
        val dayStr = if (day < 10) "0$day" else day.toString()
        val hourStr = if (hour < 10) "0$hour" else hour.toString()
        val minuteStr = if (minute < 10) "0$minute" else minute.toString()
        return "$hourStr:$minuteStr $monthStr/$dayStr/$yearStr"
    }
}

data class MyLatLng(val latitude: Double = 0.0, val longitude: Double = 0.0) {
    fun toLatLng() = LatLng(latitude, longitude)
}