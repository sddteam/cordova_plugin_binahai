package inc.bastion.binahai;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseManager {
  public static DatabaseManager instance;
  private static DBHelper dbHelper;
  private SQLiteDatabase database;

  private DatabaseManager(Context context){
    dbHelper = new DBHelper(context);
    database = dbHelper.getWritableDatabase();
  }

  public static synchronized DatabaseManager getInstance(Context context){
    if(instance == null){
      instance = new DatabaseManager(context);
    }
    return instance;
  }

  public SQLiteDatabase getDatabase(){
    return database;
  }
}
