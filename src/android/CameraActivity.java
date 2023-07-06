package inc.bastion.binahai;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaPlugin;

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
import ai.binah.sdk.api.vital_signs.VitalSignTypes;
import ai.binah.sdk.api.vital_signs.VitalSignsListener;
import ai.binah.sdk.api.vital_signs.VitalSignsResults;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignBloodPressure;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignOxygenSaturation;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignPulseRate;
import ai.binah.sdk.api.vital_signs.vitals.VitalSignRespirationRate;
import ai.binah.sdk.session.FaceSessionBuilder;
import io.ionic.starter.R;

public class CameraActivity extends CordovaActivity implements ImageListener, VitalSignsListener, SessionInfoListener {
  private static final String TAG = "CameraActivity";

  private static final String LICENSE_KEY = "668765-6009B5-426FAD-D62FC0-D89858-19B9FF";
  private static final long MEASUREMENT_DURATION = 60;
  private static final String LOG_TAG = "BinahSample";
  private static final int PERMISSION_REQUEST_CODE = 12345;

  private Session mSession;
  private Bitmap mFaceDetection;
  private TextureView _cameraView;
  private TextView _pulseRate;
  private TextView _respirationRate;
  private TextView _oxygenSaturation;
  private Button _startStop;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_camera);
    initUi();
    _startStop.setOnClickListener(
      (View.OnClickListener) v -> handleStartStopButtonClicked());
    mFaceDetection = createFaceDetectionBitmap();
  }

  private void initUi(){
    _cameraView = findViewById(R.id.cameraView);
    _pulseRate = findViewById(R.id.pulseRate);
    _respirationRate = findViewById(R.id.respirationRate);
    _oxygenSaturation = findViewById(R.id.oxygenSaturation);
    _startStop = findViewById(R.id.startStopButton);
  }

  @Override
  protected void onStart() {
    super.onStart();
    int permissionStatus = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
    if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
      createSession();
    } else {
      requestPermissions((new String[]{android.Manifest.permission.CAMERA}), PERMISSION_REQUEST_CODE);
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mSession != null) {
      mSession.terminate();
      mSession = null;
    }
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
  public void onImage(ImageData imageData) {
    runOnUiThread(() -> {
      if (imageData.getImageValidity() != ImageValidity.VALID) {
        Log.i(LOG_TAG, "Image Validity Error: "+ imageData.getImageValidity());
      }
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

  @SuppressLint("SetTextI18n")
  @Override
  public void onVitalSign(VitalSign vitalSign) {
    runOnUiThread(() -> {
      if (vitalSign.getType() == VitalSignTypes.PULSE_RATE) {
        VitalSignPulseRate pulseRate = (VitalSignPulseRate) vitalSign;
        _pulseRate.setText("PR: "+ pulseRate.getValue());
      }

      if (vitalSign.getType() == VitalSignTypes.RESPIRATION_RATE) {
        VitalSignRespirationRate respirationRate = (VitalSignRespirationRate) vitalSign;
        _respirationRate.setText("RR: "+ respirationRate.getValue());
        Log.i("VITAL SIGN", "Respiration Rate: " + respirationRate.getValue());
      }

      if (vitalSign.getType() == VitalSignTypes.OXYGEN_SATURATION) {
        VitalSignOxygenSaturation oxygenSaturation = (VitalSignOxygenSaturation) vitalSign;
        _oxygenSaturation.setText("OS: "+ oxygenSaturation.getValue());
      }
    });
  }

  @Override
  public void onFinalResults(VitalSignsResults finalResults) {
    runOnUiThread(() -> {
      VitalSignBloodPressure bloodPressure =
        (VitalSignBloodPressure) finalResults.getResult(VitalSignTypes.BLOOD_PRESSURE);

      VitalSignPulseRate pulseRate =
        (VitalSignPulseRate) finalResults.getResult(VitalSignTypes.PULSE_RATE);
      String pulseRateValue = pulseRate != null ? pulseRate.getValue().toString() : "N/A";

      String bloodPressureValue;
      if (bloodPressure != null) {
        bloodPressureValue = bloodPressure.getValue().getSystolic()
          +"/"+ bloodPressure.getValue().getDiastolic();
      } else {
        bloodPressureValue = "N/A";
      }

      showAlert("Final Results", "Pulse Rate: "+ pulseRateValue +"\n"+ "Blood Pressure: "+ bloodPressureValue);
    });
  }

  @Override
  public void onSessionStateChange(SessionState sessionState) {
    runOnUiThread(() -> {
      switch (sessionState) {
        case READY: {
          _startStop.setEnabled(true);
          _startStop.setText(R.string.start);
          getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          break;
        }
        case PROCESSING: {
          _startStop.setEnabled(true);
          _startStop.setText(R.string.stop);
          getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          break;
        }
        default: {
          _startStop.setEnabled(false);
          getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          break;
        }
      }
    });
  }

  @Override
  public void onWarning(WarningData warningData) {
    runOnUiThread(() -> {
      if (warningData.getCode() == AlertCodes.MEASUREMENT_CODE_MISDETECTION_DURATION_EXCEEDS_LIMIT_WARNING) {
        _pulseRate.setText("");
      }

      Toast.makeText(this, "Warning: " + warningData.getCode(), Toast.LENGTH_SHORT).show();
    });
  }

  @Override
  public void onError(ErrorData errorData) {
    runOnUiThread(() -> {
      showAlert(null, "Error: "+ errorData.getCode());
    });
  }

  @Override
  public void onLicenseInfo(LicenseInfo licenseInfo) {
    runOnUiThread(() -> {
      String activationId = licenseInfo.getLicenseActivationInfo().getActivationID();
      if (!activationId.isEmpty()) {
        Log.i(LOG_TAG, "License Activation ID: "+ activationId);
      }

      LicenseOfflineMeasurements offlineMeasurements = licenseInfo.getLicenseOfflineMeasurements();
      if (offlineMeasurements != null) {
        Log.i(LOG_TAG,
          "License Offline Measurements: " +
            offlineMeasurements.getTotalMeasurements() +"/" +
            offlineMeasurements.getRemainingMeasurements()
        );
      }
    });
  }

  @Override
  public void onEnabledVitalSigns(SessionEnabledVitalSigns sessionEnabledVitalSigns) {
    runOnUiThread(() -> {
      Log.i(LOG_TAG,"Pulse Rate Enabled: "+ sessionEnabledVitalSigns.isEnabled(VitalSignTypes.PULSE_RATE));
      Log.i(LOG_TAG,"Respiration Rate Enabled: "+ sessionEnabledVitalSigns.isEnabled(VitalSignTypes.RESPIRATION_RATE));
      Log.i(LOG_TAG,"Oxygen Saturation Enabled: "+ sessionEnabledVitalSigns.isEnabled(VitalSignTypes.OXYGEN_SATURATION));
    });
  }

  private void createSession() {
    LicenseDetails licenseDetails = new LicenseDetails(LICENSE_KEY);
    try {
      SubjectDemographic subjectDemographic = new SubjectDemographic(Sex.MALE, 23.0, 52.0);
      mSession = new FaceSessionBuilder(getApplicationContext())
        .withSubjectDemographic(subjectDemographic)
        .withImageListener(this)
        .withVitalSignsListener(this)
        .withSessionInfoListener(this)
        .build(licenseDetails);
    } catch (HealthMonitorException e) {
      showAlert(null, "Error: " + e.getErrorCode());
    }
  }

  private void handleStartStopButtonClicked() {
    if (mSession == null) {
      return;
    }

    try {
      if (mSession.getState() == SessionState.READY) {
        mSession.start(MEASUREMENT_DURATION);
        _pulseRate.setText("");
        _respirationRate.setText("");
        _oxygenSaturation.setText("");
      } else {
        mSession.stop();
      }
    } catch (HealthMonitorException e) {
      showAlert(null, "Error: " + e.getErrorCode());
    }
  }

  private void showAlert(String title, String message) {
    new AlertDialog.Builder(this)
      .setTitle(title)
      .setMessage(message)
      .setPositiveButton("OK", null)
      .setCancelable(false)
      .show();
  }

  private Bitmap createFaceDetectionBitmap() {
    Drawable drawable = ContextCompat.getDrawable(this, R.drawable.face_detection);
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
}
