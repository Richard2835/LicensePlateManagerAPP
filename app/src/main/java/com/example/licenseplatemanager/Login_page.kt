package com.example.licenseplatemanager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Login_page : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)

        var actionBar = getSupportActionBar()

        // showing the back button in action bar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        mAuth = FirebaseAuth.getInstance()
        var email = findViewById<EditText>(R.id.emailID)
        var pass = findViewById<EditText>(R.id.passID)

        var loginBtn = findViewById<MaterialButton>(R.id.loginBtnID)
        var registerBtn = findViewById<MaterialButton>(R.id.registerBtnID)
        var loadingBar = findViewById<ProgressBar>(R.id.progressLoginID)

        val activeUser = Firebase.auth.currentUser
        if (activeUser != null) {
            val activeUserMail = activeUser!!.email
            Toast.makeText(this, "úspešne prihlásený ako $activeUserMail", Toast.LENGTH_SHORT)
                .show()
            startActivity(Intent(this, HomePage::class.java))
            finish()
        }

//        val emailTxt: String
//        val passTxt: String
//        emailTxt = email.text.toString().trim()
//        passTxt = pass.text.toString().trim()

        loginBtn.setOnClickListener() {
            loadingBar.visibility = View.VISIBLE
            if (chceckData(email,pass) == 0) {
                mAuth.signInWithEmailAndPassword(email.text.toString().trim(), pass.text.toString().trim())
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            loadingBar.visibility = View.GONE
                            val user = mAuth.currentUser
                            val userMail = user!!.email
                            Toast.makeText(this,"úspešne prihlásený ako $userMail", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this,HomePage::class.java))
                            finish()
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(baseContext, "chyba pri prihlasovaní!",
                                Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener { exception ->
                        Toast.makeText(applicationContext,exception.localizedMessage,Toast.LENGTH_LONG).show()
                        loadingBar.visibility = View.GONE
                    }
//                Toast.makeText(this,"uspesne prihlasenie", Toast.LENGTH_SHORT).show()
//                loadingBar.visibility = View.GONE
            }
            else {
                loadingBar.visibility = View.GONE
                Toast.makeText(this,"zle vyplnene polia!",Toast.LENGTH_SHORT).show()
            }
        }

        registerBtn.setOnClickListener() {
            startActivity(Intent(this,Register_page::class.java))
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                startActivity(Intent(this,MainActivity::class.java))
                finish()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    fun chceckData(email: EditText, pass: EditText): Int {

        var emailTxt: String
        var passTxt: String
        emailTxt = email.text.toString().trim()
        passTxt = pass.text.toString().trim()

        //        Kontrola emailu

        if(emailTxt.isEmpty()) {
            email.setError("Zadajte e-mail!")
            email.requestFocus()
            return 1
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(emailTxt).matches()) {
            email.setError("E-mail nespĺňa požiadavky!")
            email.requestFocus()
            return 1
        }

        //        Kontrola hesla

        if(passTxt.isEmpty()) {
            pass.setError("Pole nesmie byť prázdne!")
            pass.requestFocus()
            return 1
        }

        if(passTxt.length < 6) {
            pass.setError("Minimum znakov 6!")
            pass.requestFocus()
            return 1
        }

        return 0
    }


}