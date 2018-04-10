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
            Cursor cursor = dbHelper.fetchAllTunnels();

            try {
                while (cursor.moveToNext()) {
                    String tunnelName = getString(cursor, SSLDroidDbAdapter.KEY_NAME);
                    int listenPort = getInt(cursor, SSLDroidDbAdapter.KEY_LOCALPORT);
                    int targetPort = getInt(cursor, SSLDroidDbAdapter.KEY_REMOTEPORT);
                    String targetHost = getString(cursor, SSLDroidDbAdapter.KEY_REMOTEHOST);
                    String keyFile = getString(cursor, SSLDroidDbAdapter.KEY_PKCSFILE);
                    String keyPass = getString(cursor, SSLDroidDbAdapter.KEY_PKCSPASS);
                    try {
                        TcpProxy proxy = new TcpProxy(tunnelName, listenPort, targetHost, targetPort, keyFile, keyPass);
                        proxy.serve();
                        Log.d(TAG, "Tunnel: "+tunnelName+" "+listenPort+" "+targetHost+" "+targetPort+" "+keyFile);
                    } catch (Exception e) {
                        Log.d(TAG, "Error:" + e.toString());
                        new AlertDialog.Builder(SSLDroid.this)
                                .setTitle("SSLDroid encountered a fatal error: "+e.getMessage())
                                .setPositiveButton(android.R.string.ok, null)
                                .create();
                    }
                }
            } finally {
                cursor.close();
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

    private int getInt(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(columnName));
    }

    private String getString(Cursor cursor, String column) {
        return cursor.getString(cursor.getColumnIndexOrThrow(column));
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
