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

package com.freeme.filemanager.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.drm.DrmManagerClient;
import android.drm.DrmStore;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.PopupMenu.OnMenuItemClickListener;
import com.freeme.filemanager.R;
import com.freeme.filemanager.model.Clipboard;
import com.freeme.filemanager.model.EditUtility;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.FileManagerLog;
import com.freeme.filemanager.util.MountPointHelper;
import com.freeme.filemanager.util.OptionsUtil;
import com.freeme.filemanager.view.CustomDialog;
import com.freeme.filemanager.view.FileManagerBaseActivity;

enum SortBy {
    TYPE, NAME, SIZE, TIME
}

public class FileManagerOperationActivity extends FileManagerBaseActivity implements
        DialogInterface.OnDismissListener, OnMenuItemClickListener {
    private static final String TAG = "FileManagerOperationActivity";

    public static final int PASTE_DONE = 1;
    public static final int LOADING = 2;
    // permission related
    public static final int OPERATION_SUCCESS = 0;
    public static final int DELETE_FAIL = 1;
    public static final int DELETE_DENY = 2;
    public static final int CUT_FAIL = 3;
    public static final int PASTE_FAIL = 4;
    public static final int PASTE_SAME_FOLDER = 5;
    public static final int INSUFFICIENT_MEMORY = 6;
    public static final int SHOW_PROGRESS_DIALOG = 7;
    public static final int SHOW_WAITING_DIALOG = 8;

    public static final String OLD_FILE_PATH = "old_file_path";
    public static final String NEW_FILE_PATH = "new_file_path";
    private static final String SAVED_PATH_KEY = "saved_path";

    private static final String PREF_SORT_BY = "pref_sort_by";
    private static final int BACKGROUND_COLOR = 0xff848284;
    private static final int TAB_BG_COLOR = 0xff848284;
    // 1000 for good user experience
    private static final int TIME_THRESHOLD_FOR_LOADING_DIALOG_MIN = 1000;
    // must be less than ANR 10 seconds
    private static final int TIME_THRESHOLD_FOR_LOADING_DIALOG_MAX = 7000;
    private static final int EMPTY_MSG_DELAY_TIME = 200;
    private static final int TAG_MAX_LENGTH = 15;

    // screen orientation
    private boolean mPortrait = true;

    private View mBarLayout = null;
    private RelativeLayout mEditBar = null;
    private Button mTextSelect = null;
    private PopupMenu mPopupMenu = null;

    private Button mCreateFolderBtnDone = null;
    private Button mCreateFolderBtnCancel = null;
    private EditText mCreateFolderNameEditText = null;
    private boolean mReset = false;
    private Button mRenameBtnDone = null;
    private Button mRenameBtnCancel = null;
    private EditText mRenameNameEditText = null;
    private String mNewFilePath = null;
    private FileInfo mRenameSelectedFileInfoItem = null;
    private boolean mShowRenameExtDialog = false;

    private Dialog mDialog = null;
    private ProgressDialog mWaitingDialog = null;

    private long mWaitingStart = 0;
    private long mWaitingEnd = 0;
    private Handler mHandler = null;

    private FileTask mFileTask = null;
    private boolean mBackgroundThreadRunning = false;

    private boolean mLongClicked = false;
    private MenuItem mLongClickedItem = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FileManagerLog.d(TAG, "onCreate()");

        // get screen orientation
        if (mResources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mPortrait = true;
        } else {
            mPortrait = false;
        }

        // get sort by
        mSortBy = getPrefsSortBy();

        String savedCurPath = null;
        if (savedInstanceState != null) {
            savedCurPath = savedInstanceState.getString(SAVED_PATH_KEY);
        }
        if (savedCurPath != null && mMountPointHelper.isFileRootMount(savedCurPath)) {
            String[] result = savedCurPath.split(MountPointHelper.SEPARATOR);
            int i = 1;
            for (i = 1; i < result.length - 1; i++) { // i=1 to skip first empty string and "/"
                addTab(result[i]);
                addToNavigationList(mCurrentDirPath, result[i + 1], 0);
            }
            addTab(result[i]);
        } else {
            // set default path as /mnt/
            addTab(MountPointHelper.ROOT);
        }
        updateHomeButton();

        registerForContextMenu(mListView);

        // set up handler
        setUpHandler();

        ScannerClient scannerClient = ScannerClient.getInstance();
        scannerClient.init(getApplicationContext());
        scannerClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isBusyForLoadingTask()) {
            invalidateOptionsMenu();
        }
    }

    /**
     * The method updates the list view if need
     */
    protected void updateCurrentDirFileList() {
        File dir = new File(mCurrentDirPath);
        long dirModifiedTime = dir.lastModified();
        if (dirModifiedTime != mCurrentDirModifiedTime) {
            dismissDialog();
            switchToNavigationView(true);
        }
    }

    /**
     * This method is called when the activity is no longer visible
     */
    @Override
    protected void onStop() {
        FileManagerLog.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_PATH_KEY, mCurrentDirPath);
    }

    private void dismissDialog() {
        if (mDialog != null) {
            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }
            mDialog = null;
        }
    }

    /**
     * This method is called when the activity is being destroyed.
     */
    @Override
    protected void onDestroy() {
        FileManagerLog.d(TAG, "onDestroy");
        if (isBusyForFileTask()) {
            mFileTask.cancel(true);
        }

        ScannerClient.getInstance().disconnect();

        if (mWaitingDialog != null) {
            if (mWaitingDialog.isShowing()) {
                mWaitingDialog.dismiss();
            }
            mWaitingDialog = null;
        }

        dismissDialog();
        super.onDestroy();
    }

    @Override
    protected void setMainContentView() {
        setContentView(R.layout.main);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View customActionBarView = inflater.inflate(R.layout.title_layout, null);

            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                            | ActionBar.DISPLAY_SHOW_TITLE);

            mBarLayout = customActionBarView.findViewById(R.id.bar_background);
            mNavigationBar = (HorizontalScrollView) customActionBarView
                    .findViewById(R.id.navigation_bar);
            mTabsHolder = (LinearLayout) customActionBarView.findViewById(R.id.tabs_holder);

            mEditBar = (RelativeLayout) customActionBarView.findViewById(R.id.edit_bar);
            mEditBar.setBackgroundColor(BACKGROUND_COLOR);
            mEditBar.setVisibility(View.INVISIBLE);

            mTextSelect = (Button) customActionBarView.findViewById(R.id.text_select);
            ImageButton doneMenuItem = (ImageButton) customActionBarView
                    .findViewById(R.id.done_menu_item);
            doneMenuItem.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    switchToNavigationView(false);
                }
            });

            actionBar.setCustomView(customActionBarView);
            actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.title_bar_bg));
            actionBar.setSplitBackgroundDrawable(getResources().getDrawable(
                    R.drawable.actionbar_background));
        }
    }

    protected void prepareForMediaEventHandle(String action) {
        if (isBusyForFileTask()) {
            boolean result = mFileTask.cancel(true);
            FileManagerLog.e(TAG, "prepareForMediaEventHandle mFileTask is running:" + result);
        }
    }

    protected void updateViewCompomentStateForMediaEvent(String action, String mountPoint) {
        if (!action.equals(Intent.ACTION_MEDIA_MOUNTED)
                && (mCurrentDirPath.startsWith(mountPoint) || mCurrentDirPath.equals(mountPoint))
                && (mAdapter.getMode() == FileInfoAdapter.MODE_EDIT)) {
            switchToNavigationView(false);
        }
    }

    /**
     * This method creates a new handler and handles incoming messages.
     */
    private void setUpHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                case PASTE_DONE:
                    switchToNavigationView(true);
                    break;
                case LOADING:
                    long waitingTime = 0;
                    FileManagerLog.d(TAG, "Background thread running: " + mBackgroundThreadRunning);

                    if (mBackgroundThreadRunning) {
                        mWaitingEnd = System.currentTimeMillis();
                        waitingTime = mWaitingEnd - mWaitingStart;
                        FileManagerLog.d(TAG, "Waiting time: " + waitingTime);

                        if (waitingTime > TIME_THRESHOLD_FOR_LOADING_DIALOG_MIN) {
                            FileManagerLog.d(TAG, "Wait for background thread shortly");
                            if (waitingTime < TIME_THRESHOLD_FOR_LOADING_DIALOG_MAX) {
                                FileManagerLog.d(TAG, "Wait for background thread and "
                                        + "show a loading dialog");
                                showLoadingDialog();
                            } else {
                                // set false to solve the problem that if UI thread waits too long,
                                // ANR would occur.
                                mBackgroundThreadRunning = false;
                            }
                        }
                        FileManagerLog.d(TAG, "Send handler message again");
                        mHandler.sendEmptyMessageDelayed(0, EMPTY_MSG_DELAY_TIME);
                    } else {
                        FileManagerLog.d(TAG, "Background thread finished and "
                                + "clear handler messages");
                        if (mHandler.hasMessages(0)) {
                            mHandler.removeMessages(0);
                        }
                        dismissLoadingDialog();
                        if (EditUtility.getLastOperation() == EditUtility.DETAILS) {
                            switchToNavigationView(false);
                        } else {
                            switchToNavigationView(true);
                        }
                    }
                    break;
                default:
                    break;
                }
            }
        };
    }

    @Override
    protected void loadFileInfoList(File[] files) {
        for (File file : files) {
            FileInfo fileInfo = null;
            try {
                fileInfo = new FileInfo(this, file, mDrmManagerClient);
            } catch (IllegalArgumentException e) {
                FileManagerLog.d(TAG, "Loading file is null");
                e.printStackTrace();
                continue;
            }
            if (fileInfo != null) {
                //*/added by tyd carl,20120703.[tyd00429949]
                if(!fileInfo.getFileName().startsWith("."))
                    //*/
                mFileInfoList.add(fileInfo);
            }
        }
    }

    /**
     * This method switches edit view to navigation view
     * @param refresh whether to refresh the screen after the switch is done
     */
    private void switchToNavigationView(boolean refresh) {
        FileManagerLog.d(TAG, "Switch to navigation view");
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
        }

        mBarLayout.setVisibility(View.VISIBLE);
        mEditBar.setVisibility(View.INVISIBLE);
        mListView.setFastScrollEnabled(true);
        mAdapter.setMode(FileInfoAdapter.MODE_NORMAL);
        // refresh only when paste or delete operation is performed
        if (refresh) {
            showDirectoryContent(mCurrentDirPath);
        } else {
            saveLastSelection();
            mAdapter.setAllItemsChecked(false);
            // restore the selection in the navigation view
            restoreLastSelection();
        }
        invalidateOptionsMenu();
    }

    /**
     * This method switches navigation view to edit view
     * @param pos the position of the selected item; otherwise, -1
     */
    private void switchToEditView(int pos) {
        FileManagerLog.d(TAG, "Switch to edit view");
        if (isBusyForFileTask()) {
            return;
        }
        mEditBar.setVisibility(View.VISIBLE);
        mBarLayout.setVisibility(View.INVISIBLE);
        mListView.setFastScrollEnabled(false);

        // save the selection in the navigation view
        saveLastSelection();
        mAdapter.setMode(FileInfoAdapter.MODE_EDIT);
        
        // restore the selection in the edit view
        restoreLastSelection();
        
        updateEditBarWidgetState();
        invalidateOptionsMenu();
    }

    /**
     * The method shares the files/folders MMS: support only single files BT: support single and
     * multiple files
     */
    private void share() {
        Intent intent;
        boolean forbidden = false;
        List<String> files = null;
        ArrayList<Parcelable> sendList = new ArrayList<Parcelable>();

        if (mLongClicked) {
            String fileName = getLongClickedName();
            if (fileName != null) {
                files = new ArrayList<String>();
                files.add(fileName);
            } else {
                return;
            }
        } else {
            if (mAdapter.getMode() == FileInfoAdapter.MODE_EDIT) {
                files = mAdapter.getCheckedItemsList();
            } else {
                FileManagerLog.w(TAG, "Maybe dispatch events twice, view mode error.");
                return;
            }
        }
        
        for (String s : files) {
            File sendfile = new File(mCurrentDirPath + MountPointHelper.SEPARATOR + s);
            if (sendfile.isDirectory()) {
                AlertDialog dialog = new AlertDialog.Builder(this).setMessage(
                        R.string.error_info_cant_send_folder).setPositiveButton(R.string.confirm, null).create();
                dialog.show();
                return;
            }
        }
        
        if (files.size() > 1) {
            // send multiple files
            FileManagerLog.d(TAG, "Share multiple files");
            for (String s : files) {
                File file = new File(mCurrentDirPath + MountPointHelper.SEPARATOR + s);

                if (OptionsUtil.isDrmSupported()) {
                    String mimeType = EditUtility.getMimeTypeForFile(this, file);

                    if (mimeType.startsWith("application/vnd.oma.drm")) {
                        if (mDrmManagerClient.checkRightsStatus(file.getPath(),
                                DrmStore.Action.TRANSFER) != DrmStore.RightsStatus.RIGHTS_VALID) {
                            forbidden = true;
                            break;
                        }
                    }
                }
                sendList.add(Uri.fromFile(file));
            }

            if (!forbidden) {
                intent = new Intent();
                intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                intent.setType(EditUtility.getShareMultipleMimeType(this, mDrmManagerClient,
                        mCurrentDirPath, files));
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, sendList);

                try {
                    startActivity(Intent.createChooser(intent, getString(R.string.send_file)));
                } catch (android.content.ActivityNotFoundException e) {
                    FileManagerLog.e(TAG, "Cannot find any activity", e);
                }
            }
        } else {
            // send single file
            FileManagerLog.d(TAG, "Share a single file");
            File file = new File(mCurrentDirPath + MountPointHelper.SEPARATOR + files.get(0));
            String filePath = file.getPath();
            String mimeType = EditUtility.getMimeTypeForFile(this, file);
            boolean isDrmFile = false;

            if (OptionsUtil.isDrmSupported()) {
                if (mimeType.startsWith("application/vnd.oma.drm")) {
                    if (mDrmManagerClient.checkRightsStatus(filePath, DrmStore.Action.TRANSFER) != DrmStore.RightsStatus.RIGHTS_VALID) {
                        forbidden = true;
                    } else {
                        mimeType = mDrmManagerClient.getOriginalMimeType(filePath);
                    }
                }
            }

            if (mimeType == null || mimeType.startsWith("unknown")) {
                mimeType = EditUtility.UNRECOGNIZED_FILE_MIME_TYPE;
            }

            if (!forbidden) {
                intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType(mimeType);
                Uri uri = Uri.fromFile(file);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                FileManagerLog.d(TAG, "Share Uri file: " + uri);
                FileManagerLog.d(TAG, "Share file mimetype: " + mimeType);
                FileManagerLog.d(TAG, "Share file write permission: " + file.canWrite());

                try {
                    startActivity(Intent.createChooser(intent, getString(R.string.send_file)));
                } catch (android.content.ActivityNotFoundException e) {
                    FileManagerLog.e(TAG, "Cannot find any activity", e);
                }
            }
        }

        if (forbidden) {
            removeDialog(CustomDialog.DIALOG_FORBIDDEN);
            showDialog(CustomDialog.DIALOG_FORBIDDEN);
        }
    }

    /**
     * The method gets the long clicked single item and create a list of File object
     * @return a list of File object
     */
    protected List<FileInfo> getLongClickedFile() {
        FileManagerLog.d(TAG, "getLongClickedFile");
        if (mLongClicked && mLongClickedItem != null) {
            List<FileInfo> list = new ArrayList<FileInfo>();
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) mLongClickedItem
                    .getMenuInfo();
            FileManagerLog.d(TAG, "AdapterContextMenuInfo, position: " + info.position);
            if (info.position >= mFileInfoList.size() || info.position < 0) {
                FileManagerLog.e(TAG, "operation events error");
                FileManagerLog.e(TAG, "mFileInfoList.size(): " + mFileInfoList.size());
                return null;
            }
            list.add(mFileInfoList.get(info.position));
            mLongClicked = false;
            mLongClickedItem = null;
            return list;
        }
        return null;
    }

    /**
     * The method gets the long clicked single item FileInfo
     * @return the FileInfo object of the long clicked item
     */
    protected List<FileInfo> getLongClickedItemFileInfo() {
        FileManagerLog.d(TAG, "getLongClickedItemFileInfo");
        if (mLongClicked && mLongClickedItem != null) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) mLongClickedItem
                    .getMenuInfo();
            mLongClicked = false;
            mLongClickedItem = null;
            List<FileInfo> longClickedFileInfo = new ArrayList<FileInfo>();
            FileManagerLog.d(TAG, "AdapterContextMenuInfo, position: " + info.position);
            if (info.position >= mFileInfoList.size() || info.position < 0) {
                FileManagerLog.e(TAG, "Operation events error");
                FileManagerLog.e(TAG, "mFileInfoList.size(): " + mFileInfoList.size());
                return null;
            }
            longClickedFileInfo.add(mFileInfoList.get(info.position));
            return longClickedFileInfo;
        }
        return null;
    }

    /**
     * The method gets the long clicked single item name
     * @return the name of the long clicked item
     */
    protected String getLongClickedName() {
        FileManagerLog.d(TAG, "getLongClickedItemFileInfo");
        if (mLongClicked && mLongClickedItem != null) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) mLongClickedItem
                    .getMenuInfo();
            mLongClicked = false;
            mLongClickedItem = null;
            FileManagerLog.d(TAG, "AdapterContextMenuInfo, position: " + info.position);
            if (info.position >= mFileInfoList.size() || info.position < 0) {
                FileManagerLog.e(TAG, "Operation events error");
                FileManagerLog.e(TAG, "mFileInfoList.size(): " + mFileInfoList.size());
                return null;
            }
            return mFileInfoList.get(info.position).getFileName();
        }
        return null;
    }

    private String getSelectedFileName() {
        if (mLongClicked) {
            return getLongClickedName();
        } else {
            return mAdapter.getCheckedItemsList().get(0);
        }
    }

    private FileInfo getSeletedItemFileInfo() {
        List<FileInfo> fileInfoList = null;
        if (mLongClicked) {
            fileInfoList = getLongClickedItemFileInfo();
        } else {
            fileInfoList = mAdapter.getCheckedFileInfos();
        }
        if (fileInfoList != null && !fileInfoList.isEmpty()) {
            return fileInfoList.get(0);
        } else {
            return null;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (isBusyForLoadingTask()) {
            return;
        }
        FileManagerLog.e(TAG, "Selected position: " + position);
        if (mAdapter.getMode() == FileInfoAdapter.MODE_NORMAL) {
            if (position >= mFileInfoList.size() || position < 0) {
                FileManagerLog.e(TAG, "mFileInfoList.size(): " + mFileInfoList.size());
                return;
            }

            FileInfo selecteItemFileInfo = mAdapter.getItem(position);

            if (selecteItemFileInfo.isDirectory()) {
                String focusedItemFileName = selecteItemFileInfo.getFileName();
                int top = view.getTop();
                FileManagerLog.v(TAG, "fromTop = " + top);
                addToNavigationList(mCurrentDirPath, focusedItemFileName, top);

                addTab(focusedItemFileName);
                updateHomeButton();
                showDirectoryContent(mCurrentDirPath);
            } else {
                // open file here
                boolean canOpen = true;
                String mimeType = selecteItemFileInfo.getFileMimeType();

                if (OptionsUtil.isDrmSupported()) {
                    if (mimeType.startsWith("application/vnd.oma.drm")) {
                        mimeType = selecteItemFileInfo.getFileOriginalMimeType();

                        if (mimeType == null || (mimeType != null && mimeType.length() == 0)) {
                            canOpen = false;
                            EditUtility.showToast(FileManagerOperationActivity.this,
                                    R.string.support_fail);
                        } else {
                            int action = selecteItemFileInfo.getFileDrmActionId();
                            int rightsStatus = 0;
                            //int rightsStatus = mDrmManagerClient.checkRightsStatusForTap(
                              //      selecteItemFileInfo.getFilePath(), action);
                            if (rightsStatus == DrmStore.RightsStatus.RIGHTS_INVALID) {
                                FileManagerLog.d(TAG, "Show license acquisition dialog");
                            }
                        }
                    }
                }

                if (canOpen) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.fromFile(selecteItemFileInfo.getFile());
                    FileManagerLog.d(TAG, "Open uri file: " + uri);
                    intent.setDataAndType(uri, mimeType);

                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) {
                        EditUtility.showToast(this, R.string.msg_unable_open_file);
                        FileManagerLog.w(TAG, "Cannot open file: "
                                + selecteItemFileInfo.getFilePath());
                    }
                }
            }
        } else {
            FileManagerLog.w(TAG, "onItemClick-" + position);
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.edit_checkbox);
            if (checkBox.isChecked()) {
                checkBox.setChecked(false);
                mAdapter.setChecked(position, false);
            } else {
                checkBox.setChecked(true);
                mAdapter.setChecked(position, true);
            }
            updateEditBarWidgetState();
            invalidateOptionsMenu();
        }
    }

    private void updateEditBarWidgetState() {
        int selectedCount = mAdapter.getCheckedItemsCount();
        String selected = getResources().getString(R.string.selected);
        selected = "" + selectedCount + " " + selected;
        mTextSelect.setText(selected);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        FileManagerLog.d(TAG, "onClick: " + id);

        if (mAdapter.getMode() == FileInfoAdapter.MODE_EDIT) {
            if (mMountPointHelper.isFileRootMount(mCurrentDirPath)) {
                updateEditBarWidgetState();
                invalidateOptionsMenu();
            }
            return;
        }
        super.onClick(view);
    }

    /**
     * Prepare the screen's standard options menu to be displayed.
     * @param menu the options menu
     * @return return true for the menu to be displayed
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        FileManagerLog.d(TAG, "onPrepareOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        menu.clear();

        if (mAdapter.getMode() == FileInfoAdapter.MODE_NORMAL) {
            if (mCurrentDirPath.equals(MountPointHelper.ROOT_PATH)) {
                return true;
            }
            inflater.inflate(R.menu.navigation_view_menu, menu);
            if (mCurrentDirPath.equals(MountPointHelper.ROOT_PATH) || !new File(mCurrentDirPath).canWrite()) {
                menu.findItem(R.id.edit).setEnabled(false);
                menu.findItem(R.id.sort).setEnabled(false);
                menu.findItem(R.id.create_folder).setEnabled(false);
            } else {
                menu.findItem(R.id.edit).setEnabled(true);
                menu.findItem(R.id.create_folder).setEnabled(true);
            }

            if (mFileInfoList.isEmpty()) {
                menu.findItem(R.id.sort).setEnabled(false);
            } else {
                menu.findItem(R.id.sort).setEnabled(true);
            }
        } else {
            // edit view
            inflater.inflate(R.menu.edit_view_menu, menu);
            int selectedCount = mAdapter.getCheckedItemsCount();

            // enable(disable) copy, cut, and delete icon
            if (selectedCount == 0) {
                menu.findItem(R.id.copy).setEnabled(false);
                menu.findItem(R.id.delete).setEnabled(false);
            } else if (selectedCount == 1) {
                File f = new File(mCurrentDirPath + MountPointHelper.SEPARATOR
                        + mAdapter.getCheckedItemsList().get(0));
                if (f.canWrite()) {
                    if (OptionsUtil.isDrmSupported() && f.isFile()) {
                        // no drm file can be copied, so disable copy icon
                        String ext = EditUtility.getFileExtension(f.getName());
                        if ((ext != null && ext.equalsIgnoreCase(EditUtility.EXT_DRM_CONTENT))) {
                            menu.findItem(R.id.copy).setEnabled(false);
                        }
                    } else {
                        menu.findItem(R.id.copy).setEnabled(true);
                    }
                    menu.findItem(R.id.delete).setEnabled(true);
                } else {
                    menu.findItem(R.id.copy).setEnabled(false);
                    menu.findItem(R.id.delete).setEnabled(false);
                }
            } else {
                menu.findItem(R.id.copy).setEnabled(true);
                menu.findItem(R.id.delete).setEnabled(true);
            }

            if (mFileInfoList.isEmpty()) {
                menu.findItem(R.id.select_all_menu).setEnabled(false);
                menu.findItem(R.id.deselect_all_menu).setEnabled(false);
            } else if (0 == selectedCount) {
                menu.findItem(R.id.select_all_menu).setEnabled(true);
                menu.findItem(R.id.deselect_all_menu).setEnabled(false);
            } else if (mFileInfoList.size() == selectedCount) {
                menu.findItem(R.id.select_all_menu).setEnabled(false);
                menu.findItem(R.id.deselect_all_menu).setEnabled(true);
            } else {
                menu.findItem(R.id.select_all_menu).setEnabled(true);
                menu.findItem(R.id.deselect_all_menu).setEnabled(true);
            }
        }
        return true;
    }

    /**
     * A callback method to be invoked when an item in the options menu is selected
     * @param item the menu item that was selected
     * @return true if the callback consumed the click, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FileManagerLog.d(TAG, "onOptionsItemSelected: " + item.getItemId());
        if (isBusyForFileTask()) {
            // solve rare case: user presses "paste" twice fast enough
            return true;
        }
        if (mAdapter.getMode() == FileInfoAdapter.MODE_NORMAL) {
            switch (item.getItemId()) {
            case R.id.create_folder:
                removeDialog(CustomDialog.DIALOG_CREATE_FOLDER);
                showDialog(CustomDialog.DIALOG_CREATE_FOLDER);
                break;

            case R.id.edit:
                switchToEditView(-1); // -1 for nothing is checked automatically
                break;

            case R.id.sort:
                removeDialog(CustomDialog.DIALOG_SORT);
                showDialog(CustomDialog.DIALOG_SORT);
                break;

            default:
                return super.onOptionsItemSelected(item);
            }
        } else {
            // edit view
            switch (item.getItemId()) {
            case R.id.popup_menu:
                if (mPopupMenu == null) {
                    View popupMenuBaseLine = (View) findViewById(R.id.popup_menu_base_line);
                    mPopupMenu = constructPopupMenu(popupMenuBaseLine);
                }
                if (mPopupMenu != null) {
                    updatePopupMenuItemState();
                    mPopupMenu.show();
                }
                break;

            case R.id.copy:
                EditUtility.copy(this, mAdapter.getCheckedFileInfos());
                EditUtility.setLastOperation(EditUtility.COPY);
                switchToNavigationView(false);
                break;

            case R.id.delete:
                Bundle bundle = new Bundle();
                if (mAdapter.getCheckedItemsCount() == 1) {
                    bundle.putBoolean("Single", true);
                } else {
                    bundle.putBoolean("Single", false);
                }
                removeDialog(CustomDialog.DIALOG_MULTI_DELETE);
                showDialog(CustomDialog.DIALOG_MULTI_DELETE, bundle);
                break;

            case R.id.select_all_menu:
                mAdapter.setAllItemsChecked(true);
                updateEditBarWidgetState();
                invalidateOptionsMenu();
                break;

            case R.id.deselect_all_menu:
                mAdapter.setAllItemsChecked(false);
                updateEditBarWidgetState();
                invalidateOptionsMenu();
                break;

            default:
                return super.onOptionsItemSelected(item);
            }
        }

        return true;
    }

    /**
     * The method creates an alert delete dialog
     * @param args argument, the boolean value who will indicates whether the selected files just
     *            only one. The prompt message will be different.
     * @return a dialog
     */
    protected AlertDialog alertDeleteDialog() {
        FileManagerLog.d(TAG, "create alertDeleteDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String alertMsg = mResources.getString(R.string.alert_delete_single);

        builder.setTitle(R.string.delete).setIcon(R.drawable.ic_dialog_alert_holo_light)
                .setMessage(alertMsg).setPositiveButton(mResources.getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                FileManagerLog.d(TAG, "alertDeleteDialog-pressed OK");
                                if (isBusyForFileTask()) {
                                    // if a task is executing, do nothing
                                    return;
                                }
                                List<FileInfo> deletedFileInfo = getLongClickedItemFileInfo();
                                if (deletedFileInfo != null && !deletedFileInfo.isEmpty()) {
                                    // delete selected items
                                    mFileTask = new FileTask(deletedFileInfo, EditUtility.DELETE);
                                    mFileTask.execute();
                                }
                            }
                        }).setNegativeButton(mResources.getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        return builder.create();
    }

    /**
     * The method creates an alert delete dialog
     * @param args argument, the boolean value who will indicates whether the selected files just
     *            only one. The prompt message will be different.
     * @return a dialog
     */
    protected AlertDialog alertMultiDeleteDialog(Bundle args) {
        FileManagerLog.d(TAG, "create alertMultiDeleteDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String alertMsg = null;
        if (args.getBoolean("Single")) {
            alertMsg = mResources.getString(R.string.alert_delete_single);
        } else {
            alertMsg = mResources.getString(R.string.alert_delete_multiple);
        }

        builder.setTitle(R.string.delete).setIcon(R.drawable.ic_dialog_alert_holo_light)
                .setMessage(alertMsg).setPositiveButton(mResources.getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                FileManagerLog.d(TAG, "alertMultiDeleteDialog-pressed OK");
                                if (isBusyForFileTask()) {
                                    // if a task is executing, do nothing
                                    return;
                                }
                                List<FileInfo> fileInfoList = mAdapter.getCheckedFileInfos();
                                if (fileInfoList == null || fileInfoList.isEmpty()) {
                                    FileManagerLog.w(TAG, "Empty deleted file info list.");
                                } else {
                                    // delete selected items
                                    mFileTask = new FileTask(fileInfoList, EditUtility.DELETE);
                                    mFileTask.execute();
                                }
                            }
                        }).setNegativeButton(mResources.getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        return builder.create();
    }

    /**
     * The method creates an alert forward forbidden dialog
     * @return a dialog
     */
    protected AlertDialog alertForbiddenDialog() {
        FileManagerLog.d(TAG, "Show alertForbiddenDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(com.mediatek.internal.R.string.drm_forwardforbidden_title).setIcon(
                R.drawable.ic_dialog_alert_holo_light).setMessage(
                com.mediatek.internal.R.string.drm_forwardforbidden_message).setCancelable(false)
                .setPositiveButton(mResources.getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
        return builder.create();
    }

    /**
     * The method creates an alert sort dialog
     * @return a dialog
     */
    protected AlertDialog alertSortDialog() {
        FileManagerLog.d(TAG, "Show alertSortDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(mResources.getString(R.string.sort_by));
        builder.setSingleChoiceItems(R.array.sort_by, mSortBy,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (id != mSortBy) {
                            if (id == SortBy.TYPE.ordinal()) {
                                setPrefsSortBy(SortBy.TYPE.ordinal());
                            } else if (id == SortBy.NAME.ordinal()) {
                                setPrefsSortBy(SortBy.NAME.ordinal());
                            } else if (id == SortBy.SIZE.ordinal()) {
                                setPrefsSortBy(SortBy.SIZE.ordinal());
                            } else {
                                setPrefsSortBy(SortBy.TIME.ordinal());
                            }
                            dialog.dismiss();
                            sortFileInfoList();
                        }
                    }
                }).setNegativeButton(mResources.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        return builder.create();
    }

    /**
     * This method switches edit view to navigation view
     * @param refresh whether to refresh the screen after the switch is done
     */
    private void sortFileInfoList() {
        FileManagerLog.d(TAG, "Start sortFileInfoList()");
        saveLastSelection();

        // refresh only when paste or delete operation is performed
        Collections.sort(mFileInfoList, new FileInfoComparator(mSortBy));
        ((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();

        // restore the selection in the navigation view
        restoreLastSelection();
        FileManagerLog.d(TAG, "End sortFileInfoList()");
    }

    protected AlertDialog alertCreateFolderDialog() {
        FileManagerLog.d(TAG, "Show alertCreateFolderDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.create_folder_edit_text, null);
        mCreateFolderNameEditText = (EditText) view.findViewById(R.id.create_folder_name);

        builder.setTitle(R.string.new_folder).setView(view).setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (EditUtility.getFreeSpace(mCurrentDirPath) > 0) {
                            String fileName = mCreateFolderNameEditText.getText().toString();

                            if (EditUtility
                                    .isValidName(FileManagerOperationActivity.this, fileName)) {
                                if (EditUtility.createFolder(FileManagerOperationActivity.this,
                                        mCurrentDirPath + MountPointHelper.SEPARATOR + fileName)) {
                                    FileManagerLog.d(TAG, "Create new folder successfully: "
                                            + mCurrentDirPath + MountPointHelper.SEPARATOR + fileName);
                                    // send broadcast to notify other applications for create folder
                                    EditUtility.notifyUpdates(FileManagerOperationActivity.this,
                                            Intent.ACTION_MEDIA_MOUNTED, new File(mCurrentDirPath
                                                    + MountPointHelper.SEPARATOR + fileName));
                                    switchToNavigationView(true);
                                }
                            }
                        } else {
                            // show insufficient memory message
                            dialog.dismiss();
                            EditUtility.showToast(FileManagerOperationActivity.this,
                                    R.string.insufficient_memory);
                        }
                    }
                }).setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog createFolderAlertDialog = builder.create();
        createFolderAlertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        setTextChangedCallbackForCreateFolder();
        return createFolderAlertDialog;
    }

    /**
     * This method register callback and set filter to Edit, in order to make sure that user input
     * is legal. The input can't be illegal filename and can't be too long.
     */
    private void setTextChangedCallbackForCreateFolder() {
        mReset = true;
        setEditTextFilter(mCreateFolderNameEditText);
        mCreateFolderNameEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().length() <= 0 || s.toString().matches(".*[/\\\\:*?\"<>|].*")) {
                    // characters not allowed
                    if (s.toString().matches(".*[/\\\\:*?\"<>|].*")) {
                        EditUtility.showToast(FileManagerOperationActivity.this,
                                R.string.invalid_char_prompt);
                    }
                    mCreateFolderBtnDone.setEnabled(false);
                } else {
                    mCreateFolderBtnDone.setEnabled(true);
                }
            }
        });
    }

    /**
     * This method is used to set filter to EditText which is used for user entering filename. This
     * filter will ensure that the inputed filename wouldn't be too long. If so, the inputed info
     * would be rejected.
     * @param edit The EditText for filter to be registered.
     */
    private void setEditTextFilter(final EditText edit) {
        InputFilter filter = new InputFilter() {

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                    int dstart, int dend) {
                int oldSize = 0;
                int sourceSize = 0;

                // original
                String name = edit.getText().toString();
                oldSize = name.getBytes().length;
                FileManagerLog.d(TAG, "Original file name: " + name);
                FileManagerLog.d(TAG, "Original size: " + oldSize);

                // new add sequences
                String seq = source.toString();
                sourceSize = seq.getBytes().length;
                FileManagerLog.d(TAG, "Source: " + seq);
                FileManagerLog.d(TAG, "source size: " + sourceSize);

                if (mReset) {
                    mReset = false;
                    return null;
                } else {
                    int newSize = oldSize + sourceSize;
                    FileManagerLog.d(TAG, "New size: " + newSize);

                    if (newSize > EditUtility.FILENAME_MAX_LENGTH) {
                        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        boolean hasVibrator = vibrator.hasVibrator();
                        if (hasVibrator) {
                            vibrator.vibrate(new long[] { 100, 100 }, -1);
                        }
                        return "";
                    } else {
                        return null;
                    }
                }
            }
        };
        edit.setFilters(new InputFilter[] { filter });
    }

    protected AlertDialog alertRenameDialog() {
        FileManagerLog.d(TAG, "Show alertRenameDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.rename_edit_text, null);
        mRenameNameEditText = (EditText) view.findViewById(R.id.rename_text);

        builder.setTitle(R.string.rename).setView(view).setPositiveButton(
                mResources.getString(R.string.done), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String fileName = mRenameNameEditText.getText().toString();
                        if (null == mRenameSelectedFileInfoItem) {
                            FileManagerLog.w(TAG, "mRenameSelectedFileInfoItem is null.");
                            return;
                        }

                        String newFilePath = mCurrentDirPath + MountPointHelper.SEPARATOR + fileName;
                        //*/ Add by xiaocui 2012-08-02 for:[tyd00436129 ] rename datebase of music 
                        String oldFilePath = mRenameSelectedFileInfoItem.getFilePath();
                        //*/
                        File newFile = new File(newFilePath);
                        File oldFile = mRenameSelectedFileInfoItem.getFile();
                        if (EditUtility.isValidName(FileManagerOperationActivity.this, fileName)) {
                            if (newFile.exists()) {
                                // show a toast notification if renaming a file/folder to the same
                                // name
                                StringBuffer msg = new StringBuffer();
                                msg.append(fileName).append(" ").append(
                                        getResources().getString(R.string.already_exists));
                                EditUtility.showToast(FileManagerOperationActivity.this, msg
                                        .toString());
                            } else {
                                String origFileExtension = EditUtility
                                        .getFileExtension(mRenameSelectedFileInfoItem.getFileName());
                                String newFileExtension = EditUtility.getFileExtension(fileName);

                                if (oldFile.isFile()
                                        && ((null == origFileExtension && null != newFileExtension)
                                                || (null != origFileExtension && null == newFileExtension) || ((null != origFileExtension && null != newFileExtension) && !newFileExtension
                                                .equals(origFileExtension)))) {
                                    Bundle data = new Bundle();
                                    data.putString(FileManagerOperationActivity.NEW_FILE_PATH,
                                            newFilePath);
                                    mShowRenameExtDialog = true;
                                    showDialog(CustomDialog.DIALOG_RENAME_EXTENSION, data);
                                } else {
                                    if (mRenameSelectedFileInfoItem.rename(
                                            FileManagerOperationActivity.this, newFilePath,
                                            ScannerClient.getInstance())) {
                                        updateListForRename();
                                        //*/ Add by xiaocui 2012-08-02 for:[tyd00436129 ] rename datebase of music 
                                        EditUtility.renameToDatabase(getApplicationContext(), oldFilePath, newFilePath, fileName);
                                        //*/
                                    } else {
                                        EditUtility.showToast(FileManagerOperationActivity.this,
                                                R.string.msg_change_name);
                                    }
                                }
                            }
                        }
                    }
                }).setNegativeButton(mResources.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog renameAlertDialog = builder.create();
        renameAlertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        setTextChangedCallbackForRename();
        return renameAlertDialog;
    }

    private void updateListForRename() {
        Collections.sort(mFileInfoList, new FileInfoComparator(mSortBy));
        int position = mFileInfoList.indexOf(mRenameSelectedFileInfoItem);
        switchToNavigationView(false);
        mListView.setSelection(position);
    }

    /**
     * This method register callback and set filter to Edit, in order to make sure that user input
     * is legal. The input can't be illegal filename and can't be too long.
     */
    private void setTextChangedCallbackForRename() {
        mReset = true;
        setEditTextFilter(mRenameNameEditText);
        mRenameNameEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().length() <= 0 || s.toString().matches(".*[/\\\\:*?\"<>|].*")) { // characters
                    // not
                    // allowed
                    if (s.toString().matches(".*[/\\\\:*?\"<>|].*")) {
                        EditUtility.showToast(FileManagerOperationActivity.this,
                                R.string.invalid_char_prompt);
                    }
                    mRenameBtnDone.setEnabled(false);
                } else {
                    mRenameBtnDone.setEnabled(true);
                }
            }
        });
    }

    void renameForExtension() {
        if (mNewFilePath != null) {
            mRenameSelectedFileInfoItem.rename(this, mNewFilePath, ScannerClient.getInstance());
        }
    }

    /**
     * The method creates an alert delete dialog
     * @param args argument, the boolean value who will indicates whether the selected files just
     *            only one. The prompt message will be different.
     * @return a dialog
     */
    protected AlertDialog alertRenameExtensionDialog() {
        FileManagerLog.d(TAG, "Show alertRenameExtensionDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String alertMsg = mResources.getString(R.string.msg_rename_ext);

        builder.setTitle(R.string.confirm_rename).setIcon(R.drawable.ic_dialog_alert_holo_light)
                .setMessage(alertMsg).setPositiveButton(mResources.getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                renameForExtension();
                                updateListForRename();
                            }
                        }).setNegativeButton(mResources.getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                switchToNavigationView(false);
                            }
                        });
        return builder.create();
    }

    /**
     * This method sets the sorting type in the preference
     * @param sort the sorting type
     */
    private void setPrefsSortBy(int sort) {
        mSortBy = sort;
        Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putInt(PREF_SORT_BY, sort);
        editor.commit();
    }

    /**
     * This method gets the sorting type from the preference
     * @return the sorting type
     */
    private int getPrefsSortBy() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        return prefs.getInt(PREF_SORT_BY, 0);
    }

    /**
     * A callback method to be invoked when long click on an item
     * @param menu The context menu that is being built
     * @param v The view for which the context menu is being built
     * @param menuInfo Extra information about the item for which the context menu should be shown.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        FileManagerLog.d(TAG, "onCreateContextMenu");
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        FileManagerLog.d(TAG, "AdapterContextMenuInfo, position: " + info.position);
        if (info.position >= mFileInfoList.size() || info.position < 0) {
            FileManagerLog.e(TAG, "ContextMenu events error");
            FileManagerLog.e(TAG, "mFileInfoList.size(): " + mFileInfoList.size());
            return;
        }

        menu.setHeaderTitle(mFileInfoList.get(info.position).getFileDescription());
        
        FileInfo fileInfo = mFileInfoList.get(info.position);
        File file = fileInfo.getFile();
        String ext = EditUtility.getFileExtension(fileInfo.getFileName());

        if (file.canWrite() && !mMountPointHelper.isMountPointPath(fileInfo.getFilePath())) {
            if (ext != null && OptionsUtil.isDrmSupported()) {
                if (!(ext.equalsIgnoreCase(EditUtility.EXT_DRM_CONTENT))) {
                    menu.add(0, R.id.copy, 0, R.string.copy);
                }
            } else {
                menu.add(0, R.id.copy, 0, R.string.copy);
            }
            menu.add(0, R.id.cut, 0, R.string.cut);
            menu.add(0, R.id.delete, 0, R.string.delete);
            menu.add(0, R.id.rename, 0, R.string.rename);
        }

        if (!file.isDirectory()) {
            if (OptionsUtil.isDrmSupported()) {
                if (ext != null && ext.equalsIgnoreCase(EditUtility.EXT_DRM_CONTENT)) {
                    if (mDrmManagerClient.checkRightsStatus(file.getPath(),
                            DrmStore.Action.TRANSFER) == DrmStore.RightsStatus.RIGHTS_VALID) {
                        menu.add(0, R.id.share, 0, R.string.share);
                    }
                } else {
                    menu.add(0, R.id.share, 0, R.string.share);
                }
            } else {
                menu.add(0, R.id.share, 0, R.string.share);
            }
        }

        menu.add(0, R.id.details, 0, R.string.details);

        if (OptionsUtil.isDrmSupported()) {
            if (file.isFile()) {
                if (mDrmManagerClient.getDrmObjectType(file.getPath(), null) != DrmStore.DrmObjectType.UNKNOWN) {
                    String mimeType = mDrmManagerClient.getOriginalMimeType(file.getPath());

                    if (mimeType != null && mimeType.trim().length() != 0) {
                        FileManagerLog.d(TAG, "Context menu dcf mimetype: " + mimeType);
                        menu.add(0, R.id.protection_info, 0,
                                com.mediatek.internal.R.string.drm_protectioninfo_title);
                    }
                }
            }
        }
    }

    /**
     * A callback method to be invoked when an item in the context menu is selected
     * @param item The context menu item that was selected.
     * @return true if the callback consumed the click, false otherwise
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = null;
        mLongClickedItem = item;
        mLongClicked = true;
        switch (item.getItemId()) {
        case R.id.copy:
            FileManagerLog.d(TAG, "onContextItemSelected: copy");
            List<FileInfo> copiedFileInfo = getLongClickedFile();
            if (copiedFileInfo != null) {
                EditUtility.copy(this, copiedFileInfo);
                EditUtility.setLastOperation(EditUtility.COPY);
                switchToNavigationView(false);
            }
            break;

        case R.id.cut:
            FileManagerLog.d(TAG, "onContextItemSelected: cut");
            List<FileInfo> cuttedFileInfo = getLongClickedFile();
            if (cuttedFileInfo != null) {
                SparseBooleanArray grayOutItem = new SparseBooleanArray();
                info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                //*/modified by carl.remark by tyd john 20120725 for build error
                //File fl=cuttedFileInfo.get(0);//added
               // grayOutItem.put(fl.hashCode(), true);//
                 grayOutItem.put(info.position, true);
                //*/
                EditUtility.cut(this, cuttedFileInfo);
                EditUtility.setLastOperation(EditUtility.CUT);
                switchToNavigationView(false);
            }
            break;

        case R.id.delete:
            FileManagerLog.d(TAG, "onContextItemSelected: delete");
            removeDialog(CustomDialog.DIALOG_DELETE);
            showDialog(CustomDialog.DIALOG_DELETE);
            break;

        case R.id.rename:
            FileManagerLog.d(TAG, "onContextItemSelected: rename");
            removeDialog(CustomDialog.DIALOG_RENAME);
            showDialog(CustomDialog.DIALOG_RENAME);
            EditUtility.setLastOperation(EditUtility.RENAME);
            break;

        case R.id.share:
            FileManagerLog.d(TAG, "onContextItemSelected: share");
            share();
            EditUtility.setLastOperation(EditUtility.SHARE);
            break;

        case R.id.details:
            FileManagerLog.d(TAG, "onContextItemSelected: details");
            List<FileInfo> detailsFileInfo = getLongClickedFile();
            if (detailsFileInfo != null) {
                FileInfo fileInfo = detailsFileInfo.get(0);
                if (fileInfo != null) {
                    FileManagerLog.d(TAG, "onContextItemSelected-details:"
                            + fileInfo.getFileDescription());
                    mFileTask = new FileTask(fileInfo, EditUtility.DETAILS);
                    mFileTask.execute();
                }
            }
            break;

        case R.id.protection_info:
            FileManagerLog.d(TAG, "onContextItemSelected: protection info");
            List<FileInfo> fileInfos = getLongClickedFile();
            if (fileInfos != null && !fileInfos.isEmpty()) {
                // calling framework to show a protection info dialog
                //mDrmManagerClient.showProtectionInfoDialog(this, fileInfos.get(0).getFilePath()); 
                EditUtility.setLastOperation(EditUtility.PROTECTION_INFO);
            }
            break;

        default:
            mLongClicked = false;
            mLongClickedItem = null;
            FileManagerLog.d(TAG, "onContextItemSelected: default");
            return super.onContextItemSelected(item);
        }
        return true;
    }

    /**
     * A callback method to be invoked when showDialog() is called. This is only called once.
     * @param id the type of the dialog to be created
     * @param args arguments, dor delete dialog
     * @return a dialog
     */
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        FileManagerLog.d(TAG, "onCreateDialog operation: " + id);
        String name = null;
        File file = null;

        switch (id) {
        case CustomDialog.DIALOG_CREATE_FOLDER:
            mDialog = alertCreateFolderDialog();
            break;

        case CustomDialog.DIALOG_DELETE:
            mDialog = alertDeleteDialog();
            mDialog.setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface arg0) {
                    if (mLongClicked) {
                        mLongClicked = false;
                        mLongClickedItem = null;
                    }
                }
            });
            break;

        case CustomDialog.DIALOG_MULTI_DELETE:
            mDialog = alertMultiDeleteDialog(args);
            break;

        case CustomDialog.DIALOG_RENAME:
            mDialog = alertRenameDialog();
            mDialog.setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (!mShowRenameExtDialog) {
                        mRenameSelectedFileInfoItem = null;
                    }
                    mShowRenameExtDialog = false;
                }
            });
            break;

        case CustomDialog.DIALOG_SORT:
            mDialog = alertSortDialog();
            break;

        case CustomDialog.DIALOG_FORBIDDEN:
            mDialog = alertForbiddenDialog();
            mDialog.setOnDismissListener(this);
            break;
        case CustomDialog.DIALOG_RENAME_EXTENSION:
            mDialog = alertRenameExtensionDialog();
            mDialog.setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    mNewFilePath = null;
                    mRenameSelectedFileInfoItem = null;
                }
            });
            break;
        default:
            mDialog = null;
            break;
        }
        return mDialog;
    }

    /**
     * A callback method to be invoked when an already managed dialog is to be shown
     * @param id the type of the dialog to be shown
     * @param dialog the dialog
     * @param args the dialog arguments provided to showDialog(int, Bundle).
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        FileManagerLog.d(TAG, "onPrepareDialog operation: " + id);
        EditText editText = null;
        String name = null;

        switch (id) {
        case CustomDialog.DIALOG_CREATE_FOLDER:
            mCreateFolderBtnDone = ((AlertDialog) dialog)
                    .getButton(DialogInterface.BUTTON_POSITIVE);
            mCreateFolderBtnDone.setEnabled(false);
            mCreateFolderBtnCancel = ((AlertDialog) dialog)
                    .getButton(DialogInterface.BUTTON_NEGATIVE);
            mReset = true;
            mCreateFolderNameEditText.setText("");
            break;
        case CustomDialog.DIALOG_MULTI_DELETE:
            if (args.getBoolean("Single")) {
                ((AlertDialog) dialog).setMessage(mResources
                        .getString(R.string.alert_delete_single));
            } else {
                ((AlertDialog) dialog).setMessage(mResources
                        .getString(R.string.alert_delete_multiple));
            }
            break;
        case CustomDialog.DIALOG_RENAME:
            mRenameBtnDone = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
            mRenameBtnDone.setEnabled(false);
            mRenameBtnCancel = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);

            mRenameSelectedFileInfoItem = getSeletedItemFileInfo();
            if (mRenameSelectedFileInfoItem == null) {
                break;
            }
            name = mRenameSelectedFileInfoItem.getFileName();
            mReset = true;
            mRenameNameEditText.setText(name);
            File oldFile = mRenameSelectedFileInfoItem.getFile();
            String fileExtension = EditUtility.getFileExtension(name);
            int selection = name.length();
            if (oldFile != null && oldFile.isFile() && fileExtension != null) {
                selection = selection - fileExtension.length() - 1;
            }
            mRenameNameEditText.setSelection(selection);
            FileManagerLog.d(TAG, "onPrepareDialog rename: " + name);
            break;
        case CustomDialog.DIALOG_RENAME_EXTENSION:
            mNewFilePath = args.getString(NEW_FILE_PATH);
            break;
        default:
            break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mAdapter.getMode() == FileInfoAdapter.MODE_EDIT) {
                switchToNavigationView(false);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * A callback method to be invoked when the device configuration changes
     * @param newConfig the new device configuration.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mPortrait = true;
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mPortrait = false;
        }
        if (!newConfig.locale.equals(mLocale)) {
            mPopupMenu = null;
            if (mAdapter.getMode() == FileInfoAdapter.MODE_EDIT) {
                updateEditBarWidgetState();
            }
        }
        super.onConfigurationChanged(newConfig);
    }

    /*
     * Just for Details Dialog
     * @see
     * android.content.DialogInterface.OnDismissListener#onDismiss(android.content.DialogInterface)
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        FileManagerLog.d(TAG, "onDismiss dialog");
        mWaitingStart = System.currentTimeMillis();

        FileManagerLog.d(TAG, "onDismiss dialog checking background thread: "
                + mBackgroundThreadRunning);
        if (mBackgroundThreadRunning) {
            // wait until background thread finishes the cleaning step
            // this avoids switchToNavigationView() gets called before the cleaning step
            // (e.g. cleaning incomplete copied file) send msg to itself
            if (mFileTask != null && !mFileTask.isCancelled()) {
                boolean cancelResult = mFileTask.cancel(true);
                FileManagerLog.d(TAG, "Cancel AsyncTsk when dismiss dialog: " + cancelResult);
            }
            Message.obtain(mHandler, LOADING).sendToTarget();
        }
    }

    /**
     * This method gets the custom dialog being created
     * @return dialog
     */
    public Dialog getDialog() {
        return mDialog;
    }

    /**
     * This method gets the DrmManagerClient instance
     * @return DrmManagerClient instance
     */
    public DrmManagerClient getDrmManagerClient() {
        return mDrmManagerClient;
    }

    private PopupMenu constructPopupMenu(View anchorView) {
        final PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.inflate(R.menu.popup_menu);
        popupMenu.setOnMenuItemClickListener(this);
        return popupMenu;
    }

    void updatePopupMenuItemState() {
        final Menu menu = mPopupMenu.getMenu();
        int selectedCount = mAdapter.getCheckedItemsCount();

        // enable(disable) copy, cut, and delete icon
        if (selectedCount == 0) {
            menu.findItem(R.id.cut).setEnabled(false);
        } else if (selectedCount == 1) {
            File f = new File(mCurrentDirPath + MountPointHelper.SEPARATOR + mAdapter.getCheckedItemsList().get(0));
            if (f.canWrite()) {
                menu.findItem(R.id.cut).setEnabled(true);
            } else {
                menu.findItem(R.id.cut).setEnabled(false);
            }
        } else {
            menu.findItem(R.id.cut).setEnabled(true);
        }

        // enable(disable) rename icon
        menu.findItem(R.id.rename).setEnabled(false);
        if (selectedCount == 1) {
            File f = new File(mCurrentDirPath + MountPointHelper.SEPARATOR + mAdapter.getCheckedItemsList().get(0));
            if (f.canWrite()) {
                menu.findItem(R.id.rename).setEnabled(true);
            }
        }

        // enable(disable) details icon
        if (selectedCount == 1) {
            menu.findItem(R.id.details).setEnabled(true);
        } else {
            menu.findItem(R.id.details).setEnabled(false);
        }

        // enable(disable) protection info icon
        if (OptionsUtil.isDrmSupported()) {
            menu.findItem(R.id.protection_info).setTitle(
                    com.mediatek.internal.R.string.drm_protectioninfo_title);
            menu.findItem(R.id.protection_info).setEnabled(false);

            if (selectedCount == 1) {
                File file = new File(mCurrentDirPath + MountPointHelper.SEPARATOR
                        + mAdapter.getCheckedItemsList().get(0));
                if (file.isFile()) {
                    String path = file.getPath();

                    if (mDrmManagerClient.getDrmObjectType(path, null) != DrmStore.DrmObjectType.UNKNOWN) {
                        String mimeType = mDrmManagerClient.getOriginalMimeType(path);

                        if (mimeType != null && mimeType.trim().length() != 0) {
                            menu.findItem(R.id.protection_info).setEnabled(true);
                        } else {
                            menu.findItem(R.id.protection_info).setEnabled(false);
                        }
                    }
                }
            }
        } else {
            menu.removeItem(R.id.protection_info);
        }

        // enable(disable) paste icon
        menu.findItem(R.id.paste).setEnabled(false);
        if ((EditUtility.getLastOperation() == EditUtility.COPY)
                || (EditUtility.getLastOperation() == EditUtility.CUT)) {
            // choose zero or one folder to paste, otherwise, disable paste icon
            if (Clipboard.getContents().size() > 0) {
                if (selectedCount == 0) {
                    if (new File(mCurrentDirPath).canWrite()) {
                        menu.findItem(R.id.paste).setEnabled(true);
                    }
                } else {
                    if (selectedCount == 1) {
                        String file = mAdapter.getCheckedItemsList().get(0);
                        File f = new File(mCurrentDirPath + MountPointHelper.SEPARATOR + file);
                        if (f.isDirectory() && f.canWrite()) {
                            menu.findItem(R.id.paste).setEnabled(true);
                        }
                    }
                }
            }
        }

        // enable(disable) share icon
        if (selectedCount == 0) {
            menu.findItem(R.id.share).setEnabled(false);
        } else if (selectedCount == 1) {
            menu.findItem(R.id.share).setEnabled(true);
            if (OptionsUtil.isDrmSupported()) {
                File file = new File(mCurrentDirPath + MountPointHelper.SEPARATOR
                        + mAdapter.getCheckedItemsList().get(0));

                if (file.isFile()) {
                    String ext = EditUtility.getFileExtension(file.getName());
                    if (ext != null && ext.equalsIgnoreCase(EditUtility.EXT_DRM_CONTENT)) {
                        if (mDrmManagerClient.checkRightsStatus(file.getPath(),
                                DrmStore.Action.TRANSFER) != DrmStore.RightsStatus.RIGHTS_VALID) {
                            menu.findItem(R.id.share).setEnabled(false);
                        }
                    }
                } else {
                    menu.findItem(R.id.share).setEnabled(false);
                }
            }
        } else {
            boolean flag = true;
            List<String> files = mAdapter.getCheckedItemsList();
            for (String s : files) {
                File file = new File(mCurrentDirPath + MountPointHelper.SEPARATOR + s);
                if (file.isDirectory()) {
                    flag = false;
                    break; // break for loop; disable share icon
                }
            }

            if (flag) {
                menu.findItem(R.id.share).setEnabled(true);
            } else {
                menu.findItem(R.id.share).setEnabled(false);
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (isBusyForFileTask()) {
            // solve rare case: user presses "paste" twice fast enough
            return true;
        }
        switch (menuItem.getItemId()) {
        case R.id.cut:
            FileManagerLog.e(TAG, "onMenuItemClick- cut");
            for (int i = 0; i < mFileInfoList.size(); i++) {
                mFileInfoList.get(i).setCut(false);
            }
            EditUtility.cut(this, mAdapter.getCheckedFileInfos());
            EditUtility.setLastOperation(EditUtility.CUT);
            switchToNavigationView(false);
            break;

        case R.id.rename:
            FileManagerLog.e(TAG, "onMenuItemClick- rename");
            removeDialog(CustomDialog.DIALOG_RENAME);
            showDialog(CustomDialog.DIALOG_RENAME);
            EditUtility.setLastOperation(EditUtility.RENAME);
            break;

        case R.id.details:
            FileManagerLog.e(TAG, "onMenuItemClick- details");
            FileInfo fileInfo = mAdapter.getCheckedFileInfos().get(0);
            mFileTask = new FileTask(fileInfo, EditUtility.DETAILS);
            mFileTask.execute();
            break;

        case R.id.protection_info:
            FileManagerLog.e(TAG, "onMenuItemClick- protection_info");
            // calling framework to show a protection info dialog
            String path = mCurrentDirPath + MountPointHelper.SEPARATOR + mAdapter.getCheckedItemsList().get(0);
            //mDrmManagerClient.showProtectionInfoDialog(this, path);
            switchToNavigationView(false);
            EditUtility.setLastOperation(EditUtility.PROTECTION_INFO);
            break;

        case R.id.paste:
            FileManagerLog.e(TAG, "onMenuItemClick- paste");
            if (EditUtility.getLastOperation() == EditUtility.COPY
                    || EditUtility.getLastOperation() == EditUtility.CUT) {
                if (mAdapter.getCheckedItemsCount() == 0) {
                    mFileTask = new FileTask(mCurrentDirPath, EditUtility.PASTE);
                    mFileTask.execute();
                } else {
                    // paste to the selected target folder
                    String targetDir = mCurrentDirPath + MountPointHelper.SEPARATOR
                            + mAdapter.getCheckedItemsList().get(0);
                    mFileTask = new FileTask(targetDir, EditUtility.PASTE);
                    mFileTask.execute();
                }
            }
            break;

        case R.id.share:
            share();
            EditUtility.setLastOperation(EditUtility.SHARE);
            break;

        default:
            return false;
        }
        return true;
    }

    public static final class ScannerClient implements MediaScannerConnectionClient {
        private ArrayList<String> mPaths = new ArrayList<String>();
        private MediaScannerConnection mScannerConnection = null;
        private int mScanningFileNumber = 0;
        private long mScanFilesWatingTimeStart = 0;
        private Object mLock = new Object();

        private static ScannerClient sInstance = null;

        /**
         * Please call the method init(Context ) before useing it.
         * @return ScannerClient ScannerClient Object
         */
        public static ScannerClient getInstance() {
            if (sInstance == null) {
                sInstance = new ScannerClient();
            }
            return sInstance;
        }

        private ScannerClient() {
        }

        public void init(Context context) {
            if (mScannerConnection != null && mScannerConnection.isConnected()) {
                mScannerConnection.disconnect();
                mScannerConnection = null;
            }
            mScannerConnection = new MediaScannerConnection(context, this);
        }

        public void scanPath(String path) {
            FileManagerLog.i(TAG, "scanPath() thread id: " + Thread.currentThread().getId());
            synchronized (mLock) {
                mScanningFileNumber++;
                mScanFilesWatingTimeStart = System.currentTimeMillis();
                if (mScannerConnection.isConnected()) {
                    mScannerConnection.scanFile(path, null);
                } else {
                    mPaths.add(path);
                    mScannerConnection.connect();
                }
            }
        }

        public void connect() {
            if (!mScannerConnection.isConnected()) {
                mScannerConnection.connect();
            }
        }

        public void disconnect() {
            synchronized (mLock) {
                mScanningFileNumber = 0;
                mPaths.clear();
                mScanFilesWatingTimeStart = 0;
                mScannerConnection.disconnect();
            }
        }

        @Override
        public void onMediaScannerConnected() {
            FileManagerLog.i(TAG, "onMediaScannerConnected(), thread id: "
                    + Thread.currentThread().getId());
            synchronized (mLock) {
                if (!mPaths.isEmpty()) {
                    for (String path : mPaths) {
                        mScannerConnection.scanFile(path, null);
                    }
                    mPaths.clear();
                }
            }
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            FileManagerLog.i(TAG, "onScanCompleted() thread: " + Thread.currentThread().getId());
            FileManagerLog.i(TAG, "path=" + path + ", uri: " + uri.toString());
            synchronized (mLock) {
                mScanningFileNumber--;
            }
        }

        public boolean waitForScanningCompleted() {
            FileManagerLog.i(TAG, "waitForScanningCompleted() :" + Thread.currentThread().getId());
            FileManagerLog.i(TAG, "mScanningFileNumber: " + mScanningFileNumber);
            if (mScanningFileNumber == 0) {
                return true;
            }

            if (System.currentTimeMillis() - mScanFilesWatingTimeStart >= 3000) {
                FileManagerLog.i(TAG, "Query MediaStore waiting overtime: "
                        + (System.currentTimeMillis() - mScanFilesWatingTimeStart));
                return true;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public class FileTask extends AsyncTask<Void, Object, Void> {
        private static final int INDEX_RETURNED_ACTION = 3;
        private final int mTask;
        private final Object mInfo;
        private ProgressBar mProgressBar = null;
        private TextView mProgressText = null;
        private TextView mProgressPercentage = null;
        private TextView mProgressCount = null;
        private TextView mDetailsText = null;
        private String[] mDetailsParts = null;
        private List<FileInfo> mDeletedFilesInfo = null;

        /**
         * The constructor to construct a background thread
         * @param info the required information passed to the background thread
         * @param task the ID of task to be performed
         */
        FileTask(Object info, int task) {
            // for mInfo: pass targetDir for paste operation; pass deleteList for delete operation
            mInfo = info;
            mTask = task;
        }

        /**
         * A callback method to be invoked before the background thread starts running
         */
        @Override
        protected void onPreExecute() {
            FileManagerLog.d(TAG, "onPreExecute: " + mTask);
            mBackgroundThreadRunning = true;

            /*
             * Create a progress dialog here instead of using "showDialog()" like other custom
             * dialogs because onCreateDialog is being called only once and this causes the problem
             * of not calling isCancelled() [AsyncTask cancel callback] when cancel is clicked.
             */
            if (mTask == EditUtility.PASTE || mTask == EditUtility.DELETE) {
                Object[] progressInfo = { this };
                mDialog = new CustomDialog(FileManagerOperationActivity.this,
                        CustomDialog.DIALOG_PROGRESS, progressInfo);
                mProgressBar = (ProgressBar) mDialog.findViewById(R.id.progress_bar);
                mProgressText = (TextView) mDialog.findViewById(R.id.progress_text);
                mProgressPercentage = (TextView) mDialog.findViewById(R.id.progress_percentage);
                mProgressCount = (TextView) mDialog.findViewById(R.id.progress_count);

                if (mTask == EditUtility.DELETE) { // default title = pasting
                    TextView mProgressTitle = (TextView) mDialog
                            .findViewById(R.id.title_bar_progress);
                    mProgressTitle.setText(R.string.deleting);
                }
                mDialog.setCancelable(false);
            } else { // mTask is details
                mDetailsParts = new String[3];
                String info = EditUtility.getDetails(FileManagerOperationActivity.this,
                        (FileInfo) mInfo, mDetailsParts);
                Object[] detailsInfo = { mCurrentDirPath, info };
                mDialog = new CustomDialog(FileManagerOperationActivity.this,
                        CustomDialog.DIALOG_DETAILS, detailsInfo);
                mDetailsText = (TextView) mDialog.findViewById(R.id.details_text);
                mDialog.setOnDismissListener(FileManagerOperationActivity.this);
                mDialog.show();
            }
        }

        /**
         * A callback method to be invoked when the background thread starts running
         * @param params the method need not parameters here
         * @return null, the background thread need not return anything
         */
        @Override
        protected Void doInBackground(Void... params) {
            if (mTask == EditUtility.PASTE) {
                EditUtility.paste(FileManagerOperationActivity.this, mInfo.toString(), this);
                EditUtility.setLastOperation(EditUtility.PASTE);
            } else if (mTask == EditUtility.DELETE) {
                if (mInfo != null) {
                    mDeletedFilesInfo = EditUtility.delete(FileManagerOperationActivity.this,
                            (List<FileInfo>) mInfo, this);
                    EditUtility.setLastOperation(EditUtility.DELETE);
                }
            } else if (mTask == EditUtility.DETAILS) {
                EditUtility.updateContentSize((FileInfo) mInfo, this);
                EditUtility.setLastOperation(EditUtility.DETAILS);
            }
            mBackgroundThreadRunning = false;
            FileManagerLog.d(TAG, "Fn-doInBackground() is done: " + mTask);
            return null;
        }

        /**
         * A callback method to be invoked while the background thread is running
         * @param progress the progress information to be updated on the UI thread
         */
        @Override
        protected void onProgressUpdate(Object... progress) {
            if (mTask == EditUtility.PASTE || mTask == EditUtility.DELETE) {
                String tag = null;
                if (progress[1] != null) {
                    tag = progress[1].toString();
                    if (tag.length() >= TAG_MAX_LENGTH) {
                        tag = tag.substring(0, TAG_MAX_LENGTH) + "...";
                    }
                }

                FileManagerLog.d(TAG, "On progress update action: "
                        + (Integer) progress[INDEX_RETURNED_ACTION]);

                if ((Integer) progress[INDEX_RETURNED_ACTION] == SHOW_WAITING_DIALOG) {
                    mWaitingDialog = ProgressDialog.show(FileManagerOperationActivity.this, "",
                            mResources.getString(R.string.wait) + "...", true);
                    mWaitingDialog.show();
                } else {
                    if (mWaitingDialog != null) {
                        if (mWaitingDialog.isShowing()) {
                            mWaitingDialog.dismiss();
                        }
                        mWaitingDialog = null;
                    }
                }

                if ((Integer) progress[INDEX_RETURNED_ACTION] == SHOW_PROGRESS_DIALOG) {
                    mDialog.show();
                } else if ((Integer) progress[INDEX_RETURNED_ACTION] == INSUFFICIENT_MEMORY) {
                    EditUtility.showToast(FileManagerOperationActivity.this, R.string.insufficient_memory);
                } else if ((Integer) progress[INDEX_RETURNED_ACTION] == DELETE_FAIL) {
                    String msg = mResources.getString(R.string.delete_fail) + " " + tag;
                    EditUtility.showToast(FileManagerOperationActivity.this, msg);
                } else if ((Integer) progress[INDEX_RETURNED_ACTION] == DELETE_DENY) {
                    String msg = mResources.getString(R.string.delete_deny) + " " + tag;
                    EditUtility.showToast(FileManagerOperationActivity.this, msg);
                } else if ((Integer) progress[INDEX_RETURNED_ACTION] == CUT_FAIL) {
                    String msg = mResources.getString(R.string.cut_fail) + " " + tag;
                    EditUtility.showToast(FileManagerOperationActivity.this, msg);
                } else if ((Integer) progress[INDEX_RETURNED_ACTION] == PASTE_FAIL) {
                    String msg = mResources.getString(R.string.paste_fail) + " " + tag;
                    EditUtility.showToast(FileManagerOperationActivity.this, msg);
                } else if ((Integer) progress[INDEX_RETURNED_ACTION] == PASTE_SAME_FOLDER) {
                    String msg = mResources.getString(R.string.paste_same_folder, tag);
                    EditUtility.showToast(FileManagerOperationActivity.this, msg);
                } else if ((Integer) progress[INDEX_RETURNED_ACTION] == OPERATION_SUCCESS) {
                    mProgressBar.setProgress((Integer) progress[0]);
                    mProgressText.setText(progress[1].toString());
                    mProgressPercentage.setText(progress[0].toString() + "%");
                    mProgressCount.setText(progress[2].toString());
                }
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(mDetailsParts[0]).append(mResources.getString(R.string.size)).append(":")
                        .append(progress[2].toString()).append("\n").append(mDetailsParts[2]);
                mDetailsText.setText(sb.toString());
                Log.i("heqianqian","sb*************************="+sb.toString());
            }
        }

        private void updateListForDelete() {
            switchToNavigationView(false);
            if (mDeletedFilesInfo != null) {
                for (int i = 0; i < mDeletedFilesInfo.size(); i++) {
                    mFileInfoList.remove(mDeletedFilesInfo.get(i));
                }
                mDeletedFilesInfo.clear();
                mDeletedFilesInfo = null;
            }
            ((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
            ((List<FileInfo>) mInfo).clear();
        }

        private void updateUIAndClearScene() {
            if (mTask == EditUtility.DELETE) {
                // send broadcast to notify other applications for deleting files
                EditUtility.notifyUpdates(FileManagerOperationActivity.this,
                        Intent.ACTION_MEDIA_MOUNTED, new File(mCurrentDirPath));
                updateListForDelete();
                dismissDialog();
            }

            if (mTask == EditUtility.PASTE) {
                // send broadcast to notify other applications for copying files
                EditUtility.notifyUpdates(FileManagerOperationActivity.this,
                        Intent.ACTION_MEDIA_MOUNTED, new File(mCurrentDirPath));
                dismissDialog();
                Message.obtain(mHandler, PASTE_DONE).sendToTarget();
            }
        }

        /**
         * A callback method to be invoked after the background thread performs the task
         * @param result the value returned by doInBackground(), but it is not needed here
         */
        @Override
        protected void onPostExecute(Void result) {
            FileManagerLog.d(TAG, "onPostExecute file task: " + mTask);
            updateUIAndClearScene();
        }

        /**
         * A callback method to update the progress bar on the UI thread
         * @param percentage the percentage should be displayed, indicates completed task
         * @param fileName the operation is performed on this file present
         * @param count the total number of files or folders
         * @param showErrorMsg for operation failed case
         */
        public void onUpateProgessBar(int percentage, String fileName, String count,
                int showErrorMsg) {
            publishProgress(percentage, fileName, count, showErrorMsg);
        }

        /**
         * A callback method to be invoked when the background thread's task is cancelled
         */
        @Override
        protected void onCancelled() {
            FileManagerLog.d(TAG, "onCancelled file task: " + mTask);
            updateUIAndClearScene();
        }
    }

    protected void returnToRootPointForUnmount(String path) {
        super.returnToRootPointForUnmount(path);
        FileManagerLog.w(TAG, "returnToRootPointForUnmount");
        dismissDialog();
        if (isBusyForFileTask()){
            boolean canceled = mFileTask.cancel(true);
            FileManagerLog.w(TAG, "returnToRootPointForUnmount cancel mFileTask: " + canceled);
        }
        FileManagerLog.d(TAG, "returnToRootPointForUnmount: dismissDialog()");
    }

    private boolean isBusyForFileTask(){
        if (mFileTask != null && !mFileTask.isCancelled()) {
            if (mFileTask.getStatus() == AsyncTask.Status.PENDING
                    || mFileTask.getStatus() == AsyncTask.Status.RUNNING) {
                FileManagerLog.i(TAG, "mFileTask is running.");
                return true;
            }
        }
        return false;
    }

    public boolean isBusyForLoadingTask() {
        if (isBusyForFileTask()) {
            return true;
        }
        return super.isBusyForLoadingTask();
    }

}
