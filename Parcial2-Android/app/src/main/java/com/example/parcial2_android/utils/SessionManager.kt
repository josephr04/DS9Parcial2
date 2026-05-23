package com.example.parcial2_android.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sesion_app", Context.MODE_PRIVATE)

    fun guardarSesion(usuarioId: Int, email: String, token: String) {
        prefs.edit().apply {
            putInt("usuario_id", usuarioId)
            putString("email", email)
            putString("token", token)
            putBoolean("activa", true)
            apply()
        }
    }

    fun obtenerUsuarioId(): Int = prefs.getInt("usuario_id", -1)

    fun obtenerEmail(): String? = prefs.getString("email", null)

    fun obtenerToken(): String? = prefs.getString("token", null)

    fun isSesionActiva(): Boolean {
        val activa = prefs.getBoolean("activa", false)
        return activa
    }

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}