package com.upright.goble.connection.oldBle;

import android.content.Context;

import com.upright.goble.connection.Characteristic;

import java.util.UUID;

import static com.upright.goble.connection.URMain.DATA_ONLINE_NOTIFICATION;
import static com.upright.goble.connection.URMain.DATA_SYNC_NOTIFICATION;
import static com.upright.goble.connection.URMain.DATA_SYNC_READ;

public  class URBleDataSync {

    /*Data sync components
 [1 byte]- 15 timestamp reliable, 127 not, [4 bytes]- for timestamp, uint32, [2 bytes]-for angle, sinit16
 [training data, divided to minutes for each interval], [255] - no more data to read

 The training data components:
 1bit[posture score]-0 if straight and 1 if slouch, 1bit[use mode]-0 if training and 1 if tracking
 2bits[movement]-value between 0-3,4bits[numOfVibrations]- value between 0 and 14*/

    URGConnection urgConnection;
    Characteristic characteristic;

    public static final UUID  DATA_SYNC_ACK_UUID = UUID.fromString("0000aae3-0000-1000-8000-00805f9b34fb");
    public static final UUID  DATA_SYNC_TIMESTAMP_UUID = UUID.fromString("0000aac5-0000-1000-8000-00805f9b34fb");
    public static final UUID DATA_SYNC_READ_UUID = UUID.fromString("0000aae2-0000-1000-8000-00805f9b34fb");
    public static final UUID DATA_ONLINE_ACK_UUID = UUID.fromString("0000aac9-0000-1000-8000-00805f9b34fb");

    public static final  byte           SEND_DATA_CMD = 1,MEMORY_RESET_CMD = 2;


    public URBleDataSync(Context context, Characteristic characteristic) {
        urgConnection = URGConnection.init(context);
        this.characteristic = characteristic;
    }

    public void registerToDataNotification() {
        readCurrentTimeStamp();
        characteristic.setupNotification(DATA_SYNC_ACK_UUID, DATA_SYNC_NOTIFICATION);
    }

    public void readTrainingData()
    {
        characteristic.writeCharacteristic(DATA_SYNC_READ_UUID,new byte[]{SEND_DATA_CMD});
    }

    public void registerOnlineData()
    {
        characteristic.setupNotification(DATA_ONLINE_ACK_UUID, DATA_ONLINE_NOTIFICATION);
    }

    public void readCurrentTimeStamp()
    {
        characteristic.readCharacteristic(DATA_SYNC_TIMESTAMP_UUID, DATA_SYNC_READ);
    }


}
