package com.sensor.sensor_app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class SensorDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sensor_readings.db";
    private static final int DATABASE_VERSION = 2; // versión actualizada
    private static final String TABLE_READINGS = "readings";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_VALUE = "value";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_STATE = "state"; // nuevo atributo

    private static final String TAG = "SensorDatabaseHelper";

    private SQLiteDatabase database;

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_READINGS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_VALUE + " REAL," +
                    COLUMN_TIMESTAMP + " INTEGER," +
                    COLUMN_STATE + " TEXT)"; // se guarda "Cerca" o "Lejos"

    public SensorDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        database = getWritableDatabase(); // abrimos la DB al crear el helper
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        Log.d(TAG, "Tabla de lecturas creada.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_READINGS +
                    " ADD COLUMN " + COLUMN_STATE + " TEXT DEFAULT 'Desconocido'");
            Log.d(TAG, "Columna 'state' agregada en la actualización de DB.");
        }
    }

    // Guardar lectura de proximidad
    public long insertReading(float value) {
        long newRowId = -1;
        String state = (value < 5.0f) ? "Cerca" : "Lejos"; // definir estado según valor

        try {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_VALUE, value);
            cv.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
            cv.put(COLUMN_STATE, state);
            newRowId = database.insert(TABLE_READINGS, null, cv);
            Log.d(TAG, "Lectura insertada: ID=" + newRowId + ", Valor=" + value + ", Estado=" + state);
        } catch (Exception e) {
            Log.e(TAG, "Error al insertar lectura: " + e.getMessage());
        }
        return newRowId;
    }

    // Obtener todos los registros
    public Cursor getAllRecords() {
        Cursor cursor = null;
        try {
            cursor = database.query(
                    TABLE_READINGS,
                    new String[]{COLUMN_ID, COLUMN_VALUE, COLUMN_TIMESTAMP, COLUMN_STATE},
                    null,
                    null,
                    null,
                    null,
                    COLUMN_TIMESTAMP + " DESC"
            );
            Log.d(TAG, "Cursor obtenido con registros.");
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener registros: " + e.getMessage());
        }
        return cursor;
    }

    // Borrar todos los registros
    public int clearAllRecords() {
        int rowsDeleted = 0;
        try {
            rowsDeleted = database.delete(TABLE_READINGS, null, null);
            Log.d(TAG, "Se borraron " + rowsDeleted + " registros.");
        } catch (Exception e) {
            Log.e(TAG, "Error al limpiar registros: " + e.getMessage());
        }
        return rowsDeleted;
    }

    // Cerrar la DB cuando la app ya no la necesite
    public void closeDB() {
        if (database != null && database.isOpen()) {
            database.close();
            Log.d(TAG, "Base de datos cerrada.");
        }
    }

    @Override
    public void close() {
        closeDB();
        super.close();
    }
}
