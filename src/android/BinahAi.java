package inc.bastion.binahai;

import android.app.Activity;
import android.content.Intent;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * This class echoes a string called from JavaScript.
 */
public class BinahAi extends CordovaPlugin {
  private static final String TAG = "BinahAi";

  private static final String START_CAMERA = "startCamera";
  private static final String START_SCAN = "startScan";

  private CallbackContext startCameraCallbackContext;
  private static final int START_CAMERA_REQUEST_CODE = 69;

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (START_CAMERA.equals(action)){
        return startCamera(callbackContext);
    }else if(START_SCAN.equals(action)){
        return startScan(callbackContext);
    }
    return false;
  }

  private boolean startCamera(CallbackContext callbackContext){
    startCameraCallbackContext = callbackContext;

    cordova.getActivity().runOnUiThread(() -> {
      Intent intent = new Intent(cordova.getActivity(), CameraActivity.class);
      cordova.startActivityForResult(this, intent, START_CAMERA_REQUEST_CODE);
    });

    return true;
  }

  private boolean startScan(CallbackContext callbackContext) {
    callbackContext.success(START_SCAN);
    return true;
  }
}
