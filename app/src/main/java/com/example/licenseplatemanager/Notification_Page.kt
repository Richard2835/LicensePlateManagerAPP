package com.example.licenseplatemanager

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class Notification_Page : AppCompatActivity(), NotificationAdapter.OnItemClickListener {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private lateinit var valueListener: ValueEventListener
    private lateinit var notifRecView: RecyclerView
    private lateinit var list: ArrayList<NotificationData>
    private lateinit var emptyLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notification_page)

        var actionBar = getSupportActionBar()

        // showing the back button in action bar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        mAuth = FirebaseAuth.getInstance()
        db = Firebase.database.reference

        val user = Firebase.auth.currentUser
        if (user == null) {
            showToast("zlyhala autentifikácia účtu!")
            backToLoginPage()
        }

        emptyLayout = findViewById<LinearLayout>(R.id.emptyPageID)
        notifRecView = findViewById<RecyclerView>(R.id.notifRecViewID)
        notifRecView.layoutManager = LinearLayoutManager(this)
        notifRecView.setHasFixedSize(true)
        list = arrayListOf<NotificationData>()

        setEmptyContent(true)

        getDataFromDB(mAuth.currentUser!!.uid.toString())
    }

    public override fun onStart() {
        super.onStart()
        var username = findViewById<TextView>(R.id.userNameID)
        var mail = findViewById<TextView>(R.id.userMailID)

//        val user = Firebase.auth.currentUser
        val user = mAuth.currentUser
        if (user == null) {
            showToast("zlyhala autentifikácia účtu!")
            backToLoginPage()
        } else {
            mail.text = user!!.email
            username.text = user!!.displayName
//            showToast("vitajte späť!")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                backToHomePage()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onItemClick(position: Int) {
        if (mAuth.currentUser != null) {
            db.child(list[position].park.toString())
                .child(mAuth.currentUser?.uid.toString())
                .child(list[position].plate.toString())
                .child("seen").setValue(true)
        }
    }

    private fun getDataFromDB(uid: String) {
        valueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                // kotrola existencie referencie
                if (snapshot.exists()) {
                    // prechadzanie poloziek (child-ov)
                    for (i_park in snapshot.children) {
                        if (i_park.child(uid).exists()) {
                            // prechadzanie jednotlivych ecv pre nase UID
                            for (i_plate in i_park.child(uid).children) {
                                // ak sa najde nevidene, pridame do listu notifikacii
                                if (i_plate.child("seen").exists() &&
                                    i_plate.child("lastVisited").exists()) {
                                    if (i_plate.child("seen").value == false) {
                                        list.add(
                                            NotificationData(
                                                i_plate.key.toString(),
                                                i_park.key.toString(),
                                                i_plate.child("lastVisited").value.toString()
                                            )
                                        )
                                    }
                                }

                            }
                        }

                    }
                    if (list.isEmpty()) {
                        setEmptyContent(true)
                    } else {
                        setEmptyContent(false)
                        notifRecView.adapter = NotificationAdapter(list, this@Notification_Page)
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.toString(), Toast.LENGTH_LONG).show()
            }
        }
        db.addValueEventListener(valueListener)
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun setEmptyContent(isEmpty: Boolean) {
        if (isEmpty) {
            emptyLayout.visibility = View.VISIBLE
            notifRecView.visibility = View.INVISIBLE
        } else {
            emptyLayout.visibility = View.INVISIBLE
            notifRecView.visibility = View.VISIBLE
        }
    }

    private fun backToLoginPage() {
        startActivity(Intent(this, Login_page::class.java))
        finish()
    }

    private fun backToHomePage() {
        startActivity(Intent(this, HomePage::class.java))
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        db.removeEventListener(valueListener)
//        db.removeEventListener(deleteUser)
    }
}