package com.freeme.filemanager.view.garbage;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import com.freeme.filemanager.R;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.garbage.AsyncGarbageCleanupHelper.GarbageItem;
import com.freeme.filemanager.util.FeatureOption;
import com.umeng.analytics.MobclickAgent;

import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GarbageCleanupActivity extends Activity implements ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener,
        AsyncGarbageCleanupHelper.GarbageCleanupStatesListener {
    private GarbageExpandAdapter mAdapter;
    private AsyncGarbageCleanupHelper mAsyncGarbageCleanupHelper;
    private Button mButton;
    private boolean mFinishState = false;
    private boolean mCleanupState = false;
    private Handler mHandler;
    private ExpandableListView mExpandableListView;
    private String TAG = "GarbageCleanupActivity";

    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        mHandler = new Handler(getMainLooper());
        setContentView(R.layout.garbage_cleanup_layout);
        mExpandableListView = (ExpandableListView) findViewById(R.id.expande_list);
        mButton = (Button) findViewById(R.id.cleanup_button);
        mAsyncGarbageCleanupHelper = new AsyncGarbageCleanupHelper(this);
        mExpandableListView.setGroupIndicator(null);
        mExpandableListView.setOnGroupClickListener(this);
        mExpandableListView.setOnChildClickListener(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mAdapter = new GarbageExpandAdapter(this);
        mAdapter.setOnUpdateButtonStateListener(new GarbageExpandAdapter.OnUpdateButtonStateListener() {
            public void onUpdate() {
                updateButtonState();
            }
        });
        initButtonListener();
        mExpandableListView.setAdapter(mAdapter);
        clickEvent();
    }

    private void initButtonListener() {
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((mAsyncGarbageCleanupHelper != null) && (mAsyncGarbageCleanupHelper.getState() == mAsyncGarbageCleanupHelper.STATE_CLEANUP_FINISH)) {
                    finish();
                    return;
                }
                clickEvent();
            }
        });
    }

    private void clickEvent() {
        if(mAsyncGarbageCleanupHelper == null || mAsyncGarbageCleanupHelper.getState() == mAsyncGarbageCleanupHelper.STATE_START_SCAN){
            return;
        }
        
        ArrayList groupMarkList = new ArrayList(mAdapter.getGroupMarkItem());
        
        
        if (groupMarkList != null && !groupMarkList.isEmpty()) {
            
            Log.i("appGarbageCleanup", "groupMarkList, size: "+groupMarkList.size());
            for(int i=0; i<groupMarkList.size(); i++) {
                Log.i("appGarbageCleanup", "getGroupMarkItem(); i: "+i+",item: "+groupMarkList.get(i));
            }
            
            mAsyncGarbageCleanupHelper.setActionOperate(groupMarkList);
        }
        
        List<GarbageItem> itemList = mAdapter.getChildSelectedItems();
        if ((itemList != null) && (!itemList.isEmpty())) {
            mAsyncGarbageCleanupHelper.setAppGarbageCleanupItem(itemList);
        }
        
        mAsyncGarbageCleanupHelper.setGarbageCleanupStatesListener(this);
        mAsyncGarbageCleanupHelper.cleanUp();
        
    }

    private void updateButtonState() {
        ArrayList localArrayList = mAdapter.getGroupMarkItem();
        if (((localArrayList != null) && (localArrayList.size() > 0)) || (this.mAdapter.getChildMarkItem() > 0)) {
            if(!mButton.getText().equals(getResources().getString(R.string.garbage_scanning))){
            mButton.setEnabled(true);
            }
            return;
        }
        mButton.setEnabled(false);
    }

    @Override
    public boolean onChildClick(ExpandableListView paramExpandableListView, View paramView, int groupPosition, int childPosition, long id) {
        if ((mAdapter.getGroupItemProgressState(groupPosition) == 0) || (mFinishState)) {
            return false;
        }
        mAdapter.markChildItem(groupPosition, childPosition);
        updateButtonState();
        return true;
    }
    
    @Override
    public boolean onGroupClick(ExpandableListView expandableListView,
            View view, int groupPosition, long id) {
        Log.i("appGarbageCleanup", "onGroupClick(),groupPosition: "+groupPosition);
        if ((mAdapter.getGroupItemProgressState(groupPosition) == 0) || (mFinishState)) {
            return false;
        }
        if (groupPosition < mAdapter.getGroupCount()-1) {
            mAdapter.markGroupItem(groupPosition);
            updateButtonState();
            return true;
        }
        return false;
    }

    @Override
    public void finish() {
        super.finish();
        if (mAsyncGarbageCleanupHelper == null) {
            return;
        }
        mAsyncGarbageCleanupHelper.stopRunning();
        mAsyncGarbageCleanupHelper = null;
        notifyToRefreshView();
        //modify by tyd liuyong 20140806 for kk storage
        if (FeatureOption.MTK_MULTI_STORAGE_SUPPORT) {
            String internalPath = Util.MEMORY_DIR;
            String externalPath = Util.SD_DIR;
            notifyFileChanged(internalPath);
            notifyFileChanged(externalPath);
        }else{
            String internalPath = Util.MEMORY_DIR;
            notifyFileChanged(internalPath);
        }
        
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void notifyToRefreshView() {
        //modify by tyd liuyong 20140806 for kk storage
        Intent intent;
        if (FeatureOption.MTK_MULTI_STORAGE_SUPPORT) {
            intent = new Intent(GlobalConsts.BROADCAST_REFRESH, Uri.fromFile(new File("/storage/sdcard0")));
        }else{
            intent = new Intent(GlobalConsts.BROADCAST_REFRESH, Uri.fromFile(new File("/storage/emulated/0")));
        }
        intent.putExtra(GlobalConsts.BROADCAST_REFRESH_EXTRA, GlobalConsts.BROADCAST_REFRESH_TABVIEW);
        sendBroadcast(intent);
        Log.i("wulianghuanNotify", "notifyToRefreshView()");
    }
    
    private void notifyFileChanged(String path) {
        if(!TextUtils.isEmpty(path)){
        File file = new File(path);
        Log.i(TAG, "file=" + file);
        if (file != null && file.exists()) {
            Log.i("wulianghuanNotify", "notifyFileChanged(), path: "+path);
            Intent localIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
            localIntent.setClassName("com.android.providers.media", "com.android.providers.media.MediaScannerReceiver");
            localIntent.setData(Uri.fromFile(new File(path)));
            sendBroadcast(localIntent);
        }
    }
    }
    @Override
    public void onFinish(final int position, final long fileSize, final int fileCount) {
        mHandler.post(new Runnable() {
            public void run() {

                mAdapter.setGroupItemProgress(position, 8);
                mAdapter.setGroupData(position, fileSize);
            }
        });
    }
    
    @Override
    public void onAppGarbageFinish(final List<GarbageItem> list) {
        mHandler.post(new Runnable() {
            public void run() {
                mAdapter.setChildData(list);
            }
        });
    }

    @Override
    public void onUpdateUI(final int state) {
        mHandler.post(new Runnable() {
            public void run() {
                switch (state) {
                case AsyncGarbageCleanupHelper.STATE_START_SCAN:
                    mButton.setEnabled(false);
                    mExpandableListView.setEnabled(false);
                    mButton.setText(getResources().getString(R.string.garbage_scanning));
                    mAdapter.setAllGroupItemProgress(0);
                    break;
                    
                case AsyncGarbageCleanupHelper.STATE_SCAN_FINISH:
                    mButton.setEnabled(true);
                    mExpandableListView.setEnabled(true);
                    mButton.setText(getResources().getString(R.string.start_clean));
                    break;
                    
                case AsyncGarbageCleanupHelper.STATE_START_CLEANUP:
                    mButton.setEnabled(false);
                    mExpandableListView.setEnabled(false);
                    mButton.setText(getResources().getString(R.string.garbage_cleaning));
                    ArrayList localArrayList = mAdapter.getGroupMarkItem();
                    if (mAdapter != null) {
                        mCleanupState = true;
                        mAdapter.setCleanupState(mCleanupState);
                    }
                    for (int i = 0; i < localArrayList.size(); i++) {
                        mAdapter.setGroupItemProgress((Integer)localArrayList.get(i), 0);
                        mAdapter.notifyDataSetChanged();
                    }
                    break;
                    
                case AsyncGarbageCleanupHelper.STATE_CLEANUP_FINISH:
                    if (mAsyncGarbageCleanupHelper != null) {
                        long cleanSize = mAsyncGarbageCleanupHelper.getTotalDeletedFileSize();
                        if (cleanSize > 0) {
                 //*/modified by droi mingjun on 2015-12-28 for garbage data       
                            String emptyDir = Util.convertStorage(mAsyncGarbageCleanupHelper.getTotalDeletedFileSize());
                            if (emptyDir.equals("64.0 KB") || emptyDir.equals("32.0 KB")) {
                                Toast.makeText(GarbageCleanupActivity.this, getResources().getString(R.string.no_garbage_result), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(GarbageCleanupActivity.this,
                                        getResources().getString(R.string.garbage_clean_result, Util.convertStorage(mAsyncGarbageCleanupHelper.getTotalDeletedFileSize())), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(GarbageCleanupActivity.this, getResources().getString(R.string.no_garbage_result), Toast.LENGTH_SHORT).show();
                        }
                  //*/   
                        mAsyncGarbageCleanupHelper.stopRunning();
                        mAsyncGarbageCleanupHelper.resetDeletedParam();
                    }
                    mButton.setEnabled(true);
                    mExpandableListView.setEnabled(true);
                    mButton.setText(getResources().getString(R.string.garbage_clean_finish));
                    if (mAdapter != null) {
                        mFinishState = true;
                        mAdapter.setFinishState(mFinishState);
                    }
                    break;
                    
                }
            }
        });
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
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
    }

}
