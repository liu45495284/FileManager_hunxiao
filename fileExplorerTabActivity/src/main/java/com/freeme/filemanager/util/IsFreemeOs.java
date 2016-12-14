package com.freeme.filemanager.util;

import java.lang.reflect.Field;
import org.apache.http.util.TextUtils;
import android.os.Build;
import android.util.Log;

public class IsFreemeOs {
    private static final String TAG = "freemeVersion";
    
    public static boolean isFreemeOs() {
        String freemeVersion = "";
        try {
            Field freemeosField = Build.VERSION.class.getDeclaredField("FREEMEOS");
            freemeosField.setAccessible(true);
            Build.VERSION v = new Build.VERSION();
            Object o = freemeosField.get(v);
            freemeVersion = o.toString();
            Log.i(TAG, "freemeVersion = " + freemeVersion);
        } catch (Exception e) {
            return false;
        }

        if (TextUtils.isEmpty(freemeVersion)) {
            return false;
        }
        return true;
    }

}
