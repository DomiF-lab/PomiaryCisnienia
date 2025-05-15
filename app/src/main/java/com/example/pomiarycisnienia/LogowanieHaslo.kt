package com.example.pomiarycisnienia

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class LogowanieHaslo : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var poleHaslo: EditText
    private lateinit var tekstBledu2: TextView
    private lateinit var przyciskKontynuuj: Button

    private lateinit var mail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.logowanie_haslo)

        auth = FirebaseAuth.getInstance()

        poleHaslo = findViewById(R.id.poleHaslo)
        tekstBledu2 = findViewById(R.id.tekstBledu2)
        przyciskKontynuuj = findViewById(R.id.przyciskLogowanie2)

        // Pobranie e-maila z poprzedniej aktywności
        mail = intent.getStringExtra("email")?.trim()?.lowercase().orEmpty()

        // Ukrycie tekstu błędu i dezaktywacja przycisku na start
        tekstBledu2.visibility = View.INVISIBLE
        przyciskKontynuuj.isEnabled = false
        przyciskKontynuuj.alpha = 0.5f

        // Walidacja hasła: przycisk aktywny, jeśli hasło ma co najmniej 8 znaków
        poleHaslo.addTextChangedListener {
            val valid = (it?.length ?: 0) >= 8
            przyciskKontynuuj.isEnabled = valid
            przyciskKontynuuj.alpha = if (valid) 1f else 0.5f
        }

        // Obsługa kliknięcia przycisku
        przyciskKontynuuj.setOnClickListener {
            val haslo = poleHaslo.text.toString().trim()

            if (haslo.length < 8) {
                tekstBledu2.text = "Hasło musi mieć co najmniej 8 znaków"
                tekstBledu2.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Próba logowania
            auth.signInWithEmailAndPassword(mail, haslo)
                .addOnCompleteListener { loginTask ->
                    if (loginTask.isSuccessful) {
                        startActivity(Intent(this, OknoGlowne::class.java))
                        finish()
                    } else {
                        // Jeśli logowanie się nie powiodło, próba rejestracji
                        auth.createUserWithEmailAndPassword(mail, haslo)
                            .addOnCompleteListener { regTask ->
                                if (regTask.isSuccessful) {
                                    val intent = Intent(this, LogowanieImie::class.java)
                                    intent.putExtra("email", mail)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    val kodBledu = (regTask.exception as? FirebaseAuthException)?.errorCode

                                    when (kodBledu) {
                                        "ERROR_EMAIL_ALREADY_IN_USE" -> {
                                            tekstBledu2.text = "Nieprawidłowe hasło lub konto już istnieje"
                                        }
                                        else -> {
                                            tekstBledu2.text = "Rejestracja nie powiodła się"
                                        }
                                    }

                                    tekstBledu2.visibility = View.VISIBLE
                                }
                            }
                    }
                }
        }
    }
}
