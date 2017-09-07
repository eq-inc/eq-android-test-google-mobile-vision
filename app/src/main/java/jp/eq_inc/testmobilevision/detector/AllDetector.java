package jp.eq_inc.testmobilevision.detector;

import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.text.TextRecognizer;

public class AllDetector extends Detector {
    private FaceDetector mFaceDetector;
    private BarcodeDetector mBarcodeDetector;
    private TextRecognizer mTextRecognizer;

    private AllDetector() {
        super();
    }

    @Override
    public SparseArray detect(Frame frame) {
        return null;
    }

    public static class Builder{
        private Integer mClassificationType = null;
        private Integer mLandmarkType = null;
        private Float mMinFaceSize = null;
        private Integer mMode = null;
        private Boolean mProminentFaceOnly = null;
        private Boolean mTrackingEnabled = null;
        private Integer mBarcodeFormats = null;

        public Builder setClassificationType(int classificationType){
            mClassificationType = classificationType;
            return this;
        }

        public Builder setLandmarkType(int landmarkType){
            mLandmarkType = landmarkType;
            return this;
        }

        public Builder setMinFaceSize(float proportionalMinFaceSize){
            mMinFaceSize = proportionalMinFaceSize;
            return this;
        }

        public Builder setMode(int mode){
            mMode = mode;
            return this;
        }

        public Builder setProminentFaceOnly(boolean prominentFaceOnly){
            mProminentFaceOnly = prominentFaceOnly;
            return this;
        }

        public Builder setTrackingEnabled(boolean trackingEnabled){
            mTrackingEnabled = trackingEnabled;
            return this;
        }

        public Builder setBarcodeFormats (int format){
            mBarcodeFormats = format;
            return this;
        }

        public AllDetector build(){

        }
    }
}
