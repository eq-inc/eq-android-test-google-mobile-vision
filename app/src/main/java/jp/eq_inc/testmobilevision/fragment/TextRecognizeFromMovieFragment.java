package jp.eq_inc.testmobilevision.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
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
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jp.eq_inc.testmobilevision.R;
import jp.eq_inc.testmobilevision.adapter.AbstractFaceDetectCursorAdapter;
import jp.eq_inc.testmobilevision.adapter.MovieAdapter;
import jp.eq_inc.testmobilevision.adapter.MovieFrameAdapter;

public class TextRecognizeFromMovieFragment extends AbstractDetectFragment {
    private Bitmap mDetectedBitmap;
    private ImageView mDetectedIv;
    private TextView mDetectedInformationTv;
    private MovieFrameAdapter mMovieFrameAdapter;
    private Long mCurrentSelectedId = null;
    private Integer mCurrentSelectedItemPosition = null;

    public TextRecognizeFromMovieFragment() {
        // Required empty public constructor
    }

    public static TextRecognizeFromMovieFragment newInstance(Bundle param) {
        TextRecognizeFromMovieFragment fragment = new TextRecognizeFromMovieFragment();
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
        View view = inflater.inflate(R.layout.fragment_text_recognize_from_movie, container, false);

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
        if (mCurrentSelectedId != null) {
            if (mCurrentSelectedItemPosition != null && mCurrentSelectedItemPosition >= 0) {
                TextRecognizeTask task = new TextRecognizeTask();
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
            TextRecognizeTask task = new TextRecognizeTask();
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

    private class TextRecognizeTask extends AsyncTask<Long, Void, Boolean> {
        private Bitmap mReservedDetectedBitmap;
        private StringBuilder mReservedDetectedInformation;
        private ProgressDialog mProgressDialog;
        private TextRecognizer mTextRecognizer;
        private Integer mSelectedRotation = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Activity activity = getActivity();
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setMessage(activity.getString(R.string.please_wait));
            mProgressDialog.show();

            mTextRecognizer = new TextRecognizer.Builder(activity).build();
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
                SparseArray<TextBlock> detectedFaceArray = mTextRecognizer.detect(fullImageFrame);
                if (detectedFaceArray != null) {
                    StringBuilder builder = mReservedDetectedInformation = new StringBuilder();
                    Paint linePaint = new Paint();
                    Paint facePartPaint = new Paint();

                    linePaint.setARGB(100, 255, 0, 0);
                    linePaint.setStrokeWidth(activity.getResources().getDimensionPixelSize(R.dimen.face_line_width));
                    facePartPaint.setARGB(100, 0, 255, 0);
                    facePartPaint.setStrokeWidth(activity.getResources().getDimensionPixelSize(R.dimen.landmark_point_size));

                    for (int i = 0, size = detectedFaceArray.size(); i < size; i++) {
                        TextBlock detectedTextBlock = detectedFaceArray.valueAt(i);
                        List<? extends Text> childTextComponentList = detectedTextBlock.getComponents();
                        boolean haveChildTextComponent = ((childTextComponentList != null) && (childTextComponentList.size() > 0));
                        String description = null;
                        Rect textBlockBounds = detectedTextBlock.getBoundingBox();

                        if (!haveChildTextComponent) {
                            description = detectedTextBlock.getValue();
                        }

                        // テキストを囲う線を描画
                        drawQuadLine(fullImageCanvas, description, textBlockBounds.left, textBlockBounds.top, textBlockBounds.width(), textBlockBounds.height(), linePaint, rotation);

                        builder.append("Value: ").append(detectedTextBlock.getValue()).append("\n");
                        builder.append(" Language: ").append(detectedTextBlock.getLanguage()).append("\n");

                        // テキストをさらに展開
                        expandTextComponent(fullImageCanvas, rotation, builder, 1, detectedTextBlock.getComponents());
                    }
                }
                ret = true;
            }

            return ret;
        }

        private void expandTextComponent(Canvas fullImageCanvas, int rotation, StringBuilder builder, int indent, List<? extends Text> textComponentList) {
            if (textComponentList != null && textComponentList.size() > 0) {
                Activity activity = getActivity();
                Paint linePaint = new Paint();
                linePaint.setStrokeWidth(activity.getResources().getDimensionPixelSize(R.dimen.face_line_width) / 2);
                int indentModThree = indent % 3;

                if (indentModThree == 0) {
                    linePaint.setARGB(100, 255 - indent, 0, 0);
                } else if (indentModThree == 1) {
                    linePaint.setARGB(100, 0, 255 - indent, 0);
                } else {
                    linePaint.setARGB(100, 0, 0, 255 - indent);
                }

                for (Text textComponent : textComponentList) {
                    List<? extends Text> childTextComponentList = textComponent.getComponents();
                    boolean haveChildTextComponent = ((childTextComponentList != null) && (childTextComponentList.size() > 0));

                    Rect bounds = textComponent.getBoundingBox();
                    String description = null;

                    if (!haveChildTextComponent) {
                        description = textComponent.getValue();
                    }

                    // テキストを囲う線を描画
                    drawQuadLine(fullImageCanvas, description, bounds.left, bounds.top, bounds.width(), bounds.height(), linePaint, rotation);

                    if (haveChildTextComponent) {
                        expandTextComponent(fullImageCanvas, rotation, builder, indent + 1, childTextComponentList);
                    }
                }
            }
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

            if (mTextRecognizer != null) {
                mTextRecognizer.release();
                mTextRecognizer = null;
            }
        }
    }
}
