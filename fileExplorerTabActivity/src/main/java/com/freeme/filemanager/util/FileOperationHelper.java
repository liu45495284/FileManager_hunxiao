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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipException;

import com.freeme.filemanager.R;
import com.freeme.filemanager.controller.FileManagerOperationActivity.ScannerClient;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.Util.MemoryCardInfo;
import com.freeme.filemanager.util.Util.SDCardInfo;
import com.freeme.filemanager.view.Settings;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import android.content.IContentProvider;
import android.media.MediaScannerConnection;

public class FileOperationHelper {
    private static final String LOG_TAG = "FileOperation";
    
    public ArrayList<FileInfo> mCurFileNameList = new ArrayList<FileInfo>();

    private boolean mMoving;
    
    public static boolean cancelfileoperation = false;

    private AsyncTask mAsyncTask = null;
    
    private IOperationProgressListener mOperationListener;

    private FilenameFilter mFilter;
    
    private Context mContext;
    
    private int taskType = -1;

    private String mOldFilePath;

    private String mNewPath;


    public interface IOperationProgressListener {
        void onFinish();
        
        void onFileChanged(int type);
    }

    public FileOperationHelper(IOperationProgressListener l, Context context) {
        mOperationListener = l;
        this.mContext = context;
    }
    
    private void asnycExecute(Runnable r) {
        final Runnable _r = r;
        setCancelFileOperation(false);
        mMoving = false;
        mAsyncTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object... params) {
                    _r.run();
                return null;
            }
            
            @Override
            protected void onPostExecute(Object result) {
                if (mOperationListener != null) {
                    mOperationListener.onFinish();
                }

                if(taskType != -1){
                    mOperationListener.onFileChanged(taskType);
                }
                clear();
            }

