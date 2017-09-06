package jp.eq_inc.testmobilevision.fragment;

import android.Manifest;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Region;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import jp.eq_inc.testmobilevision.R;
import jp.eq_inc.testmobilevision.adapter.AbstractFaceDetectCursorAdapter;

abstract public class AbstractFaceDetectFragment extends Fragment {
    protected OnFragmentInteractionListener mListener;
    protected AbstractFaceDetectCursorAdapter mItemAdapter;

    abstract public void changeFaceDetectParams();

    public AbstractFaceDetectFragment() {
        // Required empty public constructor
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

        if (mItemAdapter != null) {
            mItemAdapter.updateAsync();
        }
    }

    public String[] getRuntimePermissions() {
        return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    protected void changeCanvasPosition(Canvas imageCanvas, int frameRotation){
        int imageWidth = imageCanvas.getWidth();
        int imageHeight = imageCanvas.getHeight();

        if(frameRotation != Frame.ROTATION_0){
            switch (frameRotation){
                case Frame.ROTATION_90:
                    imageCanvas.rotate(270, imageWidth / 2, imageHeight / 2);
                    imageCanvas.translate( - (imageHeight - imageWidth) / 2, (imageHeight - imageWidth) / 2);
                    break;
                case Frame.ROTATION_180:
                    imageCanvas.rotate(180, imageWidth / 2, imageHeight / 2);
                    break;
                case Frame.ROTATION_270:
                    imageCanvas.rotate(90, imageWidth / 2, imageHeight / 2);
                    imageCanvas.translate( - (imageHeight - imageWidth) / 2, (imageHeight - imageWidth) / 2);
                    break;
            }
        }
    }

    protected void drawFaceLine(Canvas imageCanvas, Face detectedFace, Paint linePaint, int frameRotation){
        PointF facePosition = detectedFace.getPosition();
        drawFaceLine(imageCanvas, detectedFace.getId(), facePosition.x, facePosition.y, detectedFace.getWidth(), detectedFace.getHeight(), linePaint, frameRotation);
    }

    protected void drawFaceLine(Canvas imageCanvas, int id, float posX, float posY, float width, float height, Paint linePaint, int frameRotation){
        int imageWidth = imageCanvas.getWidth();
        int imageHeight = imageCanvas.getHeight();
        float lineWidth = linePaint.getStrokeWidth();
        if (posX < 0) {
            posX = 0;
        }
        if (posY < 0) {
            posY = 0;
        }
        PointF faceRightBottomPointF = new PointF(posX + width, posY + height);
        if(frameRotation == Frame.ROTATION_0 || frameRotation == Frame.ROTATION_180){
            if (faceRightBottomPointF.x > imageWidth) {
                faceRightBottomPointF.x = imageWidth;
            }
            if (faceRightBottomPointF.y > imageHeight) {
                faceRightBottomPointF.y = imageHeight;
            }
        }else if(frameRotation == Frame.ROTATION_90 || frameRotation == Frame.ROTATION_270){
            if (faceRightBottomPointF.x > imageHeight) {
                faceRightBottomPointF.x = imageHeight;
            }
            if (faceRightBottomPointF.y > imageWidth) {
                faceRightBottomPointF.y = imageWidth;
            }
        }

        Path clipPath = new Path();
        imageCanvas.save();
        clipPath.addRect(posX + lineWidth, posY + lineWidth, faceRightBottomPointF.x - lineWidth, faceRightBottomPointF.y - lineWidth, Path.Direction.CW);
        imageCanvas.clipPath(clipPath, Region.Op.DIFFERENCE);
        imageCanvas.drawRect(posX, posY, faceRightBottomPointF.x, faceRightBottomPointF.y, linePaint);
        imageCanvas.restore();

        // IDを表示
        Paint idPaint = new Paint();
        idPaint.setColor(Color.BLACK);
        idPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.face_id_width));
        imageCanvas.drawText("ID: " + String.valueOf(id), posX, posY, idPaint);
    }

    protected String getLandmarkTypeString(int type){
        String ret = null;

        switch (type) {
            case Landmark.BOTTOM_MOUTH:
                ret = "bottom mouth";
                break;
            case Landmark.LEFT_CHEEK:
                ret = "left cheek";
                break;
            case Landmark.LEFT_EAR:
                ret = "left ear";
                break;
            case Landmark.LEFT_EAR_TIP:
                ret = "left ear tip";
                break;
            case Landmark.LEFT_EYE:
                ret = "left eye";
                break;
            case Landmark.LEFT_MOUTH:
                ret = "left mouth";
                break;
            case Landmark.NOSE_BASE:
                ret = "nose base";
                break;
            case Landmark.RIGHT_CHEEK:
                ret = "right cheek";
                break;
            case Landmark.RIGHT_EAR:
                ret = "right ear";
                break;
            case Landmark.RIGHT_EAR_TIP:
                ret = "right ear tip";
                break;
            case Landmark.RIGHT_EYE:
                ret = "right eye";
                break;
            case Landmark.RIGHT_MOUTH:
                ret = "right mouth";
                break;
        }

        return ret;
    }

    protected int getAdapterPosition(RecyclerView.ViewHolder holder){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ? holder.getAdapterPosition() : holder.getPosition();
    }
}
