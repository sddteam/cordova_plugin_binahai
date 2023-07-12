package inc.bastion.binahai;

import android.app.Activity;
import android.content.Intent;
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

import org.json.JSONArray;
import org.json.JSONException;

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
import ai.binah.sdk.api.vital_signs.vitals.VitalSignBloodPressure;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignPulseRate;
import ai.binah.sdk.session.FaceSessionBuilder;

/**
 * This class echoes a string called from JavaScript.
 */
public class BinahAi extends CordovaPlugin implements TestActivity.ImagePreviewListener {
  private static final String TAG = "BinahAi";
  private Session mSession;
  private static final long MEASUREMENT_DURATION = 60;

  private static final String START_CAMERA = "startCamera";
  private static final String START_SCAN = "startScan";

  private CallbackContext startCameraCallbackContext;
  private int containerViewId = 20;
  private boolean toBack = true;
  private ViewParent webViewParent;

  private TestActivity fragment;

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
    final float opacity = Float.parseFloat("1");
    fragment = new TestActivity();
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
    callbackContext.success(START_SCAN);
    handleStartStopButtonClicked();

    return true;
  }

  @Override
  public void onSessionCreated(Session session) {
    this.mSession = session;
  }

  @Override
  public void onFinalResult(VitalSignsResults vitalSignsResults) {
    VitalSignBloodPressure bloodPressure =
      (VitalSignBloodPressure) vitalSignsResults.getResult(VitalSignTypes.BLOOD_PRESSURE);

    VitalSignPulseRate pulseRate =
      (VitalSignPulseRate) vitalSignsResults.getResult(VitalSignTypes.PULSE_RATE);
    String pulseRateValue = pulseRate != null ? pulseRate.getValue().toString() : "N/A";

    String bloodPressureValue;
    if (bloodPressure != null) {
      bloodPressureValue = bloodPressure.getValue().getSystolic()
        +"/"+ bloodPressure.getValue().getDiastolic();
    } else {
      bloodPressureValue = "N/A";
    }
  }

  private void handleStartStopButtonClicked() {
    if (mSession == null) {
      return;
    }

    try {
      if (mSession.getState() == SessionState.READY) {
        mSession.start(MEASUREMENT_DURATION);
      } else {
        mSession.stop();
      }
    } catch (HealthMonitorException e) {
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
