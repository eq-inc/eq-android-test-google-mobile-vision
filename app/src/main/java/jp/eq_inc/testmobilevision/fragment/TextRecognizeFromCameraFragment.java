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
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.util.DisplayMetrics;
import android.util.SparseArray;
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
import com.google.android.gms.vision.FocusingProcessor;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
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

import jp.co.thcomp.util.LogUtil;
import jp.eq_inc.testmobilevision.R;
import jp.eq_inc.testmobilevision.view.GraphicOverlay;

public class TextRecognizeFromCameraFragment extends AbstractDetectFragment {
    private enum Status {
        Init, Recognizing,
    }

    private SurfaceView mCameraPreview;
    private GraphicOverlay mPreviewOverlay;
    private TextView mDetectedInformationTv;
    private TextRecognizer mTextRecognizer;
    private CameraSource mCameraSource;
    private Status mStatus = Status.Init;
    private HashMap<TextTracker, TextBlock> mDrawTextBlockMap = new HashMap<TextTracker, TextBlock>();
    private int mReservedRequestedOrientation = 0;
    private Size mRealPreviewSize = null;
    private PointF mShownPreviewSize = new PointF();
    private boolean mTakingPicture = false;

    public TextRecognizeFromCameraFragment() {
        // Required empty public constructor
    }

    public static TextRecognizeFromCameraFragment newInstance(Bundle param) {
        TextRecognizeFromCameraFragment fragment = new TextRecognizeFromCameraFragment();
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
        View view = inflater.inflate(R.layout.fragment_text_recognize_from_camera, container, false);

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

    private boolean initTextRecognizer() {
        boolean ret = false;

        try {
            Activity activity = getActivity();

            if (mTextRecognizer != null) {
                mTextRecognizer.release();
                mTextRecognizer = null;
            }
            mTextRecognizer = new TextRecognizer.Builder(activity).build();

            // use multi processor
            SwitchCompat tempSwitch = (SwitchCompat) activity.findViewById(R.id.scUseMultiProcessor);
            if (tempSwitch.isChecked()) {
                MultiProcessor.Builder<TextBlock> multiProcessorBuilder = new MultiProcessor.Builder<TextBlock>(mMultiProcessFactory);
                mTextRecognizer.setProcessor(multiProcessorBuilder.build());
            } else {
                FocusingProcessor<TextBlock> focusingProcessor = new FocusingProcessor<TextBlock>(mTextRecognizer, new TextTracker()) {
                    @Override
                    public int selectFocus(Detector.Detections<TextBlock> detections) {
                        SparseArray<TextBlock> detectedItems = detections.getDetectedItems();
                        int selectedItem = 0;
                        int largestSize = 0;
                        for (int i = 0, size = detectedItems.size(); i < size; i++) {
                            TextBlock detectedItem = detectedItems.valueAt(i);
                            Rect bounds = detectedItem.getBoundingBox();
                            int tempBoundsSize = bounds.width() * bounds.height();
                            if (largestSize < tempBoundsSize) {
                                largestSize = tempBoundsSize;
                                selectedItem = detectedItems.keyAt(i);
                            }
                        }

                        return selectedItem;
                    }
                };
                mTextRecognizer.setProcessor(focusingProcessor);
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
            CameraSource.Builder builder = new CameraSource.Builder(activity, mTextRecognizer);

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
            if (initTextRecognizer()) {
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
            if (mTextRecognizer != null) {
                mTextRecognizer.release();
                mTextRecognizer = null;
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

    private MultiProcessor.Factory<TextBlock> mMultiProcessFactory = new MultiProcessor.Factory<TextBlock>() {
        @Override
        public Tracker<TextBlock> create(TextBlock textBlock) {
            return new TextTracker();
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
            for (Map.Entry<TextTracker, TextBlock> entry : mDrawTextBlockMap.entrySet()) {
                TextTracker tracker = entry.getKey();

                if (tracker.mVisible) {
                    TextBlock textBlock = entry.getValue();

                    if (mRealPreviewSize != null) {
                        float xRate = mShownPreviewSize.x / mRealPreviewSize.getWidth();
                        float yRate = mShownPreviewSize.y / mRealPreviewSize.getHeight();

                        List<? extends Text> childTextComponentList = textBlock.getComponents();
                        boolean haveChildTextComponent = ((childTextComponentList != null) && (childTextComponentList.size() > 0));
                        Rect textBlockBound = textBlock.getBoundingBox();

                        TextRecognizeFromCameraFragment.this.drawQuadLine(canvas, null, textBlockBound.left * xRate, textBlockBound.top * yRate, textBlockBound.width() * xRate, textBlockBound.height() * yRate, tracker.mLinePaint, Frame.ROTATION_0);

                        if(haveChildTextComponent){
                            expandTextComponent(canvas, Frame.ROTATION_0, xRate, yRate, 1, childTextComponentList);
                        }
                    }

                    builder.append("Value: ").append(textBlock.getValue()).append("\n");

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

    private class TextTracker extends Tracker<TextBlock> {
        private Paint mLinePaint;
        private boolean mVisible = false;

        public TextTracker() {
            super();
            mLinePaint = new Paint();
            mLinePaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.face_line_width));
            mLinePaint.setARGB(100, 255, 0, 0);
        }

        @Override
        public void onNewItem(int i, TextBlock textBlock) {
            LogUtil.d("", "onNewItem: " + textBlock.getValue());

            super.onNewItem(i, textBlock);
            mVisible = true;
            mDrawTextBlockMap.put(this, textBlock);
            mPreviewOverlay.postInvalidate();
        }

        @Override
        public void onUpdate(Detector.Detections<TextBlock> detections, TextBlock textBlock) {
            super.onUpdate(detections, textBlock);

            mVisible = true;
            mDrawTextBlockMap.put(this, textBlock);
            mPreviewOverlay.postInvalidate();
        }

        @Override
        public void onMissing(Detector.Detections<TextBlock> detections) {
            if (mDrawTextBlockMap.containsKey(this)) {
                LogUtil.d("", "onMissing: " + mDrawTextBlockMap.get(this).getValue());
            }

            super.onMissing(detections);
            mVisible = false;
            mPreviewOverlay.postInvalidate();
        }

        @Override
        public void onDone() {
            if (mDrawTextBlockMap.containsKey(this)) {
                LogUtil.d("", "onDone: " + mDrawTextBlockMap.get(this).getValue());
            }

            super.onDone();
            mDrawTextBlockMap.remove(this);
        }
    }
}
