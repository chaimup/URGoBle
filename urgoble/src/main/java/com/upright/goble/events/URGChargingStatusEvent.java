package com.upright.goble.events;

public class URGChargingStatusEvent {

    int value;

    public String getValue() {
        String chargingStatus = "DISCONNECTED";
        if(value == 1){
            chargingStatus = "CONNECTED NOT FINISHED";
        } else if(value==2){
            chargingStatus = "CONNECTED FINISHED";
        }
        return chargingStatus;
    }

    public URGChargingStatusEvent(int value) {
        this.value = value;
    }
}
