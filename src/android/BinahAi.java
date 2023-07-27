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

import javax.security.auth.callback.Callback;

import ai.binah.sdk.api.HealthMonitorException;
import ai.binah.sdk.api.session.Session;
import ai.binah.sdk.api.session.SessionState;
import ai.binah.sdk.api.session.demographics.Sex;

public class BinahAi extends CordovaPlugin implements CameraActivity.ImagePreviewListener {
  private static final String TAG = "BinahAi";
  private Session mSession;
  private static final long MEASUREMENT_DURATION = 120;

  private static final String START_CAMERA = "startCamera";
  private static final String STOP_CAMERA = "stopCamera";
  private static final String START_SCAN = "startScan";
  private static final String STOP_SCAN = "stopScan";
  private static final String IMAGE_VALIDATION = "imageValidation";

  private CallbackContext startCameraCallbackContext;
  private CallbackContext stopCameraCallbackContext;
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
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          startCamera(licenseKey, callbackContext);
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
    }
    return false;
  }

  @Override
  public void onStop() {
    Log.d(TAG, "MAIN: ON STOPPING");
    super.onStop();
    if(mSession != null){
      Log.d(TAG, "MAIN: SESSION NOT NULL");
      mSession.terminate();
      mSession = null;
    }
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

    return true;
  }

  private boolean startCamera(String licenseKey, CallbackContext callbackContext){
    startCameraCallbackContext = callbackContext;
    final float opacity = Float.parseFloat("1");
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
  public void onFinalResult(JSONArray vitalSignsResults) {
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

  private boolean hasView(CallbackContext callbackContext){
    if(fragment == null){
      callbackContext.error("No Preview");
      return false;
    }

    return true;
  }
}
