package com.example.licenseplatemanager

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var valueListener: ValueEventListener
    private lateinit var deleteUser: ValueEventListener
    private lateinit var recView: RecyclerView
    private lateinit var list: ArrayList<ParkData>

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)

        mAuth = FirebaseAuth.getInstance()
        db = Firebase.database.reference

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

//    override fun onStop() {
//        super.onStop()
//        mAuth.signOut()
//    }

//    override fun onResume() {
//        super.onResume()
//        showToast("pokracujeme")
//    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mainmenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.logoutID -> {
                mAuth.signOut()
                showToast("odhlásenie bolo úspešné")
                backToLoginPage()
                return true
            }
            R.id.removeID -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Odstránenie užívateľa")
                    .setMessage("Naozaj chcete odstrániť vaše užívateľské konto?\n" +
                            "aktuálne prihlásený ako: ${mAuth.currentUser!!.email}")
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
                    .setNegativeButton("nie") {dialog, which ->
                    }
                    .show()

//                Toast.makeText(this,"remove user", Toast.LENGTH_SHORT).show()
                return true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showToast(text: String){
        Toast.makeText(this,text,Toast.LENGTH_SHORT).show()
    }

    private fun backToLoginPage() {
        startActivity(Intent(this,Login_page::class.java))
        finish()
    }

    private fun getDataFromDB(uid: String) {
        valueListener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                // kotrola existencie referencie
                if (snapshot.exists()){
                    var count: Int
                    count = 0
                    // prechadzanie poloziek (child-ov)
                    for (i in snapshot.children){
                        // kontrola existencie UID v danom parkovisku
                        if (i.child(uid).exists()){
                            count = 0
                            // vnoreny cyklus pre zistenie poctu znaciek daneho usera v danom parkovisku
                            for (j in i.child(uid).children){
                                if (j.key.toString().compareTo("empty") == 0)
                                    break
                                else
                                    count += 1
                            }
                        }

                        list.add(ParkData(i.key.toString(),count.toString()))
                    }
                    recView.adapter = ParkAdapter(list,this@HomePage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext,error.toString(),Toast.LENGTH_LONG).show()
            }
        }
        db.addValueEventListener(valueListener)
    }

    override fun onItemClick(position: Int) {
        val intent = Intent(this,LicPlate_Page::class.java)
        intent.putExtra("parkName",list[position].parkName)
        intent.putExtra("numPlates",list[position].plateNum)
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
                Toast.makeText(applicationContext,error.toString(),Toast.LENGTH_LONG).show()
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