package com.example.licenseplatemanager

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
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
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class LicPlate_Page : AppCompatActivity(), LicPlateAdapter.OnItemClickListener {
    //    deklaracia potrebnych premennych
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private lateinit var parkReference: DatabaseReference
    private lateinit var valueListener: ValueEventListener
    private lateinit var plateRecView: RecyclerView
    private lateinit var list: ArrayList<LicPlateData>
    private lateinit var emptyLayout: LinearLayout
    private lateinit var parkHeading: TextView

    //    onCreate metoda, kde nastavime niektore z premennych (Firebase, recycerView)
    //    hlavna metoda pri otvoreni novej instancie tejto triedy
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lic_plate_page)

        var actionBar = getSupportActionBar()

        // showing the back button in action bar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        // ziskanie referencii na Firebase a kontrola prihlaseneho usera
        mAuth = FirebaseAuth.getInstance()
        db = Firebase.database.reference

        val user = Firebase.auth.currentUser
        if (user == null) {
            showToast("zlyhala autentifikácia účtu!")
            backToLoginPage()   // ak sa neda autentifikovat, vraciame sa na LoginPage
        }

        // nastavenie recyclerVieweru
        emptyLayout = findViewById<LinearLayout>(R.id.emptyPageID)
        plateRecView = findViewById<RecyclerView>(R.id.plateRecViewID)
        plateRecView.layoutManager = LinearLayoutManager(this)
        plateRecView.setHasFixedSize(true)
        list = arrayListOf<LicPlateData>()

        parkHeading = findViewById<TextView>(R.id.selectedParkID)

        // ziskanie mien z predoslej aktivity
        val bundle: Bundle? = intent.extras
        val parkName = bundle!!.getString("parkName")
        val numPlates = bundle?.getString("numPlates")

        // ak bolo v danom parkovisku 0 priradenych znaciek, nastavime prazdny layout recycleru
        parkHeading.text = parkName
        if (numPlates.equals("0")) setEmptyContent(true) else setEmptyContent(false)

        parkReference = db.child(parkHeading.text.toString())
            .child(mAuth.currentUser!!.uid.toString())
            .ref

        // nastavenie tlacitka pridania novej ECV, kde po jeho stlaceni sa otvori dialogove okno
        // kde uzivatel zapise pozadovanu znacku a ta sa ulozi do DB (ak tam taka este nie je)
        val addBtn = findViewById<ImageView>(R.id.addBtnID)
        addBtn.setOnClickListener() {
            var typedLicPlate = EditText(this)
            typedLicPlate.inputType = InputType.TYPE_CLASS_TEXT
            typedLicPlate.hint = "napr. XXYYYZZ"
            MaterialAlertDialogBuilder(this)
                .setTitle("Pridanie novej EČV")
                .setView(typedLicPlate)
                .setPositiveButton("Pridať") { p0, p1 ->
                    if (!isPlateInList(typedLicPlate.text.toString().uppercase().trim())) {
                        addItem(typedLicPlate.text.toString().uppercase().trim())
                        showToast("ečv ${typedLicPlate.text.toString().uppercase().trim()} pridaná")
                    } else {
                        showToast("zadaná EČV už je v databáze")
                    }
                }
                .show()
        }

        getDataFromDB()
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
        }
    }

    // vytvorenie menu toolbaru
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mainmenu, menu)
        return true
    }

    // urcenie vramci toolbaru, co sa ma stat po kliknuti na polozku, vysvetlene v HomePage aktivite
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.notifIconID -> {
                startActivity(Intent(this, Notification_Page::class.java))
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
                    .setPositiveButton("Áno") { p0, p1 ->
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
                    .setNegativeButton("Nie") { dialog, which ->
                    }
                    .show()

                return true
            }
            // sipka nazad, vraciam sa na HomePage
            android.R.id.home -> {
                backToHomePage()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // obsluha kliknutia na polozku (ECV) na parkovisku, po stlaceni na cervene X, vyskoci
    // dialogove okno, ci si ozaj prajem zmazat danu ECV, ak ano, zmaze sa
    override fun onItemClick(position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Odstránenie ${list[position].name}")
            .setMessage("Naozaj si prajete odstrániť túto EČV?")
            .setPositiveButton("Áno") { p0, p1 ->
                list[position].name?.let { removeItem(it) }
            }
            .setNegativeButton("Nie") { dialog, which ->
            }
            .show()
    }

    // metoda nacitania znaciek k danemu parkovisku danehu usera, ulozenie tychto do recyclerView
    // pomocou adapteru a zapnutie listeneru, ktory pocuva na zmeny v DB
    private fun getDataFromDB() {
        valueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                // kotrola existencie referencie
                if (snapshot.exists()) {
                    // prechadzanie poloziek (child-ov)
                    for (i in snapshot.children) {
                        // kontrola existencie UID v danom parkovisku
                        if (i.key.toString().equals("empty"))
                            break
                        // pridanie do arrayListu (ecv + datum vytvorenia)
                        list.add(
                            LicPlateData(
                                i.key.toString(),
                                i.child("created").value.toString()
                            )
                        )
                    }
                    // ak je prazdne, nastavi sa prazdny layout, inak sa naplnia polozky do recycleru
                    if (list.isEmpty() || list[0].name.equals("empty")) {
                        setEmptyContent(true)
                    } else {
                        setEmptyContent(false)
                        plateRecView.adapter = LicPlateAdapter(list, this@LicPlate_Page)
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.toString(), Toast.LENGTH_LONG).show()
            }
        }
        parkReference.addValueEventListener(valueListener)  // zapnutie pocuvania na zmeny
    }

    // kontrola, ci sa dana ECV nachadza na parkovisku (pomocna funkcia pre addBtn)
    private fun isPlateInList(plateName: String): Boolean {
        for (plate in list.iterator()) {
            if (plate.name!!.compareTo(plateName) == 0)
                return true
        }
        return false
    }

    // pridanie novej ECV do DB aj s datumom vytvorenia
    private fun addItem(plateName: String) {
        val item = object : ValueEventListener {
            val cal = Calendar.getInstance()
            var actualDate: String = cal.get(Calendar.DAY_OF_MONTH).toString() +
                    "." + (cal.get(Calendar.MONTH)+1).toString() +
                    "." + cal.get(Calendar.YEAR).toString()
            var visitedTime: String = cal.get(Calendar.DAY_OF_MONTH).toString() +
                    "/" + (cal.get(Calendar.MONTH)+1).toString() +
                    " " + cal.get(Calendar.HOUR_OF_DAY).toString() +
                    ":" + cal.get(Calendar.MINUTE).toString()

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    snapshot.child(plateName).child("created").ref.setValue(actualDate)
                    snapshot.child(plateName).child("notify").ref.setValue(false)
                    snapshot.child(plateName).child("seen").ref.setValue(true)
                    snapshot.child(plateName).child("lastVisited").ref.setValue(visitedTime)
                    if (list.size == 0 && snapshot.child("empty").exists()) {
                        snapshot.child("empty").ref.removeValue()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast(error.toString())
            }
        }
        parkReference.addListenerForSingleValueEvent(item)
    }

    // odstranenie ECV z DB, ak odstranujeme poslednu polozku, musime tu pridat "empty" znacku,
    // aby ostal zachovany zaznam o uid pre dane parkovisko, inak by sa uz nove polozky
    // nemohli pridat!
    private fun removeItem(plateName: String) {
        val item = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    if (list.size == 1) {
                        snapshot.child("empty").ref.setValue("null")
                    }
                    snapshot.child(plateName).ref.removeValue()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast(error.toString())
            }
        }
        parkReference.addListenerForSingleValueEvent(item)
    }

    // odstranenie uctu
    private fun deleteUserFromDB(uid: String) {
        val deleteUser = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (i in snapshot.children) {
                        if (i.child(uid).exists())
                            i.child(uid).ref.removeValue()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast(error.toString())
            }
        }
        db.addListenerForSingleValueEvent(deleteUser)
    }

    // pomocna funkcia vypisania Toastu, kde bude vypisana sprava z parametru tejto metody
    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    // metoda prepinana layoutov recycleru (ak je prazny, zneviditelnime ho a nastavime prazdne
    // logo pozadia)
    private fun setEmptyContent(isEmpty: Boolean) {
        if (isEmpty) {
            emptyLayout.visibility = View.VISIBLE
            plateRecView.visibility = View.INVISIBLE
        } else {
            emptyLayout.visibility = View.INVISIBLE
            plateRecView.visibility = View.VISIBLE
        }
    }

    // otvorenie aktivity Login_page a nasledne zatvorenie (zrusenie) tejto aktivity
    private fun backToLoginPage() {
        startActivity(Intent(this, Login_page::class.java))
        finish()
    }

    // otvorenie aktivity HomePage a nasledne zatvorenie (zrusenie) tejto aktivity
    private fun backToHomePage() {
        startActivity(Intent(this, HomePage::class.java))
        finish()
    }

    // funkcia, ktora zmaze neziaduce listenery, aby dalej nepocuvali pri zatvoreni aktivity
    override fun onDestroy() {
        super.onDestroy()
        parkReference.removeEventListener(valueListener)
//        db.removeEventListener(deleteUser)
    }

}