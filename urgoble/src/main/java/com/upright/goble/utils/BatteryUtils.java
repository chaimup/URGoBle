package com.upright.goble.utils;

public class BatteryUtils {
    public static int batteryLevel(int level) {
        switch (level) {
            case 2:
                return 5;

            case 3:
                return 10;

            case 4:
                return 20;

            case 5:
                return 30;

            case 6:
                return 40;

            case 7:
                return 50;

            case 8:
                return 60;

            case 9:
                return 70;

            case 10:
                return 80;

            case 11:
                return 90;

            case 12:
                return 100;

            default:
                return 0;
        }
    }

}
