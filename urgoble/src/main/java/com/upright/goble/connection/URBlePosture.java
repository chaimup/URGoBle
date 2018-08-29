package com.upright.goble.connection;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import java.util.UUID;

import static com.upright.goble.connection.URBleBase.TRACK_TRAIN_UUID;
import static com.upright.goble.connection.URMain.USE_MODE_READ;


public class URBlePosture {

    URGConnection urgConnection;
    Characteristic characteristic;

    public static final UUID SENSITIVITY_UUID = UUID.fromString("0000aac1-0000-1000-8000-00805f9b34fb");

    public static final int  TRACK_MODE = 1,TRAIN_MODE = 0;

    public URBlePosture(Context context, Characteristic characteristic) {
        urgConnection = URGConnection.init(context);
        this.characteristic = characteristic;
    }

    public void setSensitivityValue(final int value)
    {
        if (value!=10)
            writeValue(SENSITIVITY_UUID, new byte[]{(byte) value});
        else
            writeValue(SENSITIVITY_UUID, new byte[]{(byte) 9});
    }

    public void writeTrackModeValue()
    {
        writeValue(TRACK_TRAIN_UUID, new byte[]{TRACK_MODE});
//        setState(DEVICE_STATE_TRACKING);
    }

    public void writeTrainModeValue()
    {
        writeValue(TRACK_TRAIN_UUID, new byte[]{TRAIN_MODE});
//        setState(DEVICE_STATE_TRAIN_PLAYING);
    }

    public void writeValue(UUID uuid, byte[] action)
    {
        characteristic.writeCharacteristic(uuid,action);
    }

    public void readUseModeValue()
    {
        characteristic.readCharacteristic(TRACK_TRAIN_UUID, USE_MODE_READ);
    }


}
