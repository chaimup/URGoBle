package com.upright.goble.connection.newBle;

import android.content.Context;

import com.upright.goble.connection.Characteristic;
import com.upright.goble.connection.oldBle.URBleBase;

import java.util.UUID;

import static com.upright.goble.connection.URMain.DEVICE_MODEL_READ;
import static com.upright.goble.connection.URMain.FIRMWARE_UPDATE_INFO_READ;
import static com.upright.goble.connection.URMain.SYSTEM_INFO_READ;

public class URBleBaseNew extends URBleBase {

    public static final UUID DEVICE_MODEL_UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    public static final UUID SYSTEM_INFO_UUID = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");

    public URBleBaseNew(Context context, Characteristic characteristic) {
        super(context, characteristic);
    }

    public void readSystemInfo()
    {
        characteristic.readCharacteristic(SYSTEM_INFO_UUID, SYSTEM_INFO_READ);
    }

    public void readDeviceModel()
    {
        characteristic.readCharacteristic(DEVICE_MODEL_UUID, DEVICE_MODEL_READ);
    }

    public  void readDeviceVersion() {
        characteristic.readCharacteristic(FIRMWAREUPDATE_INFO_VERSION_UUID, FIRMWARE_UPDATE_INFO_READ);
    }

}
