package com.example.parcial2_android.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.parcial2_android.R
import com.example.parcial2_android.databinding.ActivityLoginBinding
import com.example.parcial2_android.data.local.BaseDeDatos
import com.example.parcial2_android.utils.HashHelper
import com.example.parcial2_android.utils.SessionManager
import com.example.parcial2_android.ui.auth.RegistroActivity  // ← AGREGA ESTA LÍNEA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var database: BaseDeDatos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        database = BaseDeDatos.obtenerInstancia(this)

        // Verificar sesión activa
        if (sessionManager.isSesionActiva()) {
            irAlMain()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnIniciarSesion.setOnClickListener {
            realizarLogin()
        }

        binding.txtOlvideClave.setOnClickListener {
            // Recuperación de contraseña simulada
            startActivity(Intent(this, RecuperarContrasenaActivity::class.java))
        }

        binding.btnCrearCuenta.setOnClickListener {
            startActivity(RegistroActivity.getIntent(this))
        }

        binding.btnTogglePassword.setOnClickListener {
            val tipo = binding.etPassword.inputType
            if (tipo == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                binding.etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            } else {
                binding.etPassword.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility)
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }
    }

    private fun realizarLogin() {
        val correo = binding.etCorreo.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // Validaciones
        if (correo.isEmpty()) {
            binding.etCorreo.error = "El correo es requerido"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.etCorreo.error = "Correo inválido (ej: usuario@utp.ac.pa)"
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "La contraseña es requerida"
            return
        }

        // Bloquear UI mientras se valida
        binding.btnIniciarSesion.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            val usuario = database.daoUsuario().buscarPorCorreo(correo)

            withContext(Dispatchers.Main) {
                binding.btnIniciarSesion.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE

                if (usuario == null) {
                    Toast.makeText(this@LoginActivity, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                if (HashHelper.verificarContrasena(password, usuario.contrasenaHash)) {
                    // Login exitoso
                    val token = UUID.randomUUID().toString()
                    val expira = (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L).toString()

                    lifecycleScope.launch(Dispatchers.IO) {
                        database.daoUsuario().actualizarTokenSesion(usuario.id, token, expira)
                    }

                    // Guardar sesión en SharedPreferences solo si marcó "Mantener sesión"
                    if (binding.chkMantenerSesion.isChecked) {
                        sessionManager.guardarSesion(usuario.id, usuario.correo, token)
                    }

                    Toast.makeText(
                        this@LoginActivity,
                        "Bienvenido ${usuario.nombreCompleto}",
                        Toast.LENGTH_SHORT
                    ).show()

                    irAlMain()
                } else {
                    binding.etPassword.error = "Contraseña incorrecta"
                    Toast.makeText(this@LoginActivity, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun irAlMain() {
        startActivity(android.content.Intent(this, com.example.parcial2_android.ui.main.MainActivity::class.java))
        finish()
    }

    companion object {
        fun getIntent(context: android.content.Context) = android.content.Intent(context, LoginActivity::class.java)
    }
}