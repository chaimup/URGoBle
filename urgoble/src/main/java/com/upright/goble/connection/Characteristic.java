package com.upright.goble.connection;

import java.util.UUID;

public interface Characteristic {

    void writeCharacteristic(UUID uuid, byte[] action);
    void setupNotification(UUID uuid,  int callbackValue);
    void readCharacteristic(UUID uuid, int callbackValue);
    void readCharacteristic(UUID uuid, byte[] bytesNumber, int callbackValue);

}
