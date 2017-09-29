package jp.eq_inc.testmobilevision.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
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
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiDetector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jp.eq_inc.testmobilevision.R;
import jp.eq_inc.mobilevision.detector.AllDetector;
import jp.eq_inc.mobilevision.processor.EachFocusingProcessor;
import jp.eq_inc.testmobilevision.view.GraphicOverlay;

public class MultiDetectFromCameraFragment extends AbstractDetectFragment {
    private enum Status {
        Init, Recognizing,
    }

    private SurfaceView mCameraPreview;
    private GraphicOverlay mPreviewOverlay;
    private TextView mDetectedInformationTv;
    private MultiDetector mMultiDetector;
    private CameraSource mCameraSource;
    private Status mStatus = Status.Init;
    private HashMap<AbstractLocalTracker, Object> mDrawObjectMap = new HashMap<AbstractLocalTracker, Object>();
    private int mReservedRequestedOrientation = 0;
    private Size mRealPreviewSize = null;
    private PointF mShownPreviewSize = new PointF();
    private boolean mTakingPicture = false;

    public MultiDetectFromCameraFragment() {
        // Required empty public constructor
    }

    public static MultiDetectFromCameraFragment newInstance(Bundle param) {
        MultiDetectFromCameraFragment fragment = new MultiDetectFromCameraFragment();
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
        View view = inflater.inflate(R.layout.fragment_multi_detect_from_camera, container, false);

        mCameraPreview = (SurfaceView) view.findViewById(R.id.svCameraPreview);
        mPreviewOverlay = (GraphicOverlay) view.findViewById(R.id.goPreviewOverlay);
        mPreviewOverlay.setOnDrawListener(mPreviewOverlayListener);
        mPreviewOverlay.setOnClickListener(mPreviewOverlayClickListener);
        mDetectedInformationTv = (TextView) view.findViewById(R.id.tvDetectInformation);

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

    private boolean initAllDetector() {
        boolean ret = false;

        try {
            Activity activity = getActivity();

            if (mMultiDetector != null) {
                mMultiDetector.release();
                mMultiDetector = null;
            }
            MultiDetector.Builder builder = new MultiDetector.Builder();
            FaceDetector.Builder faceDetectBuilder = new FaceDetector.Builder(activity);
            BarcodeDetector.Builder barcodeDetectBuilder = new BarcodeDetector.Builder(activity);
            TextRecognizer.Builder textRecognizerBuilder = new TextRecognizer.Builder(activity);

            // classification
            SwitchCompat tempSwitch = (SwitchCompat) activity.findViewById(R.id.scClassification);
            if (tempSwitch.isChecked()) {
                faceDetectBuilder.setClassificationType(FaceDetector.ALL_CLASSIFICATIONS);
            } else {
                faceDetectBuilder.setClassificationType(FaceDetector.NO_CLASSIFICATIONS);
            }

            // landmark
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scLandmark);
            if (tempSwitch.isChecked()) {
                faceDetectBuilder.setLandmarkType(FaceDetector.ALL_LANDMARKS);
            } else {
                faceDetectBuilder.setLandmarkType(FaceDetector.NO_LANDMARKS);
            }

            // mode
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scDetectMode);
            if (tempSwitch.isChecked()) {
                faceDetectBuilder.setMode(FaceDetector.FAST_MODE);
            } else {
                faceDetectBuilder.setMode(FaceDetector.ACCURATE_MODE);
            }

            // prominent face only
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scProminentFaceOnly);
            faceDetectBuilder.setProminentFaceOnly(tempSwitch.isChecked());