            @Override
            protected void onCancelled(Object result) {
                if(taskType != -1){
                    mOperationListener.onFinish();
                    mOperationListener.onFileChanged(taskType);
                }
                super.onCancelled(result);
            }
            
        };
        mAsyncTask.execute();
    }


    public void setFilenameFilter(FilenameFilter f) {
        mFilter = f;
    }


    //heqianqian
    public boolean CreateFolder(String path, String name) {
        File f = new File(Util.makePath(path, name));
        if (f.exists())
            return false;
        boolean createStatus = f.mkdir();
        Log.i("heqianqian","createStatus="+createStatus);
        if(createStatus){
            mOperationListener.onFileChanged(GlobalConsts.TYPE_NOTIFY_SCAN);
        }
        return createStatus;
    }

    public void Copy(ArrayList<FileInfo> files) {
        copyFileList(files);
    }

    public boolean Paste(String path) {
        taskType = GlobalConsts.TYPE_NOTIFY_SCAN;
        if (mCurFileNameList.size() == 0)
            return false;
        final String _path = path;
        asnycExecute(new Runnable() {
            @Override
            public void run() {
                for (FileInfo f : mCurFileNameList) {
                    synchronized (f) {
                        if (mAsyncTask == null) {
                            break;
                        }
                        CopyFile(f, _path);
                    }
                }
            }
        });

        return true;
    }

    public boolean canPaste() {
        return mCurFileNameList.size() != 0;
    }

    public void StartMove(ArrayList<FileInfo> files) {
        if (mMoving)
            return;

        mMoving = true;
        copyFileList(files);
    }

    public boolean isMoveState() {
        return mMoving;
    }

    public boolean canMove(String path) {
        for (FileInfo f : mCurFileNameList) {
            if (!f.IsDir)
                continue;

            if (Util.containsPath(f.filePath, path))
                return false;
        }

        return true;
    }

    public void clear() {
        synchronized(mCurFileNameList) {
            mCurFileNameList.clear();
        }
    }

    public boolean EndMove(String path) {
        /*taskType = GlobalConsts.TYPE_NOTIFY_SCAN;
         modify by tyd sxp for update db when move
         */
        taskType = GlobalConsts.TYPE_MOVE_NOTIFY_SCAN;
        if (!mMoving)
            return false;
        mMoving = false;

        if (TextUtils.isEmpty(path))
            return false;

        final String _path = path;
        asnycExecute(new Runnable() {
            @Override
            public void run() {
                for (FileInfo f : mCurFileNameList) {
                    synchronized (f) {
                        if (mAsyncTask == null) {
                            break;
                        }
                        MoveFile(f, _path);
                    }
                }

            }
        });

        return true;
    }

    public ArrayList<FileInfo> getFileList() {
        return mCurFileNameList;
    }

    public boolean isFileSelected(String path) {
        synchronized(mCurFileNameList) {
            for (FileInfo f : mCurFileNameList) {
                if (f.filePath.equalsIgnoreCase(path))
                    return true;
            }
        }
        return false;
    }

    public boolean Rename(FileInfo f, String newName) {
        if (f == null || newName == null) {
            Log.e(LOG_TAG, "Rename: null parameter");
            return false;
        }
       
        File file = new File(f.filePath);
        
        File parentFile = new File(Util.getPathFromFilepath(f.filePath));
        String parentFileListsName[];
        parentFileListsName=parentFile.list();
        for(String filename : parentFileListsName)
        {
            //modify by tyd heqianqian for ignorecase when rename on 20151215
            if(filename.equalsIgnoreCase(newName) ){
                return false;
            }
        }
        mNewPath = Util.makePath(Util.getPathFromFilepath(f.filePath), newName);
        //*/add by mingjun on 2016-01-04 for rename sub file
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            for (File fs : listFiles) {
                ContentResolver mediaProvider = mContext.getContentResolver();
                Uri uri = MediaStore.Files.getContentUri("external");
                uri = uri.buildUpon().appendQueryParameter("mtk_filemanager", "true").build();
                String where = MediaStore.Files.FileColumns.DATA + "=?";
                String newSuPath = fs.getPath().replace(f.filePath, mNewPath);
                String[] subwhereArgs = new String[] {fs.getPath()};
                ContentValues subvalues = new ContentValues();
                 subvalues.put(MediaStore.Files.FileColumns.DATA, newSuPath);
                mediaProvider.update(uri, subvalues, where, subwhereArgs);
            }
        }
        
        try {
            boolean ret = file.renameTo(new File(mNewPath));
            mOldFilePath = f.filePath;
            ContentResolver mediaProvider = mContext.getContentResolver();
            Uri uri = MediaStore.Files.getContentUri("external");
            uri = uri.buildUpon().appendQueryParameter("mtk_filemanager", "true").build();
            String where = MediaStore.Files.FileColumns.DATA + "=?";
            String[] whereArgs = new String[] {mOldFilePath};

            ContentValues values = new ContentValues();
            values.put(MediaStore.Files.FileColumns.DATA, mNewPath);
            values.put(MediaStore.Files.FileColumns.DISPLAY_NAME, newName);
            /*/ modify by freemeos.liuhaoran on 20160709 for do not fix title
            values.put(MediaStore.Files.FileColumns.TITLE, newName);
            //*/
            values.put(MediaStore.Files.FileColumns.DATE_MODIFIED ,System.currentTimeMillis()/1000);
            whereArgs = new String[] { mOldFilePath };
            try {
                mediaProvider.update(uri, values, where, whereArgs);
                scanPathforMediaStore(newName);
                deleteFileRecord(mOldFilePath);
            } catch (NullPointerException e) {
                Log.e(LOG_TAG, "Error, NullPointerException:" + e + ",update db may failed!!!");
            }
          //*/end
            ArrayList<FileInfo> fileInfoList = new ArrayList<FileInfo>();
            fileInfoList.add(f);
            mOperationListener.onFileChanged(GlobalConsts.TYPE_NOTIFY_REFRESH);
            return ret;
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Fail to rename file," + e.toString());
        }
        return false;
    }
    
  //*/ add by droi liuhaoran for Update the media library on 20160426
    public void scanPathforMediaStore(String path) {
        if (mContext != null && !TextUtils.isEmpty(path)) {
            String[] paths = { path };
            MediaScannerConnection.scanFile(mContext, paths, null, null);

            
        }
    }
    //*/
    
  //*/ add by droi liuhaoran for Update the media library on 20160426
    private void deleteFileRecord(String path){
        
        if (TextUtils.isEmpty(path)) {
            return;
        }
        Uri uri = MediaStore.Files.getContentUri("external");
        StringBuffer sb = new StringBuffer();
        sb.append(MediaStore.Files.FileColumns.DATA + " LIKE '" );
        sb.append(path);
        sb.append("%'");
        if (mContext != null) {
            ContentResolver cr = mContext.getContentResolver();
            Log.i(LOG_TAG, "path=" + path);
                    cr.delete(uri, sb.toString(), null);
                    
        }
    }
    //*/
    
    public boolean Delete(ArrayList<FileInfo> files) {
        taskType = GlobalConsts.TYPE_NOTIFY_REFRESH;
        copyFileList(files);
        asnycExecute(new Runnable() {
            @Override
            public void run() {
                Iterator fileIterator = mCurFileNameList.iterator();
                while (fileIterator.hasNext()) {
                    FileInfo f  = (FileInfo) fileIterator.next();
                    synchronized (f) {
                        if (mAsyncTask == null) {
                            break;
                        }
                        DeleteFile(f);
                    }
                }
                
//              for (FileInfo f : mCurFileNameList) {
//                  synchronized (f) {
//                      if (mAsyncTask == null) {
//                          break;
//                      }
//                      DeleteFile(f);
//                  }
//              }
            }
        });
        return true;
    }
    
    public void cancelFileOperation(){
        setCancelFileOperation(true);
    }
    
    public void setCancelFileOperation(boolean setting){
        cancelfileoperation = setting;
    }
    
    public boolean isCancelFileOperation() {
        return cancelfileoperation;
    }
    
    protected void DeleteFile(FileInfo f) {
        if(isCancelFileOperation()){
            if(mAsyncTask != null && mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                mAsyncTask.cancel(true);
            }
            if(mAsyncTask.isCancelled()){
                mAsyncTask = null;
                setCancelFileOperation(false);
                clear();
                return;
            }
        }
        if (f == null) {
            Log.e(LOG_TAG, "DeleteFile: null parameter");
            return;
        }

        File file = new File(f.filePath);
        boolean directory = file.isDirectory();
        if (directory) {
            for (File child : file.listFiles(mFilter)) {
                if (Util.isNormalFile(child.getAbsolutePath())) {
                    DeleteFile(Util.GetFileInfo(child, mFilter, true));
                }
            }
        }

        if (!file.delete())
            return;
        deleteFileRecord(f);
    }

    private void deleteFileRecord(FileInfo fileInfo){
        if (fileInfo != null){
            ContentResolver contentResolver = mContext.getContentResolver();
            Uri localUri = MediaStore.Files.getContentUri("external");
            String[] astringArray = new String[1];
            astringArray[0] = fileInfo.filePath;
            contentResolver.delete(localUri, "_data=?", astringArray);
        }
    }
    
    private String CopyFile(FileInfo f, String dest) {
        String copyDestFile = null;
        if(isCancelFileOperation()){
            if(mAsyncTask != null && mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                mAsyncTask.cancel(true);
            }
            if(mAsyncTask.isCancelled()){
                mAsyncTask = null;
                setCancelFileOperation(false);
                clear();
                return null;
            }
        }
        if (f == null || dest == null) {
            Log.e(LOG_TAG, "CopyFile: null parameter");
            return null;
        }
        File file = new File(f.filePath);
        //*/ Added by tyd wulianghuan 2013-12-11 for: file could be not exist, when the storage device is bad removed during copy
        if(!file.exists()){
            return null;
        }
        //*/
        if (file.isDirectory()) {

            // directory exists in destination, rename it
            String destPath = Util.makePath(dest, f.fileName);
            copyDestFile = destPath;
           
            File destFile = new File(destPath);
            int i = 1;
            while (destFile.exists()) {
                destPath = Util.makePath(dest, f.fileName + " " + i++);
                destFile = new File(destPath);
            }
            if(!destFile.exists()){
                destFile.mkdir();
            }
            for (File child : file.listFiles(mFilter)) {
                if (!child.isHidden() && Util.isNormalFile(child.getAbsolutePath())) {
                    CopyFile(Util.GetFileInfo(child, mFilter, Settings.instance().getShowDotAndHiddenFiles()), destPath);
                }
            }
        } else {
            copyDestFile = Util.copyFile(f.filePath, dest);
        }
        return copyDestFile;
    }

    private boolean MoveFile(FileInfo f, String dest) {
        if(isCancelFileOperation()){
            if(mAsyncTask != null && mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                mAsyncTask.cancel(true);
            }
            if(mAsyncTask.isCancelled()){
                mAsyncTask = null;
                setCancelFileOperation(false);
                clear();
                return false;
            }
        }
        if (f == null || dest == null) {
            Log.e(LOG_TAG, "CopyFile: null parameter");
            return false;
        }

        File file = new File(f.filePath);
        String newPath = Util.makePath(dest, f.fileName);
//      if(!f.filePath.equals(newPath)){
        if(Util.isInSameVolume(f.filePath, newPath)){
            try {
             //*/ modify by freemeos.liuhaoran on 20160627 for Under the same file, the file paste disappear after cut
             //modify by mingjun for update media database
               Boolean succeed = false;
               if(newPath != null&&!newPath.equals(f.filePath)){
                   succeed = file.renameTo(new File(newPath));
              //*/
               if(succeed){
                  deleteFileRecord(f);
               }
               }
                return succeed;
            //end
            } catch (SecurityException e) {
                Log.e(LOG_TAG, "Fail to move file," + e.toString());
            }
        }else{
            String destpath = CopyFile(f, dest);
            if(destpath != null){
                DeleteFile(f);
                return true;
            }}
//        }
        return false;
    }

    private void copyFileList(ArrayList<FileInfo> files) {
        synchronized(mCurFileNameList) {
            mCurFileNameList.clear();
            for (FileInfo f : files) {
                mCurFileNameList.add(f);
            }
        }
    }
    
    public boolean compress(final String saveName)
    {
        taskType = GlobalConsts.TYPE_NOTIFY_SCAN;
        asnycExecute(new Runnable() {
            @Override
            public void run() {
                try{
                    FileInfo fileInfo = mCurFileNameList.get(0);
                    String filePath = fileInfo.filePath;
                    String savePath = filePath.substring(0, filePath.lastIndexOf("/")+1)+saveName+".zip";
                    ArchiveHelper.compressZipArchive(mCurFileNameList, savePath, "GBK", saveName);
                    if(isCancelFileOperation()){
                        File file = new File(savePath);
                        if(file.exists()){
                            file.delete();
                        }
                        if(mAsyncTask != null && mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                            mAsyncTask.cancel(true);
                        }
                        if(mAsyncTask.isCancelled()){
                            mAsyncTask = null;
                            setCancelFileOperation(false);
                            clear();
                        }
                    }
                }catch (Exception e) {
                    Log.i(LOG_TAG, "compress occur exception: "+e.toString());
                }
            }
        });
        
        return true;
    }
    
    public boolean deCompress(final String zipFilePath, final String destPath) {
        taskType = GlobalConsts.TYPE_NOTIFY_SCAN;
        setCancelFileOperation(false);
        new AsyncTask<Object, Object, Integer>() {
            @Override
            protected Integer doInBackground(Object[] paramArrayOfObject) {
                int state = 0;
                try{
                    state = ArchiveHelper.decompressZipArchive(zipFilePath, destPath);
                    Log.i("heqianqian","***********state"+state);
                }catch (Exception e) {
                }
                if(Util.getCurMemoryFreeSize(destPath) == 0){
                    state = GlobalConsts.DECOMPRESS_ZIP_STATE_NO_FREE;
                    Log.i("heqianqian","***********state"+state);
                }
                return state;
            }
            @Override
            protected void onPostExecute(Integer state) {
                if(GlobalConsts.DECOMPRESS_ZIP_STATE_FILE_EXISTS == state){
                    Toast.makeText(mContext, R.string.decompress_failed_for_exists, Toast.LENGTH_LONG).show();
                }else if(GlobalConsts.DECOMPRESS_ZIP_STATE_NO_FREE == state){
                    Toast.makeText(mContext, R.string.decompress_failed_for_nofress, Toast.LENGTH_LONG).show();
                }else if(GlobalConsts.DECOMPRESS_ZIP_STATE_CANCEL== state){
                    
                }
               //*/add by tyd liuyong 20140723
             else if(GlobalConsts.DECOMPRESS_ZIP_STATE_NO_FILE == state){
                    Toast.makeText(mContext, R.string.decompress_failed_for_nofile, Toast.LENGTH_LONG).show();
                }
               //*/
                if (mOperationListener != null) {
                    mOperationListener.onFinish();
                }
                mOperationListener.onFileChanged(taskType);
            }
        }.execute(new Object[0]);
        return true;
    }
}
