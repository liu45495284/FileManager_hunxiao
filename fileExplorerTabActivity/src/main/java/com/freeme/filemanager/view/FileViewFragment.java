/*
 * This file is part of FileManager.
 * FileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * TYD Inc. (C) 2012. All rights reserved.
 */

package com.freeme.filemanager.view;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.storage.StorageVolume;

import com.droi.sdk.analytics.DroiAnalytics;
import com.freeme.filemanager.BuildConfig;
import com.freeme.filemanager.FileExplorerTabActivity;
import com.freeme.filemanager.FileManagerApplication;
import com.freeme.filemanager.FileManagerApplication.SDCardChangeListener;
import com.freeme.filemanager.R;
import com.freeme.filemanager.FileExplorerTabActivity.IBackPressedListener;
import com.freeme.filemanager.controller.ActivitiesManager;
import com.freeme.filemanager.controller.FileListAdapter;
import com.freeme.filemanager.controller.FileViewInteractionHub;
import com.freeme.filemanager.controller.IFileInteractionListener;
import com.freeme.filemanager.controller.FileViewInteractionHub.Mode;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.FileCategoryHelper;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.FileSortHelper;
import com.freeme.filemanager.util.MountHelper;
import com.freeme.filemanager.util.StorageHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.util.Util.MemoryCardInfo;
import com.freeme.filemanager.view.garbage.CleanUpDatabaseHelper;

/*/
 * Modified by droi xueweili for fragment lazy load 20160509
 */
public class FileViewFragment extends BaseFragment implements IFileInteractionListener, IBackPressedListener, SDCardChangeListener {

    public static final String EXT_FILTER_KEY = "ext_filter";

    private static final String LOG_TAG = "FileViewActivity";

    public static final String EXT_FILE_FIRST_KEY = "ext_file_first";

    public static final String ROOT_DIRECTORY = "root_directory";

    public static final String PICK_FOLDER = "pick_folder";
    
    public static final int VIEW_DELAY_LOAD = 0x101;
    
    private ImageButton mVolumeSwitch;
    
    private ListView mFileListView;

    private ArrayAdapter<FileInfo> mAdapter;

    public FileViewInteractionHub mFileViewInteractionHub;

    private FileCategoryHelper mFileCagetoryHelper;

    private FileIconHelper mFileIconHelper;

    private ArrayList<FileInfo> mFileNameList = new ArrayList<FileInfo>();

    private FileExplorerTabActivity mActivity;

    private View mRootView;
    
    private RelativeLayout mGalleryNavigationBar;
    
    private static final String sdDir = Util.getSdDirectory();
    
    private String mVolumeDescription;
    
    private String mVolumePath;
    //add by mingjun for load file anr
    private String mTagPath=null;
    ProgressDialog loadDialog;
    private int isLayout=0;
    private Handler handler;
    //end
    //*/modify by droi liuhaoran for stop run
    /*/ Added by tyd wulianghuan 2013-12-12
    private CleanUpDatabaseHelper mCleanUpDatabaseHelper;
    private SQLiteDatabase mDatabase;
    private HashMap<String, String> mFolderNameMap;
    //*/
    private boolean isFirst=true;
    //*/
    
    //*/add by droi liuhaoran for get Sd listener on 20160419
    private FileManagerApplication mApplication;
    //*/

    private boolean mReceiverTag = false;
    
    private boolean isInit = false;
    
