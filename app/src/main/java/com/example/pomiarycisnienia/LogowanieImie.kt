package com.example.pomiarycisnienia

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Aktywność odpowiedzialna za pobranie imienia użytkownika po rejestracji
 * oraz zapisanie go w bazie danych Firestore.
 */
class LogowanieImie : AppCompatActivity() {

    private lateinit var poleImie: EditText
    private lateinit var tekstBleduImienia: TextView
    private lateinit var przyciskImie: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var mail: String


    /**
     * Metoda wywoływana przy tworzeniu aktywności.
     * Inicjalizuje komponenty UI, obsługuje walidację imienia oraz zapis do bazy danych.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.logowanie_imie)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        mail = intent.getStringExtra("email")?.trim()?.lowercase().orEmpty()

        // Inicjalizacja widoków
        poleImie = findViewById(R.id.poleImie)
        tekstBleduImienia = findViewById(R.id.tekstBleduImienia)
        przyciskImie = findViewById(R.id.przyciskImie)

        tekstBleduImienia.visibility = View.INVISIBLE
        przyciskImie.isEnabled = false
        przyciskImie.alpha = 0.5f

        // Walidacja w czasie wpisywania
        poleImie.addTextChangedListener {
            val imie = it.toString().trim()
            //https://stackoverflow.com/questions/35392798/regex-to-validate-full-name-having-atleast-four-characters
            val poprawne = imie.matches(Regex("^[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ\\- ]{2,}$"))
            przyciskImie.isEnabled = poprawne
            przyciskImie.alpha = if (poprawne) 1f else 0.5f
        }

        // Zapis imienia do Firestore
        przyciskImie.setOnClickListener {
            val imie = poleImie.text.toString().trim()

            //https://stackoverflow.com/questions/35392798/regex-to-validate-full-name-having-atleast-four-characters
            if (imie.length < 2 || !imie.matches(Regex("^[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ\\- ]{2,}$"))) {
                tekstBleduImienia.text = "Imię nie może zawierać cyfr ani znaków specjalnych"
                tekstBleduImienia.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Pobranie identyfikatora aktualnie zalogowanego użytkownika
            val uid = auth.currentUser?.uid
            // Przygotowanie danych użytkownika do zapisania w bazie
            if (uid != null) {
                val uzytkownik = hashMapOf(
                    "mail" to mail,
                    "imie" to imie
                )

                // Zapis danych użytkownika do kolekcji "uzytkownicy"
                firestore.collection("uzytkownicy").document(uid)
                    .set(uzytkownik)
                    .addOnSuccessListener {
                        // Przekierowanie do głównego okna aplikacji po sukcesie
                        startActivity(Intent(this, OknoGlowne::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        // Obsługa błędu zapisu do bazy
                        tekstBleduImienia.text = "Błąd podczas zapisu danych"
                        tekstBleduImienia.visibility = View.VISIBLE
                    }
            } else {
                // Obsługa przypadku braku identyfikatora użytkownika
                tekstBleduImienia.text = "Nie można uzyskać użytkownika"
                tekstBleduImienia.visibility = View.VISIBLE
            }
        }
    }
}
