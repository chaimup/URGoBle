package com.upright.goble.events;

import lombok.Data;

@Data
public class URGSensorEvent {


    int indexAngle, sensorAngle;
    public URGSensorEvent(int indexAngle, int sensorAngle) {
        this.indexAngle = indexAngle;
        this.sensorAngle = sensorAngle;
    }
}

