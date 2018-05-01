package hu.blint.ssldroid.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import hu.blint.ssldroid.TrustType;
import hu.blint.ssldroid.TunnelConfig;

public class TunnelDb {
    private static final Column<Long> ID = Column.id("_id");
    private static final Column<String> NAME = Column.string("name");
    private static final Column<Integer> LOCAL_PORT = Column.integer("localport");
    private static final Column<String> REMOTE_HOST = Column.string("remotehost");
    private static final Column<Integer> REMOTE_PORT = Column.integer("remoteport");
    private static final Column<String> KEY_FILE = Column.string("pkcsfile");
    private static final Column<String> KEY_PASS = Column.string("pkcspass");
    private static final Column<TrustType> TRUST_TYPE = Column.en("trust_type", TrustType.class, TrustType.ALL);
    private static final Column<String> TRUST_FILE = Column.string("trust_file");
    private static final Column<String> TRUST_PASS = Column.string("trust_pass");

    public static final EntityTable<TunnelConfig> TABLE = new EntityTable<TunnelConfig>(
            "tunnels",
            ID, NAME,
            LOCAL_PORT,
            REMOTE_HOST, REMOTE_PORT,
            KEY_FILE, KEY_PASS,
            TRUST_TYPE, TRUST_FILE, TRUST_PASS
    ) {
        @Override
        protected TunnelConfig load(Cursor cursor) {
            return new TunnelConfig(
                    ID.get(cursor), NAME.get(cursor),
                    LOCAL_PORT.get(cursor),
                    REMOTE_HOST.get(cursor), REMOTE_PORT.get(cursor),
                    KEY_FILE.get(cursor), KEY_PASS.get(cursor),
                    TRUST_TYPE.get(cursor), TRUST_FILE.get(cursor), TRUST_PASS.get(cursor)
            );
        }

        @Override
        protected void save(ContentValues values, TunnelConfig tunnel) {
            NAME.put(values, tunnel.name);
            LOCAL_PORT.put(values, tunnel.listenPort);
            REMOTE_HOST.put(values, tunnel.targetHost);
            REMOTE_PORT.put(values, tunnel.targetPort);
            KEY_FILE.put(values, tunnel.keyFile);
            KEY_PASS.put(values, tunnel.keyPass);
            TRUST_TYPE.put(values, tunnel.trustType);
            TRUST_FILE.put(values, tunnel.trustFile);
            TRUST_PASS.put(values, tunnel.trustPass);
        }
    };

    public static void addTrustColumns(SQLiteDatabase database) {
        TABLE.addColumns(database, TRUST_TYPE, TRUST_FILE, TRUST_PASS);
    }
}
