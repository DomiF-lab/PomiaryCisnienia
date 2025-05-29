package com.example.pomiarycisnienia

import android.os.Bundle
import android.widget.ImageView
import android.content.Intent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OknoGlowne : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var poleImie: TextView
    private lateinit var ikonaProfilu: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.okno_glowne)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        poleImie = findViewById(R.id.zalogowanoJako)

        ikonaProfilu = findViewById(R.id.ikonaProfilu)

        // Obsługa kliknięcia w ikonę użytkownika
        ikonaProfilu.setOnClickListener {
            startActivity(Intent(this, OknoWyloguj::class.java))
        }

        val uid = auth.currentUser?.uid

        if (uid != null) {
            // Pobranie imienia użytkownika z Firestore
            firestore.collection("uzytkownicy").document(uid).get()
                .addOnSuccessListener { document ->
                    val imie = document.getString("imie")
                    if (!imie.isNullOrBlank()) {
                        poleImie.text = "Zalogowano jako $imie"
                    }
                }
        }
    }
}