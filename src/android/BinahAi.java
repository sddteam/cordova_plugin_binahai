package inc.bastion.binahai;

import android.app.Activity;
import android.content.Intent;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ai.binah.sdk.api.HealthMonitorException;
import ai.binah.sdk.api.SessionEnabledVitalSigns;
import ai.binah.sdk.api.alerts.ErrorData;
import ai.binah.sdk.api.alerts.WarningData;
import ai.binah.sdk.api.images.ImageListener;
import ai.binah.sdk.api.license.LicenseDetails;
import ai.binah.sdk.api.license.LicenseInfo;
import ai.binah.sdk.api.license.LicenseOfflineMeasurements;
import ai.binah.sdk.api.session.Session;
import ai.binah.sdk.api.session.SessionInfoListener;
import ai.binah.sdk.api.session.SessionState;
import ai.binah.sdk.api.session.demographics.Sex;
import ai.binah.sdk.api.session.demographics.SubjectDemographic;
import ai.binah.sdk.api.vital_signs.VitalSign;
import ai.binah.sdk.api.vital_signs.VitalSignTypes;
import ai.binah.sdk.api.vital_signs.VitalSignsListener;
import ai.binah.sdk.api.vital_signs.VitalSignsResults;
import ai.binah.sdk.api.vital_signs.vitals.SNSZone;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignBloodPressure;
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
import ai.binah.sdk.session.FaceSessionBuilder;

/**
 * This class echoes a string called from JavaScript.
 */
public class BinahAi extends CordovaPlugin implements CameraActivity.ImagePreviewListener {
  private static final String TAG = "BinahAi";
  private Session mSession;
  private static final long MEASUREMENT_DURATION = 60;

  private static final String START_CAMERA = "startCamera";
  private static final String START_SCAN = "startScan";
  private static final String STOP_SCAN = "stopScan";

  private CallbackContext startCameraCallbackContext;
  private CallbackContext startScanCallbackContext;
  private CallbackContext stopScanCallbackContext;

  private int containerViewId = 20;
  private boolean toBack = true;
  private ViewParent webViewParent;

