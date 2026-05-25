package com.example.parcial2_android.ui.incidencias

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.parcial2_android.R
import com.example.parcial2_android.databinding.ActivityMisIncidenciasBinding
import com.example.parcial2_android.ui.adapters.IncidenciasAdapter
import com.example.parcial2_android.ui.dashboard.DashboardActivity
import com.example.parcial2_android.ui.perfil.PerfilActivity
import kotlinx.coroutines.launch

class MisIncidenciasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMisIncidenciasBinding
    private val viewModel: MisIncidenciasViewModel by viewModels { MisIncidenciasViewModel.Factory }
    private lateinit var adapter: IncidenciasAdapter

    private var filtroEstado: String? = null
    private var filtroPrioridad: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMisIncidenciasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarRecyclerView()
        configurarBuscador()
        configurarFiltros()
        configurarFab()
        configurarBottomNav()
        observarDatos()
    }

    // ─── RECYCLERVIEW ───────────────────────────────────────────────────────
    private fun configurarRecyclerView() {
        adapter = IncidenciasAdapter(
            onClick = { incidencia ->
                val intent = Intent(this, NuevaIncidenciaActivity::class.java).apply {
                    putExtra(NuevaIncidenciaActivity.EXTRA_INCIDENCIA_ID, incidencia.id)
                    putExtra(NuevaIncidenciaActivity.EXTRA_MODO, NuevaIncidenciaActivity.MODO_EDICION)
                }
                startActivity(intent)
            },
            onDelete = { incidencia ->
                AlertDialog.Builder(this)
                    .setTitle("Eliminar incidencia")
                    .setMessage("¿Estás seguro de que quieres eliminar esta incidencia?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewModel.eliminarIncidencia(incidencia)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )
        binding.rvIncidencias.layoutManager = LinearLayoutManager(this)
        binding.rvIncidencias.adapter = adapter
    }

    // ─── BUSCADOR ───────────────────────────────────────────────────────────
    private fun configurarBuscador() {
        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.buscar(s?.toString()?.trim() ?: "", filtroEstado, filtroPrioridad)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ─── FILTROS ────────────────────────────────────────────────────────────
    private fun configurarFiltros() {
        configurarFiltrosEstado()
        configurarFiltrosPrioridad()
    }

    private fun configurarFiltrosEstado() {
        filtroEstado = null
        actualizarEstiloChip(binding.chipTodas, true)
        actualizarEstiloChip(binding.chipPendiente, false)
        actualizarEstiloChip(binding.chipEnRevision, false)
        actualizarEstiloChip(binding.chipResuelta, false)

        val chipsEstado = listOf(
            binding.chipTodas to null,
            binding.chipPendiente to "PENDIENTE",
            binding.chipEnRevision to "EN_REVISION",
            binding.chipResuelta to "RESUELTA"
        )

        chipsEstado.forEach { (chip, estado) ->
            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    filtroEstado = estado
                    chipsEstado.forEach { (c, _) ->
                        if (c == buttonView) {
                            actualizarEstiloChip(c, true)
                        } else {
                            c.isChecked = false
                            actualizarEstiloChip(c, false)
                        }
                    }
                    aplicarFiltros()
                }
            }
        }
    }

    private fun configurarFiltrosPrioridad() {
        // Agregar chip "Todas" para prioridad
        actualizarEstiloChip(binding.chipTodasPrioridad, true)
        actualizarEstiloChip(binding.chipAlta, false)
        actualizarEstiloChip(binding.chipMedia, false)
        actualizarEstiloChip(binding.chipBaja, false)

        filtroPrioridad = null  // Inicialmente mostrar todas

        val chipsPrioridad = listOf(
            binding.chipTodasPrioridad to null,
            binding.chipAlta to "ALTA",
            binding.chipMedia to "MEDIA",
            binding.chipBaja to "BAJA"
        )

        chipsPrioridad.forEach { (chip, prioridad) ->
            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    filtroPrioridad = prioridad
                    chipsPrioridad.forEach { (c, _) ->
                        if (c == buttonView) {
                            actualizarEstiloChip(c, true)
                        } else {
                            c.isChecked = false
                            actualizarEstiloChip(c, false)
                        }
                    }
                    aplicarFiltros()
                }
            }
        }
    }

    private fun actualizarEstiloChip(chip: com.google.android.material.chip.Chip, seleccionado: Boolean) {
        if (seleccionado) {
            chip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.purple_primary))
            chip.setTextColor(ContextCompat.getColor(this, R.color.white))
            chip.chipStrokeColor = ContextCompat.getColorStateList(this, R.color.purple_primary)
        } else {
            chip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.white))
            chip.setTextColor(ContextCompat.getColor(this, R.color.purple_primary))
            chip.chipStrokeColor = ContextCompat.getColorStateList(this, R.color.purple_light)
        }
    }

    private fun aplicarFiltros() {
        val query = binding.etBuscar.text?.toString()?.trim() ?: ""
        viewModel.buscar(query, filtroEstado, filtroPrioridad)
    }

    // ─── FAB ────────────────────────────────────────────────────────────────
    private fun configurarFab() {
        binding.fabNuevaIncidencia.setOnClickListener {
            val intent = Intent(this, NuevaIncidenciaActivity::class.java).apply {
                putExtra(NuevaIncidenciaActivity.EXTRA_MODO, NuevaIncidenciaActivity.MODO_CREACION)
            }
            startActivity(intent)
        }
    }

    // ─── BOTTOM NAV ─────────────────────────────────────────────────────────
    private fun configurarBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_incidencias
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_incidencias -> true // ya estamos aquí
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_notificaciones -> {
                    // startActivity(Intent(this, NotificacionesActivity::class.java))
                    true
                }
                R.id.nav_perfil -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    true
                }
                else -> true
            }
        }
    }

    // ─── OBSERVADORES ───────────────────────────────────────────────────────
    private fun observarDatos() {
        lifecycleScope.launch {
            viewModel.incidenciasFiltradas.collect { lista ->
                adapter.submitList(lista)
                binding.llEstadoVacio.visibility =
                    if (lista.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.rvIncidencias.visibility =
                    if (lista.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            }
        }
    }
}