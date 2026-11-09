# Alcancía 🐷

App Android de control de gastos y ahorro con diseño Material You, creada con Google AI Studio.

**Versión actual: 1.2.0**

## ✨ Características

- **Inicio:** saldo disponible del mes, ingresos, gastos y gastos por categoría.
- **Movimientos:** vista de lista o cuadrícula, mantén presionado para editar o borrar, hora opcional y repetición (diaria o con días personalizados).
- **Metas de ahorro:** aportes y retiros; lo ahorrado resta del saldo del mes pero no cuenta como gasto.
- **Compras:** calculadora y lista de compras, con opción de registrarla como gasto.
- **Estadísticas** de tus movimientos.
- **Personalización:** temas y colores en modo claro y oscuro, con vista previa.
- **Seguridad:** bloqueo con huella y PIN de 4 dígitos.
- **Recordatorios** y respuesta háptica.
- **Datos:** exporta a CSV (Excel) y crea o restaura copias de seguridad de la app.

## 🤖 IA de ahorros (opcional)

La app funciona completa sin IA. Si la activas:

- **IA en la nube (Gemini):** usas tu propia clave de API de Google. Incluye un asistente en chat que puede crear, editar y eliminar movimientos, categorías, metas y compras. Cada acción pide tu confirmación. Modelos: Gemini 3.5 Flash-Lite y 3.5 Flash. Al activarla, la app te avisa de que tus datos se envían a Google para poder responder.
- **IA local (Ollama):** sugerencia de categoría con el botón ✨ usando un modelo que corre en tu propio dispositivo o red.

## 🔒 Privacidad

- Tus movimientos se guardan en tu dispositivo.
- La app no incluye cuentas ni servidor propio.
- Con la IA de la nube activada, los datos necesarios para responder se envían a Google. Con la IA local, la comunicación es con tu propio servidor Ollama.
- Los límites de uso de la IA en la nube dependen de tu cuenta de Google (consúltalos en AI Studio).

## 📲 Instalación

1. Ve a [Releases](../../releases) y descarga el APK de la versión que quieras.
2. Permite instalar apps de orígenes desconocidos cuando Android lo pida.
3. Abre el APK e instala.

## 🗂️ Versiones

- **v1.2.0** — IA de ahorros con asistente en la nube y IA local opcional.
- **v1.1** — Mejoras de la v1.0.
- **v1.0.0** — Versión inicial.

## 🙌 Créditos

Creado por **PixelX_O** con Google AI Studio.

## 📄 Licencia

[MIT](LICENSE)
