package inc.bastion.binahai;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserInfoDataAccessObject {
  private DatabaseManager databaseManager;
  private String TABLE_NAME = "UserInfo";

  public UserInfoDataAccessObject(DatabaseManager databaseManager){
    this.databaseManager = databaseManager;
  }

  public long insert(UserInfo userInfo){
    SQLiteDatabase db = databaseManager.getDatabase();

    ContentValues values = new ContentValues();
    values.put("first_name", userInfo.getFirst_name());
    values.put("last_name", userInfo.getLast_name());
    values.put("birthday", userInfo.getBirthday());
    values.put("sex", userInfo.getSex());
    values.put("weight", userInfo.getWeight());
    values.put("height", userInfo.getHeight());

    return db.insert(TABLE_NAME, null, values);
  }

  public void delete(long user_id){
    SQLiteDatabase db = databaseManager.getDatabase();
    db.delete(TABLE_NAME, "user_id = ?", new String[]{String.valueOf(user_id)});
  }

  public void deleteAll() {
    SQLiteDatabase db = databaseManager.getDatabase();
    db.delete(TABLE_NAME, null, null);
  }
}
