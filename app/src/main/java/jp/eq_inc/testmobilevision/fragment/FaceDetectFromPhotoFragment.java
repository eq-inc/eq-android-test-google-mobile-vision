package jp.eq_inc.testmobilevision.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.IOException;
import java.util.List;

import jp.eq_inc.testmobilevision.R;
import jp.eq_inc.testmobilevision.adapter.ImageAdapter;

public class FaceDetectFromPhotoFragment extends Fragment implements View.OnClickListener {
    private OnFragmentInteractionListener mListener;
    private ImageAdapter mImageAdapter;
    private Bitmap mDetectedBitmap;
    private ImageView mDetectedIv;
    private TextView mDetectedInformationTv;

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
        mImageAdapter = new ImageAdapter(activity);
        mImageAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(mImageAdapter);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mImageAdapter != null) {
            mImageAdapter.updateAsync();
        }
    }

    @Override
    public void onClick(View v) {
        ImageAdapter.InnerViewHolder holder = (ImageAdapter.InnerViewHolder) v.getTag(ImageAdapter.ViewTagHolder);
        int currentPosition = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ? holder.getAdapterPosition() : holder.getPosition();
        FaceDetectTask task = new FaceDetectTask();
        task.execute(mImageAdapter.getItemId(currentPosition));
    }

    private class FaceDetectTask extends AsyncTask<Long, Void, Boolean> {
        private Bitmap mReservedDetectedBitmap;
        private StringBuilder mReservedDetectedInformation;
        private ProgressDialog mProgressDialog;
        private FaceDetector mFaceDetector;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Activity activity = getActivity();
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setMessage(activity.getString(R.string.please_wait));
            mProgressDialog.show();

            FaceDetector.Builder builder = new FaceDetector.Builder(activity);
            SwitchCompat tempSwitch = (SwitchCompat) activity.findViewById(R.id.scClassification);
            if (tempSwitch.isChecked()) {
                builder.setClassificationType(FaceDetector.ALL_CLASSIFICATIONS);
            } else {
                builder.setClassificationType(FaceDetector.NO_CLASSIFICATIONS);
            }
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scLandmark);
            if (tempSwitch.isChecked()) {
                builder.setLandmarkType(FaceDetector.ALL_LANDMARKS);
            } else {
                builder.setLandmarkType(FaceDetector.NO_LANDMARKS);
            }
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scDetectMode);
            if (tempSwitch.isChecked()) {
                builder.setMode(FaceDetector.FAST_MODE);
            } else {
                builder.setMode(FaceDetector.ACCURATE_MODE);
            }
            tempSwitch = (SwitchCompat) activity.findViewById(R.id.scProminentFaceOnly);
            builder.setProminentFaceOnly(tempSwitch.isChecked());

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

                        Frame fullImageFrame = new Frame.Builder().setBitmap(fullImage).setRotation(rotation).build();
                        SparseArray<Face> detectedFaceArray = mFaceDetector.detect(fullImageFrame);
                        if (detectedFaceArray != null) {
                            StringBuilder builder = mReservedDetectedInformation = new StringBuilder();

                            for (int i = 0, size = detectedFaceArray.size(); i < size; i++) {
                                Face detectedFace = detectedFaceArray.valueAt(i);

                                Paint linePaint = new Paint();
                                linePaint.setARGB(100, 255, 0, 0);
                                linePaint.setStrokeWidth(activity.getResources().getDimensionPixelSize(R.dimen.face_line_width));
                                Paint facePartPaint = new Paint();
                                facePartPaint.setARGB(100, 0, 255, 0);
                                facePartPaint.setStrokeWidth(activity.getResources().getDimensionPixelSize(R.dimen.landmark_point_size));

                                PointF faceLeftTopPointF = detectedFace.getPosition();
                                if(faceLeftTopPointF.x < 0){
                                    faceLeftTopPointF.x = 0;
                                }
                                if(faceLeftTopPointF.y < 0){
                                    faceLeftTopPointF.y = 0;
                                }
                                PointF faceRightBottomPointF = new PointF(faceLeftTopPointF.x + detectedFace.getWidth(), faceLeftTopPointF.y + detectedFace.getHeight());
                                if(faceRightBottomPointF.x > fullImageCanvas.getWidth()){
                                    faceRightBottomPointF.x = fullImageCanvas.getWidth();
                                }
                                if(faceRightBottomPointF.y > fullImageCanvas.getHeight()){
                                    faceRightBottomPointF.y = fullImageCanvas.getHeight();
                                }
                                float[] facePoints = new float[]{
                                        faceLeftTopPointF.x, faceLeftTopPointF.y, faceRightBottomPointF.x, faceLeftTopPointF.y,
                                        faceRightBottomPointF.x, faceLeftTopPointF.y, faceRightBottomPointF.x, faceRightBottomPointF.y,
                                        faceRightBottomPointF.x, faceRightBottomPointF.y, faceLeftTopPointF.x, faceRightBottomPointF.y,
                                        faceLeftTopPointF.x, faceRightBottomPointF.y, faceLeftTopPointF.x, faceLeftTopPointF.y,
                                };
                                fullImageCanvas.drawLines(facePoints, linePaint);

                                builder.append("Face ID: ").append(detectedFace.getId()).append("\n");

                                List<Landmark> faceLandmarkList = detectedFace.getLandmarks();
                                if(faceLandmarkList != null && faceLandmarkList.size() > 0){
                                    for(Landmark faceLandmark : faceLandmarkList){
                                        builder.append(" ");

                                        switch(faceLandmark.getType()){
                                            case Landmark.BOTTOM_MOUTH:
                                                builder.append("bottom mouth: ");
                                                break;
                                            case Landmark.LEFT_CHEEK:
                                                builder.append("left cheek: ");
                                                break;
                                            case Landmark.LEFT_EAR:
                                                builder.append("left ear: ");
                                                break;
                                            case Landmark.LEFT_EAR_TIP:
                                                builder.append("left ear tip: ");
                                                break;
                                            case Landmark.LEFT_EYE:
                                                builder.append("left eye: ");
                                                break;
                                            case Landmark.LEFT_MOUTH:
                                                builder.append("left mouth: ");
                                                break;
                                            case Landmark.NOSE_BASE:
                                                builder.append("nose base: ");
                                                break;
                                            case Landmark.RIGHT_CHEEK:
                                                builder.append("right cheek: ");
                                                break;
                                            case Landmark.RIGHT_EAR:
                                                builder.append("right ear: ");
                                                break;
                                            case Landmark.RIGHT_EAR_TIP:
                                                builder.append("right ear tip: ");
                                                break;
                                            case Landmark.RIGHT_EYE:
                                                builder.append("right eye: ");
                                                break;
                                            case Landmark.RIGHT_MOUTH:
                                                builder.append("right mouth: ");
                                                break;
                                        }

                                        PointF landmarkPosition = faceLandmark.getPosition();
                                        builder.append(landmarkPosition).append("\n");

                                        fullImageCanvas.drawPoint(landmarkPosition.x, landmarkPosition.y, facePartPaint);
                                    }
                                }
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
