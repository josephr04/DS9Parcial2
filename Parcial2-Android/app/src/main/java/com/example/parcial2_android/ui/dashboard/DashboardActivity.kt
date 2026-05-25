package com.example.parcial2_android.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.parcial2_android.R
import com.example.parcial2_android.data.local.BaseDeDatos
import com.example.parcial2_android.databinding.ActivityDashboardBinding
import com.example.parcial2_android.ui.auth.LoginActivity
import com.example.parcial2_android.ui.incidencias.MisIncidenciasActivity
import com.example.parcial2_android.ui.notificaciones.NotificacionesActivity
import com.example.parcial2_android.ui.perfil.PerfilActivity
import com.example.parcial2_android.utils.SessionManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

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

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cargarEstadisticas()
        configurarBottomNav()
    }

    private fun cargarEstadisticas() {
        lifecycleScope.launch {
            val dao = BaseDeDatos.obtenerInstancia(applicationContext).daoIncidencia()

            val total      = withContext(Dispatchers.IO) { dao.contarTodas() }
            val pendientes = withContext(Dispatchers.IO) { dao.contarPorEstado("PENDIENTE") }
            val enRevision = withContext(Dispatchers.IO) { dao.contarPorEstado("EN_REVISION") }
            val resueltas  = withContext(Dispatchers.IO) { dao.contarPorEstado("RESUELTA") }
            val categorias = withContext(Dispatchers.IO) { dao.contarPorCategoria() }

            // Tarjetas
            binding.tvTotal.text      = total.toString()
            binding.tvPendientes.text = pendientes.toString()
            binding.tvEnRevision.text = enRevision.toString()
            binding.tvResueltas.text  = resueltas.toString()

            // Leyenda del pie chart
            val pPend = if (total > 0) Math.round(pendientes * 100f / total) else 0
            val pRev  = if (total > 0) Math.round(enRevision * 100f / total) else 0
            val pRes  = if (total > 0) Math.round(resueltas * 100f / total) else 0
            binding.tvLeyendaPendientes.text = "Pendientes ($pPend%)"
            binding.tvLeyendaRevision.text   = "En Revisión ($pRev%)"
            binding.tvLeyendaResueltas.text  = "Resueltas ($pRes%)"

            // Barras de categorías dinámicas
            actualizarBarrasCategorias(categorias, total)

            // Pie chart
            configurarPieChart(pendientes, enRevision, resueltas)
        }
    }

    private fun actualizarBarrasCategorias(
        categorias: List<com.example.parcial2_android.data.local.dao.ConteoCategoriaNombre>,
        total: Int
    ) {
        val mapa = categorias.associate { cat ->
            cat.nombre.uppercase() to
                    if (total > 0) Math.round(cat.total * 100f / total) else 0
        }

        fun pct(keyword: String) = mapa.entries
            .firstOrNull { entry ->
                val normalizado = entry.key
                    .replace("É", "E")
                    .replace("Á", "A")
                    .replace("Í", "I")
                    .replace("Ó", "O")
                    .replace("Ú", "U")
                normalizado.contains(keyword.uppercase())
            }?.value ?: 0

        val pctInfra    = pct("INFRAESTRUCTURA")
        val pctElectric = pct("ELECTR")
        val pctLab      = pct("LABORATORIO")
        val pctConect   = pct("CONECTIVIDAD")
        val pctSegur    = pct("SEGURIDAD")
        val pctAcadem   = pct("ACAD")

        binding.pbInfraestructura.progress = pctInfra
        binding.pbElectricos.progress      = pctElectric
        binding.pbLaboratorio.progress     = pctLab
        binding.pbConectividad.progress    = pctConect
        binding.pbSeguridad.progress       = pctSegur
        binding.pbAcademicas.progress      = pctAcadem

        binding.tvPctInfraestructura.text  = "$pctInfra%"
        binding.tvPctElectricos.text       = "$pctElectric%"
        binding.tvPctLaboratorio.text      = "$pctLab%"
        binding.tvPctConectividad.text     = "$pctConect%"
        binding.tvPctSeguridad.text        = "$pctSegur%"
        binding.tvPctAcademicas.text       = "$pctAcadem%"
    }

    private fun configurarPieChart(pendientes: Int, enRevision: Int, resueltas: Int) {
        val total = (pendientes + enRevision + resueltas).toFloat()
        if (total == 0f) return

        val entries = listOf(
            PieEntry(pendientes / total * 100f, "Pendientes"),
            PieEntry(enRevision / total * 100f, "En Revisión"),
            PieEntry(resueltas  / total * 100f, "Resueltas")
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                android.graphics.Color.parseColor("#E24B4A"),
                android.graphics.Color.parseColor("#BA7517"),
                android.graphics.Color.parseColor("#3B6D11")
            )
            sliceSpace = 3f
            valueTextSize = 12f
            valueTextColor = android.graphics.Color.WHITE
        }

        binding.pieChart.apply {
            data = PieData(dataSet).apply {
                setValueFormatter(PercentFormatter(binding.pieChart))
            }
            isDrawHoleEnabled = true
            holeRadius = 58f
            transparentCircleRadius = 61f
            description.isEnabled = false
            legend.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelColor(android.graphics.Color.TRANSPARENT)
            animateY(800)
            invalidate()
        }
    }

    private fun configurarBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_dashboard
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_incidencias -> {
                    startActivity(Intent(this, MisIncidenciasActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_dashboard -> true
                R.id.nav_notificaciones -> {
                    startActivity(Intent(this, NotificacionesActivity::class.java))
                    finish()
                    true
                }
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