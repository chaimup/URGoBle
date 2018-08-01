package com.upright.goble.go;

import android.content.Context;
import android.util.Log;

import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.upright.goble.connection.URGConnection;
import com.upright.goble.events.URGConnEvent;
import com.upright.goble.gatt.URGatt;
import com.upright.goble.utils.Logger;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

public class URGoDevice {

    URGConnection connection;
    URGatt gatt;

    private RxBleDevice device;

    public URGoDevice(Context context, String macAddrss) {
        //connection = new URGConnection(context, macAddrss);
        connection = new URGConnection(context);
        subscribeEvents();
    }

    private Observable<RxBleConnection> connectionObservable;


    private boolean isConnected() {
        return  connection.isConnected();
    }

    public URGConnection connection() {
        return connection;
    }

    public void connect() {
        connection.scanAndConnect();
    }

    private void subscribeEvents() {
        connection
                .bus()
                .toObservable()
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object object) throws Exception {
                        if (object instanceof URGConnEvent) {
                            Logger.log( "URGoDevice: Connection Event Received: " + ((URGConnEvent) object).getState());
                            gatt = new URGatt(connection);
                            gatt.read();
                        }
                    }
                });
    }
}
