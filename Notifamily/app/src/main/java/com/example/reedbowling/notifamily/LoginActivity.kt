package com.example.reedbowling.notifamily

import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView

import android.content.Intent
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import kotlinx.android.synthetic.main.activity_login.*

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */

    private lateinit var mAuth : FirebaseAuth
    private var isSignIn : Boolean = true
    private val database = FirebaseDatabase.getInstance()
    private val myApplication = MyApplication()
    private var fromNotif = false
    private var notif_id = ""

    override fun onStart() {
        super.onStart()

        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            val userRef = database.getReference("users").child(currentUser.uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {
                    val user = p0.getValue(UserReceiver::class.java)?.mapToUser()
                    myApplication.setUser(user!!)
                    enterApp()
                }

                override fun onCancelled(p0: DatabaseError) {
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mAuth = FirebaseAuth.getInstance()

        intent.apply {
            if (hasExtra("ACTION")) {
                when (getStringExtra("ACTION")) {
                    "logout" -> Toast.makeText(this@LoginActivity, R.string.action_logout_message, Toast.LENGTH_SHORT).show()
                    "delete" -> Toast.makeText(this@LoginActivity, R.string.action_delete_account, Toast.LENGTH_SHORT).show()
                }
            } else if (hasExtra("NOTIF_ID")) {
                fromNotif = true
                notif_id = getStringExtra("NOTIF_ID")
            }
        }
        // Set up the login form.
        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptAuthRequest(true)
                return@OnEditorActionListener true
            }
            false
        })

        email_sign_in_button.setOnClickListener { attemptAuthRequest(true) }
        email_register_button.setOnClickListener { attemptAuthRequest(false) }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptAuthRequest(signIn : Boolean) {

        // Reset errors.
        email.error = null
        password.error = null

        isSignIn = signIn

        // Store values at the time of the login attempt.
        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            if (signIn) {
                login_progress.visibility = View.VISIBLE
                mAuth.signInWithEmailAndPassword(emailStr, passwordStr).addOnCompleteListener(MyAuthOnCompleteListener())
            } else {
                login_progress.visibility = View.VISIBLE
                login_progress.animate()
                mAuth.createUserWithEmailAndPassword(emailStr, passwordStr).addOnCompleteListener(MyAuthOnCompleteListener())
            }
        }
    }

    inner class MyAuthOnCompleteListener : OnCompleteListener<AuthResult> {
        override fun onComplete(task: Task<AuthResult>) {
            if (task.isSuccessful) {
                if (!isSignIn) {
                    val user = User("Parent", task.result?.user?.uid!!, "", 0, null, 0)
                    database.getReference("users").child(user.id).setValue(user)
                    myApplication.setUser(user)
                    enterApp()
                } else {
                    enterAppLogin()
                }
            } else {
                val errorStr = if (isSignIn) getString(R.string.error_invalid_signin) else getString(R.string.error_invalid_register)
                Toast.makeText(this@LoginActivity, errorStr, Toast.LENGTH_SHORT).show()
                email.error = "Invalid"
                password.error = "Invalid"
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }

    private fun enterAppLogin() {
        val currentUser = mAuth.currentUser
        val userRef = database.getReference("users").child(currentUser!!.uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                val user = p0.getValue(UserReceiver::class.java)?.mapToUser()
                myApplication.setUser(user!!)
                enterApp()
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })
    }

    private fun enterApp() {
        val viewModel = ViewModelProviders.of(this).get(MyViewModel::class.java)
        viewModel.getChildren()
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("IS_PARENT", myApplication.isParent())
            if (fromNotif) putExtra("NOTIF_ID", notif_id)
        }
        login_progress.visibility = View.GONE
        startActivity(intent)
    }

    override fun onBackPressed() {
        if (mAuth.currentUser != null) {
            super.onBackPressed()
        }
    }
}