package hu.blint.ssldroid.ui;

import android.content.Context;
import android.preference.PreferenceManager;

public class Settings {
    private static final String PREFERENCES_SHOW_NOTIFICATIONS = "preferences_showNotifications";

    public static boolean isShowNotifications(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFERENCES_SHOW_NOTIFICATIONS, true);
    }
}
