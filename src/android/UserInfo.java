package inc.bastion.binahai;

public class UserInfo {
  private long user_id;
  private String first_name;
  private String last_name;
  private String birthday;
  private String sex;
  private double weight;
  private double height;

  public UserInfo(){}

  public UserInfo(
    long user_id,
    String first_name,
    String last_name,
    String birthday,
    String sex,
    double weight,
    double height){
    this.user_id = user_id;
    this.first_name = first_name;
    this.last_name = last_name;
    this.birthday = birthday;
    this.sex = sex;
    this.weight = weight;
    this.height = height;
  }

  public long getUser_id(){
    return user_id;
  }

  public String getFirst_name(){
    return first_name;
  }

  public String getLast_name(){
    return last_name;
  }

  public String getBirthday(){
    return birthday;
  }

  public String getSex(){
   return sex;
  }

  public double getWeight(){
    return weight;
  }

  public double getHeight(){
    return height;
  }
}
