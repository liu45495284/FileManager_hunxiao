package com.freeme.filemanager.util;

import android.app.Activity;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.IMountService.Stub;

import com.freeme.filemanager.R;

import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class MountHelper {
    private static final String LOG_TAG = "MountHelper";
    private static Context mContext;
    private static IMountService mMountService = null;
    private static MountHelper sInstance;
    private String mVolumpath;
    
    private MountHelper() {
        IBinder iBinder = ServiceManager.getService("mount");
        if (iBinder != null) {
            mMountService = IMountService.Stub.asInterface(iBinder);
            return;
        }
    }

    public static MountHelper getInstance(Context context)
    {
        if (sInstance == null) {
            mContext = context;
            sInstance = new MountHelper();
        }
        MountHelper localMountHelper = sInstance;
        return localMountHelper;
    }

    public void unMount(String volumpath) {
        if(TextUtils.isEmpty(volumpath)){
            return;
        }
        mVolumpath = volumpath;
        doUnmount();
    }
    
    private boolean hasAppsAccessingStorage() throws RemoteException {
        int stUsers[] = mMountService.getStorageUsers(mVolumpath);
        if (stUsers != null && stUsers.length > 0) {
            return true;
        }
        return true;
    }
    
    private void doUnmount(){
        Log.i("doUnmount", "mVolumpath is: "+mVolumpath);
        if(mVolumpath.startsWith("/storage")){
            Toast.makeText(mContext, R.string.unmount_sdcard_inform_text, Toast.LENGTH_SHORT).show();
        }else if(mVolumpath.startsWith("/mnt")){
            Toast.makeText(mContext, R.string.unmount_usb_storage_inform_text, Toast.LENGTH_SHORT).show();
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    mMountService.unmountVolume(mVolumpath, true, false);
                } catch (RemoteException e) {
                    if(mVolumpath.startsWith("/storage")){
                        Toast.makeText(mContext, R.string.dlg_error_unmount_sdcard_text, Toast.LENGTH_SHORT).show();
                    }else if(mVolumpath.startsWith("/mnt")){
                        Toast.makeText(mContext, R.string.dlg_error_unmount_usb_storage_text, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }.start();
    }
    
}
