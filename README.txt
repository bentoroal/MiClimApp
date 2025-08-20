MiClimApp - Aplicación de Clima para Android
============================================

Descripción:
------------
MiClimApp es una aplicación Android que muestra el clima actual y el pronóstico diario para tu ubicación en Chile. Utiliza la API de Open-Meteo y permite configurar alertas personalizadas según temperatura mínima y velocidad máxima del viento. Ofrece ubicación automática por GPS o selección manual por comuna.

Características:
----------------
- Clima actual con íconos visuales y datos precisos
- Pronóstico diario para los próximos 7 días
- Alertas personalizadas por heladas y viento fuerte
- Ubicación automática o manual por comuna
- Notificaciones diarias programadas (mañana y noche)
- Persistencia de preferencias del usuario
- Interfaz amigable y adaptada al idioma español

Arquitectura:
-------------
- MVVM con separación clara de responsabilidades
- Retrofit para llamadas HTTP
- WorkManager para tareas en segundo plano
- ViewBinding para manejo seguro de vistas
- SharedPreferences para persistencia local
- Material Design para UI moderna

Estructura del Proyecto:
------------------------
- /api: Interfaces Retrofit y configuración base
- /model: Modelos de datos (Comuna, Forecast, Weather)
- /ui: Fragments y adaptadores de UI
- /utils: Funciones auxiliares (formato, batería, thresholds)
- /worker: Workers para notificaciones programadas
- /notifications: Helper para mostrar notificaciones
- /res/layout: Archivos XML de interfaz
- /res/menu: Menú inferior de navegación
- /res/navigation: Gráfico de navegación entre fragments

Configuración Inicial:
----------------------
1. Clonar el repositorio:
   git clone https://github.com/tuusuario/miclimapp.git

2. Abrir el proyecto en Android Studio

3. Configurar local.properties si se requiere clave de API

4. Ejecutar en dispositivo físico o emulador Android 8.0+

API Utilizada:
--------------
- Open-Meteo (https://open-meteo.com)

Notificaciones:
---------------
Se envían automáticamente a las 8:00 AM y 8:00 PM si hay condiciones climáticas que superan los umbrales definidos por el usuario. Se recomienda desactivar la optimización de batería para asegurar el funcionamiento en segundo plano.

Permisos Requeridos:
--------------------
- Acceso a ubicación (coarse y fine)
- Permiso para mostrar notificaciones (Android 13+)

Recomendaciones para Fabricantes:
---------------------------------
Para dispositivos Xiaomi, Huawei, Oppo, etc., permitir inicio automático y desactivar restricciones de batería.

Tests:
------
Ejecutar pruebas unitarias con:
   ./gradlew test

Licencia:
---------
Este proyecto está bajo la licencia MIT. Consulta el archivo LICENSE para más detalles.
