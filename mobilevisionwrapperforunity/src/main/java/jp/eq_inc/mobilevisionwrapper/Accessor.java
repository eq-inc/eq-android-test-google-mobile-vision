package jp.eq_inc.mobilevisionwrapper;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.nio.IntBuffer;

public class Accessor {
    public Frame.Builder getFrameBuilder() {
        return new Frame.Builder();
    }

    public static SparseArray<Face> detectFaceForUnity(Activity activity) {
        return detectFaceForUnity(activity, null);
    }

    public static SparseArray<Face> detectFaceForUnity(Activity activity, FaceDetector.Builder faceDetectorBuilder) {
        SparseArray<Face> ret = null;
        View decorView = activity.getWindow().getDecorView();
        SurfaceView surfaceView = findSurfaceView(decorView);

        if (surfaceView != null) {
            Rect frameRect = surfaceView.getHolder().getSurfaceFrame();
            int[] buffer = new int[frameRect.width() * frameRect.height() /* (Integer.SIZE / Byte.SIZE)*/];
            IntBuffer pixelByteBuffer = IntBuffer.wrap(buffer);
            GLES30.glReadBuffer(GLES20.GL_BACK);
            GLES30.glReadPixels(0, 0, frameRect.width(), frameRect.height(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelByteBuffer);

            int[] frameByteArray = pixelByteBuffer.array();
            Bitmap frameBitmap = null;

            try {
                frameBitmap = Bitmap.createBitmap(frameByteArray, frameRect.width(), frameRect.height(), Bitmap.Config.ARGB_8888);
                Log.d("", "frameBitmap = " + frameBitmap);

//                if(frameBitmap == null){
//                    pixelByteBuffer = IntBuffer.wrap(buffer);
//                    GLES30.glReadBuffer(GLES20.GL_FRONT);
//                    GLES30.glReadPixels(0, 0, frameRect.width(), frameRect.height(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelByteBuffer);
//                    Log.d("",
//                            String.format(
//                                    "%X %X %X %X %X %X %X %X %X %X %X %X %X %X %X %X",
//                                    buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5], buffer[6], buffer[7],
//                                    buffer[8], buffer[9], buffer[10], buffer[11], buffer[12], buffer[13], buffer[14], buffer[15]));
//                    frameByteArray = pixelByteBuffer.array();
//                    frameBitmap = Bitmap.createBitmap(frameByteArray, frameRect.width(), frameRect.height(), Bitmap.Config.ARGB_8888);
//                    Log.d("", "frameBitmap2 = " + frameBitmap);
//                }
//
//                if(frameBitmap == null){
//                    pixelByteBuffer = IntBuffer.wrap(buffer);
//                    GLES30.glReadBuffer(GLES20.GL_FRONT_AND_BACK);
//                    GLES30.glReadPixels(0, 0, frameRect.width(), frameRect.height(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelByteBuffer);
//                    Log.d("",
//                            String.format(
//                                    "%X %X %X %X %X %X %X %X %X %X %X %X %X %X %X %X",
//                                    buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5], buffer[6], buffer[7],
//                                    buffer[8], buffer[9], buffer[10], buffer[11], buffer[12], buffer[13], buffer[14], buffer[15]));
//                    frameByteArray = pixelByteBuffer.array();
//                    frameBitmap = Bitmap.createBitmap(frameByteArray, frameRect.width(), frameRect.height(), Bitmap.Config.ARGB_8888);
//                    Log.d("", "frameBitmap3 = " + frameBitmap);
//                }
//
//                if(frameBitmap == null){
//                    pixelByteBuffer = IntBuffer.wrap(buffer);
//                    GLES30.glReadBuffer(GLES20.GL_FRONT_FACE);
//                    GLES30.glReadPixels(0, 0, frameRect.width(), frameRect.height(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelByteBuffer);
//                    Log.d("",
//                            String.format(
//                                    "%X %X %X %X %X %X %X %X %X %X %X %X %X %X %X %X",
//                                    buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5], buffer[6], buffer[7],
//                                    buffer[8], buffer[9], buffer[10], buffer[11], buffer[12], buffer[13], buffer[14], buffer[15]));
//                    frameByteArray = pixelByteBuffer.array();
//                    frameBitmap = Bitmap.createBitmap(frameByteArray, frameRect.width(), frameRect.height(), Bitmap.Config.ARGB_8888);
//                    Log.d("", "frameBitmap4 = " + frameBitmap);
//                }

                Frame.Builder frameBuilder = new Frame.Builder();
                frameBuilder.setBitmap(frameBitmap);
                Frame frame = frameBuilder.build();

                if (faceDetectorBuilder == null) {
                    faceDetectorBuilder = new FaceDetector.Builder(activity);
                }

                FaceDetector faceDetector = null;
                try {
                    faceDetector = faceDetectorBuilder.build();
                    ret = faceDetector.detect(frame);
                } finally {
                    if (faceDetector != null) {
                        faceDetector.release();
                    }
                }
            } finally {
                if (frameBitmap != null) {
                    frameBitmap.recycle();
                    frameBitmap = null;
                }
            }
        } else {
            Log.e(Accessor.class.getSimpleName(), "not found SurfaceView");
        }

        return ret;
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

    public interface OnDetectedItemListener {
        public void onDetected(SparseArray<Face> result);
    }

    private Thread mDetectThread;

    public void startDetectFace(final Activity activity, final FaceDetector.Builder builder, final int intervalMS, final OnDetectedItemListener listener) {
        if (mDetectThread == null) {
            mDetectThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    View decorView = activity.getWindow().getDecorView();
                    SurfaceView surfaceView = findSurfaceView(decorView);
                    FaceDetector.Builder faceDetectorBuilder = builder;

                    if (surfaceView != null) {
                        if (faceDetectorBuilder == null) {
                            faceDetectorBuilder = new FaceDetector.Builder(activity);
                        }

                        FaceDetector faceDetector = faceDetectorBuilder.build();

                        if (faceDetector != null) {
                            Rect frameRect = surfaceView.getHolder().getSurfaceFrame();
                            int[] buffer = new int[frameRect.width() * frameRect.height()];

                            while (mDetectThread != null) {
                                SparseArray<Face> ret = null;
                                IntBuffer pixelByteBuffer = IntBuffer.wrap(buffer);
                                GLES30.glReadBuffer(GLES20.GL_BACK);
                                GLES30.glReadPixels(0, 0, frameRect.width(), frameRect.height(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelByteBuffer);

                                int[] frameByteArray = pixelByteBuffer.array();
                                Bitmap frameBitmap = null;

                                try {
                                    frameBitmap = Bitmap.createBitmap(frameByteArray, frameRect.width(), frameRect.height(), Bitmap.Config.ARGB_8888);
                                    Log.d("", "frameBitmap = " + frameBitmap);

                                    Frame.Builder frameBuilder = new Frame.Builder();
                                    frameBuilder.setBitmap(frameBitmap);
                                    Frame frame = frameBuilder.build();

                                    ret = faceDetector.detect(frame);
                                    listener.onDetected(ret);
                                } finally {
                                    if (frameBitmap != null) {
                                        frameBitmap.recycle();
                                        frameBitmap = null;
                                    }
                                }

                                synchronized (Accessor.this) {
                                    try {
                                        Accessor.this.wait(intervalMS);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    } else {
                        Log.e(Accessor.class.getSimpleName(), "not found SurfaceView");
                    }
                }
            });
            mDetectThread.start();
        }
    }

    public void stopDetectFace() {
        if (mDetectThread != null) {
            mDetectThread = null;
        }
    }
}
