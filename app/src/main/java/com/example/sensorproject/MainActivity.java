package com.example.sensorproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.EmpaticaDevice;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaStatusDelegate;
import com.example.sensorproject.databinding.ActivityMainBinding;
import com.squareup.okhttp.internal.NamedRunnable;

public class MainActivity extends AppCompatActivity {

    private EmpaService empaService;

    private boolean bound = false;

    private ActivityMainBinding viewBinding;

    private static final int REQUEST_ENABLE_BT = 1;

    private static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;

    private static final String PERMISSION_STRING = Manifest.permission.ACCESS_COARSE_LOCATION;

    private TextView accel_xLabel;

    private TextView accel_yLabel;

    private TextView accel_zLabel;

    private TextView bvpLabel;

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

    @Override
    public void onStart() {
        super.onStart();
        if ( ContextCompat.checkSelfPermission( this, PERMISSION_STRING ) !=
             PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( this,
                                               new String[] {
                                                       Manifest.permission.ACCESS_COARSE_LOCATION },
                                               REQUEST_PERMISSION_ACCESS_COARSE_LOCATION );
        } else {
            bindService();
        }
    }

    private void bindService() {
        Intent intent = new Intent( this, EmpaService.class) ;
        bindService(intent, connection, Context.BIND_AUTO_CREATE );
        waitForBind();
    }

    private void waitForBind() {
        final Handler handler = new Handler();

        handler.post( new Runnable() {
            @Override
            public void run() {
                if ( bound ) {
                    viewBinding.findSensorButton.setVisibility( View.VISIBLE );
                } else {
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }

    @Override
    protected void onCreate( Bundle savedInstanceState ) {

        super.onCreate( savedInstanceState );

        viewBinding = ActivityMainBinding.inflate( getLayoutInflater() );
        View view = viewBinding.getRoot();

        setContentView( view );

        initUiComponents();

        final Button getReadingsButton = viewBinding.getReadingsButton;
        getReadingsButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick( View v ) {

                if ( bound ) {
                    updateLabel( accel_xLabel, "" + empaService.getX() );
                    updateLabel( accel_yLabel, "" + empaService.getX() );
                    updateLabel( accel_zLabel, "" + empaService.getX() );
                    updateLabel( bvpLabel, "" + empaService.getBvp() );
                    updateLabel( batteryLabel, "" + empaService.getLevel() );
                    updateLabel( edaLabel, "" + empaService.getGsr() );
                    updateLabel( temperatureLabel, "" + empaService.getT() );
                }
            }
        } );

        findSensorButton = viewBinding.findSensorButton;

        findSensorButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick( View v ) {
                Handler handler = new Handler() ;

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        EmpaStatus status = empaService.getStatus();
                        updateLabel( statusLabel, status.name() );
                        if (EmpaStatus.READY.equals(status)) {
                            updateLabel( statusLabel, status.name() + " - Turn on your device" );
                            // start scanning
                            empaService.startScanning();
                            hide();
                            // Periodically check if device is still connected
//                            handler.postDelayed( this, 5000 );
                        } else if ( EmpaStatus.CONNECTING.equals( status ) ) {
//                            handler.postDelayed( this, 5000 );
                        } else if (EmpaStatus.CONNECTED.equals( status) ) {
                            show();
                            // Periodically check if device is still connected
//                            handler.postDelayed(this, 5000);
                        } else if (EmpaStatus.DISCONNECTED.equals( status )) {
                            updateLabel( deviceNameLabel, "" );
                            hide();
                        }
                        handler.postDelayed(this, 5000);
                    }
                });


            }
        } );

        final Button disconnectButton = viewBinding.disconnectButton;

        disconnectButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick( View v ) {
                empaService.disconnect();
            }
        } );
    }

    @Override
    public void onRequestPermissionsResult( int requestCode,
                                            @NonNull String[] permissions,
                                            @NonNull int[] grantResults ) {

        switch ( requestCode ) {
            case REQUEST_PERMISSION_ACCESS_COARSE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if ( grantResults.length > 0 &&
                     grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                    // Permission was granted, yay!
                    bindService();
                } else {
                    // Permission denied, boo!
                    final boolean needRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION );
                    new AlertDialog.Builder( this ).setTitle( "Permission required" )
                                                   .setMessage(
                                                           "Without this permission bluetooth low energy devices cannot be found, allow it in order to connect to the device." )
                                                   .setPositiveButton( "Retry",
                                                                       new DialogInterface.OnClickListener() {
                                                                           public void onClick(
                                                                                   DialogInterface dialog,
                                                                                   int which ) {
                                                                               // try again
                                                                               if ( needRationale ) {
                                                                                   // the "never ask again" flash is not set, try again with permission request
                                                                                   bindService();
                                                                               } else {
                                                                                   // the "never ask again" flag is set so the permission requests is disabled, try open app settings to enable the permission
                                                                                   Intent intent = new Intent(
                                                                                           Settings.ACTION_APPLICATION_DETAILS_SETTINGS );
                                                                                   Uri uri = Uri.fromParts(
                                                                                           "package",
                                                                                           getPackageName(),
                                                                                           null );
                                                                                   intent.setData(
                                                                                           uri );
                                                                                   startActivity(
                                                                                           intent );
                                                                               }
                                                                           }
                                                                       } )
                                                   .setNegativeButton( "Exit application",
                                                                       new DialogInterface.OnClickListener() {
                                                                           public void onClick(
                                                                                   DialogInterface dialog,
                                                                                   int which ) {
                                                                               // without permission exit is the only way
                                                                               finish();
                                                                           }
                                                                       } )
                                                   .show();
                }
                break;
            default:
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
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                label.setText( text );
            }
        } );
    }

    private void initUiComponents() {

        // Initialize vars that reference UI components
        statusLabel = viewBinding.status;

        dataCnt = viewBinding.dataArea;

        accel_xLabel = viewBinding.accelX;

        accel_yLabel = viewBinding.accelY;

        accel_zLabel = viewBinding.accelZ;

        bvpLabel = viewBinding.bvp;

        edaLabel = viewBinding.eda;

        ibiLabel = viewBinding.ibi;

        temperatureLabel = viewBinding.temperature;

        batteryLabel = viewBinding.battery;

        deviceNameLabel = viewBinding.deviceName;
    }

    void show() {

        runOnUiThread( new Runnable() {

            @Override
            public void run() {
                findSensorButton.setVisibility( View.INVISIBLE );
                dataCnt.setVisibility( View.VISIBLE );
            }
        } );
    }

    void hide() {

        runOnUiThread( new Runnable() {

            @Override
            public void run() {
                findSensorButton.setVisibility( View.VISIBLE );
                dataCnt.setVisibility( View.INVISIBLE );
            }
        } );
    }
}
