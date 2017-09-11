package jp.eq_inc.testmobilevision.detector;

import android.content.Context;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

public class AllDetector extends Detector {
    public enum DetectorType {
        Face, Barcode, Text;

        public static DetectorType getDetectorTypeFromItem(Object item) {
            if (item instanceof com.google.android.gms.vision.face.Face) {
                return DetectorType.Face;
            } else if (item instanceof com.google.android.gms.vision.barcode.Barcode) {
                return DetectorType.Barcode;
            } else if (item instanceof TextBlock) {
                return DetectorType.Text;
            }

            return null;
        }
    }

    private Detector[] mDetectorArray;
    private Detector.Detections[] mDetectionsArray;

    private AllDetector(FaceDetector faceDetector, BarcodeDetector barcodeDetector, TextRecognizer textRecognizer) {
        super();

        mDetectorArray = new Detector[DetectorType.values().length];
        mDetectionsArray = new Detector.Detections[DetectorType.values().length];

        mDetectorArray[DetectorType.Face.ordinal()] = faceDetector;
        mDetectorArray[DetectorType.Barcode.ordinal()] = barcodeDetector;
        mDetectorArray[DetectorType.Text.ordinal()] = textRecognizer;
    }

    @Override
    public SparseArray detect(Frame frame) {
        SparseArray[] detectedItemsArray = new SparseArray[DetectorType.values().length];

        // 全てのDetector/Recognizerにて解析を実施
        for(int i = 0, size = DetectorType.values().length; i<size; i++){
            detectedItemsArray[i] = mDetectorArray[i].detect(frame);
        }

        // 解析結果を1つのSparceArrayに纏めて返却
        SparseArray ret = new SparseArray();
        for(SparseArray detectedItems : detectedItemsArray){
            for(int i=0, size=detectedItems.size(); i<size; i++){
                ret.append(detectedItems.keyAt(i), detectedItems.valueAt(i));
            }
        }

        return ret;
    }

    @Override
    public void release() {
        super.release();
        for (DetectorType detectorType : DetectorType.values()) {
            mDetectorArray[detectorType.ordinal()].release();
        }
    }

    @Override
    public boolean setFocus(int i) {
        boolean ret = false;

        for (DetectorType detectorType : DetectorType.values()) {
            Detections detections = mDetectionsArray[detectorType.ordinal()];

            if (detections != null) {
                Object detectedItem = detections.getDetectedItems().get(i, null);
                if (detectedItem != null) {
                    ret = mDetectorArray[detectorType.ordinal()].setFocus(i);
                    break;
                }
            }
        }

        return ret;
    }

    @Override
    public void setProcessor(Processor processor) {
        // Processorを指定していないと、Detector.receiveFrameにてIllegalStateExceptionが発生するので、必ず設定
        super.setProcessor(processor);
        for (DetectorType detectorType : DetectorType.values()) {
            mDetectorArray[detectorType.ordinal()].setProcessor(processor);
        }
    }

    public static class Builder {
        private Context mContext;
        private Integer mClassificationType = null;
        private Integer mLandmarkType = null;
        private Float mMinFaceSize = null;
        private Integer mMode = null;
        private Boolean mProminentFaceOnly = null;
        private Boolean mTrackingEnabled = null;
        private Integer mBarcodeFormats = null;

        public Builder(Context context) {
            if (context == null) {
                throw new NullPointerException("context is null");
            }

            mContext = context;
        }

        public Builder setClassificationType(int classificationType) {
            mClassificationType = classificationType;
            return this;
        }

        public Builder setLandmarkType(int landmarkType) {
            mLandmarkType = landmarkType;
            return this;
        }

        public Builder setMinFaceSize(float proportionalMinFaceSize) {
            mMinFaceSize = proportionalMinFaceSize;
            return this;
        }

        public Builder setMode(int mode) {
            mMode = mode;
            return this;
        }

        public Builder setProminentFaceOnly(boolean prominentFaceOnly) {
            mProminentFaceOnly = prominentFaceOnly;
            return this;
        }

        public Builder setTrackingEnabled(boolean trackingEnabled) {
            mTrackingEnabled = trackingEnabled;
            return this;
        }

        public Builder setBarcodeFormats(int format) {
            mBarcodeFormats = format;
            return this;
        }

        public AllDetector build() {
            FaceDetector.Builder faceDetectorBuilder = new FaceDetector.Builder(mContext);
            if (mClassificationType != null) {
                faceDetectorBuilder.setClassificationType(mClassificationType);
            }
            if (mLandmarkType != null) {
                faceDetectorBuilder.setLandmarkType(mLandmarkType);
            }
            if (mMinFaceSize != null) {
                faceDetectorBuilder.setMinFaceSize(mMinFaceSize);
            }
            if (mMode != null) {
                faceDetectorBuilder.setMode(mMode);
            }
            if (mProminentFaceOnly != null) {
                faceDetectorBuilder.setProminentFaceOnly(mProminentFaceOnly);
            }
            if (mTrackingEnabled != null) {
                faceDetectorBuilder.setTrackingEnabled(mTrackingEnabled);
            }

            BarcodeDetector.Builder barcodeDetectorBuilder = new BarcodeDetector.Builder(mContext);
            if (mBarcodeFormats != null) {
                barcodeDetectorBuilder.setBarcodeFormats(mBarcodeFormats);
            }

            return new AllDetector(faceDetectorBuilder.build(), barcodeDetectorBuilder.build(), new TextRecognizer.Builder(mContext).build());
        }
    }
}
