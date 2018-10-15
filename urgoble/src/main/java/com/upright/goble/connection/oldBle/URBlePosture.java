package com.upright.goble.connection.oldBle;

import android.content.Context;

import com.upright.goble.connection.Characteristic;

import java.util.UUID;

import static com.upright.goble.connection.URMain.RANGE_READ;
import static com.upright.goble.connection.oldBle.URBleBase.TRACK_TRAIN_UUID;
import static com.upright.goble.connection.URMain.USE_MODE_READ;


public class URBlePosture {

    URGConnection urgConnection;
    Characteristic characteristic;

    public static final UUID RANGE_UUID = UUID.fromString("0000aac1-0000-1000-8000-00805f9b34fb");//SENSENTIVITY_UUID

    public static final int  TRACK_MODE = 1,TRAIN_MODE = 0;

    public URBlePosture(Context context, Characteristic characteristic) {
        urgConnection = URGConnection.init(context);
        this.characteristic = characteristic;
    }

    public void setRangeValue(final int value)
    {
        if (value!=10)
            writeValue(RANGE_UUID, new byte[]{(byte) value});
        else
            writeValue(RANGE_UUID, new byte[]{(byte) 9});
    }

    //readSensitivityValue
    public void readRangeValue()
    {
        characteristic.readCharacteristic(RANGE_UUID, RANGE_READ);
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
