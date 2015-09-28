package com.flammekastar.locationapp;

/**
 * Created by Flammekastaren on 27/09/2015.
 */
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.LinkedList;
import java.util.List;

public class SQLiteHelper extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "RunDB";
    private static final String TABLE_RUNS = "runs";

    private static final String KEY_ID = "_id";
    private static final String KEY_DISTANCE = "distance";
    private static final String KEY_TIME = "time";

    private static final String[] COLUMNS = {KEY_ID,KEY_DISTANCE,KEY_TIME};

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // SQL statement to create book table
        String CREATE_RUN_TABLE = "CREATE TABLE runs ( " +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "distance INTEGER, "+
                "time INTEGER )";

        // create books table
        db.execSQL(CREATE_RUN_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older books table if existed
        db.execSQL("DROP TABLE IF EXISTS books");

        // create fresh books table
        this.onCreate(db);
    }

    public void addRun(Run run){

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_DISTANCE, run.getDistance()); // get title
        values.put(KEY_TIME, run.getTime()); // get author

        // 3. insert
        db.insert(TABLE_RUNS, // table
                null, //nullColumnHack
                values); // key/value -> keys = column names/ values = column values

        // 4. close
        db.close();
    }

    public Cursor getAllCursor() {
        String query = "SELECT  * FROM " + TABLE_RUNS;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        return cursor;
    }

    public List<Run> getAllRuns() {
        List<Run> runs = new LinkedList<Run>();

        // 1. build the query
        String query = "SELECT  * FROM " + TABLE_RUNS;

        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // 3. go over each row, build book and add it to list
        Run run = null;
        if (cursor.moveToFirst()) {
            do {
                run = new Run();
                run.setId(Integer.parseInt(cursor.getString(0)));
                run.setDistance(Integer.parseInt(cursor.getString(1)));
                run.setTime(Integer.parseInt(cursor.getString(2)));

                // Add book to books
                runs.add(run);
            } while (cursor.moveToNext());
        }
        // return books
        return runs;
    }
}