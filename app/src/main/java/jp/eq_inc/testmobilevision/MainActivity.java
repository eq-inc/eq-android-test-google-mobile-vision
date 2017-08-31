package jp.eq_inc.testmobilevision;

import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.Size;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.Serializable;

import jp.eq_inc.testmobilevision.fragment.FaceDetectFromPhotoFragment;

public class MainActivity extends AppCompatActivity {
    private static final MenuItem[] sMenuItemArray = new MenuItem[]{
            new MenuItem("Face Detect from Photo", FaceDetectActivity.class, new String[]{FaceDetectActivity.INTENT_STRING_PARAM_FRAGMENT_NAME}, new String[]{FaceDetectFromPhotoFragment.class.getName()}),
            //new MenuItem("Face Detect from Movie", FaceDetectFromMovieActivity.class),
            //new MenuItem("Face Detect from Camera", FaceDetectFromCameraActivity.class),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createMenuList();
    }

    private void createMenuList() {
        LinearLayout menuListLayout = (LinearLayout) findViewById(R.id.llMenuList);
        LayoutInflater inflater = getLayoutInflater();

        for (MenuItem menuItem : sMenuItemArray) {
            View menuItemView = inflater.inflate(R.layout.item_menu, menuListLayout, false);
            ((TextView) menuItemView.findViewById(R.id.tvMenuTitle)).setText(menuItem.mTitle);
            menuItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MenuItem menuItem = (MenuItem) v.getTag();
                    Intent intent = new Intent();
                    intent.setClass(MainActivity.this, menuItem.mActivityClass);

                    if (menuItem.mParamNameArray != null && menuItem.mParamValueArray != null) {
                        for (int i = 0, size = menuItem.mParamNameArray.length; i < size; i++) {
                            if (menuItem.mParamValueArray[i] instanceof Boolean) {
                                intent.putExtra(menuItem.mParamNameArray[i], (Boolean) menuItem.mParamValueArray[i]);
                            } else if (menuItem.mParamValueArray[i] instanceof Byte) {
                                intent.putExtra(menuItem.mParamNameArray[i], (Byte) menuItem.mParamValueArray[i]);
                            } else if (menuItem.mParamValueArray[i] instanceof Character) {
                                intent.putExtra(menuItem.mParamNameArray[i], (Character) menuItem.mParamValueArray[i]);
                            } else if (menuItem.mParamValueArray[i] instanceof Float) {
                                intent.putExtra(menuItem.mParamNameArray[i], (Float) menuItem.mParamValueArray[i]);
                            } else if (menuItem.mParamValueArray[i] instanceof Double) {
                                intent.putExtra(menuItem.mParamNameArray[i], (Double) menuItem.mParamValueArray[i]);
                            } else if (menuItem.mParamValueArray[i] instanceof Integer) {
                                intent.putExtra(menuItem.mParamNameArray[i], (Integer) menuItem.mParamValueArray[i]);
                            } else if (menuItem.mParamValueArray[i] instanceof Long) {
                                intent.putExtra(menuItem.mParamNameArray[i], (Long) menuItem.mParamValueArray[i]);
                            } else if (menuItem.mParamValueArray[i] instanceof Parcelable) {
                                intent.putExtra(menuItem.mParamNameArray[i], (Parcelable) menuItem.mParamValueArray[i]);
                            } else if (menuItem.mParamValueArray[i] instanceof Short) {
                                intent.putExtra(menuItem.mParamNameArray[i], (Short) menuItem.mParamValueArray[i]);
                            } else if (menuItem.mParamValueArray[i] instanceof Serializable) {
                                intent.putExtra(menuItem.mParamNameArray[i], (Serializable) menuItem.mParamValueArray[i]);
                            } else if (menuItem.mParamValueArray[i] instanceof String) {
                                intent.putExtra(menuItem.mParamNameArray[i], (String) menuItem.mParamValueArray[i]);
                            }
                        }
                    }
                    MainActivity.this.startActivity(intent);
                }
            });
            menuItemView.setTag(menuItem);
            menuListLayout.addView(menuItemView);
        }
    }

    private static class MenuItem {
        private String mTitle;
        private Class mActivityClass;
        private String[] mParamNameArray;
        private Object[] mParamValueArray;

        public MenuItem(String title, Class activityClass) {
            mTitle = title;
            mActivityClass = activityClass;
        }

        public MenuItem(String title, Class activityClass, String[] intentParamNameArray, Object[] intentParamValueArray) {
            mTitle = title;
            mActivityClass = activityClass;

            if (intentParamNameArray != null && intentParamValueArray != null) {
                if (intentParamNameArray.length != intentParamValueArray.length) {
                    throw new IllegalArgumentException("intentParamNameArray.length != intentParamValueArray.length");
                }

                mParamNameArray = intentParamNameArray;
                mParamValueArray = intentParamValueArray;
            }
        }
    }
}
