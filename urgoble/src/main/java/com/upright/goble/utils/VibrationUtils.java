package com.upright.goble.utils;

import com.upright.goble.connection.oldBle.URVibration;

public class VibrationUtils {

    int[] patternGoDeviceArr = {2,1,0,4,3};
    private int patternGoDevice;
    public static final int UPRIGHT_VERSION_1_1_9 = 119;

    public static final int VIBRATION_GENTLE = 125;
    public static final int VIBRATION_MEDIUM = 50;
    public static final int VIBRATION_STRONG = 0;

    public static final int VIBRATION_GENTLE_OLD = 2;
    public static final int VIBRATION_MEDIUM_OLD = 1;
    public static final int VIBRATION_STRONG_OLD = 0;

    public static final int VIBRATION_GENTLE_ID = 2;
    public static final int VIBRATION_MEDIUM_ID = 1;
    public static final int VIBRATION_STRONG_ID = 0;

    URVibration urVibration;

    public VibrationUtils(URVibration urVibration) {
        this.urVibration = urVibration;
    }

    public void prepareParameters(int pattern, int strengthId){
        int newPattern = getCurrentPattern(pattern);
        int strength = getVibrationStrengthByDeviceVersion(strengthId);

        urVibration.onVibratePatternChange(newPattern, strength);
    }

    public int getVibrationStrengthByDeviceVersion(int strengthId) {
//        boolean newVersion = Integer.valueOf(currentVersion) >= UPRIGHT_VERSION_1_1_9;
        boolean newVersion = true;
        int strength = strengthId == VIBRATION_GENTLE_ID ? (newVersion ? VIBRATION_GENTLE : VIBRATION_GENTLE_OLD) :
                strengthId == VIBRATION_MEDIUM_ID ? (newVersion ? VIBRATION_MEDIUM : VIBRATION_MEDIUM_OLD) :
                        strengthId == VIBRATION_STRONG_ID ? (newVersion ? VIBRATION_STRONG : VIBRATION_STRONG_OLD) : strengthId;
        return strength;
    }

    private int getCurrentPattern(int pattern){
        return patternGoDeviceArr[pattern];
    }

}
