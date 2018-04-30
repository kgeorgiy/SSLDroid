package hu.blint.ssldroid.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Arrays;

import hu.blint.ssldroid.Log;

public class SSLDroidDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "applicationdata";
    private static final int DATABASE_VERSION = 6;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS tunnels (_id integer primary key autoincrement, "
            + "name text not null, localport integer not null, remotehost text not null, "
            + "remoteport integer not null, pkcsfile text not null, pkcspass text );";
    private static final String STATUS_CREATE = "CREATE TABLE IF NOT EXISTS status (name text, value text);";

    public SSLDroidDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        database.execSQL(STATUS_CREATE);
    }

    // Method is called during an update of the database, e.g. if you increase
    // the database version
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion,
                          int newVersion) {
        if (oldVersion <= 1 && 2 <= newVersion) {
            Log.w("Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will add a status table");
            database.execSQL("CREATE TABLE IF NOT EXISTS status (name text, value text);");
        }
        if (oldVersion <= 5 && 6 <= newVersion) {
            Log.w("Upgrading database: Adding trust columns");
            for (String column : Arrays.asList(
                    SSLDroidDbAdapter.TUNNEL_TRUST_TYPE,
                    SSLDroidDbAdapter.TUNNEL_TRUST_FILE,
                    SSLDroidDbAdapter.TUNNEL_TRUST_PASS
            )) {
                database.execSQL(String.format(
                        "ALTER TABLE %s add column %s",
                        SSLDroidDbAdapter.TUNNELS_TABLE,
                        column
                ));
            }
        }
        onCreate(database);
    }
}
