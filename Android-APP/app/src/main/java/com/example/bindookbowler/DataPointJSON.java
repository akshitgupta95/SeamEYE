package com.example.bindookbowler;

public class DataPointJSON {

    private long time;
    private CoordinateXYZ acceleration;
    private CoordinateXYZ gyroscope;

    public long getTime() {
        return time;
    }

    public CoordinateXYZ getAcceleration() {
        return acceleration;
    }

    public CoordinateXYZ getGyroscope() {
        return gyroscope;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setAcceleration(CoordinateXYZ acceleration) {
        this.acceleration = acceleration;
    }

    public void setGyroscope(CoordinateXYZ gyroscope) {
        this.gyroscope = gyroscope;
    }

    @Override
    public String toString() {
        return "DATA: time: " + (time)
                + "  Acc: " + acceleration.toString() + " Gyro: " + gyroscope.toString();
    }
}
