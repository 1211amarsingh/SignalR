package com.example.signalr;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!isMyServiceRunning(SignalRService.class)) {
            startService(new Intent(this, SignalRService.class));
            Log.e("Service", "start");
        } else {
            Log.e("Service", "stop+start");
            stopService(new Intent(this, SignalRService.class));

            startService(new Intent(this, SignalRService.class));
        }
        SignalRService.setListener(new MyListener() {
            @Override
            public void onLocationChange(String response) {

            }
        });
        SignalRService.invokeConnect("connect", "string to send");
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}