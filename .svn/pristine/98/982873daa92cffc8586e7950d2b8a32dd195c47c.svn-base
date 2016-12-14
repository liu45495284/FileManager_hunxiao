package com.freeme.filemanager.view;

import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.freeme.filemanager.FileExplorerTabActivity;
import com.freeme.filemanager.FileManagerApplication;
import com.freeme.filemanager.FileManagerApplication.ScannerReceiver;
import com.freeme.filemanager.R;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.FileCategoryHelper;
import com.freeme.filemanager.util.StorageHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.util.FileCategoryHelper.CategoryInfo;
import com.freeme.filemanager.util.FileCategoryHelper.FileCategory;
import com.freeme.filemanager.util.featureoption.FeatureOption;
import com.umeng.analytics.MobclickAgent;

public class MoneyInfoActivity extends Activity {
    private RoundProgressBar mRoundProgressBar1, mRoundProgressBar2 ,mRoundProgressBar3, mRoundProgressBar4, mRoundProgressBar5;
    private long progress = 0;
    private long sdMoneryCard = 0;
    private long sdMoneryCardused = 0;
    private long sdMoneryCards;
    private long sdMoneryFrees;
    private String sdMoneryCarduseds;
    private long sdMoneryCardusedl;
    private int isSdCard=1;
    private TextView infoAvailable;
    private TextView infoMemory;
     private FileCategoryHelper mFileCagetoryHelper;
     private AsyncTask<Void, Void, Object> mRefreshCategoryInfoTask;
    public static final String TAG = "MoneyInfoActivity";
    
    private ScannerReceiver mScannerReceiver;
    
    private LinearLayout mMusicLinearLayout;
    private LinearLayout mVideoLinearLayout;
    private LinearLayout mPictureLinearLayout;
    private LinearLayout mApkLinearLayout;
    private LinearLayout mDcLinearLayout;
    private LinearLayout mOtherLinearLayout;
    private static HashMap<Integer, FileCategory> button3Category = new HashMap<Integer, FileCategory>();
    
    static {
        button3Category.put(R.id.category_music_small, FileCategory.Music);
        button3Category.put(R.id.category_video_small, FileCategory.Video);
        button3Category.put(R.id.category_picture_small, FileCategory.Picture);
        button3Category.put(R.id.category_document_small, FileCategory.Doc);
        button3Category.put(R.id.category_apk_small, FileCategory.Apk);
        button3Category.put(R.id.category_favorite_small, FileCategory.Other);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cricle_progress);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(false);
        
        infoAvailable = (TextView)findViewById(R.id.info_available);
        infoMemory = (TextView)findViewById(R.id.info_memory);
        sdMoneryCard = getIntent().getLongExtra("sdMoneryCard", 0);
        sdMoneryCardused = getIntent().getLongExtra("sdMoneryCardused", 0);
        sdMoneryCards = getIntent().getLongExtra("sdMoneryCards",0);
        sdMoneryFrees = getIntent().getLongExtra("sdMoneryFrees",0);
        sdMoneryCardusedl = getIntent().getLongExtra("sdMoneryCardusedl",0);
        isSdCard = getIntent().getIntExtra("isCard", 1);
        if(isSdCard == GlobalConsts.IS_SD_CARD){
            setTitle(getResources().getString(R.string.sd_info_storage));
            infoMemory.setText(getResources().getString(R.string.sd_card_size,Util.convertStorage(sdMoneryCards)));
            infoAvailable.setText(getResources().getString(R.string.sd_card_available,Util.convertStorage(sdMoneryFrees)));
        }else{
            //*/ add by droi liuhaoran for ROM fake on 20160531
            if(FileManagerApplication.mMemoryCardInfo != 0){
                
                //*/ freeme.liuhaoran , 20160805 , ROM fake
                 if (FileManagerApplication.mIsFeiMa.equals("true")) {
                        if(com.freeme.filemanager.util.featureoption.FeatureOption.TYD_TOOL_TEST_ROM_16G ||com.freeme.filemanager.util.featureoption.FeatureOption.TYD_TOOL_TEST_ROM_32G || 
                                com.freeme.filemanager.util.featureoption.FeatureOption.TYD_TOOL_TEST_ROM_64G || com.freeme.filemanager.util.featureoption.FeatureOption.TYD_TOOL_TEST_ROM_128G){
                            infoMemory.setText(getResources().getString(R.string.memory_size,FeatureOption.FAKE_ROM_SIZE + " GB"));
                            sdMoneryCard = Long.parseLong(FeatureOption.FAKE_ROM_SIZE) * 1024 * 1024 * 1024 / 100000;
                            infoAvailable.setText(getResources().getString(R.string.sd_card_available,Util.convertStorage(sdMoneryFrees + Long.parseLong(FeatureOption.FAKE_ROM_SIZE)*1024*1024*1024 - sdMoneryCards)));
                            Log.i("FeatureOption", "fake ");
                        }else{
                            infoMemory.setText(getResources().getString(R.string.memory_size,Util.convertStorage(sdMoneryCards)));
                            infoAvailable.setText(getResources().getString(R.string.sd_card_available,Util.convertStorage(sdMoneryFrees)));
                            Log.i("FeatureOption", "real ");
                        }
                    }else {
                        infoMemory.setText(getResources().getString(R.string.memory_size,FileManagerApplication.mMemoryCardInfo + " GB"));
                        infoAvailable.setText(getResources().getString(R.string.sd_card_available,Util.convertStorage(sdMoneryFrees + FileManagerApplication.mMemoryCardInfo *1024*1024*1024 - sdMoneryCards)));
                        sdMoneryCard = FileManagerApplication.mMemoryCardInfo * 1024 * 1024 * 1024 / 100000;
                    }
                 //*/
            //*/
                }else {
            infoMemory.setText(getResources().getString(R.string.memory_size,Util.convertStorage(sdMoneryCards)));
            infoAvailable.setText(getResources().getString(R.string.sd_card_available,Util.convertStorage(sdMoneryFrees)));
                }
            setTitle(getResources().getString(R.string.interior_info_storage));
        }
        
