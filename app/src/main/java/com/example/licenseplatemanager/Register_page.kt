package com.example.licenseplatemanager

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.regex.Pattern

class Register_page : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_page)

        var actionBar = getSupportActionBar()

        // showing the back button in action bar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        mAuth = FirebaseAuth.getInstance()
        db = Firebase.database.reference

        var user = findViewById<EditText>(R.id.userID)
        var email = findViewById<EditText>(R.id.emailID)
        var pass = findViewById<EditText>(R.id.passID)
        var passAgain = findViewById<EditText>(R.id.passAgainID)
        var enterBtn = findViewById<MaterialButton>(R.id.enterBtnID)
        var loadingBar = findViewById<ProgressBar>(R.id.progressRegID)

        enterBtn.setOnClickListener() {
            regAccount(user, email, pass, passAgain, loadingBar)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                backToLoginPage()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun regAccount(
        user: EditText,
        email: EditText,
        pass: EditText,
        passAgain: EditText,
        loading: ProgressBar
    ) {
        var userTxt: String
        var emailTxt: String
        var passTxt: String
        var passAgainTxt: String

        userTxt = user.text.toString().trim()
        emailTxt = email.text.toString().trim()
        passTxt = pass.text.toString().trim()
        passAgainTxt = passAgain.text.toString().trim()

        loading.visibility = View.VISIBLE
//        Kontrola mena uctu

        if (userTxt.isEmpty()) {
            user.setError("Zadajte názov konta!")
            user.requestFocus()
            return
        }

//        Kontrola emailu

        if (emailTxt.isEmpty()) {
            email.setError("Zadajte e-mail!")
            email.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailTxt).matches()) {
            email.setError("E-mail nespĺňa požiadavky!")
            email.requestFocus()
            return
        }

//        Kontrola hesla

        if (passTxt.isEmpty()) {
            pass.setError("Pole nesmie byť prázdne!")
            pass.requestFocus()
            return
        }

        if (passAgainTxt.isEmpty()) {
            passAgain.setError("Pole nesmie byť prázdne!")
            passAgain.requestFocus()
            return
        }

        if (passTxt.length < 6) {
            pass.setError("Minimum znakov 6!")
            pass.requestFocus()
            return
        }

        if (passAgainTxt.compareTo(passTxt) != 0) {
            pass.setError("Heslá sa nezhodujú!")
            passAgain.setError("Heslá sa nezhodujú!")
            pass.requestFocus()
            passAgain.requestFocus()
            return
        }


        loading.visibility = View.VISIBLE
        mAuth.createUserWithEmailAndPassword(emailTxt, passTxt)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    loading.visibility = View.GONE
//                    Log.d(TAG, "createUserWithEmail:success")
                    val user = mAuth.currentUser
                    if (user != null) {
                        val setUserName = userProfileChangeRequest {
                            displayName = userTxt
                        }
                        user!!.updateProfile(setUserName)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "registrácia úspešná", Toast.LENGTH_SHORT)
                                        .show()
//                                    backToLoginPage()
                                }
                            }
//                        Toast.makeText(this,"registrácia úspešná, meno sa však nepridalo!", Toast.LENGTH_SHORT).show()
                        addUserToDB()
                        toHomePage()
                    }
                } else {
                    Toast.makeText(this, "registrácia sa nepodarila!", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(applicationContext, exception.localizedMessage, Toast.LENGTH_LONG)
                    .show()
                loading.visibility = View.GONE
            }
    }

//    private fun backToLoginPage() {
//        startActivity(Intent(this,Login_page::class.java))
//        finish()
//    }

    private fun toHomePage() {
        startActivity(Intent(this, HomePage::class.java))
        finish()
    }

    private fun backToLoginPage() {
        startActivity(Intent(this,Login_page::class.java))
        finish()
    }

    private fun addUserToDB() {
        var addUser = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (i in snapshot.children) {
                    i.child(mAuth.currentUser!!.uid.toString()).child("empty").ref.setValue("null")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.toString(), Toast.LENGTH_LONG).show()
            }
        }
//        db.addValueEventListener(addUser)
        db.addListenerForSingleValueEvent(addUser)
    }
}