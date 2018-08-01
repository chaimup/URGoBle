package com.upright.goble.gatt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import com.jakewharton.rx.ReplayingShare;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.upright.goble.connection.URGConnection;
import com.upright.goble.utils.BytesUtil;
import com.upright.goble.utils.Logger;

import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

public class URGatt {

    UUID FIRMWAREUPLOAD_INFO_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    UUID BATTERY_SERVICE = UUID.fromString("0000AAA0-0000-1000-8000-00805f9b34fb");
    UUID BATTERY_CHARACTERISTIC = UUID.fromString("0000AAA1-0000-1000-8000-00805f9b34fb");

    RxBleDevice bleDevice;
    RxBleConnection bleConnection;
    private Disposable connectionDisposable;
    private Observable<RxBleConnection> connectionObservable;
    private PublishSubject<Boolean> disconnectTriggerSubject = PublishSubject.create();
    private Context context;
    RxBleClient rxBleClient;


    public URGatt (URGConnection connection) {
        this.bleDevice = connection.getBleDevice();
        connectionObservable = prepareConnectionObservable();
    }

    public URGatt (Context context) {
        this.context = context;
        connectionObservable = prepareConnectionObservable();
    }

    private void logCharasteristic(byte[] bytes) {
        Logger.log("getCharacteristic: " + (new String(bytes)));
        Logger.log("getHexCharacteristic: " + (BytesUtil.bytesToHex(bytes)));
    }

    private void logCharasteristic(BluetoothGattCharacteristic characteristic) {

        int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        Logger.log("Battery value: ");
    }

    public boolean isConnected() {
        return  bleDevice != null && bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void onReadFailure(Throwable throwable) {
       Logger.log("Read error: " + throwable);
    }

    public void read() {
            connectionObservable
                    .flatMapSingle(RxBleConnection::discoverServices)
                   // .flatMapSingle(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(BATTERY_SERVICE, BATTERY_CHARACTERISTIC))
                    .flatMapSingle(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic( BATTERY_SERVICE, BATTERY_CHARACTERISTIC))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(disposable -> uuu(disposable))
                    .subscribe(
                            characteristic -> {
                                logCharasteristic(characteristic);
                                Logger.log( "Hey, connection has been established!");
                            },
                            this::onConnectionFailure,
                            this::onConnectionFinished
                    );
    }


    public void read1() {
        connectionObservable
                .firstOrError()
                .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(BATTERY_CHARACTERISTIC))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bytes -> {
                   logCharasteristic(bytes);
                }, this::onReadFailure);
    }

    private void onConnectionFailure(Throwable throwable) {
        Logger.log("Gatt: onConnectionFailure:  " + throwable);
    }

    private void onConnectionFinished() {
        Logger.log("Gatt: onConnectionFinished");
    }

    private void uuu(Disposable disposable) {
    }

    private Observable<RxBleConnection> prepareConnectionObservable() {
        rxBleClient = RxBleClient.create(context);
        bleDevice = rxBleClient.getBleDevice("98:7B:F3:5B:F9:BC");

        return bleDevice
                .establishConnection(false)
                .takeUntil(disconnectTriggerSubject)
                //.compose(bindUntilEvent(PAUSE))
                .compose(ReplayingShare.instance());
    }
    private void triggerDisconnect() {
        disconnectTriggerSubject.onNext(true);
    }
}
