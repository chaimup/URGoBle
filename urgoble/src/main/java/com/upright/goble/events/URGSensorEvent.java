package com.upright.goble.events;

import lombok.Data;

@Data
public class URGSensorEvent {


    int indexAngle, sensorAngle;

    public int getIndexAngle() {
        return indexAngle;
    }

    public void setIndexAngle(int indexAngle) {
        this.indexAngle = indexAngle;
    }

    public int getSensorAngle() {
        return sensorAngle;
    }

    public void setSensorAngle(int sensorAngle) {
        this.sensorAngle = sensorAngle;
    }

    public URGSensorEvent(int indexAngle, int sensorAngle) {
        this.indexAngle = indexAngle;
        this.sensorAngle = sensorAngle;
    }
}

