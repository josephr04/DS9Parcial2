package com.universidad.incidencias.data.local.entidad

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────
// USUARIOS
// ─────────────────────────────────────────────
@Entity(tableName = "usuarios")
data class EntidadUsuario(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "nombre_usuario")
    val nombreUsuario: String,

    @ColumnInfo(name = "correo")
    val correo: String,

    /** Hash SHA-256 de la contraseña. NUNCA texto plano. */
    @ColumnInfo(name = "contrasena_hash")
    val contrasenaHash: String,

    @ColumnInfo(name = "nombre_completo")
    val nombreCompleto: String,

    /** "ESTUDIANTE" | "DOCENTE" | "ADMIN" */
    @ColumnInfo(name = "rol")
    val rol: String = "ESTUDIANTE",

    @ColumnInfo(name = "esta_activo")
    val estaActivo: Boolean = true,

    @ColumnInfo(name = "creado_en")
    val creadoEn: String,   // ISO-8601: "2025-01-15T10:30:00"

    /** Token de sesión persistente (se mantiene aunque la app se cierre). */
    @ColumnInfo(name = "token_sesion")
    val tokenSesion: String? = null,

    @ColumnInfo(name = "token_expira_en")
    val tokenExpiraEn: String? = null
)

// ─────────────────────────────────────────────
// CATEGORIAS
// ─────────────────────────────────────────────
@Entity(tableName = "categorias")
data class EntidadCategoria(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "descripcion")
    val descripcion: String = "",

    /** Nombre del ícono de Material (ej: "electrical_services"). */
    @ColumnInfo(name = "icono")
    val icono: String = "",

    /** Color hex para la UI (ej: "#E53935"). */
    @ColumnInfo(name = "color")
    val color: String = "#607D8B"
)

// ─────────────────────────────────────────────
// INCIDENCIAS
// ─────────────────────────────────────────────
@Entity(
    tableName = "incidencias",
    foreignKeys = [
        ForeignKey(
            entity = EntidadUsuario::class,
            parentColumns = ["id"],
            childColumns = ["usuario_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EntidadCategoria::class,
            parentColumns = ["id"],
            childColumns = ["categoria_id"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [
        Index("usuario_id"),
        Index("categoria_id"),
        Index("estado"),
        Index("prioridad")
    ]
)
data class EntidadIncidencia(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "usuario_id")
    val usuarioId: Int,

    @ColumnInfo(name = "categoria_id", defaultValue = "1")
    val categoriaId: Int,

    @ColumnInfo(name = "titulo")
    val titulo: String,

    @ColumnInfo(name = "descripcion")
    val descripcion: String,

    /** "BAJA" | "MEDIA" | "ALTA" | "CRITICA" */
    @ColumnInfo(name = "prioridad")
    val prioridad: String = "MEDIA",

    /** "PENDIENTE" | "EN_PROCESO" | "RESUELTA" | "RECHAZADA" */
    @ColumnInfo(name = "estado")
    val estado: String = "PENDIENTE",

    /** Ruta absoluta local a la imagen comprimida (JPG/PNG, máx 5 MB). */
    @ColumnInfo(name = "ruta_foto")
    val rutaFoto: String? = null,

    @ColumnInfo(name = "latitud")
    val latitud: Double? = null,

    @ColumnInfo(name = "longitud")
    val longitud: Double? = null,

    /** Se asigna automáticamente al crear la incidencia. */
    @ColumnInfo(name = "creado_en")
    val creadoEn: String,

    @ColumnInfo(name = "actualizado_en")
    val actualizadoEn: String,

    /** false mientras no se haya sincronizado con el servidor remoto. */
    @ColumnInfo(name = "esta_sincronizado")
    val estaSincronizado: Boolean = false
)

// ─────────────────────────────────────────────
// HISTORIAL DE CAMBIOS
// ─────────────────────────────────────────────
@Entity(
    tableName = "historial_cambios",
    foreignKeys = [
        ForeignKey(
            entity = EntidadIncidencia::class,
            parentColumns = ["id"],
            childColumns = ["incidencia_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EntidadUsuario::class,
            parentColumns = ["id"],
            childColumns = ["cambiado_por"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("incidencia_id"),
        Index("cambiado_por")
    ]
)
data class EntidadHistorialCambio(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "incidencia_id")
    val incidenciaId: Int,

    @ColumnInfo(name = "cambiado_por")
    val cambiadoPor: Int,

    @ColumnInfo(name = "estado_anterior")
    val estadoAnterior: String? = null,

    @ColumnInfo(name = "estado_nuevo")
    val estadoNuevo: String? = null,

    @ColumnInfo(name = "prioridad_anterior")
    val prioridadAnterior: String? = null,

    @ColumnInfo(name = "prioridad_nueva")
    val prioridadNueva: String? = null,

    @ColumnInfo(name = "notas")
    val notas: String? = null,

    @ColumnInfo(name = "cambiado_en")
    val cambiadoEn: String   // ISO-8601
)