package hu.blint.ssldroid.db;

import android.content.ContentValues;
import android.database.Cursor;

public abstract class Column<T> {
    private final String name;
    private final String type;
    private final String sqlDeclaration;

    private Column(String name, String type) {
        this.name = name;
        this.type = type;
        this.sqlDeclaration = name + " " + type;
    }


    public String getName() {
        return name;
    }

    public abstract T get(Cursor cursor);
    public abstract void put(ContentValues values, T value);

    private static String getString(final Cursor cursor, final String name) {
        return cursor.getString(cursor.getColumnIndexOrThrow(name));
    }

    public static Column<Long> id(final String name) {
        return new Column<Long>(name, "integer primary key autoincrement") {
            @Override
            public Long get(Cursor cursor) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(name));
            }

            @Override
            public void put(ContentValues values, Long value) {
                values.put(name, value);
            }
        };
    }

    public static Column<String> string(final String name) {
        return new Column<String>(name, "text not null") {
            @Override
            public String get(Cursor cursor) {
                return getString(cursor, name);
            }

            @Override
            public void put(ContentValues values, String value) {
                values.put(name, value);
            }
        };
    }

    public static Column<String> optionalString(final String name) {
        return new Column<String>(name, "text") {
            @Override
            public String get(Cursor cursor) {
                return getString(cursor, name);
            }

            @Override
            public void put(ContentValues values, String value) {
                values.put(name, value);
            }
        };
    }

    public static Column<Integer> integer(final String name) {
        return new Column<Integer>(name, "integer not null") {
            @Override
            public Integer get(Cursor cursor) {
                return cursor.getInt(cursor.getColumnIndexOrThrow(name));
            }

            @Override
            public void put(ContentValues values, Integer value) {
                values.put(name, value);
            }
        };
    }

    public static <E extends Enum<E>> Column<E> en(final String name, final Class<E> values, final E def) {
        return new Column<E>(name, "text") {
            @Override
            public E get(Cursor cursor) {
                final String value = getString(cursor, name);
                return  value != null ? Enum.valueOf(values, value) : def;
            }

            @Override
            public void put(ContentValues values, E value) {
                values.put(name, value.name());
            }
        };
    }

    public String getSqlDeclaration() {
        return sqlDeclaration;
    }
}
