package com.example.pomiarycisnienia

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.firestore.Query
import java.util.*

/**
 * Główna aktywność aplikacji.
 *
 * Odpowiada za:
 * - wyświetlanie ostatnich pomiarów ciśnienia,
 * - prezentację wykresu danych,
 * - obsługę przypomnień o pomiarze,
 * - wylogowanie użytkownika oraz przejście do innych widoków.
 */
class OknoGlowne : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var poleImie: TextView
    private lateinit var ikonaProfilu: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var przyciskPrzejdz: Button
    private lateinit var switchPowiadomien: Switch
    private lateinit var tekstUstawHarmonogram: TextView
    private lateinit var adapter: OknoGlowneAdapter
    private lateinit var wykresCisnienia: LineChart


    // Klucze do zapamiętywania ustawień powiadomień
    private val PREFS = "powiadomienia_prefs"
    private val GODZINA = "godzina"
    private val MINUTA = "minuta"

    /**
     * Metoda wywoływana przy tworzeniu aktywności.
     *
     * Inicjalizuje komponenty interfejsu, pobiera dane użytkownika z Firestore,
     * konfiguruje wykres oraz ustawia przypomnienia zgodnie z zapisanymi preferencjami.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.okno_glowne)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        poleImie = findViewById(R.id.zalogowanoJako)
        ikonaProfilu = findViewById(R.id.ikonaProfilu)
        recyclerView = findViewById(R.id.recyclerView)
        przyciskPrzejdz = findViewById(R.id.przyciskPrzejdz)
        switchPowiadomien = findViewById(R.id.switchPowiadomien)
        tekstUstawHarmonogram = findViewById(R.id.tekstUstawHarmonogram)
        wykresCisnienia = findViewById(R.id.wykresCisnienia)

        // Prośba o uprawnienie do powiadomień na Androidzie 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        // Obsługa kliknięcia w ikonę profilu (wylogowanie)
        ikonaProfilu.setOnClickListener {
            startActivity(Intent(this, OknoWyloguj::class.java))
        }

        // Obsługa przejścia do widoku "Wszystkie pomiary"
        przyciskPrzejdz.setOnClickListener {
            startActivity(Intent(this, WszystkiePomiary::class.java))
        }

        // Wyświetlenie imienia zalogowanego użytkownika
        val uid = auth.currentUser?.uid
        if (uid != null) {
            firestore.collection("uzytkownicy").document(uid).get()
                .addOnSuccessListener { document ->
                    val imie = document.getString("imie")
                    if (!imie.isNullOrBlank()) {
                        poleImie.text = "Zalogowano jako $imie"
                    } else {
                        // Brak imienia - przenieś na ekran wpisywania imienia
                        val intent = Intent(this, LogowanieImie::class.java)
                        intent.putExtra("email", auth.currentUser?.email)
                        startActivity(intent)
                        finish()
                    }
                }
                .addOnFailureListener {
                    // Dokument (imię) nie istnieje - przenosi na ekran wpisywania imienia
                    val intent = Intent(this, LogowanieImie::class.java)
                    intent.putExtra("email", auth.currentUser?.email)
                    startActivity(intent)
                    finish()
                }
        }

        // Konfiguracja RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = OknoGlowneAdapter(emptyList())
        recyclerView.adapter = adapter

        // Wczytanie najnowszego pomiaru
        wczytajOstatniPomiar()
        // Wczytanie danych do wykresy
        wczytajDaneDoWykresu()

        // Przywróć stan switcha na podstawie zapisu
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val czyPowiadomienia = prefs.getBoolean("powiadomienia_wlaczone", false)
        switchPowiadomien.isChecked = czyPowiadomienia
        ustawStanTekstuHarmonogramu(czyPowiadomienia)
        ustawKolorySwitchaPowiadomien()


        // Obsługa zmiany stanu switcha
        switchPowiadomien.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("powiadomienia_wlaczone", isChecked).apply()
            ustawStanTekstuHarmonogramu(isChecked)
            ustawKolorySwitchaPowiadomien()
            if (isChecked) {
                val lastHour = prefs.getInt(GODZINA, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
                val lastMinute = prefs.getInt(MINUTA, Calendar.getInstance().get(Calendar.MINUTE))
                pokazTimePicker(lastHour, lastMinute)
            } else {
                anulujPowiadomienie()
                Toast.makeText(this, "Przypomnienie wyłączone", Toast.LENGTH_SHORT).show()
            }
        }

        // Obsługa kliknięcia w "Ustaw harmonogram"
        tekstUstawHarmonogram.setOnClickListener {
            if (switchPowiadomien.isChecked) {
                val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
                val lastHour = prefs.getInt(GODZINA, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
                val lastMinute = prefs.getInt(MINUTA, Calendar.getInstance().get(Calendar.MINUTE))
                pokazTimePicker(lastHour, lastMinute)
            }
        }
    }

    /**
     * Ustawia tekst, kolor i aktywność pola harmonogramu w zależności od stanu powiadomień.
     *
     * Gdy powiadomienia są włączone, tekst zmienia się na "Edytuj Harmonogram" wraz z ustawioną godziną pod spodem.
     * Gdy powiadomienia są wyłączone, tekst to "Ustaw Harmonogram", pole jest nieaktywne i przyjmuje szary kolor.
     *
     * @param wlaczony Określa, czy powiadomienia są włączone (true) czy wyłączone (false).
     */
    private fun ustawStanTekstuHarmonogramu(wlaczony: Boolean) {
        tekstUstawHarmonogram.isEnabled = wlaczony
        tekstUstawHarmonogram.setTextColor(
            if (wlaczony) Color.parseColor("#E30000") else Color.parseColor("#AAAAAA")
        )
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val godzina = prefs.getInt(GODZINA, -1)
        val minuta = prefs.getInt(MINUTA, -1)

        if (wlaczony && godzina != -1 && minuta != -1) {
            val godzinaMinuta = String.format("%02d:%02d", godzina, minuta)
            tekstUstawHarmonogram.text = "Edytuj Harmonogram:\ncodziennie o $godzinaMinuta"
        } else {
            tekstUstawHarmonogram.text = "Ustaw Harmonogram"
        }
    }

    /**
     * Wyświetla okno wyboru godziny powiadomienia.
     *
     * @param godzina Godzina domyślna
     * @param minuta Minuta domyślna
     */
    private fun pokazTimePicker(godzina: Int, minuta: Int) {
        val picker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            zaplanujPowiadomienie(selectedHour, selectedMinute)
        }, godzina, minuta, true)
        picker.setTitle("Wybierz godzinę przypomnienia")
        picker.show()
    }

    /**
     * Ustawia kolory thumb (przycisku) switcha powiadomień w zależności od jego stanu.
     * Czerwony thumb – włączony, szary – wyłączony.
     */
    private fun ustawKolorySwitchaPowiadomien() {
        val thumbStates = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val thumbColors = intArrayOf(
            Color.parseColor("#E30000"), // Czerwony, gdy włączony
            Color.LTGRAY                  // Szary, gdy wyłączony
        )
        switchPowiadomien.thumbTintList = ColorStateList(thumbStates, thumbColors)
    }


    /**
     * Ustawia codzienne przypomnienie o wskazanej godzinie oraz zapisuje ją do preferencji.
     *
     * @param godzina Godzina (0–23), o której ma zostać wysłane przypomnienie
     * @param minuta Minuta (0–59)
     */
    private fun zaplanujPowiadomienie(godzina: Int, minuta: Int) {
        // Zapis do preferencji
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putInt(GODZINA, godzina)
            .putInt(MINUTA, minuta)
            .apply()

        val intent = Intent(this, ReminderBroadcast::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val kalendarz = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, godzina)
            set(Calendar.MINUTE, minuta)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            kalendarz.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        Toast.makeText(this, "Przypomnienie ustawione na $godzina:${"%02d".format(minuta)}", Toast.LENGTH_SHORT).show()
        ustawStanTekstuHarmonogramu(true)
    }

    /**
     * Anuluje zaplanowane przypomnienie.
     */
    private fun anulujPowiadomienie() {
        val intent = Intent(this, ReminderBroadcast::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Wczytuje i wyświetla ostatni pomiar danego użytkownika z bazy Firestore.
     */
    private fun wczytajOstatniPomiar() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        firestore.collection("pomiary")
            .whereEqualTo("mail", email)
            .orderBy("dataPomiaruTimestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.documents.mapNotNull { doc ->
                    val id = doc.id
                    val data = doc.getString("dataPomiaru") ?: ""
                    val sys = doc.getString("sys") ?: ""
                    val dia = doc.getString("dia") ?: ""
                    val puls = doc.getString("puls") ?: ""
                    val samopoczucie = doc.getString("samopoczucie") ?: ""
                    PomiarModel(id, data, "$sys/$dia", puls, samopoczucie)
                }
                adapter.aktualizujListe(lista)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd ładowania danych", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Adapter do prezentacji ostatniego pomiaru w kafelku.
     *
     * @param lista Lista obiektów pomiaru do wyświetlenia
     */
    inner class OknoGlowneAdapter(private var lista: List<PomiarModel>) :
        RecyclerView.Adapter<OknoGlowneAdapter.ViewHolder>() {

        /**
         * ViewHolder do przechowywania widoków pojedynczego kafelka pomiaru.
         */
        inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            val data: TextView = itemView.findViewById(R.id.dataPomiaru)
            val cisnienie: TextView = itemView.findViewById(R.id.wartoscCisnienia)
            val puls: TextView = itemView.findViewById(R.id.wartoscTetna)
            val samopoczucie: TextView = itemView.findViewById(R.id.Samopoczucie)
            val etykieta: TextView = itemView.findViewById(R.id.etykietaOceny)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.pojedynczy_kafelek, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = lista[position]
            holder.data.text = item.data
            holder.cisnienie.text = item.cisnienie
            holder.puls.text = "Tętno: ${item.puls}"
            holder.samopoczucie.text = "Samopoczucie:"
            holder.etykieta.text = item.samopoczucie.replaceFirstChar { it.uppercase() }

            when (item.samopoczucie.lowercase()) {
                "dobre" -> holder.etykieta.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#00F048"))
                "złe" -> holder.etykieta.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#F00000"))
                else -> holder.etykieta.backgroundTintList = null
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(this@OknoGlowne, Pomiar::class.java)
                intent.putExtra("zrodlo", "szczegolyPomiaru")
                intent.putExtra("idPomiaru", item.id)
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int = lista.size

        /**
         * Aktualizuje dane wyświetlane w adapterze.
         *
         * @param nowa Nowa lista pomiarów do wyświetlenia
         */
        fun aktualizujListe(nowa: List<PomiarModel>) {
            lista = nowa
            notifyDataSetChanged()
        }
    }

    /**
     * Pobiera do 10 ostatnich pomiarów i wyświetla wykres ciśnienia SYS i DIA.
     */
    private fun wczytajDaneDoWykresu() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        firestore.collection("pomiary")
            .whereEqualTo("mail", email)
            .orderBy("dataPomiaruTimestamp", Query.Direction.ASCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val sysList = mutableListOf<Entry>()
                val diaList = mutableListOf<Entry>()
                val pulsList = mutableListOf<Entry>()
                val labels = mutableListOf<String>()

                // grupowanie po dniu
                val mapByDate = LinkedHashMap<String, MutableList<Triple<Float, Float, Float>>>()

                for (doc in result.documents) {
                    val sys = doc.getString("sys")?.toFloatOrNull()
                    val dia = doc.getString("dia")?.toFloatOrNull()
                    val puls = doc.getString("puls")?.toFloatOrNull()
                    val data = doc.getString("dataPomiaru") ?: ""

                    // Skrócenie daty
                    val day = if (data.length > 5) data.substring(0, 5) else data

                    if (sys != null && dia != null && puls != null) {
                        if (!mapByDate.containsKey(day)) {
                            mapByDate[day] = mutableListOf()
                        }
                        mapByDate[day]?.add(Triple(sys, dia, puls))
                    }
                }

                // Budowanie paczki wyników
                var osX = 0f
                for ((date, pomiary) in mapByDate) {
                    for (pomiar in pomiary) {
                        sysList.add(Entry(osX, pomiar.first))
                        diaList.add(Entry(osX, pomiar.second))
                        pulsList.add(Entry(osX, pomiar.third))
                    }
                    labels.add(date)
                    osX += 1f
                }

                pokazWykres(sysList, diaList, pulsList, labels)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd ładowania wykresu", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Konfiguruje i rysuje wykres linii SYS i DIA.
     *
     * @param sysList Lista punktów SYS
     * @param diaList Lista punktów DIA
     * @param pulsList Lista punktów Pulsu
     * @param labels Lista etykiet X (dat)
     */
    private fun pokazWykres(sysList: List<Entry>, diaList: List<Entry>, pulsList: List<Entry>, labels: List<String>) {
        val sysSet = LineDataSet(sysList, "Skurczowe (SYS)").apply {
            lineWidth = 0.001f
            circleRadius = 4f
            color = Color.parseColor("#E30000")
            setCircleColor(Color.parseColor("#E30000"))
            setDrawCircles(true)
            setDrawValues(false)
            setDrawCircleHole(false)
            setDrawIcons(false)
            setDrawHighlightIndicators(false)
            setDrawHorizontalHighlightIndicator(false)
            setDrawVerticalHighlightIndicator(false)
            setDrawFilled(false)
        }
        val diaSet = LineDataSet(diaList, "Rozkurczowe (DIA)").apply {
            lineWidth = 0.001f
            circleRadius = 4f
            color = Color.parseColor("#0004E3")
            setCircleColor(Color.parseColor("#0004E3"))
            setDrawCircles(true)
            setDrawValues(false)
            setDrawCircleHole(false)
            setDrawIcons(false)
            setDrawHighlightIndicators(false)
            setDrawHorizontalHighlightIndicator(false)
            setDrawVerticalHighlightIndicator(false)
            setDrawFilled(false)
        }

        val pulsSet = LineDataSet(pulsList, "Puls").apply {
            lineWidth = 0.001f
            circleRadius = 4f
            color = Color.parseColor("#00F048")
            setCircleColor(Color.parseColor("#00F048"))
            setDrawCircles(true)
            setDrawValues(false)
            setDrawCircleHole(false)
            setDrawIcons(false)
            setDrawHighlightIndicators(false)
            setDrawHorizontalHighlightIndicator(false)
            setDrawVerticalHighlightIndicator(false)
            setDrawFilled(false)
        }


        val liniaData = LineData(sysSet, diaSet, pulsSet)
        wykresCisnienia.data = liniaData

        // Formatowanie osi x (daty)
        wykresCisnienia.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return if (idx in labels.indices) labels[idx] else ""
            }
        }
        wykresCisnienia.xAxis.labelRotationAngle = -45f
        wykresCisnienia.xAxis.granularity = 1f

        // Pozostałe ustawienia wyglądu
        wykresCisnienia.setExtraTopOffset(16f)
        wykresCisnienia.axisRight.isEnabled = false
        wykresCisnienia.description.isEnabled = false
        wykresCisnienia.legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        wykresCisnienia.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        wykresCisnienia.setNoDataText("Brak wystarczającej liczby pomiarów")
        wykresCisnienia.invalidate()
    }
}
