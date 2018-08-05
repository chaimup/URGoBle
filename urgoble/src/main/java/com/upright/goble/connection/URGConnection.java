package com.upright.goble.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.jakewharton.rx.ReplayingShare;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;
import com.upright.goble.events.URGCalibEvent;
import com.upright.goble.events.URGConnEvent;
import com.upright.goble.events.URGEventBus;
import com.upright.goble.events.URGScanEvent;
import com.upright.goble.events.URGSensorEvent;
import com.upright.goble.events.URGWriteEvent;
import com.upright.goble.utils.BytesUtil;
import com.upright.goble.utils.Logger;

import java.nio.ByteBuffer;
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
    Handler handler = new Handler();

    // Demo/POC characteristics
    UUID BATTERY_CHARACTERISTIC = UUID.fromString("0000AAA1-0000-1000-8000-00805f9b34fb");
    UUID CALIB_CMD_UUID = UUID.fromString("0000aab1-0000-1000-8000-00805f9b34fb");
    byte CALIB_COMMAND_STRAIGHT_CALIB_VALUE = 1;
    UUID SENSOR_CHARACTERISTIC_UUID = UUID.fromString("0000aaca-0000-1000-8000-00805f9b34fb");
    UUID CALIB_CHARACTERISTIC_UUID = UUID.fromString("0000aab3-0000-1000-8000-00805f9b34fb");
    UUID CALIB_ACK_UUID = UUID.fromString("0000aab2-0000-1000-8000-00805f9b34fb");

    int mStraightAngle;
    int mSlouchAngle;
    int mStraightRatio = 5;
    int mSlouchRatio = 5;

    static int numberOfStraightFrames = 40;
    int numberOfSlouchFrames = 10;
    static int lastStraightAcc;
    static int lastSlouchAcc;

    private static final String DEVICE_SEARCH_STRING = "upright";
    Hashtable<BluetoothDevice, Integer> bleDevices = new Hashtable<BluetoothDevice, Integer>();

    public URGConnection(Context context) {
        rxBleClient = RxBleClient.create(context);
        eventBus = URGEventBus.bus();
        startAutoConnect();
        setScanTimer();
    }

    private Runnable scanTimerRunnable = new Runnable() {
        @Override
        public void run() {
            Logger.log("scan finished");
            if(scanDisposable != null)
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

    public void startAutoConnect() {
        stopAutoConnect();
        autoConnectRunnable.run();
    }

    private void stopAutoConnect() {
        handler.removeCallbacks(autoConnectRunnable);
    }

    private Runnable autoConnectRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (isConnected())
                    Logger.log("auto connect:  already connected");
                else if (bleDevice != null && bleDevice.getMacAddress().length() > 0) {
                    Logger.log("auto connect: trying to connect...");
                    bleConnect();
                }
            } finally {
                handler.postDelayed(autoConnectRunnable, 2000);
            }
        }
    };


    private void setScanTimer() {
        handler.postDelayed(scanTimerRunnable, 3000);
    }

    private <RxBleConnectionState> void onConnectionStateChange(RxBleConnection.RxBleConnectionState state) {
        Logger.log("onConnectionStateChange: " + state.toString());
        eventBus.send(new URGConnEvent(state));
        if(state == RxBleConnection.RxBleConnectionState.DISCONNECTED )
            startAutoConnect();
        else if(state == RxBleConnection.RxBleConnectionState.CONNECTED)
            stopAutoConnect();
    }

    public void bleConnect() {
        if (!bluetoothEnabled())
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
                                registerCalibrationDoneNotification();
                            },
                            this::onConnectionFailure,
                            this::onConnectionFinished
                    );
        }
    }

    private void onConnectionFailure(Throwable throwable) {
    }

    public void readCalibration() {
        if (isConnected()) {
            connectionObservable
                    .firstOrError()
                    .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(CALIB_CHARACTERISTIC_UUID))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bytes -> {
                        readCalibrationAngles(bytes);
                        registerSensorNotifications();
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

    public void registerSensorNotifications() {
        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(SENSOR_CHARACTERISTIC_UUID))
                    .doOnNext(notificationObservable -> Logger.log("Setting up sensor notification..."))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onSensorReceived, this::onNotificationSetupFailure);
        }
    }

    public void registerCalibrationDoneNotification() {
        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(CALIB_ACK_UUID))
                    .doOnNext(notificationObservable -> Logger.log("Setting up sensor notification..."))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onCalibAckReceived, this::onNotificationSetupFailure);
        }
    }

    private void onCalibAckReceived(byte[] bytes) {
        Logger.log("onCalibAckReceived: " + BytesUtil.bytesToInt(bytes, 1));
        int state = BytesUtil.bytesToInt(bytes, 1);

        URGCalibEvent event = new URGCalibEvent(state);
        eventBus.send(event);
        if(event.getState() == URGCalibEvent.CALIB_FINISHED) {
            readCalibration();
        }
    }

    public void scanAndConnect() {
        if (!bluetoothEnabled())
            return;

        triggerDisconnect();
        stopAutoConnect();
        bleDevices.clear();
        scanDisposable = null;
        setScanTimer();
        if (scanDisposable != null)
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
        // try to read correctly from bytes instead of reading sensor value again..

//        int angle = BytesUtil.bytesToInt(bytes, 2);
//        sendSensorDisplayAngle(angle);
        connectionObservable
                .flatMapSingle(RxBleConnection::discoverServices)
                .flatMapSingle(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(SENSOR_CHARACTERISTIC_UUID))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        characteristic -> {
                            onSensorReceivedChar(characteristic);
                        },
                        this::onReadSensorFailure,
                        this::onReadSensorFinished
                );
    }

    private void onReadSensorFinished() {
    }

    private void onReadSensorFailure(Throwable throwable) {
    }

    private void onSensorReceivedChar(BluetoothGattCharacteristic characteristic) {
        int angle = (characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0));
        sendSensorDisplayAngle(angle);
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

    private void sendSensorDisplayAngle(int angle) {
        // mStraightAngle = 81;
        if (angle < mSlouchAngle && ((angle - mStraightAngle) / mStraightRatio) <= numberOfStraightFrames) {
            int angelRange = (angle - mStraightAngle) / mStraightRatio;
            Log.i("vy2110", "straight: " + mStraightAngle + " : ration: " + mStraightRatio + " : angle: " + angle + " : sned: " + angelRange);

            int sedValue = (angelRange > 0 && angelRange < 50) ? angelRange : 0;
            eventBus.send(new URGSensorEvent(sedValue, angle));
        } else {
            if (angle >= mSlouchAngle && angle < 900) {
                int slouchFrame = numberOfStraightFrames + (angle - mSlouchAngle) / mSlouchRatio;
                if (slouchFrame >= 50)
                    slouchFrame = 49;
                eventBus.send(new URGSensorEvent(slouchFrame, angle));
            }
        }
    }

    public void readCalibrationAngles(byte[] data) {

        byte[] straightData = {data[0], data[1]};
        byte[] slouchData = {data[2], data[3]};

        mStraightAngle = (ByteBuffer.wrap(straightData).getShort());
        mSlouchAngle = (ByteBuffer.wrap(slouchData).getShort());

        Logger.log("mStraightAngle: " + lastStraightAcc + " | mSlouchAngle: " + mSlouchAngle);

        lastStraightAcc = mStraightAngle / 10;
        lastSlouchAcc = mSlouchAngle / 10;

        int upperSlouchLimit = mStraightAngle + 500;

        if (mSlouchAngle < upperSlouchLimit)
            numberOfStraightFrames = 10 + (2) * (mSlouchAngle / 10 - mStraightAngle / 10);
        else if (mSlouchAngle < 0) {
            numberOfStraightFrames = numberOfStraightFrames;
        }

        if (numberOfStraightFrames >= 50)
            numberOfStraightFrames = 40;

        numberOfSlouchFrames = 50 - numberOfStraightFrames;

        if (mSlouchAngle > 0 &&
                mSlouchAngle - mStraightAngle > 0 &&
                !((mSlouchAngle - mStraightAngle) / numberOfStraightFrames == 0)) {
            mStraightRatio = (mSlouchAngle - mStraightAngle) / numberOfStraightFrames;
        } else
            mStraightRatio = 1;

        if (mSlouchAngle < upperSlouchLimit && !((upperSlouchLimit - mSlouchAngle) / numberOfSlouchFrames == 0)) {
            mSlouchRatio = (upperSlouchLimit - mSlouchAngle) / numberOfSlouchFrames;

        } else
            mSlouchRatio = 1;
    }
}

