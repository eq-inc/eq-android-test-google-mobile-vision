package jp.eq_inc.testmobilevision.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.gms.vision.barcode.Barcode;

import jp.eq_inc.testmobilevision.R;

public class BarcodeFormatAdapter extends BaseAdapter {
    private Integer[] mItemArray = {
            Barcode.ALL_FORMATS,
            Barcode.AZTEC,
            Barcode.CODABAR,
            Barcode.CODE_39,
            Barcode.CODE_93,
            Barcode.CODE_128,
            Barcode.DATA_MATRIX,
            Barcode.EAN_8,
            Barcode.EAN_13,
            Barcode.ITF,
            Barcode.PDF417,
            Barcode.QR_CODE,
            Barcode.UPC_A,
            Barcode.UPC_E,
    };
    private String[] mMenuTitleArray;
    private Context mContext;

    public BarcodeFormatAdapter(Context context) {
        mContext = context;
        mMenuTitleArray = context.getResources().getStringArray(R.array.barcode_format_array);
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
