package hu.blint.ssldroid.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.Closeable;

public class SSLDroidDbAdapter implements Closeable {
    // Database fields
    private static final String STATUS_TABLE = "status";
    private static final String KEY_STATUS_NAME = "name";
    private static final String KEY_STATUS_VALUE = "value";
    private static final String STOPPED_STATUS = "stopped";

    private SQLiteDatabase database;
    private SSLDroidDb dbHelper;

    public SSLDroidDbAdapter(Context context) {
        dbHelper = new SSLDroidDb(context);
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
        database.close();
    }

    private boolean getStatus(String status) {
        Cursor cursor = query(STATUS_TABLE, whereStatus(status), KEY_STATUS_NAME, KEY_STATUS_VALUE);
        try {
            return cursor.getCount() > 0;
        } finally {
            cursor.close();
        }
    }

    private String whereStatus(String name) {
        return KEY_STATUS_NAME + "='" + name + "'";
    }

    private Cursor query(String table, String where, String... columns) {
        return database.query(table, columns, where, null, null, null, null);
    }

    public boolean getStopStatus() {
        return getStatus(STOPPED_STATUS);
    }

    public void setStopStatus() {
        setStatus(STOPPED_STATUS);
    }

    private void setStatus(String status) {
        ContentValues stopStatus = new ContentValues();
        stopStatus.put(KEY_STATUS_NAME, status);
        stopStatus.put(KEY_STATUS_VALUE, "yes");
        database.updateWithOnConflict(STATUS_TABLE, stopStatus, whereStatus(status), null, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void clearStopStatus() {
        database.delete(STATUS_TABLE, whereStatus(STOPPED_STATUS), null);
    }
}
