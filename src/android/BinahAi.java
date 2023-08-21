package inc.bastion.binahai;

import android.nfc.Tag;
import android.os.Build;
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

import javax.security.auth.callback.Callback;

import ai.binah.sdk.api.HealthMonitorException;
import ai.binah.sdk.api.session.Session;
import ai.binah.sdk.api.session.SessionState;
import ai.binah.sdk.api.session.demographics.Sex;

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

  private CallbackContext startCameraCallbackContext;
  private CallbackContext stopCameraCallbackContext;
  private CallbackContext startScanCallbackContext;
  private CallbackContext stopScanCallbackContext;
  private CallbackContext imageValidationCallbackContext;
  private CallbackContext getSessionStateCallbackContext;

  private int containerViewId = 20;
  private boolean toBack = true;
  private ViewParent webViewParent;

  private CameraActivity fragment;

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (START_CAMERA.equals(action)){
      String licenseKey = args.getString(0);
      long duration = args.getLong(1);
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          startCamera(licenseKey, duration, callbackContext);
        }
      });
      return true;
    }else if(STOP_CAMERA.equals(action)){
      return stopCamera(callbackContext);
    }else if(START_SCAN.equals(action)){
      return startScan(callbackContext);
    }else if(STOP_SCAN.equals(action)){
      return stopScan(callbackContext);
    }else if(IMAGE_VALIDATION.equals(action)){
      return imageValidation(callbackContext);
    }else if(GET_SESSION_STATE.equals(action)){
      return getSessionState(callbackContext);
    }
    return false;
  }

  private boolean startCamera(String licenseKey, long duration, CallbackContext callbackContext){
    startCameraCallbackContext = callbackContext;
    final float opacity = Float.parseFloat("1");
    MEASUREMENT_DURATION = duration;
    fragment = new CameraActivity();
    fragment.setEventListener(this);
    fragment.licenseKey = licenseKey;

    int apiLevel = Build.VERSION.SDK_INT;
    Log.d(TAG, String.valueOf(apiLevel));
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

  @Override
  public void onStartScan(JSONObject vitalSign) {
    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, vitalSign);
    pluginResult.setKeepCallback(true);
    startScanCallbackContext.sendPluginResult(pluginResult);
  }

  @Override
  public void onFinalResult(JSONArray vitalSignsResults) {
    if(imageValidationCallbackContext != null){
      PluginResult imageValidationPluginResult = new PluginResult(PluginResult.Status.OK);
      imageValidationPluginResult.setKeepCallback(false);
      imageValidationCallbackContext.sendPluginResult(imageValidationPluginResult);
    }

    if(getSessionStateCallbackContext != null){
      PluginResult getSessionStatePluginResult = new PluginResult(PluginResult.Status.OK);
      getSessionStatePluginResult.setKeepCallback(false);
      getSessionStateCallbackContext.sendPluginResult(getSessionStatePluginResult);
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
  public void onCameraStarted(Session session) {
    this.mSession = session;

    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "Camera started");
    pluginResult.setKeepCallback(true);
    startCameraCallbackContext.sendPluginResult(pluginResult);
  }

  @Override
  public void onCameraError(HealthMonitorException e) {
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

  private boolean hasView(CallbackContext callbackContext){
    if(fragment == null){
      callbackContext.error("No Preview");
      return false;
    }

    return true;
  }

}
