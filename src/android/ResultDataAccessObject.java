package inc.bastion.binahai;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ResultDataAccessObject {
  private static final int MAX_TABLE_ROWS = 20;
  private DatabaseManager databaseManager;
  private String TABLE_NAME = "ScanResult";

  public ResultDataAccessObject(DatabaseManager databaseManager){
    this.databaseManager = databaseManager;
  }

  public long insertResult(ScanResult scanResult){
    SQLiteDatabase db = databaseManager.getDatabase();

    int currentRowCount = getCurrentResultCount();

    if(currentRowCount >= MAX_TABLE_ROWS){
      int rowsToDelete = currentRowCount - MAX_TABLE_ROWS + 1;
      deleteOldestRows(rowsToDelete);
    }
    ContentValues values = new ContentValues();
    values.put("user_id", scanResult.getUser_id());
    values.put("date_time", scanResult.getDate_time());
    values.put("vital_signs_data", scanResult.getVital_signs_data().toString());

    return db.insert(TABLE_NAME, null, values);
  }

  public void deleteResult(String measurement_id){
    SQLiteDatabase db = databaseManager.getDatabase();
    db.delete(TABLE_NAME, "measurement_id = ?", new String[]{measurement_id});
  }

  public void deleteAllResults() {
    SQLiteDatabase db = databaseManager.getDatabase();
    db.delete(TABLE_NAME, null, null);
  }

  public List<ScanResult> getAllResults(String userId) throws JSONException {
    List<ScanResult> scanResults = new ArrayList<>();
    SQLiteDatabase db = databaseManager.getDatabase();

    String selection = "user_id = ?";
    String[] selectionArgs = {userId};

    Cursor cursor = db.query(TABLE_NAME, null, selection, selectionArgs, null, null, null);
    if(cursor != null){
      while (cursor.moveToNext()){
        @SuppressLint("Range") long  measurement_id = cursor.getLong(cursor.getColumnIndex("measurement_id"));
        @SuppressLint("Range") String user_id = cursor.getString(cursor.getColumnIndex("user_id"));
        @SuppressLint("Range") String date_time = cursor.getString(cursor.getColumnIndex("date_time"));
        @SuppressLint("Range") JSONObject vital_signs_data = new JSONObject(cursor.getString(cursor.getColumnIndex("vital_signs_data")));

        ScanResult scanResult = new ScanResult();
        scanResult.setMeasurement_id(measurement_id);
        scanResult.setUser_id(user_id);
        scanResult.setDate_time(date_time);
        scanResult.setVital_signs_data(vital_signs_data);

        scanResults.add(scanResult);
      }

      cursor.close();
    }

    return scanResults;
  }

  public List<ScanResult> getResultsByDateTimeRange(String userId, String startDateTime, String endDateTime) throws JSONException {
    List<ScanResult> scanResults = new ArrayList<>();
    SQLiteDatabase db = databaseManager.getDatabase();

    String selection = "user_id = ? AND date_time BETWEEN ? AND ?";
    String[] selectionArgs = {userId, startDateTime, endDateTime};

    Cursor cursor = db.query(TABLE_NAME, null, selection, selectionArgs, null, null, null);

    if(cursor != null){
      while (cursor.moveToNext()) {
        @SuppressLint("Range") long measurement_id = cursor.getLong(cursor.getColumnIndex("measurement_id"));
        @SuppressLint("Range") String user_id = cursor.getString(cursor.getColumnIndex("user_id"));
        @SuppressLint("Range") String date_time = cursor.getString(cursor.getColumnIndex("date_time"));
        @SuppressLint("Range") JSONObject vital_signs_data = new JSONObject(cursor.getString(cursor.getColumnIndex("vital_signs_data")));

        ScanResult scanResult = new ScanResult();
        scanResult.setMeasurement_id(measurement_id);
        scanResult.setUser_id(user_id);
        scanResult.setDate_time(date_time);
        scanResult.setVital_signs_data(vital_signs_data);

        scanResults.add(scanResult);
      }

      cursor.close();
    }

    return scanResults;
  }

  public ScanResult getResultsByMeasurementId(String userId, String measurementId){
    SQLiteDatabase db = databaseManager.getDatabase();
    try{
      String selection = "user_id = ? AND measurement_id = ?";
      String[] selectionArgs = {userId, measurementId};

      Cursor cursor = db.query(TABLE_NAME, null, selection, selectionArgs, null, null, null);

      if(cursor != null && cursor.moveToFirst()){
        @SuppressLint("Range") long measurement_id = cursor.getLong(cursor.getColumnIndex("measurement_id"));
        @SuppressLint("Range") String user_id = cursor.getString(cursor.getColumnIndex("user_id"));
        @SuppressLint("Range") String date_time = cursor.getString(cursor.getColumnIndex("date_time"));
        @SuppressLint("Range") JSONObject vital_signs_data = new JSONObject(cursor.getString(cursor.getColumnIndex("vital_signs_data")));

        ScanResult scanResult = new ScanResult();
        scanResult.setMeasurement_id(measurement_id);
        scanResult.setUser_id(user_id);
        scanResult.setDate_time(date_time);
        scanResult.setVital_signs_data(vital_signs_data);

        cursor.close();

        return scanResult;
      }
    }catch (JSONException e){
      e.printStackTrace();
    }
    return null;
  }

  private int getCurrentResultCount(){
    SQLiteDatabase db = databaseManager.getDatabase();
    Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM ScanResult", null);
    int rowCount = 0;

    if(cursor != null){
      if(cursor.moveToFirst()){
        rowCount = cursor.getInt(0);
      }
      cursor.close();
    }

    return rowCount;
  }

  private void deleteOldestRows(int rowsToDelete){
    SQLiteDatabase db = databaseManager.getDatabase();
    db.execSQL("DELETE FROM ScanResult WHERE measurement_id IN " +
      "(SELECT measurement_id FROM ScanResult ORDER BY measurement_id ASC LIMIT " + rowsToDelete + ")");
  }
}
