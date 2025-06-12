package com.example.pomiarycisnienia

/**
 * Model danych reprezentujący użytkownika aplikacji.
 *
 * @property mail Adres e-mail użytkownika
 * @property imie Imię użytkownika
 */
data class Uzytkownik(
    var mail: String = "",
    var imie: String = ""
)
