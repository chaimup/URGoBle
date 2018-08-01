package com.upright.goble.events;

import lombok.Data;

@Data
public class URGScanEvent {
    String macAddress;

    public URGScanEvent(String macAddress) {
        this.macAddress = macAddress;
    }
}
