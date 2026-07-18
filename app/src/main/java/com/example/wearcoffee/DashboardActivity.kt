package com.example.wearcoffee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Vibrator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvRepartidorName: TextView
    private lateinit var btnPedidos: Button
    private lateinit var btnSensores: Button
    private lateinit var btnLogout: Button

    private var pollJob: Job? = null
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val prefs = getSharedPreferences("wear_coffee_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)
        val name = prefs.getString("user_name", "Repartidor")

        if (token == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        tvRepartidorName = findViewById(R.id.tvRepartidorName)
        btnPedidos = findViewById(R.id.btnPedidos)
        btnSensores = findViewById(R.id.btnSensores)
        btnLogout = findViewById(R.id.btnLogout)

        tvRepartidorName.text = name

        btnPedidos.setOnClickListener {
            startActivity(Intent(this, DeliveryDetailActivity::class.java))
        }

        btnSensores.setOnClickListener {
            startActivity(Intent(this, SensorsActivity::class.java))
        }

        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            Toast.makeText(this, "Sesion cerrada", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Iniciar polling de notificaciones
        startNotificationPolling(token)
    }

    private fun startNotificationPolling(token: String) {
        pollJob = activityScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val response = ApiClient.service.getNotificaciones(token)
                    if (response.success && !response.notificaciones.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            for (n in response.notificaciones) {
                                Toast.makeText(this@DashboardActivity, n.mensaje, Toast.LENGTH_LONG).show()
                                triggerVibration()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignorar errores de red temporales en segundo plano
                }
                delay(10000) // Poll cada 10 segundos
            }
        }
    }

    private fun triggerVibration() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(300)
            }
        } catch (e: Exception) {
            // Ignorar fallas de vibracion en emuladores que no la soportan
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollJob?.cancel()
        activityScope.cancel()
    }
}
