package jp.eq_inc.testmobilevision.processor;

import android.graphics.Rect;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.FocusingProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.google.android.gms.vision.text.TextBlock;

import jp.eq_inc.testmobilevision.detector.AllDetector;

public class EachFocusingProcessor implements Detector.Processor {
    private FocusingProcessor[] mFocusingProcessorArray;

    public EachFocusingProcessor(Detector detector, Tracker tracker) {
        mFocusingProcessorArray = new FocusingProcessor[]{
                new LargestFaceFocusingProcessor(detector, tracker),
                new BarcodeFocusingProcessor(detector, tracker),
                new TextFocusingProcessor(detector, tracker),
        };
    }

    @Override
    public void release() {
        for (int i = 0, size = mFocusingProcessorArray.length; i < size; i++) {
            mFocusingProcessorArray[i].release();
        }
    }

    @Override
    public void receiveDetections(Detector.Detections detections) {
        SparseArray detectedItems = detections.getDetectedItems();
        if (detectedItems.size() > 0) {
            Object detectedItem = detectedItems.valueAt(0);
            AllDetector.DetectorType detectorType = AllDetector.DetectorType.getDetectorTypeFromItem(detectedItem);

            if (detectorType != null) {
                mFocusingProcessorArray[detectorType.ordinal()].receiveDetections(detections);
            }
        }
    }

    public static class BarcodeFocusingProcessor extends FocusingProcessor<Barcode> {

        public BarcodeFocusingProcessor(Detector<Barcode> detector, Tracker<Barcode> tracker) {
            super(detector, tracker);
        }

        @Override
        public int selectFocus(Detector.Detections<Barcode> detections) {
            SparseArray<Barcode> detectedItems = detections.getDetectedItems();
            int selectedItem = 0;
            int largestSize = 0;
            for (int i = 0, size = detectedItems.size(); i < size; i++) {
                Barcode detectedItem = detectedItems.valueAt(i);
                Rect bounds = detectedItem.getBoundingBox();
                int tempBoundsSize = bounds.width() * bounds.height();
                if (largestSize < tempBoundsSize) {
                    largestSize = tempBoundsSize;
                    selectedItem = detectedItems.keyAt(i);
                }
            }

            return selectedItem;
        }
    }

    public static class TextFocusingProcessor extends FocusingProcessor<TextBlock> {

        public TextFocusingProcessor(Detector<TextBlock> detector, Tracker<TextBlock> tracker) {
            super(detector, tracker);
        }

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
    }
}
