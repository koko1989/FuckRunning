package com.acgmiao.dev.fuckrunning.activity;

import com.acgmiao.dev.fuckrunning.util.PermissionUtils;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.acgmiao.dev.fuckrunning.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

import static com.acgmiao.dev.fuckrunning.util.CoordTrans.GPS2GCJ;

public class MapsActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        LocationListener,
        GoogleMap.CancelableCallback {

    /**
     * Refresh Once Per Second
     */
    private static final long LOCATION_REFRESH_TIME_INTERVAL = 1000;

    /**
     * Min Meter
     */
    private static final float LOCATION_MIN_DISTANCE = 1.0f;

    /**
     * Google Map Instance
     */
    private GoogleMap mMap;

    /**
     * LocationManager Instance
     */
    private LocationManager mLocationManager;

    /**
     * Location Listener Add Flag
     */
    private boolean mIsAddListener;

    /**
     * Is Permission Granted
     */
    private boolean mIsPermissionGranted;

    /**
     * Location Broadcast Receiver
     */
    private BroadcastReceiver mBroadcastReceiver;

    /**
     * Is Resume Flag
     */
    private boolean mIsResume;

    /**
     * Last of My Location
     */
    private Location mLastLocation;

    /**
     *
     */
    private Marker myLocationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equalsIgnoreCase(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                    if (mIsPermissionGranted && mIsResume) {
                        boolean isGPSOn = isGPSOn();
                        if (isGPSOn) {
                            startGetLocation();
                        } else {
                            stopGetLocation();
                        }
                    }
                }
            }
        };
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsResume = true;
        registerBroadcastReceiver();
        if (mIsPermissionGranted) {
            startGetLocation();
        } else {
            if (checkLocationPermission()) {
                mIsPermissionGranted = true;
                startGetLocation();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsResume = true;
        unregisterBroadcastReceiver();
        if (mIsPermissionGranted) {
            stopGetLocation();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        boolean isGotLocationPermission = checkLocationPermission();
        if (isGotLocationPermission) {
            // goto next method
            mIsPermissionGranted = true;
            startGetLocation();
        } else {
            // need to get location permission
            requestLocationPermission();
        }


//        updateLocationUI();

        //丢失默认事件，待修复
        //mMap.setOnMyLocationButtonClickListener(this);
//        mMap.setOnMyLocationClickListener(this);
    }

    @Override
    public void onFinish() {

    }

    @Override
    public void onCancel() {

    }

    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void unregisterBroadcastReceiver() {
        unregisterReceiver(mBroadcastReceiver);
    }

    private void initLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
    }

    @SuppressLint("MissingPermission")
    private void startGetLocation() {
        initLocationManager();
        if (!mIsAddListener) {
            List<String> providers = mLocationManager.getProviders(true);
            for (int i = 0; i < providers.size(); i++) {
                String provider = providers.get(i);
                Log.e("test", "startGetLocation:" + provider);
                mLocationManager.requestLocationUpdates(
                        provider, LOCATION_REFRESH_TIME_INTERVAL, LOCATION_MIN_DISTANCE, this);
            }
            mIsAddListener = true;
        }
    }

    private void stopGetLocation() {
        initLocationManager();
        if (mIsAddListener) {
            Log.e("test", "stopGetLocation");
            mLocationManager.removeUpdates(this);
            mIsAddListener = false;
        }
    }

    /**
     * Update My Location Marker
     */
    private void updateMyLocationMarker() {
        if (mLastLocation != null) {
            LatLng latLng = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
            if(myLocationMarker == null) {
                // init marker
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                myLocationMarker = mMap.addMarker(markerOptions);
                //move map
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
                mMap.animateCamera(cameraUpdate,this);
            }
            // my position
            myLocationMarker.setPosition(latLng);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("test", "onLocationChanged:" + location.toString() + ",Provider:" + location.getProvider());
        LatLng mglocation = GPS2GCJ(new LatLng(location.getLatitude(), location.getLongitude()));
        location.setLatitude(mglocation.latitude);
        location.setLongitude(mglocation.longitude);
        mLastLocation = location;
        updateMyLocationMarker();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.e("test", "onStatusChanged:" + s + "," + i);
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.e("test", "onProviderEnabled:" + s);
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.e("test", "onProviderDisabled:" + s);
    }

    private boolean isGPSOn() {
        initLocationManager();
        boolean gpsOn = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return gpsOn;
    }

    /**
     * check Location Permission
     *
     * @return
     */
    private boolean checkLocationPermission() {
        boolean locationPermission = false;
        int permissionStatus = ContextCompat.
                checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            locationPermission = true;
        }
        return locationPermission;
    }

    /**
     * requestLocationPermission
     */
    public void requestLocationPermission() {
        PermissionUtils.requestPermission(this,
                PermissionUtils.LOCATION_PERMISSION_REQUEST_CODE,
                android.Manifest.permission.ACCESS_FINE_LOCATION, false);
    }


    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
//        try {
//            if (mPermissionDenied) {
//                mMap.setMyLocationEnabled(false);
//                mMap.getUiSettings().setMyLocationButtonEnabled(false);
//            } else {
//                mMap.setMyLocationEnabled(true);
//                mMap.getUiSettings().setMyLocationButtonEnabled(true);
//                mMap.setLocationSource(new MyLocationSource());
//            }
//        } catch (SecurityException e) {
//            Log.e("Exception: %s", e.getMessage());
//        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocationApplication button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return true;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode != PermissionUtils.LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }
        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            mIsPermissionGranted = true;
            initLocationManager();
            startGetLocation();
        } else {
            mIsPermissionGranted = false;
            Log.e("test", "not get location permission");
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
//        if (mPermissionDenied) {
//            showMissingPermissionError();
//            mPermissionDenied = false;
//        }
    }

    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog.newInstance(false).show(getSupportFragmentManager(), "dialog");
    }

    private LocationSource.OnLocationChangedListener myLocationListener = null;


    private class MyLocationSource implements LocationSource {
        @Override
        public void activate(OnLocationChangedListener listener) {
//            myLocationListener = listener;
//            MyLocation mylocation = new MyLocation();
//            mylocation.getLocation();
//            if (mylocation.location == null) {
//                //什么乱七八糟的
//                mPermissionDenied = true;
//                updateLocationUI();
//            } else {
//                myLocationListener.onLocationChanged(mylocation.location);
//            }
        }

        @Override
        public void deactivate() {
            myLocationListener = null;
        }
    }
}