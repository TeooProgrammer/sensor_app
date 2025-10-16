package com.sensor.sensor_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView sensorValueText, tituloText;
    private Button registerBtn, viewBtn, sendBtn;
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private float currentProximityValue = 0f;
    private SensorDatabaseHelper dbHelper;

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Inicializar vistas ---
        tituloText = findViewById(R.id.txtTitulo);
        sensorValueText = findViewById(R.id.sensorValueText);
        registerBtn = findViewById(R.id.registerBtn);
        viewBtn = findViewById(R.id.viewBtn);
        sendBtn = findViewById(R.id.sendBtn);

        // --- Configurar título ---
        if (tituloText != null) {
            tituloText.setText("Sensor App");
            tituloText.setTextSize(30);
            tituloText.setTextColor(getResources().getColor(R.color.verde_principal));
        }

        dbHelper = new SensorDatabaseHelper(this);

        // --- Configurar sensor de proximidad ---
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (proximitySensor == null) {
                Toast.makeText(this, "No se encontró sensor de proximidad", Toast.LENGTH_LONG).show();
            }
        }

        // ✅ PEDIR PERMISO DE NOTIFICACIONES (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }

        // --- Botón Registrar lectura ---
        if (registerBtn != null) {
            registerBtn.setOnClickListener(v -> {
                if (proximitySensor == null) {
                    Toast.makeText(this, "No hay sensor disponible", Toast.LENGTH_SHORT).show();
                    return;
                }

                long id = dbHelper.insertReading(currentProximityValue);
                String state = (currentProximityValue < 5.0f) ? "Cerca" : "Lejos";

                if (id != -1) {
                    Toast.makeText(this,
                            "Lectura registrada: ID=" + id + ", Valor=" + currentProximityValue + ", Estado=" + state,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error al registrar lectura", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // --- Botón Ver historial ---
        if (viewBtn != null) {
            viewBtn.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            });
        }

        // --- Botón Enviar (inicia servicio de envío) ---
        if (sendBtn != null) {
            sendBtn.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, DataUploadService.class);
                startService(intent);
                Toast.makeText(this, "Enviando registros...", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (proximitySensor != null && sensorManager != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (proximitySensor != null && sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        currentProximityValue = event.values[0];

        if (sensorValueText != null && proximitySensor != null) {
            float maxRange = proximitySensor.getMaximumRange();
            String displayText = (currentProximityValue < maxRange)
                    ? "Cerca: " + currentProximityValue
                    : "Lejos: " + currentProximityValue;

            sensorValueText.setText(displayText);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No se usa
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.closeDB();
    }

    // (Opcional) Mostrar resultado del permiso
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de notificaciones concedido ✅", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso de notificaciones denegado ❌", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

