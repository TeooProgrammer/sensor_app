# Sensor App

**Autor:** Mateo Lavecchia

## Descripción
Sensor App es una aplicación de Android que permite **registrar, visualizar y enviar datos de sensores** (por ejemplo, el sensor de proximidad) a un almacenamiento local y simular su envío a un servidor. La app también muestra **notificaciones durante el proceso de envío**.

## Funcionalidades
- Registrar lecturas del sensor.
- Ver historial de lecturas registradas.
- Enviar registros mediante un servicio en primer plano (`Foreground Service`).
- Notificaciones dinámicas mientras se envían los datos.
- Guarda los datos en un archivo temporal (`sensor_data.txt`).

## Permisos necesarios
- INTERNET
- WAKE_LOCK
- FOREGROUND_SERVICE
- FOREGROUND_SERVICE_DATA_SYNC
- POST_NOTIFICATIONS (Android 13+)
- ACTIVITY_RECOGNITION (opcional, según sensores)

## Uso
1. Abrir la app en un dispositivo Android con sensor disponible.
2. Presionar **"Registrar"** para guardar la lectura actual.
3. Presionar **"Ver historial"** para revisar registros.
4. Presionar **"Enviar"** para subir los datos; se mostrará una notificación indicando el progreso.

## Requisitos
- Android 8.0 (API 26) o superior.
- Android 13+ requiere aceptar permisos de notificaciones.

## Notas
- El envío de datos se simula guardando los registros en un archivo local.
- Las notificaciones del servicio se muestran únicamente si el usuario concede el permiso de notificaciones en Android 13+.

