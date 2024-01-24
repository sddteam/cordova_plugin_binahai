package inc.bastion.binahai;

import android.content.Intent;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ai.binah.sdk.api.HealthMonitorException;
import ai.binah.sdk.api.session.Session;
import ai.binah.sdk.api.session.SessionState;
import ai.binah.sdk.api.vital_signs.VitalSignConfidence;
import ai.binah.sdk.api.vital_signs.VitalSignTypes;
import ai.binah.sdk.api.vital_signs.vitals.RRI;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignBloodPressure;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignHemoglobin;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignHemoglobinA1C;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignLFHF;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignMeanRRI;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignOxygenSaturation;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignPNSIndex;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignPNSZone;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignPRQ;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignPulseRate;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignRMSSD;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignRRI;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignRespirationRate;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignSD1;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignSD2;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignSDNN;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignSNSIndex;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignSNSZone;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignStressIndex;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignStressLevel;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignWellnessIndex;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignWellnessLevel;

public class BinahAi extends CordovaPlugin implements CameraActivity.ImagePreviewListener {
  private static final String TAG = "BinahAi";
  private Session mSession;
  private static long MEASUREMENT_DURATION = 120;

  private static final String START_CAMERA = "startCamera";
  private static final String STOP_CAMERA = "stopCamera";
  private static final String START_SCAN = "startScan";
  private static final String STOP_SCAN = "stopScan";
  private static final String IMAGE_VALIDATION = "imageValidation";
  private static final String GET_SESSION_STATE = "getSessionState";
  private static final String USER_FACE_VALIDATION = "userFaceValidation";
  private static final String GET_ALL_MEASUREMENT = "getAllMeasurement";
  private static final String GET_MEASUREMENT_BY_ID = "getMeasurementById";
  private static final String GET_MEASUREMENT_BY_DATE_TIME = "getMeasurementByDateTime";
  private static final String DELETE_MEASUREMENT_BY_ID = "deleteMeasurementById";
  private static final String SHARE_RESULT = "shareResult";

  private CallbackContext startCameraCallbackContext;
  private CallbackContext startScanCallbackContext;
  private CallbackContext stopScanCallbackContext;
  private CallbackContext imageValidationCallbackContext;
  private CallbackContext getSessionStateCallbackContext;
  private CallbackContext userFaceValidationCallbackContext;

  private int containerViewId = 20;
  private boolean toBack = true;
  private ViewParent webViewParent;
  private String _base64Image;

