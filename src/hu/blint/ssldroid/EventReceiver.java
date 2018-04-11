package hu.blint.ssldroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import hu.blint.ssldroid.db.SSLDroidDbAdapter;

public class EventReceiver extends BroadcastReceiver {
    private boolean isStopped(Context context){
        SSLDroidDbAdapter dbHelper = new SSLDroidDbAdapter(context);
        try {
            return dbHelper.getStopStatus();
        } finally {
            dbHelper.close();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(SSLDroid.class.getName());
        Log.d("Event received " + intent.getAction());
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("Boot completed");
            startIfNeeded(context, i);
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
            Log.d("Connectivity change " + activeNetInfo);
            context.stopService(i);
            if (activeNetInfo != null && activeNetInfo.isAvailable()) {
                startIfNeeded(context, i);
            }
        }
    }

    private void startIfNeeded(Context context, Intent i) {
        if (!isStopped(context)) {
            context.startService(i);
        } else {
            Log.w("Not starting service as directed by explicit close");
        }
    }
}

