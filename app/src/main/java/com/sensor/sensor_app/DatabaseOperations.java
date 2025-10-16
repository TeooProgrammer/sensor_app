package com.sensor.sensor_app;

import android.database.Cursor;

public interface DatabaseOperations {
    Cursor getAllRecords();      // Devuelve todos los registros
    int clearAllRecords();       // Borra todos los registros
}

