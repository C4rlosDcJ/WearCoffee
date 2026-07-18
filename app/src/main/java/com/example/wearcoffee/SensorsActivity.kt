package com.example.wearcoffee

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import kotlin.random.Random

class SensorsActivity : AppCompatActivity() {

    private lateinit var tvHeartRate: TextView
    private lateinit var tvBattery: TextView
    private lateinit var tvLocation: TextView
    private lateinit var btnBack: Button
    private lateinit var ivHeart: ImageView

    private var sensorJob: Job? = null
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var locationManager: LocationManager
    private val PERMISSION_REQUEST_CODE = 1001

    // Coordenadas iniciales cerca de la UTVT Toluca (como base por si no hay señal de satélite)
    private var currentLat = 19.2925
    private var currentLng = -99.6100
    private var usingRealGps = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLat = location.latitude
            currentLng = location.longitude
            usingRealGps = true
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensors)

        val prefs = getSharedPreferences("wear_coffee_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)
        val uuid = prefs.getString("device_uuid", "unknown-uuid")

        if (token == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        tvHeartRate = findViewById(R.id.tvHeartRate)
        tvBattery = findViewById(R.id.tvBattery)
        tvLocation = findViewById(R.id.tvLocation)
        btnBack = findViewById(R.id.btnBack)
        ivHeart = findViewById(R.id.ivHeart)

        btnBack.setOnClickListener {
            finish()
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        checkLocationPermissions()

        // Iniciar simulacion y envio de sensores
        startSensorTelemetry(token, uuid ?: "unknown-uuid")
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 1f, locationListener)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 1f, locationListener)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Permiso de ubicacion denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startSensorTelemetry(token: String, deviceUuid: String) {
        sensorJob = activityScope.launch(Dispatchers.IO) {
            var pulseScale = false
            while (isActive) {
                // 1. Simular Ritmo Cardiaco (68-92 lpm)
                val simulatedHeartRate = Random.nextInt(68, 92)

                // 2. Obtener Nivel de Bateria Real
                val batteryLevel = getBatteryPercent()

                // 3. Si no tenemos señal de GPS real todavía, simulamos un ligero movimiento en Toluca
                if (!usingRealGps) {
                    currentLat += (Random.nextDouble() - 0.5) * 0.0003
                    currentLng += (Random.nextDouble() - 0.5) * 0.0003
                }

                // 4. Actualizar UI
                withContext(Dispatchers.Main) {
                    tvHeartRate.text = "$simulatedHeartRate lpm"
                    tvBattery.text = "Bateria: $batteryLevel%"
                    tvLocation.text = String.format("GPS: %.4f, %.4f", currentLat, currentLng)

                    // Animacion de latido del corazon (escalar icono)
                    pulseScale = !pulseScale
                    ivHeart.animate().scaleX(if (pulseScale) 1.2f else 1.0f)
                        .scaleY(if (pulseScale) 1.2f else 1.0f).setDuration(200).start()
                }

                // 5. Enviar al Servidor
                try {
                    ApiClient.service.reportarSensores(
                        token,
                        SensorReportRequest(
                            device_uuid = deviceUuid,
                            bateria = batteryLevel,
                            ritmo_cardiaco = simulatedHeartRate,
                            lat = currentLat,
                            lng = currentLng
                        )
                    )
                } catch (e: Exception) {
                    // Evitar crasheo por falla de red temporal
                }

                delay(5000) // Actualizacion cada 5 segundos
            }
        }
    }

    private fun getBatteryPercent(): Int {
        return try {
            val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                ((level.toFloat() / scale.toFloat()) * 100).toInt()
            } else {
                95 // Fallback
            }
        } catch (e: Exception) {
            95 // Fallback
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        sensorJob?.cancel()
        activityScope.cancel()
    }
}
