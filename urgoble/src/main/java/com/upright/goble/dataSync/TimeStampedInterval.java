package com.upright.goble.dataSync;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class TimeStampedInterval {
    public static final byte DIRTY = 127, CLEAN = 15;
    public static final int STANDALONE = 0, ONLINE = 1;
    int                 mSyncChannel;
    int                 mFlag;
    byte[]              mTimeStamp;
    byte[]              mAngle;
    List<UsageInterval> mIntervals;

    public TimeStampedInterval(int flag, byte[] timeStamp, byte[] angle, int sync)
    {
        mFlag = flag;
        mSyncChannel = sync;
        mTimeStamp = new byte[4];

        for (int i = 0 ; i < 4 ; i++)
            mTimeStamp[i] = timeStamp [i];

        mAngle = new byte[2];

        for (int i = 0; i < 2 ; i++)
            mAngle[i] = angle[i];

        mIntervals = new LinkedList<>();
    }

    public int getSyncChannel()
    {
        return  mSyncChannel;
    }
    public int getTimeStamp()
    {
        return ByteBuffer.wrap(mTimeStamp).getInt();
    }

    public int getStraightAngle()
    {
        return ByteBuffer.wrap(mAngle).getShort();
    }

    public boolean isTimeStampValid()
    {
        return (mFlag == CLEAN);
    }

    public List<UsageInterval> getIntervalsData()
    {
        return mIntervals;
    }

    public void addInterval(UsageInterval currInterval)
    {
        mIntervals.add(currInterval);
    }
}
