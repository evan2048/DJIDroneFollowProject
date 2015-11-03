package com.evan2048.evanfollow;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import dji.midware.data.manager.P3.ServiceManager;
import dji.midware.usb.P3.DJIUsbAccessoryReceiver;
import dji.midware.usb.P3.UsbAccessoryService;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIError;
import dji.sdk.api.DJIDroneTypeDef.DJIDroneType;
import dji.sdk.api.Camera.DJICameraDecodeTypeDef.DecoderType;
import dji.sdk.api.GroundStation.DJIFollowMeInitializationInfo;
import dji.sdk.api.GroundStation.DJIFollowMeTarget;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationFollowMeMode;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationFollowMeYawMode;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationResult;
import dji.sdk.api.MainController.DJIMainControllerSystemState;
import dji.sdk.api.mediacodec.DJIVideoDecoder;
import dji.sdk.interfaces.DJIGeneralListener;
import dji.sdk.interfaces.DJIGroundStationExecuteCallBack;
import dji.sdk.interfaces.DJIMcuUpdateStateCallBack;
import dji.sdk.interfaces.DJIReceivedVideoDataCallBack;
import com.evan2048.bluetooth.BluetoothSPP;
import com.evan2048.bluetooth.BluetoothSPP.BluetoothConnectionListener;
import com.evan2048.bluetooth.BluetoothSPP.BluetoothStateListener;
import com.evan2048.bluetooth.BluetoothSPP.OnDataReceivedListener;
import com.evan2048.util.LocationTools;
import com.evan2048.bluetooth.BluetoothState;
import com.evan2048.bluetooth.BluetoothDeviceList;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import net.sf.marineapi.nmea.parser.*;

public class MainActivity extends FragmentActivity implements OnClickListener,OnTouchListener, ConnectionCallbacks, OnConnectionFailedListener, LocationListener, SurfaceTextureListener {
    
    //Environment
    private static final String TAG = "MainActivity";  //debug TAG
    //DJI
    private final DJIDroneType DJI_DRONE_TYPE=DJIDroneType.DJIDrone_Phantom3_Professional;  //dji drone type
    private DJIVideoDecoder mVideoDecoder;
    private DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack;
    private DJIMcuUpdateStateCallBack mDjiMcuUpdateStateCallBack;
    private static boolean isDJIAoaStarted = false;  //DJIAoa
    private boolean isDJICameraStart = false;  //is dji camera start
    private double droneLocationLatitude = 0.0;
    private double droneLocationLongitude = 0.0;
    private double droneAltitude = 0.0;
    //Google Map
    private GoogleMap mGoogleMap; // Might be null if Google Play services APK is not available.
    private final double GOOGLE_MAP_FRAGMENT_SIZE_SCALE = 0.4;  //google map fragment scale to device screen size
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    //Bluetooth
    private BluetoothSPP mBluetoothSPP;  //https://github.com/akexorcist/Android-BluetoothSPPLibrary
    //target
    private double targetLocationLatitude = 0.0;
    private double targetLocationLongitude = 0.0;
    private int targetSatelliteCount = 0;
    private double targetHDOP = 0.0;
    private boolean isFollowing = false;
    //drone target
    private float drone2TargetDistance = 0;
    private float drone2TargetBearing = 0;
    //Other
    private int deviceScreenWidth = 0;  //device screen size
    private int deviceScreenHeight = 0;
    private Location deviceCurrentLocation;
    DecimalFormat mDecimalFormat=new DecimalFormat("#.0");  //format double value to #.0
    
    //print log message
    private void showLog(String str)
    {
        Log.e(TAG, str);
    }
    
