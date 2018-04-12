package hu.blint.ssldroid.ui;

import android.content.Context;
import android.preference.PreferenceManager;

public class Settings {
    private static final String PREFERENCES_SHOW_NOTIFICATIONS = "preferences_showNotifications";
    private static final String PREFERENCES_SHOW_TUNNEL_INFO = "preferences_showTunnelInfo";
    private static final String PREFERENCES_CONNECTION_TIMEOUT = "preferences_connectionTimeout";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;

    public static boolean isShowNotifications(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFERENCES_SHOW_NOTIFICATIONS, true);
    }

    public static boolean isShowTunnelInfo(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFERENCES_SHOW_TUNNEL_INFO, true);
    }

    public static int getConnectionTimeout(Context context) {
        return parseConnectionTimeout(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERENCES_CONNECTION_TIMEOUT, Integer.toString(DEFAULT_CONNECTION_TIMEOUT)));
    }

    public static int parseConnectionTimeout(String text) {
        try {
            return Math.max(0, Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return DEFAULT_CONNECTION_TIMEOUT;
        }
    }
}
