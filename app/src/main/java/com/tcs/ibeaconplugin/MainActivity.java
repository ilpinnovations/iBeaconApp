package com.tcs.ibeaconplugin;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;


public class MainActivity extends ActionBarActivity {
    String tokenid = "";
    final String id = "603091788273";
    String Tag = "IBEACONS";
    private BluetoothAdapter mBluetoothAdapter;
    private static final int ENABLE_BL_REQUEST_CODE = 0;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);
        NewInitialiser n = new NewInitialiser(this);

    }
}

