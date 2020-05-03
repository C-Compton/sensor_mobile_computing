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
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.empatica.empalink.config.EmpaStatus;
import com.example.sensorproject.databinding.ActivityMainBinding;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity<U extends View> extends AppCompatActivity implements EmpaService.EmpaServiceDelegate {

    private EmpaService empaService;

    private EmpaStatus empaStatus = EmpaStatus.DISCONNECTED;

    private int counter;

    private boolean bound = false;

    private ActivityMainBinding viewBinding;

    private static final int REQUEST_ENABLE_BT = 1;

    private static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;

    private static final String CHANNEL_ID = "HEALTH";

    private static final String PERMISSION_STRING = Manifest.permission.ACCESS_COARSE_LOCATION;

    private TextView youAreLabel;

    private TextView hydrationLevelLabel;

    private TextView deviceStatus;

    private ImageView background;

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
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            CharSequence name = getString( R.string.channel_name );
            String description = getString( R.string.channel_description );
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel( CHANNEL_ID, name, importance );
            channel.setDescription( description );
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService( NotificationManager.class );
            notificationManager.createNotificationChannel( channel );
        }
    }

    // TODO this is just here for demo/testing purposes. Delete before submitting.
    private void changeHydrationLevelEveryMin() {
        Timer timer = new Timer();
        counter = 0;
        timer.schedule( new TimerTask() {
            @Override
            public void run() {
                MainActivity.this.runOnUiThread( new Runnable() {
                    public void run() {
                        if ( ( counter % 2 ) == 0 ) {
                            onHydrationLevelChange( HydrationLevel.WELL_HYDRATED );
                        } else {
                            onHydrationLevelChange( HydrationLevel.VERY_DEHYDRATED );
                        }
                        ++counter;
                    }
                } );
            }
        }, 60000, 60000 );
    }

    @Override
    public void onStart() {
        super.onStart();
        if ( ContextCompat.checkSelfPermission( this, PERMISSION_STRING ) != PackageManager.PERMISSION_GRANTED ) {
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

        setTitle( "HydrationAlert" );

        findSensorButton = viewBinding.findSensorButton;

        findSensorButton.setOnClickListener( v -> {
            Log.i( "STATUS", "EmpaStatus: " + empaStatus.name() );
            if ( EmpaStatus.DISCONNECTED.equals( empaStatus ) ) {
                // start scanning
                updateLabel( deviceStatus, getString( R.string.turn_on_dev ) );
                hide();
                empaService.startScanning();
            }
            hide();
        } );

        // TODO this is just for debugging/demos, changes the hydration level every minute
        // changeHydrationLevelEveryMin();
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

    // This function styles the status bar; doing this here because it was difficult
    // to do with the ConstraintLayout I have in activity_main.xml
    @Override
    public void setTitle( CharSequence title ) {
        super.setTitle( title );

        TextView textView = new TextView( this );
        textView.setText( title );
        textView.setTextColor( Color.BLACK );
        textView.setTextSize( 22 );
        textView.setLayoutParams( new LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT ) );
        textView.setGravity( Gravity.CENTER_HORIZONTAL );
        getSupportActionBar().setDisplayOptions( ActionBar.DISPLAY_SHOW_CUSTOM );
        getSupportActionBar().setCustomView( textView );
    }

    // Update a label with some text, making sure this is run in the UI thread
    private void updateLabel( final TextView label, final String text ) {
        runOnUiThread( () -> label.setText( text ) );
    }

    private void updateImageView( final ImageView view, final int attribute ) {
        runOnUiThread( () -> view.setImageResource( attribute ) );
    }

    private void initUiComponents() {
        // Initialize vars that reference UI components
        youAreLabel = viewBinding.youAre;
        hydrationLevelLabel = viewBinding.hydrationLevel;
        deviceStatus = viewBinding.empaticaBattery;
        background = viewBinding.background;
    }

    void show() {
        runOnUiThread( () -> {
            findSensorButton.setVisibility( View.INVISIBLE );
            youAreLabel.setVisibility( View.VISIBLE );
            hydrationLevelLabel.setVisibility( View.VISIBLE );
            deviceStatus.setVisibility( View.VISIBLE );
        } );
    }

    void hide() {
        runOnUiThread( () -> {
            findSensorButton.setVisibility( View.VISIBLE );
            youAreLabel.setVisibility( View.INVISIBLE );
            hydrationLevelLabel.setVisibility( View.INVISIBLE );
            deviceStatus.setVisibility( View.VISIBLE );
        } );
    }

    @Override
    public void onHydrationLevelChange( HydrationLevel h ) {
        // Set background image and hydration level text based on hydration level
        updateLabel( youAreLabel, getString( R.string.you_are ) );
        updateLabel( hydrationLevelLabel, h.getValue() );
        updateImageView( background, HydrationLevel.WELL_HYDRATED.equals( h ) ?
                                     R.drawable.hydrated_background :
                                     R.drawable.dehydrated_background );

        // Send notification to alert user about changed hydration level
        Intent intent = new Intent( this, MainActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_SINGLE_TOP );
        PendingIntent pendingIntent = PendingIntent.getActivity( this, 0, intent, 0 );

        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder( this, CHANNEL_ID )
                .setSmallIcon( R.drawable.notification_icon )
                .setContentTitle( "Hydration Alert" )
                .setContentText( "You are " + h.value )
                .setPriority( NotificationCompat.PRIORITY_DEFAULT )
                .setContentIntent( pendingIntent )
                .setAutoCancel( true );

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from( this );
        notificationManager.notify( ( int ) System.currentTimeMillis(), builder.build() );

        Log.i( "HydroHomies", "Hydration level updated to : " + h.getValue() );
    }

    @Override
    public void onBatteryLevelChange( float level ) {
        // Battery level has changed. Update device status to inform user
        updateLabel( deviceStatus, getString( R.string.battery_lev, level ) );
    }

    @Override
    public void onDeviceStatusChange( EmpaStatus status ) {
        if ( empaStatus != status ) {
            this.empaStatus = status;
            switch ( status ) {
                case DISCOVERING:
                    updateLabel( deviceStatus, getString( R.string.discover_dev ) );
                    break;
                case CONNECTING:
                    updateLabel( deviceStatus, getString( R.string.dev_connecting ) );
                    findSensorButton.setVisibility( View.INVISIBLE );
                    break;
                case CONNECTED:
                    show();
                    updateLabel( deviceStatus, getString( R.string.empty_string ) );
                    break;
                case DISCONNECTED:
                    updateLabel( deviceStatus, getString( R.string.dev_not_connected ) );
                    hide();
                    break;
                // For now, fall through to READY
                case DISCONNECTING:
                case INITIAL:
                case READY:
                    updateLabel( deviceStatus, getString( R.string.turn_on_dev ) );
                    hide();
                default:
                    // For now, assume we're ready.

            }
        }
    }
}