            // face tracking
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scFaceTracking);
            faceDetectBuilder.setTrackingEnabled(tempSwitch.isChecked());

            // barcode format
            Integer selectedFormat = (Integer) ((Spinner) activity.findViewById(R.id.spnrBarcodeFormat)).getSelectedItem();
            if (selectedFormat == null) {
                selectedFormat = Barcode.ALL_FORMATS;
            }
            barcodeDetectBuilder.setBarcodeFormats(selectedFormat);

            FaceDetector faceDetector = faceDetectBuilder.build();
            BarcodeDetector barcodeDetector = barcodeDetectBuilder.build();
            TextRecognizer textRecognizer = textRecognizerBuilder.build();

            // use multi processor
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scUseMultiProcessor);
            if (tempSwitch.isChecked()) {
                MultiProcessor.Builder multiProcessorBuilder = new MultiProcessor.Builder(mMultiProcessFactory);
                MultiProcessor processor = multiProcessorBuilder.build();
                faceDetector.setProcessor(processor);
                barcodeDetector.setProcessor(processor);
                textRecognizer.setProcessor(processor);
            } else {
                LargestFaceFocusingProcessor faceProcessor = new LargestFaceFocusingProcessor(faceDetector, new FaceTracker());
                faceDetector.setProcessor(faceProcessor);
                EachFocusingProcessor.BarcodeFocusingProcessor barcodeProcessor = new EachFocusingProcessor.BarcodeFocusingProcessor(barcodeDetector, new BarcodeTracker());
                barcodeDetector.setProcessor(barcodeProcessor);
                EachFocusingProcessor.TextFocusingProcessor textProcessor = new EachFocusingProcessor.TextFocusingProcessor(textRecognizer, new TextBlockTracker());
                textRecognizer.setProcessor(textProcessor);
            }

            builder.add(faceDetector);
            builder.add(barcodeDetector);
            builder.add(textRecognizer);
            mMultiDetector = builder.build();

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
            CameraSource.Builder builder = new CameraSource.Builder(activity, mMultiDetector);

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
            if (initAllDetector()) {
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

        if (mStatus == Status.Recognizing) {
            if (mMultiDetector != null) {
                mMultiDetector.release();
                mMultiDetector = null;
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

            mShownPreviewSize.set(mPreviewOverlay.getWidth(), mPreviewOverlay.getHeight());
            start();
        }
    };

    private MultiProcessor.Factory mMultiProcessFactory = new MultiProcessor.Factory() {
        @Override
        public Tracker create(Object o) {
            return new AllTracker();
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
                                    Locale.getDefault(),
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
            for (Map.Entry<AbstractLocalTracker, Object> entry : mDrawObjectMap.entrySet()) {
                AbstractLocalTracker tracker = entry.getKey();

                if (tracker.mVisible) {
                    Object item = entry.getValue();

                    if (mRealPreviewSize != null) {
                        float xRate = mShownPreviewSize.x / mRealPreviewSize.getWidth();
                        float yRate = mShownPreviewSize.y / mRealPreviewSize.getHeight();
                        RectF bound = tracker.getBounds(item);

                        MultiDetectFromCameraFragment.this.drawQuadLine(canvas, null, bound.left * xRate, bound.top * yRate, bound.width() * xRate, bound.height() * yRate, tracker.mLinePaint, Frame.ROTATION_0);

                        if (item instanceof TextBlock) {
                            List<? extends Text> childTextComponentList = ((TextBlock) item).getComponents();
                            boolean haveChildTextComponent = ((childTextComponentList != null) && (childTextComponentList.size() > 0));

                            if (haveChildTextComponent) {
                                expandTextComponent(canvas, Frame.ROTATION_0, xRate, yRate, 1, childTextComponentList);
                            }
                        }
                    }

                    builder.append(tracker.logOutput(item));
                }
            }

            mDetectedInformationTv.setText(builder.toString());
        }
    };

    private void expandTextComponent(Canvas fullImageCanvas, int rotation, float xRate, float yRate, int indent, List<? extends Text> textComponentList) {
        if (textComponentList != null && textComponentList.size() > 0) {
            Activity activity = getActivity();
            Paint linePaint = new Paint();
            linePaint.setStrokeWidth(activity.getResources().getDimensionPixelSize(R.dimen.face_line_width) / 2);
            int indentModThree = indent % 3;

            if (indentModThree == 0) {
                linePaint.setARGB(100, 255 - indent, 0, 0);
            } else if (indentModThree == 1) {
                linePaint.setARGB(100, 0, 255 - indent, 0);
            } else {
                linePaint.setARGB(100, 0, 0, 255 - indent);
            }

            for (Text textComponent : textComponentList) {
                List<? extends Text> childTextComponentList = textComponent.getComponents();
                boolean haveChildTextComponent = ((childTextComponentList != null) && (childTextComponentList.size() > 0));

                Rect bounds = textComponent.getBoundingBox();

                // テキストを囲う線を描画
                // 親要素を囲う線と重複させないために、描画開始位置を線幅分、中にずらす。そして幅・高さはずらした分を両端から引く必要がある
                drawQuadLine(fullImageCanvas, null, bounds.left * xRate + linePaint.getStrokeWidth() * indent, bounds.top * yRate + linePaint.getStrokeWidth() * indent, bounds.width() * xRate - linePaint.getStrokeWidth() * indent * 2, bounds.height() * yRate - linePaint.getStrokeWidth() * indent * 2, linePaint, rotation);

                if (haveChildTextComponent) {
                    expandTextComponent(fullImageCanvas, rotation, xRate, yRate, indent + 1, childTextComponentList);
                }
            }
        }
    }

    private interface TrackerCommonIf {
        String getDisplayValue(Object item);

        RectF getBounds(Object item);

        String logOutput(Object item);
    }

    abstract private class AbstractLocalTracker<T> extends Tracker<T> implements TrackerCommonIf {
        protected Paint mLinePaint;
        protected boolean mVisible = false;

        public AbstractLocalTracker() {
            super();
            mLinePaint = new Paint();
            mLinePaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.face_line_width));
            mLinePaint.setARGB(100, 255, 0, 0);
        }

        @Override
        public void onNewItem(int i, T o) {
            super.onNewItem(i, o);
            mVisible = true;
            mDrawObjectMap.put(this, o);
            mPreviewOverlay.postInvalidate();
        }

        @Override
        public void onUpdate(Detector.Detections<T> detections, T o) {
            super.onUpdate(detections, o);
            mVisible = true;
            mDrawObjectMap.put(this, o);
            mPreviewOverlay.postInvalidate();
        }

        @Override
        public void onMissing(Detector.Detections<T> detections) {
            super.onMissing(detections);
            super.onMissing(detections);
            mVisible = false;
            mPreviewOverlay.postInvalidate();
        }

        @Override
        public void onDone() {
            super.onDone();
            mDrawObjectMap.remove(this);
        }
    }

    private class AllTracker extends AbstractLocalTracker {
        private TrackerCommonIf[] mTrackerArray;

        public AllTracker() {
            mTrackerArray = new TrackerCommonIf[]{
                    new FaceTracker(),
                    new BarcodeTracker(),
                    new TextBlockTracker(),
            };
        }

        @Override
        public String getDisplayValue(Object item) {
            AllDetector.DetectorType detectorType = AllDetector.DetectorType.getDetectorTypeFromItem(item);
            return mTrackerArray[detectorType.ordinal()].getDisplayValue(item);
        }

        @Override
        public RectF getBounds(Object item) {
            AllDetector.DetectorType detectorType = AllDetector.DetectorType.getDetectorTypeFromItem(item);
            return mTrackerArray[detectorType.ordinal()].getBounds(item);
        }

        @Override
        public String logOutput(Object item) {
            AllDetector.DetectorType detectorType = AllDetector.DetectorType.getDetectorTypeFromItem(item);
            return mTrackerArray[detectorType.ordinal()].logOutput(item);
        }
    }

    private class FaceTracker extends Tracker<Face> implements TrackerCommonIf {
        @Override
        public String getDisplayValue(Object item) {
            String ret = null;

            if ((item != null) && (item instanceof Face)) {
                ret = String.valueOf(((Face) item).getId());
            }

            return ret;
        }

        @Override
        public RectF getBounds(Object item) {
            RectF ret = null;

            if ((item != null) && (item instanceof Face)) {
                Face castedItem = (Face) item;
                PointF position = castedItem.getPosition();
                ret = new RectF(position.x, position.y, position.x + castedItem.getWidth(), position.y + castedItem.getHeight());
            }

            return ret;
        }

        @Override
        public String logOutput(Object item) {
            String ret = null;

            if (item != null && item instanceof Face) {
                StringBuilder tempRet = logOutputFace(null, 0, (Face) item);
                if (tempRet != null) {
                    ret = tempRet.toString();
                }
            }

            return ret;
        }
    }

    private class BarcodeTracker extends Tracker<Barcode> implements TrackerCommonIf {
        @Override
        public String getDisplayValue(Object item) {
            String ret = null;

            if ((item != null) && (item instanceof Barcode)) {
                ret = ((Barcode) item).displayValue;
            }

            return ret;
        }

        @Override
        public RectF getBounds(Object item) {
            RectF ret = null;

            if ((item != null) && (item instanceof Barcode)) {
                Rect bounds = ((Barcode) item).getBoundingBox();
                ret = new RectF(bounds.left, bounds.top, bounds.right, bounds.bottom);
            }

            return ret;
        }

        @Override
        public String logOutput(Object item) {
            String ret = "";

            if (item != null && item instanceof Barcode) {
                StringBuilder tempRet = logOutputBarcode(null, 0, (Barcode) item);
                if (tempRet != null) {
                    ret = tempRet.toString();
                }
            }

            return ret;
        }
    }

    private class TextBlockTracker extends Tracker<TextBlock> implements TrackerCommonIf {
        @Override
        public String getDisplayValue(Object item) {
            String ret = null;

            if ((item != null) && (item instanceof Barcode)) {
                ret = ((TextBlock) item).getValue();
            }

            return ret;
        }

        @Override
        public RectF getBounds(Object item) {
            RectF ret = null;

            if ((item != null) && (item instanceof TextBlock)) {
                Rect bounds = ((TextBlock) item).getBoundingBox();
                ret = new RectF(bounds.left, bounds.top, bounds.right, bounds.bottom);
            }

            return ret;
        }

        @Override
        public String logOutput(Object item) {
            String ret = "";

            if (item != null && item instanceof TextBlock) {
                StringBuilder tempRet = logOutputTextBlock(null, 0, (TextBlock) item);
                if (tempRet != null) {
                    ret = tempRet.toString();
                }
            }

            return ret;
        }
    }
}
