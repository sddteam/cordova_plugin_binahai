package inc.bastion.binahai;

import android.accessibilityservice.AccessibilityService;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ai.binah.sdk.api.HealthMonitorException;
import ai.binah.sdk.api.SessionEnabledVitalSigns;
import ai.binah.sdk.api.alerts.AlertCodes;
import ai.binah.sdk.api.alerts.ErrorData;
import ai.binah.sdk.api.alerts.WarningData;
import ai.binah.sdk.api.images.ImageData;
import ai.binah.sdk.api.images.ImageListener;
import ai.binah.sdk.api.images.ImageValidity;
import ai.binah.sdk.api.license.LicenseDetails;
import ai.binah.sdk.api.license.LicenseInfo;
import ai.binah.sdk.api.license.LicenseOfflineMeasurements;
import ai.binah.sdk.api.session.Session;
import ai.binah.sdk.api.session.SessionInfoListener;
import ai.binah.sdk.api.session.SessionState;
import ai.binah.sdk.api.session.demographics.Sex;
import ai.binah.sdk.api.session.demographics.SubjectDemographic;
import ai.binah.sdk.api.vital_signs.VitalSign;
import ai.binah.sdk.api.vital_signs.VitalSignConfidence;
import ai.binah.sdk.api.vital_signs.VitalSignTypes;
import ai.binah.sdk.api.vital_signs.VitalSignsListener;
import ai.binah.sdk.api.vital_signs.VitalSignsResults;
import ai.binah.sdk.api.vital_signs.vitals.RRI;
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
import com.binahptt.bastion.R;

public class CameraActivity extends Fragment implements ImageListener, SessionInfoListener, VitalSignsListener{
  public interface ImagePreviewListener{
    void onSessionCreated(Session session);
    void onStartScan(JSONObject vitalSign);
    void onFinalResult(JSONArray vitalSignsResults);
    void onImageValidation(JSONObject imageValidationCode);
  }
  private ImagePreviewListener eventListener;
  private static final String TAG = "CameraActivity";

  private static final int PERMISSION_REQUEST_CODE = 12345;
  private static final String LICENSE_KEY = "668765-6009B5-426FAD-D62FC0-D89858-19B9FF";
  public String licenseKey;

  private Session mSession;
  private Bitmap mFaceDetection;
  private TextureView _cameraView;
  private View _view;
  private JSONObject _vitalHolder = new JSONObject();

  private String appResourcePackage;

  public void setEventListener(ImagePreviewListener listener){
    eventListener = listener;
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    appResourcePackage = getActivity().getPackageName();

    _view = inflater.inflate(getResources().getIdentifier("activity_camera", "layout", appResourcePackage), container, false);
    initUi();
    mFaceDetection = createFaceDetectionBitmap();

    return _view;
  }