    // memorize the scroll positions of previous paths
    private ArrayList<PathScrollPositionItem> mScrollPositionList = new ArrayList<PathScrollPositionItem>();
    private String mPreviousPath;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(LOG_TAG, "received broadcast:" + intent.toString());
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                //*/add by droi liuhaoran for get Sd listener to refresh progressBar and file list on 20160419
                notifyFileChanged(true);
                //showFile();
            }else if(action.equals(Intent.ACTION_MEDIA_EJECT) || action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL) || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)){
                //invisableFile();
                notifyFileChanged(false);
                //*/
            }
            else if(action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)){
                updateUI();
                
            }else if(action.equals(GlobalConsts.BROADCAST_REFRESH)){
                if(intent.getIntExtra(GlobalConsts.BROADCAST_REFRESH_EXTRA, -1) == GlobalConsts.BROADCAST_REFRESH_TABVIEW){
                    updateUI();
                }
            }
        }
    };
    //private refreshFileListTask mRefreshFileListTask;
    private boolean mBackspaceExit;

    
    private Handler mFileViewHandler = new Handler(){
        
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
            case VIEW_DELAY_LOAD:
                init();
                isInit = true;
                break;

            default:
                break;
            }
            
        };
    };

    @Override
    public View onFragmentCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mActivity = ((FileExplorerTabActivity)getActivity());
        mRootView = inflater.inflate(R.layout.file_explorer_stub, container, false);
        mFileViewHandler.sendEmptyMessageDelayed(VIEW_DELAY_LOAD,350);
        return mRootView;
    }
    @Override
    public void initUserData() {
        super.initUserData();
    }
    
    public void init(){
        long time1 = System.currentTimeMillis();
        //Debug.startMethodTracing("file_view");
        ViewStub stub = (ViewStub) mRootView.findViewById(R.id.viewContaniner);
        stub.setLayoutResource(R.layout.file_explorer_list);
        stub.inflate();
        ActivitiesManager.getInstance().registerActivity(
                ActivitiesManager.ACTIVITY_FILE_VIEW, mActivity);

        mFileCagetoryHelper = new FileCategoryHelper(mActivity);
        mFileViewInteractionHub = new FileViewInteractionHub(this, 1);
        // */ modify by droi liuhaoran for stop run
        /*/ Added by tyd wulianghuan 2013-12-12
        mCleanUpDatabaseHelper = new CleanUpDatabaseHelper(mActivity);
        mDatabase = mCleanUpDatabaseHelper.openDatabase();
        mFolderNameMap = new HashMap<String, String>();
        //*/ 

        // */add by droi liuhaoran for get Sd listener on 20160419
        mApplication = (FileManagerApplication) mActivity.getApplication();
        // */

        // notifyFileChanged();
        Intent intent = mActivity.getIntent();
        String action = intent.getAction();
        if (!TextUtils.isEmpty(action)
                && (action.equals(Intent.ACTION_PICK) || action
                        .equals(Intent.ACTION_GET_CONTENT))) {
            mFileViewInteractionHub.setMode(Mode.Pick);

            boolean pickFolder = intent.getBooleanExtra(PICK_FOLDER, false);
            if (!pickFolder) {
                String[] exts = intent.getStringArrayExtra(EXT_FILTER_KEY);
                if (exts != null) {
                    mFileCagetoryHelper.setCustomCategory(exts);
                }
            } else {
                mFileCagetoryHelper.setCustomCategory(new String[] {} /*
                                                                     * folder
                                                                     * only
                                                                     */);
                mRootView.findViewById(R.id.pick_operation_bar).setVisibility(
                        View.VISIBLE);

                mRootView.findViewById(R.id.button_pick_confirm)
                        .setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                try {
                                    Intent intent = Intent.parseUri(
                                            mFileViewInteractionHub
                                                    .getCurrentPath(), 0);
                                    mActivity.setResult(Activity.RESULT_OK,
                                            intent);
                                    mActivity.finish();
                                } catch (URISyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                mRootView.findViewById(R.id.button_pick_cancel)
                        .setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                mActivity.finish();
                            }
                        });
            }
        } else {
            mFileViewInteractionHub.setMode(Mode.View);
        }
        mVolumeSwitch = (ImageButton) mRootView
                .findViewById(R.id.volume_navigator);
        updateVolumeSwitchState();
        mGalleryNavigationBar = (RelativeLayout) mRootView
                .findViewById(R.id.gallery_navigation_bar);
        mVolumeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int visibility = getVolumesListVisibility();
                if (visibility == View.GONE) {
                    buildVolumesList();
                    showVolumesList(true);
                } else if (visibility == View.VISIBLE) {
                    showVolumesList(false);
                }
            }

        });
        mFileListView = (ListView) mRootView.findViewById(R.id.file_path_list);
        mFileIconHelper = new FileIconHelper(mActivity);
        mAdapter = new FileListAdapter(mActivity, R.layout.file_browser_item,
                mFileNameList, mFileViewInteractionHub, mFileIconHelper);

        boolean baseSd = intent.getBooleanExtra(GlobalConsts.KEY_BASE_SD,
                !FileExplorerPreferenceActivity.isReadRoot(mActivity));
        Log.i(LOG_TAG, "baseSd = " + baseSd);

        String rootDir = intent.getStringExtra(ROOT_DIRECTORY);
        if (!TextUtils.isEmpty(rootDir)) {
            if (baseSd && this.sdDir.startsWith(rootDir)) {
                rootDir = this.sdDir;
            }
        } else {
            rootDir = baseSd ? this.sdDir : GlobalConsts.ROOT_PATH;
        }

        String currentDir = FileExplorerPreferenceActivity
                .getPrimaryFolder(mActivity);
        Uri uri = intent.getData();
        if (uri != null) {
            if (baseSd && this.sdDir.startsWith(uri.getPath())) {
                currentDir = this.sdDir;
            } else {
                currentDir = uri.getPath();
            }
        }
        initVolumeState();
        mBackspaceExit = (uri != null)
                && (TextUtils.isEmpty(action) || (!action
                        .equals(Intent.ACTION_PICK) && !action
                        .equals(Intent.ACTION_GET_CONTENT)));

        mFileListView.setAdapter(mAdapter);
        IntentFilter intentFilter = new IntentFilter();
        // add by xueweili for get sdcard
        intentFilter.setPriority(1000);

        /*
         * intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
         * intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
         * intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
         * intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
         * intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
         */
        if (!mReceiverTag) {
            mReceiverTag = true;
            intentFilter.addAction(GlobalConsts.BROADCAST_REFRESH);
            intentFilter.addDataScheme("file");
            mActivity.registerReceiver(mReceiver, intentFilter);
        }

        // */add by droi liuhaoran for get Sd listener on 20160419
        mApplication.addSDCardChangeListener(this);
        // */

        setHasOptionsMenu(true);

        // add by mingjun for load file
        mRootView.addOnLayoutChangeListener(new OnLayoutChangeListener() {

            @Override
            public void onLayoutChange(View arg0, int arg1, int arg2, int arg3,
                    int arg4, int arg5, int arg6, int arg7, int arg8) {
                isLayout = arg1;
            }
        });
        loadDialog = new ProgressDialog(mRootView.getContext());
    }
    
    private void buildVolumesList(){
        LinearLayout linearyLayout = (LinearLayout) mRootView.findViewById(R.id.dropdown_navigation_list);
        linearyLayout.removeAllViews();
        
        ArrayList mountVolumeList = StorageHelper.getInstance(mActivity).getSortedMountVolumeList();
        if (mountVolumeList == null || mountVolumeList.size()==0){
            return;
        }
        
        Iterator iterator = mountVolumeList.iterator();
        while (iterator.hasNext()) {
            storageVolume = (StorageVolume) iterator.next();
            //Log.i("liuhaoran", "storageVolume = " + storageVolume);
            //*/ freeme.liuhaoran , 20160728 , adapter zhanxun M
            if(storageVolume.getStorageId() != 0){
                linearyLayout.addView(createStorageVolumeItem(storageVolume.getPath(), storageVolume.getDescription(mActivity)));
            }
            //*/
        }
    }
    
    private String  internalPath = Util.getDefaultPath();
    
    private View createStorageVolumeItem(final String volumPath,String volumDescription) {

        View listItem = LayoutInflater.from(mActivity).inflate(R.layout.dropdown_item, null);
        View listContent = listItem.findViewById(R.id.list_item);
        ImageView img = (ImageView) listItem.findViewById(R.id.item_icon);
        TextView text = (TextView) listItem.findViewById(R.id.path_name);
        text.setText(volumDescription);
        
        //*/ freeme.liuhaoran , 20160728 , volumeItem image
        /*/
        img.setImageResource(getStorageVolumeIconByDescription(volumDescription));
        //*/
        Log.i("liuhaoran3" ,"storageVolume.getPath() = " + storageVolume.getPath() );
        Log.i("liuhaoran3" ,"internalPath = " + internalPath);
        if (storageVolume.getPath().equals(internalPath)) {
            img.setImageDrawable((getResources().getDrawable(R.drawable.storage_internal_n)));
        }else if ((storageVolume.getDescription(mActivity).toString()).contains("SD")) {
            img.setImageDrawable((getResources().getDrawable(R.drawable.storage_sd_card_n)));
        }else if (storageVolume.getDescription(mActivity).toString().contains("usbotg")) {
            img.setImageDrawable((getResources().getDrawable(R.drawable.storage_usb_n)));
        }
        //*/
        
        //modigy by droi heqianqian if the stroage device is not phone memeory, then set the storage could be unmoumt
        ImageView unmount_btn = (ImageView) listItem.findViewById(R.id.unmount_btn);
        if(volumPath.equals(Util.SD_DIR)){
            //*/ freeme.liuhaoran , 20160802 , judge whether there is a SD card operation permissions
            if(ContextCompat.checkSelfPermission(mActivity, "android.permission.MOUNT_UNMOUNT_FILESYSTEMS") == PackageManager.PERMISSION_GRANTED){
            unmount_btn.setVisibility(View.VISIBLE);
            unmount_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                        MountHelper.getInstance(mActivity).unMount(volumPath);
                        showVolumesList(false);
                        mVolumeSwitch.setVisibility(View.GONE);
                        int mounedCount = StorageHelper.getInstance(mActivity).getMountedVolumeCount();
                    }
            });
        }else {
            unmount_btn.setVisibility(View.INVISIBLE);
        }
        //*/
        }
        listItem.setOnClickListener(mStorageVolumeClick);
        listItem.setTag(new Pair(volumPath, volumDescription));
        return listItem;
    }
    
    private View.OnClickListener mStorageVolumeClick = new View.OnClickListener(){
        @Override
        public void onClick(View paramView)
        {
            showVolumesList(false);
            Pair localPair = (Pair) paramView.getTag();
            if (((String) localPair.first).equals(mVolumePath)){
                return;
            }
            mVolumePath = (String) localPair.first;
            mVolumeDescription = (String) localPair.second;
            mFileViewInteractionHub.setRootPath(mVolumePath);
            mFileViewInteractionHub.exitActionMode();
            updateUI();
        }
    };
    
    /*/
    private int getStorageVolumeIconByDescription(String description) {
        Log.i("liuhaoran", "description = " + description.toString());
        if (description.equals(this.mActivity.getString(R.string.storage_phone))){
            return R.drawable.storage_internal_n;}
        if (description.equals(this.mActivity.getString(R.string.storage_sd_card))){
            return R.drawable.storage_sd_card_n;}
        if (description.equals(this.mActivity.getString(R.string.storage_external_usb))){
            return R.drawable.storage_usb_n;}
        return -1;
    }
    //*/
    
    private int getVolumesListVisibility() {
        return this.mRootView.findViewById(R.id.dropdown_navigation).getVisibility();
    }
    
    private void showVolumesList(boolean show) {
        View view = mRootView.findViewById(R.id.dropdown_navigation);
        view.setVisibility(show==true? View.VISIBLE : View.GONE);
    }
    
    private void initVolumeState() {
        LoadDataTask loadData = new LoadDataTask();
        loadData.execute();
    }
    
    private void initVolumeState(StorageVolume paramStorageVolume) {
        if (paramStorageVolume == null){
            return;
        }
        this.mVolumePath = paramStorageVolume.getPath();
        this.mVolumeDescription = paramStorageVolume.getDescription(this.mActivity);
        //*/ freeme.liuhaoran , 20160728 , adapter zhanxun M
        if (paramStorageVolume.getStorageId() == 0) {
            mVolumeDescription = getString(R.string.storage_phone);
        }
        //*/
        Log.i("liuhaoran", "mVolumeDescription = " + mVolumeDescription  + "----" + paramStorageVolume.getStorageId());
        if(mFileViewInteractionHub ==null){
        mFileViewInteractionHub = new FileViewInteractionHub(this, 1);
        }
        mFileViewInteractionHub.setRootPath(this.mVolumePath);
    }
    //add by mingjun 2015-15-26 for refreshfile
     @Override
    public void fragmentShow() {
        super.fragmentShow();
    }
    //end
    @Override
    public void fragmentHint() {
        super.fragmentHint();
        /*/ modify by droi liuhaoran for stop run on 20160604
        mDatabase.close();
        //*/
    }
    
    //*/ add by freemeos.liuhaoran on 20160711 for filelist not refresh after delete form the music page
    String mPageName = "page1";
    @Override
    public void onResume() {
        super.onResume();
        DroiAnalytics.onFragmentStart(getActivity(), mPageName);
        refresh();
    }
    //*/

    @Override
    public void onPause() {
        super.onPause();
        DroiAnalytics.onFragmentEnd(getActivity(), mPageName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReceiverTag) {
            mReceiverTag = false;
            mActivity.unregisterReceiver(mReceiver);
            /*/add by droi liuhaoran for get Sd listener on 20160419
            mApplication.removeSDCardChangeListener(this);
            //*/
        }
        //*/ Added by tyd wulianghuan 2013-12-26 for fix NullPointException
        if(mFileViewInteractionHub != null){
            // keep the progressDialog's lifecycle is consistent with mActivity
            mFileViewInteractionHub.DismissProgressDialog();
            // to cancel the doing works for this context is destroyed
            //modified by mingjun 20150526 for data load anr
            //mFileViewInteractionHub.onOperationButtonCancel();
        }
        //*/
        
        if(mApplication != null){
            mApplication.removeSDCardChangeListener(this);
        }
        
    }

    private Menu optionMenu = null;
    private boolean isOperate=false;

    private StorageVolume storageVolume;
    
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mFileViewInteractionHub.onPrepareOptionsMenu(menu);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mFileViewInteractionHub.onCreateOptionsMenu(menu);
        onRefreshMenu(isOperate);
        optionMenu = menu;

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onBack() {
        //*/modify by droi liuhaoran for pressed onback to home
        /*/modified by mingjun for copy ui back
        if (mBackspaceExit || mFileViewInteractionHub == null) {
            return false;
        }
        //*/
      if (mBackspaceExit ) {
      return false;
  }
      //*/
      
        //*/modify by droi liuhaoran for delete the dir of mtklog,fileManager stop run after press the back on 20160423
        if(mPreviousPath != null){
        if((mPreviousPath.equals(Util.SD_DIR)||mPreviousPath.equals(Util.MEMORY_DIR))&&isOperate){
            mFileViewInteractionHub.onOperationButtonCancel();
            mFileViewInteractionHub.showConfirmOperationBar(false);
            return true;
        }
        }
        
        if(mFileViewInteractionHub != null){
            return mFileViewInteractionHub.onBackPressed();
        }
        return false;
        //*/
    }

    private class PathScrollPositionItem {
        String path;
        int pos;
        PathScrollPositionItem(String s, int p) {
            path = s;
            pos = p;
        }
    }
    
    @Override
    public void showPathGalleryNavbar(boolean show){
        if(mGalleryNavigationBar != null){
            mGalleryNavigationBar.setVisibility(show == true? View.VISIBLE : View.GONE);
        }
    }
    
    private void updateVolumeSwitchState(){
        //*/ modify by droi liuhaoran for updateVolumeList
        if(mVolumeSwitch != null){
            int mounedCount = StorageHelper.getInstance(mActivity).getMountedVolumeCount();
            ArrayList<StorageVolume> mountVolumeList = StorageHelper.getInstance(mActivity).getSortedMountVolumeList();
            //*/ freeme.liuhaoran , 20160728 , adapter zhanxun M
            ArrayList<StorageVolume> volumeList = new ArrayList<StorageVolume>();
            for (int i = 0; i < mountVolumeList.size(); i++) {
                StorageVolume localStorageVolume = mountVolumeList.get(i);
                if (localStorageVolume.getStorageId() != 0) {
                    volumeList.add(localStorageVolume);
                    Log.i("liuhaoran", "volumeList =" + volumeList.size());
                }
            }
            if(volumeList.size() > 1){
            //*/
                mVolumeSwitch.setVisibility(View.VISIBLE);
            }else{
                /*/
                Util.SD_DIR = "aaaaa";
                //*/
                mVolumeSwitch.setVisibility(View.GONE);
            }
        }
    }
