package inc.bastion.binahai;

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

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import ai.binah.sdk.api.HealthMonitorException;
import ai.binah.sdk.api.session.Session;
import ai.binah.sdk.api.session.SessionState;

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
  private static final String GET_ALL_HISTORY = "getAllHistory";
  private static final String GET_HISTORY_BY_ID = "getHistoryById";
  private static final String GET_HISTORY_BY_DATE_TIME = "getHistoryByDateTime";
  private static final String GET_VITAL_DESCRIPTION = "getVitalDescription";

  private CallbackContext startCameraCallbackContext;
  private CallbackContext stopCameraCallbackContext;
  private CallbackContext startScanCallbackContext;
  private CallbackContext stopScanCallbackContext;
  private CallbackContext imageValidationCallbackContext;
  private CallbackContext getSessionStateCallbackContext;
  private CallbackContext userFaceValidationCallbackContext;

  private int containerViewId = 20;
  private boolean toBack = true;
  private ViewParent webViewParent;
  private String _base64Image;
  private final int PERMISSION_REQUEST_CODE = 1234;

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
    } else if (GET_ALL_HISTORY.equals(action)){
      return getAllHistory(callbackContext);
    } else if (GET_HISTORY_BY_DATE_TIME.equals(action)){
      String dateTime = args.getString(0);
      return getHistoryByDateTime(callbackContext, dateTime);
    } else if (GET_VITAL_DESCRIPTION.equals(action)){
      return getVitalDescription(callbackContext);
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

  public boolean stopCamera(CallbackContext callbackContext){
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

  private boolean getAllHistory(CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        try{
          databaseManager = DatabaseManager.getInstance(cordova.getActivity().getApplicationContext());
          ResultDataAccessObject resultDAO = new ResultDataAccessObject(databaseManager);

          List<ScanResult> scanResults = resultDAO.getAllResults();
          JSONArray jsonArray = new JSONArray();

          for(ScanResult scanResult : scanResults){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("measurement_id", scanResult.getMeasurement_id());
            jsonObject.put("user_id", scanResult.getUser_id());
            jsonObject.put("date_time", scanResult.getDate_time());
            jsonObject.put("vital_signs_data", scanResult.getVital_signs_data());

            jsonArray.put(jsonObject);
          }

          callbackContext.success(jsonArray);
        }catch (JSONException e){
          e.printStackTrace();
        }
      }
    });
    return true;
  }

  private boolean getHistoryByDateTime(CallbackContext callbackContext, String dateTime){
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
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("measurement_id", scanResult.getMeasurement_id());
            jsonObject.put("user_id", scanResult.getUser_id());
            jsonObject.put("date_time", scanResult.getDate_time());
            jsonObject.put("vital_signs_data", scanResult.getVital_signs_data());

            jsonArray.put(jsonObject);
          }

          callbackContext.success(jsonArray);
        }catch (JSONException e){
          e.printStackTrace();
        }
      }
    });
    return true;
  }

  private boolean getVitalDescription(CallbackContext callbackContext) throws JSONException {
    String[] resourceNames = {
      "blood_pressure",
      "pulse_rate",
      "prq",
      "respiration_rate",
      "wellness_score",
      "hemoglobin",
      "hemoglobin_a1c",
      "oxygen_saturation"
    };

    JSONObject jsonObject = new JSONObject();
    String appResourcePackage = cordova.getActivity().getPackageName();

    for (String resourceName : resourceNames) {
      int resourceId = cordova.getActivity().getResources().getIdentifier(resourceName, "string", appResourcePackage);
      String resourceValue = cordova.getActivity().getString(resourceId);
      jsonObject.put(resourceName, resourceValue);
    }
    callbackContext.success(jsonObject);

    return true;
  }

  @Override
  public void onStartScan(JSONObject vitalSign) {
    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, vitalSign);
    pluginResult.setKeepCallback(true);
    startScanCallbackContext.sendPluginResult(pluginResult);
  }

  @Override
  public void onFinalResult(JSONObject vitalSignsResults) {
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
  public void onImageValidation(JSONObject imageErrorCode) {
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
    PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, errorCode);
    pluginResult.setKeepCallback(false);
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