  @Override
  public void onStart() {
    Log.d(TAG, "ON STARTING");
    super.onStart();
    int permissionStatus = ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA);
    if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
      createSession();
    } else {
      requestPermissions((new String[]{android.Manifest.permission.CAMERA}), PERMISSION_REQUEST_CODE);
    }
  }

  @Override
  public void onStop() {
    Log.d(TAG, "ON STOPPING");
    super.onStop();
    if (mSession != null) {
      Log.d(TAG, mSession.getState().name());
      mSession.terminate();
      mSession = null;
    }
  }



  @Override
  public void onImage(ImageData imageData) {
    getActivity().runOnUiThread(() -> {



      Canvas canvas = _cameraView.lockCanvas();
      if (canvas == null) {
        return;
      }
      // Drawing the bitmap on the TextureView canvas
      Bitmap image = imageData.getImage();
      canvas.drawBitmap(
        image,
        null,
        new Rect(0, 0, _cameraView.getWidth(),
          _cameraView.getBottom() - _cameraView.getTop()),
        null
      );

      //Drawing the face detection (if not null..)
      Rect roi = imageData.getROI();
      if (roi != null) {
        //Log.d(TAG, "ROI: TOP: " + roi.top + "RIGHT: " + roi.right + "BOTTOM: " + roi.bottom + "LEFT: " + roi.left);
        JSONObject imageErrorCode = new JSONObject();
        try {
          if (imageData.getImageValidity() != ImageValidity.VALID) {
            Log.i(TAG, "Image Validity Error: "+ imageData.getImageValidity());
            imageErrorCode.put("imageValidationError", imageData.getImageValidity());
          }else{
            if(isInRanged(roi)){
              Log.i(TAG, "Image Validity Error: "+ imageData.getImageValidity());
              imageErrorCode.put("imageValidationError", imageData.getImageValidity());
            }
          }
          eventListener.onImageValidation(imageErrorCode);
        }catch (JSONException e){
          e.printStackTrace();
        }

        //First we scale the SDK face detection rectangle to fit the TextureView size
        RectF targetRect = new RectF(roi);
        Matrix m = new Matrix();
        m.postScale(1f, 1f, image.getWidth() / 2f, image.getHeight() / 2f);
        m.postScale(
          (float)_cameraView.getWidth() / image.getWidth(),
          (float)_cameraView.getHeight() / image.getHeight()
        );
        m.mapRect(targetRect);
        // Then we draw it on the Canvas
        canvas.drawBitmap(mFaceDetection, null, targetRect, null);
      }

      _cameraView.unlockCanvasAndPost(canvas);
    });
  }

  public boolean isInRanged(Rect roi){
    int min = 20;
    int max = 10;
    Rect rect = new Rect(120, 130, 360, 455);

    boolean topDiff = roi.top <= Math.abs(rect.top + max) && roi.top >= Math.abs(rect.top - min);
    boolean leftDiff = roi.left <= Math.abs(rect.left + max) && roi.left >= Math.abs(rect.left - min);
    boolean rightDiff = roi.right <= Math.abs(rect.right + max) && roi.right >= Math.abs(rect.right - min);
    boolean bottomDiff = roi.bottom <= Math.abs(rect.bottom + max) && roi.bottom >= Math.abs(rect.bottom - min);

    //Log.d(TAG, "ROI: " + roi + " / " + String.valueOf(topDiff) + String.valueOf(leftDiff) + String.valueOf(rightDiff) + String.valueOf(bottomDiff));
    return topDiff && leftDiff && rightDiff && bottomDiff;
  }

  private void initUi(){
    _cameraView = (TextureView) _view.findViewById(R.id.cameraView);
  }

  private Bitmap createFaceDetectionBitmap() {
    Drawable drawable = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.face_detection);
    if (drawable == null) {
      return null;
    }

    int width = drawable.getIntrinsicWidth();
    width = width > 0 ? width : 1;
    int height = drawable.getIntrinsicHeight();
    height = height > 0 ? height : 1;

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);

    return bitmap;
  }

  private void createSession() {
    Log.d(TAG, "CREATING SESSION");
    LicenseDetails licenseDetails = new LicenseDetails(licenseKey);
    try {
      if(mSession != null){
        mSession.terminate();
        mSession = null;
        Log.d(TAG, "TERMINATING");
      }
      mSession = new FaceSessionBuilder(getActivity().getApplicationContext())
        .withImageListener(this)
        .withVitalSignsListener(this)
        .withSessionInfoListener(this)
        .build(licenseDetails);
      eventListener.onSessionCreated(mSession);
    } catch (HealthMonitorException e) {
      showAlert(null, "Error(CREATE SESSION): " + e.getErrorCode());
    }
  }

  private void showAlert(String title, String message) {
    new AlertDialog.Builder(getActivity())
      .setTitle(title)
      .setMessage(message)
      .setPositiveButton("OK", null)
      .setCancelable(false)
      .show();
  }

  @Override
  public void onSessionStateChange(SessionState sessionState) {
    getActivity().runOnUiThread(() -> {
      Toast.makeText(getContext(), "Session state: " + sessionState.name(), Toast.LENGTH_SHORT).show();
    });
  }

  @Override
  public void onWarning(WarningData warningData) {
    getActivity().runOnUiThread(() -> {
      Toast.makeText(getContext(), "Warning: " + warningData.getCode(), Toast.LENGTH_SHORT).show();
    });
  }

  @Override
  public void onError(ErrorData errorData) {
    getActivity().runOnUiThread(() -> {
      showAlert(null, "Domain: "+ errorData.getDomain() + " Error: "+ errorData.getCode());
    });
  }

  @Override
  public void onLicenseInfo(LicenseInfo licenseInfo) {
    getActivity().runOnUiThread(() -> {
      String activationId = licenseInfo.getLicenseActivationInfo().getActivationID();
      if (!activationId.isEmpty()) {
        Log.i(TAG, "License Activation ID: "+ activationId);
      }

      LicenseOfflineMeasurements offlineMeasurements = licenseInfo.getLicenseOfflineMeasurements();
      if (offlineMeasurements != null) {
        Log.i(TAG,
          "License Offline Measurements: " +
            offlineMeasurements.getTotalMeasurements() +"/" +
            offlineMeasurements.getRemainingMeasurements()
        );
      }
    });
  }

  @Override
  public void onEnabledVitalSigns(SessionEnabledVitalSigns enabledVitalSigns) {
   getActivity().runOnUiThread(() -> {
     for (int vs: VitalSignTypes.all()) {
       // Checking if pulse rate is enabled
       //Log.i("ENABLED_VITALS", "Is "+ vs +" enabled: " + enabledVitalSigns.isEnabled(vs));

       // Checking if pulse rate is enabled for the specific device:
       //Log.i("ENABLED_VITALS", "Is "+ vs +" device enabled: " + enabledVitalSigns.isDeviceEnabled(vs));

       // Checking if pulse rate is enabled for the measurement mode:
       //Log.i("ENABLED_VITALS", "Is "+ vs +" mode enabled: " + enabledVitalSigns.isMeasurementModeEnabled(vs));

       // Checking if pulse rate is enabled for the license:
       //Log.i("ENABLED_VITALS", "Is "+ vs +" license enabled: " + enabledVitalSigns.isLicenseEnabled(vs));
     }
    });
  }

  @Override
  public void onVitalSign(VitalSign vitalSign) {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          int vitalSignType = vitalSign.getType();
          switch (vitalSignType){
            case VitalSignTypes.PULSE_RATE:
              VitalSignPulseRate pulseRate = (VitalSignPulseRate) vitalSign;
              _vitalHolder.put("pulseRate", pulseRate.getValue());
              break;
            case VitalSignTypes.RESPIRATION_RATE:
              VitalSignRespirationRate respirationRate = (VitalSignRespirationRate) vitalSign;
              _vitalHolder.put("respirationRate", respirationRate.getValue());
              break;
            case VitalSignTypes.OXYGEN_SATURATION:
              VitalSignOxygenSaturation oxygenSaturation = (VitalSignOxygenSaturation) vitalSign;
              _vitalHolder.put("oxygenSaturation", oxygenSaturation.getValue());
              break;
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
        eventListener.onStartScan(_vitalHolder);
      }
    });
  }

  @Override
  public void onRequestPermissionsResult(
    int requestCode,
    @NonNull String[] permissions,
    @NonNull int[] grantResults
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSION_REQUEST_CODE
      && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      createSession();
    }
  }

  @Override
  public void onFinalResults(VitalSignsResults vitalSignsResults) {
    if(_vitalHolder != null){
      // Clear the JSONObject
      Iterator<String> keys = _vitalHolder.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        keys.remove();
      }
    }
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        List<VitalSign> vitalSigns = vitalSignsResults.getResults();
        Map<Integer, String> signResults = new HashMap<>();
        for (VitalSign sign: vitalSigns) {
          int signType = sign.getType();
          switch (signType) {
            case VitalSignTypes.BLOOD_PRESSURE:
              VitalSignBloodPressure bloodPressure = (VitalSignBloodPressure) sign;
              String bloodPressureValue = bloodPressure != null ? bloodPressure.getValue().getSystolic() + "/" + bloodPressure.getValue().getDiastolic() : "N/A";
              signResults.put(signType, bloodPressureValue);
              break;
            case VitalSignTypes.PULSE_RATE:
              VitalSignPulseRate pulseRate = (VitalSignPulseRate) sign;
              String pulseRateValue = pulseRate != null ? pulseRate.getValue().toString() : "N/A";
              VitalSignConfidence pr = pulseRate.getConfidence();
              signResults.put(signType, pulseRateValue);
              break;
            case VitalSignTypes.LFHF:
              VitalSignLFHF lfhf = (VitalSignLFHF) sign;
              String lfhfValue = lfhf != null ? lfhf.getValue().toString() : "N/A";
              signResults.put(signType, lfhfValue);
              break;
            case VitalSignTypes.MEAN_RRI:
              VitalSignMeanRRI meanRRI = (VitalSignMeanRRI) sign;
              String meanRRIValue = meanRRI != null ? meanRRI.getValue().toString() : "N/A";
              signResults.put(signType, meanRRIValue);
              break;
            case VitalSignTypes.OXYGEN_SATURATION:
              VitalSignOxygenSaturation oxygenSaturation = (VitalSignOxygenSaturation) sign;
              String oxygenSaturationValue = oxygenSaturation != null ? oxygenSaturation.getValue().toString() : "N/A";
              signResults.put(signType, oxygenSaturationValue);
              break;
            case VitalSignTypes.PNS_INDEX:
              VitalSignPNSIndex pnsIndex = (VitalSignPNSIndex) sign;
              String pnsIndexValue = pnsIndex != null ? pnsIndex.getValue().toString() : "N/A";
              signResults.put(signType, pnsIndexValue);
              break;
            case VitalSignTypes.PNS_ZONE:
              VitalSignPNSZone pnsZone = (VitalSignPNSZone) sign;
              String pnsZoneValue = pnsZone != null ? pnsZone.getValue().name() : "N/A";
              signResults.put(signType, pnsZoneValue);
              break;
            case VitalSignTypes.PRQ:
              VitalSignPRQ prq = (VitalSignPRQ) sign;
              String prqValue = prq != null ? prq.getValue().toString() : "N/A";
              signResults.put(signType, prqValue);
              break;
            case VitalSignTypes.RMSSD:
              VitalSignRMSSD rmssd = (VitalSignRMSSD) sign;
              String rmssdValue = rmssd != null ? rmssd.getValue().toString() : "N/A";
              signResults.put(signType, rmssdValue);
              break;
            case VitalSignTypes.RRI:
              VitalSignRRI rri = (VitalSignRRI) sign;
              String rriValue = "N/A";
              if(rri != null){
                for(RRI rriVal: rri.getValue()){
                  rriValue = rriVal.getTimestamp() +":"+ rriVal.getTimestamp();
                }
              }
              signResults.put(signType, rriValue);
              break;
            case VitalSignTypes.RESPIRATION_RATE:
              VitalSignRespirationRate respirationRate = (VitalSignRespirationRate) sign;
              String respirationRateValue = respirationRate != null ? respirationRate.getValue().toString() : "N/A";
              signResults.put(signType, respirationRateValue);
              break;
            case VitalSignTypes.SD1:
              VitalSignSD1 sd1 = (VitalSignSD1) sign;
              String sd1Value = sd1 != null ? sd1.getValue().toString() : "N/A";
              signResults.put(signType, sd1Value);
              break;
            case VitalSignTypes.SD2:
              VitalSignSD2 sd2 = (VitalSignSD2) sign;
              String sd2Value = sd2 != null ? sd2.getValue().toString() : "N/A";
              signResults.put(signType, sd2Value);
              break;
            case VitalSignTypes.SDNN:
              VitalSignSDNN sdnn = (VitalSignSDNN) sign;
              String sdnnValue = sdnn != null ? sdnn.getValue().toString() : "N/A";
              signResults.put(signType, sdnnValue);
              break;
            case VitalSignTypes.SNS_INDEX:
              VitalSignSNSIndex snsIndex = (VitalSignSNSIndex) sign;
              String snsIndexValue = snsIndex != null ? snsIndex.getValue().toString() : "N/A";
              signResults.put(signType, snsIndexValue);
              break;
            case VitalSignTypes.SNS_ZONE:
              VitalSignSNSZone snsZone = (VitalSignSNSZone) sign;
              String snsZoneValue = snsZone != null ? snsZone.getValue().name() : "N/A";
              signResults.put(signType, snsZoneValue);
              break;
            case VitalSignTypes.STRESS_LEVEL:
              VitalSignStressLevel stressLevel = (VitalSignStressLevel) sign;
              String stressLevelValue = stressLevel != null ? stressLevel.getValue().name() : "N/A";
              signResults.put(signType, stressLevelValue);
              break;
            case VitalSignTypes.STRESS_INDEX:
              VitalSignStressIndex stressIndex = (VitalSignStressIndex) sign;
              String stressIndexValue = stressIndex != null ? stressIndex.getValue().toString() : "N/A";
              signResults.put(signType, stressIndexValue);
              break;
            case VitalSignTypes.WELLNESS_INDEX:
              VitalSignWellnessIndex wellnessIndex = (VitalSignWellnessIndex) sign;
              String wellnessIndexValue = wellnessIndex != null ? wellnessIndex.getValue().toString() : "N/A";
              signResults.put(signType, wellnessIndexValue);
              break;
            case VitalSignTypes.WELLNESS_LEVEL:
              VitalSignWellnessLevel wellnessLevel = (VitalSignWellnessLevel) sign;
              String wellnessLevelValue = wellnessLevel != null ? wellnessLevel.getValue().name(): "N/A";
              signResults.put(signType, wellnessLevelValue);
              break;
          }
        }

        JSONArray finalResult = new JSONArray();
        for (Map.Entry<Integer, String> entry : signResults.entrySet()){
          Integer signType = entry.getKey();
          Object signValue = entry.getValue();
          String signTypeName = SignTypeNames.SIGN_TYPE_NAMES.get(signType);
          try {
            JSONObject vitalSignObj = new JSONObject();
            vitalSignObj.put("name", signTypeName);
            vitalSignObj.put("value", signValue);
            finalResult.put(vitalSignObj);
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
        eventListener.onFinalResult(finalResult);
      }
    });

  }

}
