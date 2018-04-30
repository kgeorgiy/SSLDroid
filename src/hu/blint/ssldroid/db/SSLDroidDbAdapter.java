package hu.blint.ssldroid.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import hu.blint.ssldroid.TrustType;
import hu.blint.ssldroid.TunnelConfig;

public class SSLDroidDbAdapter implements Closeable {
    // Database fields
    public static final String TUNNELS_TABLE = "tunnels";
    private static final String TUNNEL_ROW_ID = "_id";
    private static final String TUNNEL_NAME = "name";
    private static final String TUNNEL_LOCAL_PORT = "localport";
    private static final String TUNNEL_REMOTE_HOST = "remotehost";
    private static final String TUNNEL_REMOTE_PORT = "remoteport";
    private static final String TUNNEL_KEY_FILE = "pkcsfile";
    private static final String TUNNEL_KEY_PASS = "pkcspass";
    public static final String TUNNEL_TRUST_TYPE = "trust_type";
    public static final String TUNNEL_TRUST_FILE = "trust_file";
    public static final String TUNNEL_TRUST_PASS = "trust_pass";

    private static final String STATUS_TABLE = "status";
    private static final String KEY_STATUS_NAME = "name";
    private static final String KEY_STATUS_VALUE = "value";
    private static final String STOPPED_STATUS = "stopped";

    private SQLiteDatabase database;
    private SSLDroidDbHelper dbHelper;

    public SSLDroidDbAdapter(Context context) {
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
        return database.insert(TUNNELS_TABLE, null, saveTunnel(tunnel));
    }

    /**
     * Update the tunnel
     */
    public void updateTunnel(TunnelConfig tunnel) {
        database.update(TUNNELS_TABLE, saveTunnel(tunnel), whereTunnel(tunnel.id), null);
    }

    private String whereTunnel(Long rowId) {
        return rowId == null ? null : TUNNEL_ROW_ID + "=" + rowId;
    }

    /**
     * Deletes tunnel
     */
    public void deleteTunnel(long rowId) {
        database.delete(TUNNELS_TABLE, whereTunnel(rowId), null);
    }

    /**
     * Return a Cursor over the list of all tunnels in the database
     *
     * @return Cursor over all notes
     */
    public List<TunnelConfig> fetchAllTunnels() {
        final Cursor cursor = queryTunnel(null);
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

    private Cursor queryTunnel(Long rowId) {
        return query(TUNNELS_TABLE, whereTunnel(rowId),
                TUNNEL_ROW_ID, TUNNEL_NAME, TUNNEL_LOCAL_PORT, TUNNEL_REMOTE_HOST, TUNNEL_REMOTE_PORT,
                TUNNEL_KEY_FILE, TUNNEL_KEY_PASS,
                TUNNEL_TRUST_TYPE, TUNNEL_TRUST_FILE, TUNNEL_TRUST_PASS);
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

    private String getString(Cursor cursor, String column, String def) {
        final String value = cursor.getString(cursor.getColumnIndexOrThrow(column));
        return value != null ? value : def;
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
    
    public TunnelConfig fetchTunnel(long rowId) throws SQLException {
        Cursor cursor = queryTunnel(rowId);
        if (!cursor.moveToNext()) {
            throw new SQLException("Tunnel id=" + rowId + " not found");
        }
        return getTunnel(cursor);
    }


    private TunnelConfig getTunnel(Cursor cursor) {
        return new TunnelConfig(
                getLong(cursor, SSLDroidDbAdapter.TUNNEL_ROW_ID),
                getString(cursor, SSLDroidDbAdapter.TUNNEL_NAME),
                getInt(cursor, SSLDroidDbAdapter.TUNNEL_LOCAL_PORT),
                getString(cursor, SSLDroidDbAdapter.TUNNEL_REMOTE_HOST),
                getInt(cursor, SSLDroidDbAdapter.TUNNEL_REMOTE_PORT),
                getString(cursor, SSLDroidDbAdapter.TUNNEL_KEY_FILE),
                getString(cursor, SSLDroidDbAdapter.TUNNEL_KEY_PASS),
                TrustType.valueOf(getString(cursor, SSLDroidDbAdapter.TUNNEL_TRUST_TYPE, TrustType.ALL.name())),
                getString(cursor, SSLDroidDbAdapter.TUNNEL_TRUST_FILE, ""),
                getString(cursor, SSLDroidDbAdapter.TUNNEL_TRUST_PASS, "")
        );
    }

    private ContentValues saveTunnel(TunnelConfig tunnel) {
        ContentValues values = new ContentValues();
        values.put(TUNNEL_NAME, tunnel.name);
        values.put(TUNNEL_LOCAL_PORT, tunnel.listenPort);
        values.put(TUNNEL_REMOTE_HOST, tunnel.targetHost);
        values.put(TUNNEL_REMOTE_PORT, tunnel.targetPort);
        values.put(TUNNEL_KEY_FILE, tunnel.keyFile);
        values.put(TUNNEL_KEY_PASS, tunnel.keyPass);
        values.put(TUNNEL_TRUST_TYPE, tunnel.trustType.name());
        values.put(TUNNEL_TRUST_FILE, tunnel.trustFile);
        values.put(TUNNEL_TRUST_PASS, tunnel.trustPass);
        return values;
    }
}
