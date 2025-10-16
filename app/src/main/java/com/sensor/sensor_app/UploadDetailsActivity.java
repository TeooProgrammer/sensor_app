package com.sensor.sensor_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Muestra los detalles de una subida seleccionada desde el historial.
 */
public class UploadDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_details);

        // Configuración de ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_upload_details));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Enlazar TextViews según el XML actualizado
        TextView tvDetailTitle = findViewById(R.id.tvDetailTitle);
        TextView tvRecordInfoId = findViewById(R.id.tvRecordInfoId);
        TextView tvRecordInfoValue = findViewById(R.id.tvRecordInfoValue);
        TextView tvRecordInfoDateTime = findViewById(R.id.tvRecordInfoDateTime);
        TextView tvRecordInfoState = findViewById(R.id.tvRecordInfoState); // nuevo TextView para Estado

        // Validar existencia de los TextViews
        if (tvDetailTitle == null || tvRecordInfoId == null
                || tvRecordInfoValue == null || tvRecordInfoDateTime == null
                || tvRecordInfoState == null) {
            finish(); // Cierra la actividad si falta algún TextView
            return;
        }

        // Recuperar datos del Intent
        String title = getIntent() != null ? getIntent().getStringExtra("detail_title") : null;
        int recordId = getIntent() != null ? getIntent().getIntExtra("record_id", -1) : -1;
        float value = getIntent() != null ? getIntent().getFloatExtra("record_value", 0f) : 0f;
        String date = getIntent() != null ? getIntent().getStringExtra("record_date") : "N/D";
        String time = getIntent() != null ? getIntent().getStringExtra("record_time") : "N/D";
        String state = getIntent() != null ? getIntent().getStringExtra("record_state") : "N/D";

        // Asignar textos a los TextViews
        tvDetailTitle.setText(title != null && !title.isEmpty() ? title : "Detalle del Registro");
        tvRecordInfoId.setText(recordId != -1 ? "ID del Registro: " + recordId : "ID del Registro: N/D");
        tvRecordInfoValue.setText("Valor: " + value);
        tvRecordInfoDateTime.setText("Fecha y Hora: " + date + " " + time);
        tvRecordInfoState.setText("Estado: " + state);

        // Enlazar botón Volver
        Button btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish()); // Cierra la actividad al tocarlo
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Cierra la actividad al tocar la flecha de volver
        return true;
    }
}
