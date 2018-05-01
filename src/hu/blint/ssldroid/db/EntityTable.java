package hu.blint.ssldroid.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public abstract class EntityTable<T> {
    private final String name;
    private final Column<Long> idColumn;
    private final String[] columnNames;
    private final String createTable;

    public EntityTable(String name, Column<Long> idColumn, Column<?>... columns) {
        this.name = name;
        this.idColumn = idColumn;

        columnNames = new String[columns.length + 1];
        columnNames[0] = idColumn.getName();
        for (int i = 0; i < columns.length; i++) {
            columnNames[i + 1] = columns[i].getName();
        }

        final StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS " + name + " (" + idColumn.getSqlDeclaration());
        for (Column<?> column : columns) {
            sb.append(", ").append(column.getSqlDeclaration());
        }
        createTable = sb.append(")").toString();
    }

    protected abstract T load(Cursor cursor);
    protected abstract void save(ContentValues values, T value);

    public long create(SQLiteDatabase database, T value) {
        return database.insert(name, null, save(value));
    }

    public T read(SQLiteDatabase database, long id) {
        Cursor cursor = database.query(name, columnNames, where(id), null, null, null, null);
        try {
            if (!cursor.moveToNext()) {
                throw new SQLException("Row id=" + id + " not found");
            }
            return load(cursor);
        } finally {
            cursor.close();
        }
    }

    public List<T> readAll(SQLiteDatabase database) {
        Cursor cursor = database.query(name, columnNames, null, null, null, null, null);
        final List<T> tunnels = new ArrayList<T>();
        try {
            while (cursor.moveToNext()) {
                tunnels.add(load(cursor));
            }
            return tunnels;
        } finally {
            cursor.close();
        }
    }

    public void update(SQLiteDatabase database, long id, T value) {
        database.update(name, save(value), where(id), null);
    }

    public void delete(SQLiteDatabase database, long rowId) {
        database.delete(name, where(rowId), null);
    }

    private String where(long rowId) {
        return idColumn.getName() + "=" + rowId;
    }

    private ContentValues save(T value) {
        ContentValues values = new ContentValues();
        save(values, value);
        return values;
    }

    public void addColumns(final SQLiteDatabase database, final Column<?>... columns) {
        for (Column<?> column : columns) {
            database.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s", name, column.getSqlDeclaration()));
        }
    }

    public void createTable(SQLiteDatabase database) {
        database.execSQL(createTable);
    }
}
