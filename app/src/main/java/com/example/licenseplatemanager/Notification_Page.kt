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

        // ukazanie sipky spat v toolbare
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        // ziskanie referencii Firebase
        mAuth = FirebaseAuth.getInstance()
        db = Firebase.database.reference

        // kontrola usera, ak nie je prihlaseny, vraciame sa na LoginPage
        val user = Firebase.auth.currentUser
        if (user == null) {
            showToast("zlyhala autentifikácia účtu!")
            backToLoginPage()
        }

        // nastavenie recyclerView pre polozky notifikacii neprecitanych
        emptyLayout = findViewById<LinearLayout>(R.id.emptyPageID)
        notifRecView = findViewById<RecyclerView>(R.id.notifRecViewID)
        notifRecView.layoutManager = LinearLayoutManager(this)
        notifRecView.setHasFixedSize(true)
        list = arrayListOf<NotificationData>()

        // na zaciatku nastavenie prazdneho contentu (miesto recycleru sa objavi prazdne logo)
        setEmptyContent(true)

        // nacitania vsetkych neprecitanych notifikacii k danemu kontu (useru)
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

    // obsluha toolbaru (sipky spat), po stlaceni na ikonu sa vraciame na HomePage
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                backToHomePage()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // po klikniti na ikonu X pri polozke notifikacii, je v DB zmenena hodnota tejto notifikacie
    // na videnu (seen = true), cim sa znovu aktualizuje cely recyclerView, a dana notifikacia,
    // sa uz nebude nachadzat v zozname
    override fun onItemClick(position: Int) {
        if (mAuth.currentUser != null) {
            db.child(list[position].park.toString())
                .child(mAuth.currentUser?.uid.toString())
                .child(list[position].plate.toString())
                .child("seen").setValue(true)
        }
    }

    // nacitanie a naplnenie recyclerVieweru o neprecitane notifikacie k danemu kontu (uid),
    // prehladava sa pri kazdej uzivatelovej ECV hodnota "seen", ktora ak je false, prida
    // tuto do listu
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
                                    i_plate.child("lastVisited").exists()
                                ) {
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

                    }   // ak je zoznam prazdny, nastavi sa neviditelnost recycleru a zaroven
                    // zviditelni sa prazdne logo, informujuce o ziadnych novych notifikaciach
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
        db.addValueEventListener(valueListener) // pridanie listeneru, pocuvania na zmeny z DB
    }

    // vytvorenie a vypisanie spravy Toast na zaklade parametra tejto funkcie
    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    // metoda prepinana layoutov recycleru (ak je prazny, zneviditelnime ho a nastavime prazdne
    // logo pozadia)
    private fun setEmptyContent(isEmpty: Boolean) {
        if (isEmpty) {
            emptyLayout.visibility = View.VISIBLE
            notifRecView.visibility = View.INVISIBLE
        } else {
            emptyLayout.visibility = View.INVISIBLE
            notifRecView.visibility = View.VISIBLE
        }
    }

    // zmena aktivity na LoginPage a zatvorenie aktualnej aktivity
    private fun backToLoginPage() {
        startActivity(Intent(this, Login_page::class.java))
        finish()
    }

    // zmena aktivity na HomePage a zatvorenie aktualnej aktivity
    private fun backToHomePage() {
        startActivity(Intent(this, HomePage::class.java))
        finish()
    }

    // odstranenie nepotrebnych listenerov
    override fun onDestroy() {
        super.onDestroy()
        db.removeEventListener(valueListener)
    }
}