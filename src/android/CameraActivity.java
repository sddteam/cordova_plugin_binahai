package inc.bastion.binahai;

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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import ai.binah.sdk.api.HealthMonitorException;
import ai.binah.sdk.api.SessionEnabledVitalSigns;
import ai.binah.sdk.api.alerts.ErrorData;
import ai.binah.sdk.api.alerts.WarningData;
import ai.binah.sdk.api.images.ImageData;
import ai.binah.sdk.api.images.ImageListener;
import ai.binah.sdk.api.images.ImageValidity;
import ai.binah.sdk.api.license.LicenseDetails;
import ai.binah.sdk.api.license.LicenseInfo;
import ai.binah.sdk.api.session.Session;
import ai.binah.sdk.api.session.SessionInfoListener;
import ai.binah.sdk.api.session.SessionState;
import ai.binah.sdk.api.session.demographics.Sex;
import ai.binah.sdk.api.session.demographics.SubjectDemographic;
import ai.binah.sdk.api.vital_signs.VitalSign;
import ai.binah.sdk.api.vital_signs.VitalSignsListener;
import ai.binah.sdk.api.vital_signs.VitalSignsResults;
import ai.binah.sdk.session.FaceSessionBuilder;
import io.ionic.starter.R;

public class TestActivity extends Fragment implements ImageListener, SessionInfoListener, VitalSignsListener{
  public interface ImagePreviewListener{
    void onSessionCreated(Session session);
    void onFinalResult(VitalSignsResults vitalSignsResults);
  }
  private ImagePreviewListener eventListener;
  private static final String TAG = "TestActivity";

  private static final int PERMISSION_REQUEST_CODE = 12345;
  private static final String LICENSE_KEY = "668765-6009B5-426FAD-D62FC0-D89858-19B9FF";

  private Session mSession;
  private Bitmap mFaceDetection;
  private TextureView _cameraView;
  private TextView _pulseRate;
  private TextView _respirationRate;
  private TextView _oxygenSaturation;
  private Button _startStop;
  private View _view;

  private String appResourcePackage;

  public void setEventListener(ImagePreviewListener listener){
    eventListener = listener;
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    appResourcePackage = getActivity().getPackageName();
    createSession();

    _view = inflater.inflate(getResources().getIdentifier("activity_test", "layout", appResourcePackage), container, false);
    initUi();
    mFaceDetection = createFaceDetectionBitmap();

    return _view;
  }

  @Override
  public void onImage(ImageData imageData) {
    getActivity().runOnUiThread(() -> {
      if (imageData.getImageValidity() != ImageValidity.VALID) {
        Log.i(TAG, "Image Validity Error: "+ imageData.getImageValidity());
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

  private void initUi(){
    _cameraView = (TextureView) _view.findViewById(R.id.cameraView);
    _pulseRate = (TextView) _view.findViewById(R.id.pulseRate);
    _respirationRate = _view.findViewById(R.id.respirationRate);
    _oxygenSaturation = _view.findViewById(R.id.oxygenSaturation);
    _startStop = _view.findViewById(R.id.startStopButton);
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
    LicenseDetails licenseDetails = new LicenseDetails(LICENSE_KEY);
    try {
      SubjectDemographic subjectDemographic = new SubjectDemographic(Sex.MALE, 23.0, 52.0);
      mSession = new FaceSessionBuilder(getActivity().getApplicationContext())
        .withSubjectDemographic(subjectDemographic)
        .withImageListener(this)
        .withVitalSignsListener(this)
        .withSessionInfoListener(this)
        .build(licenseDetails);

      eventListener.onSessionCreated(mSession);

    } catch (HealthMonitorException e) {
      showAlert(null, "Error: " + e.getErrorCode());
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

  }

  @Override
  public void onWarning(WarningData warningData) {

  }

  @Override
  public void onError(ErrorData errorData) {

  }

  @Override
  public void onLicenseInfo(LicenseInfo licenseInfo) {

  }

  @Override
  public void onEnabledVitalSigns(SessionEnabledVitalSigns sessionEnabledVitalSigns) {

  }

  @Override
  public void onVitalSign(VitalSign vitalSign) {

  }

  @Override
  public void onFinalResults(VitalSignsResults vitalSignsResults) {
    eventListener.onFinalResult(vitalSignsResults);
  }

}
