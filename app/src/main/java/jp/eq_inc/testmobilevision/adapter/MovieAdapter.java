package jp.eq_inc.testmobilevision.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;

import jp.eq_inc.testmobilevision.R;

public class MovieAdapter extends AbstractFaceDetectCursorAdapter {
    public MovieAdapter(Context context) {
        super(context);
    }

    public void updateAsync() {
        if (mUpdateThread == null) {
            mUpdateThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Cursor fCursor = MediaStore.Video.query(mContext.getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null);
                        if ((fCursor != null) && (fCursor.getCount() > 0)) {
                            mMainLooperHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mCursor != null) {
                                        mCursor.close();
                                    }

                                    mCursor = fCursor;
                                    MovieAdapter.this.notifyDataSetChanged();
                                }
                            });
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
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        holder.itemView.setTag(ViewTagHolder, holder);

        new Thread(new Runnable() {
            @Override
            public void run() {
                mCursor.moveToPosition(position);

                final Bitmap thumbnailBitmap = MediaStore.Video.Thumbnails.getThumbnail(
                        mContext.getContentResolver(),
                        mCursor.getLong(mCursor.getColumnIndex(MediaStore.MediaColumns._ID)),
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null);

                int currentPosition = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ? holder.getAdapterPosition() : holder.getPosition();
                if (currentPosition == position) {
                    mMainLooperHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            int currentPosition = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ? holder.getAdapterPosition() : holder.getPosition();
                            if (currentPosition == position) {
                                ((ImageView) holder.itemView.findViewById(R.id.ivImage)).setImageBitmap(thumbnailBitmap);
                                ((InnerViewHolder) holder).setImage(thumbnailBitmap);
                            }
                        }
                    });
                } else {
                    thumbnailBitmap.recycle();
                }
            }
        }).start();
    }
}
