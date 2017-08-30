package jp.eq_inc.testmobilevision;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jp.eq_inc.testmobilevision.fragment.OnFragmentInteractionListener;

public class FaceDetectActivity extends AbstractFaceDetectActivity implements OnFragmentInteractionListener {
    public static final String INTENT_STRING_PARAM_FRAGMENT_NAME = "INTENT_STRING_PARAM_FRAGMENT_NAME";
    public static final String INTENT_BUNDLE_PARAM_FRAGMENT_PARAM = "INTENT_BUNDLE_PARAM_FRAGMENT_PARAM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detect);
    }

    @Override
    void onRequestPermissionsResult(boolean allGranted) {
        boolean needFinish = true;

        if (allGranted) {
            Intent intent = getIntent();
            String fragmentName = intent.getStringExtra(INTENT_STRING_PARAM_FRAGMENT_NAME);
            Bundle fragmentParam = intent.getBundleExtra(INTENT_BUNDLE_PARAM_FRAGMENT_PARAM);

            if (fragmentName != null && fragmentName.length() > 0) {
                try {
                    Class fragmentClass = Class.forName(fragmentName);
                    Method newInstanceMethod = fragmentClass.getMethod("newInstance", Bundle.class);
                    Fragment fragment = (Fragment) newInstanceMethod.invoke(null, fragmentParam);
                    getSupportFragmentManager().beginTransaction().add(R.id.flFragment, fragment).commitAllowingStateLoss();
                    needFinish = false;
                } catch (ClassNotFoundException e) {
                } catch (NoSuchMethodException e) {
                } catch (InvocationTargetException e) {
                } catch (IllegalAccessException e) {
                }
            }
        }

        if (needFinish) {
            finish();
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
