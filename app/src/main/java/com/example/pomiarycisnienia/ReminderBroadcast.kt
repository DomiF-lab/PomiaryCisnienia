package com.example.pomiarycisnienia

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Odbiornik powiadomień odpowiedzialny za wyświetlanie przypomnienia o pomiarze ciśnienia.
 *
 * Tworzy kanał powiadomień (dla Androida 8.0+) i wyświetla notyfikację, jeśli użytkownik wyraził zgodę.
 */
class ReminderBroadcast : BroadcastReceiver() {

    /**
     * Wywoływana automatycznie przez system, gdy nadejdzie zaplanowane przypomnienie.
     *
     * @param context Kontekst systemowy
     * @param intent Intencja uruchomienia powiadomienia
     */
    override fun onReceive(context: Context, intent: Intent) {
        val powiadomienie = NotificationCompat.Builder(context, KANAL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Przypomnienie o pomiarze")
            .setContentText("Nie zapomnij dziś zmierzyć ciśnienia!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Utworzenie kanału powiadomień (wymagane dla Androida 8.0+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val kanal = NotificationChannel(
                KANAL_ID,
                "Kanał przypomnień o pomiarze",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(kanal)
        }

        // Sprawdzenie, czy użytkownik przyznał uprawnienia do powiadomień
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Wyświetlenie powiadomienia
        NotificationManagerCompat.from(context).notify(ID_POWIADOMIENIA, powiadomienie.build())
    }

    companion object {
        private const val KANAL_ID = "kanalPowiadomien"
        private const val ID_POWIADOMIENIA = 1221
    }
}
