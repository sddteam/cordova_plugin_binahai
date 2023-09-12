package inc.bastion.binahai;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
  private static final String DATABASE_NAME = "bioscan_result_db";
  private static final int DATABASE_VERSION = 1;

  public DBHelper(Context context){
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String createTableQuery = "CREATE TABLE IF NOT EXISTS ScanResult (" +
      "measurement_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
      "date_time TEXT, "+
      "vital_signs_data TEXT)";
    db.execSQL(createTableQuery);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if(oldVersion < newVersion){
      db.execSQL("DROP TABLE IF EXISTS ScanResult");
      onCreate(db);
    }
  }
}
