package com.sensor.sensor_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    private Button registerBtn, viewBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        registerBtn = findViewById(R.id.menuRegisterBtn);
        viewBtn = findViewById(R.id.menuViewBtn);

        registerBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        viewBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }
}
