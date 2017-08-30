package jp.eq_inc.testmobilevision;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class FaceDetectFromCameraActivity extends AbstractFaceDetectActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detect_from_camera);
    }

    @Override
    void onRequestPermissionsResult(boolean allGranted) {

    }
}
