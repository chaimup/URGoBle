package com.upright.goble.sample;

import android.content.Context;
import android.util.Log;

import com.upright.goble.events.URGConnEvent;
import com.upright.goble.go.URGoDevice;

import io.reactivex.functions.Consumer;


public class URConnection {
    String MAC_ADDRESS = "98:7B:F3:5B:F9:BC";

    Context context;
    String mackAddress;
    URGoDevice device;

    private static final String TAG = "URConnection";

    URConnection(Context context, String mackAddress) {

        this.context = context;
        this.mackAddress = mackAddress;

        device = new URGoDevice(context, MAC_ADDRESS);
        subscribeEvents();
    }

    public void connect() {
        device.connect();
    }

    private void subscribeEvents() {
        device.connection()
              .bus()
              .toObservable()
              .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object object) throws Exception {
                        if (object instanceof URGConnEvent) {
                            Log.i(TAG, "App: Connection Event Received: " + ((URGConnEvent) object).getState());
                        }
                    }
                });
    }
}