        Log.i("infoAvailable", "sdMoneryCards = " + sdMoneryCards);
        mFileCagetoryHelper = new FileCategoryHelper(this);
        refreshCategoryInfo();
        mRoundProgressBar3 = (RoundProgressBar) findViewById(R.id.roundProgressBar3);
        mRoundProgressBar3.setMax(sdMoneryCard,sdMoneryCards,sdMoneryFrees,isSdCard,true);
                new Thread(new Runnable() {
                    
                    @Override
                    public void run() {
                        while(progress <= sdMoneryCardused){
                            progress += 10;
                            mRoundProgressBar3.setProgress(progress);
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        
                    }
                }).start();
    }
    
    private void onItemClick(LinearLayout linearLayout){
        linearLayout.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v) {
                     FileCategory f = button3Category.get(v.getId());
                     Intent intent = new Intent(MoneyInfoActivity.this,MenoryInfoFileListActivity.class);
                     intent.putExtra("category_card", isSdCard);
                     intent.putExtra("category", f);
                     startActivity(intent);
            }});
    }
    
    private void setCategorySize(FileCategory fc, long size) {
        int txtId = 0;
        int resId = 0;
        switch (fc) {
            case Music:
                txtId = R.id.category_legend_music_small;
                resId = R.string.category_music;
                mMusicLinearLayout = (LinearLayout)findViewById(R.id.category_music_small);
                onItemClick(mMusicLinearLayout);
                break;
            case Video:
                txtId = R.id.category_legend_video_small;
                resId = R.string.category_video;
                mVideoLinearLayout = (LinearLayout)findViewById(R.id.category_video_small);
                onItemClick(mVideoLinearLayout);
                break;
            case Picture:
                txtId = R.id.category_legend_picture_small;
                resId = R.string.category_picture;
                mPictureLinearLayout = (LinearLayout)findViewById(R.id.category_picture_small);
                onItemClick(mPictureLinearLayout);
                break;
            case Doc:
                txtId = R.id.category_legend_document_small;
                resId = R.string.category_document;
                mApkLinearLayout = (LinearLayout)findViewById(R.id.category_apk_small);
                onItemClick(mApkLinearLayout);
                break;
            case Apk:
                txtId = R.id.category_legend_apk_small;
                resId = R.string.category_apk;
                mDcLinearLayout = (LinearLayout)findViewById(R.id.category_document_small);
                onItemClick(mDcLinearLayout);
                break;
            case Other:
                txtId = R.id.category_legend_other_small;
                resId = R.string.category_other;
//                mOtherLinearLayout = (LinearLayout)findViewById(R.id.category_favorite_small);
//                onItemClick(mOtherLinearLayout);
                break;
        }

        if (txtId == 0 || resId == 0)
            return;
        setTextView(txtId, Util.convertStorage(size));
    }
    
    //*/ add by freemeos.liuhaoran on 20160714 for refresh category info after delete list item
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        MobclickAgent.onResume(this);
        refreshCategoryInfo();
    }
    //*/

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
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
    private void setTextView(int id, String t) {
        TextView text = (TextView)findViewById(id);
        text.setText(t);
    }
    
    private void setCategoryInfo() {
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            return;
        }
        StorageHelper.MountedStorageInfo mountedStorageInfo = StorageHelper.getInstance(this).getMountedStorageInfo();
        if (mountedStorageInfo != null) {
        //  this.mCategoryBar.setFullValue(mountedStorageInfo.total);
            long size = 0;
            if(FileCategoryHelper.sCategories != null){
                for (FileCategory fc : FileCategoryHelper.sCategories) {
                    CategoryInfo categoryInfo = mFileCagetoryHelper.getCategoryInfos().get(fc);
                    if(fc == FileCategory.Other)
                        continue;
                    setCategorySize(fc, categoryInfo.size);
                    size += categoryInfo.size;
                }
                long otherSize = sdMoneryCardusedl - size;
                Util.convertStorage(size);
                setCategorySize(FileCategory.Other, otherSize);
            }
        }
    }
    public void refreshCategoryInfo() {
        mRefreshCategoryInfoTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object... arg0) {
                mFileCagetoryHelper.refreshCategoryInfo(isSdCard,true);
                return null;
            }

            protected void onPostExecute(Object paramVoid) {
                setCategoryInfo();
            }
        };
        mRefreshCategoryInfoTask.execute(new Void[0]);
    }
}
