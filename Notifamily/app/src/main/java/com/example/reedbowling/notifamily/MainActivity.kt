package com.example.reedbowling.notifamily

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList

class MainActivity : AppCompatActivity(), ChildListFragment.OnChildListFragmentInteractionListener, NotificationListFragment.OnNotificationListFragmentInteractionListener {

    /*
        Login Credentials:
            Example Parent: email = admin@example.com, password = admin2
            Example Child Jimmy: email = adminchild@example.com, password = admin1
            Example Child Samantha: email = adminchild2@example.com, password = admin1

        Notifications are via cloud messaging. When the app is in the background, the notification
        appears like a normal android push notification. When the app is in the foreground, a
        snackbar appears displaying the message and a button to take you to the notification info
     */

    private var mAuth = FirebaseAuth.getInstance()
    private var isParent = false
    private val database = FirebaseDatabase.getInstance()
    enum class CurrentFragment {
        CHILD_LIST, NOTIFICATION_LIST, MAP, SETTINGS
    }

    private var receiver: BroadcastReceiver? = null

    private var currentFragment = CurrentFragment.CHILD_LIST

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        val transaction = supportFragmentManager.beginTransaction()
        lateinit var fragment : Fragment

        when (item.itemId) {
            R.id.navigation_home -> {
                fragment = if (isParent) ChildListFragment.newInstance(1) else NotificationListFragment.newInstance(1, mAuth.currentUser!!.uid)
                floatingActionButton.hide()
                title = (if (isParent) getString(R.string.fragment_title_child_list) else getString(R.string.title_notifications))
                currentFragment = if (isParent) CurrentFragment.CHILD_LIST else CurrentFragment.NOTIFICATION_LIST
                transaction.replace(R.id.fragmentContainer, fragment).commit()
            }
            R.id.navigation_map -> {
                val currentIds = mutableListOf<String?>()
                floatingActionButton.hide()
                currentFragment = CurrentFragment.MAP
                title = getString(R.string.title_map)
                if (isParent) {
                    database.getReference("users").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (child in snapshot.children) {
                                val user = child.getValue(UserReceiver::class.java)?.mapToUser()
                                if (user?.parentId == mAuth.currentUser?.uid) {
                                    currentIds.add(user?.id)
                                }
                            }
                            fragment = MapFragment().apply {
                                arguments = Bundle().apply {
                                    putStringArrayList("CURRENT_IDS", currentIds as ArrayList<String>)
                                    putBoolean("IS_PARENT", isParent)
                                }
                            }
                            transaction.replace(R.id.fragmentContainer, fragment).commit()
                        }

                        override fun onCancelled(p0: DatabaseError) {
                        }
                    })
                } else {
                    currentIds.add(mAuth.currentUser?.uid)
                    fragment = MapFragment().apply {
                        arguments = Bundle().apply {
                            putStringArrayList("CURRENT_IDS", currentIds as ArrayList<String>)
                            putBoolean("IS_PARENT", isParent)
                        }
                    }
                    transaction.replace(R.id.fragmentContainer, fragment).commit()
                }
            }
            R.id.navigation_settings -> {
                fragment = SettingsFragment().apply {
                    arguments = Bundle().apply {
                        putBoolean("IS_PARENT", isParent)
                    }
                }
                floatingActionButton.hide()
                currentFragment = CurrentFragment.SETTINGS
                title = getString(R.string.setting_title)
                transaction.replace(R.id.fragmentContainer, fragment).commit()
            }
        }
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(this, MyLocationService::class.java))

        receiver = MyBroadcastReceiver()
        val filter = IntentFilter()
        filter.addAction("NEW_MESSAGE")

        registerReceiver(receiver, filter)

        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
            if (it.isSuccessful) {
                val updates = mutableMapOf<String, Any>()
                updates["deviceToken"] = it.result!!.token
                database.getReference("users").child(mAuth.currentUser!!.uid).updateChildren(updates)
            }
        }

        isParent = intent.getBooleanExtra("IS_PARENT", false)

        val transaction = supportFragmentManager.beginTransaction()

        floatingActionButton.hide()
        val fragment = if (isParent) ChildListFragment.newInstance(1) else NotificationListFragment.newInstance(1, mAuth.currentUser!!.uid)
        title = (if (isParent) getString(R.string.fragment_title_child_list) else getString(R.string.title_notifications))

        transaction.replace(R.id.fragmentContainer, fragment).commit()

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        /* If we came from a notification */
        if (intent.hasExtra("NOTIF_ID")) {
            database.getReference("notifications").child(intent.getStringExtra("NOTIF_ID")).addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val item = snapshot.getValue(NotificationItemReceiver::class.java)?.mapToNotificationItem()
                    onNotificationListFragmentInteraction(item)
                }

                override fun onCancelled(p0: DatabaseError) {
                }
            })
        }
    }

    override fun onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver)
            receiver = null
        }

        super.onDestroy()
    }

    /* Notifications when app is in foreground */
    inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e("ON_RECEIVE", "got message")
            val snackbar = Snackbar.make(container, intent?.getStringExtra("NOTIF_MESSAGE") as CharSequence, Snackbar.LENGTH_LONG)
            snackbar.setAction("Info") {
                database.getReference("notifications").child(intent.getStringExtra("NOTIF_ID")).addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val item = snapshot.getValue(NotificationItemReceiver::class.java)?.mapToNotificationItem()
                        onNotificationListFragmentInteraction(item)
                    }

                    override fun onCancelled(p0: DatabaseError) {
                    }
                })
            }
            snackbar.show()
        }
    }

    override fun onChildListFragmentInteraction(item: User?) {
        floatingActionButton.show()
        title = item?.name
        currentFragment = CurrentFragment.NOTIFICATION_LIST
        val fragment = NotificationListFragment.newInstance(1, item!!.id)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        floatingActionButton.setOnClickListener {
            val fragment2 = AddNotificationDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("USER", item.id)
                }
            }
            val manager = supportFragmentManager
            fragment2.show(manager, "fragment_add_notif")
        }
        transaction.commit()
    }

    override fun onNotificationListFragmentInteraction(item: NotificationItem?) {
        val fragment = NotificationInfoDialogFragment().apply {
            arguments = Bundle().apply {
                putString("USER_ID", item?.user)
                putBoolean("IS_PARENT", isParent)
                putString("NOTIF_ID", item?.id)
                putString("NOTIF_CONTENT", item?.content)
                putSerializable("NOTIF_TIME", item?.time)
                putBoolean("NOTIF_COMPLETED", item!!.completed)
                putBoolean("NOTIF_NOTIFIED", item.notified)
                putBoolean("NOTIF_PARENT_NOTIFIED", item.parentNotified)
                if (item.location != null) {
                    putParcelable("NOTIF_LOCATION", item.location)
                }
            }
        }
        fragment.show(supportFragmentManager, "fragment_notification_info")
    }

    override fun onBackPressed() {
        if (isParent && currentFragment == CurrentFragment.NOTIFICATION_LIST) {
            val fragment = ChildListFragment.newInstance(1)
            floatingActionButton.hide()
            title = (if (isParent) getString(R.string.fragment_title_child_list) else getString(R.string.title_notifications))
            currentFragment = CurrentFragment.CHILD_LIST
            supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
        }
    }
}
