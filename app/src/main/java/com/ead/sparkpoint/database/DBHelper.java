package com.ead.sparkpoint.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "sparkpoint.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_USER = "user";
    public static final String COL_ID = "id";
    public static final String COL_USERNAME = "username";
    public static final String COL_EMAIL = "email";
    public static final String COL_ROLE_ID = "roleId";
    public static final String COL_ROLE_NAME = "roleName";
    public static final String COL_NIC = "nic";
    public static final String COL_FIRST_NAME = "first_name";
    public static final String COL_LAST_NAME = "last_name";
    public static final String COL_PASSWORD = "password"; // Be mindful of storing raw passwords
    public static final String COL_PHONE = "phone";
    public static final String COL_ACCESS_TOKEN = "accessToken";
    public static final String COL_REFRESH_TOKEN = "refreshToken";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USER_TABLE = "CREATE TABLE " + TABLE_USER + "("
                + COL_ID + " TEXT PRIMARY KEY,"
                + COL_USERNAME + " TEXT,"
                + COL_EMAIL + " TEXT,"
                + COL_ROLE_ID + " INTEGER,"
                + COL_ROLE_NAME + " TEXT,"
                + COL_FIRST_NAME + " TEXT," // Add column
                + COL_LAST_NAME + " TEXT,"  // Add column
                + COL_PASSWORD + " TEXT,"   // Add column
                + COL_NIC + " TEXT,"
                + COL_PHONE + " TEXT,"      // Add column
                + COL_ACCESS_TOKEN + " TEXT,"
                + COL_REFRESH_TOKEN + " TEXT" + ")";
        db.execSQL(CREATE_USER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
    }


}
