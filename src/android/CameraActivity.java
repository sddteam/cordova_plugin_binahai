package inc.bastion.binahai;

import android.accessibilityservice.AccessibilityService;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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
import ai.binah.sdk.session.FaceSessionBuilder;

public class CameraActivity extends Fragment implements ImageListener, SessionInfoListener, VitalSignsListener{
  public interface ImagePreviewListener{
    void onStartScan(JSONObject vitalSign);
    void onFinalResult(JSONObject vitalSignsResults);
    void onImageValidation(JSONObject imageValidationCode);
    void onSessionState(String sessionState);
    void onBNHCameraStarted(Session session);
    void onBNHCameraError(HealthMonitorException e);
    void onBNHWarning(int warningCode);
    void onBNHError(int errorCode);
    void onFaceValidation(Bitmap image);
  }
  private ImagePreviewListener eventListener;
  private static final String TAG = "CameraActivity";
  private static final int PERMISSION_REQUEST_CODE = 12345;
  public String licenseKey;

  private Session mSession;
  private Bitmap mFaceDetection;
  private TextureView _cameraView;
  private View _view;
  private JSONObject _vitalHolder = new JSONObject();

  private String appResourcePackage;
  private Bitmap bitmapImage;

  private Timer imageValidationTimer;
  private boolean isValidationTimerRunning = false;


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
  public void onResume() {
    super.onResume();
    int permissionStatus = ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA);
    if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
      createSession();
    } else {
      requestPermissions((new String[]{android.Manifest.permission.CAMERA}), PERMISSION_REQUEST_CODE);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (mSession != null) {
      mSession.terminate();
      mSession = null;
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if(mSession != null){
      mSession.terminate();
      mSession = null;
    }
    _vitalHolder = null;
    eventListener = null;
    //imageValidationTimer.cancel();
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
        if (!isValidationTimerRunning) {
          int expandRoi = 30;
          int compressionQuality = 20;
          Rect expandedRoi = new Rect(
            roi.left - expandRoi,
            roi.top - expandRoi,
            roi.right + expandRoi,
            roi.bottom + expandRoi
          );

          Bitmap croppedBitmap = Bitmap.createBitmap(
            image,
            expandedRoi.left,
            expandedRoi.top,
            expandedRoi.width(),
            expandedRoi.height()
          );

          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          croppedBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputStream);
          byte[] compressedImageData = outputStream.toByteArray();
          Bitmap compressedBitmap = BitmapFactory.decodeByteArray(compressedImageData, 0, compressedImageData.length);
          bitmapImage = compressedBitmap;

          isValidationTimerRunning = true;
          startFaceValidationTimer();
        }
        //eventListener.onFaceValidation(imageData.getImage());
        JSONObject imageErrorCode = new JSONObject();
        try {
          if (imageData.getImageValidity() != ImageValidity.VALID) {
            //Log.i(TAG, "Image Validity Error: "+ imageData.getImageValidity());
            imageErrorCode.put("imageValidationError", imageData.getImageValidity());
          }else{
            if(isInRanged(roi)){
              //Log.i(TAG, "Image Validity Error: "+ imageData.getImageValidity());
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

  public void startFaceValidationTimer() {
    stopFaceValidationTimer(); // Stop the previous timer if it's running

    imageValidationTimer = new Timer();
    TimerTask imageValidationTask = new TimerTask() {
      @Override
      public void run() {
        if (bitmapImage != null) {
          getActivity().runOnUiThread(() -> {
            eventListener.onFaceValidation(bitmapImage);
          });
        }
      }
    };

    imageValidationTimer.schedule(imageValidationTask, 0, 10000); // Initial delay of 0, repeat every 10 seconds
  }

  public void stopFaceValidationTimer() {
    if (imageValidationTimer != null) {
      imageValidationTimer.cancel();
      imageValidationTimer = null;
    }
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
    int camera_id = getActivity().getResources().getIdentifier("cameraView", "id", appResourcePackage);
    _cameraView = (TextureView) _view.findViewById(camera_id);
  }

  private Bitmap createFaceDetectionBitmap() {
    int face_detection_id = getActivity().getResources().getIdentifier("face_detection", "drawable", appResourcePackage);
    Drawable drawable = ContextCompat.getDrawable(getActivity().getApplicationContext(), face_detection_id);
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
    if(mSession != null){
      return;
    }
    LicenseDetails licenseDetails = new LicenseDetails(licenseKey);
    try {
      mSession = new FaceSessionBuilder(getActivity().getApplicationContext())
        .withImageListener(this)
        .withVitalSignsListener(this)
        .withSessionInfoListener(this)
        .build(licenseDetails);
      eventListener.onBNHCameraStarted(mSession);
    } catch (HealthMonitorException e) {
      //showAlert(null, "Create session error: " + e.getErrorCode());
      eventListener.onBNHCameraError(e);
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
      //Toast.makeText(getContext(), "Session state: " + sessionState.name(), Toast.LENGTH_SHORT).show();
      eventListener.onSessionState(sessionState.name());
    });
  }

  @Override
  public void onWarning(WarningData warningData) {
    getActivity().runOnUiThread(() -> {
      Toast.makeText(getContext(), "Domain: "+ warningData.getDomain() + " Warning: " + warningData.getCode(), Toast.LENGTH_SHORT).show();
      eventListener.onBNHWarning(warningData.getCode());
    });
  }

  @Override
  public void onError(ErrorData errorData) {
    getActivity().runOnUiThread(() -> {
      //showAlert(null, "Domain: "+ errorData.getDomain() + " Error: "+ errorData.getCode());
      eventListener.onBNHError(errorData.getCode());
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

  private String determineSignLevel(int value, int lowThreshold, int highThreshold){
    if(value < lowThreshold){
      return "Low";
    }else if(value <= highThreshold){
      return "Normal";
    }else{
      return "High";
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
        try {
          List<VitalSign> vitalSigns = vitalSignsResults.getResults();
          Map<Integer, String> signResults = new HashMap<>();
          Map<Integer, JSONObject> _signResults = new HashMap<>();
          for (VitalSign sign: vitalSigns) {
            JSONObject results = new JSONObject();
            int signType = sign.getType();
            switch (signType) {
              case VitalSignTypes.BLOOD_PRESSURE:
                VitalSignBloodPressure bloodPressure = (VitalSignBloodPressure) sign;
                int systolicValue = bloodPressure.getValue().getSystolic();
                String bloodPressureValue = bloodPressure.getValue().getSystolic() + "/" + bloodPressure.getValue().getDiastolic();
                results.put("value",bloodPressureValue);
                results.put("level",determineSignLevel(systolicValue, 100, 129));
                results.put("description",getString(getActivity().getResources().getIdentifier("blood_pressure", "string", appResourcePackage)));
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.PULSE_RATE:
                VitalSignPulseRate pulseRate = (VitalSignPulseRate) sign;
                int pulseRateValue = pulseRate.getValue();
                VitalSignConfidence pr = pulseRate.getConfidence();
                results.put("value", pulseRateValue);
                results.put("level", determineSignLevel(pulseRateValue, 60, 100));
                results.put("description", getString(getActivity().getResources().getIdentifier("pulse_rate", "string", appResourcePackage)));
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.LFHF:
                VitalSignLFHF lfhf = (VitalSignLFHF) sign;
                double lfhfValue = lfhf.getValue();
                results.put("value", lfhfValue);
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.MEAN_RRI:
                VitalSignMeanRRI meanRRI = (VitalSignMeanRRI) sign;
                int meanRRIValue = meanRRI.getValue();
                results.put("value", meanRRIValue);
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.OXYGEN_SATURATION:
                VitalSignOxygenSaturation oxygenSaturation = (VitalSignOxygenSaturation) sign;
                int oxygenSaturationValue = oxygenSaturation.getValue();
                results.put("value", oxygenSaturationValue);
                results.put("level", determineSignLevel(oxygenSaturationValue, 95, 100));
                results.put("description", getString(getActivity().getResources().getIdentifier("oxygen_saturation", "string", appResourcePackage)));
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.PNS_INDEX:
                VitalSignPNSIndex pnsIndex = (VitalSignPNSIndex) sign;
                double pnsIndexValue = pnsIndex.getValue();
                results.put("value", pnsIndexValue);
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.PNS_ZONE:
                VitalSignPNSZone pnsZone = (VitalSignPNSZone) sign;
                String pnsZoneValue = pnsZone.getValue().name();
                results.put("value", pnsZoneValue);
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.PRQ:
                VitalSignPRQ prq = (VitalSignPRQ) sign;
                double prqValue = prq.getValue();
                results.put("value", prqValue);
                results.put("level", determineSignLevel((int) prqValue, 4, 5));
                results.put("description", getString(getActivity().getResources().getIdentifier("prq", "string", appResourcePackage)));
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.RMSSD:
                VitalSignRMSSD rmssd = (VitalSignRMSSD) sign;
                int rmssdValue = rmssd.getValue();
                results.put("value", rmssdValue);
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.RRI:
                VitalSignRRI rri = (VitalSignRRI) sign;
                String rriValue = "N/A";
                if(rri != null){
                  for(RRI rriVal: rri.getValue()){
                    rriValue = rriVal.getTimestamp() +":"+ rriVal.getTimestamp();
                  }
                }
                results.put("value", rriValue);
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.RESPIRATION_RATE:
                VitalSignRespirationRate respirationRate = (VitalSignRespirationRate) sign;
                int respirationRateValue = respirationRate.getValue();
                results.put("value", respirationRateValue);
                results.put("level", determineSignLevel(respirationRateValue, 12, 20));
                results.put("description", getString(getActivity().getResources().getIdentifier("respiration_rate", "string", appResourcePackage)));
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.SD1:
                VitalSignSD1 sd1 = (VitalSignSD1) sign;
                int sd1Value = sd1.getValue();
                results.put("value", sd1Value);
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.SD2:
                VitalSignSD2 sd2 = (VitalSignSD2) sign;
                int sd2Value = sd2.getValue();
                results.put("value", sd2Value);
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.SDNN:
                VitalSignSDNN sdnn = (VitalSignSDNN) sign;
                int sdnnValue = sdnn.getValue();
                results.put("value", sdnnValue);
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.SNS_INDEX:
                VitalSignSNSIndex snsIndex = (VitalSignSNSIndex) sign;
                double snsIndexValue = snsIndex.getValue();
                results.put("value", snsIndexValue);
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.SNS_ZONE:
                VitalSignSNSZone snsZone = (VitalSignSNSZone) sign;
                String snsZoneValue = snsZone.getValue().name();
                results.put("value", snsZoneValue);
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.STRESS_LEVEL:
                VitalSignStressLevel stressLevel = (VitalSignStressLevel) sign;
                String stressLevelValue = stressLevel.getValue().name();
                results.put("value", stressLevelValue);
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.STRESS_INDEX:
                VitalSignStressIndex stressIndex = (VitalSignStressIndex) sign;
                int stressIndexValue = stressIndex.getValue();
                results.put("value", stressIndexValue);
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.WELLNESS_INDEX:
                VitalSignWellnessIndex wellnessIndex = (VitalSignWellnessIndex) sign;
                int wellnessIndexValue = wellnessIndex.getValue();
                results.put("value",wellnessIndexValue);
                results.put("description", getString(getActivity().getResources().getIdentifier("wellness_score", "string", appResourcePackage)));
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.WELLNESS_LEVEL:
                VitalSignWellnessLevel wellnessLevel = (VitalSignWellnessLevel) sign;
                String wellnessLevelValue = wellnessLevel.getValue().name();
                results.put("value", wellnessLevelValue);
                results.put("description", getString(getActivity().getResources().getIdentifier("wellness_score", "string", appResourcePackage)));
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.HEMOGLOBIN:
                VitalSignHemoglobin hemoglobin = (VitalSignHemoglobin) sign;
                double hemoglobinValue = hemoglobin.getValue();
                results.put("value", hemoglobinValue);
                results.put("level", determineSignLevel((int) hemoglobinValue, 14, 18));
                results.put("description", getString(getActivity().getResources().getIdentifier("hemoglobin", "string", appResourcePackage)));
                _signResults.put(signType, results);
                break;
              case VitalSignTypes.HEMOGLOBIN_A1C:
                VitalSignHemoglobinA1C hemoglobinA16 = (VitalSignHemoglobinA1C) sign;
                double hemoglobinA1CValue = hemoglobinA16.getValue();
                results.put("value", hemoglobinA1CValue);
                String state;
                if(hemoglobinA1CValue < 5.7){
                  state = "Normal";
                }else if(hemoglobinA1CValue >= 6.4){
                  state = "Prediabetes Risk";
                }else{
                  state = "Diabetes Risk";
                }
                results.put("level", state);
                results.put("description", getString(getActivity().getResources().getIdentifier("hemoglobin_a1c", "string", appResourcePackage)));
                _signResults.put(signType, results);
                break;
            }
          }

          JSONObject finalResult = new JSONObject();
          for (Map.Entry<Integer, JSONObject> entry : _signResults.entrySet()){
            Integer signType = entry.getKey();
            JSONObject signValue = entry.getValue();
            String signTypeName = SignTypeNames.SIGN_TYPE_NAMES.get(signType);
            JSONObject vitalSignObj = new JSONObject();

            String[] keys = {"name", "value", "description"};

            for(String key : keys){
              if(signValue.has(key)){
                vitalSignObj.put(key, signValue.get(key));
              }
            }
            finalResult.put(signTypeName, vitalSignObj);
          }
          isValidationTimerRunning = false;
          stopFaceValidationTimer();
          eventListener.onFinalResult(finalResult);
        }catch(JSONException e){
          e.printStackTrace();
        }
      }
    });
  }
}
