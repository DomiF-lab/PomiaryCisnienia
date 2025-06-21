package com.example.pomiarycisnienia

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * Aktywność odpowiedzialna za dodawanie, edytowanie oraz przeglądanie pomiarów ciśnienia.
 *
 * Umożliwia:
 * - wprowadzanie danych pomiarowych (SYS, DIA, puls),
 * - określenie samopoczucia i ewentualnych dolegliwości,
 * - ustawienie daty i godziny pomiaru,
 * - zapis lub aktualizację pomiaru w bazie danych Firestore,
 * - usunięcie istniejącego pomiaru.
 */
class Pomiar : AppCompatActivity() {

    // Inicjalizacja obiektów Firebase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Pola interfejsu użytkownika
    private lateinit var tekstUsun: TextView
    private lateinit var tekstEdytuj: TextView
    private lateinit var poleSYS: TextInputEditText
    private lateinit var poleDIA: TextInputEditText
    private lateinit var polePULS: TextInputEditText
    private lateinit var przyciskDobre: Button
    private lateinit var przyciskZle: Button
    private lateinit var inputDolegliwosci: MaterialAutoCompleteTextView
    private lateinit var poleDataPomiaru: MaterialAutoCompleteTextView
    private lateinit var przyciskZapisz: Button
    private lateinit var tekstDolegliwosci: TextView
    private lateinit var inputDolegliwosciLayout: View


    // Zmienne pomocnicze
    private var wybraneDolegliwosci = mutableSetOf<String>()
    private var samopoczucie: String = ""
    private var emailUzytkownika: String = ""
    private var dokumentId: String? = null
    private var trybEdycji = false


