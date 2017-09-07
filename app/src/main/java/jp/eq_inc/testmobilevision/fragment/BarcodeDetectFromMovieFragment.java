package jp.eq_inc.testmobilevision.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
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
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jp.eq_inc.testmobilevision.R;
import jp.eq_inc.testmobilevision.adapter.AbstractFaceDetectCursorAdapter;
import jp.eq_inc.testmobilevision.adapter.MovieAdapter;
import jp.eq_inc.testmobilevision.adapter.MovieFrameAdapter;

public class BarcodeDetectFromMovieFragment extends AbstractDetectFragment {
    private Bitmap mDetectedBitmap;
    private ImageView mDetectedIv;
    private TextView mDetectedInformationTv;
    private MovieFrameAdapter mMovieFrameAdapter;
    private Long mCurrentSelectedId = null;
    private Integer mCurrentSelectedItemPosition = null;

    public BarcodeDetectFromMovieFragment() {
        // Required empty public constructor
    }

    public static BarcodeDetectFromMovieFragment newInstance(Bundle param) {
        BarcodeDetectFromMovieFragment fragment = new BarcodeDetectFromMovieFragment();
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
        View view = inflater.inflate(R.layout.fragment_barcode_detect_from_movie, container, false);

        mDetectedIv = (ImageView) view.findViewById(R.id.ivDetectedImage);
        mDetectedInformationTv = (TextView) view.findViewById(R.id.tvFaceInformation);

        RecyclerView photoListRecyclerView = (RecyclerView) view.findViewById(R.id.rcPhotoList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, true);
        photoListRecyclerView.setLayoutManager(layoutManager);
        mItemAdapter = new MovieAdapter(activity);
        mItemAdapter.setOnItemClickListener(mMovieListClickListener);
        photoListRecyclerView.setAdapter(mItemAdapter);

        RecyclerView frameListRecyclerView = (RecyclerView) view.findViewById(R.id.rcFrameList);
        mMovieFrameAdapter = new MovieFrameAdapter(activity);
        mMovieFrameAdapter.setOnItemClickListener(mFrameListClickListener);
        layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false);
        frameListRecyclerView.setLayoutManager(layoutManager);
        frameListRecyclerView.setAdapter(mMovieFrameAdapter);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mMovieFrameAdapter != null) {
            mMovieFrameAdapter.release();
            mMovieFrameAdapter = null;
        }
    }

    @Override
    public String[] getRuntimePermissions() {
        ArrayList<String> runtimePermissionList = new ArrayList<String>();
        String[] superRuntimePermissionArray = super.getRuntimePermissions();

        runtimePermissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        for (String superRuntimePermission : superRuntimePermissionArray) {
            runtimePermissionList.add(superRuntimePermission);
        }

        return runtimePermissionList.toArray(new String[0]);
    }

    @Override
    public void changeCommonParams() {
        if(mCurrentSelectedId != null){
            if(mCurrentSelectedItemPosition != null && mCurrentSelectedItemPosition >= 0){
                BarcodeDetectTask task = new BarcodeDetectTask();
                task.execute((long) mCurrentSelectedItemPosition);
            }
        }
    }

    private View.OnClickListener mMovieListClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AbstractFaceDetectCursorAdapter.InnerViewHolder holder = (AbstractFaceDetectCursorAdapter.InnerViewHolder) v.getTag(AbstractFaceDetectCursorAdapter.ViewTagHolder);
            int currentPosition = getAdapterPosition(holder);
            MovieSelectTask task = new MovieSelectTask();
            task.execute(mCurrentSelectedId = mItemAdapter.getItemId(currentPosition));
            mCurrentSelectedItemPosition = null;
        }
    };

    private View.OnClickListener mFrameListClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AbstractFaceDetectCursorAdapter.InnerViewHolder holder = (AbstractFaceDetectCursorAdapter.InnerViewHolder) v.getTag(AbstractFaceDetectCursorAdapter.ViewTagHolder);
            int currentPosition = mCurrentSelectedItemPosition = getAdapterPosition(holder);
            BarcodeDetectTask task = new BarcodeDetectTask();
            task.execute((long) currentPosition);
        }
    };

    private class MovieSelectTask extends AsyncTask<Long, Bitmap, String> {
        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Activity activity = getActivity();
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setMessage(activity.getString(R.string.please_wait));
            mProgressDialog.show();
        }

        @Override
        protected String doInBackground(Long... params) {
            Activity activity = getActivity();
            String contentId = params[0].toString();
            String filePath = null;

            Cursor cursor = activity.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, MediaStore.MediaColumns._ID + "=?", new String[]{contentId}, null);

            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();

                        String moviePath = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                        File file = new File(moviePath);
                        if (file.exists()) {
                            filePath = moviePath;
                        }
                    }
                } finally {
                    cursor.close();
                }
            }

            return filePath;
        }

        @Override
        protected void onPostExecute(String moviePath) {
            super.onPostExecute(moviePath);

            mProgressDialog.hide();
            mProgressDialog.dismiss();

            if (moviePath != null) {
                mMovieFrameAdapter.setMoviePath(moviePath);
                mMovieFrameAdapter.updateAsync();
            }
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
            Integer selectedFormat = (Integer) ((Spinner)activity.findViewById(R.id.spnrBarcodeFormat)).getSelectedItem();
            if(selectedFormat == null){
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
            Bitmap fullImage = mReservedDetectedBitmap = mMovieFrameAdapter.getFrameBitmap(params[0]);

            if (fullImage != null) {
                Canvas fullImageCanvas = new Canvas(fullImage);
                int rotation = Frame.ROTATION_0;
                if (mSelectedRotation != null) {
                    rotation = mSelectedRotation;

                    // canvasを画像の回転状態に合わせて回転・移動させる
                    changeCanvasPosition(fullImageCanvas, mSelectedRotation);
                }
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
                        drawQuadLine(fullImageCanvas, "Value: " + detectedBarcode.displayValue, barcodeBounds.left, barcodeBounds.top, barcodeBounds.width(), barcodeBounds.height(), linePaint, rotation);

                        builder.append("Value: ").append(detectedBarcode.displayValue).append("\n");
                    }
                }
                ret = true;
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
