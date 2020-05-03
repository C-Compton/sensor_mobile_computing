package com.example.sensorproject;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.EmpaticaDevice;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;

import java.util.ArrayList;
import java.util.List;

public class EmpaService extends Service implements EmpaDataDelegate, EmpaStatusDelegate {

    private EmpaDeviceManager deviceManager = null;

    private EmpaServiceDelegate empaServiceDelegate = null;

    private EmpaStatus status;

    private final IBinder binder = new EmpaServiceBinder();

    private final Weka weka = new Weka();

    private HydrationLevel hydrationLevel = HydrationLevel.UNKNOWN_LEVEL;

    // Empatica device battery level
    // Device only reports battery level when it changes significantly.
    // so start at min float value to ensure different form value to be reported
    private float batteryLevel = Float.MIN_VALUE;

    private final List<Double> gsrHistory = new ArrayList<>();

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (deviceManager != null ){
            deviceManager.cleanUp();
        }
    }

    public void setEmpaServiceDelegate( EmpaServiceDelegate delegate ) {
        this.empaServiceDelegate = delegate;
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
        // no op
    }

    @Override
    public void didUpdateSensorStatus( int status, EmpaSensorType type ) {
        // no op
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
        // no op
    }

    @Override
    public void bluetoothStateChanged() {
        // no op
    }

    @Override
    public void didUpdateOnWristStatus( int status ) {
        // no op
    }

    //
    // EmpaDataDelegate Methods
    //

    @Override
    public void didReceiveGSR( float gsr, double timestamp ) {

        gsrHistory.add( (double) gsr );

        if (gsrHistory.size() < 8) { // Take rolling aggregates across 2 seconds of data
            return;
        }

        // Rolling history of 2 seconds of sensor readings
        if (gsrHistory.size() > 8) {
            gsrHistory.remove( 0 );
        }

        double min = min(gsrHistory);
        double max = max(gsrHistory);
        double var = var(gsrHistory);
        double std = std(gsrHistory);

        Pair<Double, Double> results = weka.classification( min, max, var, std ) ;
        if (results.second < 90.0 ) {
            return;
        }
        HydrationLevel newHydrationLevel = HydrationLevel.convert( results.first.intValue() );
        if(! hydrationLevel.equals( newHydrationLevel )) {
            hydrationLevel = newHydrationLevel;
            if (empaServiceDelegate != null ) {
                empaServiceDelegate.onHydrationLevelChange(newHydrationLevel);
            }
        }
    }

    @Override
    public void didReceiveBVP( float bvp, double timestamp ) {
        // no op
    }

    @Override
    public void didReceiveIBI( float ibi, double timestamp ) {
        //no op
    }

    @Override
    public void didReceiveTemperature( float t, double timestamp ) {
        // no op
    }

    @Override
    public void didReceiveAcceleration( int x, int y, int z, double timestamp ) {
        // no op
    }

    @Override
    public void didReceiveBatteryLevel( float level, double timestamp ) {
        // If the new value differs from the old, update old value
        // and call delegate to update the UI
        if ( this.batteryLevel != level) {
            this.batteryLevel = level;
            empaServiceDelegate.onBatteryLevelChange(level);
        }
    }

    @Override
    public void didReceiveTag( double timestamp ) {
        // no op
    }

    //
    // Standard Getters
    //

    public EmpaStatus getStatus() {
        return status;
    }


    //
    // Helper Methods
    //
    public void startScanning() {
        deviceManager.startScanning();
    }

    private double min(List<Double> data) {
        if( data == null || data.isEmpty()) {
            return 0.0;
        }

        return data.stream()
                .reduce(Double.MAX_VALUE, (a, b) -> a.compareTo(b) <= 0 ? a : b);
    }

    private double max(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return 0.0;
        }

        return data.stream()
                .reduce(Double.MIN_VALUE, (a, b) -> a.compareTo(b) <= 0 ? b : a);
    }

    private double mean(List<Double> data) {
        if ( data == null || data.isEmpty() ){
            return 0.0;
        }

        return data.stream()
                .mapToDouble(i -> i)
                .average()
                .orElse(0.0);
    }

    private double var(List<Double> data) {
        if ( data == null ) {
            return 0.0;
        }
        double avg = mean(data);
        double s = data.stream()
                .reduce(0.0, (a, b) -> a + Math.pow(b - avg, 2));

        return s / data.size();
    }

    private double std(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return 0.0;
        }
        return Math.sqrt(var(data));
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

    public interface EmpaServiceDelegate {
        void onHydrationLevelChange(HydrationLevel h);
        void onBatteryLevelChange(float level);
    }
}
