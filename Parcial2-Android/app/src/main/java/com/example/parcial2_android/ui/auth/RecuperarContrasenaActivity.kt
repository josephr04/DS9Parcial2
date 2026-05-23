package com.example.parcial2_android.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.parcial2_android.R
import com.example.parcial2_android.data.local.BaseDeDatos
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.*
import java.security.MessageDigest

class RecuperarContrasenaActivity : AppCompatActivity() {

    private var pasoActual = 1
    private var otpGenerado = ""
    private var correoDestino = ""

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerReenvio: CountDownTimer? = null

    // Vistas Paso 1
    private lateinit var layoutPaso1: View
    private lateinit var etCorreo: EditText
    private lateinit var btnEnviarInstrucciones: MaterialButton
    private lateinit var tvVolverLogin: View  // ← Cambiado de TextView a View

    // Vistas Paso 2
    private lateinit var layoutPaso2: View
    private lateinit var tvCorreoDestino: TextView
    private lateinit var cardDemoOtp: CardView
    private lateinit var tvCodigoDemo: TextView
    private lateinit var btnAutocompletar: MaterialButton
    private lateinit var otpInputs: Array<EditText>
    private lateinit var tvErrorOtp: TextView
    private lateinit var btnVerificarCodigo: MaterialButton
    private lateinit var btnReenviar: TextView
    private lateinit var tvCuentaRegresiva: TextView
    private lateinit var btnVolverPaso1: View  // ← Cambiado de TextView a View

    // Vistas Paso 3
    private lateinit var layoutPaso3: View
    private lateinit var etNuevaContrasena: EditText
    private lateinit var etConfirmarContrasena: EditText
    private lateinit var progressFortaleza: ProgressBar
    private lateinit var tvFortalezaLabel: TextView
    private lateinit var tvErrorContrasena: TextView
    private lateinit var btnCambiarContrasena: MaterialButton
    private lateinit var btnVolverPaso2: View  // ← Cambiado de TextView a View

    // Vistas Paso 4
    private lateinit var layoutPaso4: View
    private lateinit var btnIrLogin: MaterialButton

