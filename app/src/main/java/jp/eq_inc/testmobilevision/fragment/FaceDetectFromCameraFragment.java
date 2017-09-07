package jp.eq_inc.testmobilevision.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.util.DisplayMetrics;
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
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import jp.co.thcomp.util.LogUtil;
import jp.eq_inc.testmobilevision.R;
import jp.eq_inc.testmobilevision.view.GraphicOverlay;

public class FaceDetectFromCameraFragment extends AbstractDetectFragment {
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
    private int mReservedRequestedOrientation = 0;
    private Size mRealPreviewSize = null;
    private PointF mShownPreviewSize = new PointF();
    private boolean mTakingPicture = false;

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
        mPreviewOverlay.setOnClickListener(mPreviewOverlayClickListener);
        mDetectedInformationTv = (TextView) view.findViewById(R.id.tvFaceInformation);

        ViewGroup cameraPreviewContainer = (ViewGroup) view.findViewById(R.id.flCameraPreviewContainer);
        cameraPreviewContainer.getViewTreeObserver().addOnGlobalLayoutListener(mParentViewGroupLayoutListener);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stop();
    }

    @Override
    public void onResume() {
        super.onResume();

        Activity activity = getActivity();
        mReservedRequestedOrientation = activity.getRequestedOrientation();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().setRequestedOrientation(mReservedRequestedOrientation);
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

            // face tracking
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scFaceTracking);
            builder.setTrackingEnabled(tempSwitch.isChecked());

            mFaceDetector = builder.build();

            // use multi processor
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scUseMultiProcessor);
            if (tempSwitch.isChecked()) {
                MultiProcessor.Builder<Face> multiProcessorBuilder = new MultiProcessor.Builder<Face>(mMultiProcessFactory);
                mFaceDetector.setProcessor(multiProcessorBuilder.build());
            } else {
                LargestFaceFocusingProcessor.Builder focusingProcessorBuilder = new LargestFaceFocusingProcessor.Builder(mFaceDetector, new FaceTracker());
                mFaceDetector.setProcessor(focusingProcessorBuilder.build());
            }

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
                mCameraSource.stop();
                mCameraSource.release();
                mCameraSource = null;
            }
            CameraSource.Builder builder = new CameraSource.Builder(activity, mFaceDetector);

            // previewサイズ
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
                        mRealPreviewSize = mCameraSource.getPreviewSize();
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

            mStatus = Status.Init;
            ret = true;
        }

        return ret;
    }

    @Override
    public String[] getRuntimePermissions() {
        ArrayList<String> runtimePermissionList = new ArrayList<String>();
        String[] superRuntimePermissionArray = super.getRuntimePermissions();

        runtimePermissionList.add(Manifest.permission.CAMERA);
        runtimePermissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        for (String superRuntimePermission : superRuntimePermissionArray) {
            runtimePermissionList.add(superRuntimePermission);
        }

        return runtimePermissionList.toArray(new String[0]);
    }

    @Override
    public void changeCommonParams() {
        // 一旦全てのインスタンスを解放
        stop();
        start();
    }

    private void changePreviewSize(int width, int height) {
        ViewGroup parentViewGroup = (ViewGroup) mCameraPreview.getParent();
        ViewGroup.LayoutParams cameraPreviewParams = mCameraPreview.getLayoutParams();

        cameraPreviewParams.width = width;
        cameraPreviewParams.height = height;
        parentViewGroup.updateViewLayout(mCameraPreview, cameraPreviewParams);

        ViewGroup.LayoutParams previewOverlayParams = mPreviewOverlay.getLayoutParams();
        previewOverlayParams.width = width;
        previewOverlayParams.height = height;
        parentViewGroup.updateViewLayout(mPreviewOverlay, previewOverlayParams);

        mShownPreviewSize.set(width, height);
    }

    private ViewTreeObserver.OnGlobalLayoutListener mParentViewGroupLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            ViewGroup parentViewGroup = (ViewGroup) mCameraPreview.getParent();
            parentViewGroup.getViewTreeObserver().removeOnGlobalLayoutListener(this);

            mCameraPreview.getViewTreeObserver().addOnGlobalLayoutListener(mCameraPreviewLayoutListener);
            mPreviewOverlay.getViewTreeObserver().addOnGlobalLayoutListener(mPreviewOverlayLayoutListener);

            DisplayMetrics metrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
            float rate = ((float) metrics.heightPixels) / metrics.widthPixels;

            int previewWidth = parentViewGroup.getWidth();
            int previewHeight = (int) (previewWidth * rate);
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

    private View.OnClickListener mPreviewOverlayClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCameraSource != null && !mTakingPicture) {
                mTakingPicture = true;
                mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] bytes) {
                        mTakingPicture = false;

                        Activity activity = getActivity();
                        File cacheDir = activity.getExternalCacheDir();
                        if (!cacheDir.exists()) {
                            cacheDir.mkdirs();
                        }

                        if (cacheDir.exists()) {
                            Calendar currentCalendar = Calendar.getInstance();
                            String fileName = String.format(
                                    "%04d.%02d.%02d-%02d.%02d.%02d.%03d.jpg",
                                    currentCalendar.get(Calendar.YEAR),
                                    currentCalendar.get(Calendar.MONTH) + 1,
                                    currentCalendar.get(Calendar.DAY_OF_MONTH),
                                    currentCalendar.get(Calendar.HOUR_OF_DAY),
                                    currentCalendar.get(Calendar.MINUTE),
                                    currentCalendar.get(Calendar.SECOND),
                                    currentCalendar.get(Calendar.MILLISECOND)
                            );
                            File jpegFile = new File(cacheDir.getAbsolutePath() + File.separator + fileName);
                            FileOutputStream jpegFileStream = null;

                            try {
                                jpegFileStream = new FileOutputStream(jpegFile);
                                jpegFileStream.write(bytes);

                                new AlertDialog.Builder(activity)
                                        .setMessage(getString(R.string.taken_picture, jpegFile.getAbsoluteFile()))
                                        .setPositiveButton(android.R.string.ok, null)
                                        .show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (jpegFileStream != null) {
                                    try {
                                        jpegFileStream.close();
                                    } catch (IOException e) {
                                    }
                                }
                            }

                        }
                    }
                });
            }
        }
    };

    private GraphicOverlay.OnDrawListener mPreviewOverlayListener = new GraphicOverlay.OnDrawListener() {
        @Override
        public void onDraw(Canvas canvas) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            StringBuilder builder = new StringBuilder();
            for (Map.Entry<FaceTracker, Face> entry : mDrawFaceMap.entrySet()) {
                FaceTracker tracker = entry.getKey();

                if (tracker.mVisible) {
                    Face face = entry.getValue();

                    if (mRealPreviewSize != null) {
                        float xRate = mShownPreviewSize.x / mRealPreviewSize.getWidth();
                        float yRate = mShownPreviewSize.y / mRealPreviewSize.getHeight();

                        PointF facePosition = face.getPosition();
                        FaceDetectFromCameraFragment.this.drawQuadLine(canvas, "ID: " + String.valueOf(face.getId()), facePosition.x * xRate, facePosition.y * yRate, face.getWidth() * xRate, face.getHeight() * yRate, tracker.mLinePaint, Frame.ROTATION_0);
                    }

                    builder.append("ID: ").append(face.getId()).append("\n");
                    if (face.getIsLeftEyeOpenProbability() > 0) {
                        builder.append((face.getIsLeftEyeOpenProbability() * 100) + "% left eye is opened").append("\n");
                    }
                    if (face.getIsRightEyeOpenProbability() > 0) {
                        builder.append((face.getIsRightEyeOpenProbability() * 100) + "% right eye is opened").append("\n");
                    }
                    if (face.getIsSmilingProbability() > 0) {
                        builder.append((face.getIsSmilingProbability() * 100) + "% is smiled").append("\n");
                    }
                }
            }

            mDetectedInformationTv.setText(builder.toString());
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
            LogUtil.d("", "onNewItem: " + face.getId());

            super.onNewItem(i, face);
            mVisible = true;
            mDrawFaceMap.put(this, face);
            mPreviewOverlay.postInvalidate();

            // 顔毎に線色を変える。ただし、同じIDの人は同じ色にする
            int id = face.getId();
            int idModThree = id % 3;
            if (idModThree == 0) {
                mLinePaint.setARGB(100, 256 - id, 0, 0);
            } else if (idModThree == 1) {
                mLinePaint.setARGB(100, 0, 256 - id, 0);
            } else {
                mLinePaint.setARGB(100, 0, 0, 256 - id);
            }
        }

        @Override
        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            super.onUpdate(detections, face);

            mVisible = true;
            mDrawFaceMap.put(this, face);
            mPreviewOverlay.postInvalidate();
        }

        @Override
        public void onMissing(Detector.Detections<Face> detections) {
            if (mDrawFaceMap.containsKey(this)) {
                LogUtil.d("", "onMissing: " + mDrawFaceMap.get(this).getId());
            }

            super.onMissing(detections);
            mVisible = false;
            mPreviewOverlay.postInvalidate();
        }

        @Override
        public void onDone() {
            if (mDrawFaceMap.containsKey(this)) {
                LogUtil.d("", "onDone: " + mDrawFaceMap.get(this).getId());
            }

            super.onDone();
            mDrawFaceMap.remove(this);
        }
    }
}
