package jp.eq_inc.testmobilevision;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

import jp.co.thcomp.util.Constant;
import jp.co.thcomp.util.LogUtil;

abstract public class AbstractDetectActivity extends AppCompatActivity {
    private static final int RequestCodeRuntimePermission = AbstractDetectActivity.class.hashCode() & 0x0000FFFF;
    protected Handler mMainLooperHandler;

    abstract void onRequestPermissionsResult(boolean allGranted);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.logoutput(Constant.LOG_SWITCH.LOG_SWITCH_ERROR | Constant.LOG_SWITCH.LOG_SWITCH_WARNING | Constant.LOG_SWITCH.LOG_SWITCH_INFO | Constant.LOG_SWITCH.LOG_SWITCH_DEBUG);
        mMainLooperHandler = new Handler(Looper.getMainLooper());

        String[] runtimePermissions = getRuntimePermissions();
        if (runtimePermissions != null && runtimePermissions.length > 0) {
            ArrayList<Integer> needRequestRuntimePermissionIndexList = new ArrayList<Integer>();
            for (int i = 0, size = runtimePermissions.length; i < size; i++) {
                if (ActivityCompat.checkSelfPermission(this, runtimePermissions[i]) == PackageManager.PERMISSION_DENIED) {
//                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, runtimePermissions[i])){
//                        needRequestRuntimePermissionIndexList.add(i);
//                    }
                    needRequestRuntimePermissionIndexList.add(i);
                }
            }

            if (needRequestRuntimePermissionIndexList.size() > 0) {
                ArrayList<String> requestRuntimePermissionList = new ArrayList<String>();
                for (int needRequestRuntimePermissionIndex : needRequestRuntimePermissionIndexList) {
                    requestRuntimePermissionList.add(runtimePermissions[needRequestRuntimePermissionIndex]);
                }

                ActivityCompat.requestPermissions(this, requestRuntimePermissionList.toArray(new String[0]), RequestCodeRuntimePermission);
            } else {
                mMainLooperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onRequestPermissionsResult(true);
                    }
                });
            }
        } else {
            mMainLooperHandler.post(new Runnable() {
                @Override
                public void run() {
                    onRequestPermissionsResult(true);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean allGranted = true;
        if (grantResults != null && grantResults.length > 0) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    allGranted = false;
                    break;
                }
            }
        } else {
            allGranted = false;
        }

        onRequestPermissionsResult(allGranted);
    }

    protected String[] getRuntimePermissions() {
        return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    }
}
