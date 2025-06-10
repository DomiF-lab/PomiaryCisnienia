package com.example.pomiarycisnienia

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.graphics.Color
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class OknoWyloguj : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var poleImie: TextView
    private lateinit var przyciskTak: Button
    private lateinit var przyciskNie: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.okno_wyloguj)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        poleImie = findViewById(R.id.zalogowanoJako)
        przyciskTak = findViewById(R.id.przyciskTak)
        przyciskNie = findViewById(R.id.przyciskNie)

        // Pobranie UID aktualnie zalogowanego użytkownika
        val uid = auth.currentUser?.uid

        // Pobranie imienia użytkownika z Firestore i wyświetlenie go
        if (uid != null) {
            firestore.collection("uzytkownicy").document(uid).get()
                .addOnSuccessListener { document ->
                    val imie = document.getString("imie")
                    if (!imie.isNullOrBlank()) {
                        poleImie.text = "Zalogowano jako $imie"
                    }
                }
        }

        // Obsługa kliknięcia w przycisk "Tak" – wylogowanie i przejście do ekranu powitalnego
        przyciskTak.setOnClickListener {
            auth.signOut()
            finishAffinity()
            startActivity(Intent(this, Powitanie::class.java))
        }

        // Obsługa kliknięcia w przycisk "Nie" – powrót do ekranu głównego
        przyciskNie.setOnClickListener {
            startActivity(Intent(this, OknoGlowne::class.java))
            finish()
        }
    }
}