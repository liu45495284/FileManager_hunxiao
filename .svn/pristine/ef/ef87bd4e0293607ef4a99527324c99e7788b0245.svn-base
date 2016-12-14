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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.freeme.filemanager.R;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.FileManagerLog;
import com.freeme.filemanager.util.MountPointHelper;
import com.umeng.analytics.MobclickAgent;

import android.content.Intent;
import android.drm.DrmManagerClient;
import android.drm.DrmUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;


public class FileManagerSelectFileActivity extends FileManagerBaseActivity {
    private static final String TAG = "FileManagerSelectFileActivity";

    /**
     * create the main ui of FileManagerSelectFileActivity
     * @param savedInstanceState Instance state informations stored in onSaveInstanceState() method.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set default path as "/mnt/"
        addTab(MountPointHelper.ROOT);
        updateHomeButton();
    }

    @Override
    protected void setMainContentView() {
        setContentView(R.layout.select_file_main);
        // set up a sliding navigation bar for navigation view
        mNavigationBar = (HorizontalScrollView) findViewById(R.id.navigation_bar);

        // set up a tab holder to hold all tabs on the navigation bar
        mTabsHolder = (LinearLayout) findViewById(R.id.tabs_holder);

        Button btnCancel = (Button) findViewById(R.id.select_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                FileManagerLog.d(TAG, "Select file canceled, quit directly ");

                // Do the same thing as quit by back-key, not to point out "canceled"
                finish();
            }
        });
    }

    protected void prepareForMediaEventHandle(String action) {
        return;
    }

    protected void updateViewCompomentStateForMediaEvent(String action, String mountPoint) {
        return;
    }

    @Override
    protected void loadFileInfoList(File[] files) {
        // load the file info first.
        for (File file : files) {
            FileInfo fileInfo = new FileInfo(this, file, mDrmManagerClient);
          //*/added by tyd carl,20120703.[tyd00429949]
            if(!fileInfo.getFileName().startsWith("."))
                //*/
            mFileInfoList.add(fileInfo);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileInfo selecteItemFileInfo = mFileInfoList.get(position);

        if (selecteItemFileInfo.isDirectory()) {
            String focusedItemFileName = selecteItemFileInfo.getFileName();
            int top = view.getTop();
            FileManagerLog.v(TAG, "top = " + top);
            addToNavigationList(mCurrentDirPath, focusedItemFileName, top);
            addTab(focusedItemFileName);
            updateHomeButton();
            showDirectoryContent(selecteItemFileInfo.mFilePath);
        } else {
            Intent intent = new Intent();
            Uri uri = Uri.fromFile(selecteItemFileInfo.getFile());
            FileManagerLog.d(TAG, "Add uri file: " + uri);
            intent.setData(uri);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    /**
     * This method gets the DrmManagerClient instance
     * @return DrmManagerClient instance
     */
    protected DrmManagerClient getDrmManagerClient() {
        return mDrmManagerClient;
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
