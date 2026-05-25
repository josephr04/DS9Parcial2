package com.example.parcial2_android.data.local.dao

import androidx.room.*
import com.example.parcial2_android.data.local.entidad.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────
// DAO USUARIO
// ─────────────────────────────────────────────
@Dao
interface DaoUsuario {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: EntidadUsuario): Long

    @Update
    suspend fun actualizar(usuario: EntidadUsuario)

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun buscarPorCorreo(correo: String): EntidadUsuario?

    @Query("SELECT * FROM usuarios WHERE nombre_usuario = :nombreUsuario LIMIT 1")
    suspend fun buscarPorNombreUsuario(nombreUsuario: String): EntidadUsuario?

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): EntidadUsuario?

    /** Actualiza el token de sesión tras un login exitoso. */
    @Query("""
        UPDATE usuarios
        SET token_sesion = :token, token_expira_en = :expiraEn
        WHERE id = :usuarioId
    """)
    suspend fun actualizarTokenSesion(usuarioId: Int, token: String, expiraEn: String)

    /** Invalida la sesión al hacer logout. */
    @Query("""
        UPDATE usuarios
        SET token_sesion = NULL, token_expira_en = NULL
        WHERE id = :usuarioId
    """)
    suspend fun cerrarSesion(usuarioId: Int)

    /** Recupera el usuario cuya sesión sigue activa (para persistir el login). */
    @Query("""
        SELECT * FROM usuarios
        WHERE token_sesion IS NOT NULL
          AND token_expira_en > :ahora
        LIMIT 1
    """)
    suspend fun buscarSesionActiva(ahora: String): EntidadUsuario?

    @Query("UPDATE usuarios SET contrasena_hash = :nuevoHash WHERE id = :usuarioId")
    suspend fun actualizarContrasena(usuarioId: Int, nuevoHash: String)
}

// ─────────────────────────────────────────────
// DAO CATEGORIA
// ─────────────────────────────────────────────
@Dao
interface DaoCategoria {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarTodas(categorias: List<EntidadCategoria>)

    @Query("SELECT * FROM categorias ORDER BY nombre ASC")
    fun obtenerTodas(): Flow<List<EntidadCategoria>>

    @Query("SELECT * FROM categorias WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): EntidadCategoria?
}

// ─────────────────────────────────────────────
// DAO INCIDENCIA
// ─────────────────────────────────────────────
@Dao
interface DaoIncidencia {
    @Query("SELECT EXISTS(SELECT 1 FROM categorias WHERE id = :categoriaId)")
    suspend fun verificarCategoriaExiste(categoriaId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(incidencia: EntidadIncidencia): Long

    @Update
    suspend fun actualizar(incidencia: EntidadIncidencia)

    @Delete
    suspend fun eliminar(incidencia: EntidadIncidencia)

    @Query("SELECT * FROM incidencias WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): EntidadIncidencia?

    /** Todas las incidencias del usuario activo, ordenadas por fecha desc. */
    @Query("""
        SELECT * FROM incidencias
        WHERE usuario_id = :usuarioId
        ORDER BY creado_en DESC
    """)
    fun obtenerPorUsuario(usuarioId: Int): Flow<List<EntidadIncidencia>>

    /** Filtro por estado y/o prioridad (null = sin filtro). */
    @Query("""
        SELECT * FROM incidencias
        WHERE (:estado IS NULL OR estado = :estado)
          AND (:prioridad IS NULL OR prioridad = :prioridad)
        ORDER BY creado_en DESC
    """)
    fun obtenerFiltradas(estado: String?, prioridad: String?): Flow<List<EntidadIncidencia>>

    /** Para sincronizacion en background. */
    @Query("SELECT * FROM incidencias WHERE esta_sincronizado = 0")
    suspend fun obtenerPendientesSincronizacion(): List<EntidadIncidencia>

    @Query("UPDATE incidencias SET esta_sincronizado = 1 WHERE id = :id")
    suspend fun marcarComoSincronizada(id: Int)

    // Estadisticas del dashboard
    @Query("SELECT COUNT(*) FROM incidencias")
    fun contarTotal(): Flow<Int>

    @Query("SELECT COUNT(*) FROM incidencias WHERE estado = 'RESUELTA'")
    fun contarResueltas(): Flow<Int>

    @Query("SELECT COUNT(*) FROM incidencias WHERE estado = 'PENDIENTE'")
    fun contarPendientes(): Flow<Int>

    /** Categoria con mas reportes para el dashboard. */
    @Query("""
        SELECT categoria_id, COUNT(*) AS total
        FROM incidencias
        GROUP BY categoria_id
        ORDER BY total DESC
        LIMIT 1
    """)
    fun obtenerCategoriaMasReportada(): Flow<ConteoCategoria?>

    @Query("""
        UPDATE incidencias
        SET estado = :nuevoEstado, actualizado_en = :actualizadoEn
        WHERE id = :id
    """)
    suspend fun actualizarEstado(id: Int, nuevoEstado: String, actualizadoEn: String)
}

/** Resultado parcial para el dashboard. */
data class ConteoCategoria(
    @ColumnInfo(name = "categoria_id") val categoriaId: Int,
    @ColumnInfo(name = "total") val total: Int
)

// ─────────────────────────────────────────────
// DAO HISTORIAL DE CAMBIOS
// ─────────────────────────────────────────────
@Dao
interface DaoHistorialCambio {

    @Insert
    suspend fun insertar(entrada: EntidadHistorialCambio): Long

    @Query("""
        SELECT * FROM historial_cambios
        WHERE incidencia_id = :incidenciaId
        ORDER BY cambiado_en DESC
    """)
    fun obtenerPorIncidencia(incidenciaId: Int): Flow<List<EntidadHistorialCambio>>

    @Query("SELECT * FROM historial_cambios ORDER BY cambiado_en DESC LIMIT :limite")
    fun obtenerRecientes(limite: Int = 50): Flow<List<EntidadHistorialCambio>>
}