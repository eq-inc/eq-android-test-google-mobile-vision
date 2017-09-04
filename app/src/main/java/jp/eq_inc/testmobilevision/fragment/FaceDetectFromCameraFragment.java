package jp.eq_inc.testmobilevision.fragment;

import android.Manifest;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jp.co.thcomp.util.LogUtil;
import jp.eq_inc.testmobilevision.R;
import jp.eq_inc.testmobilevision.view.GraphicOverlay;

public class FaceDetectFromCameraFragment extends AbstractFaceDetectFragment {
    private enum Status {
        Init, Detecting,
    }

    private SurfaceView mCameraPreview;
    private GraphicOverlay mPreviewOverlay;
    private TextView mDetectedInformationTv;
    private FaceDetector mFaceDetector;
    private CameraSource mCameraSource;
    private Status mStatus = Status.Init;
    private HashMap<FaceTracker, Face> mDrawFaceMap = new HashMap<FaceTracker, Face>();

    public FaceDetectFromCameraFragment() {
        // Required empty public constructor
    }

    public static FaceDetectFromCameraFragment newInstance(Bundle param) {
        FaceDetectFromCameraFragment fragment = new FaceDetectFromCameraFragment();
        fragment.setArguments(param);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_face_detect_from_camera, container, false);

        mCameraPreview = (SurfaceView) view.findViewById(R.id.svCameraPreview);
        mPreviewOverlay = (GraphicOverlay) view.findViewById(R.id.goPreviewOverlay);
        mPreviewOverlay.setOnDrawListener(mPreviewOverlayListener);
        mDetectedInformationTv = (TextView) view.findViewById(R.id.tvFaceInformation);

        ViewGroup cameraPreviewContainer = (ViewGroup) view.findViewById(R.id.flCameraPreviewContainer);
        cameraPreviewContainer.getViewTreeObserver().addOnGlobalLayoutListener(mParentViewGroupLayoutListener);

