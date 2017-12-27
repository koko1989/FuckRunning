package com.acgmiao.dev.fuckrunning.util;


import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.acgmiao.dev.fuckrunning.MyLocationApplication;
import com.google.android.gms.maps.model.LatLng;

import static com.acgmiao.dev.fuckrunning.util.CoordTrans.GPS2GCJ;

public class MyLocation {

    public Location location;

    public void getLocation() {
        if (ContextCompat.checkSelfPermission(MyLocationApplication.getApplication(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationManager LManager = (LocationManager) MyLocationApplication.getApplication().getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setCostAllowed(false);
        criteria.setAccuracy(Criteria.ACCURACY_COARSE); //设置水平位置精度
        String providerName = LManager.getBestProvider(criteria, true);
        location = LManager.getLastKnownLocation(providerName);
        if (location != null) {
            double lot = location.getLongitude();
            double lat = location.getLatitude();
            LatLng mglocation = GPS2GCJ(new LatLng(lat, lot));
            location.setLatitude(mglocation.latitude);
            location.setLongitude(mglocation.longitude);
        } else {
            Log.e(this.toString(), "getLocation: location null");
        }
    }
}
