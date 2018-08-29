package com.upright.goble.dataSync;

public class UsageInterval {
    public static final byte STRAIGHT_DATA = 0 , TRAIN_DATA = 0;

    int mScore_data;
    int mMode_data;
    int mMove_data;
    int mVibs_data;

    public  UsageInterval (int score_data, int mode_data, int move_data, int vibs_data)
    {
        mScore_data = score_data;
        mMode_data = mode_data;
        mMove_data = move_data;
        mVibs_data = vibs_data;
    }

    public String getScore()
    {
        if (mScore_data == STRAIGHT_DATA) return "straight"; else return "slouch";
    }

    public String getMode()
    {
        if (mMode_data == TRAIN_DATA) return "train"; else return "track";
    }

    public int getVibsNumber()
    {
        return mVibs_data;
    }

    public int getMove()
    {
        return mMode_data;
    }
}
