package com.example.pomiarycisnienia

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth

class WszystkiePomiary : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PomiarAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var nowyPomiar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wszystkie_pomiary)

        // Inicjalizacja instancji Firestore
        firestore = FirebaseFirestore.getInstance()

        // Konfiguracja RecyclerView
        recyclerView = findViewById(R.id.widokRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PomiarAdapter(emptyList())
        recyclerView.adapter = adapter

        // Obsługa kliknięcia w tekst "Nowy pomiar"
        nowyPomiar = findViewById(R.id.tekstNowyPomiar)
        nowyPomiar.setOnClickListener {
            val intent = Intent(this, Pomiar::class.java)
            intent.putExtra("zrodlo", "nowyPomiar")
            startActivity(intent)
        }

        // Obsługa przycisku powrotu
        val przyciskWroc = findViewById<Button>(R.id.przyciskWroc)
        przyciskWroc.setOnClickListener {
            val intent = Intent(this, OknoGlowne::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

    }

    override fun onResume() {
        super.onResume()
        // Po powrocie do aktywności wczytywana jest aktualna lista pomiarów
        wczytajPomiary()
    }

    // Wczytywanie pomiarów z Firestore i przekazanie ich do adaptera
    private fun wczytajPomiary() {

        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        firestore.collection("pomiary")
            .whereEqualTo("mail", email)
            .get()

            .addOnSuccessListener { result ->
                val listaPomiarow = result.documents.mapNotNull { doc ->
                    val id = doc.id
                    val data = doc.getString("dataPomiaru") ?: ""
                    val sys = doc.getString("sys") ?: ""
                    val dia = doc.getString("dia") ?: ""
                    val puls = doc.getString("puls") ?: ""
                    val samopoczucie = doc.getString("samopoczucie") ?: ""
                    PomiarModel(id, data, "$sys/$dia", puls, samopoczucie)
                }
                adapter.aktualizujListe(listaPomiarow)
            }
    }

    // Adapter do wyświetlania listy pomiarów w RecyclerView
    inner class PomiarAdapter(private var lista: List<PomiarModel>) :
        RecyclerView.Adapter<PomiarAdapter.PomiarViewHolder>() {

        // Klasa ViewHolder dla elementu listy
        inner class PomiarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val dataPomiaru: TextView = itemView.findViewById(R.id.dataPomiaru)
            val wartoscCisnienia: TextView = itemView.findViewById(R.id.wartoscCisnienia)
            val wartoscTetna: TextView = itemView.findViewById(R.id.wartoscTetna)
            val samopoczucie: TextView = itemView.findViewById(R.id.Samopoczucie)
            val etykietaOceny: TextView = itemView.findViewById(R.id.etykietaOceny)
        }

        // Tworzenie nowego ViewHoldera
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PomiarViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.pojedynczy_kafelek, parent, false)
            return PomiarViewHolder(view)
        }

        // Przypisywanie danych do elementów ViewHoldera
        override fun onBindViewHolder(holder: PomiarViewHolder, position: Int) {
            val pomiar = lista[position]
            holder.dataPomiaru.text = pomiar.data
            holder.wartoscCisnienia.text = pomiar.cisnienie
            holder.wartoscTetna.text = "Tętno: ${pomiar.puls}"
            holder.samopoczucie.text = "Samopoczucie:"
            holder.etykietaOceny.text = pomiar.samopoczucie.replaceFirstChar { it.uppercase() }

            // Kolorowanie tła przycisku etykiety oceny w zależności od treści
            when (pomiar.samopoczucie.lowercase()) {
                "dobre" -> holder.etykietaOceny.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#00F048"))
                "złe" -> holder.etykietaOceny.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#F00000"))
                else -> holder.etykietaOceny.backgroundTintList = null
            }

            // Obsługa kliknięcia w dany pomiar – otwierana jest aktywność Pomiar z danymi
            holder.itemView.setOnClickListener {
                val intent = Intent(this@WszystkiePomiary, Pomiar::class.java)
                intent.putExtra("zrodlo", "szczegolyPomiaru")
                intent.putExtra("idPomiaru", pomiar.id)
                startActivity(intent)
            }
        }

        // Zwracanie liczby elementów w liście
        override fun getItemCount(): Int = lista.size

        // Aktualizacja danych w adapterze
        fun aktualizujListe(nowaLista: List<PomiarModel>) {
            lista = nowaLista
            notifyDataSetChanged()
        }
    }
}
