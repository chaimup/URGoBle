package com.upright.goble.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;

import com.jakewharton.rx.ReplayingShare;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;
import com.upright.goble.events.URGConnEvent;
import com.upright.goble.events.URGEventBus;
import com.upright.goble.events.URGReadEvent;
import com.upright.goble.events.URGScanEvent;
import com.upright.goble.events.URGSensorEvent;
import com.upright.goble.events.URGWriteEvent;
import com.upright.goble.utils.BytesUtil;
import com.upright.goble.utils.Logger;

import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

public class URGConnection {

    private RxBleClient rxBleClient;
    private RxBleDevice bleDevice;
    private URGEventBus eventBus;
    private Observable<RxBleConnection> connectionObservable;
    private Disposable scanDisposable;
    private PublishSubject<Boolean> disconnectTriggerSubject = PublishSubject.create();
    CountDownTimer scanTimer;
    CountDownTimer connectionAttemptsTimer;

    // Demo/POC characteristics
    UUID BATTERY_CHARACTERISTIC = UUID.fromString("0000AAA1-0000-1000-8000-00805f9b34fb");
    UUID CALIB_CMD_UUID = UUID.fromString("0000aab1-0000-1000-8000-00805f9b34fb");
    byte CALIB_COMMAND_STRAIGHT_CALIB_VALUE = 1;
    UUID SENSOR_SERVICE_UUID = UUID.fromString("0000aad0-0000-1000-8000-00805f9b34fb");
    UUID SENSOR_CHARACTERISTIC_UUID = UUID.fromString("0000aad1-0000-1000-8000-00805f9b34fb");
    private static final String DEVICE_SEARCH_STRING = "upright";
    Hashtable<BluetoothDevice, Integer> bleDevices = new Hashtable<BluetoothDevice, Integer>();

    public URGConnection(Context context) {
        rxBleClient = RxBleClient.create(context);
        //bleDevice = rxBleClient.getBleDevice(macAddress);
        eventBus = URGEventBus.bus();
        setScanTimer();
        setAutoConnectTimer(true);
    }

    private void setAutoConnectTimer(boolean enable) {
        Logger.log("setAutoConnectTimer: " + enable);

        if(enable) {
            connectionAttemptsTimer = new CountDownTimer(6000000, 2000) {
                public void onTick(long millisUntilFinished) {
                    Logger.log("checking auto connect...");
                    if (isConnected())
                        Logger.log("device already connected");
                    else if (bleDevice != null && bleDevice.getMacAddress().length() > 0) {
                        Logger.log("trying to auto connect...");
                        bleConnect();
                    }
                }

                public void onFinish() {
                }
            }.start();
        } else {
            if(connectionAttemptsTimer != null)
                connectionAttemptsTimer.cancel();
        }
    }

