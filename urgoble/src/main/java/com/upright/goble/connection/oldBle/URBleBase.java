package com.upright.goble.connection.oldBle;

import android.content.Context;

import com.upright.goble.connection.Characteristic;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import static com.upright.goble.connection.URMain.FIRMWARE_UPDATE_INFO_READ;
import static com.upright.goble.connection.URMain.HARDWARE_REVISION_READ;
import static com.upright.goble.connection.URMain.SYSTEM_INFO_READ;

public class URBleBase {

    public URGConnection urgConnection;
    public Characteristic characteristic;

    public static final UUID FIRMWAREUPDATE_INFO_VERSION_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID HARDWARE_REVISION_UUID = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
    public static final UUID SYSTEM_INFO_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID TRACK_TRAIN_UUID = UUID.fromString("0000aac7-0000-1000-8000-00805f9b34fb");;

    public URBleBase(Context context, Characteristic characteristic) {
        urgConnection = URGConnection.init(context);
        this.characteristic = characteristic;
    }


    public void onSwitchTrainingTracking(boolean autoSwitch)
    {
        characteristic.writeCharacteristic(TRACK_TRAIN_UUID,new byte[]{(byte) (autoSwitch ? 1 : 0)});
    }

    public  void readDeviceVersion() {
        characteristic.readCharacteristic(FIRMWAREUPDATE_INFO_VERSION_UUID, FIRMWARE_UPDATE_INFO_READ);
    }

    public  void readHardwareRevision()
    {
        characteristic.readCharacteristic(HARDWARE_REVISION_UUID, HARDWARE_REVISION_READ);
    }

    public void readSystemInfo()
    {
        characteristic.readCharacteristic(SYSTEM_INFO_UUID, SYSTEM_INFO_READ);
    }

    /**
     * writeHardwareRevision - write hardware version
     * using after update for special vibration version V0_B1 in new device.
     */
    public  void writeHardwareRevision()
    {
        byte[] hardwareRevision = new byte[0];
//        URDataManager dbManager = new URDataManager(getConnection().getContext());
        String hwRevision = "V0_B1"; //dbManager.getHWRevision();

        try {
            hardwareRevision = hwRevision.getBytes("UTF-8"); //transfer hardware revision string to byte array
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte[] finalHardwareRevision = hardwareRevision;
        characteristic.writeCharacteristic(HARDWARE_REVISION_UUID,finalHardwareRevision);

    }


}
