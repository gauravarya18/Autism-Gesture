package com.example.gesturerecognition;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class CacheManager extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Records";
    private static final String tableName = "Cache";
    private static final String TypeCol = "Col0";
    private static final String XCol= "Col1";
    private static final String YCol = "Col2";
    private static final String ZCol = "Col3";

    private static final String tableNameCSV = "CSVFile";

    private static final String tableNameGesture = "Gestures";
    private static final String Shape = "Col0";

    public CacheManager(Context context) {
        super(context, DATABASE_NAME, null, 1);

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

        String createTableCSV = "CREATE TABLE " + tableNameCSV +
//                " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " (" +
                TypeCol +" TEXT,"  +
                XCol +" FlOAT,"  +
                YCol +" FLOAT,"  +
                ZCol +" FLOAT)";
        sqLiteDatabase.execSQL(createTableCSV);

        String createTableGesture = "CREATE TABLE " + tableNameGesture +
                " (" +
                Shape +" TEXT)";
        sqLiteDatabase.execSQL(createTableGesture);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + sqLiteDatabase);
        onCreate(sqLiteDatabase);
    }

    public long addGesture(String gestureName)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(this.Shape,gestureName);
        long result = db.insert(this.tableNameGesture, null, contentValues);
        return result;
    }

    public List<String> getAllGestures()
    {
        SQLiteDatabase db=this.getWritableDatabase();
        Cursor cursor=this.getData(db,2);
        List<String> data = new ArrayList<String>();
        for(cursor.moveToLast();;)
        {
            data.add(cursor.getString(0));
            if(!cursor.moveToPrevious())
                break;

        }

        return data;
    }

    public long addEntry(Object mCachedObject,int index) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(this.TypeCol,mCachedObject.getType());
        contentValues.put(this.XCol,mCachedObject.getX());
        contentValues.put(this.YCol,mCachedObject.getY());
        contentValues.put(this.ZCol,mCachedObject.getZ());
        if(index==0) {
            long result = db.insert(this.tableName, null, contentValues);
            return result;
        }
        else
        {
            long result = db.insert(this.tableNameCSV, null, contentValues);
            return result;
        }
    }

    private Cursor getData(SQLiteDatabase db,int index){

        String table;
        if(index==0)
        {
            table=tableName;
        }
        else if(index ==1)
        {
            table=tableNameCSV;
        }
        else
        {
            table=tableNameGesture;
        }
        String query = "SELECT * FROM " + table;
        Cursor data = db.rawQuery(query, null);
        return data;
    }
    public List<String[]> getDataComplete()
    {
        SQLiteDatabase db=this.getWritableDatabase();
        Cursor cursor=this.getData(db,0);
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

    public float[][] getTestingData(String s,int index)
    {
        SQLiteDatabase db=this.getWritableDatabase();
        Cursor cursor=this.getData(db,index);
        int len = cursor.getCount()/2;
        float[][] Data = new float[3][len];
        int i=0;
        for(cursor.moveToLast();;)
        {
            if(i==len)
                break;

            if(cursor.getString(0).matches(s)) {
                Data[0][i] = Float.parseFloat(cursor.getString(1));
                Data[1][i] = Float.parseFloat(cursor.getString(2));
                Data[2][i] = Float.parseFloat(cursor.getString(3));
            }
            if(!cursor.moveToPrevious())
                break;
            i=i+1;
        }
        return Data;
    }
    public void afterSync(int index)
    {
        String table;
        if(index==0)
        {
            table=tableName;
        }
        else
        {
            table=tableNameCSV;
        }
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL("delete from "+ table);
    }

    public void deleteAll()
    {
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL("delete from "+ tableName);
        db.execSQL("delete from "+ tableNameGesture);
        db.execSQL("delete from "+ tableNameCSV);
    }
}
