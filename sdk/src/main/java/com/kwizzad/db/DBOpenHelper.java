package com.kwizzad.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.kwizzad.log.QLog;

class DBOpenHelper extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "kvstore";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_VALUE = "vv";

    private static final String DATABASE_NAME = "bshkvstore.db";

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_NAME + "(" +
            COLUMN_ID + " text PRIMARY KEY, " +
            COLUMN_VALUE + " text" +
            " );";

    public DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        QLog.d("db open %b", database.isOpen());
        QLog.d("db integrity %b", database.isDatabaseIntegrityOk());

        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        QLog.w("Upgrading database from version %d to %d, which will destroy all old data", oldVersion, newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

}