        // 使用しないので、非表示
        getActivity().findViewById(R.id.spnrRotation).setVisibility(View.GONE);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stop();
    }

    private boolean initFaceDetector() {
        boolean ret = false;

        try {
            Activity activity = getActivity();

            if (mFaceDetector != null) {
                mFaceDetector.release();
                mFaceDetector = null;
            }
            FaceDetector.Builder builder = new FaceDetector.Builder(activity);

            // classification
            SwitchCompat tempSwitch = (SwitchCompat) activity.findViewById(R.id.scClassification);
            if (tempSwitch.isChecked()) {
                builder.setClassificationType(FaceDetector.ALL_CLASSIFICATIONS);
            } else {
                builder.setClassificationType(FaceDetector.NO_CLASSIFICATIONS);
            }

            // landmark
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scLandmark);
            if (tempSwitch.isChecked()) {
                builder.setLandmarkType(FaceDetector.ALL_LANDMARKS);
            } else {
                builder.setLandmarkType(FaceDetector.NO_LANDMARKS);
            }

            // mode
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scDetectMode);
            if (tempSwitch.isChecked()) {
                builder.setMode(FaceDetector.FAST_MODE);
            } else {
                builder.setMode(FaceDetector.ACCURATE_MODE);
            }

            // prominent face only
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scProminentFaceOnly);
            builder.setProminentFaceOnly(tempSwitch.isChecked());

            mFaceDetector = builder.build();
            MultiProcessor.Builder<Face> multiProcessorBuilder = new MultiProcessor.Builder<Face>(mMultiProcessFactory);
            mFaceDetector.setProcessor(multiProcessorBuilder.build());

            ret = true;
        } catch (Exception e) {

        }

        return ret;
    }

    private boolean initCameraSource() {
        boolean ret = false;

        try {
            Activity activity = getActivity();

            if (mCameraSource != null) {
                mCameraSource.release();
                mCameraSource = null;
            }
            CameraSource.Builder builder = new CameraSource.Builder(activity, mFaceDetector);
            builder.setRequestedPreviewSize(mCameraPreview.getWidth(), mCameraPreview.getHeight());

            // auto focus
            SwitchCompat tempSwitch = (SwitchCompat) activity.findViewById(R.id.scAutoFocus);
            builder.setAutoFocusEnabled(tempSwitch.isChecked());

            // facing
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scFacing);
            if (tempSwitch.isChecked()) {
                builder.setFacing(CameraSource.CAMERA_FACING_FRONT);
            } else {
                builder.setFacing(CameraSource.CAMERA_FACING_BACK);
            }

            // detect fps
            try {
                EditText etDetectFps = (EditText) activity.findViewById(R.id.etDetectFps);
                float detectFps = Float.parseFloat(etDetectFps.getText().toString());
                builder.setRequestedFps(detectFps);
            } catch (NumberFormatException e) {

            }
            mCameraSource = builder.build();

            ret = true;
        } catch (Exception e) {

        }

        return ret;
    }

    private boolean start() {
        boolean ret = false;

        if (mStatus == Status.Init) {
            if (initFaceDetector()) {
                if (initCameraSource()) {
                    try {
                        mCameraSource.start(mCameraPreview.getHolder());
                        Size previewSize = mCameraSource.getPreviewSize();
                        changePreviewSize(previewSize.getWidth(), previewSize.getHeight());
                        ret = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return ret;
    }

    private boolean stop() {
        boolean ret = false;

        if (mStatus == Status.Detecting) {
            if (mFaceDetector != null) {
                mFaceDetector.release();
                mFaceDetector = null;
            }
            if (mCameraSource != null) {
                mCameraSource.stop();
                mCameraSource.release();
                mCameraSource = null;
            }

            ret = true;
        }

        return ret;
    }

    @Override
    public String[] getRuntimePermissions() {
        ArrayList<String> runtimePermissionList = new ArrayList<String>();
        String[] superRuntimePermissionArray = super.getRuntimePermissions();

        runtimePermissionList.add(Manifest.permission.CAMERA);
        for (String superRuntimePermission : superRuntimePermissionArray) {
            runtimePermissionList.add(superRuntimePermission);
        }

        return runtimePermissionList.toArray(new String[0]);
    }

    @Override
    public void changeFaceDetectParams() {
    }

    private void changePreviewSize(int width, int height){
        ViewGroup parentViewGroup = (ViewGroup) mCameraPreview.getParent();
        ViewGroup.LayoutParams cameraPreviewParams = mCameraPreview.getLayoutParams();

        cameraPreviewParams.width = width;
        cameraPreviewParams.height = height;
        parentViewGroup.updateViewLayout(mCameraPreview, cameraPreviewParams);

        ViewGroup.LayoutParams previewOverlayParams = mPreviewOverlay.getLayoutParams();
        previewOverlayParams.width = width;
        previewOverlayParams.height = height;
        parentViewGroup.updateViewLayout(mPreviewOverlay, previewOverlayParams);
    }

    private ViewTreeObserver.OnGlobalLayoutListener mParentViewGroupLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            ViewGroup parentViewGroup = (ViewGroup) mCameraPreview.getParent();
            parentViewGroup.getViewTreeObserver().removeOnGlobalLayoutListener(this);

            mCameraPreview.getViewTreeObserver().addOnGlobalLayoutListener(mCameraPreviewLayoutListener);
            mPreviewOverlay.getViewTreeObserver().addOnGlobalLayoutListener(mPreviewOverlayLayoutListener);
            ViewGroup.LayoutParams cameraPreviewParams = mCameraPreview.getLayoutParams();

            int previewWidth = parentViewGroup.getWidth();
            int previewHeight = previewWidth * 3 / 4;
            changePreviewSize(previewWidth, previewHeight);
        }
    };

    private ViewTreeObserver.OnGlobalLayoutListener mCameraPreviewLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            mCameraPreview.getViewTreeObserver().removeOnGlobalLayoutListener(mCameraPreviewLayoutListener);
        }
    };

    private ViewTreeObserver.OnGlobalLayoutListener mPreviewOverlayLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            mPreviewOverlay.bringToFront();
            mPreviewOverlay.getViewTreeObserver().removeOnGlobalLayoutListener(mPreviewOverlayLayoutListener);
            start();
        }
    };

    private MultiProcessor.Factory<Face> mMultiProcessFactory = new MultiProcessor.Factory<Face>() {
        @Override
        public Tracker<Face> create(Face face) {
            return new FaceTracker();
        }
    };

    private GraphicOverlay.OnDrawListener mPreviewOverlayListener = new GraphicOverlay.OnDrawListener() {
        @Override
        public void onDraw(Canvas canvas) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            for (Map.Entry<FaceTracker, Face> entry : mDrawFaceMap.entrySet()) {
                FaceTracker tracker = entry.getKey();

                if (tracker.mVisible) {
                    Face face = entry.getValue();

                    RectF faceRect = new RectF(face.getPosition().x, face.getPosition().y, face.getPosition().x + face.getWidth(),face.getPosition().y + face.getHeight());
                    LogUtil.d("GraphicOverlay.OnDrawListener", "face is in " + faceRect);
                    LogUtil.d("GraphicOverlay.OnDrawListener", "preview size is " + mCameraSource.getPreviewSize());

                    tracker.mLinePaint.setARGB(100, face.getId() % 256, 0, 0);
                    FaceDetectFromCameraFragment.this.drawFaceLine(canvas, face, tracker.mLinePaint, Frame.ROTATION_0);
                }
            }
        }
    };

    private class FaceTracker extends Tracker<Face> {
        private Paint mLinePaint;
        private boolean mVisible = false;

        public FaceTracker() {
            super();
            mLinePaint = new Paint();
            mLinePaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.face_line_width));
        }

        @Override
        public void onNewItem(int i, Face face) {
            super.onNewItem(i, face);
            mVisible = true;
            mDrawFaceMap.put(this, face);
            mPreviewOverlay.postInvalidate();
        }

        @Override
        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            super.onUpdate(detections, face);
            mVisible = true;
            mDrawFaceMap.put(this, face);
            LogUtil.d("", "metadata: " + detections.getFrameMetadata().getWidth() + ", " + detections.getFrameMetadata().getHeight());
            mPreviewOverlay.postInvalidate();
        }

        @Override
        public void onMissing(Detector.Detections<Face> detections) {
            super.onMissing(detections);
            mVisible = false;
        }

        @Override
        public void onDone() {
            super.onDone();
            mDrawFaceMap.remove(this);
        }
    }
}
