package jp.eq_inc.testmobilevision.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.util.List;

import jp.eq_inc.testmobilevision.R;
import jp.eq_inc.testmobilevision.adapter.ImageAdapter;

public class TextRecognizeFromPhotoFragment extends AbstractDetectFragment implements View.OnClickListener {
    private Bitmap mDetectedBitmap;
    private ImageView mDetectedIv;
    private TextView mDetectedInformationTv;
    private Long mCurrentSelectedId = null;

    public TextRecognizeFromPhotoFragment() {
        // Required empty public constructor
    }

    public static TextRecognizeFromPhotoFragment newInstance(Bundle param) {
        TextRecognizeFromPhotoFragment fragment = new TextRecognizeFromPhotoFragment();
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
        View view = inflater.inflate(R.layout.fragment_text_recognize_from_photo, container, false);

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
        TextRecognizeTask task = new TextRecognizeTask();
        task.execute(mCurrentSelectedId = mItemAdapter.getItemId(currentPosition));
    }

    @Override
    public void changeCommonParams() {
        if (mCurrentSelectedId != null) {
            TextRecognizeTask task = new TextRecognizeTask();
            task.execute(mCurrentSelectedId);
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
                                Rect textBlockBounds = detectedTextBlock.getBoundingBox();

                                // テキストを囲う線を描画
                                drawQuadLine(fullImageCanvas, null, textBlockBounds.left, textBlockBounds.top, textBlockBounds.width(), textBlockBounds.height(), linePaint, rotation);

                                builder.append("Value: ").append(detectedTextBlock.getValue()).append("\n");

                                // テキストをさらに展開
                                expandTextComponent(fullImageCanvas, rotation, builder, 1, detectedTextBlock.getComponents());
                            }
                        }
                    }
                }finally {
                    cursor.close();
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

                    // テキストを囲う線を描画
                    // 親要素を囲う線と重複させないために、描画開始位置を線幅分、中にずらす。そして幅・高さはずらした分を両端から引く必要がある
                    drawQuadLine(fullImageCanvas, null, bounds.left + linePaint.getStrokeWidth() * indent, bounds.top + linePaint.getStrokeWidth() * indent, bounds.width() - linePaint.getStrokeWidth() * indent * 2, bounds.height() - linePaint.getStrokeWidth() * indent * 2, linePaint, rotation);

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
