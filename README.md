# WearCoffee - Aplicación Wearable (WearOS)

WearCoffee es una aplicación nativa para relojes inteligentes (Wear OS) diseñada como herramienta telemétrica y de navegación manos libres para el equipo de reparto de **Sakura Coffee**.

## Características
* **Autenticación Directa**: Inicio de sesión integrado mediante JSON Web Tokens (JWT).
* **Gestión de Entregas (Flujo de Pedido)**: 
  * Recepción de propuestas de pedidos en tiempo real con detalles del cliente (nombre, dirección, total y artículos).
  * Aceptación de entrega directa.
  * Cambios de estado en un solo toque: "Empezar Ruta" y "Completar Entrega".
* **Telemetría de Sensores**:
  * Envío automático de coordenadas GPS en tiempo real mediante `LocationManager`.
  * Reporte periódico de nivel de batería física del dispositivo.
  * Reporte de pulsaciones cardíacas.
* **Diseño Sakura Premium**: Interfaz limpia, optimizada para pantallas circulares con diseño de tarjetas claras y tipografías legibles.

## Tecnologías Utilizadas
* **Lenguaje**: Kotlin
* **SDK Mínimo**: Android API 30 (Wear OS 3.0+)
* **Consumo de API**: Retrofit 2 + OkHttp 4
* **Asincronía**: Kotlin Coroutines (hilos secundarios `Dispatchers.IO` para envío periódico de telemetría cada 5 segundos)
* **Localización**: Android Location API (`LocationManager`)

## Configuración y Ejecución
1. Abre el proyecto en **Android Studio**.
2. Dirígete a la clase `com.example.wearcoffee.ApiClient` y configura la variable `BASE_URL` apuntando a la dirección IP de tu servidor Flask (por defecto para el emulador local: `http://10.0.2.2:5001/`).
3. Compila y ejecuta la aplicación en un Emulador de Wear OS o dispositivo físico.
