package com.example.reedbowling.notifamily

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.dialogfragment_reauthenticate.*

class ReauthenticateUserDialogFragment : DialogFragment() {

    private val mAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialogfragment_reauthenticate, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reauthSubmit.setOnClickListener {
            reauthEmail.error = null
            reauthPass.error = null
            val email = reauthEmail.text.toString()
            val password = reauthPass.text.toString()
            var cancel = false
            var focusView: View? = null

            if (TextUtils.isEmpty(email)) {
                reauthEmail.error = getString(R.string.error_field_required)
                focusView = reauthEmail
                cancel = true
            } else if (!isEmailValid(email)) {
                reauthEmail.error = getString(R.string.error_invalid_email)
                focusView = reauthEmail
                cancel = true
            }
            if (!isPasswordValid(password)) {
                reauthPass.error = getString(R.string.error_invalid_password)
                focusView = reauthEmail
                cancel = true
            }

            if (cancel) {
                focusView?.requestFocus()
            } else {
                val credential = EmailAuthProvider.getCredential(email, password)
                mAuth.currentUser?.reauthenticate(credential)?.addOnCompleteListener { task1 ->
                    if (task1.isSuccessful) {
                        val userId = mAuth.currentUser!!.uid
                        mAuth.currentUser?.delete()?.addOnCompleteListener { task2 ->
                            if (task2.isSuccessful) {
                                database.getReference("users").child(userId).removeValue()
                                val intent = Intent(context, LoginActivity::class.java).apply {
                                    putExtra("ACTION", "delete")
                                }
                                mAuth.signOut()
                                startActivity(intent)
                            } else {
                                Toast.makeText(context, R.string.setting_failed_delete, Toast.LENGTH_SHORT).show()
                                dismiss()
                            }
                        }
                    } else {
                        Toast.makeText(context, R.string.setting_failed_reauth, Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }
}