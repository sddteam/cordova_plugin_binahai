package inc.bastion.binahai;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class BinahAi extends CordovaPlugin {
    private static final String TAG = "BinahAi";

    private static final String LICENSE_KEY = "668765-6009B5-426FAD-D62FC0-D89858-19B9FF";

    private static final String START_CAMERA = "startCamera";
    private static final String START_SCAN = "startScan";

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
        callbackContext.success(START_CAMERA);
        return true;
    }

    private boolean startScan(CallbackContext callbackContext) {
        callbackContext.success(START_SCAN);
        return true;
    }

    
}
