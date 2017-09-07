package jp.eq_inc.testmobilevision.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;

import jp.eq_inc.testmobilevision.R;

public class FrameRotationAdapter extends BaseAdapter {
    private Integer[] mItemArray = {
            Frame.ROTATION_0,
            Frame.ROTATION_90,
            Frame.ROTATION_180,
            Frame.ROTATION_270,
    };
    private String[] mMenuTitleArray;
    private Context mContext;

    public FrameRotationAdapter(Context context) {
        mContext = context;
        mMenuTitleArray = context.getResources().getStringArray(R.array.frame_rotation_array);
    }

    @Override
    public int getCount() {
        return mItemArray.length;
    }

    @Override
    public Object getItem(int position) {
        return mItemArray[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_frame_rotation_spinner, parent, false);
        }

        TextView itemText = (TextView) convertView.findViewById(R.id.tvFrameRotation);
        itemText.setText(mMenuTitleArray[position]);

        return convertView;
    }
}
