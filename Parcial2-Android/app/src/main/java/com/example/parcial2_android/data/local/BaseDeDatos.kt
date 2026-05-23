package com.example.parcial2_android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.parcial2_android.data.local.dao.*
import com.example.parcial2_android.data.local.entidad.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        EntidadUsuario::class,
        EntidadCategoria::class,
        EntidadIncidencia::class,
        EntidadHistorialCambio::class
    ],
    version = 1,
    exportSchema = true
)
abstract class BaseDeDatos : RoomDatabase() {

    abstract fun daoUsuario(): DaoUsuario
    abstract fun daoCategoria(): DaoCategoria
    abstract fun daoIncidencia(): DaoIncidencia
    abstract fun daoHistorialCambio(): DaoHistorialCambio

    companion object {

        @Volatile
        private var INSTANCIA: BaseDeDatos? = null

        fun obtenerInstancia(context: Context): BaseDeDatos {
            return INSTANCIA ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    BaseDeDatos::class.java,
                    "incidencias_utp.db"
                )
                    .addCallback(CallbackPrepoblar())
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCIA = instancia
                instancia
            }
        }
    }

    /**
     * Inserta las 6 categorias base del enunciado la primera vez
     * que se crea la base de datos.
     */
    private class CallbackPrepoblar : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCIA?.let { baseDatos ->
                CoroutineScope(Dispatchers.IO).launch {
                    baseDatos.daoCategoria().insertarTodas(categoriasPredeterminadas())
                }
            }
        }

        private fun categoriasPredeterminadas() = listOf(
            EntidadCategoria(
                id = 1,
                nombre = "Infraestructura danada",
                descripcion = "Danos en paredes, pisos, techos, mobiliario",
                icono = "construction",
                color = "#795548"
            ),
            EntidadCategoria(
                id = 2,
                nombre = "Problemas electricos",
                descripcion = "Cortos, tomacorrientes, iluminacion",
                icono = "electrical_services",
                color = "#F9A825"
            ),
            EntidadCategoria(
                id = 3,
                nombre = "Equipos de laboratorio",
                descripcion = "Equipos defectuosos o fuera de servicio",
                icono = "science",
                color = "#1565C0"
            ),
            EntidadCategoria(
                id = 4,
                nombre = "Fallas de conectividad",
                descripcion = "Wi-Fi, red institucional, puntos de acceso",
                icono = "wifi_off",
                color = "#6A1B9A"
            ),
            EntidadCategoria(
                id = 5,
                nombre = "Problemas de seguridad",
                descripcion = "Puertas, cerraduras, vigilancia, accesos",
                icono = "security",
                color = "#B71C1C"
            ),
            EntidadCategoria(
                id = 6,
                nombre = "Emergencias academicas",
                descripcion = "Situaciones que afectan actividades academicas",
                icono = "school",
                color = "#2E7D32"
            )
        )
    }
}