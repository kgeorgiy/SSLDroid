package hu.blint.ssldroid.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import hu.blint.ssldroid.Log;
import hu.blint.ssldroid.TunnelConfig;

public class SSLDroidDb extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "applicationdata";
    private static final int DATABASE_VERSION = 7;

    // Database creation sql statement
    private static final String STATUS_CREATE = "CREATE TABLE IF NOT EXISTS status (name text, value text);";
    public final EntityDb<TunnelConfig> tunnels;

    public SSLDroidDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        SQLiteDatabase db = getWritableDatabase();
        tunnels = new EntityDb<TunnelConfig>(db, TunnelDb.TABLE);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(STATUS_CREATE);
        TunnelDb.TABLE.createTable(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion <= 1 && 2 <= newVersion) {
            Log.i("Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will add a status table");
            database.execSQL("CREATE TABLE IF NOT EXISTS status (name text, value text);");
        }
        if (oldVersion <= 5 && 6 <= newVersion) {
            Log.i("Upgrading database: Adding trust columns");
            TunnelDb.addTrustColumns(database);
        }
    }
}
