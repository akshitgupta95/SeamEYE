package com.example.bindookbowler;

public class DataBuffer {

    private DataPointBT[] data;

    private int capacity = 0;
    private int writePos = 0;
    private int available = 0;



    public DataBuffer(int capacity){
        this.capacity = capacity;
        this.data = new DataPointBT[capacity];
    }

    public void reset() {
        this.writePos = 0;
        this.available = 0;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public int getAvailable() {
        return this.available;
    }

    public int getRemainingCapacity() {
        return this.capacity - this.available;
    }

    public boolean put(DataPointBT d) {
        if(available < capacity){
            if(writePos >= capacity){
                writePos = 0;
            }
            data[writePos] = d;
            writePos++;
            available++;
            return true;
        }
        return false;
    }

    public DataPointBT take() {
        if(available == 0){
            return null;
        }
        int nextSlot = writePos - available;
        if(nextSlot < 0){
            nextSlot += capacity;
        }
        DataPointBT nextDataPointBT = data[nextSlot];
        available--;
        return nextDataPointBT;
    }

    public DataPointBT most_recent() {
        if(available == 0){
            return null;
        }

        int nextSlot = writePos - 1;
        if(nextSlot < 0){
            nextSlot += capacity;
        }
        DataPointBT nextDataPointBT = data[nextSlot];
        return nextDataPointBT;
    }




}
