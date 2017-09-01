package jp.eq_inc.testmobilevision.helper;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.widget.RecyclerView;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.AndroidUtil;
import org.jcodec.common.model.Picture;

import java.io.IOException;
import java.util.Stack;

public class MovieFrameDecoder {
    private FrameGrab mGrab;
    private HandlerThread mDecoderThread;
    private Stack<MovieFrameDecodeHolder> mRequestStack = new Stack<MovieFrameDecodeHolder>();
    private Handler mWorkLooperHandler;
    private MovieFrameDecoder.Callback mCallback;
    private int mDecodedBitmapMaxSideLength = 0;

    public MovieFrameDecoder(FrameGrab grab, MovieFrameDecoder.Callback callback){
        mGrab = grab;
        mCallback = callback;
        mDecoderThread = new HandlerThread(getClass().getSimpleName());
        mDecoderThread.start();
        mWorkLooperHandler = new Handler(mDecoderThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                MovieFrameDecodeHolder decodeHolder = mRequestStack.pop();
                Bitmap frameBitmap = null;

                if(decodeHolder != null){
                    Picture picture = null;
                    try {
                        picture = mGrab.seekToSecondSloppy(decodeHolder.position).getNativeFrame();
                        frameBitmap = AndroidUtil.toBitmap(picture);

                        if(mDecodedBitmapMaxSideLength > 0){
                            float frameWidth = frameBitmap.getWidth();
                            float frameHeight = frameBitmap.getHeight();

                            if (frameWidth > mDecodedBitmapMaxSideLength || frameHeight > mDecodedBitmapMaxSideLength) {
                                Rect changedRect = null;
                                float rate = 0;

                                if (frameWidth > frameHeight) {
                                    rate = frameHeight / frameWidth;
                                    changedRect = new Rect(0, 0, mDecodedBitmapMaxSideLength, (int) (rate * mDecodedBitmapMaxSideLength));
                                } else {
                                    rate = frameWidth / frameHeight;
                                    changedRect = new Rect(0, 0, (int) (rate * mDecodedBitmapMaxSideLength), mDecodedBitmapMaxSideLength);
                                }

                                Bitmap changedBitmap = Bitmap.createScaledBitmap(frameBitmap, changedRect.width(), changedRect.height(), false);
                                if (!frameBitmap.equals(changedBitmap)) {
                                    frameBitmap.recycle();
                                    frameBitmap = changedBitmap;
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JCodecException e) {
                        e.printStackTrace();
                    }

                    mCallback.onDecoded(decodeHolder.holder, decodeHolder.position, frameBitmap);
                }

                return true;
            }
        });
    }

    public void setMaxBitmapSideLength(int length){
        mDecodedBitmapMaxSideLength = length;
    }

    public void add(RecyclerView.ViewHolder holder, int position){
        mRequestStack.push(new MovieFrameDecodeHolder(holder, position));
        mWorkLooperHandler.sendEmptyMessage(0);
    }

    public void release(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            mDecoderThread.quitSafely();
        }else{
            mDecoderThread.quit();
        }
    }

    public interface Callback{
        void onDecoded(RecyclerView.ViewHolder holder, int position, Bitmap bitmap);
    }


    private static class MovieFrameDecodeHolder {
        public RecyclerView.ViewHolder holder;
        public int position;

        public MovieFrameDecodeHolder(RecyclerView.ViewHolder holder, int position){
            this.holder = holder;
            this.position = position;
        }
    }
}
