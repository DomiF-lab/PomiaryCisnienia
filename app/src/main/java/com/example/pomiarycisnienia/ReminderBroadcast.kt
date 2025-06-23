package com.example.pomiarycisnienia

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * ReminderBroadcast – Klasa odbierająca zdarzenia systemowe (BroadcastReceiver)
 * i wyświetlająca powiadomienie przypominające użytkownikowi o wykonaniu pomiaru ciśnienia.
 *
 * Funkcje:
 * - Tworzy kanał powiadomień (dla Androida 8.0 i nowszych).
 * - Buduje i wyświetla powiadomienie z możliwością przejścia do głównej aktywności aplikacji.
 * - Sprawdza, czy aplikacja posiada wymagane uprawnienia do wyświetlania powiadomień.
 *
 */
class ReminderBroadcast : BroadcastReceiver() {

    /**
     * Metoda wywoływana po odebraniu zdarzenia.
     *
     * Tworzy kanał powiadomień, buduje powiadomienie
     * przypominające o pomiarze ciśnienia i wyświetla je użytkownikowi,
     * jeśli przyznane zostały odpowiednie uprawnienia.
     */
    override fun onReceive(context: Context, intent: Intent) {
        // Utworzenie kanału powiadomień (dla Androida 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                KANAL_ID,
                "Kanał przypomnień o pomiarze",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // PendingIntent uruchamiający główną aktywność po kliknięciu powiadomienia
        val notificationIntent = Intent(context, OknoGlowne::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Budowa powiadomienia
        val notification = NotificationCompat.Builder(context, KANAL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Przypomnienie o pomiarze")
            .setContentText("Nie zapomnij dziś zmierzyć ciśnienia!")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Sprawdzenie, czy użytkownik przyznał uprawnienia do powiadomień
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Wyświetlenie powiadomienia
        NotificationManagerCompat.from(context)
            .notify(ID_POWIADOMIENIA + intent.getIntExtra("id", 0), notification)
    }

    companion object {
        private const val KANAL_ID = "kanalPowiadomien"
        private const val ID_POWIADOMIENIA = 1221
    }
}
