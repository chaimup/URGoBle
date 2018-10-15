package com.upright.goble.connection.oldBle;

import android.content.Context;
import android.util.Log;

import com.upright.goble.utils.Logger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;

import static com.upright.goble.utils.BytesUtil.bytesToInt;

public class URBleFirmUpdate {

    private static final String TAG = "200a.URBleFirmUpdate";
    long mStartTime = 0;
    boolean mIsWaitForReconnection = false;
    URGConnection urgConnection;


    public URBleFirmUpdate(Context context) {
        urgConnection = URGConnection.init(context);
    }

    static public final UUID
            FIRMMWAREUPLOAD_UPGRADE_UUID = UUID.fromString("0000aaa4-0000-1000-8000-00805f9b34fb"),
            FIRMMWAREUPLOAD_UUID_HEADER_UUID = UUID.fromString("f000ffc1-0451-4000-b000-000000000000"),
            FIRMMWAREUPLOAD_UUID_BLOCK_UUID = UUID.fromString("f000ffc2-0451-4000-b000-000000000000");

    static public final String FIRMWARE_UPDATE_VERSION__A_STRING = "firmware revision";

    static public final short FIRMWAREUPDATE_UPDATE_ENABLE = 1,
            FIRMWAREUPDATE_UPDATE_NORMAL = 0;

    static public final int FIRMWAREUPLOAD_BLOCK_SIZE = 16,
    // ERRORS
            FIRMWAREUPDATE_ERROR_INVALID_INDEX_REQUEST_BY_DEVICE = -410;

    static public final int FIRMWARE_TIMEOUT_MSEC = 60000; // 1 minute

    byte[] mBinAdxlData,
            mBinBmaData, mFinalBinData;
    int mCurrentBlockIndex = 0;

    Timer mTimer = new Timer();
    TimerTask mTimerTask = null;
    String mDeviceVersion = null;

    public static boolean isFirmwareMode(String version) {
        boolean isFirmwareMode = false;

        if (version != null &&
                ((version.charAt(0) == 'a' || version.charAt(0) == 'A') ||
                        (version.toLowerCase().equals(FIRMWARE_UPDATE_VERSION__A_STRING) == true))) {
            isFirmwareMode = true;
        }

        return isFirmwareMode;
    }

    boolean veirfyBinary(byte[] binFirmware) {
        return binFirmware != null &&
                binFirmware.length > FIRMWAREUPLOAD_BLOCK_SIZE &&
                (binFirmware.length % FIRMWAREUPLOAD_BLOCK_SIZE) == 0;
    }

