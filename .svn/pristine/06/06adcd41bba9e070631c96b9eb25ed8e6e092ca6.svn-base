/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.freeme.filemanager.view;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import com.freeme.filemanager.R;
import com.freeme.filemanager.model.EditUtility;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.FileManagerLog;
import com.freeme.filemanager.util.MountPointHelper;
import com.umeng.analytics.MobclickAgent;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;


public class FileManagerSelectPathActivity extends FileManagerBaseActivity implements
        DialogInterface.OnDismissListener {
    private static final String TAG = "FileManagerSelectPathActivity";

    private static final String DOWNLOAD_PATH_KEY = "download path";
    private String mDownloadPath = "";
    private Button mBtnSave = null;
    private ImageButton mBtnCreateFolder = null;
    private Dialog mDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        mDownloadPath = getInitialDownloadPath();
        FileManagerLog.d(TAG, "Receive an intent with download path: " + mDownloadPath);

        if (mDownloadPath.equals(MountPointHelper.ROOT_PATH)
                || !mMountPointHelper.isFileRootMount(mDownloadPath)) {
            addTab(MountPointHelper.ROOT_PATH);
        } else {
            String[] result = mDownloadPath.split(MountPointHelper.SEPARATOR);
            int i = 1;
            for (i = 1; i < result.length - 1; i++) { // i=1 to skip first empty string and "/"
                addTab(result[i]);
                addToNavigationList(mCurrentDirPath, result[i + 1], 0);
            }
            addTab(result[i]);
        }
        updateHomeButton();

        LinearLayout bottomBar = (LinearLayout) findViewById(R.id.download_bottom_bar);
        mBtnSave = (Button) bottomBar.findViewById(R.id.download_btn_save);
        mBtnSave.setOnClickListener(this);
        Button btnCancel = (Button) bottomBar.findViewById(R.id.download_btn_cancel);
        btnCancel.setOnClickListener(this);

        LinearLayout backgroundBar = (LinearLayout) findViewById(R.id.bar_background);
        mBtnCreateFolder = (ImageButton) backgroundBar.findViewById(R.id.btn_create_folder);
        mBtnCreateFolder.setOnClickListener(this);

        boolean isCurrentVolumeMounted = mMountPointHelper.isFileRootMount(mCurrentDirPath);
        if (isCurrentVolumeMounted) {
            File dir = new File(mCurrentDirPath);
            if (dir.canWrite()) {
                mBtnCreateFolder.setEnabled(true);
                mBtnCreateFolder.setClickable(true);
            } else {
                mBtnCreateFolder.setEnabled(false);
                mBtnCreateFolder.setClickable(false);
            }
        } else {
            mBtnCreateFolder.setEnabled(false);
            mBtnCreateFolder.setClickable(false);
            mBtnSave.setEnabled(false);
            mBtnSave.setClickable(false);
            FileManagerLog.e(TAG, "SDCard is unmounted");
        }
    }

    String getInitialDownloadPath() {
        Bundle extras = getIntent().getExtras();
        String downloadPath = null;
        if (extras == null) {
            FileManagerLog.d(TAG, "Receive data from intent is null");
            return MountPointHelper.ROOT_PATH;
        }

        downloadPath = extras.getString(DOWNLOAD_PATH_KEY);
        FileManagerLog.d(TAG, "Retrieve path from intent: " + downloadPath);

        // if sdcard is not mounted, we do not check;
        // just keep original download path
        if (downloadPath == null) {
            FileManagerLog.d(TAG, "Path retrieved from intent is null");
            return MountPointHelper.ROOT_PATH;
        }

        boolean isSelectedVolumeMounted = mMountPointHelper.isFileRootMount(downloadPath);
        if (!isSelectedVolumeMounted) {
            return downloadPath;
        }

        File f = new File(downloadPath);
        if (f.exists()) {
            return downloadPath;
        }

        String path = f.getPath();
        if (!path.equalsIgnoreCase("/mnt/sdcard/download")) {
            return MountPointHelper.ROOT_PATH;
        }
        boolean result = EditUtility.createFolder(this, path);
        FileManagerLog.d(TAG, "Create download folder result: " + result);
        if (result) {
            return path;
        }

        return MountPointHelper.ROOT_PATH;
    }

    protected void updateHomeButton() {
        super.updateHomeButton();
        if (MountPointHelper.ROOT_PATH.equals(mCurrentDirPath)) {
            if (mBtnSave != null && mBtnCreateFolder != null) {
                mBtnSave.setEnabled(false);
                mBtnSave.setClickable(false);
                mBtnCreateFolder.setEnabled(false);
                mBtnCreateFolder.setClickable(false);
            }
        }
    }

    protected void prepareForMediaEventHandle(String action) {
        return;
    }

    protected void updateViewCompomentStateForMediaEvent(String action, String mountPoint) {
        if (action.equals(Intent.ACTION_MEDIA_MOUNTED) && mountPoint.equals(mCurrentDirPath)) {
            FileManagerLog.d(TAG, "Mount SDCard, Mount point: " + mountPoint);

            // enable create folder icon if sdcard is mounted
            if (new File(mCurrentDirPath).canWrite()) {
                mBtnCreateFolder.setEnabled(true);
                mBtnCreateFolder.setClickable(true);
                mBtnSave.setEnabled(true);
                mBtnSave.setClickable(true);
            } else {
                mBtnCreateFolder.setEnabled(false);
                mBtnCreateFolder.setClickable(false);
                mBtnSave.setEnabled(false);
                mBtnSave.setClickable(false);
            }

        } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
                && (mCurrentDirPath.startsWith(mountPoint) || mCurrentDirPath.equals(mountPoint))) {
            // disable create folder icon if sdcard is unmounted
            mBtnCreateFolder.setEnabled(false);
            mBtnCreateFolder.setClickable(false);
            mBtnSave.setEnabled(false);
            mBtnSave.setClickable(false);
        }
    }

    @Override
    protected void setMainContentView() {
        setContentView(R.layout.download_main);
        // set up a sliding navigation bar for navigation view
        mNavigationBar = (HorizontalScrollView) findViewById(R.id.navigation_bar);

        // set up a tab holder to hold all tabs on the navigation bar
        mTabsHolder = (LinearLayout) findViewById(R.id.tabs_holder);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileInfo selecteItemFileInfo = mFileInfoList.get(position);
        String focusedItemFileName = selecteItemFileInfo.getFileName();
        int top = view.getTop();
        FileManagerLog.v(TAG, "top = " + top);
        addToNavigationList(mCurrentDirPath, focusedItemFileName, top);
        addTab(focusedItemFileName);
        updateHomeButton();
        showDirectoryContent(mCurrentDirPath);
        File currentPath = new File(mCurrentDirPath);
        if (currentPath.canWrite()) {
            mBtnCreateFolder.setEnabled(true);
            mBtnCreateFolder.setClickable(true);
            mBtnSave.setEnabled(true);
            mBtnSave.setClickable(true);
        } else {
            mBtnCreateFolder.setEnabled(false);
            mBtnCreateFolder.setClickable(false);
            mBtnSave.setEnabled(false);
            mBtnSave.setClickable(false);
        }
    }

    @Override
    protected void loadFileInfoList(File[] files) {
        // load the file info first.
        for (File file : files) {
            if (file.isDirectory()) {
                FileInfo fileInfo = new FileInfo(this, file, null);
              //*/added by tyd carl,20120703.[tyd00429949]
               
                if(!fileInfo.getFileName().startsWith("."))
                    //*/
                mFileInfoList.add(fileInfo);
            }
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        FileManagerLog.d(TAG, "onClick: " + id);
        boolean isCurrentVolumeMounted = mMountPointHelper.isFileRootMount(mCurrentDirPath);
        switch (id) {
        case R.id.download_btn_save:
            if (isCurrentVolumeMounted) {
                mDownloadPath = mCurrentDirPath;
            }
            FileManagerLog.d(TAG, "Select path: " + mDownloadPath);
            Intent okIntent = new Intent();
            okIntent.putExtra("download path", mDownloadPath);
            setResult(RESULT_OK, okIntent);
            finish();
            break;
        case R.id.download_btn_cancel:
            FileManagerLog.d(TAG, "Select path RESULT_CANCELED. ");
            Intent canceledIntent = new Intent();
            setResult(RESULT_CANCELED, canceledIntent);
            finish();
            break;
        case R.id.btn_create_folder:
            if (isCurrentVolumeMounted) {
                removeDialog(CustomDialog.DIALOG_CREATE_FOLDER);
                showDialog(CustomDialog.DIALOG_CREATE_FOLDER);
            }
            break;
        default:
            super.onClick(view);
        }
    }

    /**
     * A callback method to be invoked when showDialog() is called. This is only called once.
     * @param id the type of the dialog to be created
     * @param args arguments
     * @return a dialog
     */
    protected Dialog onCreateDialog(int id, Bundle args) {
        FileManagerLog.d(TAG, "onCreateDialog operation: " + id);
        String name = null;
        File file = null;

        if (id == CustomDialog.DIALOG_CREATE_FOLDER) {
            Object[] createInfo = { mCurrentDirPath };
            mDialog = new CustomDialog(this, CustomDialog.DIALOG_CREATE_FOLDER, createInfo);
            mDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            mDialog.setOnDismissListener(this);
        } else {
            mDialog = null;
        }
        return mDialog;
    }

    /**
     * A callback method to be invoked when an already managed dialog is to be shown
     * @param id the type of the dialog to be shown
     * @param dialog the dialog
     * @param args the dialog arguments provided to showDialog(int, Bundle).
     */
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        FileManagerLog.d(TAG, "onPrepareDialog operation: " + id);
        EditText editText = null;

        if (CustomDialog.DIALOG_CREATE_FOLDER == id) {
            editText = (EditText) dialog.findViewById(R.id.create_folder_name);
            ((CustomDialog) dialog).resetFilter();
            editText.setText("");
            ((CustomDialog) dialog).setCurrentDirPath(mCurrentDirPath);
            FileManagerLog.d(TAG, "create folder in directory: " + mCurrentDirPath);
        }
    }

    /**
     * This method refreshes download view
     */
    private void refreshDownloadView() {
        FileManagerLog.d(TAG, "Refresh download view");
        if (!mFileInfoList.isEmpty()) {
            mSelectedFileName = mFileInfoList.get(mListView.getFirstVisiblePosition())
                    .getFileName();
        }
        showDirectoryContent(mCurrentDirPath);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        refreshDownloadView();
    }

    /**
     * This method gets the custom dialog being created
     * @return dialog
     */
    public Dialog getDialog() {
        return mDialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }
    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
