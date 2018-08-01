package com.upright.goble.events;

import lombok.Data;

@Data
public class URGSensorEvent {


    int angle;
    public URGSensorEvent(int angle) {
        this.angle = angle;
    }
}

