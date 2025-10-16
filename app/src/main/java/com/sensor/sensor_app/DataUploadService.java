package com.sensor.sensor_app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataUploadService extends Service {

    public static final String ACTION_UPLOAD_COMPLETE = "com.sensor.sensor_app.ACTION_UPLOAD_COMPLETE";
    private static final String CHANNEL_ID = "DataUploadChannel";
    private static final int NOTIFICATION_ID = 101;
    private static final String TAG = "DataUploadService";

    private SensorDatabaseHelper dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new SensorDatabaseHelper(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (dbHelper == null) dbHelper = new SensorDatabaseHelper(this);

        // ✅ Crear canal antes de iniciar foreground
        createNotificationChannel();

        // ✅ Mostrar notificación solo si tiene permiso
        if (tienePermisoNotificaciones()) {
            Notification notification = buildNotification("Iniciando subida de registros...");
            startForeground(NOTIFICATION_ID, notification);
        } else {
            Log.w(TAG, "Sin permiso de notificaciones — ejecutando en silencio");
        }

        new Thread(() -> {
            try {
                uploadData();
            } catch (Exception e) {
                Log.e(TAG, "Error general en hilo de subida", e);
                updateNotification("Error durante la subida");
                stopForeground(true);
                stopSelf();
            }
        }).start();

        return START_NOT_STICKY;
    }

    private void uploadData() {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getAllRecords();
            if (cursor == null || cursor.getCount() == 0) {
                updateNotification("No hay registros para subir.");
                stopForeground(true);
                stopSelf();
                return;
            }

            StringBuilder sb = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            int idIndex = cursor.getColumnIndex("_id");
            int valueIndex = cursor.getColumnIndex("value");
            int timeIndex = cursor.getColumnIndex("timestamp");
            int stateIndex = cursor.getColumnIndex("state");

            while (cursor.moveToNext()) {
                int id = cursor.getInt(idIndex);
                float value = cursor.getFloat(valueIndex);
                long timestamp = cursor.getLong(timeIndex);
                String state = cursor.getString(stateIndex);

                sb.append("ID: ").append(id)
                        .append(", Valor: ").append(value)
                        .append(", Fecha y Hora: ").append(sdf.format(new Date(timestamp)))
                        .append(", Estado: ").append(state)
                        .append("\n");
            }

            try (FileOutputStream fos = openFileOutput("sensor_data.txt", Context.MODE_PRIVATE)) {
                fos.write(sb.toString().getBytes());
            } catch (Exception e) {
                Log.e(TAG, "Error al escribir archivo temporal", e);
            }

            String[] lines = sb.toString().split("\n");

            for (int i = 0; i < lines.length; i++) {
                updateNotification("Enviando registro " + (i + 1) + " de " + lines.length);
                try { Thread.sleep(800); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }

            int deleted = dbHelper.clearAllRecords();
            Log.i(TAG, "Registros eliminados: " + deleted);

            updateNotification("Subida completa. Registros enviados: " + lines.length);

            Intent broadcastIntent = new Intent(ACTION_UPLOAD_COMPLETE);
            sendBroadcast(broadcastIntent);

        } catch (Exception e) {
            Log.e(TAG, "Error en la subida", e);
            updateNotification("Error durante la subida de registros.");
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            stopForeground(true);
            stopSelf();
        }
    }

    private Notification buildNotification(String text) {
        Intent intent = new Intent(this, HistoryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Subida de datos del sensor")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_upload)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 👈 Visible en Android 15
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // 👈 Muestra inmediata
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        if (!tienePermisoNotificaciones()) {
            Log.w(TAG, "No se actualiza notificación (sin permiso)");
            return;
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification(text));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Subida de datos",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones del envío de datos del sensor");
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private boolean tienePermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

