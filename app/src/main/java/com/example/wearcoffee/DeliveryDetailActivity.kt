package com.example.wearcoffee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeliveryDetailActivity : AppCompatActivity() {

    private lateinit var tvEmptyState: TextView
    private lateinit var containerActiveOrder: LinearLayout
    private lateinit var tvClientName: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvItems: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnStartRoute: Button
    private lateinit var btnCompleteDelivery: Button
    private lateinit var btnBack: Button
    private lateinit var btnRefresh: Button

    // Nuevos elementos para aceptar/rechazar propuestas
    private lateinit var containerProposalActions: LinearLayout
    private lateinit var containerDeliveryActions: LinearLayout
    private lateinit var btnAcceptOffer: Button
    private lateinit var btnDeclineOffer: Button

    private var activeOrderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_detail)

        val prefs = getSharedPreferences("wear_coffee_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)

        if (token == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        tvEmptyState = findViewById(R.id.tvEmptyState)
        containerActiveOrder = findViewById(R.id.containerActiveOrder)
        tvClientName = findViewById(R.id.tvClientName)
        tvAddress = findViewById(R.id.tvAddress)
        tvItems = findViewById(R.id.tvItems)
        tvTotal = findViewById(R.id.tvTotal)
        btnStartRoute = findViewById(R.id.btnStartRoute)
        btnCompleteDelivery = findViewById(R.id.btnCompleteDelivery)
        btnBack = findViewById(R.id.btnBack)
        btnRefresh = findViewById(R.id.btnRefresh)

        containerProposalActions = findViewById(R.id.containerProposalActions)
        containerDeliveryActions = findViewById(R.id.containerDeliveryActions)
        btnAcceptOffer = findViewById(R.id.btnAcceptOffer)
        btnDeclineOffer = findViewById(R.id.btnDeclineOffer)

        btnBack.setOnClickListener {
            finish()
        }

        btnRefresh.setOnClickListener {
            Toast.makeText(this, "Actualizando...", Toast.LENGTH_SHORT).show()
            fetchAssignedOrders(token)
        }

        btnAcceptOffer.setOnClickListener {
            activeOrderId?.let { id ->
                updateOrderStatus(token, id, "asignado")
            }
        }

        btnDeclineOffer.setOnClickListener {
            activeOrderId?.let { id ->
                updateOrderStatus(token, id, "pendiente")
            }
        }

        btnStartRoute.setOnClickListener {
            activeOrderId?.let { id ->
                updateOrderStatus(token, id, "en_camino")
            }
        }

        btnCompleteDelivery.setOnClickListener {
            activeOrderId?.let { id ->
                updateOrderStatus(token, id, "entregado")
            }
        }

        fetchAssignedOrders(token)
    }

    private fun fetchAssignedOrders(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.service.getPedidos(token)
                withContext(Dispatchers.Main) {
                    if (response.success && !response.pedidos.isNullOrEmpty()) {
                        val activeOrder = response.pedidos.first()
                        activeOrderId = activeOrder.id
                        
                        tvClientName.text = "Cliente: ${activeOrder.cliente}"
                        tvAddress.text = "Direccion: ${activeOrder.direccion}"
                        
                        val itemsText = activeOrder.items.joinToString(", ") { "${it.cantidad}x ${it.nombre}" }
                        tvItems.text = "Items: $itemsText"
                        
                        tvTotal.text = String.format("Total: $%.2f", activeOrder.total)

                        // Si el estado es propuesto, mostrar acciones de propuesta (Aceptar/Rechazar)
                        if (activeOrder.estado_reparto == "propuesto") {
                            containerProposalActions.visibility = View.VISIBLE
                            containerDeliveryActions.visibility = View.GONE
                        } else {
                            containerProposalActions.visibility = View.GONE
                            containerDeliveryActions.visibility = View.VISIBLE

                            // Ajustar botones segun estado actual del reparto
                            if (activeOrder.estado_reparto == "en_camino") {
                                btnStartRoute.isEnabled = false
                                btnCompleteDelivery.isEnabled = true
                            } else {
                                btnStartRoute.isEnabled = true
                                btnCompleteDelivery.isEnabled = false
                            }
                        }

                        tvEmptyState.visibility = View.GONE
                        containerActiveOrder.visibility = View.VISIBLE
                    } else {
                        showEmptyState()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DeliveryDetailActivity, "Error al cargar pedidos: ${e.message}", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
            }
        }
    }

    private fun showEmptyState() {
        tvEmptyState.visibility = View.VISIBLE
        containerActiveOrder.visibility = View.GONE
        activeOrderId = null
    }

    private fun updateOrderStatus(token: String, id: String, state: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.service.updatePedido(token, id, UpdateOrderRequest(state))
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        Toast.makeText(this@DeliveryDetailActivity, "Estado actualizado: $state", Toast.LENGTH_SHORT).show()
                        if (state == "entregado" || state == "pendiente") {
                            showEmptyState()
                        } else {
                            fetchAssignedOrders(token)
                        }
                    } else {
                        Toast.makeText(this@DeliveryDetailActivity, response.message ?: "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DeliveryDetailActivity, "Error de conexion: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
