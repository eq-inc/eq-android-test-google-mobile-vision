package jp.eq_inc.testmobilevision.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

import jp.eq_inc.testmobilevision.R;
import jp.eq_inc.testmobilevision.adapter.ImageAdapter;

public class BarcodeDetectFromPhotoFragment extends AbstractDetectFragment implements View.OnClickListener {
    private Bitmap mDetectedBitmap;
    private ImageView mDetectedIv;
    private TextView mDetectedInformationTv;
    private Long mCurrentSelectedId = null;

    public BarcodeDetectFromPhotoFragment() {
        // Required empty public constructor
    }

    public static BarcodeDetectFromPhotoFragment newInstance(Bundle param) {
        BarcodeDetectFromPhotoFragment fragment = new BarcodeDetectFromPhotoFragment();
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
        Activity activity = getActivity();
        View view = inflater.inflate(R.layout.fragment_barcode_detect_from_photo, container, false);

        mDetectedIv = (ImageView) view.findViewById(R.id.ivDetectedImage);
        mDetectedInformationTv = (TextView) view.findViewById(R.id.tvFaceInformation);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rcPhotoList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, true);
        recyclerView.setLayoutManager(layoutManager);
        mItemAdapter = new ImageAdapter(activity);
        mItemAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(mItemAdapter);

        return view;
    }

    @Override
    public void onClick(View v) {
        ImageAdapter.InnerViewHolder holder = (ImageAdapter.InnerViewHolder) v.getTag(ImageAdapter.ViewTagHolder);
        int currentPosition = getAdapterPosition(holder);
        BarcodeDetectTask task = new BarcodeDetectTask();
        task.execute(mCurrentSelectedId = mItemAdapter.getItemId(currentPosition));
    }

    @Override
    public void changeCommonParams() {
        if (mCurrentSelectedId != null) {
            BarcodeDetectTask task = new BarcodeDetectTask();
            task.execute(mCurrentSelectedId);
        }
    }

    private class BarcodeDetectTask extends AsyncTask<Long, Void, Boolean> {
        private Bitmap mReservedDetectedBitmap;
        private StringBuilder mReservedDetectedInformation;
        private ProgressDialog mProgressDialog;
        private BarcodeDetector mBarcodeDetector;
        private Integer mSelectedRotation = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Activity activity = getActivity();
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setMessage(activity.getString(R.string.please_wait));
            mProgressDialog.show();

            BarcodeDetector.Builder builder = new BarcodeDetector.Builder(activity);

            // barcode format
            Integer selectedFormat = (Integer) ((Spinner) activity.findViewById(R.id.spnrBarcodeFormat)).getSelectedItem();
            if (selectedFormat == null) {
                selectedFormat = Barcode.ALL_FORMATS;
            }
            builder.setBarcodeFormats(selectedFormat);

            // rotation
            Spinner rotationSpinner = (Spinner) activity.findViewById(R.id.spnrRotation);
            mSelectedRotation = (Integer) rotationSpinner.getSelectedItem();

            mBarcodeDetector = builder.build();
        }

        @Override
        protected Boolean doInBackground(Long... params) {
            boolean ret = false;
            Activity activity = getActivity();
            Cursor cursor = MediaStore.Images.Media.query(activity.getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, MediaStore.MediaColumns._ID + "=?", new String[]{params[0].toString()}, null);

            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();

                        String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inMutable = true;
                        Bitmap fullImage = mReservedDetectedBitmap = BitmapFactory.decodeFile(imagePath, options);
                        Canvas fullImageCanvas = new Canvas(fullImage);
                        int rotation = Frame.ROTATION_0;

                        if (mSelectedRotation == null) {
                            try {
                                ExifInterface exifInterface = new ExifInterface(imagePath);
                                int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                                switch (orientation) {
                                    case ExifInterface.ORIENTATION_ROTATE_90:
                                        rotation = Frame.ROTATION_90;
                                        break;
                                    case ExifInterface.ORIENTATION_ROTATE_180:
                                        rotation = Frame.ROTATION_180;
                                        break;
                                    case ExifInterface.ORIENTATION_ROTATE_270:
                                        rotation = Frame.ROTATION_270;
                                        break;
                                }
                            } catch (IOException e) {
                            }
                        } else {
                            rotation = mSelectedRotation;
                        }

                        // canvasを画像の回転状態に合わせて回転・移動させる
                        changeCanvasPosition(fullImageCanvas, rotation);

                        Frame fullImageFrame = new Frame.Builder().setBitmap(fullImage).setRotation(rotation).build();
                        SparseArray<Barcode> detectedBarcodeArray = mBarcodeDetector.detect(fullImageFrame);
                        if (detectedBarcodeArray != null) {
                            StringBuilder builder = mReservedDetectedInformation = new StringBuilder();
                            Paint linePaint = new Paint();

                            linePaint.setARGB(100, 255, 0, 0);
                            linePaint.setStrokeWidth(activity.getResources().getDimensionPixelSize(R.dimen.face_line_width));

                            for (int i = 0, size = detectedBarcodeArray.size(); i < size; i++) {
                                Barcode detectedBarcode = detectedBarcodeArray.valueAt(i);
                                Rect barcodeBounds = detectedBarcode.getBoundingBox();

                                // バーコードを囲う線を描画
                                drawQuadLine(fullImageCanvas, detectedBarcode.displayValue, barcodeBounds.left, barcodeBounds.top, barcodeBounds.width(), barcodeBounds.height(), linePaint, rotation);

                                builder.append("Value: ").append(detectedBarcode.displayValue).append("\n");
                            }
                        }
                    }
                    ret = true;
                } finally {
                    cursor.close();
                }
            }

            return ret;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            mProgressDialog.hide();
            mProgressDialog.dismiss();

            if (aBoolean) {
                if (mDetectedBitmap != null) {
                    mDetectedIv.setImageBitmap(null);
                    mDetectedBitmap.recycle();
                    mDetectedBitmap = null;
                }

                mDetectedBitmap = mReservedDetectedBitmap;
                mDetectedIv.setImageBitmap(mDetectedBitmap);

                mDetectedInformationTv.setText(mReservedDetectedInformation.toString());
            }

            if (mBarcodeDetector != null) {
                mBarcodeDetector.release();
                mBarcodeDetector = null;
            }
        }
    }
}
