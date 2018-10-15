package com.upright.goble.connection;

import android.content.Context;

import com.upright.goble.connection.oldBle.URGConnection;
import com.upright.goble.events.URGBatteryLevelEvent;
import com.upright.goble.events.URGChargingStatusEvent;
import com.upright.goble.events.URGDeviceInfoEvent;
import com.upright.goble.events.URGWriteEvent;
import com.upright.goble.utils.BytesUtil;
import com.upright.goble.utils.Logger;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;

public class URMain implements Characteristic{

    public static final int CHARGING_STATUS_READ = 0;
    public static final int BATTERY_VALUE_READ = 1;
    public static final int FIRMWARE_UPDATE_INFO_READ = 2;
    public static final int HARDWARE_REVISION_READ = 3;
    public static final int SYSTEM_INFO_READ = 4;
    public static final int DELAY_READ = 5;
    public static final int PATTERN_READ = 6;
    public static final int AUTO_SWITCH_TRACKING = 7;
    public static final int USE_MODE_READ = 8;
    public static final int DATA_SYNC_READ = 9;
    public static final int DATA_SYNC_NOTIFICATION = 10;
    public static final int DATA_ONLINE_NOTIFICATION = 11;
    public static final int RANGE_READ = 12;
    //NEW UPRIGHT 2.0
    public static final int POWER_ERROR_CODE_READ = 13;
    public static final int DEVICE_MODEL_READ = 14;
    public static final int TRAINING_STATUS_READ = 15;
    public static final int VIBRATION_STRENGTH_READ = 16;

    URGConnection urgConnection;

    public URMain(Context context) {
        urgConnection = URGConnection.init(context);
    }

    @Override
    public void writeCharacteristic(UUID uuid, byte[] action){
        if(urgConnection.isConnected()){
            urgConnection.getConnectionObservable()
                    .firstOrError()
                    .flatMap(bleConnection -> bleConnection.writeCharacteristic(uuid, action))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> urgConnection.onWriteSuccess(),
                            urgConnection::onWriteFailure
                    );
        }
    }

    @Override
    public void setupNotification(UUID uuid, int callbackValue) {
        if (urgConnection.isConnected()) {
            urgConnection.getConnectionObservable()
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(uuid))
                    .doOnNext(notificationObservable -> Logger.log("Setting up sensor notification..."))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(characteristic -> {
                        onReadSuccess(characteristic,callbackValue);
                    }, this::onReadFailure);
        }
    }

    @Override
    public void readCharacteristic(UUID uuid, int callbackValue) {
        if(urgConnection.isConnected()){
            urgConnection.getConnectionObservable()
                    .firstOrError()
                    .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(uuid))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(characteristic -> {
                       onReadSuccess(characteristic,callbackValue);
                    }, this::onReadFailure);
        }
    }

    @Override
    public void readCharacteristic(UUID uuid, byte[] bytesNumber, int callbackValue) {
        if(urgConnection.isConnected()){
            urgConnection.getConnectionObservable()
                    .firstOrError()
                    .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(uuid))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(characteristic -> {
                        onReadSuccess(characteristic,callbackValue);
                    }, this::onReadFailure);
        }
    }

    public void onWriteFailure(Throwable throwable) {

    }

    public void onWriteSuccess() {
        urgConnection.getEventBus().send(new URGWriteEvent());
        Logger.log("onWriteSuccess");
    }

    public void onReadFailure(Throwable throwable) {
        Logger.log("onWriteFailure: " + throwable);
    }

    public void onReadSuccess(byte[] characteristic, int callbackValue) {
        switch (callbackValue) {
            case CHARGING_STATUS_READ:
                int state = BytesUtil.bytesToInt(characteristic, 1);
                urgConnection.getEventBus().send(new URGChargingStatusEvent(state));
                break;
            case BATTERY_VALUE_READ:
                int batteryValue = BytesUtil.bytesToInt(characteristic, 1);
                urgConnection.getEventBus().send(new URGBatteryLevelEvent(batteryValue));
                break;
            case DELAY_READ:
                byte[] newByteArray = {characteristic[1], characteristic[0]};
                int delay = ByteBuffer.wrap(newByteArray).getShort() / 10;
                break;
            case FIRMWARE_UPDATE_INFO_READ:
                String version = "0.0.0";
                try {
                    version = new String(characteristic, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Logger.log("onWriteSuccess");
                urgConnection.getEventBus().send(new URGDeviceInfoEvent(version));
                break;
            case HARDWARE_REVISION_READ:
                int test = 0;
                break;
            case SYSTEM_INFO_READ:
                byte[] systemInfoData = characteristic;
                break;
            case PATTERN_READ:
                int pattern = characteristic[0];
                break;
            case AUTO_SWITCH_TRACKING:
                int isToggle = BytesUtil.bytesToInt(characteristic, 1);
                boolean allowRestingBreaks;
                if (isToggle == 0)
                    allowRestingBreaks = true;
                else
                    allowRestingBreaks = false;
                break;
            case USE_MODE_READ:
//                postureUseModeChanged(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0));
                break;
            case DATA_SYNC_READ:
                int value = BytesUtil.bytesToInt(characteristic, characteristic.length);
                int currentTimeStamp = value / 600;
                break;
            case DATA_SYNC_NOTIFICATION:
                //go to registerToDataNotification() onCharacteristicChanged(byte[] bytes)
                break;
            case DEVICE_MODEL_READ:
                //return device model
                break;
            default:
                break;
        }
    }


}
