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
package com.freeme.filemanager.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import com.freeme.filemanager.R;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.view.FavoriteItem;
import com.freeme.filemanager.view.FileViewFragment;
import com.freeme.filemanager.view.Settings;

import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import com.freeme.filemanager.util.FeatureOption;

public class Util {
    private static String ANDROID_SECURE = "/mnt/sdcard/.android_secure";
    
    public static String USBOTG_DIR = "/storage/usbotg";
    
    public static long SYSTEM_RESERVED_STORAGE_SIZE = 30 * 1024 * 1024;//30MB
    
    private static final String LOG_TAG = "Util";
    public static final int BUFFER_SIZE = 1024 * 1024;

    private static DateFormat  date_format = DateFormat.getDateInstance();
    
    private static DateFormat  time_format = DateFormat.getTimeInstance();
    
    //add by TYD mingjun
    public static String MEMORY_DIR = Environment.getExternalStorageDirectory().getPath();

    public static String SD_DIR = "/storage/sdcard1";
    
    private static File sdFile= new File(SD_DIR);
    
    public static int USBOTG_DEFAULT_SIZE = 742903808;
    
    private static Context mContext;

    //*/ freeme.liuhaoran , 20160802 , adapter sdcard path
    private static StorageVolume storageVolume1;
    private static String PATH = "/storage/emulated/0";
    private static String defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static ArrayList<StorageVolume> mountVolumeList;
    private static StorageVolume[] storageVolumes;
    private static StorageVolume storageVolume = null;
    //*/
    public static boolean isSDCardReady() {
            return sdFile.equals(Environment.MEDIA_MOUNTED);
    }

    // if path1 contains path2
    public static boolean containsPath(String path1, String path2) {
        String path = path2;
        while (path != null) {
            if (path.equalsIgnoreCase(path1))
                return true;

            if (path.equals(GlobalConsts.ROOT_PATH))
                break;
            path = new File(path).getParent();
        }

        return false;
    }

    public static String makePath(String path1, String path2) {
        if (path1.endsWith(File.separator))
            return path1 + path2;

        return path1 + File.separator + path2;
    }

    public static boolean isNormalFile(String fullName) {
        return !fullName.equals(ANDROID_SECURE);
    }

    public static FileInfo GetFileInfo(String filePath) {
        File lFile = new File(filePath);
        if (!lFile.exists())
            return null;

        FileInfo lFileInfo = new FileInfo();
        lFileInfo.canRead = lFile.canRead();
        lFileInfo.canWrite = lFile.canWrite();
        lFileInfo.isHidden = lFile.isHidden();
        lFileInfo.fileName = Util.getNameFromFilepath(filePath);
        lFileInfo.ModifiedDate = lFile.lastModified();
        String str2 = date_format.format(lFileInfo.ModifiedDate);
        String str3 = time_format.format(lFileInfo.ModifiedDate);
        lFileInfo.fileFormatDateTime = str2 + " " + str3;
        lFileInfo.IsDir = lFile.isDirectory();
        lFileInfo.filePath = filePath;
        lFileInfo.fileSize = lFile.length();
        return lFileInfo;
    }

    public static FileInfo GetFileInfo(File f, FilenameFilter filter, boolean showHidden) {
        FileInfo lFileInfo = new FileInfo();
        String filePath = f.getPath();
        File lFile = new File(filePath);
        lFileInfo.canRead = lFile.canRead();
        lFileInfo.canWrite = lFile.canWrite();
        lFileInfo.isHidden = lFile.isHidden();
        lFileInfo.fileName = f.getName();
        lFileInfo.ModifiedDate = lFile.lastModified();
        String str2 = date_format.format(lFileInfo.ModifiedDate);
        String str3 = time_format.format(lFileInfo.ModifiedDate);
        lFileInfo.fileFormatDateTime = str2 + " " + str3;
        lFileInfo.IsDir = lFile.isDirectory();
        lFileInfo.filePath = filePath;
        if (lFileInfo.IsDir) {
            int lCount = 0;
            File[] files = lFile.listFiles(filter);

            // null means we cannot access this dir
            if (files == null) {
                return null;
            }

            for (File child : files) {
                if ((!child.isHidden() || showHidden)
                        && Util.isNormalFile(child.getAbsolutePath())) {
                    lCount++;
                }
            }
            lFileInfo.Count = lCount;

        } else {

            lFileInfo.fileSize = lFile.length();

        }
        return lFileInfo;
    }

