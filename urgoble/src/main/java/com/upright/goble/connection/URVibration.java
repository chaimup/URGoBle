package com.upright.goble.connection;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;

public class URVibration {
    URGConnection urgConnection;

    public static final UUID VIBRATE_DELAY_CHARACTERISTIC_UUID = UUID.fromString("0000aac2-0000-1000-8000-00805f9b34fb");
    public static final UUID VIBRATE_PERSONALIZATION_CHARACTERISTIC_UUID = UUID.fromString("0000aaa5-0000-1000-8000-00805f9b34fb");
    ;

    public URVibration(Context context) {
        urgConnection = URGConnection.init(context);
    }

    public void vibrationDelayAndPeriods(final int delay, final int repeating) {
        if (delay < 26) {
            byte new_delay = (byte) (delay * 10);
            byte[] byte_delivery_array = {new_delay, 0, (byte) repeating, 30};
            if (urgConnection.isConnected()) {
                urgConnection.getConnectionObservable()
                        .firstOrError()
                        .flatMap(bleConnection -> bleConnection.writeCharacteristic(VIBRATE_DELAY_CHARACTERISTIC_UUID, byte_delivery_array))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                bytes -> urgConnection.onWriteSuccess(),
                                urgConnection::onWriteFailure
                        );
            }

        } else {
            byte new_delay = 0x04;

            switch (delay) {
                case 26:
                    new_delay = 0x04;
                    break;
                case 27:
                    new_delay = 0x0E;
                    break;
                case 28:
                    new_delay = 0x18;
                    break;
                case 29:
                    new_delay = 0x22;
                    break;
                case 30:
                    new_delay = 0x2C;
                    break;
            }

            byte[] byte_delivery_array = {new_delay, 1, (byte) repeating, 30};
            if (urgConnection.isConnected()) {
                urgConnection.getConnectionObservable()
                        .firstOrError()
                        .flatMap(bleConnection -> bleConnection.writeCharacteristic(VIBRATE_DELAY_CHARACTERISTIC_UUID, byte_delivery_array))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                bytes -> urgConnection.onWriteSuccess(),
                                urgConnection::onWriteFailure
                        );
            }
        }
    }

    public void onVibratePatternChange(int pattern, int strength)
    {
        byte new_pattern = (byte) pattern;
        byte current_strength = (byte)strength;
        byte[] byte_delivary_array = {new_pattern,current_strength,(byte) 1};

        if (urgConnection.isConnected()) {
            urgConnection.getConnectionObservable()
                    .firstOrError()
                    .flatMap(bleConnection -> bleConnection.writeCharacteristic(VIBRATE_PERSONALIZATION_CHARACTERISTIC_UUID, byte_delivary_array))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> urgConnection.onWriteSuccess(),
                            urgConnection::onWriteFailure
                    );
        }


    }
}
