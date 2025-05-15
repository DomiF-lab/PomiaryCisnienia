package com.example.pomiarycisnienia

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener

class LogowanieMail : AppCompatActivity() {

    private lateinit var poleMail: EditText
    private lateinit var przyciskLogowanie: Button
    private lateinit var tekstBledu: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.logowanie_mail)

        poleMail = findViewById(R.id.poleMail)
        przyciskLogowanie = findViewById(R.id.przyciskLogowanie)
        tekstBledu = findViewById(R.id.tekstBledu)

        tekstBledu.visibility = View.INVISIBLE
        przyciskLogowanie.isEnabled = false
        przyciskLogowanie.alpha = 0.5f

        // Włączanie przycisku jeśli email wygląda prawidłowo
        poleMail.addTextChangedListener {
            val email = it.toString().trim()
            val poprawny = Patterns.EMAIL_ADDRESS.matcher(email).matches()
            przyciskLogowanie.isEnabled = poprawny
            przyciskLogowanie.alpha = if (poprawny) 1f else 0.5f
        }

        przyciskLogowanie.setOnClickListener {
            val email = poleMail.text.toString().trim().lowercase()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tekstBledu.text = "Nieprawidłowy adres e-mail"
                tekstBledu.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val intent = Intent(this, LogowanieHaslo::class.java)
            intent.putExtra("email", email)
            startActivity(intent)
            finish()
        }
    }
}
