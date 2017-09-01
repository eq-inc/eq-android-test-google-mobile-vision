package jp.eq_inc.testmobilevision.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.AndroidUtil;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Stack;

import jp.co.thcomp.util.LogUtil;
import jp.eq_inc.testmobilevision.R;
import jp.eq_inc.testmobilevision.helper.MovieFrameDecoder;

public class MovieFrameAdapter extends AbstractFaceDetectCursorAdapter {
    private static final String TAG = MovieFrameAdapter.class.getSimpleName();
    private static final int MiniKindThumbnailLongSide = 512;
    private String mMoviePath;
    private FrameGrab mGrab = null;
    private int mMovieDurationMS = 0;
    private MovieFrameDecoder mDecoder;

    public MovieFrameAdapter(Context context) {
        super(context);
    }

    public void setMoviePath(String moviePath) {
        mMoviePath = moviePath;
    }

    public Bitmap getFrameBitmap(double second) {
        Bitmap frameBitmap = null;

        try {
            Picture picture = mGrab.seekToSecondSloppy(second).getNativeFrame();
            frameBitmap = AndroidUtil.toBitmap(picture);
        } catch (JCodecException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return frameBitmap;
    }

    public void release(){
        if(mDecoder != null){
            mDecoder.release();
            mDecoder = null;
        }
    }

    public void updateAsync() {
        final File movieFile = mMoviePath != null ? new File(mMoviePath) : null;

        if ((mUpdateThread == null) && (movieFile != null) && movieFile.exists()) {
            mUpdateThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Cursor cursor = mContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Video.VideoColumns.DURATION}, MediaStore.MediaColumns.DATA + "=?", new String[]{movieFile.getAbsolutePath()}, null);
                        if (cursor != null) {
                            try {
                                if (cursor.getCount() > 0) {
                                    cursor.moveToFirst();
                                    final int fDurationMS = cursor.getInt(0);

                                    if (fDurationMS > 0) {
                                        try {
                                            final FrameGrab fGrab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(movieFile));

                                            if (fGrab != null) {
                                                mMainLooperHandler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if(mDecoder != null){
                                                            mDecoder.release();
                                                        }
                                                        mDecoder = new MovieFrameDecoder(fGrab, mDecodeCallback);
                                                        mDecoder.setMaxBitmapSideLength(MiniKindThumbnailLongSide);

                                                        mGrab = fGrab;
                                                        mMovieDurationMS = fDurationMS;
                                                        notifyDataSetChanged();
                                                    }
                                                });
                                            }
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                        } catch (JCodecException e) {
                                            e.printStackTrace();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        mMainLooperHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mGrab = null;
                                                mMovieDurationMS = 0;
                                                notifyDataSetChanged();
                                            }
                                        });
                                    }
                                }
                            } finally {
                                cursor.close();
                            }
                        }

                    } finally {
                        mUpdateThread = null;
                    }
                }
            });
            mUpdateThread.start();
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        LogUtil.d(TAG, "holder=" + holder + ", position=" + position);

        holder.itemView.setTag(ViewTagHolder, holder);
        mDecoder.add(holder, position);
    }

    @Override
    public int getItemCount() {
        return mMovieDurationMS > 0 ? (mMovieDurationMS / 1000) + 1 : 0;
    }

    private MovieFrameDecoder.Callback mDecodeCallback = new MovieFrameDecoder.Callback() {
        @Override
        public void onDecoded(final RecyclerView.ViewHolder holder, final int position, Bitmap frameBitmap) {
            final Bitmap thumbnailBitmap = frameBitmap;
            int currentPosition = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ? holder.getAdapterPosition() : holder.getPosition();
            if (currentPosition == position) {
                mMainLooperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int currentPosition = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ? holder.getAdapterPosition() : holder.getPosition();
                        LogUtil.d(TAG, "holder=" + holder + ", currentPosition=" + currentPosition + ", position=" + position);
                        if (currentPosition == position) {
                            ((ImageView) holder.itemView.findViewById(R.id.ivImage)).setImageBitmap(thumbnailBitmap);
                            ((InnerViewHolder)holder).setImage(thumbnailBitmap);
                        } else {
                            thumbnailBitmap.recycle();
                        }
                    }
                });
            } else {
                thumbnailBitmap.recycle();
            }
        }
    };
}
