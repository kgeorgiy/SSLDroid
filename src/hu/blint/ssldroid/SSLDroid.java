package hu.blint.ssldroid;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hu.blint.ssldroid.db.SSLDroidDbAdapter;

public class SSLDroid extends Service {
    private static final String TAG = "SSLDroid";

    private List<TcpProxy> proxies = new ArrayList<TcpProxy>();

    @Override
    public void onCreate() {
        SSLDroidDbAdapter dbHelper = new SSLDroidDbAdapter(this);
        try {
            proxies.clear();
            for (TunnelConfig config : dbHelper.fetchAllTunnels()) {
                try {
                    proxies.add(new TcpProxy(config));
                } catch (IOException e) {
                    Log.d(TAG, "Error creating tunnel " + config + ": " + e.toString());
                    new AlertDialog.Builder(this)
                            .setTitle("SSLDroid encountered a fatal error: " + e.getMessage())
                            .setPositiveButton(android.R.string.ok, null)
                            .create();
                }
            }
            createNotification(0, true, "SSLDroid is running", "Started and serving "+ proxies.size()+" tunnels");
        } finally {
            dbHelper.close();
        }

        //get the version
        PackageInfo info;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            Log.d(TAG, "Error getting package version; error='"+e.toString()+"'");
            return;
        }
        //startup message
        Log.d(TAG, "SSLDroid Service Started; version='"+ info.versionName +"', versionname='"+info.versionCode+"'");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            for (TcpProxy proxy : proxies) {
                proxy.stop();
            }
        } catch (Exception e) {
            Log.d("SSLDroid", "Error stopping service: " + e.toString());
        }
        removeNotification(0);
        Log.d(TAG, "SSLDroid Service Stopped");
    }

    public void removeNotification(int id) {
        NotificationManager notificationManager = getNotificationManager();
        if (notificationManager != null) {
            notificationManager.cancel(id);
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    public void createNotification(int id, boolean persistent, String title, String text) {
        Intent intent = new Intent(this, SSLDroidGui.class);
        PendingIntent activity = PendingIntent.getActivity(this, 0, intent, 0);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.icon)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(activity)
                .setOngoing(true);

        // if requested, make the notification persistent, e.g. not clearable by the user at all,
        // automatically hide on displaying the main activity otherwise
        Notification notification = builder.build();
        if (persistent) {
            notification.flags |= Notification.FLAG_NO_CLEAR;
        } else {
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
        }

        NotificationManager notificationManager = getNotificationManager();
        notificationManager.notify(id, notification);
    }
}
