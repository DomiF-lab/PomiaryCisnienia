package com.example.pomiarycisnienia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OknoGlowne : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var poleImie: TextView
    private lateinit var ikonaProfilu: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var przyciskPrzejdz: Button
    private lateinit var adapter: OknoGlowneAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.okno_glowne)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        poleImie = findViewById(R.id.zalogowanoJako)
        ikonaProfilu = findViewById(R.id.ikonaProfilu)
        recyclerView = findViewById(R.id.recyclerView)
        przyciskPrzejdz = findViewById(R.id.przyciskPrzejdz)

        // Kliknięcie w ikonę profilu -> ekran wylogowania
        ikonaProfilu.setOnClickListener {
            startActivity(Intent(this, OknoWyloguj::class.java))
        }

        // Kliknięcie w "Przejdź" -> ekran wszystkie pomiary
        przyciskPrzejdz.setOnClickListener {
            startActivity(Intent(this, WszystkiePomiary::class.java))
        }

        // Pobranie danych użytkownika
        val uid = auth.currentUser?.uid
        if (uid != null) {
            firestore.collection("uzytkownicy").document(uid).get()
                .addOnSuccessListener { document ->
                    val imie = document.getString("imie")
                    if (!imie.isNullOrBlank()) {
                        poleImie.text = "Zalogowano jako $imie"
                    }
                }
        }

        // Konfiguracja RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = OknoGlowneAdapter(emptyList())
        recyclerView.adapter = adapter

        // Wczytanie ostatniego pomiaru
        wczytajOstatniPomiar()
    }

    // Wczytanie ostatniego pomiaru z Firestore
    private fun wczytajOstatniPomiar() {

        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        firestore.collection("pomiary")
            .whereEqualTo("mail", email)
            .orderBy("timestamp")
            .limitToLast(1)
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
                }.reversed()
                adapter.aktualizujListe(lista)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd ładowania danych", Toast.LENGTH_SHORT).show()
            }
    }

    // Adapter do wyświetlania ostatniego pomiaru
    inner class OknoGlowneAdapter(private var lista: List<PomiarModel>) :
        RecyclerView.Adapter<OknoGlowneAdapter.ViewHolder>() {

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

            // Kolorowanie etykiety w zależności od samopoczucia
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

        fun aktualizujListe(nowa: List<PomiarModel>) {
            lista = nowa
            notifyDataSetChanged()
        }
    }
}