//*/
    private int computeScrollPosition(String path) {
        int pos = 0;
        if(mPreviousPath!=null) {
            if (path.startsWith(mPreviousPath)) {
                int firstVisiblePosition = mFileListView.getFirstVisiblePosition();
                if (mScrollPositionList.size() != 0
                        && mPreviousPath.equals(mScrollPositionList.get(mScrollPositionList.size() - 1).path)) {
                    mScrollPositionList.get(mScrollPositionList.size() - 1).pos = firstVisiblePosition;
                    pos = firstVisiblePosition;
                } else {
                    mScrollPositionList.add(new PathScrollPositionItem(mPreviousPath, firstVisiblePosition));
                }
            } else {
                int i;
                boolean isLast = false;
                for (i = 0; i < mScrollPositionList.size(); i++) {
                    if (!path.startsWith(mScrollPositionList.get(i).path)) {
                        break;
                    }
                }
                if (i > 0) {
                    pos = mScrollPositionList.get(i - 1).pos;
                }

                for (int j = mScrollPositionList.size() - 1; j >= i-1 && j>=0; j--) {
                    mScrollPositionList.remove(j);
                }
            }
        }

        mPreviousPath = path;
        return pos;
    }

    @Override
    public boolean onRefreshFileList(String path, FileSortHelper sort) {
        if(optionMenu != null){
            mFileViewInteractionHub.onPrepareOptionsMenu(optionMenu);
        }
        //*/ Added by tyd wulinaghuan 2013-12-09 for: fix NullPointerException bug
        if(TextUtils.isEmpty(path)){
            return false;
        }
        //*/
        final File file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            return false;
        }
        final int pos = computeScrollPosition(path);
        final ArrayList<FileInfo> fileList = mFileNameList;
        fileList.clear();


       //modify by tyd mingjun 20150526 for refresh slowly
        if(isFirst){
            isFirst=false;
             handler = new Handler() {
                public void handleMessage(Message msg) {
                       if (msg.what == 0&&fileList.isEmpty() ) {
                        if(isLayout==0&&loadDialog!=null){
                        if(mTagPath!=null){
                            if(!mTagPath.equals(file.getAbsolutePath())){
                                loadDialog.show();
                            }
                            }else{
                            Intent intent = new Intent();
                            intent.setClassName(BuildConfig.APPLICATION_ID, "FileExplorerTabActivity");
                            if (mActivity.getPackageManager().resolveActivity(intent, 0) != null) {
                                loadDialog.show();
                            }
                            }}
                        }
                };
            };
           new Thread(new ThreadShow()).start();
        LoadlistDataTask listData = new LoadlistDataTask(file,fileList,sort,pos);
        listData.execute();
        }else{
            File[] listFiles = file.listFiles(mFileCagetoryHelper.getFilter());
              if (listFiles == null)
                  return true;
         //modify by tyd liuyong 20140504 for refresh slowly
        for (File child : listFiles) {
            // do not show selected file if in move state

            if (mFileViewInteractionHub.inMoveState() && mFileViewInteractionHub.isFileSelected(child.getPath()))
                continue;
              String absolutePath = child.getAbsolutePath();
            if (Util.isNormalFile(absolutePath) && Util.shouldShowFile(absolutePath)) {
                FileInfo lFileInfo = Util.GetFileInfo(child,
                        mFileCagetoryHelper.getFilter(), Settings.instance().getShowDotAndHiddenFiles());
                if (lFileInfo != null) {
                    fileList.add(lFileInfo);
                }
            }

        }
        mTagPath = file.getAbsolutePath();
        sortCurrentList(sort);
        showEmptyView(fileList.size() == 0);
        mFileListView.post(new Runnable() {
            @Override
            public void run() {
                mFileListView.setSelection(pos);
            }
        });
        }
        
        // add by xueweili for listview to check on 20160407
        //mFileListView.requestFocusFromTouch();
        //end
        return true;
    }

    @Override
    public void onRefreshMenu(boolean operate) {
        isOperate=operate;
        if(optionMenu != null){
            if(operate){
                optionMenu.removeItem(16);
                optionMenu.removeItem(100);
                optionMenu.removeItem(117);
                optionMenu.removeItem(15);
                optionMenu.removeItem(102);
            }else{
                mFileViewInteractionHub.addMenuItems(optionMenu);
            }

            mFileViewInteractionHub.onPrepareOptionsMenu(optionMenu); 
        }
    }
    private void updateUI() {
        showVolumesList(false);
        boolean isCurMounted = StorageHelper.getInstance(mActivity).isCurrentVolumeMounted();
        if(!isCurMounted){
            return;
        }
        showPathGalleryNavbar(true);
        showListView(true);
        showMemoryNotAvailable(false, null);
        mFileViewInteractionHub.refreshFileList();
        
        //*/ Added by Tyd Linguanrong for [tyd00520064] refresh option menu, 2014-5-8
        mActivity.invalidateOptionsMenu();
        //*/
    }
    
    //*/ modify by droi liuhaoran for get Sd listener to refresh listener on 20160419
    private void notifyFileChanged(Boolean flag){
        showVolumesList(false);
        updateVolumeSwitchState();
        //boolean isCurMounted = StorageHelper.getInstance(mActivity).isCurrentVolumeMounted();
        if(flag){
            if(isMemoryNotAvailableShow()){
                initVolumeState();
                showPathGalleryNavbar(true);
                showListView(true);
                showMemoryNotAvailable(false, null);
            }
        }else{
            //*/ freeme.liuhaoran , 20160802 , adapter SD state
            String state = Util.getDefaultState();
            if(!state.equals(Environment.MEDIA_MOUNTED)){
            //*/
                showPathGalleryNavbar(false);
                showListView(false);
                showMemoryNotAvailable(true, mActivity.getString(R.string.storage_device_umouonted));
            }else{
                initVolumeState();
                showPathGalleryNavbar(true);
                showListView(true);
                showMemoryNotAvailable(false, null);
            }
        }
        
    }
    //*/
    
    //*/add by tyd sxp 20140909 for unmount error
    private void invisableFile()
    {
        showVolumesList(false);
        updateVolumeSwitchState();
        if(!isMemoryNotAvailableShow()){
        showPathGalleryNavbar(false);
        showListView(false);
        showMemoryNotAvailable(true, mActivity.getString(R.string.storage_device_umouonted));
        }
    }
    
    private void showFile()
    {
        showVolumesList(false);
        updateVolumeSwitchState();
        if(isMemoryNotAvailableShow()){
            initVolumeState();
            showPathGalleryNavbar(true);
            showListView(true);
            showMemoryNotAvailable(false, null);
        }
    }
    //*/end
    
    private void showEmptyView(boolean show) {
        View emptyView = mRootView.findViewById(R.id.empty_view);
        if (emptyView != null){
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    private void showMemoryNotAvailable(boolean show, String text){
        TextView view = (TextView)mRootView.findViewById(R.id.memory_not_available_page);
        if (view != null){
            if(show){
                showEmptyView(false);
                view.setText(text);
                view.setVisibility(View.VISIBLE);
            }else{
                view.setVisibility(View.GONE);
            }
        }
    }
    
    private boolean isMemoryNotAvailableShow(){
        TextView view = (TextView)mRootView.findViewById(R.id.memory_not_available_page);
        return view.getVisibility() == View.VISIBLE;
    }

    private void showListView(boolean show){
        if(mFileListView != null){
            mFileListView.setVisibility(show == true? View.VISIBLE: View.GONE);
        }
    }
    
    @Override
    public View getViewById(int id) {
        return mRootView.findViewById(id);
    }

    @Override
    public Context getContext() {
        return mActivity;
    }

    @Override
    public void onDataChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onPick(FileInfo f) {
        try {
            Intent intent = Intent.parseUri(Uri.fromFile(new File(f.filePath)).toString(), 0);
            mActivity.setResult(Activity.RESULT_OK, intent);
            mActivity.finish();
            return;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean shouldShowOperationPane() {
        return true;
    }

    @Override
    public boolean onOperation(int id) {
        return false;
    }
   
    @Override
    public String getDisplayPath(String paramString) {
        if (paramString.startsWith(this.mVolumePath))
            paramString = this.mVolumeDescription + paramString.substring(this.mVolumePath.length());
        return paramString;
    }

    @Override
    public String getRealPath(String paramString) {
        if (paramString.startsWith(this.mVolumeDescription))
            paramString = this.mVolumePath + paramString.substring(this.mVolumeDescription.length());
        return paramString;
    }
    
    @Override
    public boolean shouldHideMenu(int menu) {
        return false;
    }

    public void copyFile(ArrayList<FileInfo> files) {
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.onOperationCopy(files);
        }
    }

    public void refresh() {
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.refreshFileList();
        }
    }

    public void moveToFile(ArrayList<FileInfo> files) {
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.moveFileFrom(files);
        }
        
    }

    public interface SelectFilesCallback {
        // files equals null indicates canceled
        void selected(ArrayList<FileInfo> files);
    }

    public void startSelectFiles(SelectFilesCallback callback) {
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.startSelectFiles(callback);
        }
    }

    @Override
    public FileIconHelper getFileIconHelper() {
        return mFileIconHelper;
    }

    public boolean setPath(String location) {
        Log.i("onNewIntent", "location:"+location);
        StorageVolume storageVolume = Util.getMountedStorageBySubPath(mActivity, location);
        if (storageVolume == null){
            return false;
        }
        initVolumeState(storageVolume);
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.setCurrentPath(location);
            mFileViewInteractionHub.refreshFileList();
        }
        return true;
    }

    @Override
    public FileInfo getItem(int pos) {
        if (pos < 0 || pos > mFileNameList.size() - 1)
            return null;
        return mFileNameList.get(pos);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sortCurrentList(final FileSortHelper sort) {
      //*/ Modified for Droi Kimi Wu on 20160310 debug for adapter changed but listview not receive a notification.
        /*/Collections.sort(mFileNameList, sort.getComparator());
        onDataChanged();
        //*/
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Collections.sort(mFileNameList, sort.getComparator());
                mAdapter.notifyDataSetChanged();
            }
        });
        //*/
    }


    @Override
    public ArrayList<FileInfo> getAllFiles() {
        return mFileNameList;
    }

    @Override
    public void addSingleFile(final FileInfo file) {
      //*/ Modified for Droi Kimi Wu on 20160310 debug for adapter changed but listview not receive a notification.
        /*/mFileNameList.add(file);
        onDataChanged();
        //*/
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFileNameList.add(file);
                mAdapter.notifyDataSetChanged();
            }
        });
        //*/
    }
        

    @Override
    public int getItemCount() {
        return mFileNameList.size();
    }

    @Override
    public void runOnUiThread(Runnable r) {
        mActivity.runOnUiThread(r);
    }
    
    @Override
    public void hideVolumesList() {
        if(View.VISIBLE == getVolumesListVisibility())
        {
            showVolumesList(false);
        }
    }
    //add by mingjun 20150526 for  data  load anr
    class LoadDataTask extends AsyncTask<Void,Integer,StorageVolume>{  
        private Context context;  
      
        @Override  
        protected StorageVolume doInBackground(Void... params) {  
            StorageVolume data = StorageHelper.getInstance(mActivity).getLatestMountedVolume();
            return data;  
        }  
        @Override  
        protected void onPostExecute(StorageVolume integer) {        
            initVolumeState(integer);
        }  
    }  
    class LoadlistDataTask extends AsyncTask<Void,Integer,ArrayList<FileInfo>>{  
        private Context context;  
        File datafileList;
        ArrayList<FileInfo> fileList;
        FileSortHelper msort;
        int pos;
        public LoadlistDataTask(File file, ArrayList<FileInfo> fileLists, FileSortHelper sort, int mpos) {
            // TODO Auto-generated constructor stub
            datafileList = file;
            fileList=fileLists;
            msort = sort;
            pos =mpos;
            

        }
        
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
               loadDialog.setMessage(mActivity.getResources().getString(R.string.load_data));
//             loadDialog.setIndeterminate(true);
               loadDialog.setCancelable(false);
            if(mTagPath!=null){
             if(!mTagPath.equals(datafileList.getAbsolutePath())){
                fileList.clear();
                sortCurrentList(msort);
            }}
          }
        @Override  
        protected ArrayList<FileInfo> doInBackground(Void... params) {  
                   ArrayList<FileInfo> fileLists = new  ArrayList<FileInfo>();
                File[] listFiles = datafileList.listFiles(mFileCagetoryHelper.getFilter());
                
            if (listFiles != null) {
                for (File child : listFiles) {
                    if (mFileViewInteractionHub.inMoveState()
                            && mFileViewInteractionHub.isFileSelected(child
                                    .getPath()))
                        continue;
                    String absolutePath = child.getAbsolutePath();
                    if (Util.isNormalFile(absolutePath)
                            && Util.shouldShowFile(absolutePath)) {
                        FileInfo lFileInfo = Util.GetFileInfo(child,
                                mFileCagetoryHelper.getFilter(), Settings
                                        .instance().getShowDotAndHiddenFiles());

                        if (lFileInfo != null) {
                            fileLists.add(lFileInfo);
                        }
                    }
                   
               }
            }else{
                   this.cancel(true);
               }
            return fileLists;  
        }  
        @Override
        protected void onProgressUpdate(Integer... values) {
            // TODO Auto-generated method stub
            super.onProgressUpdate(values);
//             loadDialog.show();
        }
        @Override  
        protected void onPostExecute(ArrayList<FileInfo> integer) {  
            mTagPath = datafileList.getAbsolutePath();
            loadDialog.cancel();
            if(fileList.isEmpty()){
            for(int i=0;i<integer.size();i++){
                fileList.add(integer.get(i));
            }}
            fileList=integer;
               sortCurrentList(msort);
               showEmptyView(fileList.size() == 0);
               mFileListView.post(new Runnable() {
                   @Override
                   public void run() {
                       mFileListView.setSelection(pos);
                   }
               });

            mFileViewInteractionHub.setRefresh(true);
        }  
    } 
    class ThreadShow implements Runnable {  
          int time = 1;
               @Override  
               public void run() {  
                    // TODO Auto-generated method stub  
                    while (time>=0) {  
                        try {  
                           Thread.sleep(500);  
                            Message msg = new Message();  
                            msg.what = time--;  
                            handler.sendMessage(msg);  
                            System.out.println("send...");  
                        } catch (Exception e) {  
                           // TODO Auto-generated catch block  
                           e.printStackTrace();  
                           System.out.println("thread error...");  
                        }  
                    }  
                } 
             
        } 
    //end

    public void exitActionMode() {
        // TODO Auto-generated method stub
        
    }
    
    //*/ add by droi liuahoran for get Sd listener to refresh progressBar on 20160419
    @Override
    public void onMountStateChange(int flag) {
        if(flag == SDCardChangeListener.flag_INJECT){
            notifyFileChanged(true);
        }else{
            notifyFileChanged(false);
        }
    }
    //*/
    @Override
    public void finish() {
        // TODO Auto-generated method stub
        if(mRootView != null){
            mActivity.finish();
            }else {
                return;
            }
    }

    @Override
    public void initHotKnot() {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void setHotKnot() {
        // TODO Auto-generated method stub
        
    }
}

