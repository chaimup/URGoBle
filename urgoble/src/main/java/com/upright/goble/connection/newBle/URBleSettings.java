package com.upright.goble.connection.newBle;

import android.content.Context;

import com.upright.goble.connection.Characteristic;
import com.upright.goble.connection.oldBle.URGConnection;

import java.util.UUID;

import static com.upright.goble.connection.URMain.DELAY_READ;
import static com.upright.goble.connection.URMain.PATTERN_READ;
import static com.upright.goble.connection.URMain.RANGE_READ;
import static com.upright.goble.connection.URMain.TRAINING_STATUS_READ;
import static com.upright.goble.connection.URMain.VIBRATION_STRENGTH_READ;
import static com.upright.goble.connection.newBle.BytesNumber.DELAY_BYTES_NUMBER;
import static com.upright.goble.connection.newBle.BytesNumber.PATTERN_BYTES_NUMBER;
import static com.upright.goble.connection.newBle.BytesNumber.RANGE_BYTES_NUMBER;
import static com.upright.goble.connection.newBle.BytesNumber.STRENGTH_BYTES_NUMBER;
import static com.upright.goble.connection.newBle.BytesNumber.TRAIN_STATUS_BYTES_NUMBER;

public class URBleSettings{

    public static final UUID SETTINGS_UUID = UUID.fromString("0000bab1-0000-1000-8000-00805f9b34fb");

    Characteristic characteristic;
    URGConnection urgConnection;


    public URBleSettings(Context context, Characteristic characteristic) {
        urgConnection = URGConnection.init(context);
        this.characteristic = characteristic;
    }

    //read
    public void readRange() {
        characteristic.readCharacteristic(SETTINGS_UUID, RANGE_BYTES_NUMBER, RANGE_READ);
    }

    public void readDelay() {
        characteristic.readCharacteristic(SETTINGS_UUID, DELAY_BYTES_NUMBER, DELAY_READ);
    }

    public void readTrainStatus() {
        characteristic.readCharacteristic(SETTINGS_UUID, TRAIN_STATUS_BYTES_NUMBER, TRAINING_STATUS_READ);
    }

    public void readVibrationPattern()
    {
        characteristic.readCharacteristic(SETTINGS_UUID, PATTERN_BYTES_NUMBER, PATTERN_READ);
    }

    public void readVibrationStrength()
    {
        characteristic.readCharacteristic(SETTINGS_UUID, STRENGTH_BYTES_NUMBER, VIBRATION_STRENGTH_READ);
    }
    //write



    //notification



}
