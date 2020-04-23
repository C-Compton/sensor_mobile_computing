package com.example.sensorproject;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.empatica.empalink.config.EmpaStatus;
import com.example.sensorproject.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements EmpaService.EmpaServiceDelegate {

    private EmpaService empaService;

    private boolean bound = false;

    private ActivityMainBinding viewBinding;

    private static final int REQUEST_ENABLE_BT = 1;

    private static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;

    private static final String CHANNEL_ID = "HEALTH";

    private static final String PERMISSION_STRING = Manifest.permission.ACCESS_COARSE_LOCATION;

    private TextView accel_xLabel;

    private TextView accel_yLabel;

    private TextView accel_zLabel;

    private TextView hrLabel;

    private TextView edaLabel;

    private TextView ibiLabel;

    private TextView temperatureLabel;

    private TextView batteryLabel;

    private TextView statusLabel;

    private TextView deviceNameLabel;

    private LinearLayout dataCnt;

    private Button findSensorButton;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service ) {
            EmpaService.EmpaServiceBinder binder = ( EmpaService.EmpaServiceBinder ) service;

            empaService = binder.getEmpaService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected( ComponentName name ) {
            bound = false;
        }
    };

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if ( ContextCompat.checkSelfPermission( this, PERMISSION_STRING ) !=
                PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( this,
                    new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                    REQUEST_PERMISSION_ACCESS_COARSE_LOCATION );
        } else {
            bindService();
        }
    }

    private void bindService() {
        Intent intent = new Intent( this, EmpaService.class );
        bindService( intent, connection, Context.BIND_AUTO_CREATE );
        waitForBind();
    }

    private void waitForBind() {
        final Handler handler = new Handler();
        final MainActivity self = this;

        handler.post( new Runnable() {
            @Override
            public void run() {
                if ( bound ) {
                    viewBinding.findSensorButton.setVisibility( View.VISIBLE );
                    empaService.setEmpaServiceDelegate( self );
                } else {
                    handler.postDelayed( this, 1000 );
                }
            }
        } );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState ) {

        super.onCreate( savedInstanceState );

        viewBinding = ActivityMainBinding.inflate( getLayoutInflater() );
        View view = viewBinding.getRoot();

        setContentView( view );

        initUiComponents();

        final Button getReadingsButton = viewBinding.getReadingsButton;
        getReadingsButton.setOnClickListener( v -> {

            if ( bound ) {
                updateLabel( batteryLabel, "" + empaService.getLevel() );
                updateLabel( edaLabel, "" + empaService.getGsr() );
                updateLabel( temperatureLabel, "" + empaService.getT() );
            }
        } );

        findSensorButton = viewBinding.findSensorButton;

        findSensorButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick( View v ) {
                Handler handler = new Handler();

                handler.post( new Runnable() {
                    @Override
                    public void run() {
                        // Replacing the current EmpaStatus declaration with the commented
                        // one lets you run the app without connecting to bluetooth --
                        // useful if you want to debug things unrelated to the device
                        // EmpaStatus status = EmpaStatus.CONNECTED;
                        EmpaStatus status = empaService.getStatus();
                        updateLabel( statusLabel, status.name() );
                        if ( EmpaStatus.READY.equals( status ) ) {
                            updateLabel( statusLabel, status.name() + " - Turn on your device" );
                            // start scanning
                            empaService.startScanning();
                            hide();
                            // Periodically check if device is still connected
                            handler.postDelayed( this, 5000 );
                        } else if ( EmpaStatus.CONNECTING.equals( status ) ) {
                            handler.postDelayed( this, 5000 );
                        } else if ( EmpaStatus.CONNECTED.equals( status ) ) {
                            show();
                            // Periodically check if device is still connected
                            handler.postDelayed( this, 5000 );
                        } else if ( EmpaStatus.DISCONNECTED.equals( status ) ) {
                            updateLabel( deviceNameLabel, "" );
                            hide();
                        }
                    }
                } );


            }
        } );

        final Button disconnectButton = viewBinding.disconnectButton;

        disconnectButton.setOnClickListener( v -> empaService.disconnect() );
    }

    @Override
    public void onRequestPermissionsResult( int requestCode,
                                            @NonNull String[] permissions,
                                            @NonNull int[] grantResults ) {

        if ( requestCode ==
             REQUEST_PERMISSION_ACCESS_COARSE_LOCATION ) {// If request is cancelled, the result arrays are empty.
            if ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                // Permission was granted, yay!
                bindService();
            } else {
                // Permission denied, boo!
                final boolean needRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION );
                new AlertDialog.Builder( this )
                        .setTitle( "Permission required" )
                        .setMessage(
                                "Without this permission bluetooth low energy devices cannot be found, allow it in order to connect to the device." )
                        .setPositiveButton( "Retry", ( dialog, which ) -> {
                            // try again
                            if ( needRationale ) {
                                // the "never ask again" flash is not set, try again with permission request
                                bindService();
                            } else {
                                // the "never ask again" flag is set so the permission requests is disabled, try open app settings to enable the permission
                                Intent intent = new Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS );
                                Uri uri = Uri.fromParts( "package", getPackageName(), null );
                                intent.setData( uri );
                                startActivity( intent );
                            }
                        } )
                        .setNegativeButton( "Exit application", ( dialog, which ) -> {
                            // without permission exit is the only way
                            finish();
                        } )
                        .show();
            }
        } else {
            Log.e( "TAG", "Unknown request code: " + requestCode );
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewBinding = null;
        unbindService( connection );
        bound = false;
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        // The user chose not to enable Bluetooth
        if ( requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED ) {
            // You should deal with this
            return;
        }
        super.onActivityResult( requestCode, resultCode, data );
    }

    // Update a label with some text, making sure this is run in the UI thread
    private void updateLabel( final TextView label, final String text ) {
        runOnUiThread( () -> label.setText( text ) );
    }

    private void initUiComponents() {

        // Initialize vars that reference UI components
        statusLabel = viewBinding.status;

        dataCnt = viewBinding.dataArea;

        hrLabel = viewBinding.hr;

        edaLabel = viewBinding.eda;

        temperatureLabel = viewBinding.temperature;

        batteryLabel = viewBinding.battery;

        deviceNameLabel = viewBinding.deviceName;
    }

    void show() {

        runOnUiThread( () -> {
            findSensorButton.setVisibility( View.INVISIBLE );
            dataCnt.setVisibility( View.VISIBLE );
        } );
    }

    void hide() {

        runOnUiThread( () -> {
            findSensorButton.setVisibility( View.VISIBLE );
            dataCnt.setVisibility( View.INVISIBLE );
        } );
    }

    @Override
    public void onHydrationLevelChange(HydrationLevel h) {
        // TODO : Do something with updated hydration level

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        // This makes it so that the app doesn't restart when you click the notification
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Hydration Alert")
                .setContentText("You are now " + h.value)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        int notificationId = 603921;
        notificationManager.notify(notificationId, builder.build());

        Log.i("HydroHomies", "Hydration level updated to : " + h.getValue());
    }

    @Override
    public void onHeartRateUpdated(long heartRate) {
        updateLabel( hrLabel, String.valueOf(heartRate) );
    }

    public void vHydratedButtonClickAction(View theButton){
        onHydrationLevelChange(HydrationLevel.WELL_HYDRATED);
    }

    public void sHydratedButtonClickAction(View theButton){
        onHydrationLevelChange(HydrationLevel.SLIGHTLY_HYDRATED);
    }

    public void sDehydratedButtonClickAction(View theButton){
        onHydrationLevelChange(HydrationLevel.SLIGHTLY_DEHYDRATED);
    }

    public void vDehydratedButtonClickAction(View theButton) {
        onHydrationLevelChange(HydrationLevel.VERY_DEHYDRATED);
    }

}
