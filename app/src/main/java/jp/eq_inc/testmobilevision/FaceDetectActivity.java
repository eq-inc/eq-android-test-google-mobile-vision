package jp.eq_inc.testmobilevision;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jp.eq_inc.testmobilevision.adapter.FrameRotationAdapter;
import jp.eq_inc.testmobilevision.fragment.AbstractDetectFragment;
import jp.eq_inc.testmobilevision.fragment.OnFragmentInteractionListener;

public class FaceDetectActivity extends AbstractDetectActivity implements OnFragmentInteractionListener {
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
            setContentView(R.layout.activity_face_detect);
            Spinner frameRotationSpinner = (Spinner) findViewById(R.id.spnrRotation);
            frameRotationSpinner.setAdapter(new FrameRotationAdapter(this));
            frameRotationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mFragment.changeCommonParams();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            ((SwitchCompat) findViewById(R.id.scClassification)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);
            ((SwitchCompat) findViewById(R.id.scLandmark)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);
            ((SwitchCompat) findViewById(R.id.scDetectMode)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);
            ((SwitchCompat) findViewById(R.id.scProminentFaceOnly)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);
            ((SwitchCompat) findViewById(R.id.scFaceTracking)).setOnCheckedChangeListener(mSwitchCheckedChangeListener);
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
}
