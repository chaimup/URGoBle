package com.upright.goble.connection.oldBle;

import android.content.Context;

import com.upright.goble.connection.Characteristic;

import java.util.UUID;

import static com.upright.goble.connection.URMain.BATTERY_VALUE_READ;
import static com.upright.goble.connection.URMain.CHARGING_STATUS_READ;
import static java.util.UUID.fromString;

public class URBlePower{

    public URGConnection urgConnection;
    public Characteristic characteristic;

    public URBlePower(Context context, Characteristic characteristic) {
        urgConnection = URGConnection.init(context);
        this.characteristic = characteristic;
    }

    public static final UUID HALL_CONTROL_UUID = fromString("0000aaa4-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_CHARACTERISTIC = UUID.fromString("0000AAA1-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARGING_STATUS_UUID = UUID.fromString("0000aaa2-0000-1000-8000-00805f9b34fb");

    public static final byte    GO_SLEEP = 7;
    public static final byte    NO_TRANSMISSION = 8;

    public void turnOff() {
        characteristic.writeCharacteristic(HALL_CONTROL_UUID,new byte[]{GO_SLEEP});
    }

    public void readBatteryValue()
    {
        characteristic.readCharacteristic(BATTERY_CHARACTERISTIC, BATTERY_VALUE_READ);
    }

    public void registerOnBatteryValueChange()
    {
        characteristic.setupNotification(BATTERY_CHARACTERISTIC, BATTERY_VALUE_READ);
    }

    public void readChargingStatus()
    {
        characteristic.readCharacteristic(CHARGING_STATUS_UUID, CHARGING_STATUS_READ);
    }

    public void registerChargingStatus() {
        characteristic.setupNotification(CHARGING_STATUS_UUID, CHARGING_STATUS_READ);
    }

    public void airplaneMode()
    {
        characteristic.writeCharacteristic(HALL_CONTROL_UUID,new byte[]{NO_TRANSMISSION});
    }



}
