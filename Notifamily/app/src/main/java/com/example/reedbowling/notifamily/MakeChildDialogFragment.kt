package com.example.reedbowling.notifamily

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.dialogfragment_create_child.*

class MakeChildDialogFragment : DialogFragment() {

    private val mAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private lateinit var parentId : String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialogfragment_create_child, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            if (it.containsKey("PARENT_ID")) {
                parentId = it.getString("PARENT_ID")!!
            }
        }

        addChildDiscard.setOnClickListener { dismiss() }
        addChildSubmit.setOnClickListener {
            val email = addChildEmail.text.toString()
            val password = addChildPassword.text.toString()
            var cancel = false

            if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
                addChildPassword.error = getString(R.string.error_invalid_password)
                cancel = true
            }

            if (TextUtils.isEmpty(email)) {
                addChildEmail.error = getString(R.string.error_field_required)
                cancel = true
            } else if (!isEmailValid(email)) {
                addChildEmail.error = getString(R.string.error_invalid_email)
                cancel = true
            }

            if (!cancel) {
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(MyAuthOnCompleteListener())
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }

    inner class MyAuthOnCompleteListener : OnCompleteListener<AuthResult> {
        override fun onComplete(task: Task<AuthResult>) {
            if (task.isSuccessful) {
                val newUser = User("Child", task.result?.user?.uid!!, addChildName.text.toString(), 0, parentId, 0)
                database.getReference("users").child(newUser.id).setValue(newUser)
                dismiss()
            } else {
                val errorStr = getString(R.string.error_invalid_register)
                Toast.makeText(this@MakeChildDialogFragment.context, errorStr, Toast.LENGTH_SHORT).show()
                addChildEmail.error = "Invalid"
                addChildPassword.error = "Invalid"
            }
        }
    }
}