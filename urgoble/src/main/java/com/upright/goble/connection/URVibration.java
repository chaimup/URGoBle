package com.upright.goble.connection;

import android.content.Context;

import com.upright.goble.utils.BytesUtil;

import java.util.UUID;


import static com.upright.goble.connection.URMain.AUTO_SWITCH_TRACKING;
import static com.upright.goble.connection.URMain.DELAY_READ;
import static com.upright.goble.connection.URMain.PATTERN_READ;

public class URVibration{
    URGConnection urgConnection;

    public static final UUID VIBRATE_DELAY_CHARACTERISTIC_UUID = UUID.fromString("0000aac2-0000-1000-8000-00805f9b34fb");
    public static final UUID VIBRATE_PERSONALIZATION_CHARACTERISTIC_UUID = UUID.fromString("0000aaa5-0000-1000-8000-00805f9b34fb");
    public static final UUID TOGGLE_AUTO_TRACK_SWITCH_CHARACTERISTIC_UUID = UUID.fromString("0000aac8-0000-1000-8000-00805f9b34fb");

    Characteristic characteristic;

    public URVibration(Context context, Characteristic characteristic) {
        urgConnection = URGConnection.init(context);
        this.characteristic = characteristic;
    }

    public void vibrationDelayAndPeriods(final int delay, final int repeating) {
        if (delay < 26) {
            byte new_delay = (byte) (delay * 10);
            byte[] byte_delivery_array = {new_delay, 0, (byte) repeating, 30};
            characteristic.writeCharacteristic(VIBRATE_DELAY_CHARACTERISTIC_UUID,byte_delivery_array);
        } else {
            byte new_delay = 0x04;

            switch (delay) {
                case 26:
                    new_delay = 0x04;
                    break;
                case 27:
                    new_delay = 0x0E;
                    break;
                case 28:
                    new_delay = 0x18;
                    break;
                case 29:
                    new_delay = 0x22;
                    break;
                case 30:
                    new_delay = 0x2C;
                    break;
            }

            byte[] byte_delivery_array = {new_delay, 1, (byte) repeating, 30};
            characteristic.writeCharacteristic(VIBRATE_DELAY_CHARACTERISTIC_UUID,byte_delivery_array);
        }
    }

    public void onVibratePatternChange(int pattern, int strength)
    {
        byte new_pattern = (byte) pattern;
        byte current_strength = (byte)strength;
        byte[] byte_delivary_array = {new_pattern,current_strength,(byte) 1};

        characteristic.writeCharacteristic(VIBRATE_PERSONALIZATION_CHARACTERISTIC_UUID,byte_delivary_array);

    }

    public void onAutoSwitchTracking(boolean autoSwitch)
    {
        characteristic.writeCharacteristic(TOGGLE_AUTO_TRACK_SWITCH_CHARACTERISTIC_UUID,new byte[]{(byte) (autoSwitch ? 1 : 0)});
    }

    public void readIsAutoSwitchTracking()
    {
        characteristic.setupNotification(TOGGLE_AUTO_TRACK_SWITCH_CHARACTERISTIC_UUID, AUTO_SWITCH_TRACKING);
    }

    public void readVibrationPattern()
    {
        characteristic.readCharacteristic(VIBRATE_PERSONALIZATION_CHARACTERISTIC_UUID, PATTERN_READ);
    }

    public void readVibrationDelay()
    {
        characteristic.readCharacteristic(VIBRATE_DELAY_CHARACTERISTIC_UUID, DELAY_READ);

    }

    private void onAutoSwitchTrackingSuccess() {
    }

    private void onAutoSwitchTrackingReadSuccess(byte[] bytes) {
        if (bytes != null) {
            int isToggle = BytesUtil.bytesToInt(bytes, 1);

            boolean allowRestingBreaks;
            if (isToggle == 0)
                allowRestingBreaks = true;
            else
                allowRestingBreaks = false;
        }
    }

}
