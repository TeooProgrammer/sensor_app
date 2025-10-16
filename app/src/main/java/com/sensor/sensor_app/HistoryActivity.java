package com.sensor.sensor_app;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private ListView lvHistory;
    private TextView tvStatus;
    private ArrayAdapter<String> adapter;
    private List<String> uploadHistory;
    private List<Integer> uploadIds;

    private Button btnDeleteAll;
    private Button btnEnviar;

    private BroadcastReceiver uploadCompleteReceiver;
    private boolean receiverRegistered = false;

    private final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Historial de Subidas");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        lvHistory = findViewById(R.id.lvHistory);
        tvStatus = findViewById(R.id.tvStatus);
        Button btnBack = findViewById(R.id.btnBack);
        btnDeleteAll = findViewById(R.id.btnDeleteAll);
        btnEnviar = findViewById(R.id.btnEnviar);

        uploadHistory = new ArrayList<>();
        uploadIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, uploadHistory);
        lvHistory.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnDeleteAll.setOnClickListener(v -> {
            try (SensorDatabaseHelper dbHelper = new SensorDatabaseHelper(this)) {
                int deleted = dbHelper.clearAllRecords();
                Toast.makeText(this, "Se borraron " + deleted + " registros", Toast.LENGTH_SHORT).show();
                loadReadings();
            } catch (Exception e) {
                Toast.makeText(this, "Error al borrar registros: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });

        btnEnviar.setOnClickListener(v -> {
            if (uploadHistory.isEmpty()) {
                Toast.makeText(this, "No hay registros para enviar", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent serviceIntent = new Intent(this, DataUploadService.class);
            ContextCompat.startForegroundService(this, serviceIntent);

            updateStatusText("Iniciando subida de registros...");
            btnEnviar.setEnabled(false);
            btnDeleteAll.setEnabled(false);
        });

        lvHistory.setOnItemClickListener((parent, view, position, id) -> goToUploadDetails(position));

        uploadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) return;
                if (DataUploadService.ACTION_UPLOAD_COMPLETE.equals(intent.getAction())) {
                    Toast.makeText(context,
                            "Subida completada y base de datos limpiada.",
                            Toast.LENGTH_LONG).show();
                    updateStatusText("Última subida: ¡Completada con éxito!");
                    loadReadings();
                    btnEnviar.setEnabled(true);
                    btnDeleteAll.setEnabled(true);
                }
            }
        };

        updateStatusText("Esperando resultados del servicio...");
    }

    @SuppressLint("NewApi")
    @Override
    protected void onStart() {
        super.onStart();
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter(DataUploadService.ACTION_UPLOAD_COMPLETE);
            registerReceiver(uploadCompleteReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (receiverRegistered) {
            try {
                unregisterReceiver(uploadCompleteReceiver);
            } catch (IllegalArgumentException ignored) {}
            receiverRegistered = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReadings();
    }

    private void loadReadings() {
        uploadHistory.clear();
        uploadIds.clear();

        Cursor cursor = null;
        try (SensorDatabaseHelper dbHelper = new SensorDatabaseHelper(this)) {
            cursor = dbHelper.getAllRecords();

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
                    float value = cursor.getFloat(cursor.getColumnIndexOrThrow("value"));
                    long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                    String state = cursor.getString(cursor.getColumnIndexOrThrow("state"));

                    String dateStr = sdfDate.format(new Date(timestamp));
                    String timeStr = sdfTime.format(new Date(timestamp));

                    uploadHistory.add("ID " + id + " - Valor: " + value +
                            " - Fecha: " + dateStr + " - Hora: " + timeStr +
                            " - Estado: " + state);
                    uploadIds.add(id);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar registros: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        adapter.notifyDataSetChanged();

        if (uploadHistory.isEmpty()) {
            tvStatus.setTextColor(Color.RED);
            tvStatus.setText("No hay registros disponibles.");
        } else {
            tvStatus.setTextColor(Color.BLACK);
        }
    }

    private void goToUploadDetails(int position) {
        if (position < 0 || position >= uploadIds.size()) {
            Toast.makeText(this, "Registro inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        int realId = uploadIds.get(position);
        Cursor cursor = null;

        try (SensorDatabaseHelper dbHelper = new SensorDatabaseHelper(this);
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {

            cursor = db.query(
                    "readings",
                    new String[]{"_id", "value", "timestamp", "state"},
                    "_id = ?",
                    new String[]{String.valueOf(realId)},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                float value = cursor.getFloat(cursor.getColumnIndexOrThrow("value"));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                String state = cursor.getString(cursor.getColumnIndexOrThrow("state"));

                String dateStr = sdfDate.format(new Date(timestamp));
                String timeStr = sdfTime.format(new Date(timestamp));

                Intent intent = new Intent(this, UploadDetailsActivity.class);
                intent.putExtra("record_id", realId);
                intent.putExtra("record_value", value);
                intent.putExtra("record_date", dateStr);
                intent.putExtra("record_time", timeStr);
                intent.putExtra("record_state", state);
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar detalles: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void updateStatusText(String message) {
        if (tvStatus != null) tvStatus.setText(message);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiverRegistered) {
            try {
                unregisterReceiver(uploadCompleteReceiver);
            } catch (IllegalArgumentException ignored) {}
            receiverRegistered = false;
        }
    }
}
