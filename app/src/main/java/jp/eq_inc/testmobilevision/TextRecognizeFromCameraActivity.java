package jp.eq_inc.testmobilevision;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jp.co.thcomp.util.ToastUtil;
import jp.eq_inc.testmobilevision.adapter.FrameRotationAdapter;
import jp.eq_inc.testmobilevision.fragment.AbstractDetectFragment;
import jp.eq_inc.testmobilevision.fragment.OnFragmentInteractionListener;

public class TextRecognizeFromCameraActivity extends AbstractDetectActivity implements OnFragmentInteractionListener {
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
            setContentView(R.layout.activity_text_recognize_from_camera);
            ((SwitchCompat) findViewById(R.id.scUseMultiProcessor)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);
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
            mFragment.changeCommonParams();
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
                mFragment.changeCommonParams();
            } catch (NumberFormatException e) {
                ToastUtil.showToast(TextRecognizeFromCameraActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG);
            }
        }
    };
}
