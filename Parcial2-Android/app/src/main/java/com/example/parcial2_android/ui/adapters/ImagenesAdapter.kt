package com.example.parcial2_android.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.parcial2_android.R
import com.example.parcial2_android.databinding.ItemImagenBinding
import java.io.File

class ImagenesAdapter(
    private val imagenes: List<String>,
    private val onVer: (String) -> Unit,
    private val onEliminar: (String, Int) -> Unit
) : RecyclerView.Adapter<ImagenesAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemImagenBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ruta: String, position: Int) {
            val archivo = File(ruta)
            binding.tvNombreImagen.text = archivo.name

            binding.btnVerImagen.setOnClickListener {
                onVer(ruta)
            }

            binding.btnEliminarImagen.setOnClickListener {
                onEliminar(ruta, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImagenBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(imagenes[position], position)
    }

    override fun getItemCount(): Int = imagenes.size
}