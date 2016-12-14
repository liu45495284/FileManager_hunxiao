package com.freeme.filemanager.view;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Added by droi xueweili for fragment lazy load 20160509
 * @author xueweili
 *
 */
public class BaseFragment extends Fragment {

    private final static String TAG = "BaseFragment";
    protected boolean isCreated = false;

    protected boolean isVisiBle = false;
    protected boolean isCreateView = false;
    private boolean isInit = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isCreated = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = onFragmentCreateView(inflater, container,
                savedInstanceState);
        if (view != null) {
            isCreateView = true;
           /* if (isVisiBle) {
                initFragementData();
            }*/
            return view;
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public View onFragmentCreateView(LayoutInflater inflater,
                                     ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onFragmentCreateView");
        return null;
    }

    /**
     */
    public void fragmentHint() {
        Log.i(TAG, "fragmentHint");
    }

    /**
     */
    public void fragmentShow() {
        Log.i(TAG, "fragmentShow");
        if (isCreateView) {
            initFragementData();
        }
    }

   
    private void initFragementData() {
        Log.i(TAG, "initFragementData isInit=" + isInit);
        if (isInit) {
            return;
        }

        isInit = true;
        initUserData();
    }

    public void initUserData() {
        Log.i(TAG, "initUserData");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        if (isVisiBle) {
            fragmentShow();
        }
    }

    public boolean isVisiBle() {
        return isVisiBle;
    }

    public void setVisiBle(boolean isVisiBle) {
        this.isVisiBle = isVisiBle;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isVisiBle) {
            fragmentHint();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.i(TAG, "setUserVisibleHint" + "" + isVisibleToUser);
        if (!isCreated) {
            return;
        }
        if (isVisibleToUser) {
            isVisiBle = true;
            if (isCreateView) {
                fragmentShow();
                pagerUserVisible();
            }
        } else {
            isVisiBle = false;
            fragmentHint();
            pagerUserHide();
        }
    }
    protected void pagerUserVisible() {
    }
    protected void pagerUserHide() {
        
    }
}
