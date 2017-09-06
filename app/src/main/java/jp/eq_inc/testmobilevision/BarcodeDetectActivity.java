package jp.eq_inc.testmobilevision;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jp.eq_inc.testmobilevision.fragment.AbstractDetectFragment;
import jp.eq_inc.testmobilevision.fragment.OnFragmentInteractionListener;

public class BarcodeDetectActivity extends AbstractDetectActivity implements OnFragmentInteractionListener {
    public static final String INTENT_STRING_PARAM_FRAGMENT_NAME = "INTENT_STRING_PARAM_FRAGMENT_NAME";
    public static final String INTENT_BUNDLE_PARAM_FRAGMENT_PARAM = "INTENT_BUNDLE_PARAM_FRAGMENT_PARAM";
    private AbstractDetectFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean needFinish = true;
        Intent intent = getIntent();
        String fragmentName = intent.getStringExtra(INTENT_STRING_PARAM_FRAGMENT_NAME);
        Bundle fragmentParam = intent.getBundleExtra(INTENT_BUNDLE_PARAM_FRAGMENT_PARAM);

        if (fragmentName != null && fragmentName.length() > 0) {
            try {
                Class fragmentClass = Class.forName(fragmentName);
                Method newInstanceMethod = fragmentClass.getMethod("newInstance", Bundle.class);
                mFragment = (AbstractDetectFragment) newInstanceMethod.invoke(null, fragmentParam);
                needFinish = false;
            } catch (ClassNotFoundException e) {
            } catch (NoSuchMethodException e) {
            } catch (InvocationTargetException e) {
            } catch (IllegalAccessException e) {
            }
        }

        if (needFinish) {
            finish();
        } else {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_barcode_detect);

            // rotation
            Spinner frameRotationSpinner = (Spinner) findViewById(R.id.spnrRotation);
            frameRotationSpinner.setAdapter(new FrameRotationSpinner(this));
            frameRotationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mFragment.changeCommonParams();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            // barcode format
            Spinner barcodeFormatSpinner = (Spinner) findViewById(R.id.spnrBarcodeFormat);
            barcodeFormatSpinner.setAdapter(new BarcodeFormatSpinner(this));
            barcodeFormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mFragment.changeCommonParams();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    @Override
    void onRequestPermissionsResult(boolean allGranted) {
        if (allGranted) {
            getSupportFragmentManager().beginTransaction().add(R.id.flFragment, mFragment).commitAllowingStateLoss();
        }
    }

    @Override
    protected String[] getRuntimePermissions() {
        return mFragment.getRuntimePermissions();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    private static class FrameRotationSpinner extends BaseAdapter {
        private Integer[] mItemArray = {
                null,
                Frame.ROTATION_0,
                Frame.ROTATION_90,
                Frame.ROTATION_180,
                Frame.ROTATION_270,
        };
        private String[] mMenuTitleArray;
        private Context mContext;

        public FrameRotationSpinner(Context context) {
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

    private static class BarcodeFormatSpinner extends BaseAdapter {
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

        public BarcodeFormatSpinner(Context context) {
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
}
