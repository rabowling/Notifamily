package com.example.reedbowling.notifamily

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.dialogfragment_reauthenticate.view.*
import kotlinx.android.synthetic.main.fragment_settings.view.*

class SettingsFragment : Fragment() {

    private lateinit var mAuth : FirebaseAuth
    private lateinit var rootView : View
    private var isParent = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_settings, container, false)
        connectButtons()
        return rootView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        arguments?.let {
            isParent = it.getBoolean("IS_PARENT", false)
        }
    }

    private fun connectButtons() {
        if (isParent) {
            rootView.addChildButton.setOnClickListener { makeChildAccount() }
        } else {
            rootView.addChildButton.isEnabled = false
            rootView.addChildButton.alpha = 0.5f
        }
        rootView.settingUsername.setOnClickListener { reauthenticate(true) }
        rootView.settingPassword.setOnClickListener { reauthenticate(false) }
        rootView.logoutButton.setOnClickListener { logout() }
        rootView.deleteAccountButton.setOnClickListener { deleteAccount() }
    }

    private fun makeChildAccount() {
        val parentId = mAuth.currentUser?.uid
        val fragment = MakeChildDialogFragment().apply {
            arguments = Bundle().apply {
                putString("PARENT_ID", parentId)
            }
        }
        val manager = fragmentManager
        fragment.show(manager, "fragment_add_child")
    }

    private fun reauthenticate(username: Boolean) {
        val view = layoutInflater.inflate(R.layout.dialogfragment_reauthenticate, null)
        view.reauthSubmit.visibility = View.GONE
        val builder = AlertDialog.Builder(context)
        builder.setView(view)
                .setTitle("Re-authenticate")
                .setPositiveButton("Submit") {dialog, _ ->
                    val credential = EmailAuthProvider.getCredential(view.reauthEmail.text.toString(), view.reauthPass.text.toString())
                    mAuth.currentUser?.reauthenticate(credential)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            updateInfo(username)
                        } else {
                            Toast.makeText(this@SettingsFragment.context, "Failed to re-authenticate", Toast.LENGTH_SHORT).show()
                            dialog.cancel()
                        }
                    }
                }
                .create()
                .show()
    }

    private fun updateInfo(username: Boolean) {
        val userInput = EditText(context)
        val builder = AlertDialog.Builder(context)
        builder.setView(userInput)
                .setTitle(if (username) "Change Email" else "Change password")
                .setPositiveButton("Update") { dialog, _ ->
                    when (username) {
                        true -> {
                            val newEmail = userInput.text.toString()
                            if (isEmailValid(newEmail)) {
                                mAuth.currentUser?.updateEmail(newEmail)?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(this@SettingsFragment.context, "Success!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@SettingsFragment.context, "Failed to update.", Toast.LENGTH_SHORT).show()
                                    }
                                    dialog?.cancel()
                                }
                            }
                        }
                        false -> {
                            val newPassword = userInput.text.toString()
                            if (isPasswordValid(newPassword)) {
                                mAuth.currentUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to update.", Toast.LENGTH_SHORT).show()
                                    }
                                    dialog?.cancel()
                                }
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog?.cancel()
                }
                .create()
                .show()
    }

    private fun logout() {
        mAuth.signOut()
        val intent = Intent(context, LoginActivity::class.java).apply {
            putExtra("ACTION", "logout")
        }
        startActivity(intent)
    }

    private fun deleteAccount() {
        val confirmDialog = AlertDialog.Builder(this.context)
                .setMessage(R.string.setting_confirm_delete)
                .setCancelable(false)
                .setPositiveButton(R.string.yes) { _, _ ->
                    val fragment = ReauthenticateUserDialogFragment()
                    fragment.show(fragmentManager, "fragment_reauth")
                }
                .setNegativeButton(R.string.no) { dialog, _ ->
                    dialog?.cancel()
                }
        confirmDialog.create()
        confirmDialog.show()
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }
}
