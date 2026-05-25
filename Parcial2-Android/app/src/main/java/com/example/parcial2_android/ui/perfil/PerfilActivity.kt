package com.example.parcial2_android.ui.perfil

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.parcial2_android.data.local.BaseDeDatos
import com.example.parcial2_android.utils.SessionManager
import com.example.parcial2_android.databinding.ActivityPerfilBinding
import com.example.parcial2_android.R
import com.example.parcial2_android.ui.auth.LoginActivity
import com.example.parcial2_android.ui.incidencias.MisIncidenciasActivity
import com.example.parcial2_android.ui.incidencias.NuevaIncidenciaActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private lateinit var sesion: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sesion = SessionManager(applicationContext)

        // Verificar sesión
        if (!sesion.estaLogueado) {
            irALogin()
            return
        }

        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarDatosUsuario()
        configurarBotones()
        configurarBottomNav()
    }

    private fun cargarDatosUsuario() {
        lifecycleScope.launch {
            val usuario = withContext(Dispatchers.IO) {
                BaseDeDatos.obtenerInstancia(applicationContext)
                    .daoUsuario()
                    .buscarPorId(sesion.usuarioIdActivo)
            }

            usuario?.let {
                binding.tvNombreCompleto.text = it.nombreCompleto
                binding.tvCorreo.text = it.correo

                // Badge de rol legible
                binding.tvRol.text = when (it.rol) {
                    "ADMIN"      -> "Administrador"
                    "DOCENTE"    -> "Docente"
                    else         -> "Estudiante / Administrativo"
                }

                // Iniciales para el avatar (máx 2 letras)
                val partes = it.nombreCompleto.trim().split(" ")
                val iniciales = when {
                    partes.size >= 2 -> "${partes[0].first()}${partes[1].first()}"
                    partes.size == 1 -> partes[0].take(2)
                    else             -> "??"
                }.uppercase()
                binding.tvIniciales.text = iniciales
            }
        }
    }

    private fun configurarBotones() {
        binding.btnCerrarSesion.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Sí, cerrar") { _, _ ->
                    sesion.cerrarSesion()
                    irALogin()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun configurarBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_perfil
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_incidencias -> {
                    startActivity(Intent(this, MisIncidenciasActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_dashboard -> {
                    // startActivity(Intent(this, DashboardActivity::class.java))
                    true
                }
                R.id.nav_notificaciones -> {
                    // startActivity(Intent(this, NotificacionesActivity::class.java))
                    true
                }
                R.id.nav_perfil -> true // ya estamos aquí
                else -> true
            }
        }
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}