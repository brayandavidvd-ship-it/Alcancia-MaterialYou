# Alcancía v1.3.0

> **Aplicación de control financiero personal, metas de ahorro y listas de compras para Android. Software libre, 100% sin anuncios ni rastreadores.**

Alcancía es una aplicación moderna y minimalista para Android construida con Jetpack Compose y Material 3. Permite llevar el control de tus ingresos, gastos, metas de ahorro y listas de compras directamente en tu dispositivo, con soporte opcional para un asistente en la nube mediante tu propia clave de Gemini.

---

## Características Principales

- **Control financiero 100% offline:** Tus movimientos de ingresos, gastos y ahorros se almacenan localmente en SQLite mediante Android Room, sin cuentas obligatorias ni servidores propios.
- **Software libre sin anuncios ni rastreadores:** Cero SDKs de publicidad, cero analítica oculta y sin telemetría en segundo plano.
- **Metas de ahorro con anillo de progreso:** Crea metas, fija tu meta prioritaria en la pantalla de Inicio, realiza aportes o retiros con teclado numérico y revisa su historial detallado.
- **Listas de compras inteligentes:** Administra múltiples listas, define presupuestos por lista, marca productos comprados, reutiliza precios recientes y registra el total como gasto con un toque.
- **Asistente en la nube opcional (Gemini) con entrada por voz:** Activa el asistente cuando lo necesites con tu propia clave API mediante llamadas REST abiertas (OkHttp), con botón de dictado por voz en Inicio, Compras y Chat para preparar comandos o registros.
- **Personalización y accesibilidad:** Soporte para modo oscuro minimalista (`#05070B`), modo claro y sistema, modo daltónico, paletas de acento, estilos de tarjetas, densidad y tipografía numérica.
- **Seguridad integrada:** Bloqueo biométrico por huella digital y código PIN numérico configurable.
- **Recordatorios locales personalizables:** Programa avisos diarios, en días hábiles o fines de semana y personaliza el texto de la notificación con restauración rápida al mensaje predeterminado.
- **Respaldos locales en JSON:** Exporta e importa copias de seguridad completas de tus datos cuando lo desees.

---

## Tecnologías y Arquitectura

- **Lenguaje:** Kotlin
- **Interfaz de Usuario:** Jetpack Compose + Material Design 3
- **Arquitectura:** MVVM con StateFlow y Kotlin Coroutines
- **Persistencia:** Room Database (SQLite)
- **Red (Asistente opcional):** OkHttp + JSON REST abierto

---

## Instrucciones de Compilación

### Requisitos Previos

- **JDK 17** o superior
- **Android SDK** compatible con `compileSdk 36`

### Compilar desde la Terminal

Generar APK de depuración:
```bash
gradle assembleDebug
```

Generar APK de lanzamiento (`release`):
```bash
gradle assembleRelease
```

Ejecutar pruebas unitarias locales:
```bash
gradle :app:testDebugUnitTest
```

---

## Licencia y Créditos

- **Licencia:** Apache License 2.0 (ver [LICENSE](LICENSE)).
- **Créditos:** Creado por PixelX_O y Google Studio.