    //show Toast
    private void showToast(String str)
    {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
    
    //init UI
    private TextureView videoTextureView;
    private Button testButton;
    private Button bluetoothButton;
    private Button openGSButton;
    private Button initFollowButton;
    private Button startFollowButton;
    private RelativeLayout leftBottomInnerRelativeLayout;  //hold google map fragment,to resize google map
    private Button resizeMapButton;
    private TextView targetHDOPTextView;
    private TextView targetDistanceTextView;
    private void initUIControls()
    {
        videoTextureView=(TextureView)findViewById(R.id.videoTextureView);
        testButton=(Button)findViewById(R.id.testButton);
        bluetoothButton=(Button)findViewById(R.id.bluetoothButton);
        openGSButton=(Button)findViewById(R.id.openGSButton);
        initFollowButton=(Button)findViewById(R.id.initFollowButton);
        startFollowButton=(Button)findViewById(R.id.startFollowButton);
        leftBottomInnerRelativeLayout=(RelativeLayout)findViewById(R.id.leftBottomInnerRelativeLayout);
        resizeMapButton=(Button)findViewById(R.id.resizeMapButton);
        targetHDOPTextView=(TextView)findViewById(R.id.targetHDOPTextView);
        targetDistanceTextView=(TextView)findViewById(R.id.targetDistanceTextView);
        //Add Click Listener
        testButton.setOnClickListener(this);
        bluetoothButton.setOnClickListener(this);
        openGSButton.setOnClickListener(this);
        initFollowButton.setOnClickListener(this);
        startFollowButton.setOnClickListener(this);
        resizeMapButton.setOnClickListener(this);
        //Add Listener
        videoTextureView.setSurfaceTextureListener(this);
        resizeMapButton.setOnTouchListener(this);
        //Customize controls
        DisplayMetrics displayMetrics=new DisplayMetrics();  //get device screen size
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        deviceScreenWidth=displayMetrics.widthPixels;
        deviceScreenHeight=displayMetrics.heightPixels;  //reset google map fragment size
        leftBottomInnerRelativeLayout.setLayoutParams(new RelativeLayout.LayoutParams((int)(deviceScreenWidth*GOOGLE_MAP_FRAGMENT_SIZE_SCALE), (int)(deviceScreenHeight*GOOGLE_MAP_FRAGMENT_SIZE_SCALE)));
        resizeMapButton.bringToFront();
    }
    
    //click
    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
        //test button
        case R.id.testButton:
        {
            showLog("testButton click");
            
//          GGAParser mGgaParser=new GGAParser("$GNGGA,065747.00,2232.39441,N,11356.82451,E,1,12,0.88,22.9,M,-2.7,M,,*67");
//          showLog(""+mGgaParser.getPosition().getLatitude()+" "+mGgaParser.getPosition().getLongitude()+" "+mGgaParser.getSatelliteCount()+" "+mGgaParser.getHorizontalDOP());
//          
//          GNGGAParser mGnggaParser=new GNGGAParser("$GNGGA,065747.00,2232.39441,N,11356.82451,E,1,12,0.88,22.9,M,-2.7,M,,*67");
//          showLog(""+mGnggaParser.getLatitude()+" "+mGnggaParser.getLongitude()+" "+mGnggaParser.getSatelliteCount()+" "+mGnggaParser.getHDOP());
            
            break;
        }
        //bluetooth button
        case R.id.bluetoothButton:
        {
            showLog("bluetoothButton click");
            Intent intent = new Intent(getApplicationContext(), BluetoothDeviceList.class);
            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
            break;
        }
        //oepn gs button
        case R.id.openGSButton:
        {
            showLog("openGSButton click");
            isFollowing = false;
            DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack()
            {
                @Override
                public void onResult(final GroundStationResult result)
                {
                    runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            showToast(result.toString());
                        }
                    });
                }
            });
            break;
        }
        //init follow button
        case R.id.initFollowButton:
        {
            showLog("initFollowButton click");
            isFollowing = false;
            DJIFollowMeInitializationInfo info = new DJIFollowMeInitializationInfo();
            info.followMeMode = GroundStationFollowMeMode.Relative_Mode;
            info.yawMode = GroundStationFollowMeYawMode.Point_To_Customer;
            info.userLatitude = targetLocationLatitude;
            info.userLongitude = targetLocationLongitude;
            DJIDrone.getDjiGroundStation().startFollowMe(info, new DJIGroundStationExecuteCallBack()
            {
                @Override
                public void onResult(final GroundStationResult result)
                {
                    runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            showToast(result.toString());
                        }
                    });
                }
            });
            break;
        }
        //start follow button
        case R.id.startFollowButton:
        {
            showLog("startFollowButton click");
            showToast("start follow");
            isFollowing = true;
            break;
        }
        //open close map button
        case R.id.resizeMapButton:
        {
            showLog("resizeMapButton click");
            break;
        }
        default:
        {
            break;
        }
        }
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        //view
        switch (v.getId())
        {
        //resize google map
        case R.id.resizeMapButton:
        {
            if(event.getAction()==MotionEvent.ACTION_MOVE && event.getRawX()>resizeMapButton.getWidth() && event.getRawY()<deviceScreenHeight-resizeMapButton.getHeight())
            {
                int offsetX=resizeMapButton.getWidth()/2;
                int offsetY=resizeMapButton.getHeight()/2;
                leftBottomInnerRelativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(offsetX+(int)event.getRawX(), offsetY+deviceScreenHeight-(int)event.getRawY()));
            }
            break;
        }
        default:
        {
            break;
        }
        }
        //action
        switch (event.getAction())
        {
        case MotionEvent.ACTION_UP:
        {
            v.performClick();  //TODO BUG:resizeMapButton will call twice,see log
            break;
        }
        default:
        {
            break;
        }
        }
        
        return false;
    }
    
    //activate DJI SDK(if not,DJI SDK can't use)
    private void activateDJISDK()
    {
        new Thread()
        {
            public void run()
            {
                try
                {
                    DJIDrone.checkPermission(getApplicationContext(), new DJIGeneralListener()
                    {
                        @Override
                        public void onGetPermissionResult(final int result)
                        {
                            //result=0 is success
                            showLog("DJI SDK onGetPermissionResult = "+result);
                            showLog("DJI SDK onGetPermissionResultDescription = "+DJIError.getCheckPermissionErrorDescription(result));
                            if(result!=0)
                            {
                                runOnUiThread(new Runnable()
                                {
                                    public void run()
                                    {
                                        showToast(getString(R.string.djiSdkActivateError)+":"+DJIError.getCheckPermissionErrorDescription(result));
                                    }
                                });
                            }
                        }
                    });
                }
                catch(Exception e)
                {
                    showLog("activateDJISDK() Exception");
                    e.printStackTrace();
                }
            }
        }.start();
    }
    
    //start DJIAoa
    private void startDJIAoa()
    {
        if(isDJIAoaStarted)
        {
            //Do nothing
            showLog("DJI Aoa aready started");
        }
        else
        {
            ServiceManager.getInstance();
            UsbAccessoryService.registerAoaReceiver(this);
            isDJIAoaStarted = true;
            showLog("DJI Aoa start success");
        }
        Intent aoaIntent = getIntent();
        if(aoaIntent != null)
        {
            String action = aoaIntent.getAction();
            if(action==UsbManager.ACTION_USB_ACCESSORY_ATTACHED || action == Intent.ACTION_MAIN)
            {
                Intent attachedIntent = new Intent();
                attachedIntent.setAction(DJIUsbAccessoryReceiver.ACTION_USB_ACCESSORY_ATTACHED);
                sendBroadcast(attachedIntent);
            }
        }
    }
    
    //init DJI SDK
    private void initDJISDK()
    {
        startDJIAoa();
        activateDJISDK();
        DJIDrone.initWithType(this.getApplicationContext(), DJI_DRONE_TYPE);
        DJIDrone.connectToDrone(); // Connect to the drone
    }
    
    //check DJI camera connect status
    private Timer checkCameraConnectionTimer = new Timer();
    class CheckCameraConnectionTask extends TimerTask
    {
        @Override
        public void run()
        {
            if(checkCameraConnectState()==true)
            {
                if(isDJICameraStart==false)
                {
                    startDJICamera();
                    isDJICameraStart=true;
                }
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        
                    }
                });
            }
            else
            {
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        
                    }
                });
            }
        }
    }
    private boolean checkCameraConnectState()
    {
        //check connection
        return DJIDrone.getDjiCamera().getCameraConnectIsOk();
    }
    
    //init DJI camera
    private void initDJICamera()
    {
        //check camera status every 3 seconds
        checkCameraConnectionTimer.schedule(new CheckCameraConnectionTask(), 1000, 3000);
    }
    
    private void initVideoDecoder(Surface surface) {
        DJIDrone.getDjiCamera().setDecodeType(DecoderType.Hardware);
        mVideoDecoder = new DJIVideoDecoder(this, surface);
        mReceivedVideoDataCallBack = new DJIReceivedVideoDataCallBack(){
            @Override
            public void onResult(byte[] videoBuffer, int size)
            {
                DJIDrone.getDjiCamera().sendDataToDecoder(videoBuffer,size);
            }
        };
        DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(mReceivedVideoDataCallBack);
    }
    
    //start DJI camera
    private void startDJICamera()
    {
        //Mc
        mDjiMcuUpdateStateCallBack=new DJIMcuUpdateStateCallBack()
        {
            @Override
            public void onResult(DJIMainControllerSystemState state)
            {
                droneLocationLatitude=state.droneLocationLatitude;
                droneLocationLongitude=state.droneLocationLongitude;
                droneAltitude=state.altitude;
                if(droneLocationLatitude!=0.0 && droneLocationLongitude!=0.0)
                {
                    runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            //showLog("droneLocation:"+droneLocationLatitude+","+droneLocationLongitude);
                            drawDroneInGoogleMap(new LatLng(droneLocationLatitude, droneLocationLongitude));
                        }
                    });
                }
            }
        };
        DJIDrone.getDjiMainController().setMcuUpdateStateCallBack(mDjiMcuUpdateStateCallBack);
        DJIDrone.getDjiMainController().startUpdateTimer(100);
    }
    
    //destroy DJI camera
    private void destroyDJICamera()
    {
        if(checkCameraConnectionTimer!=null)
        {
            checkCameraConnectionTimer.cancel();
        }
        if(mVideoDecoder!=null)
        {
            mVideoDecoder.stopVideoDecoder();
            mVideoDecoder=null;
        }
        if(DJIDrone.getDjiCamera()!=null)
        {
            //set callback null
            DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(null);
            DJIDrone.getDjiMainController().setMcuUpdateStateCallBack(null);
            //stop timmer
            DJIDrone.getDjiMainController().stopUpdateTimer();
        }
    }
    
    //init google map
    private void initGoogleMap()
    {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mGoogleMap == null)
        {
            // Try to obtain the map from the SupportMapFragment.
            mGoogleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment)).getMap();
            // Check if we were successful in obtaining the map.
            if (mGoogleMap != null)
            {
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            }
        }
    }
    
    private Marker droneMarker;  //drone marker draw on google map
    private MarkerOptions droneMarkerOptions=new MarkerOptions();
    //draw drone in google map
    private void drawDroneInGoogleMap(LatLng latLng)
    {
        if(droneMarker==null)
        {
            droneMarkerOptions.position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            droneMarker=mGoogleMap.addMarker(droneMarkerOptions);
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));
        }
        else
        {
            droneMarker.setPosition(latLng);
        }
    }
    
    private Marker targetMarker;  //target marker draw on google map
    private MarkerOptions targetMarkerOptions=new MarkerOptions();
    //draw target in google map
    private void drawTargetInGoogleMap(LatLng latLng)
    {
        if(targetMarker==null)
        {
            targetMarkerOptions.position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            targetMarker=mGoogleMap.addMarker(targetMarkerOptions);
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));
        }
        else
        {
            targetMarker.setPosition(latLng);
        }
    }
    
    private Marker deviceMarker;  //device marker draw on google map
    private MarkerOptions deviceMarkerOptions=new MarkerOptions();
    //draw device in google map
    private void drawDeviceInGoogleMap(final LatLng latLng)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if(deviceMarker==null)
                {
                    deviceMarkerOptions.position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.google_map_blue_dot));
                    deviceMarker=mGoogleMap.addMarker(deviceMarkerOptions);
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));
                }
                else
                {
                    deviceMarker.setPosition(latLng);
                }
            }
        });
    }
    
    //init Bluetooth
    private void initBluetooth()
    {
        mBluetoothSPP=new BluetoothSPP(this);
        if(!mBluetoothSPP.isBluetoothEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BluetoothState.REQUEST_ENABLE_BT);
        }
        else
        {
            startBluetoothService();
        }
        
        //set Listener
        mBluetoothSPP.setOnDataReceivedListener(new OnDataReceivedListener()
        {
            public void onDataReceived(byte[] data, String message)
            {
                //TODO
                GGAParser mGgaParser=new GGAParser(message);
                try
                {
                    targetLocationLatitude=mGgaParser.getPosition().getLatitude();
                    targetLocationLongitude=mGgaParser.getPosition().getLongitude();
                    targetSatelliteCount=mGgaParser.getSatelliteCount();
                    targetHDOP=mGgaParser.getHorizontalDOP();
                }
                catch(Exception e)
                {
                    targetLocationLatitude=0.0;
                    targetLocationLongitude=0.0;
                    targetSatelliteCount=0;
                    targetHDOP=0.0;
                }
                
                //showLog(message);
//              GNGGAParser mParser=new GNGGAParser(message);
//              targetLocationLatitude=mParser.getLatitude();
//              targetLocationLongitude=mParser.getLongitude();
//              targetSatelliteCount=mParser.getSatelliteCount();
//              targetHDOP=mParser.getHDOP();
                //check zero
                if(targetLocationLatitude==0.0 || targetLocationLongitude==0.0 || targetSatelliteCount==0 || targetHDOP==0.0)
                {
                    return;
                }
                else
                {
                    //calculate target distance and bearing
                    float[] results=new float[3];
                    LocationTools.distanceBetween(droneLocationLatitude, droneLocationLongitude, targetLocationLatitude, targetLocationLongitude, results);
                    drone2TargetDistance=results[0];
                    drone2TargetBearing=results[2];
                    runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            drawTargetInGoogleMap(new LatLng(targetLocationLatitude, targetLocationLongitude));
                            targetHDOPTextView.setText(getString(R.string.targetHDOP)+":"+targetHDOP);
                            targetDistanceTextView.setText(getString(R.string.targetDistance)+":"+mDecimalFormat.format(drone2TargetDistance));
                        }
                    });
                    if(isFollowing)
                    {
                        DJIFollowMeTarget target = new DJIFollowMeTarget();
                        target.latitude = targetLocationLatitude;
                        target.longitude = targetLocationLongitude;
                        DJIDrone.getDjiGroundStation().sendFollowTargetGps(target, new DJIGroundStationExecuteCallBack()
                        {
                            @Override
                            public void onResult(GroundStationResult mResult)
                            {
                                
                            }
                        });
                    }
                }
            }
        });
        mBluetoothSPP.setBluetoothConnectionListener(new BluetoothConnectionListener()
        {
            public void onDeviceConnected(String name, String address)
            {
                // Do something when successfully connected
                showLog("mBluetooth onDeviceConnected:name="+name+" address="+address);
                showToast(name+" connected");
            }

            public void onDeviceDisconnected()
            {
                // Do something when connection was disconnected
                showLog("mBluetooth onDeviceDisconnected");
                showToast("bluetooth disconnected");
            }

            public void onDeviceConnectionFailed()
            {
                // Do something when connection failed
                showLog("mBluetooth onDeviceConnectionFailed");
                showToast("bluetooth connect failed");
            }
        });
        mBluetoothSPP.setBluetoothStateListener(new BluetoothStateListener()
        {                
            public void onServiceStateChanged(int state)
            {
                if(state == BluetoothState.STATE_CONNECTED)
                {
                    // Do something when successfully connected
                }
                else if(state == BluetoothState.STATE_CONNECTING)
                {
                    // Do something while connecting
                }
                else if(state == BluetoothState.STATE_LISTEN)
                {
                    // Do something when device is waiting for connection
                }
                else if(state == BluetoothState.STATE_NONE)
                {
                    // Do something when device don't have any connection
                }
            }
        });
    }
    
    //start bluetooth service
    private void startBluetoothService()
    {
        mBluetoothSPP.setupService();
        mBluetoothSPP.startService(BluetoothState.DEVICE_OTHER);
    }
    
    //destroy bluetooth
    private void destroyBluetooth()
    {
        if(mBluetoothSPP!=null)
        {
            mBluetoothSPP.disconnect();
            mBluetoothSPP.stopService();
        }
    }
    
    //build google api client
    private synchronized void buildGoogleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build();
        mGoogleApiClient.connect();
    }
    
    private void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        //onCreate init
        initUIControls();
        initBluetooth();
        initGoogleMap();
        buildGoogleApiClient();
        initDJISDK();
        initDJICamera();
        
        //TODO
