package com.example.signalr;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.concurrent.ExecutionException;

import microsoft.aspnet.signalr.client.Platform;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.http.android.AndroidPlatformComponent;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler1;
import microsoft.aspnet.signalr.client.transport.ClientTransport;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;

public class SignalRService extends Service {

    private final IBinder mBinder = new LocalBinder();

    private HubConnection connection;
    private static HubProxy proxy;
    private Handler handler;
    private static MyListener myListener;
    private String serverUrl = "http://103.240.91.162:85";
    //    private String serverUrl = "http://192.168.1.104:86";
    private static String TAG = "SignalRService";

    private String hubName = "Event";

    public static void setListener(MyListener listener) {
        myListener = listener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "startcommand");
        // We are using binding. do we really need this...?
        if (!StartHubConnection()) {
            ExitWithMessage("Chat Service failed to start!");
        }
        if (!RegisterEvents()) {
            ExitWithMessage("End-point error: Failed to register Events!");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (!StartHubConnection()) {
            ExitWithMessage("Chat Service failed to start!");
        }

        if (!RegisterEvents()) {
            ExitWithMessage("End-point error: Failed to register Events!");
        }

        return mBinder;
    }

    // https://developer.android.com/guide/components/bound-services.html
    public class LocalBinder extends Binder {
        public SignalRService getService() {
            return SignalRService.this;
        }
    }

    private boolean StartHubConnection() {
        Log.e(TAG, "starthub");
        Platform.loadPlatformComponent(new AndroidPlatformComponent());

        // Create Connection
        connection = new HubConnection(serverUrl);
//        connection.setCredentials(User.loginCredentials);
//        connection.getHeaders().put("Device", "Mobile");

        // Create Proxy
        proxy = connection.createHubProxy(hubName);

        // Establish Connection
        ClientTransport clientTransport = new ServerSentEventsTransport(connection.getLogger());
        SignalRFuture<Void> signalRFuture = connection.start(clientTransport);

        try {
            signalRFuture.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean RegisterEvents() {
        Log.e(TAG, "RegisterEvents");

        Handler mHandler = new Handler(Looper.getMainLooper());
        try {
            proxy.on("get_location", new SubscriptionHandler1<String>() {
                @Override
                public void run(final String msg) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SignalRService.this, ">" + msg, Toast.LENGTH_SHORT).show();

                            Log.e(TAG, ">" + msg);
                            if (myListener != null) {
                                myListener.onLocationChange(msg);
                            } else {
                                Log.e(TAG, "listener not initialized");
                            }
                        }
                    });
                }
            }, String.class);

            proxy.on("onError", new SubscriptionHandler1<String>() {
                @Override
                public void run(final String error) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(SignalRService.this, error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
            }, String.class);
            proxy.on("connect", new SubscriptionHandler1<Object>() {
                @Override
                public void run(Object o) {
                    Log.e(TAG, o.toString());
                }
            }, Object.class);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }


        return true;
    }

    public static void invokeConnect(String event, String data) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (proxy != null) {
                    proxy.invoke(event, data);
                    Log.e(TAG, "" + event + "> " + data);
                } else {
                    Log.e(TAG, "proxy not initialise");
                }

                return null;
            }
        }.execute();
    }


    private void ExitWithMessage(String message) {
        Log.e("Exit", "EEEEEE " + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopSelf();
            }
        }, 3000);
    }

    @Override
    public void onDestroy() {
        if (connection != null)
            connection.stop();
        super.onDestroy();
    }
}