    public static Drawable getApkIcon(Context context, String apkPath) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apkPath,
                PackageManager.GET_ACTIVITIES);
        if (info != null) {
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            try {
                return appInfo.loadIcon(pm);
            } catch (OutOfMemoryError e) {
                Log.e(LOG_TAG, e.toString());
            }
        }
        return null;
    }

    //*/ freeme.liuhaoran , 20160802 , adapter sdcard path
    // */add by mingjun on 2015-12-31 for storage path
    public static void setMountedStorageBySubPath(Context paramContext, StorageManager mStorageManager) {
        storageVolumes = mStorageManager.getVolumeList();
        mountVolumeList = new ArrayList<StorageVolume>();
        int i = storageVolumes.length;
        Log.i("liuhaoran1", "storageVolumes.length =" + i);
        for (int j = 0; j < i; ++j) {
            storageVolume1 = storageVolumes[j];
            Log.i("liuhaoran1", "isRemovable =" + storageVolume1.isRemovable() );
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (storageVolume1.getStorageId() != 0 && !storageVolume1.getState().equals(Environment.MEDIA_REMOVED)) {
                    mountVolumeList.add(storageVolume1);
                }
            }else {
                if (storageVolume1.getStorageId() != 0) {
                    mountVolumeList.add(storageVolume1);
                }
            }
        }
        Log.i("liuhaoran1", "mountVolumeList =" + mountVolumeList.size());
