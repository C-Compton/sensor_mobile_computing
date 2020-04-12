package com.example.sensorproject;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.EmpaticaDevice;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;

public class EmpaService extends Service implements EmpaDataDelegate, EmpaStatusDelegate {

    private EmpaDeviceManager deviceManager = null;

    private EmpaStatus status;

    private final IBinder binder = new EmpaServiceBinder();

    private float gsr;
    private float bvp;
    private float t;

    private float level;



    public class EmpaServiceBinder extends Binder {

        EmpaService getEmpaService( ) {

            return EmpaService.this;
        }
    }

    @Override
    public IBinder onBind( Intent intent ) {

        initEmpaticaDeviceManager();
        return binder;
    }


    public EmpaService() {
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (deviceManager != null ){
            deviceManager.cleanUp();
        }
    }

    private void initEmpaticaDeviceManager() {
        deviceManager = new EmpaDeviceManager( getApplicationContext(), this, this );

        deviceManager.authenticateWithAPIKey( getString( R.string.empatica_api_key ) );
    }

    //
    // EmpaStatusDelegate Methods
    //

    @Override
    public void didUpdateStatus( EmpaStatus status ) {
        this.status = status;
        // The device manager is ready for use
        if ( status == EmpaStatus.READY ) {
            // Start scanning
            Log.i( "TAG", "Device manager ready. Start scanning..." );
            deviceManager.startScanning();
            // The device manager has established a connection

        }
    }

    @Override
    public void didEstablishConnection() {

    }

    @Override
    public void didUpdateSensorStatus( int status, EmpaSensorType type ) {

    }

    @Override
    public void didDiscoverDevice( EmpaticaDevice bluetoothDevice,
                                   String deviceLabel,
                                   int rssi,
                                   boolean allowed ) {
        // Check if the discovered device can be used with your API key. If allowed is always false,
        // the device is not linked with your API key. Please check your developer area at
        // https://www.empatica.com/connect/developer.php
        if ( allowed ) {
            // Stop scanning. The first allowed device will do.
            deviceManager.stopScanning();
            try {
                // Connect to the device
                deviceManager.connectDevice( bluetoothDevice );
            } catch ( ConnectionNotAllowedException e ) {
                // This should happen only if you try to connect when allowed == false.
                Toast.makeText( EmpaService.this, "Sorry, you can't connect to this device",
                                Toast.LENGTH_SHORT ).show();
            }
        }
    }

    @Override
    public void didFailedScanning( int errorCode ) {
        Log.e( "DidFailedScanning", "Failed to locate Empatica device" );
    }

    @Override
    public void didRequestEnableBluetooth() {

    }

    @Override
    public void bluetoothStateChanged() {

    }

    @Override
    public void didUpdateOnWristStatus( int status ) {

    }

    //
    // EmpaDataDelegate Methods
    //

    @Override
    public void didReceiveGSR( float gsr, double timestamp ) {
        this.gsr = gsr;
    }

    @Override
    public void didReceiveBVP( float bvp, double timestamp ) {
        this.bvp = bvp;
    }

    @Override
    public void didReceiveIBI( float ibi, double timestamp ) {
    }

    @Override
    public void didReceiveTemperature( float t, double timestamp ) {
        this.t = t;
    }

    @Override
    public void didReceiveAcceleration( int x, int y, int z, double timestamp ) {
    }

    @Override
    public void didReceiveBatteryLevel( float level, double timestamp ) {
        this.level = level;
    }

    @Override
    public void didReceiveTag( double timestamp ) {

    }

    //
    // Standard Getters
    //

    public float getGsr() {
        return gsr;
    }

    public float getBvp() {
        return bvp;
    }

    public float getT() {
        return t;
    }

    public float getLevel() {
        return level;
    }

    public EmpaStatus getStatus() {
        return status;
    }


    //
    // Helper Methods
    //
    public void startScanning() {
        deviceManager.startScanning();
    }

    public void disconnect() {
        if (deviceManager != null ) {
            /*
            This and deviceManager.disconnect() attempt to write logs to a file on the Android
            device. This file does not exist, with no way of creating manually as the file name
            appears randomly generated. This causes the service to croak, requiring a restart
            of the App to re-connect any Empatica devices.
             */
            deviceManager.cancelConnection();
        }

    }
}
