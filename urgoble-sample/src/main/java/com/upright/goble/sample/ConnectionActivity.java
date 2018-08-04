package com.upright.goble.sample;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;
import com.upright.goble.connection.URGConnection;
import com.upright.goble.events.URGConnEvent;
import com.upright.goble.events.URGReadEvent;
import com.upright.goble.events.URGScanEvent;
import com.upright.goble.events.URGSensorEvent;
import com.upright.goble.events.URGWriteEvent;
import com.upright.goble.utils.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.functions.Consumer;


public class ConnectionActivity extends RxAppCompatActivity {

    private Integer images_green[] = {R.drawable.green_avi1, R.drawable.green_avi2, R.drawable.green_avi3, R.drawable.green_avi4, R.drawable.green_avi5, R.drawable.green_avi6, R.drawable.green_avi7,
            R.drawable.green_avi8, R.drawable.green_avi9, R.drawable.green_avi10, R.drawable.green_avi11, R.drawable.green_avi12, R.drawable.green_avi13, R.drawable.green_avi14, R.drawable.green_avi15, R.drawable.green_avi16,
            R.drawable.green_avi17, R.drawable.green_avi18, R.drawable.green_avi19, R.drawable.green_avi20, R.drawable.green_avi21, R.drawable.green_avi22, R.drawable.green_avi23, R.drawable.green_avi24, R.drawable.green_avi25,
            R.drawable.green_avi26, R.drawable.green_avi27, R.drawable.green_avi28, R.drawable.green_avi29, R.drawable.green_avi30, R.drawable.green_avi31, R.drawable.green_avi32, R.drawable.green_avi33, R.drawable.green_avi34,
            R.drawable.green_avi35, R.drawable.green_avi36, R.drawable.green_avi37, R.drawable.green_avi38, R.drawable.green_avi39, R.drawable.green_avi40, R.drawable.green_avi41, R.drawable.green_avi42, R.drawable.green_avi43,
            R.drawable.green_avi44, R.drawable.green_avi45, R.drawable.green_avi46, R.drawable.green_avi47, R.drawable.green_avi48, R.drawable.green_avi49, R.drawable.green_avi50};

    final static int BLUETOOTH_ENABLE_REQUEST_ID = 100;

    URGConnection connection;
    @BindView(R.id.connect) Button connectButton;
    @BindView(R.id.read) Button readButton;
    @BindView(R.id.calibrate) Button calibrateButton;
    @BindView(R.id.sensor) Button sensorButton;
    @BindView(R.id.connectStatus) ImageView connectImage;
    @BindView(R.id.sensorMan) ImageView sensorMan;
    @BindView(R.id.sensorData) TextView sensorData;
    @BindView(R.id.readData) TextView readData;
    @BindView(R.id.calibrateData) TextView calibrateData;
    @BindView(R.id.scanData) TextView scanData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        ButterKnife.bind(this);
        enableDeviceButtons(false);
        connection = new URGConnection(this);
        subscribeEvents();
    }

    @OnClick(R.id.connect)
    public void onConnectClick() {
        clearUiData();
        scanData.setText("Scanning...");
        handleConnect();
    }

    private void handleConnect() {
        connection.triggerDisconnect();
        enableBluetoothAndLocation();
    }

    @OnClick(R.id.read)
    public void onReadClick() {
        connection.read();
    }

    @OnClick(R.id.calibrate)
    public void onCalibrateClick() {
        connection.calibrate();
    }

    @OnClick(R.id.sensor)
    public void onSensorClick() {
        connection.sensor();
    }

    private void subscribeEvents() {
        connection
                .bus()
                .toObservable()
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object event) throws Exception {
                        handleEvent(event);
                    }
                });
    }

    private void handleEvent(Object event) {
        if (event instanceof URGConnEvent) {
            handleConnectionUi(((URGConnEvent) event).getState());
            Logger.log("App: Connection Event Received: " + ((URGConnEvent) event).getState());
        } else if (event instanceof URGReadEvent) {
            handleReadUi(((URGReadEvent) event).getValue());
        } else if (event instanceof URGWriteEvent) {
            handleWriteUi();
        } else if (event instanceof URGSensorEvent) {
            handleSensorUi(((URGSensorEvent) event).getIndexAngle(),((URGSensorEvent) event).getSensorAngle());
        } else if (event instanceof URGScanEvent) {
            handleScanUi(((URGScanEvent) event).getMacAddress());
        }
    }

    private void handleConnectionUi(URGConnEvent.State state) {
        switch(state) {
            case Connected:
                connectImage.setBackgroundColor(ContextCompat.getColor(this, R.color.colorGreenLight));
                enableDeviceButtons(true);
                break;

            case Disconnected:
                connectImage.setBackgroundColor(ContextCompat.getColor(this, R.color.colorBlueLight));
                clearUiData();
                enableDeviceButtons(false);
                break;

            default:
        }
    }

    private void handleReadUi(int value) {
        readData.setText(Integer.toString(value));
    }

    private void handleWriteUi() {
        calibrateData.setText("ok");
    }

    private void handleScanUi(String address) {
        scanData.setText(address == null ? "GO not found" : address);
    }

    Runnable update = new Runnable() {
        @Override
        public void run() {
            sensorMan.setImageResource(images_green[indexAngle]);
            sensorData.setText(Integer.toString(indexAngle));
        }

    };

    private void handleSensorUi(int indexAngle, int sensorAngle) {
        Logger.log("handleSensorUi: " + sensorAngle + " | " + indexAngle);
        sensorMan.setImageResource(images_green[indexAngle]);
        sensorData.setText(Integer.toString(indexAngle));
        runOnUiThread(update);
    }

    private void clearUiData() {
         readData.setText("");
         calibrateData.setText("");
         sensorData.setText("");
    }

    private void enableDeviceButtons(boolean enable) {
        Logger.log("enable buttons: " + enable);
        readButton.setEnabled(enable);
        calibrateButton.setEnabled(enable);
        sensorButton.setEnabled(enable);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            connection.scanAndConnect();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch (permsRequestCode) {
            case 200:
                //boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                connection.scanAndConnect();
                break;
        }
    }

    public void enableBluetoothAndLocation()
    {
       if(URGConnection.bluetoothEnabled()) {
           onBlueToothOk();
       } else {
           Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
           startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_REQUEST_ID);
       }
     }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BLUETOOTH_ENABLE_REQUEST_ID) {
            if (resultCode == RESULT_OK) {
                onBlueToothOk();
            }
        }
    }

    private void onBlueToothOk() {
        checkLocationPermission();
    }
}
