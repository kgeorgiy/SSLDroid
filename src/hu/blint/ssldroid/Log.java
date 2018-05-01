package hu.blint.ssldroid;

public class Log {
    public static final String TAG = "SSLDroid";

    public static void d(String message) {
        android.util.Log.d(TAG, message);
    }

    public static void w(String message) {
        android.util.Log.w(TAG, message);
    }

    public static void i(String message) {
        android.util.Log.i(TAG, message);
    }
}