//      Log.i("liuhaoran1", "storageVolumes[0] =" + storageVolumes[0] );
//      Log.i("liuhaoran1", "storageVolumes[1] =" + storageVolumes[1] );
//      Log.i("liuhaoran1", "storageVolumes[2] =" + storageVolumes[2] );
      Log.i("liuhaoran1", "defaultPath =" + defaultPath );
        if (mountVolumeList.size() == 2) {
            
            /*/ freeme.liuhaoran , 20160802 , adapter sdcard path
            storageVolume = storageVolumes[1];
            SD_DIR = storageVolume.getPath();
            //*/
            SD_DIR = getSdPath();
            //*/
        }
        if(mountVolumeList.size() == 1){
            SD_DIR = null;
        }
        if (mountVolumeList.size() > 2) {
            //*/ freeme.liuhaoran , 20160802 , adapter sdcard path
            SD_DIR = getSdPath();
            //*/
            Log.i("liuhaoran1", "getSdPath=" + getSdPath());
            storageVolume = storageVolumes[2];
            USBOTG_DIR = storageVolume.getPath();
        }
    }
    //*/
    
    //*/ freeme.liuhaoran , 20160802 , adapter sdcard path
    public static String getSdPath() {
        if (PATH.equals(defaultPath) || !mountVolumeList.get(0).isRemovable()){
            storageVolume = storageVolumes[1];
        }else {
            storageVolume = storageVolumes[0];
        }
        return storageVolume.getPath();
    }
    //*/

    // */
    public static StorageVolume getMountedStorageBySubPath(Context paramContext, String volumPath) {
        if (!TextUtils.isEmpty(volumPath)) {
            Iterator localIterator = StorageHelper.getInstance(paramContext).getMountedVolumeList().iterator();
            StorageVolume storageVolume;
            while (localIterator.hasNext()) {
                storageVolume = (StorageVolume) localIterator.next();
                if (volumPath.startsWith(storageVolume.getPath()))
                    return storageVolume;
            }
        }
        return null;
    }

    public static String getExtFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(dotPosition + 1, filename.length());
        }
        return "";
    }

    public static String getNameFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(0, dotPosition);
        }
        return "";
    }

    public static String getPathFromFilepath(String filepath) {
        int pos = filepath.lastIndexOf('/');
        if (pos != -1) {
            return filepath.substring(0, pos);
        }
        return "";
    }

    public static String getNameFromFilepath(String filepath) {
        int pos = filepath.lastIndexOf('/');
        if (pos != -1) {
            return filepath.substring(pos + 1);
        }
        return "";
    }

    // return new file path if successful, or return null
    public static String copyFile(String src, String dest) {
        File file = new File(src);
        if (!file.exists() || file.isDirectory()) {
            Log.v(LOG_TAG, "copyFile: file not exist or is directory, " + src);
            return null;
        }
        FileInputStream fi = null;
        FileOutputStream fo = null;
        try {
            fi = new FileInputStream(file);
            File destPlace = new File(dest);
            if (!destPlace.exists()) {
                if (!destPlace.mkdirs())
                    return null;
            }

            String destPath = Util.makePath(dest, file.getName());
            File destFile = new File(destPath);
            int i = 1;
            while (destFile.exists()) {
                String destName = Util.getNameFromFilename(file.getName()) + " " + i++ + "."
                        + Util.getExtFromFilename(file.getName());
                destPath = Util.makePath(dest, destName);
                destFile = new File(destPath);
            }

            if (!destFile.createNewFile())
                return null;

            fo = new FileOutputStream(destFile);
            byte[] buffer = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = fi.read(buffer, 0, BUFFER_SIZE)) != -1) {
                fo.write(buffer, 0, read);
                fo.flush();
            }

            // TODO: set access privilege

            return destPath;
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "copyFile: file not found, " + src);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(LOG_TAG, "copyFile: " + e.toString());
        } finally {
            try {
                if (fi != null)
                    fi.close();
                if (fo != null)
                    fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    // does not include sd card folder
    private static String[] SysFileDirs = new String[] {
        "miren_browser/imagecaches"
    };

    public static boolean shouldShowFile(String path) {
        return shouldShowFile(new File(path));
    }

    public static boolean shouldShowFile(File file) {
        boolean show = Settings.instance().getShowDotAndHiddenFiles();
        if (show)
            return true;

        if (file.isHidden())
            return false;

        if (file.getName().startsWith("."))
            return false;
        
        String sdFolder = getSdDirectory();
        if(sdFolder == null){
            return false;
        }
        for (String s : SysFileDirs) {
            if (file.getPath().startsWith(makePath(sdFolder, s)))
                return false;
        }

        return true;
    }

    public static ArrayList<FavoriteItem> getDefaultFavorites(Context context) {
        ArrayList<FavoriteItem> list = new ArrayList<FavoriteItem>();
        return list;
    }

    public static boolean setText(View view, int id, String text) {
        TextView textView = (TextView) view.findViewById(id);
        if (textView == null)
            return false;

        textView.setText(text);
        return true;
    }

    public static boolean setText(View view, int id, int text) {
        TextView textView = (TextView) view.findViewById(id);
        if (textView == null)
            return false;

        textView.setText(text);
        return true;
    }

    // comma separated number
    public static String convertNumber(long number) {
        return String.format("%,d", number);
    }

    // storage, G M K B
    public static String convertStorage(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        } else
            return String.format("%d B", size);
    }

    public static String getMemoryDirectory() {
        if(isSdcardExist()){
            return Environment.getExternalStorageDirectory().getPath();
        }else {
            return SD_DIR;
        }
    }


    public static String getSdDirectory() {
        if(isSdcardExist()){
            return SD_DIR;                      
        }else {
            return "/mnt";
        }
    }


    public static boolean isSdcardExist() {
        if(sdFile.equals(android.os.Environment.MEDIA_MOUNTED)){
            return true;                        
        }else {
            return false;
        }
    }
    
    public static class SDCardInfo {
        public long total;
        public long free;
    }

    public static SDCardInfo getSDCardInfo() {
        /*/ freeme.liuhaoran , 20160802 , unuseful code 
        String sDcString = null;
        if(isSdcardExist()){
            //medify by mingjun
            sDcString = sdFile.toString();          
        }else {
            return null;
        }
        //*/
    if (!TextUtils.isEmpty(SD_DIR)) {
        sdFile= new File(SD_DIR);
      
            File pathFile = sdFile;
            try {
                android.os.StatFs statfs = new android.os.StatFs(pathFile.getPath());
                //Log.i("liuhaoran1", "pathFile.getPath() =" + pathFile.getPath());
                long nTotalBlocks = statfs.getBlockCount();
                long nBlocSize = statfs.getBlockSize();
                long nFreeBlock = statfs.getFreeBlocks();
                long nAvailaBlock = statfs.getAvailableBlocks();

                SDCardInfo info = new SDCardInfo();
                info.total = nTotalBlocks * nBlocSize;
                info.free = nAvailaBlock * nBlocSize;                   
                return info;
            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, e.toString());
            }
        }

        return null;
    }


    public static class MemoryCardInfo {
        public long total;
        public long free;
    }
    
    public static MemoryCardInfo getMemoryCardInfo() {
        /*/ freeme.liuhaoran , 20160802 , adapter sdcard path
        String sMemoryCardString = null;
        if(isSdcardExist()){ 
            sMemoryCardString = sdFile.toString();    
        }else{ 
            sMemoryCardString = android.os.Environment.getExternalStorageState();
        }
        if (sMemoryCardString.equals(android.os.Environment.MEDIA_MOUNTED)) {
            File pathFile = null;
            if(isSdcardExist()){
                pathFile = sdFile;
            }else {
                pathFile = android.os.Environment.getExternalStorageDirectory();                
            }
            //*/
            String path = getDefaultPath();
            //*/
            Log.i("liuhaoran1", "path =" + Environment.getExternalStorageDirectory().getAbsolutePath());
            try {
                android.os.StatFs statfs = new android.os.StatFs(path);
                long nTotalBlocks = statfs.getBlockCount();
                long nBlocSize = statfs.getBlockSize();
                long nFreeBlock = statfs.getFreeBlocks();
                long nAvailaBlock = statfs.getAvailableBlocks();
                
                MemoryCardInfo info = new MemoryCardInfo();
                info.total = nTotalBlocks * nBlocSize;
                info.free = nAvailaBlock * nBlocSize;                   
                return info;
            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, e.toString());
            }
