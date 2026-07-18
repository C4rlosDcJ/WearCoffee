package com.example.wearcoffee

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// DATA MODELS
data class LoginRequest(
    val email: String,
    val password: String,
    val device_uuid: String?,
    val device_model: String?
)

data class UserInfo(
    val id: String,
    val nombre: String,
    val email: String,
    val rol: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val message: String?,
    val user: UserInfo?
)

data class OrderItem(
    val nombre: String,
    val cantidad: Int
)

data class Order(
    val id: String,
    val cliente: String,
    val direccion: String,
    val telefono: String?,
    val total: Double,
    val estado_reparto: String,
    val fecha: String?,
    val items: List<OrderItem>
)

data class OrdersResponse(
    val success: Boolean,
    val pedidos: List<Order>?,
    val message: String?
)

data class UpdateOrderRequest(
    val estado_reparto: String
)

data class GenericResponse(
    val success: Boolean,
    val message: String?
)

data class SensorReportRequest(
    val device_uuid: String?,
    val bateria: Int,
    val ritmo_cardiaco: Int,
    val lat: Double,
    val lng: Double
)

data class NotificationItem(
    val id: String,
    val mensaje: String,
    val fecha: String?
)

data class NotificationsResponse(
    val success: Boolean,
    val notificaciones: List<NotificationItem>?,
    val message: String?
)

// RETROFIT API INTERFACE
interface ApiInterface {
    @POST("api/wearable/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/wearable/pedidos")
    suspend fun getPedidos(@Header("Authorization") token: String): OrdersResponse

    @PUT("api/wearable/pedidos/{id}")
    suspend fun updatePedido(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: UpdateOrderRequest
    ): GenericResponse

    @POST("api/wearable/reportar_sensores")
    suspend fun reportarSensores(
        @Header("Authorization") token: String,
        @Body request: SensorReportRequest
    ): GenericResponse

    @GET("api/wearable/notificaciones")
    suspend fun getNotificaciones(@Header("Authorization") token: String): NotificationsResponse
}

// RETROFIT BUILDER CLIENT
object ApiClient {
    // 10.0.2.2 es el alias de localhost del host dentro del Emulador de Android
    private const val BASE_URL = "http://10.0.2.2:5001/"

    private val client by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    val service: ApiInterface by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)
    }
}
