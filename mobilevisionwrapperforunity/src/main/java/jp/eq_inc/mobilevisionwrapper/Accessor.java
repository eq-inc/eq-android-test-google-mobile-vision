package jp.eq_inc.mobilevisionwrapper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;

import jp.co.thcomp.activity.HandleResultActivity;
import jp.eq_inc.mobilevision.detector.AllDetector;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class Accessor {
    private static final String ACTION_CONFIRM_VIRTUAL_DISPLAY = "ACTION_CONFIRM_VIRTUAL_DISPLAY";
    private static final int REQUEST_CODE_CONFIRM_VIRTUAL_DISPLAY = "REQUEST_CODE_CONFIRM_VIRTUAL_DISPLAY".hashCode() & 0x0000FFFF;

    protected Activity mActivity;
    protected FaceDetector.Builder mFaceDetectBuilder;
    protected BarcodeDetector.Builder mBarcodeDetectBuilder;
    protected TextRecognizer.Builder mTextRecognizeBuilder;
    protected Thread mDetectThread;

    public static Accessor createAccessorForUnity(Activity activity) {
        return new AccessorForUnity(activity);
    }

    private static SurfaceView findSurfaceView(View rootView) {
        SurfaceView ret = null;

        if (rootView instanceof ViewGroup) {
            ViewGroup rootViewGroup = (ViewGroup) rootView;
            for (int i = 0, size = rootViewGroup.getChildCount(); ((ret == null) && (i < size)); i++) {
                View childView = rootViewGroup.getChildAt(i);

                if (childView instanceof SurfaceView) {
                    ret = (SurfaceView) childView;
                    break;
                } else if (childView instanceof ViewGroup) {
                    ret = findSurfaceView(childView);
                }
            }
        }

        return ret;
    }

    abstract public void startDetect(int intervalMS, OnDetectedItemListener faceDetectListener, OnDetectedItemListener barcodeDetectListener, OnDetectedItemListener textRecognizeListener);

    private Accessor(Activity activity) {
        if (activity == null) {
            throw new NullPointerException("activity == null");
        }

        mActivity = activity;
        mFaceDetectBuilder = new FaceDetector.Builder(activity);
        mBarcodeDetectBuilder = new BarcodeDetector.Builder(activity);
        mTextRecognizeBuilder = new TextRecognizer.Builder(activity);
    }

    public void setClassificationType(int classificationType) {
        mFaceDetectBuilder.setClassificationType(classificationType);
    }

    public void setLandmarkType(int landmarkType) {
        mFaceDetectBuilder.setLandmarkType(landmarkType);
    }

    public void setMinFaceSize(float proportionalMinFaceSize) {
        mFaceDetectBuilder.setMinFaceSize(proportionalMinFaceSize);
    }

    public void setMode(int mode) {
        mFaceDetectBuilder.setMode(mode);
    }

    public void setProminentFaceOnly(boolean prominentFaceOnly) {
        mFaceDetectBuilder.setProminentFaceOnly(prominentFaceOnly);
    }

    public void setTrackingEnabled(boolean trackingEnabled) {
        mFaceDetectBuilder.setTrackingEnabled(trackingEnabled);
    }

    public void setBarcodeFormats(int format) {
        mBarcodeDetectBuilder.setBarcodeFormats(format);
    }

    public void release() {
    }

    public interface OnDetectedItemListener {
        void onDetected(SparseArray<Face> result);
    }

    private static class AccessorForUnity extends Accessor {
        private enum Status {
            Init, Confirming, Confirmed, Detecting, Stopping
        }

        private SurfaceView mSourceSurfaceView;
        private MediaCodec mVideoEncoder;
        private MediaProjection mMediaProjection;
        private VirtualDisplay mVirtualDisplay;
        private Surface mInputSurface;
        private Status mStatus = Status.Init;
        private int mDetectIntervalMS = 0;
        private OnDetectedItemListener mFaceDetectListener;
        private OnDetectedItemListener mBarcodeDetectListener;
        private OnDetectedItemListener mTextRecognizeListener;

        private BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(AccessorForUnity.this.getClass().getSimpleName(), "Status = " + mStatus);

                if ((mStatus == Status.Init) || (mStatus == Status.Confirming)) {
                    View decorView = mActivity.getWindow().getDecorView();
                    mSourceSurfaceView = findSurfaceView(decorView);

                    if (mSourceSurfaceView != null) {
                        Rect surfaceFrame = mSourceSurfaceView.getHolder().getSurfaceFrame();
                        Display defaultDisplay = mActivity.getWindowManager().getDefaultDisplay();
                        DisplayMetrics defaultDisplayMetrics = new DisplayMetrics();
                        defaultDisplay.getMetrics(defaultDisplayMetrics);

                        MediaProjectionManager mpManager = (MediaProjectionManager) mActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                        mMediaProjection = mpManager.getMediaProjection(intent.getIntExtra(HandleResultActivity.INTENT_INT_EXTRA_RESULT_CODE_FROM_TARGET, Activity.RESULT_CANCELED), (Intent) intent.getParcelableExtra(HandleResultActivity.INTENT_PARCELABLE_EXTRA_TRANSFER_INTENT));

                        // Video encoderの準備
                        prepareVideoEncoder();

                        if (mStatus == Status.Init) {
                            mStatus = Status.Confirmed;
                        } else {
                            mStatus = Status.Detecting;
                            mVirtualDisplay = mMediaProjection.createVirtualDisplay("CapturingDisplay", surfaceFrame.width(), surfaceFrame.height(), defaultDisplayMetrics.densityDpi, 0, mInputSurface, null, null);
                            (mDetectThread = new Thread(mDetectRunnable)).start();
                        }
                    } else {
                        mStatus = Status.Init;
                    }
                }
            }
        };
        private Runnable mDetectRunnable = new Runnable() {
            @Override
            public void run() {
                MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
                int detectorTypeCount = AllDetector.DetectorType.values().length;
                OnDetectedItemListener[] listenerArray = new OnDetectedItemListener[detectorTypeCount];
                Detector[] detectorArray = new Detector[detectorTypeCount];
                boolean noDetector = true;

                if (mFaceDetectListener != null) {
                    detectorArray[AllDetector.DetectorType.Face.ordinal()] = mFaceDetectBuilder.build();
                    listenerArray[AllDetector.DetectorType.Face.ordinal()] = mFaceDetectListener;
                    noDetector = false;
                }
                if (mBarcodeDetectListener != null) {
                    detectorArray[AllDetector.DetectorType.Barcode.ordinal()] = mBarcodeDetectBuilder.build();
                    listenerArray[AllDetector.DetectorType.Barcode.ordinal()] = mBarcodeDetectListener;
                    noDetector = false;
                }
                if (mTextRecognizeListener != null) {
                    detectorArray[AllDetector.DetectorType.Text.ordinal()] = mTextRecognizeBuilder.build();
                    listenerArray[AllDetector.DetectorType.Text.ordinal()] = mTextRecognizeListener;
                    noDetector = false;
                }

                Log.d(AccessorForUnity.this.getClass().getSimpleName(), "noDetector = " + noDetector + ", Status = " + mStatus);
                while (!noDetector && (mStatus == Status.Detecting)) {
                    int bufferIndex = 0;
                    try{
                        bufferIndex = mVideoEncoder.dequeueOutputBuffer(videoBufferInfo, 0);

                        Log.d(AccessorForUnity.this.getClass().getSimpleName(), "bufferIndex = " + bufferIndex);
                        if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        } else if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        } else if (bufferIndex < 0) {
                        } else {
                            Bitmap frameBitmap = null;
                            Image frameImage = mVideoEncoder.getOutputImage(bufferIndex);

                            if(frameImage != null){
                                Image.Plane[] planes = frameImage.getPlanes();

                                Log.d(AccessorForUnity.this.getClass().getSimpleName(), "encode format = " + frameImage.getFormat());
                                switch (frameImage.getFormat()) {
                                    case ImageFormat.JPEG:
                                        frameBitmap = BitmapFactory.decodeByteArray(planes[0].getBuffer().array(), frameImage.getWidth(), frameImage.getHeight());
                                        break;
                                    case ImageFormat.YUV_420_888:
                                        break;
                                    case ImageFormat.YUV_422_888:
                                        break;
                                    case ImageFormat.YUV_444_888:
                                        break;
                                    case ImageFormat.FLEX_RGB_888:
                                        break;
                                    case ImageFormat.FLEX_RGBA_8888:
                                        break;
                                    case ImageFormat.RAW_SENSOR:
                                        frameBitmap = BitmapFactory.decodeByteArray(planes[0].getBuffer().array(), frameImage.getWidth(), frameImage.getHeight());
                                        break;
                                    case ImageFormat.RAW_PRIVATE:
                                        frameBitmap = BitmapFactory.decodeByteArray(planes[0].getBuffer().array(), frameImage.getWidth(), frameImage.getHeight());
                                        break;
                                }

                                Log.d(AccessorForUnity.class.getSimpleName(), "frameBitmap = " + frameBitmap);
                                if (frameBitmap != null) {
                                    try {
                                        Frame.Builder frameBuilder = new Frame.Builder();
                                        frameBuilder.setBitmap(frameBitmap);
                                        Frame frame = frameBuilder.build();
                                        SparseArray ret = null;

                                        for (int i = 0; i < detectorTypeCount; i++) {
                                            if (detectorArray[i] != null) {
                                                ret = detectorArray[i].detect(frame);
                                                listenerArray[i].onDetected(ret);
                                            }
                                        }
                                    } finally {
                                        if (frameBitmap != null) {
                                            frameBitmap.recycle();
                                            frameBitmap = null;
                                        }
                                    }
                                }
                            }
                            mVideoEncoder.releaseOutputBuffer(bufferIndex, false);
                        }
                    }catch(IllegalStateException e){
                        e.printStackTrace();
                    }

                    synchronized (AccessorForUnity.this) {
                        try {
                            AccessorForUnity.this.wait(mDetectIntervalMS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                mDetectThread = null;
                mVirtualDisplay.release();
                mVirtualDisplay = null;
                mVideoEncoder.stop();
                mStatus = Status.Confirmed;
            }
        };

        private AccessorForUnity(Activity activity) {
            super(activity);
        }

        @Override
        public void startDetect(final int intervalMS, final OnDetectedItemListener faceDetectListener, final OnDetectedItemListener barcodeDetectListener, final OnDetectedItemListener textRecognizeListener) {
            mFaceDetectListener = faceDetectListener;
            mBarcodeDetectListener = barcodeDetectListener;
            mTextRecognizeListener = textRecognizeListener;

            Log.d(AccessorForUnity.class.getSimpleName(), "startDetect: Status = " + mStatus);
            if (mStatus == Status.Init) {
                mDetectIntervalMS = intervalMS;
                LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(mActivity);
                broadcastManager.registerReceiver(mReceiver, new IntentFilter(ACTION_CONFIRM_VIRTUAL_DISPLAY));

                MediaProjectionManager mpManager = (MediaProjectionManager) mActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                Intent intent = new Intent();
                Intent confirmIntent = mpManager.createScreenCaptureIntent();

                intent.setClass(mActivity, HandleResultActivity.class);
                intent.putExtra(HandleResultActivity.INTENT_PARCELABLE_EXTRA_TRANSFER_INTENT, confirmIntent);
                intent.putExtra(HandleResultActivity.INTENT_BOOLEAN_EXTRA_ENABLE_DEBUG_LOG, true);
                intent.putExtra(HandleResultActivity.INTENT_INT_EXTRA_HOWTO_CALLBACK, HandleResultActivity.CALLBACK_BY_LOCAL_BROADCAST);
                intent.putExtra(HandleResultActivity.INTENT_STRING_EXTRA_CALLBACK_ACTION, ACTION_CONFIRM_VIRTUAL_DISPLAY);
                intent.putExtra(HandleResultActivity.INTENT_INT_EXTRA_REQUEST_CODE, REQUEST_CODE_CONFIRM_VIRTUAL_DISPLAY);
                mActivity.startActivityForResult(intent, REQUEST_CODE_CONFIRM_VIRTUAL_DISPLAY);

                mStatus = Status.Confirming;
            } else if (mStatus == Status.Confirmed) {
                Rect surfaceFrame = mSourceSurfaceView.getHolder().getSurfaceFrame();
                Display defaultDisplay = mActivity.getWindowManager().getDefaultDisplay();
                DisplayMetrics defaultDisplayMetrics = new DisplayMetrics();
                defaultDisplay.getMetrics(defaultDisplayMetrics);

                mVideoEncoder.start();
                mVirtualDisplay = mMediaProjection.createVirtualDisplay("CapturingDisplay", surfaceFrame.width(), surfaceFrame.height(), defaultDisplayMetrics.densityDpi, 0, mInputSurface, null, null);
                (mDetectThread = new Thread(mDetectRunnable)).start();
            }
        }

        @Override
        public void stopDetect() {
            switch (mStatus) {
                case Confirming:
                    break;
                case Detecting:
                    super.stopDetect();
                    LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mReceiver);
                    mStatus = Status.Stopping;
                    break;
            }
        }

        @Override
        public void release() {
            super.release();

            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
                mVirtualDisplay = null;
            }
            if (mVideoEncoder != null) {
                mVideoEncoder.release();
                mVideoEncoder = null;
            }
            if (mInputSurface != null) {
                mInputSurface.release();
                mInputSurface = null;
            }
        }

        private void prepareVideoEncoder() {
            Rect surfaceFrame = mSourceSurfaceView.getHolder().getSurfaceFrame();
            Display defaultDisplay = mActivity.getWindowManager().getDefaultDisplay();
            DisplayMetrics defaultDisplayMetrics = new DisplayMetrics();
            defaultDisplay.getMetrics(defaultDisplayMetrics);

            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_MPEG4, surfaceFrame.width(), surfaceFrame.height());
            int frameRate = 1000 / mDetectIntervalMS;

            // Set some required properties. The media codec may fail if these aren't defined.
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000); // 6Mbps
            format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
            format.setInteger(MediaFormat.KEY_CAPTURE_RATE, frameRate);
            format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1 seconds between I-frames

            // Create a MediaCodec encoder and configure it. Get a Surface we can use for recording into.
            try {
                mVideoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_MPEG4);
                mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mInputSurface = mVideoEncoder.createInputSurface();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void stopDetect() {
        if (mDetectThread != null) {
            mDetectThread = null;
        }
    }
}
