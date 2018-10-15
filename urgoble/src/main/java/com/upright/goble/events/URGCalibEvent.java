package com.upright.goble.events;

import lombok.Data;

@Data
public class URGCalibEvent {

    public static int CALIB_STARTED = 1;
    public static int CALIB_FINISHED = 2;

    int state;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public URGCalibEvent(int state) {
        this.state = state;
    }
}
