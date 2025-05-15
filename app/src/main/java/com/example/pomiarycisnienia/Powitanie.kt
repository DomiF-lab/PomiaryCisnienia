package com.example.pomiarycisnienia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class Powitanie : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.powitanie)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val przycisk = findViewById<Button>(R.id.przyciskRozpocznij)
        przycisk.setOnClickListener {
            startActivity(Intent(this, LogowanieMail::class.java))
        }
    }

    override fun onStart() {
        super.onStart()

        // Sprawdzenie, czy użytkownik jest już zalogowany
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // Jeśli tak, przejście bezpośrednio do ekranu głównego
            startActivity(Intent(this, OknoGlowne::class.java))
            finish()
        }
    }
}