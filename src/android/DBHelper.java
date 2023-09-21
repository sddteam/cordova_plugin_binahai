package inc.bastion.binahai;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
  private static final String DATABASE_NAME = "bioscan_result_database";
  private static final int DATABASE_VERSION = 1;

  public DBHelper(Context context){
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String createTableQuery = "CREATE TABLE IF NOT EXISTS ScanResult (" +
      "measurement_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
      "user_id INTEGER,"+
      "date_time TEXT, "+
      "vital_signs_data TEXT)";
    db.execSQL(createTableQuery);

    String createUserInfoTableQuery = "CREATE TABLE IF NOT EXISTS UserInfo (" +
      "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
      "first_name TEXT, " +
      "last_name TEXT, " +
      "birthday TEXT, " +
      "sex TEXT, " +
      "weight INTEGER, " +
      "height INTEGER)";
    db.execSQL(createUserInfoTableQuery);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if(oldVersion < newVersion){
      db.execSQL("DROP TABLE IF EXISTS ScanResult");
      db.execSQL("DROP TABLE IF EXISTS UserInfo");
      onCreate(db);
    }
  }

  public void deleteTable(){
    SQLiteDatabase db = this.getWritableDatabase();

    db.execSQL("DROP TABLE IF EXISTS ScanResult");
  }

  public String[] getAllTables() {
    SQLiteDatabase db = this.getReadableDatabase();
    String[] tables = null;

    try {
      // Query the sqlite_master table to get a list of all tables
      Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

      if (cursor != null) {
        int count = cursor.getCount();
        tables = new String[count];

        int i = 0;
        while (cursor.moveToNext()) {
          String tableName = cursor.getString(0);
          tables[i] = tableName;
          i++;
        }

        cursor.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      db.close();
    }

    return tables;
  }
}
