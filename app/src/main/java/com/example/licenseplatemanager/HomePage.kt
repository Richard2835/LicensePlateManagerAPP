package com.example.licenseplatemanager

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class HomePage : AppCompatActivity(), ParkAdapter.OnItemClickListener {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private lateinit var dbNotifyRef: DatabaseReference
    private lateinit var valueListener: ValueEventListener
    private lateinit var deleteUser: ValueEventListener
    private lateinit var notificationListener: ValueEventListener
    private lateinit var recView: RecyclerView
    private lateinit var list: ArrayList<ParkData>

    private lateinit var notificationManager: NotificationManager

    //    private lateinit var notificationChannel: NotificationChannel
//    private lateinit var builder: Notification.Builder
    private val channelId = "com.example.licenseplatemanager"
    private val notificationId = 1

    @SuppressLint("ServiceCast")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)

        mAuth = FirebaseAuth.getInstance()
        db = Firebase.database.reference
        dbNotifyRef = Firebase.database.reference

        val user = Firebase.auth.currentUser
        if (user == null) {
            showToast("zlyhala autentifikácia účtu!")
            backToLoginPage()
        }

        recView = findViewById<RecyclerView>(R.id.recyclerID)
//        adapter = ParkAdapter(ArrayList())
        recView.layoutManager = LinearLayoutManager(this)
        recView.setHasFixedSize(true)
        list = arrayListOf<ParkData>()

        getDataFromDB(mAuth.currentUser!!.uid.toString())

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

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
            notifyIfDataHasChanged(mAuth.currentUser!!.uid.toString())
        }
    }

    override fun onStop() {
        super.onStop()
        //mAuth.signOut()
        if (mAuth.currentUser != null)
            notifyIfDataHasChanged(mAuth.currentUser!!.uid.toString())
    }

//    override fun onResume() {
//        super.onResume()
//        showToast("pokracujeme")
//    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mainmenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.notifIconID -> {
                startActivity(Intent(this,Notification_Page::class.java))
                finish()
                return true
            }
            R.id.logoutID -> {
                mAuth.signOut()
                showToast("odhlásenie bolo úspešné")
                backToLoginPage()
                return true
            }
            R.id.removeID -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Odstránenie užívateľa")
                    .setMessage(
                        "Naozaj chcete odstrániť vaše užívateľské konto?\n" +
                                "aktuálne prihlásený ako: ${mAuth.currentUser!!.email}"
                    )
                    .setPositiveButton("áno") { p0, p1 ->
                        var uid = mAuth.currentUser!!.uid.toString()
                        mAuth.currentUser!!.delete()
                            .addOnSuccessListener {
                                showToast("užívateľ úspešne odstránený")
                                deleteUserFromDB(uid)
                                backToLoginPage()
                            }.addOnFailureListener { exception ->
                                showToast("nastala chyba: " + exception.localizedMessage)
                            }
                    }
                    .setNegativeButton("nie") { dialog, which ->
                    }
                    .show()

//                Toast.makeText(this,"remove user", Toast.LENGTH_SHORT).show()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //    notifikacie, pre verzie nad Android 8 (Oreo), je nutne vztvorit notifikacny kanal
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Prijazdy vozidiel"
            val descriptionText = "Upozornovanie prijazdov vozidiel na parkoviska"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(licPlate: String, parkName: String, visited: String) {
        val intent = Intent(this, HomePage::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.logo)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(licPlate + " zaparkovalo")
            .setContentText("Toto vozidlo vošlo na parkovisko - " + parkName)
            .setLargeIcon(bitmap)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(licPlate + " -> " + parkName)
                    .setSummaryText(visited)
                    .bigText(
                        "Info o vozidle s evidenčným číslom * " + licPlate +
                                " *\n\t - bolo zaparkované na parkovisku \"" + parkName +
                                "\"\n" +
                                "\t v čase " + visited +
                                "\n-> pre bližšie info kliknite na túto notifikáciu <-"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }

    }

    private fun notifyIfDataHasChanged(uid: String) {
        notificationListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // odstranenie notifikacie, ak sa uzivatel odhlasil, nesedi s aktualne prihlasenym!
                if (mAuth.currentUser == null ||
                    mAuth.currentUser?.uid.toString().compareTo(uid) != 0
                ) {
                    dbNotifyRef.removeEventListener(notificationListener)
                    return
                }
                if (snapshot.exists()) {
                    // prehladavame parkoviska
                    for (i in snapshot.children) {
                        if (i.child(uid).exists()) {
                            // prehladavame pre dane uid jeho ecv
                            for (j in i.child(uid).children) {
                                // ak ecv existuje a ma notify hodnotu true -> notifikuj
                                if (j.child("notify").exists() &&
                                    j.child("notify").value == true
                                ) {
                                    sendNotification(
                                        j.key.toString(),
                                        i.key.toString(),
                                        j.child("lastVisited").value.toString()
                                    )
                                    // nastavenie hodnoty nazad na false (aby nechodili stale notif.)
                                    j.child("notify").ref.setValue(false)
                                    break
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("child Cancelled: " + error.toString())
            }


        }
        dbNotifyRef.addValueEventListener(notificationListener)

    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun backToLoginPage() {
        startActivity(Intent(this, Login_page::class.java))
        finish()
    }

    private fun getDataFromDB(uid: String) {
        valueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                // kotrola existencie referencie
                if (snapshot.exists()) {
                    var count: Int
                    count = 0
                    // prechadzanie poloziek (child-ov)
                    for (i in snapshot.children) {
                        // kontrola existencie UID v danom parkovisku
                        if (i.child(uid).exists()) {
                            count = 0
                            // vnoreny cyklus pre zistenie poctu znaciek daneho usera v danom parkovisku
                            for (j in i.child(uid).children) {
                                if (j.key.toString().compareTo("empty") == 0)
                                    break
                                else
                                    count += 1
                            }
                        }

                        list.add(ParkData(i.key.toString(), count.toString()))
                    }
                    recView.adapter = ParkAdapter(list, this@HomePage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.toString(), Toast.LENGTH_LONG).show()
            }
        }
        db.addValueEventListener(valueListener)
    }

    override fun onItemClick(position: Int) {
        val intent = Intent(this, LicPlate_Page::class.java)
        intent.putExtra("parkName", list[position].parkName)
        intent.putExtra("numPlates", list[position].plateNum)
        startActivity(intent)
        finish()
    }

    private fun deleteUserFromDB(uid: String) {
        deleteUser = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (i in snapshot.children) {
                        if (i.child(uid).exists())
                            i.child(uid).ref.removeValue()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.toString(), Toast.LENGTH_LONG).show()
            }
        }
//        db.addValueEventListener(deleteUser)
        db.addListenerForSingleValueEvent(deleteUser)
    }

    override fun onDestroy() {
        super.onDestroy()
        db.removeEventListener(valueListener)
//        db.removeEventListener(deleteUser)
    }

}