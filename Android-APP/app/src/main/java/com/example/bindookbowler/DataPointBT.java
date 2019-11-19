package com.example.bindookbowler;

public class DataPointBT {

    public int time;
    public double ax, ay, az;
    public double gx, gy, gz;

    private String rawData;

    public DataPointBT(int t, double ax, double ay, double az, double gx, double gy, double gz) {
        this.time = t;
        this.ax = ax;
        this.ay = ay;
        this.az = az;

        this.gx = gx;
        this.gy = gy;
        this.gz = gz;
    }


    public String toFile() {
        String res = String.valueOf(this.time) + ",";
        res +=  String.valueOf(this.ax) + ",";
        res +=  String.valueOf(this.ay) + ",";
        res +=  String.valueOf(this.az) + ",";
        res +=  String.valueOf(this.gx) + ",";
        res +=  String.valueOf(this.gy) + ",";
        res +=  String.valueOf(this.gz) + "\n\r";
        return res;
    }


    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public double getAx() {
        return ax;
    }

    public void setAx(double ax) {
        this.ax = ax;
    }

    public double getAy() {
        return ay;
    }

    public void setAy(double ay) {
        this.ay = ay;
    }

    public double getAz() {
        return az;
    }

    public void setAz(double az) {
        this.az = az;
    }

    public double getGx() {
        return gx;
    }

    public void setGx(double gx) {
        this.gx = gx;
    }

    public double getGy() {
        return gy;
    }

    public void setGy(double gy) {
        this.gy = gy;
    }

    public double getGz() {
        return gz;
    }

    public void setGz(double gz) {
        this.gz = gz;
    }
}
