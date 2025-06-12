package com.example.pomiarycisnienia

/**
 * Model danych reprezentujący pojedynczy pomiar ciśnienia krwi.
 *
 * @property id Identyfikator dokumentu w bazie danych
 * @property data Data i godzina wykonania pomiaru (w formacie tekstowym)
 * @property cisnienie Wartość ciśnienia krwi w formacie "SYS/DIA"
 * @property puls Wartość tętna (liczba uderzeń na minutę)
 * @property samopoczucie Samoocena samopoczucia użytkownika (np. "dobre", "złe")
 */
data class PomiarModel(
    val id: String,
    val data: String,
    val cisnienie: String,
    val puls: String,
    val samopoczucie: String
)
