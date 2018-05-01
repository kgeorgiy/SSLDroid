package hu.blint.ssldroid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

import hu.blint.ssldroid.db.SSLDroidDbAdapter;
import hu.blint.ssldroid.ui.ContextAsyncTask;
import hu.blint.ssldroid.ui.Settings;

public class SSLDroid extends Service {
    private List<TcpTunnel> tunnels = new ArrayList<TcpTunnel>();

    private static boolean started = false;

    public static boolean isStarted() {
        return started;
    }

    private static boolean isStopped(Context context){
        SSLDroidDbAdapter dbHelper = new SSLDroidDbAdapter(context);
        try {
            return dbHelper.getStopStatus();
        } finally {
            dbHelper.close();
        }
    }

    public static void startIfNeeded(Context context) {
        if (!isStopped(context)) {
            start(context);
        } else {
            Log.w("Not starting service as directed by explicit close");
        }
    }

    public static void start(Context context) {
        if (!started) {
            started = true;
            context.startService(new Intent(context, SSLDroid.class));
        }
    }

    public static void stop(Context context) {
        if (started) {
            context.stopService(new Intent(context, SSLDroid.class));
            started = false;
        }
    }

    @Override
    public void onCreate() {
        new StartTask(this).execute();

        //get the version
        PackageInfo info;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            Log.d("Error getting package version; error='"+e.toString()+"'");
            return;
        }
        //startup message
        Log.d("SSLDroid Service Started; version='"+ info.versionName +"', versionname='"+info.versionCode+"'");
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
            for (TcpTunnel proxy : tunnels) {
                proxy.close();
            }
        } catch (Exception e) {
            Log.d("Error stopping service: " + e.toString());
        }
        removeNotification(0);
        Log.d("SSLDroid Service Stopped");
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
        if (!Settings.isShowNotifications(this)) {
            return;
        }

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

    private void onStarted(List<TcpTunnel> tunnels, List<String> errors) {
        this.tunnels = tunnels;
        createNotification(
                0,
                true,
                "SSLDroid is running",
                "Started and serving " + tunnels.size() + " tunnels (" + errors.size() + " errors)"
        );
    }

    public static class StartTask extends ContextAsyncTask<SSLDroid, Void, Integer, List<TcpTunnel>> {
        private final List<String> errors = new ArrayList<String>();

        StartTask(SSLDroid droid) {
            super(droid);
        }

        @Override
        protected List<TcpTunnel> doInBackground(SSLDroid droid, Void... voids) {
            SSLDroidDbAdapter dbHelper = new SSLDroidDbAdapter(droid);
            try {
                List<TcpTunnel> tunnels = new ArrayList<TcpTunnel>();
                for (TunnelConfig config : dbHelper.fetchAllTunnels()) {
                    try {
                        tunnels.add(new TcpTunnel(config, Settings.getConnectionTimeout(droid)));
                    } catch (TunnelException e) {
                        errors.add("Error creating tunnel " + config + ": " + e.toString());
                        Log.d("Error creating tunnel " + config + ": " + e.toString());
                    }
                }
                return tunnels;
            } finally {
                dbHelper.close();
            }
        }

        @Override
        protected void onPostExecute(SSLDroid droid, List<TcpTunnel> tunnels) {
            droid.onStarted(tunnels, errors);
        }
    }
}
