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
import androidx.core.content.ContextCompat;

public class RegisterActivity extends AppCompatActivity implements SensorEventListener {

    private TextView sensorValueText;
    private Button registerBtn, sendBtn;
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private float currentProximityValue = 0f;
    private SensorDatabaseHelper dbHelper;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sensorValueText = findViewById(R.id.sensorValueText);
        registerBtn = findViewById(R.id.registerBtn);
        sendBtn = findViewById(R.id.sendBtn);
        dbHelper = new SensorDatabaseHelper(this);

        // Permiso POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
            }
        }

        // Inicializar sensor de proximidad
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        if (proximitySensor == null) {
            Toast.makeText(this, "Sensor de proximidad no disponible", Toast.LENGTH_LONG).show();
        }

        registerBtn.setOnClickListener(v -> {
            long id = dbHelper.insertReading(currentProximityValue);
            String state = (proximitySensor != null && currentProximityValue < proximitySensor.getMaximumRange()) ? "Cerca" : "Lejos";
            sensorValueText.setText(state + " (" + currentProximityValue + ")");
            Toast.makeText(this, id != -1 ? "Lectura registrada ID=" + id : "Error al registrar lectura", Toast.LENGTH_SHORT).show();
        });

        sendBtn.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, DataUploadService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent);
            else startService(serviceIntent);
            Toast.makeText(this, "Enviando datos...", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (proximitySensor != null) sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (proximitySensor != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        currentProximityValue = event.values[0];
        if (proximitySensor != null) {
            String displayText = (currentProximityValue < proximitySensor.getMaximumRange()) ? "Cerca: " : "Lejos: ";
            sensorValueText.setText(displayText + currentProximityValue);
        } else {
            sensorValueText.setText("Sensor no disponible");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.closeDB();
    }
}
