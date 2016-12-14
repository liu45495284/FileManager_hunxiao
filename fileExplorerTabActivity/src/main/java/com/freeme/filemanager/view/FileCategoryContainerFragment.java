package com.freeme.filemanager.view;

import com.freeme.filemanager.FileExplorerTabActivity.IBackPressedListener;
import com.freeme.filemanager.R;
import com.freeme.filemanager.controller.FileViewInteractionHub;

import android.R.transition;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FileCategoryContainerFragment extends BaseFragment implements
        IBackPressedListener {
    private static final String TAG = "FileCategoryContainerFragment";

    private View mRootView = null;

    public FileViewInteractionHub mFileViewInteractionHub;

    private FragmentManager mFragmentManager = null;

    private boolean isInit;

    private static final int CMD_INIT = 0x01;

    @Override
    public View onFragmentCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        isInit = false;
        mRootView = inflater.inflate(R.layout.file_explorer_container, null);
        return mRootView;
    }

    public void setConfigurationChanged(boolean isChange) {

    }

    @Override
    public void initUserData() {
        super.initUserData();

    }

    public boolean isHomePage() {
        return false;
    }

    public void init() {
        if (!isInit) {
            handler.sendEmptyMessageDelayed(CMD_INIT, 0);
        }
    }

    Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            int what = msg.what;
            switch (what) {
            case CMD_INIT:
                FileCategroyFastFragment fragment = new FileCategroyFastFragment();
                mFragmentManager = getChildFragmentManager();
                FragmentTransaction transaction = mFragmentManager
                        .beginTransaction();
                transaction.replace(R.id.fragment_container, fragment);
                //*/ freeme.liuhaoran , 20161008 , for exception:Can not perform this action after onSaveInstanceState
                transaction.commitAllowingStateLoss();
                //*/
                isInit = true;
                break;
            default:
                break;
            }
        };
    };

    @Override
    public void fragmentShow() {
        super.fragmentShow();
        init();
    }

    @Override
    protected void pagerUserHide() {
        if(mFragmentManager == null){
            return;
        }
        Fragment fragment = mFragmentManager
                .findFragmentById(R.id.fragment_container);
        if (fragment != null && (fragment instanceof BaseCategoryFragment)) {
            ((BaseCategoryFragment) fragment).pagerUserHide();
        }
    }

    @Override
    protected void pagerUserVisible() {
        if(mFragmentManager == null){
            return;
        }
        Fragment fragment = mFragmentManager
                .findFragmentById(R.id.fragment_container);
        if (fragment != null && (fragment instanceof BaseCategoryFragment)) {
            ((BaseCategoryFragment) fragment).pagerUserVisible();
        }
    }

    @Override
    public boolean onBack() {
        if(mFragmentManager == null){
            return false;
        }
        if (mFragmentManager.getBackStackEntryCount() == 0) {
            return false;
        } else {
            mFragmentManager.popBackStack();
            return true;
        }
    }

}
