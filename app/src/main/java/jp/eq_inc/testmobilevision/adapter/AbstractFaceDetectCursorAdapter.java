package jp.eq_inc.testmobilevision.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import jp.co.thcomp.util.LogUtil;
import jp.eq_inc.testmobilevision.R;

abstract public class AbstractFaceDetectCursorAdapter extends RecyclerView.Adapter {
    private static final String TAG = AbstractFaceDetectCursorAdapter.class.getSimpleName();
    public static final int ViewTagHolder = "ViewTagHolder".hashCode();
    protected Context mContext;
    protected Cursor mCursor = null;
    protected Handler mMainLooperHandler;
    protected View.OnClickListener mItemClickListener;
    protected Thread mUpdateThread;

    abstract public void updateAsync();

    public AbstractFaceDetectCursorAdapter(Context context) {
        mContext = context;
        mMainLooperHandler = new Handler(Looper.getMainLooper());
    }

    public void setOnItemClickListener(View.OnClickListener listener) {
        mItemClickListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.item_detect_list, parent, false);

        view.setOnClickListener(mRootClickListener);

        return new InnerViewHolder(view);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        LogUtil.d(TAG, "recycled holder=" + holder);

        ImageView imageView = (ImageView) holder.itemView.findViewById(R.id.ivImage);
        if(imageView != null){
            imageView.setImageBitmap(null);
        }
        if (holder instanceof InnerViewHolder) {
            ((InnerViewHolder) holder).release();
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

    private View.OnClickListener mRootClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mItemClickListener != null) {
                mItemClickListener.onClick(v);
            }
        }
    };

    public static class InnerViewHolder extends RecyclerView.ViewHolder {
        protected Bitmap mBitmap;

        public InnerViewHolder(View itemView) {
            super(itemView);
        }

        public void setImage(Bitmap bitmap) {
            if (mBitmap != null) {
                mBitmap.recycle();
                mBitmap = null;
            }

            mBitmap = bitmap;
        }

        public void release() {
            if (mBitmap != null) {
                mBitmap.recycle();
                mBitmap = null;
            }
        }
    }
}
