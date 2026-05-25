package com.example.parcial2_android.ui.incidencias

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.parcial2_android.data.local.BaseDeDatos
import com.example.parcial2_android.data.local.entidad.EntidadIncidencia
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MisIncidenciasViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = BaseDeDatos.obtenerInstancia(app).daoIncidencia()

    // Estado del buscador y filtros
    private val _query = MutableStateFlow("")
    private val _filtroEstado = MutableStateFlow<String?>(null)
    private val _filtroPrioridad = MutableStateFlow<String?>(null)

    /**
     * Combina búsqueda + filtros en un único Flow reactivo.
     * Cada vez que cambia query, estado o prioridad se re-emite la lista.
     */
    val incidenciasFiltradas: StateFlow<List<EntidadIncidencia>> =
        combine(_query, _filtroEstado, _filtroPrioridad) { query, estado, prioridad ->
            Triple(query, estado, prioridad)
        }.flatMapLatest { (query, estado, prioridad) ->
            dao.obtenerFiltradas(estado, prioridad).map { lista ->
                if (query.isBlank()) lista
                else lista.filter { incidencia ->
                    incidencia.titulo.contains(query, ignoreCase = true) ||
                            incidencia.descripcion.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Actualiza el texto de búsqueda y los filtros activos. */
    fun buscar(query: String, estado: String?, prioridad: String?) {
        _query.value = query
        _filtroEstado.value = estado
        _filtroPrioridad.value = prioridad
    }

    // ✅ FIX: función faltante que llama MisIncidenciasActivity
    fun eliminarIncidencia(incidencia: EntidadIncidencia) {
        viewModelScope.launch {
            dao.eliminar(incidencia)
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return MisIncidenciasViewModel(app) as T
            }
        }
    }
}