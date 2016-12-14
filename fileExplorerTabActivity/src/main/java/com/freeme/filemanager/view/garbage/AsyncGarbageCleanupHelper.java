package com.freeme.filemanager.view.garbage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.util.FeatureOption;

import android.app.ActivityManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageStats;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class AsyncGarbageCleanupHelper {
    private static final String LOG_TAG = "AsyncGarbageCleanupHelper";
    public static final int ACTION_APPGARBAGE = 4;
    public static final int ACTION_CACHE = 2;
    public static final int ACTION_EMPTYDIR = 3;
    public static final int ACTION_TEMPFILE = 0;
    public static final int ACTION_THUMBNAIL = 1;

    public static final int STATE_DONE = 0;
    public static final int STATE_START_SCAN = 1;
    public static final int STATE_SCAN_FINISH = 2;
    public static final int STATE_START_CLEANUP = 3;
    public static final int STATE_CLEANUP_FINISH = 4;

    public static final int TYPE_APPGARBAGE_ITEM = 5;
    public static final int TYPE_APP_GARBAGE = 4;
    public static final int TYPE_CACHE = 2;
    public static final int TYPE_EMPTY_DIR = 3;
    public static final int TYPE_TEMP_FILE = 0;
    public static final int TYPE_THUMBNAIL = 1;
    private ArrayList<Integer> mActionList;
    private List mAppGarbageCleanupList;
    public List mAppGarbageList = null;
    private int mApplicationInfoCount;
    private List mApplicationInfoList;
    private Map mApplicationInfoMap;
    private Map<String, Long> mApplicationSizeMap;
    private AtomicInteger mCacheCleanupObserverCount;
    private GarbageCleanupItem mCacheCleanupStatusItem;
    private AtomicInteger mCacheSizeObserverCount;
    private Context mContext;
    private int mDeletedFileCount = 0;
    private long mDeletedFileSize = 0;
    private String mExternalPath;
    private int mFileCount;
    private long mFileSize;
    private String mInternalPath;
    private GarbageCleanupStatesListener mListener;
    private Object mLock;
    private PackageManager mPackageManager;
    private boolean mRunning;
    public boolean mSemaphore = false;
    private int mState;
    

    private final IPackageDataObserver.Stub mCacheCleanupObserver = new IPackageDataObserver.Stub() {
        @Override
        public void onRemoveCompleted(String s, boolean flag)
                throws RemoteException {
            mCacheCleanupObserverCount.addAndGet(1);
            if (flag && (mApplicationSizeMap.get(s) > 0)) {
                synchronized (mCacheCleanupStatusItem) {
                    mDeletedFileCount += 1;
                    mDeletedFileSize += mApplicationSizeMap.get(s);
                }
                mSemaphore = true;
            }
        }
    };

    public AsyncGarbageCleanupHelper() {
    }

    public AsyncGarbageCleanupHelper(Context context) {
        mLock = new Object();
        mCacheSizeObserverCount = new AtomicInteger();
        mCacheCleanupObserverCount = new AtomicInteger();
        mSemaphore = false;
        mContext = context;
        mPackageManager = mContext.getPackageManager();
        //modify by tyd liuyong 20140806 for kk storage
        if (FeatureOption.MTK_MULTI_STORAGE_SUPPORT) {
            mInternalPath = Util.MEMORY_DIR;
            mExternalPath = Util.SD_DIR;
        }else{
            mInternalPath = Util.MEMORY_DIR;            
        }
        mActionList = new ArrayList();
        mState = 0;
    }

    public class GarbageCleanupThread extends Thread {
        public GarbageCleanupThread() {

        }

        public void run() {
            operateAction();
            if (mState == STATE_START_SCAN) {
                mState = STATE_SCAN_FINISH;
            } else if (mState == STATE_START_CLEANUP) {
                mState = STATE_CLEANUP_FINISH;
            }
            mListener.onUpdateUI(mState);
            mRunning = false;
        }

        private void operateAction() {
         //*/modified by mingjun for clean up
            if (mActionList != null && mActionList.size() > 0) {
                for (int i = 0; i < 5; i++) {
                    CleanupItemInfo cleanupiteminfo = null;
                    if (mState == STATE_START_SCAN) {                       
                        for(int j=0;j<mActionList.size();j++){
                            if(i==mActionList.get(j)){
                                cleanupiteminfo = execute(mActionList.get(j), false);
                            }
                        }
                        
                    } else if (mState == STATE_START_CLEANUP) {
                        for(int j=0;j<mActionList.size();j++){
                            if(i==mActionList.get(j)){
                                cleanupiteminfo = execute(i, true);
                            }
                        }
                    }
         //*/       
                    if (cleanupiteminfo != null) {
                        mFileCount = cleanupiteminfo.fileCount;
                        mFileSize = cleanupiteminfo.fileSize;
                        mListener.onFinish(i, mFileSize, mFileCount);
                        
                    } else {
                        mListener.onFinish(i, 0, 0);
                    }
                }
            }
        }
    }

    private CleanupItemInfo execute(int index, boolean delete) {
        if (!mRunning) {
            return null;
        }
        switch (index) {
        case 0:
            return systemTempFileCleanup(delete);
        case 1:
            return thumbnailCleanup(delete);
        case 2:
            return cacheCleanup(delete);
        case 3:
            return emptyDirCleanup(delete);
        case 4:
            return appGarbageCleanup(delete);
        default:
            return null;
        }
    }

    public class GarbageItem {
        public String appName;
        public String path;
        public String rootPath;
        public long size;

        public GarbageItem(String appName, String path, String rootPath, long length) {
            super();
            this.appName = appName;
            this.path = path;
            this.rootPath = rootPath;
            this.size = length;
        }
    }

    public class CleanupItemInfo {

        public int fileCount;
        public long fileSize;
        final AsyncGarbageCleanupHelper cleanHelper;

        public CleanupItemInfo(int count, long size) {
            super();
            cleanHelper = AsyncGarbageCleanupHelper.this;
            this.fileCount = count;
            this.fileSize = size;
        }
    }

    public static interface GarbageCleanupStatesListener {

        public abstract void onAppGarbageFinish(List<GarbageItem> list);

        public abstract void onFinish(int i, long l, int j);

        public abstract void onUpdateUI(int i);
    }

    /**
     * Subclass of IPackageStatsObserver.Stub
     * @author tyd wulianghuan
     *
     */
    private class CacheSizeObserver extends IPackageStatsObserver.Stub {
        private boolean mDelete;
        private ApplicationInfo mInfo;

        public CacheSizeObserver(ApplicationInfo applicationinfo, boolean delete) {
            super();
            this.mInfo = applicationinfo;
            this.mDelete = delete;
        }

        public void onGetStatsCompleted(PackageStats packagestats, boolean succeeded) {
            synchronized (mLock) {
                if (!mRunning) {
                    mSemaphore = true;
                    mLock.notifyAll();
                    return;
                }
            }
            mCacheSizeObserverCount.addAndGet(1);
            if (succeeded) {
                mApplicationSizeMap.put(packagestats.packageName, packagestats.cacheSize + packagestats.externalCacheSize);
            }
            if (mCacheSizeObserverCount.intValue() <= mApplicationInfoCount || mDelete) {
                long fileSize = packagestats.cacheSize + packagestats.externalCacheSize;
                if(fileSize > 0){
                    mCacheCleanupStatusItem.mFileCount += 1;
                    mCacheCleanupStatusItem.mSizeCount += fileSize;
                }
                if (mDelete) {
                    mPackageManager.deleteApplicationCacheFiles(mInfo.packageName, mCacheCleanupObserver);
                }
            }
            if (mCacheSizeObserverCount.intValue() == mApplicationInfoCount) {
                synchronized (mLock) {
                    mSemaphore = true;
                    mLock.notifyAll();
                }
            }
            
        } 
    }

    /**
     * This method used to clean the garbage of uninstalled apps
     * @param flag
     * @return
     */
    private CleanupItemInfo appGarbageCleanup(boolean flag) {
        CleanupItemInfo cleanupiteminfo = null;
        if (!mRunning) {
            return null;
        }
        if (!flag) {
            scanAppGarbage();
        }
        if (mAppGarbageList != null && mAppGarbageList.size() > 0) {
            if (flag) {
                LinkedList linkedlist = new LinkedList(mAppGarbageList);
                if (mAppGarbageCleanupList != null && !mAppGarbageCleanupList.isEmpty()) {
                    Iterator iterator = mAppGarbageCleanupList.iterator();
                    while (iterator.hasNext() && mRunning) {
                        GarbageItem garbageitem = (GarbageItem) iterator.next();
                        
                        boolean isDeleted = DeleteFile(garbageitem.rootPath + garbageitem.path);
                        if (isDeleted == true) {
                            long length = garbageitem.size;
                            if (linkedlist.remove(garbageitem)) {
                                mDeletedFileCount += 1;
                                mDeletedFileSize += length;
                            }
                        }
                    }
                }
            } else {
                Iterator iterator3 = mAppGarbageList.iterator();
                int count = 0;
                long length = 0;
                while (iterator3.hasNext() && mRunning) {
                    GarbageItem garbageitem1 = (GarbageItem) iterator3.next();
                    count += 1;
                    length += (new DirectorySizeDetector(garbageitem1.rootPath + garbageitem1.path)).getSize();
                }
                cleanupiteminfo = new CleanupItemInfo(count, length);
            }
        }
        mListener.onAppGarbageFinish(mAppGarbageList);
        return cleanupiteminfo;
    }
    
    protected boolean DeleteFile(String filePath) {
        if (!mRunning) {
            return false;
        }
        boolean isDeleted = false;
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }

        File file = new File(filePath);
        if (file.exists() && file.isDirectory()) {
            for (File child : file.listFiles()) {
                DeleteFile(child.getAbsolutePath());
            }
        }
        if (mRunning && file.delete()) {
            isDeleted = true;
        }
        return isDeleted;
    }
    
    /**
     * Method for clean cache files.
     * @param flag
     * @return CleanupItemInfo
     */
    private CleanupItemInfo cacheCleanup(boolean flag) {
        CleanupItemInfo cleanupiteminfo = null;
        if (!mRunning) {
            return null;
        }
        mCacheCleanupStatusItem = new GarbageCleanupItem();
        mApplicationSizeMap = new HashMap();
        mApplicationInfoList = mPackageManager.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        HashSet runningAppHashSet = new HashSet();
        List runningProcesList = ((ActivityManager) mContext.getSystemService("activity")).getRunningAppProcesses();
        if (runningProcesList != null) {
            Iterator iterator = runningProcesList.iterator();
            if (iterator != null) {
                while (iterator.hasNext() && mRunning) {
                    // get the all packages that have been loaded into the process.
                    String[] as = ((ActivityManager.RunningAppProcessInfo) iterator.next()).pkgList;
                    for (int i = 0; i < as.length; i++) {
                        runningAppHashSet.add(as[i]);
                    }
                }
            }

            mSemaphore = false;
            mCacheSizeObserverCount.set(0);
            mCacheCleanupObserverCount.set(0);
            mApplicationInfoCount = mApplicationInfoList.size();
            for (int i = 0; i < mApplicationInfoList.size(); i++) {
                ApplicationInfo applicationinfo = (ApplicationInfo) mApplicationInfoList.get(i);
                if (!runningAppHashSet.contains(applicationinfo.packageName)) {
                    PackageManager packagemanager = mPackageManager;
                    String name = applicationinfo.packageName;
                    CacheSizeObserver cachesizeobserver = new CacheSizeObserver(applicationinfo, flag);
                    packagemanager.getPackageSizeInfo(name, cachesizeobserver);
                } else {
                    mCacheSizeObserverCount.addAndGet(1);
                    mCacheCleanupObserverCount.addAndGet(1);
                }
            }
            
            synchronized (mLock) {
                try {
                    mLock.wait(20000L);
                } catch (InterruptedException interruptedexception) {
                    interruptedexception.printStackTrace();
                }
            }
           //*/modified by dori mingjun on 2015-12-28 for cleanup
            if(flag){
                cleanupiteminfo = new CleanupItemInfo(0, 0);
            }else{
            cleanupiteminfo = new CleanupItemInfo(mCacheCleanupStatusItem.mFileCount, mCacheCleanupStatusItem.mSizeCount);
            }
          //*/
    }
        return cleanupiteminfo;
    }

    private boolean checkApkInstall(String packageName) {
        boolean installed = false;
        try {
            mPackageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    private CleanupItemInfo emptyDirCleanup(File file, boolean flag) {
        EmptyDirCleaner emptydircleaner = new EmptyDirCleaner(file, flag, false);
        if (flag) {
            mDeletedFileCount += emptydircleaner.emptyDeleteCount();
            mDeletedFileSize += emptydircleaner.emptyDeleteSize();
        }
        return new CleanupItemInfo(emptydircleaner.emptyDirCount(),
                emptydircleaner.sizeCount());
    }

    private CleanupItemInfo emptyDirCleanup(boolean flag) {
        int count = 0;
        long size = 0;
        File internalFile = new File(mInternalPath);
        if (internalFile.exists()) {
            CleanupItemInfo cleanupiteminfo = emptyDirCleanup(internalFile, flag);
            count = cleanupiteminfo.fileCount;
            size = cleanupiteminfo.fileSize;
        }
        //modify by tyd liuyong 20140806 for kk storage
        if (FeatureOption.MTK_MULTI_STORAGE_SUPPORT) {
            if(!TextUtils.isEmpty(mExternalPath)){
            File externalFile = new File(mExternalPath);
            if (externalFile.exists()) {
                CleanupItemInfo cleanupiteminfo1 = emptyDirCleanup(externalFile, flag);
                count += cleanupiteminfo1.fileCount;
                size += cleanupiteminfo1.fileSize;
            }
        }
        }
        return new CleanupItemInfo(count, size);
    }

    private void resetDeletedMarkParam() {
        if (mActionList != null)
            mActionList.clear();
        if (mAppGarbageCleanupList != null)
            mAppGarbageCleanupList.clear();
    }

    private void scanAppGarbage() {
        if (mAppGarbageList == null) {
            mAppGarbageList = new LinkedList();
        }
        File internalFile = new File(mInternalPath);
        if (internalFile.exists()) {
            scanAppGarbage(mInternalPath);
        }
        //modify by tyd liuyong 20140806 for kk storage
        if (FeatureOption.MTK_MULTI_STORAGE_SUPPORT) {
            if(!TextUtils.isEmpty(mExternalPath)){
            File externalFile = new File(mExternalPath);
            if (externalFile.exists()) {
                scanAppGarbage(mExternalPath);
            }
        }
        }
    }

    /**
     * This method do work for scan app garbage
     * @param volumePath
     */
    private void scanAppGarbage(String volumePath) {
        File file = new File(volumePath);
        if (!file.exists()) {
            return;
        }
        
        File afile[] = file.listFiles();
        if (afile != null && afile.length > 0) {
            CleanUpDatabaseHelper dbHelper = CleanUpDatabaseHelper.getDatabaseHelperInstance(mContext);
            SQLiteDatabase db = dbHelper.openDatabase();
            try {
                ArrayList<String> whiteList = CleanupUtil.getWhiteList(mContext);
                for (int i = 0; i < afile.length && mRunning; i++) {
                    if (!afile[i].isDirectory()) {
                        continue;
                    }
                    getPackageNameByPath(db, whiteList, volumePath, afile[i].getAbsolutePath());
                }
            } finally {
                db.close();
            }
        }
    }
    
    
    /**
     * Get garbageItems and put them into mAppGarbageList
     * @param db
     * @param whiteList
     * @param volumePath
     * @param absolutePath
     */
    private void getPackageNameByPath(SQLiteDatabase db, ArrayList<String> whiteList, String volumePath, String absolutePath) {
        String folderPath = absolutePath.substring(volumePath.length());
        
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("select path,package_name,name from package as a,app as b on a.app_id = b._id where a.path like'"
                                    + folderPath + "%'", null);
            while (cursor.moveToNext()) {
                String relativePath = cursor.getString(0);
                String packageName = cursor.getString(1);
                // if the relativePath in whiteList or the packageName is empty
                // or null, then to continue
                if (whiteList.contains(relativePath) || TextUtils.isEmpty(packageName)) {
                    continue;
                }
                // if one of packageNames is installed, then to break, for this
                // relativePath is used by an app
                if (checkApkInstall(packageName) == true) {
                    break;
                }
                long itemLength = (new DirectorySizeDetector(volumePath + cursor.getString(0))).getSize();
                GarbageItem garbageItem = new GarbageItem(cursor.getString(2), cursor.getString(0), volumePath, itemLength);
                // if the garbageItem isValidate garbage
                if (isGarbageItemExist(garbageItem)) {
                    mAppGarbageList.add(garbageItem);
                    break;
                }
            }
        } catch (Exception e) {
            Log.i(LOG_TAG, "scanAppGarbage(), query package_path table catch exception: " + e.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    
    private boolean isGarbageItemExist(GarbageItem garbageitem) {
        if (garbageitem != null) {
            String filePath = garbageitem.rootPath + garbageitem.path;
            File file = new File(filePath);
            if (file.exists()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Scan or clean system temp files
     * @param rootDir
     * @param tempDirs
     * @param delete
     * @return CleanupItemInfo
     */
    private CleanupItemInfo systemTempFileCleanup(String rootDir, ArrayList<String> tempDirs, boolean delete) {
        int count = 0;
        long size = 0;
        if (!TextUtils.isEmpty(rootDir)) {
            for (int i = 0; i < tempDirs.size() && mRunning; i++) {
                File file = new File((new StringBuilder()).append(rootDir).append(tempDirs.get(i)).toString());
                if (file.exists()) {
                    SystemTempFileCleaner systemtempfilecleaner = new SystemTempFileCleaner(file, false, delete);
                    if (!delete) {
                        count += systemtempfilecleaner.fileCount();
                        size += systemtempfilecleaner.size();
                    } else {
                        mDeletedFileCount += systemtempfilecleaner.fileCount();
                        mDeletedFileSize += systemtempfilecleaner.size();
                    }
                }
            }
        }
        return new CleanupItemInfo(count, size);
    }

    private CleanupItemInfo systemTempFileCleanup(boolean delete) {
        int count = 0;
        long size = 0;
        ArrayList<String> tempDirs = CleanupUtil.getTempFolderList(mContext);
        File internalFile = new File(mInternalPath);
        if (internalFile.exists()) {
            CleanupItemInfo localCleanupItemInfo1 = systemTempFileCleanup(mInternalPath, tempDirs, delete);
            count = localCleanupItemInfo1.fileCount;
            size = localCleanupItemInfo1.fileSize;
        }
        //modify by tyd liuyong 20140806 for kk storage
        if (FeatureOption.MTK_MULTI_STORAGE_SUPPORT ) {
            //*/modify by droi liuhaoran 20160316 for Sdcard path does not exist to clean up the flash back 
            if(!TextUtils.isEmpty(mExternalPath)){
                //*/
            File externalFile = new File(mExternalPath);
            if (externalFile.exists()) {
                CleanupItemInfo localCleanupItemInfo2 = systemTempFileCleanup(mExternalPath, tempDirs, delete);
                count += localCleanupItemInfo2.fileCount;
                size += localCleanupItemInfo2.fileSize;
            }
        }
    }
        return new CleanupItemInfo(count, size);
    }

    private CleanupItemInfo thumbnailCleanup(File file, boolean flag) {
        int count = 0;
        long size = 0;
        if (file.exists()) {
            File[] afile = file.listFiles();
            if (afile != null && afile.length > 0) {
                for (int i = 0; i < afile.length; i++) {
                    File file1 = afile[i];
                    long length = file1.length();
                    count += 1;
                    size += length;
                    if (file1.delete()) {
                        mDeletedFileCount += 1;
                        mDeletedFileSize += length;
                    }
                }
            }
        }
        Log.v("AsyncGarbageCleanupHelper",(new StringBuilder())
                        .append("thumbnailCleanup mDeletedFileSize = ")
                        .append(mDeletedFileSize).append(";length = ")
                        .append(size).toString());
        return new CleanupItemInfo(count, size);
    }

    private CleanupItemInfo thumbnailCleanup(boolean flag) {
        int count = 0;
        long size = 0;
        File internalFile = new File(mInternalPath);
        if (internalFile.exists()) {
            CleanupItemInfo cleanupiteminfo1 = thumbnailCleanup(
                    new File((new StringBuilder()).append(mInternalPath).append("/DCIM/.thumbnails").toString()), flag);
            count = cleanupiteminfo1.fileCount;
            size = cleanupiteminfo1.fileSize;
        }
        //modify by tyd liuyong 20140806 for kk storage
        if (FeatureOption.MTK_MULTI_STORAGE_SUPPORT) {
            //*/modify by droi liuhaoran for the clear to flash exit on 20160405 
            if(!TextUtils.isEmpty(mExternalPath)){
                //*/
                File externalFile = new File(mExternalPath);
            if (externalFile.exists()) {
                CleanupItemInfo cleanupiteminfo2 = thumbnailCleanup(
                        new File((new StringBuilder()).append(mExternalPath).append("/DCIM/.thumbnails").toString()), flag);
                count += cleanupiteminfo2.fileCount;
                size += cleanupiteminfo2.fileSize;
            }
        }
        }
        return new CleanupItemInfo(count, size);
    }

    public void cleanUp() {
        mRunning = true;
        if (mState == STATE_DONE) {
            mState = STATE_START_SCAN;
        } else if (mState == STATE_SCAN_FINISH) {
            mState = STATE_START_CLEANUP;
        }
        mListener.onUpdateUI(mState);
        new GarbageCleanupThread().start();
    }

    public int getState() {
        return mState;
    }

    public int getTotalDeletedFileCount() {
        return mDeletedFileCount;
    }

    public long getTotalDeletedFileSize() {
        return mDeletedFileSize;
    }

    public int getTotalFileCount() {
        return mFileCount;
    }

    public long getTotalFileSize() {
        return mFileSize;
    }
    

    public boolean isRunning() {
        return mRunning;
    }

    public void resetDeletedParam() {
        mDeletedFileCount = 0;
        mDeletedFileSize = 0L;
    }

    public void setActionOperate(ArrayList arraylist) {
        mActionList = arraylist;
    }

    public void setAppGarbageCleanupItem(List<GarbageItem> list) {
        if (mActionList == null) {
            mActionList = new ArrayList();
            mActionList.add(4);
        } else {
            if (!mActionList.contains(4)) {
                mActionList.add(4);
            }
        }
        mAppGarbageCleanupList = new ArrayList(list);
    }

    public void setGarbageCleanupStatesListener(
            GarbageCleanupStatesListener garbagecleanupstateslistener) {
        mListener = garbagecleanupstateslistener;
    }

    public void setState(int i) {
        mState = i;
    }

    public void stopRunning() {
        mRunning = false;
        synchronized (mLock) {
            mSemaphore = true;
            mLock.notifyAll();
        }
    }

}
