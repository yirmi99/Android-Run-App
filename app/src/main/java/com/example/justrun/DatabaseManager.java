package com.example.justrun;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLDataException;

public class DatabaseManager {
    private DatabaseHelper dbHelper;
    private Context context;
    private SQLiteDatabase database;


    public DatabaseManager(Context ctx){
        context = ctx;
    }

    public DatabaseManager open() throws SQLDataException{
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close(){
        dbHelper.close();
    }

    public void insert(String date, String duration, double kilometers){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.DATE, date);
        contentValues.put(DatabaseHelper.DURATION, duration);
        contentValues.put(DatabaseHelper.KILOMETERS, kilometers);
        database.insert(DatabaseHelper.DATABASE_TABLE, null, contentValues);
    }

    public Cursor fetch(){
        String [] columns = new String [] {DatabaseHelper.USER_ID, DatabaseHelper.DATE, DatabaseHelper.DURATION, DatabaseHelper.KILOMETERS};
        Cursor cursor = database.query(DatabaseHelper.DATABASE_TABLE, columns, null,null,null,null,null);
        if (cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public void delete(long _id){
        database.delete(DatabaseHelper.DATABASE_TABLE, DatabaseHelper.USER_ID + "=" + _id, null);
    }


}