  private CameraActivity fragment;
  private DatabaseManager databaseManager;

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (START_CAMERA.equals(action)) {
      String licenseKey = args.getString(0);
      long duration = args.getLong(1);
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          startCamera(licenseKey, duration, callbackContext);
        }
      });
      return true;
    } else if (STOP_CAMERA.equals(action)) {
      return stopCamera(callbackContext);
    } else if (START_SCAN.equals(action)) {
      return startScan(callbackContext);
    } else if (STOP_SCAN.equals(action)) {
      return stopScan(callbackContext);
    } else if (IMAGE_VALIDATION.equals(action)) {
      return imageValidation(callbackContext);
    } else if (GET_SESSION_STATE.equals(action)) {
      return getSessionState(callbackContext);
    } else if (USER_FACE_VALIDATION.equals(action)) {
      return userFaceValidation(callbackContext);
    } else if (GET_ALL_MEASUREMENT.equals(action)){
      return getAllMeasurement(callbackContext);
    } else if (GET_MEASUREMENT_BY_DATE_TIME.equals(action)){
      String dateTime = args.getString(0);
      return getMeasurementByDateTime(callbackContext, dateTime);
    } else if (GET_MEASUREMENT_BY_ID.equals(action)){
      String measurementId = args.getString(0);
      return getMeasurementById(callbackContext, measurementId);
    } else if (DELETE_MEASUREMENT_BY_ID.equals(action)){
      String measurementId = args.getString(0);
      return deleteMeasurementById(callbackContext, measurementId);
    } else if (SHARE_RESULT.equals(action)){
      String result = args.getString(0);
      return shareResult(callbackContext, result);
    }
    return false;
  }

  private boolean userFaceValidation(CallbackContext callbackContext){
    userFaceValidationCallbackContext = callbackContext;

    return true;
  }

  private boolean startCamera(String licenseKey, long duration, CallbackContext callbackContext){
    startCameraCallbackContext = callbackContext;
    final float opacity = Float.parseFloat("1");
    MEASUREMENT_DURATION = duration;
    fragment = new CameraActivity();
    fragment.setEventListener(this);
    fragment.licenseKey = licenseKey;

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        //create or update the layout params for the container view
        FrameLayout containerView = (FrameLayout)cordova.getActivity().findViewById(containerViewId);
        if(containerView == null){
          containerView = new FrameLayout(cordova.getActivity().getApplicationContext());
          containerView.setId(containerViewId);

          FrameLayout.LayoutParams containerLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
          cordova.getActivity().addContentView(containerView, containerLayoutParams);
        }

        //display camera below the webview
        if(toBack){
          View view = webView.getView();
          ViewParent rootParent = containerView.getParent();
          ViewParent curParent = view.getParent();

          view.setBackgroundColor(0x00000000);

          // If parents do not match look for.
          if(curParent.getParent() != rootParent) {
            while(curParent != null && curParent.getParent() != rootParent) {
              curParent = curParent.getParent();
            }

            if(curParent != null) {
              ((ViewGroup)curParent).setBackgroundColor(0x00000000);
              ((ViewGroup)curParent).bringToFront();
            } else {
              // Do default...
              curParent = view.getParent();
              webViewParent = curParent;
              ((ViewGroup)view).bringToFront();
            }
          }else{
            // Default
            webViewParent = curParent;
            ((ViewGroup)curParent).bringToFront();
          }

        }else{
          //set view back to front
          containerView.setAlpha(opacity);
          containerView.bringToFront();
        }


        FragmentManager fragmentManager = cordova.getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(containerView.getId(), fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
      }
    });

    return true;
  }

  private boolean stopCamera(CallbackContext callbackContext){
    if(webViewParent != null){
      cordova.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          ((ViewGroup)webView.getView()).bringToFront();
          webViewParent = null;
        }
      });
    }

    if(this.hasView(callbackContext) == false){
      return true;
    }

    FragmentManager fragmentManager = cordova.getActivity().getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.remove(fragment);
    fragmentTransaction.commit();

    callbackContext.success();

    if(getSessionStateCallbackContext != null){
      PluginResult getSessionStatePluginResult = new PluginResult(PluginResult.Status.OK);
      getSessionStatePluginResult.setKeepCallback(false);
      getSessionStateCallbackContext.sendPluginResult(getSessionStatePluginResult);
    }
    return true;
  }

  private boolean startScan(CallbackContext callbackContext) {
    try {
      if (mSession.getState() == SessionState.READY) {
        startScanCallbackContext = callbackContext;
        mSession.start(MEASUREMENT_DURATION);
      }
    } catch (HealthMonitorException e) {
      //Log.d(TAG, "Start scan error: " + e.getErrorCode());
      startScanCallbackContext.error(e.getErrorCode());
    }

    return true;
  }

  private boolean stopScan(CallbackContext callbackContext){
    if(mSession == null){
      return false;
    }
    try{
      if(mSession.getState() != SessionState.READY){
        stopScanCallbackContext = callbackContext;
        mSession.stop();
        stopScanCallbackContext.success();
      }
    }catch(HealthMonitorException e){
      Log.d(TAG, "Stop scan error: " + e.getErrorCode());
      stopScanCallbackContext.error("Stop scan error: " + e.getErrorCode());
    }
    return true;
  }

  private boolean imageValidation(CallbackContext callbackContext){
    imageValidationCallbackContext = callbackContext;

    return true;
  }

  private boolean getSessionState(CallbackContext callbackContext){
    getSessionStateCallbackContext = callbackContext;

    return true;
  }

  private boolean getAllMeasurement(CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        try{
          databaseManager = DatabaseManager.getInstance(cordova.getActivity().getApplicationContext());
          ResultDataAccessObject resultDAO = new ResultDataAccessObject(databaseManager);

          List<ScanResult> scanResults = resultDAO.getAllResults();
          JSONArray jsonArray = new JSONArray();

          for(ScanResult scanResult : scanResults){
            JSONObject jsonObject = parseVitalSignData(scanResult);
            jsonArray.put(jsonObject);
          }

          File file = new File(cordova.getContext().getFilesDir(), "vital_signs_data.json");
          FileOutputStream fos = new FileOutputStream(file);
          fos.write(jsonArray.toString().getBytes());
          fos.close();

          cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              callbackContext.success(jsonArray);
            }
          });
        }catch (JSONException | IOException e){
          e.printStackTrace();
        }
      }
    });
    return true;
  }

  private boolean getMeasurementByDateTime(CallbackContext callbackContext, String dateTime){
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        try{
          databaseManager = DatabaseManager.getInstance(cordova.getActivity().getApplicationContext());
          ResultDataAccessObject resultDAO = new ResultDataAccessObject(databaseManager);

          String startDateTime = dateTime + " 00:00:00";
          String endDateTime = dateTime + " 23:59:59";

          List<ScanResult> scanResults = resultDAO.getResultsByDateTimeRange(startDateTime, endDateTime);
          JSONArray jsonArray = new JSONArray();

          for (ScanResult scanResult : scanResults) {
            JSONObject jsonObject = parseVitalSignData(scanResult);
            jsonArray.put(jsonObject);
          }

          cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              callbackContext.success(jsonArray);
            }
          });
        }catch (JSONException e){
          e.printStackTrace();
        }
      }
    });
    return true;
  }

  private boolean getMeasurementById(CallbackContext callbackContext, String measurementId){
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        databaseManager = DatabaseManager.getInstance(cordova.getActivity().getApplicationContext());
        ResultDataAccessObject resultDAO = new ResultDataAccessObject(databaseManager);

        ScanResult scanResult = resultDAO.getResultsByMeasurementId(measurementId);
        JSONObject jsonObject = parseVitalSignData(scanResult);

        cordova.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            callbackContext.success(jsonObject);
          }
        });
      }
    });
    return true;
  }

  private boolean deleteMeasurementById(CallbackContext callbackContext, String measurementId){
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        databaseManager = DatabaseManager.getInstance(cordova.getActivity().getApplicationContext());
        ResultDataAccessObject resultDAO = new ResultDataAccessObject(databaseManager);

        resultDAO.deleteResult(measurementId);

        callbackContext.success();
      }
    });

    return true;
  }

  private boolean shareResult(CallbackContext callbackContext, String result){
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, result);
        Intent chooseIntent = Intent.createChooser(shareIntent, "Share via");
        if(shareIntent.resolveActivity(cordova.getActivity().getPackageManager()) != null){
          cordova.getActivity().startActivity(chooseIntent);
        }

        callbackContext.success();
      }
    });
    return true;
  }

  private JSONObject parseVitalSignData(ScanResult scanResult){
    JSONObject vitalInfo = getVitalInfo();
    JSONObject jsonObject = new JSONObject();
    try{
      jsonObject.put("measurement_id", scanResult.getMeasurement_id());
      jsonObject.put("user_id", scanResult.getUser_id());
      jsonObject.put("date_time", scanResult.getDate_time());

      Iterator<String> keys = scanResult.getVital_signs_data().keys();
      while (keys.hasNext()) {
        String key = keys.next();
        String signTypeName = SignTypeNames.SIGN_TYPE_NAMES.get(Integer.parseInt(key));
        vitalInfo.getJSONObject(signTypeName).put("value", scanResult.getVital_signs_data().get(key));
        vitalInfo.getJSONObject(signTypeName).put("level", getVitalSignLevel(String.valueOf(scanResult.getVital_signs_data().get(key)), Integer.parseInt(key)));
      }
      jsonObject.put("vital_signs_data", vitalInfo);

    }catch(JSONException e){
      e.printStackTrace();
    }

    return jsonObject;
  }

  private JSONObject getVitalInfo(){
    AssetManager assetManager = cordova.getActivity().getAssets();
    JSONObject jsonObject = new JSONObject();
    try{
      InputStream inputStream = assetManager.open("vital_info.json");
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
      StringBuilder stringBuilder = new StringBuilder();
      String line;

      while ((line = bufferedReader.readLine()) != null){
        stringBuilder.append(line);
      }

      String jsonData = stringBuilder.toString();
      jsonObject = new JSONObject(jsonData);
    }catch(IOException | JSONException e){
      e.printStackTrace();
    }

    return jsonObject;
  }

  private String getVitalSignLevel(String values, int key){
    double value = 0;
    try{
      value = Double.parseDouble(values);
    }catch (NumberFormatException e){

    }
    switch (key) {
      case VitalSignTypes.BLOOD_PRESSURE:
        String[] str = values.split("/");
        int systolic = Integer.parseInt(str[0].trim());
        return determineSignLevel(systolic, 100, 129);
      case VitalSignTypes.PULSE_RATE:
        return determineSignLevel(value, 60, 100);
      case VitalSignTypes.LFHF:
      case VitalSignTypes.MEAN_RRI:
        return determineSignLevel(value, 600, 1000);
      case VitalSignTypes.OXYGEN_SATURATION:
        return determineSignLevel(value, 95, 100);
      case VitalSignTypes.PNS_INDEX:
      case VitalSignTypes.SNS_INDEX:
        return determineSignLevel(value, -1, 1);
      case VitalSignTypes.PNS_ZONE:
      case VitalSignTypes.WELLNESS_LEVEL:
      case VitalSignTypes.STRESS_LEVEL:
      case VitalSignTypes.SNS_ZONE:
        if (values != null && !values.isEmpty()) {
          return values.substring(0, 1).toUpperCase() + values.substring(1).toLowerCase();
        }
      case VitalSignTypes.PRQ:
        return determineSignLevel(value, 4, 5);
      case VitalSignTypes.RMSSD:
        return determineSignLevel(value, 25, 43);
      case VitalSignTypes.RRI:
        break;
      case VitalSignTypes.RESPIRATION_RATE:
        return determineSignLevel(value, 12, 20);
      case VitalSignTypes.SD1:
        return determineSignLevel(value, 10, 25);
      case VitalSignTypes.SD2:
        return determineSignLevel(value, 20, 40);
      case VitalSignTypes.SDNN:
        return determineSignLevel(value, 50, 50);
      case VitalSignTypes.STRESS_INDEX:
        break;
      case VitalSignTypes.WELLNESS_INDEX:
        break;
      case VitalSignTypes.HEMOGLOBIN:
        return determineSignLevel(value, 14, 18);
      case VitalSignTypes.HEMOGLOBIN_A1C:
        String state;
        if(value < 5.7){
          state = "Normal";
        }else if(value >= 6.4){
          state = "Prediabetes Risk";
        }else{
          state = "Diabetes Risk";
        }
        return state;
    }
    return null;
  }

  private String determineSignLevel(double value, int lowThreshold, int highThreshold){
    if(value < lowThreshold){
      return "Low";
    }else if(value <= highThreshold){
      return "Normal";
    }else{
      return "High";
    }
  }

  @Override
  public void onStartScan(JSONObject vitalSign) {
    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, vitalSign);
    pluginResult.setKeepCallback(true);
    startScanCallbackContext.sendPluginResult(pluginResult);
  }

  @Override
  public void onFinalResult(long vitalSignsResults) {
    if(imageValidationCallbackContext != null){
      PluginResult imageValidationPluginResult = new PluginResult(PluginResult.Status.OK);
      imageValidationPluginResult.setKeepCallback(false);
      imageValidationCallbackContext.sendPluginResult(imageValidationPluginResult);
    }

    if(startScanCallbackContext != null){
      PluginResult startScanPluginResult = new PluginResult(PluginResult.Status.OK, vitalSignsResults);
      startScanPluginResult.setKeepCallback(false);
      startScanCallbackContext.sendPluginResult(startScanPluginResult);
    }
  }

  @Override
  public void onImageValidation(int imageErrorCode) {
    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, imageErrorCode);
    pluginResult.setKeepCallback(true);
    if(imageValidationCallbackContext != null){
      imageValidationCallbackContext.sendPluginResult(pluginResult);
    }
  }

  @Override
  public void onSessionState(String sessionState) {
    if(getSessionStateCallbackContext != null){
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, sessionState);
      pluginResult.setKeepCallback(true);
      getSessionStateCallbackContext.sendPluginResult(pluginResult);
    }
  }

  @Override
  public void onBNHCameraStarted(Session session) {
    this.mSession = session;

    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "Camera started");
    pluginResult.setKeepCallback(true);
    startCameraCallbackContext.sendPluginResult(pluginResult);
  }

  @Override
  public void onBNHCameraError(HealthMonitorException e) {
    Log.d(TAG, "Start camera error: " + e.getErrorCode());

    startCameraCallbackContext.error("Start camera error: " + e.getErrorCode());
  }

  @Override
  public void onBNHWarning(int warningCode) {
    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, warningCode);
    pluginResult.setKeepCallback(true);
    startCameraCallbackContext.sendPluginResult(pluginResult);
  }

  @Override
  public void onBNHError(int errorCode) {
    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, errorCode);
    pluginResult.setKeepCallback(true);
    startCameraCallbackContext.sendPluginResult(pluginResult);
  }

  @Override
  public void onFaceValidation(Bitmap image) {
    if(userFaceValidationCallbackContext != null){
      String base64Image = bitmapToBase64(image);
      _base64Image = base64Image;
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, _base64Image);
      pluginResult.setKeepCallback(true);
      userFaceValidationCallbackContext.sendPluginResult(pluginResult);
    }
  }

  private boolean hasView(CallbackContext callbackContext){
    if(fragment == null){
      callbackContext.error("No Preview");
      return false;
    }

    return true;
  }

  private String bitmapToBase64(Bitmap bitmapImage) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
    byte[] imageBytes = baos.toByteArray();
    return Base64.encodeToString(imageBytes, Base64.DEFAULT);
  }

}
