package jp.eq_inc.testmobilevision.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import jp.eq_inc.testmobilevision.R;

public class ImageAdapter extends RecyclerView.Adapter {
    public static final int ViewTagHolder = "ViewTagHolder".hashCode();
    private Context mContext;
    private Cursor mCursor = null;
    private Handler mMainLooperHandler;
    private View.OnClickListener mItemClickListener;
    private Thread mUpdateThread;

    public ImageAdapter(Context context){
        mContext = context;
        mMainLooperHandler = new Handler(Looper.getMainLooper());
    }

    public void setOnItemClickListener(View.OnClickListener listener){
        mItemClickListener = listener;
    }

    public void updateAsync(){
        if(mUpdateThread == null){
            mUpdateThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        final Cursor fCursor = MediaStore.Images.Media.query(mContext.getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null);
                        if((fCursor != null) && (fCursor.getCount() > 0)){
                            mMainLooperHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if(mCursor != null){
                                        mCursor.close();
                                    }

                                    mCursor = fCursor;
                                    ImageAdapter.this.notifyDataSetChanged();
                                }
                            });
                        }
                    }finally {
                        mUpdateThread = null;
                    }
                }
            });
            mUpdateThread.start();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.item_detect_list, parent, false);

        view.setOnClickListener(mRootClickListener);

        return new InnerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        holder.itemView.setTag(ViewTagHolder, holder);

        new Thread(new Runnable() {
            @Override
            public void run() {
                mCursor.moveToPosition(position);
                mCursor.getLong(mCursor.getColumnIndex(MediaStore.MediaColumns._ID));

                final Bitmap thumbnailBitmap = MediaStore.Images.Thumbnails.getThumbnail(
                        mContext.getContentResolver(),
                        mCursor.getLong(mCursor.getColumnIndex(MediaStore.MediaColumns._ID)),
                        MediaStore.Images.Thumbnails.MINI_KIND,
                        null);

                int currentPosition = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ? holder.getAdapterPosition() : holder.getPosition();
                if(currentPosition == position){
                    mMainLooperHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            int currentPosition = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ? holder.getAdapterPosition() : holder.getPosition();
                            if(currentPosition == position){
                                ((ImageView)holder.itemView.findViewById(R.id.ivImage)).setImageBitmap(thumbnailBitmap);
                            }
                        }
                    });
                }else{
                    thumbnailBitmap.recycle();
                }
            }
        }).start();
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        if(holder instanceof InnerViewHolder){
            ((InnerViewHolder)holder).release();
        }
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(mCursor.getColumnIndex(MediaStore.MediaColumns._ID));
    }

    @Override
    public int getItemCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    private View.OnClickListener mRootClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            if(mItemClickListener != null){
                mItemClickListener.onClick(v);
            }
        }
    };

    public static class InnerViewHolder extends RecyclerView.ViewHolder{
        private Bitmap mBitmap;

        public InnerViewHolder(View itemView) {
            super(itemView);
        }

        public void setImage(Bitmap bitmap){
            if(mBitmap != null){
                mBitmap.recycle();
                mBitmap = null;
            }

            mBitmap = bitmap;
        }

        public void release(){
            if(mBitmap != null){
                mBitmap.recycle();
                mBitmap = null;
            }
        }
    }
}