//      GGAParser mGgaParser=new GGAParser("$GNGGA,065747.00,2232.39441,N,11356.82451,E,1,12,0.88,22.9,M,-2.7,M,,*67");
//      showLog(""+mGgaParser.getPosition().getLatitude()+" "+mGgaParser.getPosition().getLongitude()+" "+mGgaParser.getSatelliteCount()+" "+mGgaParser.getHorizontalDOP());
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        ServiceManager.getInstance().pauseService(false); // Resume the DJIAoa service
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        ServiceManager.getInstance().pauseService(true); // Pause the DJIAoa service
    }
    
    @Override
    protected void onDestroy()
    {
        destroyBluetooth();
        destroyDJICamera();
        DJIDrone.disconnectToDrone();
        showLog("MainActivity onDestroy()");
        super.onDestroy();
        //android.os.Process.killProcess(android.os.Process.myPid());  //kill process
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE)
        {
            if(resultCode == Activity.RESULT_OK)
            {
                mBluetoothSPP.connect(data);
            }
        }
        else if(requestCode == BluetoothState.REQUEST_ENABLE_BT)
        {
            if(resultCode == Activity.RESULT_OK)
            {
                startBluetoothService();
            }
            else
            {
                // Do something if user doesn't choose any device (Pressed back)
            }
        }
    }
    
    //press again to exit
    private static boolean needPressAgain = false;
    private Timer ExitTimer = new Timer();
    class ExitCleanTask extends TimerTask
    {
        @Override
        public void run()
        {               
            needPressAgain = false;
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            if (needPressAgain)
            {
                needPressAgain = false;
                finish();
            } 
            else 
            {
                needPressAgain = true;
                showToast(getString(R.string.pressAgainExitString));
                ExitTimer.schedule(new ExitCleanTask(), 2000);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //GoogleApiClient onConnected
    @Override
    public void onConnected(Bundle connectionHint)
    {
        showLog("GoogleApiClient onConnected");
        createLocationRequest();
        //updateDeviceLocationTaskTimer.schedule(new updateDeviceLocationTask(), 1000, 100);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int value)
    {
        showLog("GoogleApiClient onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        showLog("GoogleApiClient onConnectionFailed");
    }

    @Override
    public void onLocationChanged(Location location)
    {
        showLog("device location:"+location.getLatitude()+","+location.getLongitude());
        deviceCurrentLocation=location;
        drawDeviceInGoogleMap(new LatLng(deviceCurrentLocation.getLatitude(), deviceCurrentLocation.getLongitude()));
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mVideoDecoder == null) {
            Surface mSurface  = new Surface(surface);
            initVideoDecoder(mSurface);
        } else {
            mVideoDecoder.setSurface(new Surface(surface));
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mVideoDecoder != null)
        {
            mVideoDecoder.setSurface(null);
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        
    }
    
}
