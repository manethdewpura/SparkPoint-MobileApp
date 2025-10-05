package com.ead.sparkpoint.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ead.sparkpoint.models.AppUser;

public class AppUserDAO {
    private DBHelper dbHelper;

    public AppUserDAO(Context context) {
        dbHelper = new DBHelper(context);
    }

    public void insertOrUpdateUser(AppUser user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_ID, user.getId());
        values.put(DBHelper.COL_USERNAME, user.getUsername());
        values.put(DBHelper.COL_EMAIL, user.getEmail());
        values.put(DBHelper.COL_ROLE_ID, user.getRoleId());
        values.put(DBHelper.COL_ROLE_NAME, user.getRoleName());
        values.put(DBHelper.COL_FIRST_NAME, user.getFirstName());
        values.put(DBHelper.COL_LAST_NAME, user.getLastName());
        values.put(DBHelper.COL_PASSWORD, user.getPassword());
        values.put(DBHelper.COL_NIC, user.getNic());
        values.put(DBHelper.COL_PHONE, user.getPhone());
        values.put(DBHelper.COL_ACCESS_TOKEN, user.getAccessToken());
        values.put(DBHelper.COL_REFRESH_TOKEN, user.getRefreshToken());

        db.insertWithOnConflict(DBHelper.TABLE_USER, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        // Do NOT close the database here
    }

    public AppUser getUser() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] columns = {
                DBHelper.COL_ID, DBHelper.COL_USERNAME, DBHelper.COL_EMAIL,
                DBHelper.COL_ROLE_ID, DBHelper.COL_ROLE_NAME,
                DBHelper.COL_FIRST_NAME, DBHelper.COL_LAST_NAME, DBHelper.COL_PASSWORD,
                DBHelper.COL_NIC, DBHelper.COL_PHONE,
                DBHelper.COL_ACCESS_TOKEN, DBHelper.COL_REFRESH_TOKEN
        };
        Cursor cursor = db.query(DBHelper.TABLE_USER, columns, null, null, null, null, null);

        AppUser user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = new AppUser(
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_USERNAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_EMAIL)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_ROLE_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_ROLE_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_FIRST_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LAST_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PASSWORD)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_NIC)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PHONE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_ACCESS_TOKEN)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_REFRESH_TOKEN))
            );
        }
        if (cursor != null) {
            cursor.close(); // Only close the cursor
        }
        // Do NOT close the database here
        return user;
    }

    public void clearUsers() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DBHelper.TABLE_USER, null, null);
        // Do NOT close the database here
    }
}
