package com.example.parcial2_android.ui.notificaciones

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.parcial2_android.R
import com.example.parcial2_android.databinding.ActivityNotificacionesBinding
import com.example.parcial2_android.ui.auth.LoginActivity
import com.example.parcial2_android.ui.dashboard.DashboardActivity
import com.example.parcial2_android.ui.incidencias.MisIncidenciasActivity
import com.example.parcial2_android.ui.perfil.PerfilActivity
import com.example.parcial2_android.utils.SessionManager
import com.google.android.material.badge.BadgeDrawable

class NotificacionesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificacionesBinding
    private lateinit var adapter: NotificacionesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sesion = SessionManager(applicationContext)
        if (!sesion.estaLogueado) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityNotificacionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarRecyclerView()
        configurarBottomNav()

        // Al entrar a la pantalla, refrescar el badge del nav
        actualizarBadge()

        // Botón "Marcar todas como leídas"
        binding.tvMarcarLeidas.setOnClickListener {
            adapter.marcarTodasLeidas()
            binding.tvMarcarLeidas.visibility = View.GONE
            actualizarBadge()
        }
    }

    // ── Mock data ────────────────────────────────────────────────────────────

    private fun generarMockData(): MutableList<NotificacionItem> = mutableListOf(
        NotificacionItem(
            id = 1,
            tipo = TipoNotificacion.CAMBIO_ESTADO,
            titulo = "Cambio de estado",
            descripcion = "Tu incidencia #1234 ha pasado a Resuelta.",
            tiempo = "Hace 5 min",
            leida = false
        ),
        NotificacionItem(
            id = 2,
            tipo = TipoNotificacion.ACTUALIZACION,
            titulo = "Nueva actualización",
            descripcion = "Se ha añadido un comentario a tu reporte por parte del administrador de infraestructura.",
            tiempo = "Hace 2h",
            leida = false
        ),
        NotificacionItem(
            id = 3,
            tipo = TipoNotificacion.ANUNCIO,
            titulo = "Mantenimiento programado",
            descripcion = "El sistema entrará en mantenimiento este sábado a las 22:00. Las incidencias enviadas se procesarán el lunes.",
            tiempo = "Hace 1 día",
            leida = true          // esta ya estaba leída
        )
    )

    // ── RecyclerView ─────────────────────────────────────────────────────────

    private fun configurarRecyclerView() {
        val lista = generarMockData()

        adapter = NotificacionesAdapter(lista) { _ ->
            // Cada vez que se marca una como leída, refrescar badge y botón
            actualizarBadge()
            actualizarBotonMarcar()
        }

        binding.rvNotificaciones.layoutManager = LinearLayoutManager(this)
        binding.rvNotificaciones.adapter = adapter

        actualizarBotonMarcar()
    }

    // ── Badge en el BottomNav ────────────────────────────────────────────────

    /**
     * Muestra u oculta el badge rojo en el ítem de Notificaciones
     * según la cantidad de no leídas.
     */
    private fun actualizarBadge() {
        val unread = adapter.countUnread()
        val badge: BadgeDrawable =
            binding.bottomNav.getOrCreateBadge(R.id.nav_notificaciones)

        if (unread > 0) {
            badge.isVisible = true
            badge.number    = unread
        } else {
            badge.isVisible = false
            binding.bottomNav.removeBadge(R.id.nav_notificaciones)
        }
    }

    private fun actualizarBotonMarcar() {
        binding.tvMarcarLeidas.visibility =
            if (adapter.countUnread() > 0) View.VISIBLE else View.GONE
    }

    // ── BottomNav ────────────────────────────────────────────────────────────

    private fun configurarBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_notificaciones

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_incidencias -> {
                    startActivity(Intent(this, MisIncidenciasActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_notificaciones -> true
                R.id.nav_perfil -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    finish()
                    true
                }
                else -> true
            }
        }
    }
}