package com.example.parcial2_android.ui.incidencias

import android.app.Application
import androidx.lifecycle.*
import com.example.parcial2_android.data.local.BaseDeDatos
import com.example.parcial2_android.data.local.entidad.EntidadIncidencia
import com.example.parcial2_android.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NuevaIncidenciaViewModel(private val app: Application) : AndroidViewModel(app) {

    private val daoIncidencia = BaseDeDatos.obtenerInstancia(app).daoIncidencia()
    private val sessionManager = SessionManager(app)

    var prioridadSeleccionada: String = "BAJA"
    var latitud: Double? = -12.046374
    var longitud: Double? = -77.042793

    private val _rutasFoto = mutableListOf<String>()
    val rutasFoto: List<String> = _rutasFoto

    private val _guardadoExitoso = MutableLiveData<Boolean>()
    val guardadoExitoso: LiveData<Boolean> = _guardadoExitoso

    private val _actualizadoExitoso = MutableLiveData<Boolean>()
    val actualizadoExitoso: LiveData<Boolean> = _actualizadoExitoso

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _incidenciaEditar = MutableLiveData<EntidadIncidencia?>()
    val incidenciaEditar: LiveData<EntidadIncidencia?> = _incidenciaEditar

    private val usuarioIdActivo: Int
        get() = if (sessionManager.estaLogueado) sessionManager.usuarioIdActivo else 1

    fun agregarImagen(rutaAbsoluta: String) { _rutasFoto.add(rutaAbsoluta) }
    fun removerImagen(rutaAbsoluta: String) { _rutasFoto.remove(rutaAbsoluta) }
    fun limpiarImagenes() {
        _rutasFoto.clear()
    }
    fun cargarIncidencia(id: Int) {
        viewModelScope.launch {
            try {
                val incidencia = daoIncidencia.buscarPorId(id)
                _incidenciaEditar.postValue(incidencia)

                // Limpiar imágenes anteriores y cargar la existente
                _rutasFoto.clear()
                incidencia?.rutaFoto?.let { ruta ->
                    if (ruta.isNotBlank()) {
                        _rutasFoto.add(ruta)
                    }
                }
            } catch (e: Exception) {
                _error.postValue("Error al cargar incidencia: ${e.message}")
            }
        }
    }

    fun guardarIncidencia(
        titulo: String,
        categoria: String,
        descripcion: String,
        prioridad: String,
        latitud: Double?,
        longitud: Double?
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d("SESSION_DEBUG", "estaLogueado=${sessionManager.estaLogueado}, id=${if(sessionManager.estaLogueado) sessionManager.usuarioIdActivo else -999}")
                android.util.Log.d("NUEVA_INC", "=== GUARDANDO INCIDENCIA ===")
                android.util.Log.d("NUEVA_INC", "Título: $titulo")
                android.util.Log.d("NUEVA_INC", "Categoría string: '$categoria'")

                if (titulo.isBlank()) {
                    _error.postValue("El título es obligatorio")
                    return@launch
                }
                if (categoria.isBlank()) {
                    _error.postValue("La categoría es obligatoria")
                    return@launch
                }
                if (descripcion.isBlank()) {
                    _error.postValue("La descripción es obligatoria")
                    return@launch
                }

                val categoriaId = mapearCategoriaId(categoria)
                android.util.Log.d("NUEVA_INC", "Categoría ID mapeado: $categoriaId")

                val ahora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                val rutaFoto = _rutasFoto.firstOrNull()

                val incidencia = EntidadIncidencia(
                    usuarioId = usuarioIdActivo,
                    categoriaId = categoriaId,
                    titulo = titulo,
                    descripcion = descripcion,
                    prioridad = prioridad,
                    estado = "PENDIENTE",
                    rutaFoto = rutaFoto,
                    latitud = latitud ?: this@NuevaIncidenciaViewModel.latitud,
                    longitud = longitud ?: this@NuevaIncidenciaViewModel.longitud,
                    creadoEn = ahora,
                    actualizadoEn = ahora,
                    estaSincronizado = false
                )

                android.util.Log.d("NUEVA_INC", "Incidencia a guardar: $incidencia")

                daoIncidencia.insertar(incidencia)
                _guardadoExitoso.postValue(true)

                android.util.Log.d("NUEVA_INC", "Incidencia guardada exitosamente")

            } catch (e: Exception) {
                android.util.Log.e("NUEVA_INC", "Error: ${e.message}")
                _error.postValue("Error al guardar: ${e.message}")
            }
        }
    }

    fun actualizarIncidencia(
        id: Int,
        titulo: String,
        categoria: String,
        descripcion: String,
        prioridad: String,
        latitud: Double?,
        longitud: Double?
    ) {
        viewModelScope.launch {
            try {
                if (titulo.isBlank()) { _error.postValue("El título es obligatorio"); return@launch }
                if (categoria.isBlank()) { _error.postValue("La categoría es obligatoria"); return@launch }
                if (descripcion.isBlank()) { _error.postValue("La descripción es obligatoria"); return@launch }

                val ahora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                val original = daoIncidencia.buscarPorId(id)

                val incidenciaActualizada = EntidadIncidencia(
                    id               = id,
                    usuarioId        = usuarioIdActivo,
                    categoriaId      = mapearCategoriaId(categoria),
                    titulo           = titulo,
                    descripcion      = descripcion,
                    prioridad        = prioridad,
                    estado           = original?.estado ?: "PENDIENTE",
                    rutaFoto         = _rutasFoto.firstOrNull(),
                    latitud          = latitud ?: this@NuevaIncidenciaViewModel.latitud,
                    longitud         = longitud ?: this@NuevaIncidenciaViewModel.longitud,
                    creadoEn         = original?.creadoEn ?: ahora,
                    actualizadoEn    = ahora,
                    estaSincronizado = false
                )

                daoIncidencia.actualizar(incidenciaActualizada)
                _actualizadoExitoso.postValue(true)

            } catch (e: Exception) {
                _error.postValue("Error al actualizar: ${e.message}")
            }
        }
    }

    private fun mapearCategoriaId(nombre: String): Int = when (nombre.trim()) {
        "Infraestructura dañada" -> 1
        "Problemas eléctricos" -> 2
        "Equipos de laboratorio" -> 3
        "Fallas de conectividad" -> 4
        "Problemas de seguridad" -> 5
        "Emergencias académicas" -> 6
        else -> 1  // Por defecto: Infraestructura dañada
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.AndroidViewModelFactory(app) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(NuevaIncidenciaViewModel::class.java))
                        return NuevaIncidenciaViewModel(app) as T
                    return super.create(modelClass)
                }
            }
    }
}