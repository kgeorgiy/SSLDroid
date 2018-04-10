package hu.blint.ssldroid.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import hu.blint.ssldroid.TunnelConfig;

public class SSLDroidDbAdapter {

    // Database fields
    public static final String KEY_ROWID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_LOCALPORT = "localport";
    public static final String KEY_REMOTEHOST = "remotehost";
    public static final String KEY_REMOTEPORT = "remoteport";
    public static final String KEY_PKCSFILE = "pkcsfile";
    public static final String KEY_PKCSPASS = "pkcspass";
    public static final String KEY_STATUS_NAME = "name";
    public static final String KEY_STATUS_VALUE = "value";
    private static final String DATABASE_TABLE = "tunnels";
    private static final String STATUS_TABLE = "status";
    private Context context;
    private SQLiteDatabase database;
    private SSLDroidDbHelper dbHelper;

    public SSLDroidDbAdapter(Context context) {
        this.context = context;
        dbHelper = new SSLDroidDbHelper(context);
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
        database.close();
    }

    /**
     * Create a new tunnel If the tunnel is successfully created return the new
     * rowId for that note, otherwise return a -1 to indicate failure.
     */
    public long createTunnel(TunnelConfig tunnel) {
        return database.insert(DATABASE_TABLE, null, saveTunnel(tunnel));
    }

    /**
     * Update the tunnel
     */
    public void updateTunnel(TunnelConfig tunnel) {
        database.update(DATABASE_TABLE, saveTunnel(tunnel), where(tunnel.id), null);
    }

    private String where(long rowId) {
        return KEY_ROWID + "="
                               + rowId;
    }

    /**
     * Deletes tunnel
     */
    public boolean deleteTunnel(long rowId) {
        return database.delete(DATABASE_TABLE, where(rowId), null) > 0;
    }

    /**
     * Return a Cursor over the list of all tunnels in the database
     *
     * @return Cursor over all notes
     */
    public List<TunnelConfig> fetchAllTunnels() {
        final Cursor cursor = query(KEY_ROWID, KEY_NAME, KEY_LOCALPORT, KEY_REMOTEHOST, KEY_REMOTEPORT, KEY_PKCSFILE, KEY_PKCSPASS);
        final List<TunnelConfig> tunnels = new ArrayList<TunnelConfig>();
        try {
            while (cursor.moveToNext()) {
                tunnels.add(getTunnel(cursor));
            }
            return tunnels;
        } finally {
            cursor.close();
        }
    }

    private long getLong(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(columnName));
    }

    private int getInt(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(columnName));
    }

    private String getString(Cursor cursor, String column) {
        return cursor.getString(cursor.getColumnIndexOrThrow(column));
    }

    private Cursor query(String... columns) {
        return database.query(DATABASE_TABLE, columns, null, null, null, null, null);
    }

    /**
     * Return a Cursor over the list of all tunnels in the database
     *
     * @return Cursor over all notes
     */
    public Cursor fetchAllLocalPorts() {
        return query(KEY_NAME, KEY_LOCALPORT);
    }

    /**
     * Return a Cursor positioned at the defined tunnel
     */
    public Cursor fetchStatus(String valuename) throws SQLException {
        return database.query(STATUS_TABLE, new String[] {
                                            KEY_STATUS_NAME, KEY_STATUS_VALUE
                                        },
                                        KEY_STATUS_NAME + "='" + valuename + "'", null, null, null, null);
    }

    public Cursor getStopStatus() {
        return fetchStatus("stopped");
    }

    public boolean setStopStatus() {
	ContentValues stopStatus = new ContentValues();
        stopStatus.put(KEY_STATUS_NAME, "stopped");
        stopStatus.put(KEY_STATUS_VALUE, "yes");
        if (getStopStatus().getCount() == 0)
            database.insert(STATUS_TABLE, null, stopStatus);
        return true;
    }
    
    public boolean delStopStatus() {
        return database.delete(STATUS_TABLE, KEY_STATUS_NAME+"= 'stopped'", null) > 0;
    }
    
    public TunnelConfig fetchTunnel(long rowId) throws SQLException {
        Cursor cursor = database.query(true, DATABASE_TABLE, new String[] {
                                            KEY_ROWID, KEY_NAME, KEY_LOCALPORT, KEY_REMOTEHOST, KEY_REMOTEPORT,
                                            KEY_PKCSFILE, KEY_PKCSPASS
                                        },
                where(rowId), null, null, null, null, null);
        if (!cursor.moveToNext()) {
            throw new SQLException("Tunnel id=" + rowId + " not found");
        }
        return getTunnel(cursor);
    }


    private TunnelConfig getTunnel(Cursor cursor) {
        return new TunnelConfig(
                getLong(cursor, SSLDroidDbAdapter.KEY_ROWID),
                getString(cursor, SSLDroidDbAdapter.KEY_NAME),
                getInt(cursor, SSLDroidDbAdapter.KEY_LOCALPORT),
                getString(cursor, SSLDroidDbAdapter.KEY_REMOTEHOST),
                getInt(cursor, SSLDroidDbAdapter.KEY_REMOTEPORT),
                getString(cursor, SSLDroidDbAdapter.KEY_PKCSFILE),
                getString(cursor, SSLDroidDbAdapter.KEY_PKCSPASS)
        );
    }

    private ContentValues saveTunnel(TunnelConfig tunnel) {
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, tunnel.name);
        values.put(KEY_LOCALPORT, tunnel.listenPort);
        values.put(KEY_REMOTEHOST, tunnel.targetHost);
        values.put(KEY_REMOTEPORT, tunnel.targetPort);
        values.put(KEY_PKCSFILE, tunnel.keyFile);
        values.put(KEY_PKCSPASS, tunnel.keyPass);
        return values;
    }
}


