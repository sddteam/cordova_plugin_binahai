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
  private static final int MAX_TABLE_ROWS = 3;
  private DatabaseManager databaseManager;

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

    return db.insert("ScanResult", null, values);
  }

  public void deleteResult(long measurement_id){
    SQLiteDatabase db = databaseManager.getDatabase();
    db.delete("ScanResult", "measurement_id = ?", new String[]{String.valueOf(measurement_id)});
  }

  public List<ScanResult> getAllResults() throws JSONException {
    List<ScanResult> scanResults = new ArrayList<>();
    SQLiteDatabase db = databaseManager.getDatabase();

    Cursor cursor = db.query("ScanResult", null, null, null, null, null, null);
    if(cursor != null){
      while (cursor.moveToNext()){
        @SuppressLint("Range") long  measurementId = cursor.getLong(cursor.getColumnIndex("measurement_id"));
        @SuppressLint("Range") long userId = cursor.getLong(cursor.getColumnIndex("user_id"));
        @SuppressLint("Range") String dateTime = cursor.getString(cursor.getColumnIndex("date_time"));
        @SuppressLint("Range") JSONObject vitalSignsData = new JSONObject(cursor.getString(cursor.getColumnIndex("vital_signs_data")));

        ScanResult scanResult = new ScanResult();
        scanResult.setMeasurement_id(measurementId);
        scanResult.setUser_id(userId);
        scanResult.setDate_time(dateTime);
        scanResult.setVital_signs_data(vitalSignsData);

        scanResults.add(scanResult);
      }

      cursor.close();
    }

    return scanResults;
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
