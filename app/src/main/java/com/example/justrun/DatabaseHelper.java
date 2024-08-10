package com.example.justrun;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {
    // DB file name
    static final String DATABASE_NAME = "MY_DB.DB";
    static final int DATABASE_VERSION = 1;
    static final String DATABASE_TABLE = "RUNS";
    // Columns of the table
    static final String USER_ID = "_ID";

    static final String DATE = "DATE";
    static final String DURATION = "DURATION";
    static final String KILOMETERS = "KILOMETERS";

    // Using classic query to create table
    private static final String CREATE_DB_QUERY = "CREATE TABLE " + DATABASE_TABLE + " ( "
            + USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DATE + ", " + DURATION + ", " + KILOMETERS + ");";


    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_DB_QUERY);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL( "DROP TABLE "+ DATABASE_TABLE);
    }

    public void deleteAndRecreateTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
        db.execSQL(CREATE_DB_QUERY);
        db.close();
    }
}

