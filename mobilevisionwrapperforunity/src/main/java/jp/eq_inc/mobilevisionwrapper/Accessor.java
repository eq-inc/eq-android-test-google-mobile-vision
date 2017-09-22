package jp.eq_inc.mobilevisionwrapper;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import jp.eq_inc.mobilevision.detector.AllDetector;

public abstract class Accessor {
    protected Activity mActivity;
    protected FaceDetector.Builder mFaceDetectBuilder;
    protected BarcodeDetector.Builder mBarcodeDetectBuilder;
    protected TextRecognizer.Builder mTextRecognizeBuilder;
    protected Thread mDetectThread;
    protected byte[] mImageBuffer;
    protected int mImageFormat;
    protected int mImageWidth;
    protected int mImageHeight;
    protected int[] mImageStride;

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

    public void setImageBuffer(byte[] imageBuffer, int imageFormat, int imageWidth, int imageHeight, int[] imageStride) {
        synchronized (this) {
            mImageBuffer = imageBuffer;
            mImageFormat = imageFormat;
            mImageWidth = imageWidth;
            mImageHeight = imageHeight;
            mImageStride = imageStride;

            notify();
        }
    }

    public interface OnDetectedItemListener {
        void onDetected(SparseArray<Face> result);
    }

    private static class AccessorForUnity extends Accessor {
        private AccessorForUnity(Activity activity) {
            super(activity);
        }

        @Override
        public void startDetect(final int intervalMS, final OnDetectedItemListener faceDetectListener, final OnDetectedItemListener barcodeDetectListener, final OnDetectedItemListener textRecognizeListener) {
            if (mDetectThread == null) {
                mDetectThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int detectorTypeCount = AllDetector.DetectorType.values().length;
                        OnDetectedItemListener[] listenerArray = new OnDetectedItemListener[detectorTypeCount];
                        Detector[] detectorArray = new Detector[detectorTypeCount];
                        boolean noDetector = true;

                        if (faceDetectListener != null) {
                            detectorArray[AllDetector.DetectorType.Face.ordinal()] = mFaceDetectBuilder.build();
                            listenerArray[AllDetector.DetectorType.Face.ordinal()] = faceDetectListener;
                            noDetector = false;
                        }
                        if (barcodeDetectListener != null) {
                            detectorArray[AllDetector.DetectorType.Barcode.ordinal()] = mBarcodeDetectBuilder.build();
                            listenerArray[AllDetector.DetectorType.Barcode.ordinal()] = barcodeDetectListener;
                            noDetector = false;
                        }
                        if (textRecognizeListener != null) {
                            detectorArray[AllDetector.DetectorType.Text.ordinal()] = mTextRecognizeBuilder.build();
                            listenerArray[AllDetector.DetectorType.Text.ordinal()] = textRecognizeListener;
                            noDetector = false;
                        }

                        if (!noDetector) {
                            byte[] imageBuffer = null;
                            int imageFormat = 0;
                            int imageWidth = 0;
                            int imageHeight = 0;
                            int[] imageStride = null;
                            int frameIndex = 0;

                            while (mDetectThread != null) {
                                synchronized (AccessorForUnity.this) {
                                    if (mImageBuffer == null) {
                                        try {
                                            Log.d(AccessorForUnity.class.getSimpleName(), "sleep in");
                                            AccessorForUnity.this.wait();
                                            Log.d(AccessorForUnity.class.getSimpleName(), "sleep out");
                                        } catch (InterruptedException e) {
                                        }
                                    }

                                    imageBuffer = mImageBuffer;
                                    imageFormat = mImageFormat;
                                    imageWidth = mImageWidth;
                                    imageHeight = mImageHeight;
                                    imageStride = mImageStride;

                                    mImageBuffer = null;
                                }

                                if (imageBuffer != null) {
                                    SparseArray ret = null;
                                    Frame.Builder frameBuilder = new Frame.Builder();
                                    frameBuilder.setImageData(ByteBuffer.wrap(imageBuffer), imageWidth, imageHeight, imageFormat);
                                    Frame frame = frameBuilder.build();

                                    if (frameIndex < 100) {
                                        YuvImage tempYuvImage = new YuvImage(imageBuffer, imageFormat, imageWidth, imageHeight, imageStride);

                                        if (tempYuvImage != null) {
                                            FileOutputStream outStream = null;

                                            try {
                                                outStream = new FileOutputStream(String.format("/mnt/sdcard/frame_%04d.png", frameIndex));
                                                tempYuvImage.compressToJpeg(new Rect(0, 0, imageWidth, imageHeight), 100, outStream);
                                            } catch (FileNotFoundException e) {
                                                e.printStackTrace();
                                            } finally {
                                                if (outStream != null) {
                                                    try {
                                                        outStream.close();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    outStream = null;
                                                }
                                            }
                                        }
                                    }

                                    for (int i = 0; i < detectorTypeCount; i++) {
                                        if (detectorArray[i] != null) {
                                            ret = detectorArray[i].detect(frame);
                                            listenerArray[i].onDetected(ret);
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
                mDetectThread.start();
            }
        }
    }

    public void stopDetect() {
        if (mDetectThread != null) {
            mDetectThread = null;
        }
    }
}