  private CameraActivity fragment;

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (START_CAMERA.equals(action)){
      return startCamera(callbackContext);
    }else if(START_SCAN.equals(action)){
      return startScan(callbackContext);
    }else if(STOP_SCAN.equals(action)){
      return stopScan(callbackContext);
    }
    return false;
  }

  private boolean startCamera(CallbackContext callbackContext){
    startCameraCallbackContext = callbackContext;
    final float opacity = Float.parseFloat("1");
    fragment = new CameraActivity();
    fragment.setEventListener(this);

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

    return true;
  }

  @Override
  public void onSessionCreated(Session session) {
    this.mSession = session;
  }

  @Override
  public void onStartScan(VitalSign vitalSign) {
    JSONObject result = new JSONObject();
    try {
      if (vitalSign.getType() == VitalSignTypes.PULSE_RATE) {
        VitalSignPulseRate pulseRate = (VitalSignPulseRate) vitalSign;
        Log.d(TAG,"PULSE RATE: " + pulseRate.getValue().toString());
        result.put("pulseRate", pulseRate.getValue());
      }
      if (vitalSign.getType() == VitalSignTypes.RESPIRATION_RATE) {
        VitalSignRespirationRate respirationRate = (VitalSignRespirationRate) vitalSign;
        Log.d(TAG,"RESPIRATION RATE: " + respirationRate.getValue().toString());
        result.put("respirationRate", respirationRate.getValue());
      }
      if (vitalSign.getType() == VitalSignTypes.OXYGEN_SATURATION) {
        VitalSignOxygenSaturation oxygenSaturation = (VitalSignOxygenSaturation) vitalSign;
        Log.d(TAG,"OXYGEN SATURATION: " + oxygenSaturation.getValue().toString());
        result.put("oxygenSaturation", oxygenSaturation.getValue());
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
    pluginResult.setKeepCallback(true);
    startScanCallbackContext.sendPluginResult(pluginResult);
  }

  @Override
  public void onFinalResult(VitalSignsResults vitalSignsResults) {
    VitalSignBloodPressure bloodPressure = (VitalSignBloodPressure) vitalSignsResults.getResult(VitalSignTypes.BLOOD_PRESSURE);
    VitalSignPulseRate pulseRate = (VitalSignPulseRate) vitalSignsResults.getResult(VitalSignTypes.PULSE_RATE);
    VitalSignLFHF lfhf = (VitalSignLFHF) vitalSignsResults.getResult(VitalSignTypes.LFHF);
    VitalSignMeanRRI meanRRI = (VitalSignMeanRRI) vitalSignsResults.getResult(VitalSignTypes.MEAN_RRI);
    VitalSignOxygenSaturation oxygenSaturation = (VitalSignOxygenSaturation) vitalSignsResults.getResult(VitalSignTypes.OXYGEN_SATURATION);
    VitalSignPNSIndex pnsIndex = (VitalSignPNSIndex) vitalSignsResults.getResult(VitalSignTypes.PNS_INDEX);
    VitalSignPNSZone pnsZone = (VitalSignPNSZone) vitalSignsResults.getResult(VitalSignTypes.PNS_ZONE);
    VitalSignPRQ prq = (VitalSignPRQ) vitalSignsResults.getResult(VitalSignTypes.PRQ);
    VitalSignRMSSD rmssd = (VitalSignRMSSD) vitalSignsResults.getResult(VitalSignTypes.RMSSD);
    VitalSignRRI rri = (VitalSignRRI) vitalSignsResults.getResult(VitalSignTypes.RRI);
    VitalSignRespirationRate respirationRate = (VitalSignRespirationRate) vitalSignsResults.getResult(VitalSignTypes.RESPIRATION_RATE);
    VitalSignSD1 sd1 = (VitalSignSD1) vitalSignsResults.getResult(VitalSignTypes.SD1);
    VitalSignSD2 sd2 = (VitalSignSD2) vitalSignsResults.getResult(VitalSignTypes.SD2);
    VitalSignSDNN sdnn = (VitalSignSDNN) vitalSignsResults.getResult(VitalSignTypes.SDNN);
    VitalSignSNSIndex snsIndex = (VitalSignSNSIndex) vitalSignsResults.getResult(VitalSignTypes.SNS_INDEX);
    VitalSignSNSZone snsZone = (VitalSignSNSZone)vitalSignsResults.getResult(VitalSignTypes.SNS_ZONE);
    VitalSignStressLevel stressLevel = (VitalSignStressLevel) vitalSignsResults.getResult(VitalSignTypes.STRESS_LEVEL);
    VitalSignStressIndex stressIndex = (VitalSignStressIndex) vitalSignsResults.getResult(VitalSignTypes.STRESS_INDEX);
    VitalSignWellnessIndex wellnessIndex = (VitalSignWellnessIndex) vitalSignsResults.getResult(VitalSignTypes.WELLNESS_INDEX);
    VitalSignWellnessLevel wellnessLevel = (VitalSignWellnessLevel) vitalSignsResults.getResult(VitalSignTypes.WELLNESS_LEVEL);

    String pulseRateValue = pulseRate != null ? pulseRate.getValue().toString() : "N/A";
    String bloodPressureValue = bloodPressure != null ? bloodPressure.getValue().getSystolic() + "/" + bloodPressure.getValue().getDiastolic() : "N/A";
    String lfhfValue = lfhf != null ? lfhf.getValue().toString() : "N/A";
    String meanRRIValue = meanRRI != null ? meanRRI.getValue().toString() : "N/A";
    String oxygenSaturationValue = oxygenSaturation != null ? oxygenSaturation.getValue().toString() : "N/A";
    String pnsIndexValue = pnsIndex != null ? pnsIndex.getValue().toString() : "N/A";
    String pnsZoneValue = pnsZone != null ? pnsZone.getValue().toString() : "N/A";
    String prqValue = prq != null ? prq.getValue().toString() : "N/A";
    String rmssdValue = rmssd != null ? rmssd.getValue().toString() : "N/A";
    String rriValue = rri != null ? rri.getValue().toString() : "N/A";
    String respirationRateValue = respirationRate != null ? respirationRate.getValue().toString() : "N/A";
    String sd1Value = sd1 != null ? sd1.getValue().toString() : "N/A";
    String sd2Value = sd2 != null ? sd2.getValue().toString() : "N/A";
    String sdnnValue = sdnn != null ? sdnn.getValue().toString() : "N/A";
    String snsIndexValue = snsIndex != null ? snsIndex.getValue().toString() : "N/A";
    String snsZoneValue = snsZone != null ? snsZone.getValue().toString() : "N/A";
    String stressLevelValue = stressLevel != null ? stressLevel.getValue().toString() : "N/A";
    String stressIndexValue = stressIndex != null ? stressIndex.getValue().toString() : "N/A";
    String wellnessIndexValue = wellnessIndex != null ? wellnessIndex.getValue().toString() : "N/A";
    String wellnessLevelValue = wellnessLevel != null ? wellnessLevel.getValue().toString() : "N/A";

    JSONObject finalResult = new JSONObject();
    JSONObject liveFinalResult = new JSONObject();
    try {
      finalResult.put("pulseRate", pulseRateValue);
      finalResult.put("bloodPressure", bloodPressureValue);
      finalResult.put("lfhf", lfhfValue);
      finalResult.put("meanRRI", meanRRIValue);
      finalResult.put("oxygenSaturation", oxygenSaturationValue);
      finalResult.put("pnsIndex", pnsIndexValue);
      finalResult.put("pnsZone", pnsZoneValue);
      finalResult.put("prq", prqValue);
      finalResult.put("rmssd", rmssdValue);
      finalResult.put("rri", rriValue);
      finalResult.put("respirationRate", respirationRateValue);
      finalResult.put("sd1", sd1Value);
      finalResult.put("sd2", sd2Value);
      finalResult.put("sdnn", sdnnValue);
      finalResult.put("snsIndex", snsIndexValue);
      finalResult.put("snsZone", snsZoneValue);
      finalResult.put("stressLevel", stressLevelValue);
      finalResult.put("stressIndex", stressIndexValue);
      finalResult.put("wellnessIndex", wellnessIndexValue);
      finalResult.put("wellnessLevel", wellnessLevelValue);

      liveFinalResult.put("pulseRate", pulseRateValue);
      liveFinalResult.put("respirationRate", respirationRateValue);
      liveFinalResult.put("oxygenSaturation", oxygenSaturationValue);

    } catch (JSONException e) {
      e.printStackTrace();
    }

    PluginResult livePluginResult = new PluginResult(PluginResult.Status.OK, liveFinalResult);
    livePluginResult.setKeepCallback(false);
    startScanCallbackContext.sendPluginResult(livePluginResult);

    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, finalResult);
    stopScanCallbackContext.sendPluginResult(pluginResult);
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
      showAlert(null, "Error: " + e.getErrorCode());
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
      showAlert(null, "Error: " + e.getErrorCode());
    }
  }

  private void showAlert(String title, String message) {
    new AlertDialog.Builder(cordova.getActivity())
      .setTitle(title)
      .setMessage(message)
      .setPositiveButton("OK", null)
      .setCancelable(false)
      .show();
  }
}
