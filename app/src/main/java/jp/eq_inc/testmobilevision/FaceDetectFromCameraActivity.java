package jp.eq_inc.testmobilevision;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jp.co.thcomp.util.ToastUtil;
import jp.eq_inc.testmobilevision.fragment.AbstractFaceDetectFragment;
import jp.eq_inc.testmobilevision.fragment.OnFragmentInteractionListener;

public class FaceDetectFromCameraActivity extends AbstractFaceDetectActivity implements OnFragmentInteractionListener {
    public static final String INTENT_STRING_PARAM_FRAGMENT_NAME = "INTENT_STRING_PARAM_FRAGMENT_NAME";
    public static final String INTENT_BUNDLE_PARAM_FRAGMENT_PARAM = "INTENT_BUNDLE_PARAM_FRAGMENT_PARAM";
    private AbstractFaceDetectFragment mFragment;

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
                mFragment = (AbstractFaceDetectFragment) newInstanceMethod.invoke(null, fragmentParam);
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
            setContentView(R.layout.activity_face_detect_from_camera);
            Spinner frameRotationSpinner = (Spinner) findViewById(R.id.spnrRotation);
            frameRotationSpinner.setAdapter(new FrameRotationSpinner(this));
            frameRotationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mFragment.changeFaceDetectParams();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            ((SwitchCompat) findViewById(R.id.scUseMultiProcessor)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);
            ((SwitchCompat) findViewById(R.id.scClassification)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);
            ((SwitchCompat) findViewById(R.id.scLandmark)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);
            ((SwitchCompat) findViewById(R.id.scDetectMode)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);
            ((SwitchCompat) findViewById(R.id.scProminentFaceOnly)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);
            ((SwitchCompat) findViewById(R.id.scFaceTracking)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);
            ((SwitchCompat) findViewById(R.id.scAutoFocus)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);
            ((SwitchCompat) findViewById(R.id.scFacing)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);

            ((EditText) findViewById(R.id.etDetectFps)).addTextChangedListener(mDetectFpsChangedListener);

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

    private CompoundButton.OnCheckedChangeListener mSwitchCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mFragment.changeFaceDetectParams();
        }
    };

    private TextWatcher mDetectFpsChangedListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // 処理なし
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // 処理なし
        }

        @Override
        public void afterTextChanged(Editable s) {
            try {
                Float.parseFloat(s.toString());
                mFragment.changeFaceDetectParams();
            } catch (NumberFormatException e) {
                ToastUtil.showToast(FaceDetectFromCameraActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG);
            }
        }
    };

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
}
