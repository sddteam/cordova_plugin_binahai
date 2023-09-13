package inc.bastion.binahai;

import java.util.HashMap;
import java.util.Map;

public class SignTypeNames {
  public static final Map<Integer, String> SIGN_TYPE_NAMES = new HashMap<>();

  static {
    SIGN_TYPE_NAMES.put(1, "pulseRate");
    SIGN_TYPE_NAMES.put(2, "respirationRate");
    SIGN_TYPE_NAMES.put(4, "oxygenSaturation");
    SIGN_TYPE_NAMES.put(8, "sdnn");
    SIGN_TYPE_NAMES.put(16, "stressLevel");
    SIGN_TYPE_NAMES.put(32, "rri");
    SIGN_TYPE_NAMES.put(64, "bloodPressure");
    SIGN_TYPE_NAMES.put(128, "stressIndex");
    SIGN_TYPE_NAMES.put(256, "meanRri");
    SIGN_TYPE_NAMES.put(512, "rmssd");
    SIGN_TYPE_NAMES.put(1024, "sd1");
    SIGN_TYPE_NAMES.put(2048, "sd2");
    SIGN_TYPE_NAMES.put(4096, "prq");
    SIGN_TYPE_NAMES.put(8192, "pnsIndex");
    SIGN_TYPE_NAMES.put(16384, "pnsZone");
    SIGN_TYPE_NAMES.put(32768, "snsIndex");
    SIGN_TYPE_NAMES.put(65536, "snsZone");
    SIGN_TYPE_NAMES.put(131072, "wellnessIndex");
    SIGN_TYPE_NAMES.put(262144, "wellnessLevel");
    SIGN_TYPE_NAMES.put(524288, "lfhf");
    SIGN_TYPE_NAMES.put(1048576, "hemoglobin");
    SIGN_TYPE_NAMES.put(2097152, "hemoglobinA1c");

    // Add more sign type names as needed
  }
}
