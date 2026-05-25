package com.example.parcial2_android.ui.notificaciones

// ─── Adapter ──────────────────────────────────────────────────────────────────

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.parcial2_android.R

class NotificacionesAdapter(
    private val items: MutableList<NotificacionItem>,
    private val onItemClick: (NotificacionItem) -> Unit
) : RecyclerView.Adapter<NotificacionesAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val cardIcono: CardView    = view.findViewById(R.id.cardIcono)
        val ivIcono: ImageView     = view.findViewById(R.id.ivIcono)
        val tvTitulo: TextView     = view.findViewById(R.id.tvTitulo)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val tvTiempo: TextView     = view.findViewById(R.id.tvTiempo)
        val viewPunto: View        = view.findViewById(R.id.viewPuntoNoLeido)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvTitulo.text      = item.titulo
        holder.tvDescripcion.text = item.descripcion
        holder.tvTiempo.text      = item.tiempo

        // Punto azul de no leído
        holder.viewPunto.visibility = if (!item.leida) View.VISIBLE else View.GONE

        // Color e ícono según tipo
        when (item.tipo) {
            TipoNotificacion.CAMBIO_ESTADO -> {
                holder.cardIcono.setCardBackgroundColor(Color.parseColor("#E6F9EC"))
                holder.ivIcono.setImageResource(R.drawable.ic_nav_incidencias)
                holder.ivIcono.imageTintList =
                    ContextCompat.getColorStateList(holder.itemView.context, R.color.purple_primary)
                        ?: android.content.res.ColorStateList.valueOf(Color.parseColor("#6A0DAD"))
            }
            TipoNotificacion.ACTUALIZACION -> {
                holder.cardIcono.setCardBackgroundColor(Color.parseColor("#FFF8E1"))
                holder.ivIcono.setImageResource(R.drawable.ic_dashboard)
                holder.ivIcono.imageTintList =
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#BA7517"))
            }
            TipoNotificacion.ANUNCIO -> {
                holder.cardIcono.setCardBackgroundColor(Color.parseColor("#F0E6FF"))
                holder.ivIcono.setImageResource(R.drawable.ic_notificaciones)
                holder.ivIcono.imageTintList =
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#6A0DAD"))
            }
        }

        // Al tocar → marcar como leída
        holder.itemView.setOnClickListener {
            if (!item.leida) {
                item.leida = true
                notifyItemChanged(position)
                onItemClick(item)
            }
        }
    }

    /** Devuelve cuántas notificaciones no leídas hay */
    fun countUnread(): Int = items.count { !it.leida }

    /** Marca todas como leídas y refresca la lista */
    fun marcarTodasLeidas() {
        items.forEach { it.leida = true }
        notifyDataSetChanged()
    }
}


// ─── Modelo ───────────────────────────────────────────────────────────────────

enum class TipoNotificacion {
    CAMBIO_ESTADO,    // ícono verde  – cambio de estado en incidencia
    ACTUALIZACION,    // ícono amarillo – comentario/actualización
    ANUNCIO           // ícono morado  – mantenimiento u otro aviso del sistema
}

data class NotificacionItem(
    val id: Int,
    val tipo: TipoNotificacion,
    val titulo: String,
    val descripcion: String,
    val tiempo: String,
    var leida: Boolean = false
)