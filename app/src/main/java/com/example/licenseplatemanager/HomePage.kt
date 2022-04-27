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
    //    deklaracia potrebnych premennych
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private lateinit var dbNotifyRef: DatabaseReference
    private lateinit var valueListener: ValueEventListener
    private lateinit var deleteUser: ValueEventListener
    private lateinit var notificationListener: ValueEventListener
    private lateinit var recView: RecyclerView
    private lateinit var list: ArrayList<ParkData>
    private lateinit var notificationManager: NotificationManager
    private val channelId = "com.example.licenseplatemanager"
    private val notificationId = 1

    //    onCreate metoda, kde nastavime niektore z premennych (Firebase, recycerView a notifikacie)
//    hlavna metoda pri otvoreni novej instancie tejto triedy
    @SuppressLint("ServiceCast")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)  // nastavenie Layoutu pre tuto aktivitu

        // ziskanie referencii Firebase
        mAuth = FirebaseAuth.getInstance()
        db = Firebase.database.reference
        dbNotifyRef = Firebase.database.reference

        // zistenie aktualne prihlaseneho usera, ak neni, vrati sa na LoginPage
        val user = Firebase.auth.currentUser
        if (user == null) {
            showToast("zlyhala autentifikácia účtu!")
            backToLoginPage()
        }

        // nastavenie recyclerView aj s adapterom
        recView = findViewById<RecyclerView>(R.id.recyclerID)
        recView.layoutManager = LinearLayoutManager(this)
        recView.setHasFixedSize(true)
        list = arrayListOf<ParkData>()

        // naplnenie recyclerViewera dostupnymi parkoviskami
        getDataFromDB(mAuth.currentUser!!.uid.toString())

        // zapnutie notifikacii
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

    }

    // onStart metoda, ak sa opat vratime k povodnej instancii tejto triedy
    public override fun onStart() {
        super.onStart()
        var username = findViewById<TextView>(R.id.userNameID)
        var mail = findViewById<TextView>(R.id.userMailID)

        // zistenie pritomnosti prihlaseneho, ak je nastavime jeho meno a email do Layoutu a
        // zapneme notifikacie, inak sa vratime nazad na LoginPage
        val user = mAuth.currentUser
        if (user == null) {
            showToast("zlyhala autentifikácia účtu!")
            backToLoginPage()
        } else {
            mail.text = user!!.email
            username.text = user!!.displayName
            notifyIfDataHasChanged(mAuth.currentUser!!.uid.toString())
        }
    }

    // po zavreti aktivity (minimalizovanie okna) bezia na pozadi notifikacie
    override fun onStop() {
        super.onStop()
        //mAuth.signOut()
        if (mAuth.currentUser != null)
            notifyIfDataHasChanged(mAuth.currentUser!!.uid.toString())
    }

    // vytvorenie menu toolbaru
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mainmenu, menu)
        return true
    }

    // urcenie vramci menu, co sa ma stat po kliknuti na polozku
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // notifikacna inkona, zobrazi aktivitu Notification_Page s nevidenymi notifikaciami
            R.id.notifIconID -> {
                startActivity(Intent(this, Notification_Page::class.java))
                finish()
                return true
            }
            // logout button odhlasi uzivatela a vrati sa na LoginPage
            R.id.logoutID -> {
                mAuth.signOut()
                showToast("odhlásenie bolo úspešné")
                backToLoginPage()
                return true
            }
            // odstranenie uzivatela - otvori sa dialogove okno, ci si tak ozaj praje
            // ak ano => uzivatel je zmazany z DB pomocou funkcie deleteUserFromDB(uid)
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

    //    notifikacie, pre verzie nad Android 8 (Oreo), je nutne vytvorit notifikacny kanal
    //    metoda prebrana z oficialnej stranky https://developer.android.com/
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

    // vytvorenie a poslanie notifikacie, kde pomocou parametrov vypiseme potrebne veci
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

    // metoda pocuvania na zmeny v databaze, kde ak nejaka nastane a tyka sa aktualne prihlaseneho
    // usera, vola sa funkcia sendNotification(), cim sa vytvori notifikacia
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


        }   // pridanie listeneru pre pocuvanie na pozadi
        dbNotifyRef.addValueEventListener(notificationListener)

    }

    // pomocna funkcia vypisania Toastu, kde bude vypisana sprava z parametru tejto metody
    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    // otvorenie aktivity Login_page a nasledne zatvorenie (zrusenie) tejto aktivity
    private fun backToLoginPage() {
        startActivity(Intent(this, Login_page::class.java))
        finish()
    }

    // metoda, ktora pre konkretne uid naplna recyclerView pomocou adaptera do Lauout itemov
    private fun getDataFromDB(uid: String) {
        valueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()    // na zaciatku sa premaze list
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
                        // pridanie polozky do listu novy objekt data triedy parkovisk
                        list.add(
                            ParkData(
                                i.key.toString(),
                                count.toString()
                            )
                        )// pridanie listeneru pre pocuvanie
                    }
                    // adapter ParkAdapter naplni recyclerView polozkami z predoslich cyklov
                    recView.adapter = ParkAdapter(list, this@HomePage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.toString(), Toast.LENGTH_LONG).show()
            }
        }
        db.addValueEventListener(valueListener) // pridanie listenera pre buduce zmeny v datach
    }

    // metoda, ktora obsluhuje kliknutie na niektoru z poloziek parkovisk
    // po kliknuti sa otvara aktivita LicPlate_Page, pricom sa mu posielaju
    // cez putExtra nazov parkoviska a pocet znaciek v nom
    override fun onItemClick(position: Int) {
        val intent = Intent(this, LicPlate_Page::class.java)
        intent.putExtra("parkName", list[position].parkName)
        intent.putExtra("numPlates", list[position].plateNum)
        startActivity(intent)
        finish()
    }

    // vymazanie usera z DB na zaklade uid
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
        db.addListenerForSingleValueEvent(deleteUser)
    }

    // funkcia, ktora zmaze neziaduce listenery, aby dalej nepocuvali pri zatvoreni aktivity
    override fun onDestroy() {
        super.onDestroy()
        db.removeEventListener(valueListener)
//        db.removeEventListener(deleteUser)
    }

}