    /**
     * Metoda wywoływana przy tworzeniu aktywności.
     *
     * Inicjalizuje komponenty interfejsu, rozpoznaje tryb działania (dodawanie / przegląd / edycja),
     * pobiera dane użytkownika oraz konfiguruje odpowiednie zachowania pól formularza.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pomiar)

        // Inicjalizacja Firebase i identyfikacja użytkownika
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        emailUzytkownika = auth.currentUser?.email ?: ""

        // Powiązanie z widokami XML
        tekstUsun = findViewById(R.id.tekstUsun)
        tekstEdytuj = findViewById(R.id.tekstEdytuj)
        poleSYS = findViewById(R.id.wartoscSkurczowe)
        poleDIA = findViewById(R.id.wartoscRozkurczowe)
        polePULS = findViewById(R.id.wartosPuls)
        przyciskDobre = findViewById(R.id.przyciskDobre)
        przyciskZle = findViewById(R.id.przyciskZle)
        inputDolegliwosci = findViewById(R.id.inputDolegliwosci)
        poleDataPomiaru = findViewById(R.id.poleDataPomiaru)
        przyciskZapisz = findViewById(R.id.przyciskZapisz)
        tekstDolegliwosci = findViewById(R.id.tekstDolegliwosci)
        inputDolegliwosciLayout = findViewById(R.id.inputDolegliwosciLayout)

        // Ukrycie karty dolegliwosci
        tekstDolegliwosci.visibility = View.GONE
        inputDolegliwosciLayout.visibility = View.GONE

        //Teskt przycisku Edytuj
        tekstEdytuj.text = "Edytuj"

        // Pobranie dokumentId i trybu edycji
        val zrodlo = intent.getStringExtra("zrodlo")
        dokumentId = intent.getStringExtra("idPomiaru")

        // Konfiguracja trybu i widoczności w zależności od kontekstu
        when (zrodlo) {
            "nowyPomiar" -> {
                trybEdycji = true
                ustawPolaEdycji(true)
                tekstEdytuj.visibility = View.INVISIBLE
                tekstUsun.visibility = View.INVISIBLE
                val teraz = Calendar.getInstance().time
                val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                poleDataPomiaru.setText(format.format(teraz))
            }
            "szczegolyPomiaru" -> {
                trybEdycji = false
                ustawPolaEdycji(false)
                tekstUsun.visibility = View.INVISIBLE
                zaladujDanePomiaru()
            }
            else -> {
                // Domyślnie: zablokowane pola
                trybEdycji = false
                ustawPolaEdycji(false)
            }
        }

        // Aktywacja trybu edycji
        tekstEdytuj.setOnClickListener {
            if (!trybEdycji) {
                // Przechodzimy do trybu edycji
                trybEdycji = true
                tekstEdytuj.text = "Anuluj"
                tekstUsun.visibility = View.VISIBLE
                ustawPolaEdycji(true)
            } else {
                // Wracamy do trybu wyświetlania (bez zapisu!)
                trybEdycji = false
                tekstEdytuj.text = "Edytuj"
                tekstUsun.visibility = View.INVISIBLE
                ustawPolaEdycji(false)
                // Załaduj ponownie dane z bazy (cofa zmiany na formularzu)
                zaladujDanePomiaru()
            }
        }

        // Usuwanie dokumentu z bazy danych po potwierdzeniu
        tekstUsun.setOnClickListener {
            if (dokumentId != null) {
                AlertDialog.Builder(this)
                    .setTitle("Usuń pomiar")
                    .setMessage("Czy na pewno chcesz usunąć ten pomiar?")
                    .setPositiveButton("Tak") { _, _ ->
                        firestore.collection("pomiary").document(dokumentId!!)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Pomiar usunięty", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Błąd podczas usuwania", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Nie", null)
                    .show()
            }
        }

        // Obsługa pola daty i godziny
        poleDataPomiaru.setOnClickListener {
            if (!trybEdycji) return@setOnClickListener
            val kalendarz = Calendar.getInstance()
            val datePicker = DatePickerDialog(this,
                { _, year, month, dayOfMonth ->
                    val timePicker = TimePickerDialog(this,
                        { _, hourOfDay, minute ->
                            // BLOKADA GODZINY Z PRZYSZŁOŚCI
                            val wybranyCzas = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                set(Calendar.HOUR_OF_DAY, hourOfDay)
                                set(Calendar.MINUTE, minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            if (wybranyCzas.timeInMillis > System.currentTimeMillis()) {
                                Toast.makeText(this@Pomiar, "Nie można wybrać godziny z przyszłości.", Toast.LENGTH_SHORT).show()
                            } else {
                                val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                poleDataPomiaru.setText(format.format(wybranyCzas.time))
                            }
                        },
                        kalendarz.get(Calendar.HOUR_OF_DAY),
                        kalendarz.get(Calendar.MINUTE),
                        true
                    )
                    timePicker.show()
                },
                kalendarz.get(Calendar.YEAR),
                kalendarz.get(Calendar.MONTH),
                kalendarz.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }

        // Odblokowanie wybieranie dolegliwości
        inputDolegliwosci.setOnClickListener {
            if (!trybEdycji) return@setOnClickListener
            if (samopoczucie == "złe") {
                pokazWielokrotnyWyborDolegliwosci()
            }
        }

        // Ustawienie samopoczucia jako dobre
        przyciskDobre.setOnClickListener {
            if (!trybEdycji) return@setOnClickListener
            samopoczucie = "dobre"
            przyciskDobre.alpha = 1.0f
            przyciskZle.alpha = 0.45f
            inputDolegliwosci.setText("Brak")
            wybraneDolegliwosci.clear()

            // Ukrycie pola dolegliwości
            tekstDolegliwosci.visibility = View.GONE
            inputDolegliwosciLayout.visibility = View.GONE
        }

        // Ustawienie samopoczucia jako złe
        przyciskZle.setOnClickListener {
            if (!trybEdycji) return@setOnClickListener
            samopoczucie = "złe"
            przyciskZle.alpha = 1.0f
            przyciskDobre.alpha = 0.45f

            // Pokazanie pola dolegliwości
            tekstDolegliwosci.visibility = View.VISIBLE
            inputDolegliwosciLayout.visibility = View.VISIBLE
            pokazWielokrotnyWyborDolegliwosci()
        }

        // Obsługa zapisu pomiaru do bazy danych
        przyciskZapisz.setOnClickListener {
            val dataPomiaru = if (poleDataPomiaru.text.toString().isBlank()) {
                val teraz = Calendar.getInstance().time
                val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                format.format(teraz)
            } else {
                poleDataPomiaru.text.toString()
            }

            val cisnienieSYS = poleSYS.text.toString().toIntOrNull()
            val cisnienieDIA = poleDIA.text.toString().toIntOrNull()
            val puls = polePULS.text.toString().toIntOrNull()

            // Walidacja danych wejściowych
            if (cisnienieSYS == null || cisnienieDIA == null || puls == null) {
                Toast.makeText(this, "Wprowadź prawidłowe wartości liczbowe", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cisnienieSYS !in 80..250 || cisnienieDIA !in 40..150 || puls !in 30..200) {
                Toast.makeText(this, "Wartości poza dozwolonym zakresem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Walidacja samopoczucia
            if (samopoczucie.isBlank()) {
                Toast.makeText(this, "Proszę wybrać samopoczucie", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val dataPomiaruDate = sdf.parse(dataPomiaru)
            val dataPomiaruTimestamp = com.google.firebase.Timestamp(dataPomiaruDate)

            val danePomiaru = hashMapOf(
                "mail" to emailUzytkownika,
                "sys" to cisnienieSYS.toString(),
                "dia" to cisnienieDIA.toString(),
                "puls" to puls.toString(),
                "samopoczucie" to samopoczucie,
                "dolegliwosci" to wybraneDolegliwosci.joinToString(", "),
                "dataPomiaru" to dataPomiaru,
                "dataPomiaruTimestamp" to dataPomiaruTimestamp
            )

            // Aktualizacja istniejącego dokumentu lub dodanie nowego
            if (trybEdycji && dokumentId != null) {
                firestore.collection("pomiary").document(dokumentId!!)
                    .set(danePomiaru)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Pomiar zaktualizowany", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Błąd podczas aktualizacji", Toast.LENGTH_SHORT).show()
                    }
            } else {
                firestore.collection("pomiary")
                    .add(danePomiaru)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Pomiar zapisany", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Błąd zapisu", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    /**
     * Wyświetla okno dialogowe z możliwością wielokrotnego wyboru dolegliwości
     * w przypadku samopoczucia oznaczonego jako "złe".
     */
    private fun pokazWielokrotnyWyborDolegliwosci() {
        val dolegliwosciArray = arrayOf("Ból głowy", "Zawroty", "Zmęczenie", "Kołatanie serca", "Mdłości")
        val wybrane = BooleanArray(dolegliwosciArray.size)

        AlertDialog.Builder(this)
            .setTitle("Wybierz dolegliwości")
            .setMultiChoiceItems(dolegliwosciArray, wybrane) { _, which, isChecked ->
                if (isChecked) {
                    wybraneDolegliwosci.add(dolegliwosciArray[which])
                } else {
                    wybraneDolegliwosci.remove(dolegliwosciArray[which])
                }
                inputDolegliwosci.setText(wybraneDolegliwosci.joinToString(", "))
            }
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /**
     * Ustawia możliwość edytowania poszczególnych pól formularza.
     *
     * @param wlacz Określa, czy pola mają być aktywne (true) czy zablokowane (false)
     */
    private fun ustawPolaEdycji(wlacz: Boolean) {
        poleSYS.isEnabled = wlacz
        poleDIA.isEnabled = wlacz
        polePULS.isEnabled = wlacz
        inputDolegliwosci.isEnabled = wlacz
        poleDataPomiaru.isEnabled = wlacz
        przyciskDobre.isEnabled = wlacz
        przyciskZle.isEnabled = wlacz
        przyciskZapisz.isEnabled = wlacz
    }

    /**
     * Ładuje dane istniejącego pomiaru z Firestore i wypełnia nimi formularz.
     *
     * Wykorzystywane w trybie podglądu szczegółów pomiaru.
     */
    private fun zaladujDanePomiaru() {
        if (dokumentId == null) return

        firestore.collection("pomiary").document(dokumentId!!)
            .get()
            .addOnSuccessListener { doc ->
                poleSYS.setText(doc.getString("sys"))
                poleDIA.setText(doc.getString("dia"))
                polePULS.setText(doc.getString("puls"))
                poleDataPomiaru.setText(doc.getString("dataPomiaru"))
                inputDolegliwosci.setText(doc.getString("dolegliwosci") ?: "")
                samopoczucie = doc.getString("samopoczucie") ?: ""

                if (samopoczucie == "dobre") {
                    przyciskDobre.alpha = 1.0f
                    przyciskZle.alpha = 0.45f
                    tekstDolegliwosci.visibility = View.GONE
                    inputDolegliwosciLayout.visibility = View.GONE
                } else if (samopoczucie == "złe") {
                    przyciskZle.alpha = 1.0f
                    przyciskDobre.alpha = 0.45f
                    tekstDolegliwosci.visibility = View.VISIBLE
                    inputDolegliwosciLayout.visibility = View.VISIBLE
                } else {
                    przyciskDobre.alpha = 0.45f
                    przyciskZle.alpha = 0.45f
                    tekstDolegliwosci.visibility = View.GONE
                    inputDolegliwosciLayout.visibility = View.GONE
                }

                doc.getString("dolegliwosci")?.split(",")?.map { it.trim() }?.let {
                    wybraneDolegliwosci.addAll(it)
                }
            }
    }
}