//        }

        return null;
    }
    
    //*/ freeme.liuhaoran , 20160802 , adapter sdcard path
    public static String getDefaultPath() {
        if (PATH.equals(defaultPath) ) {
            return defaultPath;
        }else if (mountVolumeList.size() == 1 ) {
            return mountVolumeList.get(0).getPath();
        }else if (mountVolumeList.size() == 2 && !mountVolumeList.get(0).isRemovable()) {
            return mountVolumeList.get(0).getPath();
        }else {
        return mountVolumeList.get(1).getPath();
    }
    }
    //*/
    
    //*/ freeme.liuhaoran , 20160802 , adapter sdcard path
    public static String getDefaultState() {
        if (PATH.equals(defaultPath) ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.L) {
            return Environment.getExternalStorageState(new File(defaultPath));
        }else {
            return Environment.getStorageState(new File(defaultPath));
    }
    }else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.L) {
            return Environment.getExternalStorageState(new File(mountVolumeList.get(1).getPath()));
        }else {
            return Environment.getStorageState(new File(mountVolumeList.get(1).getPath()));
        }
    }
    }
    //*/
    
    public static class UsbStrogeInfo {
        public long total;
        public long free;
    }
    
    public static UsbStrogeInfo getUsbStorgeInfo() {
        File pathFile = new File("/storage/usbotg");
        UsbStrogeInfo info = new UsbStrogeInfo();
        try {
            android.os.StatFs statfs = new android.os.StatFs(pathFile.getPath());
            long nTotalBlocks = statfs.getBlockCount();
            long nBlocSize = statfs.getBlockSize();
            long nFreeBlock = statfs.getFreeBlocks();
            long nAvailaBlock = statfs.getAvailableBlocks();
            info.total = nTotalBlocks * nBlocSize;
            info.free = nAvailaBlock * nBlocSize;
            if(info.total==0 && info.free==0){
                return null;
            }
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, e.toString());
        }
        return info;
    }

    public static void showNotification(Context context, Intent intent, String title, String body, int drawableId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(drawableId, body, System.currentTimeMillis());
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.defaults = Notification.DEFAULT_SOUND;
        if (intent == null) {
            // FIXEME: category tab is disabled
            intent = new Intent(context, FileViewFragment.class);
        }
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        notification.setLatestEventInfo(context, title, body, contentIntent);
        manager.notify(drawableId, notification);
    }

    private static DateFormat dateFormat = null;
    
    private static DateFormat timeFormat = null;
    
    private static void getDateFormat(Context context){
        if(dateFormat == null){
            dateFormat = android.text.format.DateFormat.getDateFormat(context);
        }else {
            return;
        }
    }

    private static void getTimeFormat(Context context){
        if(timeFormat == null){
            timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        }else {
            return;
        }
    }
    
    public static String formatDateString(Context context, long time) {
        getDateFormat(context);
        getTimeFormat(context);
        Date date = new Date(time);
        return dateFormat.format(date) + " " + timeFormat.format(date);
    }

    public static void updateActionModeTitle(ActionMode mode, Context context, int selectedNum) {
        if (mode != null) {
            //modify by mingjun for seleted counts
            if(selectedNum == 0){
                mode.finish();
            }else if(selectedNum == 1){
                mode.setTitle((context.getString(R.string.multi_select_title,selectedNum)).toString().replace("s", ""));
            }else{
                mode.setTitle(context.getString(R.string.multi_select_title,selectedNum));
            }
        }
    }

    public static HashSet<String> sDocMimeTypesSet = new HashSet<String>() {
        {
            add("text/plain");
            add("text/html");
            add("application/pdf");
            add("application/msword");
            add("application/vnd.ms-excel");
            add("application/vnd.ms-powerpoint");
            add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            add("application/vnd.openxmlformats-officedocument.wordprocessingml.template");
        }
    };

    public static String sZipFileMimeType = "application/zip";

    public static int CATEGORY_TAB_INDEX = 0;
    public static int SDCARD_TAB_INDEX = 1;

    public static Bitmap toRoundCorner(Bitmap bitmap, int pixels) {  
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);  
        Canvas canvas = new Canvas(output);  
        final int color = 0xff424242;  
        final Paint paint = new Paint();  
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());  
        final RectF rectF = new RectF(rect);  
        final float roundPx = pixels;  
        paint.setAntiAlias(true);  
        canvas.drawARGB(0, 0, 0, 0);  
        paint.setColor(color);  
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);  
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));  
        canvas.drawBitmap(bitmap, rect, rect, paint);  
        return output;  
    }
    
    public static boolean isInSameVolume(String path1, String path2) {
        Log.i("isInSameVolume", "path1: "+path1+", path2: "+path2);
        if ((TextUtils.isEmpty(path1))|| (TextUtils.isEmpty(path2))){
            return false;
        }
        //modify by tyd liuyong 20140806 for kk storage
        if(FeatureOption.MTK_MULTI_STORAGE_SUPPORT){
            if(path1.startsWith(MEMORY_DIR)){
                if(path2.startsWith(MEMORY_DIR)){
                    return true;
                }
            }else if(path1.startsWith(SD_DIR)){
                if(path2.startsWith(SD_DIR)){
                    return true;
                }
            }else if(path1.startsWith(USBOTG_DIR)){
                if(path2.startsWith(USBOTG_DIR)){
                    return true;
                }
            }
        }else{
            if(path1.startsWith(MEMORY_DIR)){
                if(path2.startsWith(MEMORY_DIR)){
                    return true;
                }
            }else if(path1.startsWith(USBOTG_DIR)){
                if(path2.startsWith(USBOTG_DIR)){
                    return true;
                }
            }
        }
        return false;
    }
    
    public static String getFormatedFileName(String fileName){
        String formatedName = "";
        if(TextUtils.isEmpty(fileName)){
            return formatedName;
        }
        if(fileName.lastIndexOf(".") == -1){
            formatedName = fileName;
        }else{
            formatedName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        return formatedName;
    }
    
    public static long getCurMemoryFreeSize(String currentPath){
        long freeSize = 0;
        SDCardInfo sdCardInfo = Util.getSDCardInfo();
        
        MemoryCardInfo memoryCardInfo = Util.getMemoryCardInfo();
        UsbStrogeInfo usbStrogeInfo = Util.getUsbStorgeInfo();
        //modify by tyd liuyong 20140806 for kk storage
//      if(sdCardInfo.total!=0){
        
        if(sdCardInfo!=null){
            if(currentPath.startsWith(MEMORY_DIR)){
                freeSize = memoryCardInfo.free;
            }else if(currentPath.startsWith(SD_DIR)){
                freeSize = sdCardInfo.free;
            }else if(currentPath.startsWith(USBOTG_DIR)){
                freeSize = usbStrogeInfo.free;
            }
        }else {
            if(currentPath.startsWith(MEMORY_DIR)){
                freeSize = memoryCardInfo.free;
            }else if(currentPath.startsWith(USBOTG_DIR)){
                freeSize = usbStrogeInfo.free;
            }
        }

//      }else{
//          if(currentPath.startsWith("/storage/emulated/0")){
//              freeSize = memoryCardInfo.free;
//          }else if(currentPath.startsWith("/storage/usbotg")){
//              freeSize = usbStrogeInfo.free;
//          }
//      }
        return freeSize;
    }
    
    //*/ Added by tyd wulianghuan 2013-12-12, define this method for get a relative path of the given params
    public static String getRelativePathAtVolume(String volmePath, String folderPath) {
        String relativePath = null;
        if ((!TextUtils.isEmpty(folderPath)) && (!TextUtils.isEmpty(volmePath)) && (folderPath.indexOf(volmePath) >= 0)){
            relativePath = folderPath.substring(volmePath.length());
        }
        return relativePath;
    }
    //*/
}
