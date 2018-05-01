package hu.blint.ssldroid.db;

import android.database.sqlite.SQLiteDatabase;

import java.util.List;

public class EntityDb<T> {
    private final SQLiteDatabase db;
    private final EntityTable<T> table;

    public EntityDb(SQLiteDatabase db, EntityTable<T> table) {
        this.db = db;
        this.table = table;
    }

    public long create(T value) {
        return table.create(db, value);
    }

    public T read(long id) {
        return table.read(db, id);
    }

    public List<T> readAll() {
        return table.readAll(db);
    }

    public void update(long id, T value) {
        table.update(db, id, value);
    }

    public void delete(long id) {
        table.delete(db, id);
    }
}
