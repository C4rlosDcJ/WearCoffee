package com.example.wearcoffee

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("wear_coffee_prefs", Context.MODE_PRIVATE)
        val savedToken = prefs.getString("token", null)
        
        // Auto-login si ya existe token
        if (savedToken != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        // Generar UUID del dispositivo si no existe
        var deviceUuid = prefs.getString("device_uuid", null)
        if (deviceUuid == null) {
            deviceUuid = UUID.randomUUID().toString()
            prefs.edit().putString("device_uuid", deviceUuid).apply()
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(email, password, deviceUuid)
        }
    }

    private fun performLogin(email: String, password: String, uuid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val model = Build.MODEL ?: "WearOS Device"
                val response = ApiClient.service.login(
                    LoginRequest(email, password, uuid, model)
                )

                withContext(Dispatchers.Main) {
                    if (response.success && response.token != null) {
                        val prefs = getSharedPreferences("wear_coffee_prefs", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("token", "Bearer ${response.token}")
                            putString("user_name", response.user?.nombre ?: "Repartidor")
                            putString("user_email", response.user?.email ?: "")
                            putString("user_role", response.user?.rol ?: "")
                            apply()
                        }

                        Toast.makeText(this@LoginActivity, "Ingreso exitoso", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, response.message ?: "Credenciales invalidas", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Error de conexion: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
