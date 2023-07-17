package inc.bastion.binahai;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ai.binah.sdk.api.HealthMonitorException;
import ai.binah.sdk.api.session.Session;
import ai.binah.sdk.api.session.SessionState;
import ai.binah.sdk.api.session.demographics.Sex;

public class BinahAi extends CordovaPlugin implements CameraActivity.ImagePreviewListener {
  private static final String TAG = "BinahAi";
  private Session mSession;
  private static final long MEASUREMENT_DURATION = 120;

  private static final String START_CAMERA = "startCamera";
  private static final String START_SCAN = "startScan";
  private static final String STOP_SCAN = "stopScan";
  private static final String IMAGE_VALIDATION = "imageValidation";

  private CallbackContext startCameraCallbackContext;
  private CallbackContext startScanCallbackContext;
  private CallbackContext stopScanCallbackContext;
  private CallbackContext imageValidationCallbackContext;

  private int containerViewId = 20;
  private boolean toBack = true;
  private ViewParent webViewParent;

  private CameraActivity fragment;

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (START_CAMERA.equals(action)){
      String licenseKey = args.getString(0);
      String sex = args.getString(1);
      Double age = args.getDouble(2);
      Double weight = args.getDouble(3);
      return startCamera(licenseKey, sex, age, weight, callbackContext);
    }else if(START_SCAN.equals(action)){
      return startScan(callbackContext);
    }else if(STOP_SCAN.equals(action)){
      return stopScan(callbackContext);
    }else if(IMAGE_VALIDATION.equals(action)){
      return imageValidation(callbackContext);
    }
    return false;
  }

  private boolean startCamera(String licenseKey, String sex, Double age, Double weight, CallbackContext callbackContext){
    startCameraCallbackContext = callbackContext;
    final float opacity = Float.parseFloat("1");
    fragment = new CameraActivity();
    fragment.setEventListener(this);
    fragment.licenseKey = licenseKey;
    fragment.age = age;
    fragment.weight = weight;
    Sex sSex;
    switch (sex){
      case "MALE":
        sSex = Sex.MALE;
        break;
      case  "FEMALE":
        sSex = Sex.FEMALE;
        break;
      default:
        sSex = Sex.UNSPECIFIED;
        break;
    }
    fragment.sex = sSex;

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

  private boolean startScan(CallbackContext callbackContext) {
    startScanCallbackContext = callbackContext;
    startSession();

    return true;
  }

  private boolean stopScan(CallbackContext callbackContext){
    stopScanCallbackContext = callbackContext;
    stopSession();
    stopScanCallbackContext.success("SCAN STOPPED");
    return true;
  }

  private boolean imageValidation(CallbackContext callbackContext){
    imageValidationCallbackContext = callbackContext;

    return true;
  }

  @Override
  public void onSessionCreated(Session session) {
    this.mSession = session;
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
  public void onImageValidation(int errorCode) {
    JSONObject result = new JSONObject();
    try{
      result.put("imageValidationError", errorCode);
    }catch (JSONException e){
      e.printStackTrace();
    }

    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
    pluginResult.setKeepCallback(true);
    imageValidationCallbackContext.sendPluginResult(pluginResult);
  }

  private void startSession() {
    if (mSession == null) {
      return;
    }

    try {
      if (mSession.getState() == SessionState.READY) {
        mSession.start(MEASUREMENT_DURATION);
      }
    } catch (HealthMonitorException e) {
      Log.d(TAG, "Error: " + e.getErrorCode());
    }
  }

  private void stopSession(){
    if(mSession == null){
      return;
    }
    try{
      if(mSession.getState() != SessionState.READY){
        mSession.stop();
      }
    }catch(HealthMonitorException e){
      Log.d(TAG, "Error: " + e.getErrorCode());
    }
  }
}
