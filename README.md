# Alcancía 1.1

> **App de ahorros local, privada y de código abierto para Android.**

Alcancía es una aplicación moderna, fluida y 100% privada diseñada para ayudarte a gestionar tus ingresos, gastos, metas de ahorro y listas de compras de forma segura y sin complicaciones.

---

## ✨ Características Principales

- **🔒 100% Privada y Local:** Tus finanzas se quedan en tu dispositivo. La base de datos es SQLite local (Room) y no requiere servidores externos ni cuentas para funcionar.
- **🚫 Cero Rastradores y Sin Anuncios:** Sin telemetría oculta, sin análisis en segundo plano, sin SDKs de publicidad.
- **🎨 Personalización Total:** 
  - Soporte completo de **Material You** (color dinámico de Android 12+).
  - Temas claros, oscuros y OLED / negro puro.
  - Paletas de acento personalizadas, estilos de tarjetas ajustables, selector de esquinas redondeadas y fuentes numéricas.
- **🛡️ Seguridad Integrada:**
  - Bloqueo biométrico por **huella digital** con fallback seguro.
  - Bloqueo por código **PIN numérico** configurable.
- **💾 Copias de Seguridad y Portabilidad de Datos:**
  - Exportación e importación de respaldos completos en formato JSON local.
  - Exportación de movimientos financieros a **CSV** para análisis en hojas de cálculo.
  - Herramienta segura de **limpieza de caché** de la aplicación.
- **⏰ Recordatorios en Segundo Plano:**
  - Programación estable con `WorkManager` para recordatorios diarios, de días hábiles o fines de semana que persisten tras reiniciar el dispositivo.
  - Verificación y solicitud amigable del permiso de notificaciones (`POST_NOTIFICATIONS` en Android 13+).
- **✨ IA Local Opcional (Ollama):**
  - Función completamente opcional (desactivada por defecto).
  - Conexión a tu propia instancia local de **Ollama** (ej. en tu PC vía `http://10.0.2.2:11434` o en tu red local).
  - Sugerencia inteligente de categorías para transacciones basada únicamente en la descripción.
  - **100% Privada:** Ningún dato se envía a servicios en la nube de terceros.
  - La app funciona 100% idéntica si esta función está apagada o sin conexión.

---

## 🛠️ Tecnologías y Arquitectura

- **Lenguaje:** Kotlin
- **Interfaz de Usuario:** Jetpack Compose + Material Design 3 (M3)
- **Arquitectura:** MVVM (Model-View-ViewModel) con Kotlin Coroutines y StateFlow
- **Persistencia:** Room Database (SQLite) con migraciones automáticas
- **Tareas en Segundo Plano:** Android Jetpack WorkManager
- **Red (IA Local):** OkHttp 4 con cliente ligero y timeouts seguros
- **Biometría:** AndroidX Biometric API

---

## 🚀 Instrucciones de Compilación (Build Instructions)

### Requisitos Previos

- **JDK 17** o superior.
- **Android SDK** con `compileSdk 35` y herramientas de compilación compatibles.
- Android Studio Ladybug / Koala o Gradle CLI.

### Compilar desde la Terminal

Para generar el APK de depuración:
```bash
gradle assembleDebug
```
El archivo generado se ubicará en:
`app/build/outputs/apk/debug/app-debug.apk`

Para compilar el APK de lanzamiento (Release):
```bash
gradle assembleRelease
```

### Ejecutar Pruebas Locales

```bash
gradle :app:testDebugUnitTest
```

---

## 📦 F-Droid y Código Abierto

- **Licencia:** MIT (ver archivo [LICENSE](LICENSE)).
- **Cumplimiento F-Droid:**
  - No contiene binarios propietarios ni bibliotecas de seguimiento.
  - Todo el código fuente está disponible públicamente.
  - Compatible con el pipeline de compilación automatizada de F-Droid.

---

## 👤 Autor y Créditos

- **Desarrollador:** PixelX_O & Google AI Studio
- **Versión:** 1.1
- **Licencia:** [MIT](LICENSE)
