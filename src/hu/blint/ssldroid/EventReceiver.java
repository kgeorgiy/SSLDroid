package hu.blint.ssldroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class EventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(SSLDroid.class.getName());
        Log.d("Event received " + intent.getAction());
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("Boot completed");
            SSLDroid.startIfNeeded(context);
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
            Log.d("Connectivity change " + activeNetInfo);
            SSLDroid.stop(context);
            if (activeNetInfo != null && activeNetInfo.isAvailable()) {
                SSLDroid.startIfNeeded(context);
            }
        }
    }
}
