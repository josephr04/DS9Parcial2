package com.example.parcial2_android.utils

import java.security.MessageDigest

object HashHelper {

    /**
     * Genera hash SHA-256 de un texto
     * @param input Texto a encriptar
     * @return String hexadecimal del hash
     */
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifica si un texto plano coincide con un hash
     * @param plainText Texto plano ingresado por el usuario
     * @param hash Hash almacenado en la base de datos
     * @return true si coinciden, false si no
     */
    fun verificarContrasena(plainText: String, hash: String): Boolean {
        return sha256(plainText) == hash
    }
}