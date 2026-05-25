package com.example.parcial2_android.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.parcial2_android.R
import com.example.parcial2_android.ui.notificaciones.NotificacionesActivity

/**
 * Helper para disparar notificaciones locales del sistema.
 *
 * Uso desde cualquier Activity o ViewModel:
 *
 *   NotificationHelper.notificarCambioEstado(context, "#1234", "RESUELTA")
 *   NotificationHelper.notificarActualizacion(context, "#1234")
 */
object NotificationHelper {

    private const val CHANNEL_ID   = "utp_incidencias_channel"
    private const val CHANNEL_NAME = "UTP Incidencias"

    // ── Crear el canal (llamar una sola vez, desde Application o MainActivity) ──

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertas de cambios en tus incidencias universitarias"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(canal)
        }
    }

    // ── Notificación: cambio de estado ───────────────────────────────────────

    /**
     * Se llama cuando el estado de una incidencia cambia.
     * @param incidenciaId  p.ej. "#1234"
     * @param nuevoEstado   p.ej. "RESUELTA", "EN_REVISION", "PENDIENTE"
     */
    fun notificarCambioEstado(context: Context, incidenciaId: String, nuevoEstado: String) {
        val estadoLegible = when (nuevoEstado.uppercase()) {
            "RESUELTA"     -> "Resuelta ✅"
            "EN_REVISION"  -> "En Revisión 🔍"
            "PENDIENTE"    -> "Pendiente ⏳"
            else           -> nuevoEstado
        }

        enviar(
            context   = context,
            id        = incidenciaId.hashCode(),
            titulo    = "Cambio de estado",
            mensaje   = "Tu incidencia $incidenciaId ha pasado a $estadoLegible."
        )
    }

    // ── Notificación: nueva actualización/comentario ─────────────────────────

    fun notificarActualizacion(context: Context, incidenciaId: String) {
        enviar(
            context = context,
            id      = (incidenciaId + "update").hashCode(),
            titulo  = "Nueva actualización",
            mensaje = "Se añadió un comentario a tu incidencia $incidenciaId."
        )
    }

    // ── Notificación: anuncio del sistema ────────────────────────────────────

    fun notificarAnuncio(context: Context, mensaje: String) {
        enviar(
            context = context,
            id      = mensaje.hashCode(),
            titulo  = "Aviso del sistema",
            mensaje = mensaje
        )
    }

    // ── Envío interno ────────────────────────────────────────────────────────

    private fun enviar(context: Context, id: Int, titulo: String, mensaje: String) {
        val intent = Intent(context, NotificacionesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notificaciones)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notificacion)
        } catch (e: SecurityException) {
            // El usuario no otorgó permiso POST_NOTIFICATIONS (Android 13+)
            // Manejar silenciosamente; la notificación no se muestra
        }
    }
}