    private lateinit var pasosDots: Array<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_contrasena)

        // Configurar callback de retroceso
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (pasoActual) {
                    1 -> finish()
                    2 -> mostrarPaso(1)
                    3 -> mostrarPaso(2)
                    4 -> {
                        val intent = Intent(this@RecuperarContrasenaActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
        })

        enlazarVistas()
        configurarPaso1()
        configurarPaso2()
        configurarPaso3()
        configurarPaso4()
        mostrarPaso(1)
    }

    private fun enlazarVistas() {
        // Pasos contenedores
        layoutPaso1 = findViewById(R.id.layoutPaso1)
        layoutPaso2 = findViewById(R.id.layoutPaso2)
        layoutPaso3 = findViewById(R.id.layoutPaso3)
        layoutPaso4 = findViewById(R.id.layoutPaso4)

        // Indicadores
        pasosDots = arrayOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4)
        )

        // Paso 1
        etCorreo = findViewById<EditText>(R.id.etCorreoRecuperar)
        btnEnviarInstrucciones = findViewById(R.id.btnEnviarInstrucciones)
        tvVolverLogin = findViewById(R.id.tvVolverLogin)  // Ahora es View, no TextView

        // Paso 2
        tvCorreoDestino = findViewById(R.id.tvCorreoDestino)
        cardDemoOtp = findViewById(R.id.cardDemoOtp)
        tvCodigoDemo = findViewById(R.id.tvCodigoDemo)
        btnAutocompletar = findViewById(R.id.btnAutocompletar)
        otpInputs = arrayOf(
            findViewById<EditText>(R.id.otp1),
            findViewById<EditText>(R.id.otp2),
            findViewById<EditText>(R.id.otp3),
            findViewById<EditText>(R.id.otp4),
            findViewById<EditText>(R.id.otp5),
            findViewById<EditText>(R.id.otp6)
        )
        tvErrorOtp = findViewById(R.id.tvErrorOtp)
        btnVerificarCodigo = findViewById(R.id.btnVerificarCodigo)
        btnReenviar = findViewById(R.id.btnReenviar)
        tvCuentaRegresiva = findViewById(R.id.tvCuentaRegresiva)
        btnVolverPaso1 = findViewById(R.id.btnVolverPaso1)  // Ahora es View, no TextView

        // Paso 3
        etNuevaContrasena = findViewById<EditText>(R.id.etNuevaContrasena)
        etConfirmarContrasena = findViewById<EditText>(R.id.etConfirmarContrasena)
        progressFortaleza = findViewById(R.id.progressFortaleza)
        tvFortalezaLabel = findViewById(R.id.tvFortalezaLabel)
        tvErrorContrasena = findViewById(R.id.tvErrorContrasena)
        btnCambiarContrasena = findViewById(R.id.btnCambiarContrasena)
        btnVolverPaso2 = findViewById(R.id.btnVolverPaso2)  // Ahora es View, no TextView

        // Paso 4
        btnIrLogin = findViewById(R.id.btnIrLogin)
    }

    private fun configurarPaso1() {
        btnEnviarInstrucciones.setOnClickListener {
            val correo = etCorreo.text.toString().trim()

            if (!validarCorreo(correo)) {
                etCorreo.error = "Ingresa un correo institucional válido (@utp.ac.pa)"
                return@setOnClickListener
            }

            btnEnviarInstrucciones.isEnabled = false
            btnEnviarInstrucciones.text = "Enviando..."

            scope.launch {
                delay(1200)
                correoDestino = correo
                otpGenerado = generarOtp()
                btnEnviarInstrucciones.isEnabled = true
                btnEnviarInstrucciones.text = "Enviar Instrucciones"
                mostrarPaso(2)
            }
        }

        tvVolverLogin.setOnClickListener { finish() }
    }

    private fun configurarPaso2() {
        btnAutocompletar.setOnClickListener {
            otpGenerado.forEachIndexed { i, c ->
                otpInputs[i].setText(c.toString())
            }
            tvErrorOtp.visibility = View.GONE
            resetColorOtp()
        }

        configurarNavegacionOtp()

        btnVerificarCodigo.setOnClickListener {
            val codigoIngresado = otpInputs.joinToString("") { it.text.toString() }

            if (codigoIngresado.length < 6) {
                tvErrorOtp.text = "Ingresa los 6 dígitos del código."
                tvErrorOtp.visibility = View.VISIBLE
                marcarOtpError()
                return@setOnClickListener
            }

            if (codigoIngresado != otpGenerado) {
                tvErrorOtp.text = "Código incorrecto. Usa el código del recuadro de arriba."
                tvErrorOtp.visibility = View.VISIBLE
                marcarOtpError()
                return@setOnClickListener
            }

            tvErrorOtp.visibility = View.GONE
            resetColorOtp()
            mostrarPaso(3)
        }

        btnReenviar.setOnClickListener {
            otpGenerado = generarOtp()
            tvCodigoDemo.text = otpGenerado
            limpiarOtp()
            tvErrorOtp.visibility = View.GONE
            iniciarCuentaRegresiva()
        }

        btnVolverPaso1.setOnClickListener { mostrarPaso(1) }
    }

    private fun configurarNavegacionOtp() {
        otpInputs.forEachIndexed { i, campo ->
            campo.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < otpInputs.lastIndex) {
                        otpInputs[i + 1].requestFocus()
                    }
                }
            })

            campo.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN &&
                    keyCode == KeyEvent.KEYCODE_DEL &&
                    campo.text.isNullOrEmpty() &&
                    i > 0
                ) {
                    otpInputs[i - 1].apply {
                        requestFocus()
                        setText("")
                    }
                    true
                } else false
            }
        }
    }

    private fun configurarPaso3() {
        etNuevaContrasena.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                actualizarBarraFortaleza(s.toString())
            }
        })

        btnCambiarContrasena.setOnClickListener {
            val nueva = etNuevaContrasena.text.toString()
            val confirmar = etConfirmarContrasena.text.toString()

            when {
                nueva.length < 8 -> {
                    tvErrorContrasena.text = "La contraseña debe tener al menos 8 caracteres."
                    tvErrorContrasena.visibility = View.VISIBLE
                }
                nueva != confirmar -> {
                    tvErrorContrasena.text = "Las contraseñas no coinciden."
                    tvErrorContrasena.visibility = View.VISIBLE
                }
                else -> {
                    tvErrorContrasena.visibility = View.GONE
                    btnCambiarContrasena.isEnabled = false
                    btnCambiarContrasena.text = "Guardando..."

                    scope.launch {
                        actualizarContrasenaEnDb(correoDestino, nueva)
                        delay(800)
                        btnCambiarContrasena.isEnabled = true
                        btnCambiarContrasena.text = "Cambiar Contraseña"
                        mostrarPaso(4)
                    }
                }
            }
        }

        btnVolverPaso2.setOnClickListener { mostrarPaso(2) }
    }

    private fun configurarPaso4() {
        btnIrLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun mostrarPaso(paso: Int) {
        pasoActual = paso

        layoutPaso1.visibility = if (paso == 1) View.VISIBLE else View.GONE
        layoutPaso2.visibility = if (paso == 2) View.VISIBLE else View.GONE
        layoutPaso3.visibility = if (paso == 3) View.VISIBLE else View.GONE
        layoutPaso4.visibility = if (paso == 4) View.VISIBLE else View.GONE

        pasosDots.forEachIndexed { i, dot ->
            dot.setBackgroundResource(
                if (i < paso) R.drawable.bg_dot_activo else R.drawable.bg_dot_inactivo
            )
        }

        if (paso == 2) {
            tvCorreoDestino.text = correoDestino
            tvCodigoDemo.text = otpGenerado
            limpiarOtp()
            iniciarCuentaRegresiva()
        }
    }

    private fun generarOtp(): String = (100000..999999).random().toString()

    private fun limpiarOtp() {
        otpInputs.forEach { it.setText("") }
        otpInputs.first().requestFocus()
        resetColorOtp()
    }

    private fun marcarOtpError() {
        otpInputs.forEach { campo ->
            campo.setBackgroundResource(R.drawable.bg_otp_error)
        }
    }

    private fun resetColorOtp() {
        otpInputs.forEach { campo ->
            campo.setBackgroundResource(R.drawable.bg_otp_field)
        }
    }

    private fun iniciarCuentaRegresiva() {
        timerReenvio?.cancel()
        btnReenviar.isEnabled = false
        btnReenviar.alpha = 0.4f

        timerReenvio = object : CountDownTimer(30_000, 1_000) {
            override fun onTick(restante: Long) {
                tvCuentaRegresiva.text = "Reenviar en ${restante / 1000}s"
                tvCuentaRegresiva.visibility = View.VISIBLE
            }
            override fun onFinish() {
                tvCuentaRegresiva.visibility = View.GONE
                btnReenviar.isEnabled = true
                btnReenviar.alpha = 1f
            }
        }.start()
    }

    private fun actualizarBarraFortaleza(contrasena: String) {
        var puntaje = 0
        if (contrasena.length >= 8) puntaje++
        if (contrasena.any { it.isUpperCase() }) puntaje++
        if (contrasena.any { it.isDigit() }) puntaje++
        if (contrasena.any { !it.isLetterOrDigit() }) puntaje++

        progressFortaleza.progress = puntaje * 25

        val (label, colorRes) = when (puntaje) {
            0 -> Pair("", android.R.color.transparent)
            1 -> Pair("Muy débil", R.color.fortaleza_debil)
            2 -> Pair("Débil", R.color.fortaleza_regular)
            3 -> Pair("Aceptable", R.color.fortaleza_aceptable)
            else -> Pair("Segura", R.color.fortaleza_segura)
        }

        tvFortalezaLabel.text = label
        progressFortaleza.progressTintList = ContextCompat.getColorStateList(this, colorRes)
    }

    private suspend fun actualizarContrasenaEnDb(correo: String, nuevaContrasena: String) {
        withContext(Dispatchers.IO) {
            val db = BaseDeDatos.obtenerInstancia(applicationContext)
            val usuario = db.daoUsuario().buscarPorCorreo(correo) ?: return@withContext
            val nuevoHash = sha256(nuevaContrasena)
            db.daoUsuario().actualizarContrasena(usuario.id, nuevoHash)
        }
    }

    private fun validarCorreo(correo: String): Boolean {
        return correo.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerReenvio?.cancel()
        scope.cancel()
    }
}