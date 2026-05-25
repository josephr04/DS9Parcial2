package com.example.parcial2_android.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestiona la sesión del usuario usando SharedPreferences.
 * Almacena el id, correo y token después de un login exitoso.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sesion_app", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USUARIO_ID = "usuario_id"
        private const val KEY_CORREO = "correo"
        private const val KEY_TOKEN = "token"
        private const val KEY_LOGUEADO = "logueado"
    }

    /**
     * Guarda los datos de la sesión después del login.
     * @param usuarioId ID del usuario
     * @param correo Email del usuario
     * @param token Token de sesión generado
     */
    fun guardarSesion(usuarioId: Int, correo: String, token: String) {
        prefs.edit().apply {
            putInt(KEY_USUARIO_ID, usuarioId)
            putString(KEY_CORREO, correo)
            putString(KEY_TOKEN, token)
            putBoolean(KEY_LOGUEADO, true)
            apply()
        }
    }

    /**
     * Cierra la sesión (borra todos los datos guardados).
     */
    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }

    /**
     * Indica si hay una sesión activa (usuario logueado).
     */
    val estaLogueado: Boolean
        get() = prefs.getBoolean(KEY_LOGUEADO, false)

    /**
     * ID del usuario actualmente logueado.
     * @throws IllegalStateException si no hay sesión activa.
     */
    val usuarioIdActivo: Int
        get() {
            if (!estaLogueado) throw IllegalStateException("No hay sesión activa")
            return prefs.getInt(KEY_USUARIO_ID, -1)
        }

    /**
     * Correo del usuario actualmente logueado.
     */
    val correoActivo: String?
        get() = prefs.getString(KEY_CORREO, null)

    /**
     * Token de la sesión activa.
     */
    val tokenActivo: String?
        get() = prefs.getString(KEY_TOKEN, null)

    /**
     * Verifica si la sesión es válida (sin comprobar expiración en BD).
     * Puedes usarlo en lugar de `estaLogueado` si quieres una nomenclatura más clara.
     */
    fun isSesionActiva(): Boolean = estaLogueado
}