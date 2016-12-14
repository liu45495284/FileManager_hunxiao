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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.freeme.filemanager.R;
import com.freeme.filemanager.controller.FileManagerOperationActivity.FileTask;
import com.freeme.filemanager.model.EditUtility;
import com.freeme.filemanager.model.FileManagerLog;


public class CustomDialog extends Dialog implements OnClickListener {
    private static final String TAG = "CustomDialog";

    public static final int DIALOG_CREATE_FOLDER = 0;
    public static final int DIALOG_PROGRESS = 1;
    public static final int DIALOG_DELETE = 2;
    public static final int DIALOG_RENAME = 3;
    public static final int DIALOG_DETAILS = 4;
    public static final int DIALOG_SORT = 5;
    public static final int DIALOG_FORBIDDEN = 6;
    public static final int DIALOG_MULTI_DELETE = 7;
    public static final int DIALOG_RENAME_EXTENSION = 8;
    public static final int DIALOG_FILENAME_MAX_SIZE = 255;

    private final Context mContext;
    private final int mType;
    private Button mBtnDone;
    private Button mBtnCancel;
    private EditText mName;
    private String mCurrentDirPath;
    private String mDetails;
    private String mSelectedItem;
    private FileTask mFileTask;
    private boolean mReset = false;

    /**
     * The constructor to construct a custom dialog
     * @param context the context of FileManagerActivity
     * @param type the type of the dialog
     * @param info extra info to show in a dialog
     */
    public CustomDialog(Context context, int type, Object[] info) {
        super(context);
        mContext = context;
        mType = type;
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        switch (type) {
        case DIALOG_CREATE_FOLDER:
            if (info[0] != null) {
                mCurrentDirPath = info[0].toString();
                showCreateFolderDialog();
            }
            break;

        case DIALOG_PROGRESS:
            if (info[0] != null) {
                mFileTask = (FileTask) info[0];
                showProgressDialog();
            }
            break;

        case DIALOG_DETAILS:
            if ((info[0] != null) && (info[1] != null)) {
                mCurrentDirPath = info[0].toString();
                mDetails = info[1].toString();
                showDetailsDialog();
            }
            break;

        default:
            break;
        }
    }

    /**
     * The method to get the type of the dialog (i.e. details dialog)
     * @return the type of the dialog
     */
    protected int getType() {
        return mType;
    }

    /**
     * The method to set the current directory path
     * @param path the path of the current directory
     */
    protected void setCurrentDirPath(String path) {
        mCurrentDirPath = path;
    }

    /**
     * The method to set the current selected item
     * @param item the name of the selected item
     */
    protected void setSelectedItem(String item) {
        mSelectedItem = item;
    }

    /**
     * This method register callback and set filter to Edit, in order to make sure that user input
     * is legal. The input can't be illegal filename and can't be too long.
     */
    private void setTextChangedCallback() {
        mReset = true;
        setEditTextFilter(mName);
        mName.addTextChangedListener(new TextWatcher() {

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
                        EditUtility.showToast(mContext, R.string.invalid_char_prompt);
                    }
                    mBtnDone.setEnabled(false);
                } else {
                    mBtnDone.setEnabled(true);
                }
            }
        });
    }

    /**
     * The method to show a create folder dialog
     */
    private void showCreateFolderDialog() {
        this.setContentView(R.layout.create_folder_dialog);

        mBtnDone = (Button) this.findViewById(R.id.create_btn_done);
        mBtnDone.setEnabled(false);
        mBtnDone.setOnClickListener(this);

        mBtnCancel = (Button) this.findViewById(R.id.create_btn_cancel);
        mBtnCancel.setOnClickListener(this);

        mName = (EditText) this.findViewById(R.id.create_folder_name);
        setTextChangedCallback();
    }

    /**
     * The method to show a details dialog
     */
    private void showDetailsDialog() {
        TextView textView = null;
        this.setContentView(R.layout.details_dialog);

        textView = (TextView) this.findViewById(R.id.details_text);
        textView.setText(mDetails);

        mBtnDone = (Button) this.findViewById(R.id.details_btn_ok);
        mBtnDone.setEnabled(true);
        mBtnDone.setOnClickListener(this);
    }

    /**
     * The method to show a progress dialog (while pasting)
     */
    private void showProgressDialog() {
        this.setContentView(R.layout.progress_dialog);

        mBtnCancel = (Button) this.findViewById(R.id.progress_btn_cancel);
        mBtnCancel.setOnClickListener(this);
        this.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * The method resets the filter for calculating input string
     */
    protected void resetFilter() {
        FileManagerLog.d(TAG, "Reset filter");
        mReset = true;
    }

    /**
     * This method is used to set filter to EditText which is used for user entering filename. This
     * filter will ensure that the inputed filename wouldn't be too long. If so, the inputed info
     * would be rejected.
     * @param edit The EditText for filter to be registered.
     */
    private void setEditTextFilter(EditText edit) {
        InputFilter filter = new InputFilter() {

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                    int dstart, int dend) {
                int oldSize = 0;
                int sourceSize = 0;

                // original
                String name = mName.getText().toString();
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

                    if (newSize > DIALOG_FILENAME_MAX_SIZE) {
                        Vibrator vibrator = (Vibrator) mContext
                                .getSystemService(mContext.VIBRATOR_SERVICE);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.create_btn_done:
            if (EditUtility.getFreeSpace(mCurrentDirPath) > 0) {
                String fileName = mName.getText().toString().trim();

                if (EditUtility.isValidName(mContext, fileName)) {
                    if (EditUtility.createFolder(mContext, mCurrentDirPath + "/" + fileName)) {
                        FileManagerLog.d(TAG, "Create new folder successfully: " + mCurrentDirPath
                                + "/" + fileName);
                        dismiss();
                    }
                }
            } else {
                // show insufficient memory message
                dismiss();
                EditUtility.showToast(mContext, R.string.insufficient_memory);
            }
            break;

        case R.id.progress_btn_cancel:
            boolean cancleResult = mFileTask.cancel(true); // cancel pasting
            FileManagerLog.i(TAG, "(Cancel Button)Cancel FileTask result: " + cancleResult);
            break;

        case R.id.create_btn_cancel:
        case R.id.details_btn_ok:
            dismiss();
            break;

        default:
            break;
        }
    }
}