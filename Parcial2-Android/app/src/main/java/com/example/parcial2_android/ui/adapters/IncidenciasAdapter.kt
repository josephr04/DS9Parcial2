package com.example.parcial2_android.ui.adapters

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.parcial2_android.R
import com.example.parcial2_android.data.local.entidad.EntidadIncidencia
import com.example.parcial2_android.databinding.ItemIncidenciaBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class IncidenciasAdapter(
    private val onClick: (EntidadIncidencia) -> Unit,
    private val onDelete: ((EntidadIncidencia) -> Unit)? = null
) : ListAdapter<EntidadIncidencia, IncidenciasAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(
        private val binding: ItemIncidenciaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EntidadIncidencia) {
            // Categoría
            binding.tvCategoria.text = mapearNombreCategoria(item.categoriaId)

            // Fecha
            binding.tvFecha.text = formatearFecha(item.creadoEn)

            // Título
            binding.tvTitulo.text = item.titulo

            // Prioridad
            val (textoPrioridad, colorPrioridad) = coloresPrioridad(item.prioridad)
            binding.tvPrioridad.text = textoPrioridad
            binding.tvPrioridad.setTextColor(Color.parseColor(colorPrioridad))

            // Estado
            val (textoEstado, colorEstado) = coloresEstado(item.estado)
            binding.tvEstado.text = textoEstado
            binding.tvEstado.setTextColor(Color.parseColor(colorEstado))

            // Imagen
            cargarImagen(item.rutaFoto)

            // Botón eliminar
            binding.btnEliminar.visibility = if (onDelete != null) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            binding.btnEliminar.setOnClickListener {
                onDelete?.invoke(item)
            }

            binding.root.setOnClickListener { onClick(item) }
        }

        private fun cargarImagen(rutaFoto: String?) {
            // Tomar solo la primera ruta (pueden venir separadas por coma)
            val primeraRuta = rutaFoto?.split(",")?.firstOrNull()?.trim()

            if (!primeraRuta.isNullOrBlank()) {
                val archivo = File(primeraRuta)
                if (archivo.exists()) {
                    try {
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 2
                        }
                        val bitmap = BitmapFactory.decodeFile(primeraRuta, options)
                        if (bitmap != null) {
                            binding.ivCategoriaImagen.setImageBitmap(bitmap)
                            return
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            binding.ivCategoriaImagen.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        private fun formatearFecha(iso: String): String {
            return try {
                val formatoEntrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val fecha = formatoEntrada.parse(iso)
                val formatoSalida = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PA"))
                formatoSalida.format(fecha)
            } catch (e: Exception) {
                iso.take(10)
            }
        }

        private fun mapearNombreCategoria(id: Int): String = when (id) {
            1 -> "INFRAESTRUCTURA"
            2 -> "ELÉCTRICO"
            3 -> "LABORATORIO"
            4 -> "CONECTIVIDAD"
            5 -> "SEGURIDAD"
            6 -> "ACADÉMICO"
            else -> "GENERAL"
        }

        private fun coloresPrioridad(prioridad: String): Pair<String, String> = when (prioridad) {
            "ALTA" -> "ALTA" to "#E74C3C"
            "MEDIA" -> "MEDIA" to "#F39C12"
            "BAJA" -> "BAJA" to "#27AE60"
            else -> "MEDIA" to "#F39C12"
        }

        private fun coloresEstado(estado: String): Pair<String, String> = when (estado) {
            "PENDIENTE" -> "PENDIENTE" to "#E67E22"
            "EN_REVISION" -> "EN REVISIÓN" to "#3498DB"
            "RESUELTA" -> "RESUELTA" to "#27AE60"
            else -> estado to "#666666"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIncidenciaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<EntidadIncidencia>() {
            override fun areItemsTheSame(a: EntidadIncidencia, b: EntidadIncidencia) =
                a.id == b.id
            override fun areContentsTheSame(a: EntidadIncidencia, b: EntidadIncidencia) =
                a == b
        }
    }
}