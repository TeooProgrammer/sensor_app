package com.sensor.sensor_app;

public class SensorReading {
    private int id;
    private int value;

    public SensorReading(int id, int value) {
        this.id = id;
        this.value = value;
    }

    public int getId() { return id; }
    public int getValue() { return value; }
}
