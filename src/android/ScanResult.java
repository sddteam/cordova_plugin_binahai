package inc.bastion.binahai;

import org.json.JSONObject;

public class ScanResult {
  private long measurement_id;
  private long user_id;
  private String date_time;
  private JSONObject vital_signs_data;

  public ScanResult(){}

  public ScanResult(long user_id, String date_time, JSONObject vital_signs_data){
    this.user_id = user_id;
    this.date_time = date_time;
    this.vital_signs_data = vital_signs_data;
  }

  public long getMeasurement_id(){
    return measurement_id;
  }

  public void setMeasurement_id(long measurement_id){
    this.measurement_id = measurement_id;
  }

  public long getUser_id(){
    return user_id;
  }

  public void setUser_id(long user_id){
    this.user_id = user_id;
  }

  public String getDate_time(){
    return date_time;
  }

  public void setDate_time(String date_time){
    this.date_time = date_time;
  }

  public JSONObject getVital_signs_data(){
    return vital_signs_data;
  }

  public void setVital_signs_data(JSONObject vital_signs_data){
    this.vital_signs_data = vital_signs_data;
  }
}
