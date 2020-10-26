package com.example.gesturerecognition;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class CacheManager extends SQLiteOpenHelper {

    private static final String tableName = "Cache";
    private static final String TypeCol = "Col0";
    private static final String XCol= "Col1";
    private static final String YCol = "Col2";
    private static final String ZCol = "Col3";



    public CacheManager(Context context) {
        super(context, tableName, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createTable = "CREATE TABLE " + tableName +
//                " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " (" +
                TypeCol +" TEXT,"  +
                XCol +" FlOAT,"  +
                YCol +" FLOAT,"  +
                ZCol +" FLOAT)";
        sqLiteDatabase.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + sqLiteDatabase);
        onCreate(sqLiteDatabase);
    }


    public long addEntry(Object mCachedObject) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(this.TypeCol,mCachedObject.getType());
        contentValues.put(this.XCol,mCachedObject.getX());
        contentValues.put(this.YCol,mCachedObject.getY());
        contentValues.put(this.ZCol,mCachedObject.getZ());

        long result = db.insert(this.tableName, null, contentValues);
        return result;
    }
    private Cursor getData(SQLiteDatabase db){

        String query = "SELECT * FROM " + tableName;
        Cursor data = db.rawQuery(query, null);
        return data;
    }
    public List<String[]> getDataComplete()
    {
        SQLiteDatabase db=this.getWritableDatabase();
        Cursor cursor=this.getData(db);
        List<String[]> data = new ArrayList<String[]>();
        for(cursor.moveToLast();;)
        {
            data.add(new String[]{cursor.getString(0), cursor.getString(1),cursor.getString(2),cursor.getString(3   )});
//            Log.d("hey1",cursor.getString(0)+" "+cursor.getString(1));
            if(!cursor.moveToPrevious())
                break;

        }
//        db.execSQL("DROP TABLE IF EXISTS " + db);
        return data;
    }

    public void afterSync()
    {
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL("delete from "+ tableName);
    }
}
