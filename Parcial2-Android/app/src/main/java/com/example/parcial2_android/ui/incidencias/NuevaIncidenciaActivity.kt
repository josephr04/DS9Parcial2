package com.example.parcial2_android.ui.incidencias

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.parcial2_android.R
import com.example.parcial2_android.databinding.ActivityNuevaIncidenciaBinding
import com.example.parcial2_android.ui.adapters.ImagenesAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NuevaIncidenciaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_INCIDENCIA_ID = "incidencia_id"
        const val EXTRA_MODO = "modo"
        const val MODO_CREACION = "creacion"
        const val MODO_EDICION = "edicion"
    }

    private lateinit var binding: ActivityNuevaIncidenciaBinding
    private val viewModel: NuevaIncidenciaViewModel by viewModels {
        NuevaIncidenciaViewModel.factory(application)
    }

    private lateinit var fusedLocation: FusedLocationProviderClient
    private lateinit var imagenesAdapter: ImagenesAdapter
    private var uriFotoActual: Uri? = null
    private var prioridadSeleccionada: String = "BAJA"
    private var modoEdicion = false
    private var incidenciaIdEditar = -1

    private val permisoCamaraLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) abrirCamara() else toast("Permiso de cámara denegado")
    }

    private val permisoUbicacionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val ok = permisos.values.any { it }
        if (ok) capturarUbicacion() else toast("Permiso de ubicación denegado")
    }

    private val resultadoCamara = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito && uriFotoActual != null) {
            val rutaLocal = copiarUriAInterno(uriFotoActual!!)
            if (rutaLocal != null) {
                viewModel.agregarImagen(rutaLocal)
                actualizarListaImagenes()
            }
        }
    }

    private val resultadoGaleria = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val rutaLocal = copiarUriAInterno(uri)
            if (rutaLocal != null) {
                viewModel.agregarImagen(rutaLocal)
            }
        }
        actualizarListaImagenes()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        super.onCreate(savedInstanceState)
        binding = ActivityNuevaIncidenciaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocation = LocationServices.getFusedLocationProviderClient(this)

        // ✅ Detectar modo desde el Intent
        modoEdicion = intent.getStringExtra(EXTRA_MODO) == MODO_EDICION
        incidenciaIdEditar = intent.getIntExtra(EXTRA_INCIDENCIA_ID, -1)

        // ✅ Cambiar título según el modo
        if (modoEdicion) {
            binding.tvTituloAppBar.text = "Editar Incidencia"
        } else {
            binding.tvTituloAppBar.text = "Nueva Incidencia"
        }

        configurarClickListeners()
        configurarCategorias()
        configurarPrioridad()
        configurarFotos()
        configurarUbicacion()
        configurarMapa()
        configurarBotones()
        observarViewModel()
        mostrarFechaActual()

        // ✅ Si es edición, cargar los datos existentes
        if (modoEdicion && incidenciaIdEditar != -1) {
            viewModel.cargarIncidencia(incidenciaIdEditar)
        }
    }

    private fun configurarClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun configurarCategorias() {
        val categorias = listOf(
            "Infraestructura dañada",
            "Problemas eléctricos",
            "Equipos de laboratorio",
            "Fallas de conectividad",
            "Problemas de seguridad",
            "Emergencias académicas"
        )
        val adapterSpinner = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line, categorias
        )
        binding.actvCategoria.setAdapter(adapterSpinner)
    }

    private fun configurarPrioridad() {
        val chipsPrioridad = listOf(
            binding.chipBaja to "BAJA",
            binding.chipMedia to "MEDIA",
            binding.chipAlta to "ALTA"
        )

        actualizarEstiloChipPrioridad(binding.chipBaja, true)
        actualizarEstiloChipPrioridad(binding.chipMedia, false)
        actualizarEstiloChipPrioridad(binding.chipAlta, false)
        prioridadSeleccionada = "BAJA"

        chipsPrioridad.forEach { (chip, prioridad) ->
            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    prioridadSeleccionada = prioridad
                    chipsPrioridad.forEach { (c, _) ->
                        if (c == buttonView) actualizarEstiloChipPrioridad(c, true)
                        else {
                            c.isChecked = false
                            actualizarEstiloChipPrioridad(c, false)
                        }
                    }
                }
            }
        }
    }

    private fun seleccionarChipPrioridad(prioridad: String) {
        prioridadSeleccionada = prioridad
        binding.chipBaja.isChecked = prioridad == "BAJA"
        binding.chipMedia.isChecked = prioridad == "MEDIA"
        binding.chipAlta.isChecked = prioridad == "ALTA"

        actualizarEstiloChipPrioridad(binding.chipBaja, prioridad == "BAJA")
        actualizarEstiloChipPrioridad(binding.chipMedia, prioridad == "MEDIA")
        actualizarEstiloChipPrioridad(binding.chipAlta, prioridad == "ALTA")
    }

    private fun actualizarEstiloChipPrioridad(chip: com.google.android.material.chip.Chip, seleccionado: Boolean) {
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

    private fun configurarFotos() {
        // Configurar RecyclerView para la lista de imágenes
        actualizarListaImagenes()

        binding.cvTomarFoto.setOnClickListener {
            val permiso = Manifest.permission.CAMERA
            if (ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED)
                abrirCamara()
            else
                permisoCamaraLauncher.launch(permiso)
        }
        binding.cvSeleccionarGaleria.setOnClickListener {
            resultadoGaleria.launch("image/*")
        }
    }

    private fun actualizarListaImagenes() {
        imagenesAdapter = ImagenesAdapter(
            imagenes = viewModel.rutasFoto,
            onVer = { ruta ->
                mostrarImagenEnDialogo(ruta)
            },
            onEliminar = { ruta, position ->
                viewModel.removerImagen(ruta)
                actualizarListaImagenes()
                toast("Imagen eliminada")
            }
        )
        binding.rvImagenes.layoutManager = LinearLayoutManager(this)
        binding.rvImagenes.adapter = imagenesAdapter
    }

    private fun mostrarImagenEnDialogo(ruta: String) {
        val archivo = File(ruta)
        if (!archivo.exists()) {
            toast("La imagen no existe")
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_imagen, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.ivImagenGrande)

        try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
            }
            val bitmap = BitmapFactory.decodeFile(ruta, options)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        AlertDialog.Builder(this)
            .setTitle("Vista previa")
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun abrirCamara() {
        try {
            val archivo = crearArchivoFoto()
            uriFotoActual = FileProvider.getUriForFile(this, "${packageName}.provider", archivo)
            resultadoCamara.launch(uriFotoActual!!)
        } catch (e: Exception) {
            toast("Error al abrir cámara: ${e.message}")
        }
    }

    private fun crearArchivoFoto(): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(filesDir, "IMG_${ts}.jpg")
    }

    private fun copiarUriAInterno(uri: Uri): String? {
        return try {
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val destino = File(filesDir, "IMG_${ts}.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                destino.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destino.absolutePath
        } catch (e: Exception) {
            toast("Error al guardar imagen: ${e.message}")
            null
        }
    }

    private fun configurarUbicacion() {
        binding.btnCapturarUbicacion.setOnClickListener {
            val permisos = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            val yaOk = permisos.any {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }
            if (yaOk) capturarUbicacion() else permisoUbicacionLauncher.launch(permisos)
        }
    }

    private fun mostrarFechaActual() {
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val horaFormat = SimpleDateFormat("HH:mm:ss", Locale("es", "ES"))
        val diaFormat = SimpleDateFormat("dd", Locale("es", "ES"))
        val mesFormat = SimpleDateFormat("MMM", Locale("es", "ES"))

        val fecha = Date()

        val fechaFormateada = dateFormat.format(fecha).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        binding.tvFechaActual.text = fechaFormateada
        binding.tvHoraActual.text = horaFormat.format(fecha)
        binding.tvDia.text = diaFormat.format(fecha)
        binding.tvMes.text = mesFormat.format(fecha).uppercase()
    }

    private fun capturarUbicacion() {
        try {
            fusedLocation.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) actualizarCoordenadas(loc.latitude, loc.longitude)
                else toast("No se pudo obtener la ubicación. Active el GPS.")
            }.addOnFailureListener { toast("Error GPS: ${it.message}") }
        } catch (e: SecurityException) {
            toast("Permiso de ubicación requerido")
        }
    }

    private fun actualizarCoordenadas(lat: Double, lng: Double) {
        binding.tvLatitud.text = String.format(Locale.US, "%.6f", lat)
        binding.tvLongitud.text = String.format(Locale.US, "%.6f", lng)
        viewModel.latitud = lat
        viewModel.longitud = lng
        moverMapaA(lat, lng)
    }

    private fun configurarMapa() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(-12.046374, -77.042793))
        }
        agregarMarcador(-12.046374, -77.042793, "UTP Sede Central")

        val tapReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                actualizarCoordenadas(p.latitude, p.longitude)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean {
                actualizarCoordenadas(p.latitude, p.longitude)
                return true
            }
        }
        binding.mapView.overlays.add(MapEventsOverlay(tapReceiver))
    }

    private fun moverMapaA(lat: Double, lng: Double) {
        val punto = GeoPoint(lat, lng)
        binding.mapView.overlays.clear()

        val tapReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                actualizarCoordenadas(p.latitude, p.longitude)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean {
                actualizarCoordenadas(p.latitude, p.longitude)
                return true
            }
        }
        binding.mapView.overlays.add(MapEventsOverlay(tapReceiver))
        binding.mapView.controller.animateTo(punto)
        binding.mapView.controller.setZoom(17.0)
        agregarMarcador(lat, lng, "Ubicación de la incidencia")
        binding.mapView.invalidate()
    }

    private fun agregarMarcador(lat: Double, lng: Double, titulo: String) {
        val marker = Marker(binding.mapView).apply {
            position = GeoPoint(lat, lng)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = titulo
        }
        binding.mapView.overlays.add(marker)
        binding.mapView.invalidate()
    }

    private fun configurarBotones() {
        binding.btnGuardar.setOnClickListener {
            if (validarCampos()) {
                if (modoEdicion && incidenciaIdEditar != -1) {
                    viewModel.actualizarIncidencia(
                        id = incidenciaIdEditar,
                        titulo = binding.etTitulo.text.toString().trim(),
                        categoria = binding.actvCategoria.text.toString(),
                        descripcion = binding.etDescripcion.text.toString().trim(),
                        prioridad = prioridadSeleccionada,
                        latitud = viewModel.latitud,
                        longitud = viewModel.longitud
                    )
                } else {
                    viewModel.guardarIncidencia(
                        titulo = binding.etTitulo.text.toString().trim(),
                        categoria = binding.actvCategoria.text.toString(),
                        descripcion = binding.etDescripcion.text.toString().trim(),
                        prioridad = prioridadSeleccionada,
                        latitud = viewModel.latitud,
                        longitud = viewModel.longitud
                    )
                }
            }
        }
        binding.btnCancelar.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun validarCampos(): Boolean {
        var ok = true
        if (binding.etTitulo.text.isNullOrBlank()) {
            binding.tilTitulo.error = "El título es obligatorio"
            ok = false
        } else binding.tilTitulo.error = null

        if (binding.actvCategoria.text.isNullOrBlank()) {
            binding.tilCategoria.error = "Selecciona una categoría"
            ok = false
        } else binding.tilCategoria.error = null

        if (binding.etDescripcion.text.isNullOrBlank()) {
            binding.tilDescripcion.error = "La descripción es obligatoria"
            ok = false
        } else binding.tilDescripcion.error = null

        return ok
    }

    private fun observarViewModel() {
        viewModel.incidenciaEditar.observe(this) { incidencia ->
            if (incidencia == null) return@observe

            binding.etTitulo.setText(incidencia.titulo)
            binding.etDescripcion.setText(incidencia.descripcion)

            val categorias = listOf(
                "Infraestructura dañada",
                "Problemas eléctricos",
                "Equipos de laboratorio",
                "Fallas de conectividad",
                "Problemas de seguridad",
                "Emergencias académicas"
            )
            val nombreCategoria = categorias.getOrNull(incidencia.categoriaId - 1) ?: ""
            binding.actvCategoria.setText(nombreCategoria, false)

            seleccionarChipPrioridad(incidencia.prioridad)

            if (incidencia.latitud != null && incidencia.longitud != null) {
                actualizarCoordenadas(incidencia.latitud, incidencia.longitud)
            }

            viewModel.limpiarImagenes()
            if (!incidencia.rutaFoto.isNullOrBlank()) {
                incidencia.rutaFoto.split(",").forEach { ruta ->
                    val archivo = File(ruta.trim())
                    if (archivo.exists()) viewModel.agregarImagen(ruta.trim())
                }
            }
            actualizarListaImagenes()
        }

        viewModel.guardadoExitoso.observe(this) {
            if (it) {
                val msg = if (modoEdicion) "Incidencia actualizada" else "Incidencia guardada"
                toast(msg)
                finish()
            }
        }

        viewModel.actualizadoExitoso.observe(this) {
            if (it) {
                toast("Incidencia actualizada")
                finish()
            }
        }

        viewModel.error.observe(this) {
            if (!it.isNullOrBlank()) toast(it)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}