package com.upright.goble.events;

import static com.upright.goble.utils.BatteryUtils.batteryLevel;

public class URGBatteryLevelEvent {

    int value;

    public int getValue() {
        int batteryValue = batteryLevel(value);
        return batteryValue;
    }

    public URGBatteryLevelEvent(int value) {
        this.value = value;
    }
}
