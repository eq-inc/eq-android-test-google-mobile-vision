package jp.eq_inc.mobilevisionwrapper;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLES30;
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
import java.nio.IntBuffer;

import jp.eq_inc.mobilevision.detector.AllDetector;

public abstract class Accessor {
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
                        View decorView = mActivity.getWindow().getDecorView();
                        SurfaceView surfaceView = findSurfaceView(decorView);

                        if (surfaceView != null) {
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
                                Rect frameRect = surfaceView.getHolder().getSurfaceFrame();
                                int[] buffer = new int[frameRect.width() * frameRect.height()];

                                int frameIndex = 0;
                                int[] bufferTypeArray = {GLES20.GL_FRONT, GLES20.GL_BACK, GLES20.GL_FRONT_AND_BACK, GLES20.GL_FRONT_FACE};

                                EGLContext eglContext = EGL14.eglGetCurrentContext();
                                Log.d(Accessor.class.getSimpleName(), "eglContext = " + eglContext);
                                EGLDisplay eglDisplay = EGL14.eglGetCurrentDisplay();
                                Log.d(Accessor.class.getSimpleName(), "eglDisplay = " + eglDisplay);
                                EGLSurface eglReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
                                Log.d(Accessor.class.getSimpleName(), "read eglDisplay = " + eglReadSurface);
                                EGLSurface eglDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
                                Log.d(Accessor.class.getSimpleName(), "draw eglDisplay = " + eglDrawSurface);

                                while (mDetectThread != null) {
                                    SparseArray ret = null;
                                    IntBuffer pixelByteBuffer = IntBuffer.wrap(buffer);
                                    pixelByteBuffer.position(0);
                                    GLES30.glReadBuffer(bufferTypeArray[frameIndex % bufferTypeArray.length]);
                                    Log.d(Accessor.class.getSimpleName(), "glReadBuffer's error is " + GLES30.glGetError());
                                    GLES30.glReadPixels(0, 0, frameRect.width(), frameRect.height(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelByteBuffer);
                                    Log.d(Accessor.class.getSimpleName(), "glReadPixels's error is " + GLES30.glGetError());

                                    int[] frameByteArray = pixelByteBuffer.array();
                                    Log.d(Accessor.class.getSimpleName(),
                                            String.format("%s\n0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X\n" +
                                                            "\t\t\t\t\t\t\t\t\t\t\t\t:\n" +
                                                            "0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X\n" +
                                                            "\t\t\t\t\t\t\t\t\t\t\t\t:\n" +
                                                            "0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X 0x%08X\n",
                                                    (frameIndex % bufferTypeArray.length == 0 ? "GL_FRONT" : (frameIndex % bufferTypeArray.length == 1 ? "GL_BACK" : (frameIndex % bufferTypeArray.length == 2 ? "GL_FRONT_AND_BACK" : "GL_FRONT_FACE"))),
                                                    frameByteArray[0], frameByteArray[1], frameByteArray[2], frameByteArray[3], frameByteArray[4], frameByteArray[5], frameByteArray[6], frameByteArray[7],
                                                    frameByteArray[8], frameByteArray[9], frameByteArray[10], frameByteArray[11], frameByteArray[12], frameByteArray[13], frameByteArray[14], frameByteArray[15],

                                                    frameByteArray[frameByteArray.length / 2 - 8], frameByteArray[frameByteArray.length / 2 - 7], frameByteArray[frameByteArray.length / 2 - 6], frameByteArray[frameByteArray.length / 2 - 5], frameByteArray[frameByteArray.length / 2 - 4], frameByteArray[frameByteArray.length / 2 - 3], frameByteArray[frameByteArray.length / 2 - 2], frameByteArray[frameByteArray.length / 2 - 1],
                                                    frameByteArray[frameByteArray.length / 2], frameByteArray[frameByteArray.length / 2 + 1], frameByteArray[frameByteArray.length / 2 + 2], frameByteArray[frameByteArray.length / 2 + 3], frameByteArray[frameByteArray.length / 2 + 4], frameByteArray[frameByteArray.length / 2 + 5], frameByteArray[frameByteArray.length / 2 + 6], frameByteArray[frameByteArray.length / 2 + 7],

                                                    frameByteArray[frameByteArray.length - 16], frameByteArray[frameByteArray.length - 15], frameByteArray[frameByteArray.length - 14], frameByteArray[frameByteArray.length - 13], frameByteArray[frameByteArray.length - 12], frameByteArray[frameByteArray.length - 11], frameByteArray[frameByteArray.length - 10], frameByteArray[frameByteArray.length - 9],
                                                    frameByteArray[frameByteArray.length - 8], frameByteArray[frameByteArray.length - 7], frameByteArray[frameByteArray.length - 6], frameByteArray[frameByteArray.length - 5], frameByteArray[frameByteArray.length - 4], frameByteArray[frameByteArray.length - 3], frameByteArray[frameByteArray.length - 2], frameByteArray[frameByteArray.length - 1]));

                                    Bitmap frameBitmap = null;

                                    try {
                                        frameBitmap = Bitmap.createBitmap(frameByteArray, frameRect.width(), frameRect.height(), Bitmap.Config.ARGB_8888);
                                        if (frameIndex < 100) {
                                            FileOutputStream fileOutStream = null;
                                            try {
                                                fileOutStream = new FileOutputStream(String.format("/mnt/sdcard/frame_%04d.png", frameIndex));
                                                frameBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutStream);
                                            } catch (FileNotFoundException e) {
                                                e.printStackTrace();
                                            } finally {
                                                if (fileOutStream != null) {
                                                    try {
                                                        fileOutStream.close();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        }
                                        frameIndex++;
                                        Log.d("", "frameBitmap = " + frameBitmap);

                                        Frame.Builder frameBuilder = new Frame.Builder();
                                        frameBuilder.setBitmap(frameBitmap);
                                        Frame frame = frameBuilder.build();

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

                                    synchronized (AccessorForUnity.this) {
                                        try {
                                            AccessorForUnity.this.wait(intervalMS);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            } else {
                                Log.d(Accessor.class.getSimpleName(), "no listener, finished");
                            }
                        } else {
                            Log.e(Accessor.class.getSimpleName(), "not found SurfaceView");
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
