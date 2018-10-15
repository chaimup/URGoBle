package com.upright.goble.connection.newBle;

import android.content.Context;

import com.upright.goble.connection.Characteristic;
import com.upright.goble.connection.oldBle.URBlePower;

import java.util.UUID;
import static com.upright.goble.connection.URMain.POWER_ERROR_CODE_READ;

public class URBlePowerNew extends URBlePower {

    public static final byte    SHUT_DOWN = 8;
    public static final byte    AIRPLANE_MODE = 9;

    public URBlePowerNew(Context context, Characteristic characteristic) {
        super(context, characteristic);
    }

    public static final UUID POWER_ERROR_CODE = UUID.fromString("0000BAD4-0000-1000-8000-00805f9b34fb");

    public void readBatteryValue()
    {
        characteristic.readCharacteristic(POWER_ERROR_CODE, POWER_ERROR_CODE_READ);
    }

    public void registerOnBatteryValueChange()
    {
        characteristic.setupNotification(POWER_ERROR_CODE, POWER_ERROR_CODE_READ);
    }


    public void airplaneMode()
    {
        characteristic.writeCharacteristic(HALL_CONTROL_UUID,new byte[]{AIRPLANE_MODE});
    }

    public void turnOff() {
        characteristic.writeCharacteristic(HALL_CONTROL_UUID,new byte[]{SHUT_DOWN});
    }


}
