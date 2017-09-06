package jp.eq_inc.testmobilevision.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.ExifInterface;
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
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.IOException;
import java.util.List;

import jp.eq_inc.testmobilevision.R;
import jp.eq_inc.testmobilevision.adapter.ImageAdapter;

public class FaceDetectFromPhotoFragment extends AbstractDetectFragment implements View.OnClickListener {
    private Bitmap mDetectedBitmap;
    private ImageView mDetectedIv;
    private TextView mDetectedInformationTv;
    private Long mCurrentSelectedId = null;

    public FaceDetectFromPhotoFragment() {
        // Required empty public constructor
    }

    public static FaceDetectFromPhotoFragment newInstance(Bundle param) {
        FaceDetectFromPhotoFragment fragment = new FaceDetectFromPhotoFragment();
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
        View view = inflater.inflate(R.layout.fragment_face_detect_from_photo, container, false);

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
        FaceDetectTask task = new FaceDetectTask();
        task.execute(mCurrentSelectedId = mItemAdapter.getItemId(currentPosition));
    }

    @Override
    public void changeCommonParams() {
        if (mCurrentSelectedId != null) {
            FaceDetectTask task = new FaceDetectTask();
            task.execute(mCurrentSelectedId);
        }
    }

    private class FaceDetectTask extends AsyncTask<Long, Void, Boolean> {
        private Bitmap mReservedDetectedBitmap;
        private StringBuilder mReservedDetectedInformation;
        private ProgressDialog mProgressDialog;
        private FaceDetector mFaceDetector;
        private Integer mSelectedRotation = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Activity activity = getActivity();
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setMessage(activity.getString(R.string.please_wait));
            mProgressDialog.show();

            FaceDetector.Builder builder = new FaceDetector.Builder(activity);

            // classification
            SwitchCompat tempSwitch = (SwitchCompat) activity.findViewById(R.id.scClassification);
            if (tempSwitch.isChecked()) {
                builder.setClassificationType(FaceDetector.ALL_CLASSIFICATIONS);
            } else {
                builder.setClassificationType(FaceDetector.NO_CLASSIFICATIONS);
            }

            // landmark
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scLandmark);
            if (tempSwitch.isChecked()) {
                builder.setLandmarkType(FaceDetector.ALL_LANDMARKS);
            } else {
                builder.setLandmarkType(FaceDetector.NO_LANDMARKS);
            }

            // mode
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scDetectMode);
            if (tempSwitch.isChecked()) {
                builder.setMode(FaceDetector.FAST_MODE);
            } else {
                builder.setMode(FaceDetector.ACCURATE_MODE);
            }

            // prominent face only
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scProminentFaceOnly);
            builder.setProminentFaceOnly(tempSwitch.isChecked());

            // face tracking
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scFaceTracking);
            builder.setTrackingEnabled(tempSwitch.isChecked());

            // rotation
            Spinner rotationSpinner = (Spinner) activity.findViewById(R.id.spnrRotation);
            mSelectedRotation = (Integer) rotationSpinner.getSelectedItem();

            mFaceDetector = builder.build();
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
                        SparseArray<Face> detectedFaceArray = mFaceDetector.detect(fullImageFrame);
                        if (detectedFaceArray != null) {
                            StringBuilder builder = mReservedDetectedInformation = new StringBuilder();
                            Paint linePaint = new Paint();
                            Paint facePartPaint = new Paint();

                            linePaint.setARGB(100, 255, 0, 0);
                            linePaint.setStrokeWidth(activity.getResources().getDimensionPixelSize(R.dimen.face_line_width));
                            facePartPaint.setARGB(100, 0, 255, 0);
                            facePartPaint.setStrokeWidth(activity.getResources().getDimensionPixelSize(R.dimen.landmark_point_size));

                            for (int i = 0, size = detectedFaceArray.size(); i < size; i++) {
                                Face detectedFace = detectedFaceArray.valueAt(i);
                                PointF facePosition = detectedFace.getPosition();

                                // 顔を囲う線を描画
                                drawQuadLine(fullImageCanvas, "ID: " + String.valueOf(detectedFace.getId()), facePosition.x, facePosition.y, detectedFace.getWidth(), detectedFace.getHeight(), linePaint, rotation);

                                builder.append("Face ID: ").append(detectedFace.getId()).append("\n");

                                List<Landmark> faceLandmarkList = detectedFace.getLandmarks();
                                if (faceLandmarkList != null && faceLandmarkList.size() > 0) {
                                    for (Landmark faceLandmark : faceLandmarkList) {
                                        builder.append(" ").append(getLandmarkTypeString(faceLandmark.getType())).append(": ");

                                        PointF landmarkPosition = faceLandmark.getPosition();
                                        builder.append(String.format("%.3f", landmarkPosition.x)).append(",").append(String.format("%.3f", landmarkPosition.y)).append("\n");

                                        fullImageCanvas.drawPoint(landmarkPosition.x, landmarkPosition.y, facePartPaint);
                                    }
                                }

                                if (detectedFace.getIsLeftEyeOpenProbability() != Face.UNCOMPUTED_PROBABILITY) {
                                    builder.append(" left eye is opend: ").append(String.format("%.3f", detectedFace.getIsLeftEyeOpenProbability())).append("\n");
                                }
                                if (detectedFace.getIsRightEyeOpenProbability() != Face.UNCOMPUTED_PROBABILITY) {
                                    builder.append(" right eye is opend: ").append(String.format("%.3f", detectedFace.getIsRightEyeOpenProbability())).append("\n");
                                }
                                if (detectedFace.getIsSmilingProbability() != Face.UNCOMPUTED_PROBABILITY) {
                                    builder.append(" smiling: ").append(String.format("%.3f", detectedFace.getIsSmilingProbability())).append("\n");
                                }

                                builder.append(" face angleYZ: ").append(String.format("%.3f", detectedFace.getEulerY())).append(",").append(String.format("%.3f", detectedFace.getEulerZ())).append("\n");
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

            if (mFaceDetector != null) {
                mFaceDetector.release();
                mFaceDetector = null;
            }
        }
    }
}