    private void uploadPh2() {

        boolean isFirmwareMode = isFirmwareMode(mDeviceVersion);

        if (isFirmwareMode == false) {
            // STEP 1: entering the device into firmware update mode. sending 01 to 0xAAA4.

            if (urgConnection.isConnected()) {
                urgConnection.getConnectionObservable()
                        .firstOrError()
                        .flatMap(bleConnection -> bleConnection.writeCharacteristic(FIRMMWAREUPLOAD_UPGRADE_UUID, new byte[]{FIRMWAREUPDATE_UPDATE_ENABLE}))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                bytes -> uploadSuccess(),
                                urgConnection::onWriteFailure
                        );
            }
        }
        else {
            if (mFinalBinData != null) {
                mStartTime = System.currentTimeMillis();
                onUploadBegins();
                beginUploadingFlow();
            }
        }
    }

    private void uploadSuccess() {
        mStartTime = System.currentTimeMillis();
        mIsWaitForReconnection = true;
        onDeviceRestartedWithVersionA();
    }

    void beginUploadingFlow() {

        // STEP 2: registering to 0xFFC1 for monitoring refusal.

        if (urgConnection.isConnected()) {
            urgConnection.getConnectionObservable()
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(FIRMMWAREUPLOAD_UUID_HEADER_UUID))
                    .doOnNext(notificationObservable -> Logger.log("FIRMMWAREUPLOAD_UUID_HEADER_UUID..."))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onRegistrationToHeaderChar, this::onReadFailure);
        }

    }

    private void onReadFailure(Throwable throwable) {

    }

    private void onRegistrationToHeaderChar(byte[] bytes) {
        // STEP 3: registering to 0xFFC2 for retrieving requested index.

        urgConnection.getConnectionObservable()
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(FIRMMWAREUPLOAD_UUID_BLOCK_UUID))
                .doOnNext(notificationObservable -> Logger.log("FIRMMWAREUPLOAD_UUID_BLOCK_UUID..."))
                .flatMap(notificationObservable -> notificationObservable)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onRegistrationToHeaderCharCompleted, this::onReadFailure);
    }

    private void onRegistrationToHeaderCharCompleted(byte[] bytes) {
        onDeviceBlockRequest(bytes);
        sendHeader();
    }

    protected void sendHeader() {

        // STEP 4: Sending 16 bytes of HEADER to 0xFFC2

        if (urgConnection.isConnected()) {
            urgConnection.getConnectionObservable()
                    .firstOrError()
                    .flatMap(bleConnection -> bleConnection.writeCharacteristic(FIRMMWAREUPLOAD_UUID_HEADER_UUID, mFinalBinData))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> sendHeaderSuccess(),
                            urgConnection::onWriteFailure
                    );
        }
    }

    private void sendHeaderSuccess() {
    }

    protected void onDeviceBlockRequest(byte[] characteristic) {
        byte[] requestedIndexBytes = characteristic;
        mCurrentBlockIndex = bytesToInt(requestedIndexBytes, 0);
        sendCurrentBlock();
    }

    protected void sendCurrentBlock() {


        final int totalBlocks = mFinalBinData.length / FIRMWAREUPLOAD_BLOCK_SIZE; // NOTE: [ not +1 !]: Not including header while binFirmware.length includes header!


        // STEP 5: UPLOADING DATA...
        byte[] blockToSend = new byte[FIRMWAREUPLOAD_BLOCK_SIZE + 2]; // 2 = bytes of index
        blockToSend[0] = (byte) (mCurrentBlockIndex % 0x100);  // Index 8 bit LSB
        blockToSend[1] = (byte) (mCurrentBlockIndex / 0x100);  // Index 8 bit MSB

        int bytesCount = FIRMWAREUPLOAD_BLOCK_SIZE;
        int bytesLeft = mFinalBinData.length - (mCurrentBlockIndex * FIRMWAREUPLOAD_BLOCK_SIZE);

        if (bytesLeft < 0) {
            // Device has requested invalid index number !
            onError(FIRMWAREUPDATE_ERROR_INVALID_INDEX_REQUEST_BY_DEVICE, "Invalid index request by device");
            return;
        }

        if (bytesLeft < bytesCount) {
            bytesCount = bytesLeft;
        }


        for (int i = 0; i < bytesCount; i++) {
            blockToSend[i + 2] = mFinalBinData[mCurrentBlockIndex * FIRMWAREUPLOAD_BLOCK_SIZE + i];
        }

        printCharacteristic("sendCurrentBlock (blockToSend): ", blockToSend);

        if (urgConnection.isConnected()) {
            urgConnection.getConnectionObservable()
                    .firstOrError()
                    .flatMap(bleConnection -> bleConnection.writeCharacteristic(FIRMMWAREUPLOAD_UUID_BLOCK_UUID, blockToSend))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> onBlockSent(totalBlocks),
                            urgConnection::onWriteFailure
                    );
        }
    }

    private void onBlockSent(int totalBlocks) {
        onUploading(mCurrentBlockIndex + 1, totalBlocks);

        if (mCurrentBlockIndex == totalBlocks - 1) {
            onCompleted();
        }

    }


    private static void printCharacteristic(String tag, byte[] arr) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
//            sb.append("-");
        }
        Log.d(TAG, tag + " " + sb);
    }

    public void close(boolean isDisconnect) {
        stopTimer();

        // STEP 2: registering to 0xFFC1 for monitoring refusal.
    }

    public void stopTimer() {
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }

    void onUploadBegins() {

    }

    void onError(int code, String description) {

        close(true);
        stopTimer();
    }


    /**
     * @param index
     * @param totalBlocks
     */
    void onUploading(int index, int totalBlocks) {
    }

    /**
     *
     */
    void onCompleted() {

        close(false);

        stopTimer();
    }

    /**
     *
     */
    void onDeviceRestartedWithVersionA() {
    }





}
