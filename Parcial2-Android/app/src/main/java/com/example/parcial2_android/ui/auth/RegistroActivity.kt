package com.example.parcial2_android.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.parcial2_android.R
import com.example.parcial2_android.databinding.ActivityRegistroBinding
import com.example.parcial2_android.data.local.BaseDeDatos
import com.example.parcial2_android.data.local.entidad.EntidadUsuario
import com.example.parcial2_android.utils.HashHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private lateinit var database: BaseDeDatos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = BaseDeDatos.obtenerInstancia(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegistrarse.setOnClickListener {
            realizarRegistro()
        }

        binding.txtIniciarSesion.setOnClickListener {
            finish() // Regresa al Login
        }

        // Toggle para el campo de contraseña
        binding.btnTogglePassword.setOnClickListener {
            togglePasswordVisibility(
                binding.etPassword,
                binding.btnTogglePassword
            )
        }

        // Toggle para el campo de confirmar contraseña
        binding.btnToggleConfirmPassword.setOnClickListener {
            togglePasswordVisibility(
                binding.etConfirmarPassword,
                binding.btnToggleConfirmPassword
            )
        }
    }

    /**
     * Función para alternar la visibilidad de la contraseña
     */
    private fun togglePasswordVisibility(editText: android.widget.EditText, imageView: android.widget.ImageView) {
        val tipo = editText.inputType
        if (tipo == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            // Ocultar contraseña
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            imageView.setImageResource(R.drawable.ic_visibility_off)
        } else {
            // Mostrar contraseña
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imageView.setImageResource(R.drawable.ic_visibility)
        }
        // Mover el cursor al final del texto
        editText.setSelection(editText.text.length)
    }

    private fun realizarRegistro() {
        val nombreCompleto = binding.etNombreCompleto.text.toString().trim()
        val correo = binding.etCorreo.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmarPassword.text.toString()

        // Validaciones
        if (nombreCompleto.isEmpty()) {
            binding.etNombreCompleto.error = "Nombre completo requerido"
            return
        }

        if (correo.isEmpty()) {
            binding.etCorreo.error = "Correo requerido"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.etCorreo.error = "Correo inválido (ej: usuario@utp.ac.pa)"
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Contraseña requerida"
            return
        }

        if (password.length < 6) {
            binding.etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        if (password != confirmPassword) {
            binding.etConfirmarPassword.error = "Las contraseñas no coinciden"
            return
        }

        // Generar nombre de usuario automáticamente desde el correo
        val nombreUsuario = generarNombreUsuario(correo)

        binding.btnRegistrarse.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            // Verificar si ya existe el correo
            val existeCorreo = database.daoUsuario().buscarPorCorreo(correo) != null
            val existeUsuario = database.daoUsuario().buscarPorNombreUsuario(nombreUsuario) != null

            withContext(Dispatchers.Main) {
                if (existeCorreo) {
                    binding.etCorreo.error = "Este correo ya está registrado"
                    binding.btnRegistrarse.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                    return@withContext
                }

                if (existeUsuario) {
                    Toast.makeText(
                        this@RegistroActivity,
                        "El nombre de usuario ${nombreUsuario} ya existe. Intenta con otro correo.",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnRegistrarse.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                    return@withContext
                }

                // Crear nuevo usuario
                val nuevoUsuario = EntidadUsuario(
                    nombreUsuario = nombreUsuario,
                    correo = correo,
                    contrasenaHash = HashHelper.sha256(password),
                    nombreCompleto = nombreCompleto,
                    rol = "ESTUDIANTE",
                    creadoEn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .format(Date())
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    val id = database.daoUsuario().insertar(nuevoUsuario)

                    withContext(Dispatchers.Main) {
                        binding.btnRegistrarse.isEnabled = true
                        binding.progressBar.visibility = android.view.View.GONE

                        if (id > 0) {
                            Toast.makeText(
                                this@RegistroActivity,
                                "¡Cuenta creada exitosamente! Ahora inicia sesión.",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        } else {
                            Toast.makeText(
                                this@RegistroActivity,
                                "Error al crear cuenta. Intenta de nuevo.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    /**
     * Genera nombre de usuario desde el correo (parte antes del @)
     * Ej: juan.perez@utp.ac.pa → juan.perez
     */
    private fun generarNombreUsuario(correo: String): String {
        val parteLocal = correo.substringBefore("@")
        // Eliminar puntos y caracteres especiales si quieres
        return parteLocal.lowercase()
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, RegistroActivity::class.java)
        }
    }
}