    private void setScanTimer() {
        scanTimer = new CountDownTimer(3000, 1000) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                Logger.log("scan finished");
                scanDisposable.dispose();
                BluetoothDevice device = getClosestDevice();
                if (device == null) {
                    eventBus.send(new URGScanEvent(null));
                } else {
                    subscribeBleDevice(device.getAddress());
                    eventBus.send(new URGScanEvent(device.getAddress()));
                    bleConnect();
                }
            }
        };
    }

    private <RxBleConnectionState> void onConnectionStateChange(RxBleConnection.RxBleConnectionState state) {
        Logger.log("onConnectionStateChange: " + state.toString());
        eventBus.send(new URGConnEvent(state));
    }

    public void bleConnect() {
        if(!bluetoothEnabled())
            return;

        if (isConnected()) {
            triggerDisconnect();
        } else {
            connectionObservable
                    .flatMapSingle(RxBleConnection::discoverServices)
                    .flatMapSingle(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(BATTERY_CHARACTERISTIC))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(disposable -> Logger.log("Connecting..."))
                    .subscribe(
                            characteristic -> {
                                Log.i(getClass().getSimpleName(), "connection established");
                            },
                            this::onConnectionFailure,
                            this::onConnectionFinished
                    );
        }
    }

    private void onConnectionFailure(Throwable throwable) {
    }

    public void read() {
        if (isConnected()) {
            connectionObservable
                    .firstOrError()
                    .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(BATTERY_CHARACTERISTIC))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bytes -> {
                        int intValue = BytesUtil.bytesToInt(bytes, 1);
                        eventBus.send(new URGReadEvent(intValue));
                        Logger.log("characteristic read | bytes: " + " | hex: " + BytesUtil.bytesToHex(bytes) + " | int: " + intValue);
                    }, this::onReadFailure);
        }
    }

    public void calibrate() {

        if (isConnected()) {
            connectionObservable
                    .firstOrError()
                    .flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(CALIB_CMD_UUID, new byte[]{CALIB_COMMAND_STRAIGHT_CALIB_VALUE}))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> onWriteSuccess(),
                            this::onWriteFailure
                    );
        }
    }

    public void sensor() {
        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(SENSOR_CHARACTERISTIC_UUID))
                    .doOnNext(notificationObservable -> Logger.log("Setting up sensor notification..."))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onSensorReceived, this::onNotificationSetupFailure);
        }
    }

    public void scanAndConnect() {
        if (!bluetoothEnabled())
            return;

        triggerDisconnect();
        setAutoConnectTimer(false);
        bleDevices.clear();
        scanDisposable = null;
        scanTimer.start();
        if(scanDisposable != null)
                scanDisposable.dispose();

        scanDisposable = rxBleClient.scanBleDevices(
                new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build(),
                new ScanFilter.Builder()
                        //.setDeviceAddress("7C:01:0A:38:AB:2A")
                        // add custom filters if needed
                        .build()
        )
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(this::dispose)
                .subscribe(this::scanResult, this::onScanFailure);
    }

    private void dispose() {
    }

    private void onScanFailure(Throwable throwable) {
        Logger.log("onScanFailure: " + throwable);
    }

    private void scanResult(ScanResult scanResult) {
        BluetoothDevice device = scanResult.getBleDevice().getBluetoothDevice();
        Logger.log("scan result: " + scanResult.toString());
        bleDevices.put(device, scanResult.getRssi());
    }

    private void onWriteFailure(Throwable throwable) {
        Logger.log("onWriteFailure: " + throwable);
    }

    private void onWriteSuccess() {
        eventBus.send(new URGWriteEvent());
        Logger.log("onWriteSuccess");
    }

    private void onReadFailure(Throwable throwable) {
        Logger.log("onReadFailure: " + throwable);
    }

    private void onConnectionFinished() {
        Logger.log("onConnectionFinished");
        setAutoConnectTimer(true);
    }

    public URGEventBus bus() {
        return eventBus;
    }

    public boolean isConnected() {
        return bleDevice != null && bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    public RxBleDevice getBleDevice() {
        return bleDevice;
    }

    private Observable<RxBleConnection> prepareConnectionObservable() {
        return bleDevice
                .establishConnection(false)
                .takeUntil(disconnectTriggerSubject)
                .compose(ReplayingShare.instance());
    }

    public void triggerDisconnect() {
        if (disconnectTriggerSubject != null)
            disconnectTriggerSubject.onNext(true);
    }

    private void onSensorReceived(byte[] bytes) {
        int value = BytesUtil.bytesToInt(bytes, 1);
        eventBus.send(new URGSensorEvent(value));
        Logger.log("onSensorReceived: " + value);
    }

    private void onNotificationSetupFailure(Throwable throwable) {
        Logger.log("onNotificationSetupFailure: " + throwable);
    }

    private boolean isScanning() {
        return scanDisposable != null;
    }

    private BluetoothDevice getClosestDevice() {
        Logger.log("looking for closest device...");
        int maxRssi = Integer.MIN_VALUE;
        BluetoothDevice closestDevice = null;

        Set<BluetoothDevice> devices = bleDevices.keySet();
        for (BluetoothDevice device : devices) {
            Integer rssi = bleDevices.get(device);
            String name = device.getName();
            if (rssi > maxRssi && name != null && name.toLowerCase().contains(DEVICE_SEARCH_STRING)) {
                maxRssi = rssi;
                closestDevice = device;
            }
        }

        if (closestDevice != null)
            Logger.log("closest device: " + closestDevice.getAddress());

        return closestDevice;
    }

    private void subscribeBleDevice(String macAddress) {
        bleDevice = rxBleClient.getBleDevice(macAddress);
        connectionObservable = prepareConnectionObservable();
        bleDevice.observeConnectionStateChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnectionStateChange);
    }

    public static boolean bluetoothEnabled() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        return (btAdapter != null && btAdapter.isEnabled());
    }
}
