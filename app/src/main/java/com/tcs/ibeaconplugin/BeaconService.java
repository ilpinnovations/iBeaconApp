package com.tcs.ibeaconplugin;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.altbeacon.beacon.*;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;


public class BeaconService extends Service implements BeaconConsumer {

    private AudioManager audioManager;
    private SharedPreferences sharedPreferences;
    public static final String TAG = "IBEACONS";
    private BeaconManager beaconManager;
    private static final int NOTIFICATION_ID = 123;
    private NotificationManager notificationManager;
    String uuid;
    String bName;
    Beacons beac;
    int size;
    private BackgroundPowerSaver backgroundPowerSaver;

    ArrayList<Beacons> beacon=new ArrayList<Beacons>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if(intent!=null) {
            Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        }

        try {
            JSONObject jsonObj = new JSONObject(intent.getStringExtra("JSON"));

            JSONArray arr = jsonObj.getJSONArray("triggerData");
            Log.d(TAG,arr.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject O = arr.getJSONObject(i);
                Beacons b = new Beacons(0,O.getString("tokenID"), O.getString("uuid"), O.getString("major")+"", O.getString("minor")+"", O.getString("beaconName"), O.getInt("radius"), O.getString("notificationText"));
                beacon.add(b);
                uuid=O.getString("uuid");
                bName=O.getString("beaconName");
            }
            size=beacon.size();


        } catch (JSONException e) {
            Toast.makeText(this,"No data",Toast.LENGTH_LONG);
        }

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.bind(this);
        //beaconManager.setBackgroundScanPeriod(5000l); // 5 secs
        //beaconManager.setForegroundScanPeriod(5000l); // 5 secs
        try {
            beaconManager.updateScanPeriods();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "BINDED");
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "In create");
        //Intent i = new Intent(this, BeaconService.class);

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "In bind");
        return null;
    }
    @Override
    public void onBeaconServiceConnect() {
        final Region region = new Region("MyBeacon", Identifier.parse("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);

        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                try {
                    Log.d(TAG, "didEnterRegion");
                    beaconManager.startRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didExitRegion(Region region) {
                try {
                    Log.d(TAG, "didExitRegion");
                    beac.setFlag(0);
                    postNotification("Beacon out of range","Beacon");
                    beaconManager.startRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {

            }
        });

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                for (Beacon oneBeacon : beacons) {
                    Log.d(TAG, "distance: " + oneBeacon.getDistance() + " id:" + oneBeacon.getId1() + "/" + oneBeacon.getId2() + "/" + oneBeacon.getId3());
                    checkMessage(oneBeacon.getDistance(),oneBeacon.getId2().toString(),oneBeacon.getId3().toString());
                }
            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }


    private void postNotification(String msg,String beaconName) {

            Intent notifyIntent = new Intent(BeaconService.this, BeaconService.class);
            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivities(
                    BeaconService.this,
                    0, new Intent[]{notifyIntent},
                    PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(BeaconService.this);
            builder.setSmallIcon(R.drawable.beacon_gray);
            builder.setContentTitle(beaconName);
            builder.setContentText(msg);
            builder.setAutoCancel(true);
            builder.setContentIntent(pendingIntent);

            builder.setDefaults(Notification.DEFAULT_SOUND);
            builder.setDefaults(Notification.DEFAULT_LIGHTS);
            notificationManager.notify(NOTIFICATION_ID, builder.build());

    }
    private void checkMessage(Double distance,String major,String minor){

        for(Beacons b:beacon){
            if(b.getMajor().toString().equals(major)&& b.getMinor().toString().equals(minor)) {
                beac=b;
                Log.d(TAG, b.getMajor());
                    if (distance < Double.parseDouble(b.getRadius() + "")) {
                        Log.d(TAG, b.getMinor());
                        if(b.getFlag()==0) {
                            b.setFlag(1);
                            postNotification(b.getNotificationText(), b.getBeaconName());
                            Log.d(TAG, "Notification posted");
                        }
                    }

                }

            }
        }

    }