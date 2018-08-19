package com.upright.goble.connection;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.polidea.rxandroidble2.RxBleConnection;
import com.upright.goble.events.URGBatteryLevelEvent;
import com.upright.goble.events.URGChargingStatusEvent;
import com.upright.goble.utils.BytesUtil;
import com.upright.goble.utils.Logger;

import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;

import static java.util.UUID.fromString;

public class URBlePower {
    URGConnection urgConnection;
    public URBlePower(Context context) {
        urgConnection = URGConnection.init(context);
    }

    public static final UUID HALL_CONTROL_UUID = fromString("0000aaa4-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_CHARACTERISTIC = UUID.fromString("0000AAA1-0000-1000-8000-00805f9b34fb");
    public static final UUID TRACK_TRAIN_UUID = UUID.fromString("0000aac7-0000-1000-8000-00805f9b34fb");;
    public static final UUID CHARGING_STATUS_UUID = UUID.fromString("0000aaa2-0000-1000-8000-00805f9b34fb");

    public static final byte    GO_SLEEP = 7;
    public static final byte    NO_TRANSMISSION = 8;

    public void turnOff() {
        if(urgConnection.isConnected()){
            urgConnection.getConnectionObservable()
                    .firstOrError()
                    .flatMap(bleConnection -> bleConnection.writeCharacteristic(HALL_CONTROL_UUID, new byte[]{GO_SLEEP}))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> urgConnection.onWriteSuccess(),
                            urgConnection::onWriteFailure
                    );
        }
    }

    public void readChargingStatus()
    {
        if(urgConnection.isConnected()){

            urgConnection.getConnectionObservable()
                    .firstOrError()
                    .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(CHARGING_STATUS_UUID))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(characteristic -> {
                        onReadSuccess(characteristic);
                    }, this::onReadFailure);
        }
    }

    public void registerChargingStatus() {
        if (urgConnection.isConnected()) {
            urgConnection.getConnectionObservable()
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(CHARGING_STATUS_UUID))
                    .doOnNext(notificationObservable -> Logger.log("Setting up sensor notification..."))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onReadSuccess, this::onReadFailure);
        }
    }

    public void readBatteryValue()
    {
        if(urgConnection.isConnected()){

            urgConnection.getConnectionObservable()
                    .firstOrError()
                    .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(BATTERY_CHARACTERISTIC))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(characteristic -> {
                        onReadBatterySuccess(characteristic);
                    }, this::onReadFailure);
        }
    }

    public void registerOnBatteryValueChange()
    {
        if (urgConnection.isConnected()) {
            urgConnection.getConnectionObservable()
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(BATTERY_CHARACTERISTIC))
                    .doOnNext(notificationObservable -> Logger.log("Setting up sensor notification..."))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onReadBatterySuccess, this::onReadFailure);
        }
    }

    public void onSwitchTrainingTracking(boolean autoSwitch)
    {
        if(urgConnection.isConnected()){
            urgConnection.getConnectionObservable()
                    .firstOrError()
                    .flatMap(bleConnection -> bleConnection.writeCharacteristic(TRACK_TRAIN_UUID, new byte[]{(byte) (autoSwitch ? 1 : 0)}))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> urgConnection.onWriteSuccess(),
                            urgConnection::onWriteFailure
                    );
        }
    }

    public void airplaneMode()
    {
        if(urgConnection.isConnected()){
            urgConnection.getConnectionObservable()
                    .firstOrError()
                    .flatMap(bleConnection -> bleConnection.writeCharacteristic(HALL_CONTROL_UUID, new byte[]{NO_TRANSMISSION}))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> urgConnection.onWriteSuccess(),
                            urgConnection::onWriteFailure
                    );
        }
    }

    private void onReadBatterySuccess(byte[] characteristic) {
        int batteryValue = BytesUtil.bytesToInt(characteristic, 1);

        urgConnection.getEventBus().send(new URGBatteryLevelEvent(batteryValue));
    }


    public void onReadFailure(Throwable throwable) {
        Logger.log("onWriteFailure: " + throwable);
    }

    public void onReadSuccess(byte[] characteristic) {
        int state = BytesUtil.bytesToInt(characteristic, 1);

        urgConnection.getEventBus().send(new URGChargingStatusEvent(state));
        Logger.log("onWriteSuccess");
    }

}
