package com.upright.goble.events;

import lombok.Data;

@Data
public class URGScanEvent {
    String macAddress;

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public URGScanEvent(String macAddress) {
        this.macAddress = macAddress;
    }
}
