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
package com.freeme.filemanager.controller;


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.R.drawable;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.freeme.filemanager.FileExplorerTabActivity;
import com.freeme.filemanager.FileManagerApplication;
import com.freeme.filemanager.R;
import com.freeme.filemanager.controller.FileManagerOperationActivity;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.ArchiveHelper;
import com.freeme.filemanager.util.FavoriteDatabaseHelper;
import com.freeme.filemanager.util.FileOperationHelper;
import com.freeme.filemanager.util.FileSortHelper;
import com.freeme.filemanager.util.MimeUtils;
import com.freeme.filemanager.util.StorageHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.util.FileCategoryHelper.FileCategory;
import com.freeme.filemanager.util.FileOperationHelper.IOperationProgressListener;
import com.freeme.filemanager.util.FileSortHelper.SortMethod;
import com.freeme.filemanager.util.Util.MemoryCardInfo;
import com.freeme.filemanager.util.Util.SDCardInfo;
import com.freeme.filemanager.util.Util.UsbStrogeInfo;
import com.freeme.filemanager.view.FileCategoryFragment;
import com.freeme.filemanager.view.FileExplorerPreferenceActivity;
import com.freeme.filemanager.view.FileViewFragment;
import com.freeme.filemanager.view.InformationDialog;
import com.freeme.filemanager.view.MenoryInfoFileListActivity;
import com.freeme.filemanager.view.MoneyInfoActivity;
import com.freeme.filemanager.view.PathGallery;
import com.freeme.filemanager.view.SearchActivity;
import com.freeme.filemanager.view.Settings;
import com.freeme.filemanager.view.TextInputDialog;
import com.freeme.filemanager.view.FileListItem.ModeCallback;
import com.freeme.filemanager.view.FileViewFragment.SelectFilesCallback;
import com.freeme.filemanager.view.PathGallery.IPathItemClickListener;
import com.freeme.filemanager.view.TextInputDialog.OnFinishListener;

import android.os.storage.StorageVolume;
import com.freeme.filemanager.util.FeatureOption;
import com.freeme.updateself.update.UpdateMonitor;

import com.mediatek.hotknot.HotKnotAdapter;
import android.os.SystemProperties;

public class FileViewInteractionHub implements IOperationProgressListener, IPathItemClickListener, FileExplorerTabActivity.HotknotCompleteListener {
    private static final String LOG_TAG = "FileViewInteractionHub";

    private IFileInteractionListener mFileViewListener;

    private ArrayList<FileInfo> mCheckedFileNameList = new ArrayList<FileInfo>();

    private FileOperationHelper mFileOperationHelper;

    private FileSortHelper mFileSortHelper;

    private View mConfirmOperationBar;

    private ProgressDialog progressDialog;

    private ProgressBar mRefreshProgressBar;
    
    private View mDropdownNavigation;

    private Context mContext;
    
    private PathGallery mPathGallery;
    
    private static final String ROOT_DIR = "/mnt";
    
    public int mTabIndex;

    private static final int FILE_NAME_LENGTH = 85;
    
    private static final int SEND_MAX_FILE_SIZE = 127;
    
    public String mDeComPressFilePath = null;

    private boolean mHotKnotWaitSend = false;

    public FileExplorerTabActivity fileExplorerTabActivity;


    //add by tyd liuyong 201406014 for action_delete NullPointerException after click action_compress cancel quickly
    public boolean mDeleteFlag =true;
    public boolean isCopy=false;
    
    public enum Mode {
        View, Pick
    };

    public FileViewInteractionHub(IFileInteractionListener fileViewListener, int tabIndex) {
        assert (fileViewListener != null);
        mFileViewListener = fileViewListener;
        mTabIndex = tabIndex;
        setup();
        mContext = mFileViewListener.getContext();
        mFileOperationHelper = new FileOperationHelper(this, mContext);
        mFileSortHelper = new FileSortHelper();

    }

    public void showProgress(String str_title, String str_msg, boolean showCancelButton) {
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setTitle(str_title);
        progressDialog.setMessage(str_msg);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        if(showCancelButton){
            progressDialog.setButton(mContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int i)
                {
                     //modify by tyd liuyong 20140514 for action_delete NullPointerException
                      mDeleteFlag =false; 
                      mFileOperationHelper.cancelFileOperation();
                    //modify by mingjun for show delete file
                      refreshFileList();
                    //end
                      dialog.cancel();
                    showConfirmOperationBar(false);
                  //modify by tyd liuyong 20140514 for action_delete NullPointerException
                  mDeleteFlag =true; 
                }
            });
        }
        progressDialog.show();
    }

    public void showLoadingProgress(String str_title, String str_msg, boolean showCancelButton) {
        if (showCancelButton) {
            Log.i("liuhaoran", "showLoadingProgress.show");
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setTitle(str_title);
            progressDialog.setMessage(str_msg);
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }else {
            Log.i("liuhaoran", "showLoadingProgress.dismiss");
            progressDialog.dismiss();
        }
    }


    public void sortCurrentList() {
        mFileViewListener.sortCurrentList(mFileSortHelper);
    }

    public boolean canShowCheckBox(String file_path ) {
        if(file_path!=null&&file_path.equals(ROOT_DIR)){
            return false;
        }else {
            return mConfirmOperationBar.getVisibility() != View.VISIBLE;            
        }
    }

    public void showConfirmOperationBar(boolean show) {
        isCopy=show;
        mFileViewListener.onRefreshMenu(show);
        mConfirmOperationBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void addContextMenuSelectedItem() {
        if (mCheckedFileNameList.size() == 0) {
            int pos = mListViewContextMenuSelectedItem;
            if (pos != -1) {
                FileInfo fileInfo = mFileViewListener.getItem(pos);
                if (fileInfo != null) {
                    mCheckedFileNameList.add(fileInfo);
                }
            }
        }
    }

    public ArrayList<FileInfo> getSelectedFileList() {
        return mCheckedFileNameList;
    }

    public boolean canPaste() {
        return mFileOperationHelper.canPaste();
    }

    // operation finish notification
    @Override
    public void onFinish() {
        DismissProgressDialog();
        mFileViewListener.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showConfirmOperationBar(false);
                refreshFileList();
            }
        });
    }
    
    public void DismissProgressDialog(){
        if (isLoadingShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public FileInfo getItem(int pos) {
        return mFileViewListener.getItem(pos);
    }

    public boolean isInSelection() {
        if(mCheckedFileNameList != null){
        return mCheckedFileNameList.size() > 0;
        }else {
            return false;
        }
    }

    public boolean isMoveState() {
        return mFileOperationHelper.isMoveState() || mFileOperationHelper.canPaste();
    }
    //added by tyd shixiaopeng 20140508 for copy and move
    public boolean inMoveState() {
        return mFileOperationHelper.isMoveState() && mFileOperationHelper.canPaste();
    }
    //end

    private void setup() {
        setupFileListView();
        setupPathGallery();
        setupOperationPane();
    }

    // buttons
    private void setupOperationPane() {
        mConfirmOperationBar = mFileViewListener.getViewById(R.id.moving_operation_bar);
        setupClick(mConfirmOperationBar, R.id.button_moving_confirm);
        setupClick(mConfirmOperationBar, R.id.button_moving_cancel);
    }

    private void setupClick(View v, int id) {
        View button = (v != null ? v.findViewById(id) : mFileViewListener.getViewById(id));
        if (button != null)
            button.setOnClickListener(buttonClick);
    }

    private View.OnClickListener buttonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.button_moving_confirm:
                    if(TextUtils.isEmpty(mDeComPressFilePath)){
                        onOperationButtonConfirm();
                    }else{
                        onOperationDeCompress();
                    }
                    break;
                case R.id.button_moving_cancel:
                    onOperationButtonCancel();
                    break;
            }
        }

    };

    public void onOperationReferesh() {
        refreshFileList();

        if (getRefresh()) {
            Toast.makeText(mContext,mContext.getString(R.string.refresh_over),Toast.LENGTH_SHORT).show();
        }
    }

    private boolean refresh = false;

    public boolean getRefresh() {
        return refresh;
    }

    public void setRefresh(boolean flag) {
        refresh = flag;
    }

    private void onOperationFavorite() {
        String path = mCurrentPath;

        if (mListViewContextMenuSelectedItem != -1) {
            path = mFileViewListener.getItem(mListViewContextMenuSelectedItem).filePath;
            Log.i(LOG_TAG, "path=" +path);
        }

        onOperationFavorite(path);
        Log.i(LOG_TAG, "path=" +path);
        refreshFileList();
    }

    private void onOperationSetting() {
        Intent intent = new Intent(mContext, FileExplorerPreferenceActivity.class);
        if (intent != null) {
            try {
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, "fail to start setting: " + e.toString());
            }
        }
    }

    private void onOperationFavorite(String path) {
        FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper.getInstance();
        if (databaseHelper != null) {
            int stringId = 0;
            if (databaseHelper.isFavorite(path)) {
                databaseHelper.delete(path);
                stringId = R.string.removed_favorite;
            } else {
                databaseHelper.insert(Util.getNameFromFilepath(path), path);
                stringId = R.string.added_favorite;
            }
            Toast.makeText(mContext, stringId, Toast.LENGTH_SHORT).show();
        }
    }

    public void onOperationShowSysFiles() {
        Settings.instance().setShowDotAndHiddenFiles(!Settings.instance().getShowDotAndHiddenFiles());
        //*/add by tyd sxp 20140910 for hidden file bug
        notifyRefreshViewInfo();
        //*/end
        refreshFileList();
    }

    public void onOperationSelectAllOrCancel() {
        //add by tyd liuyong 20140814 for protect file option
        if(mFileOperationHelper.cancelfileoperation){
            Toast.makeText(mContext, R.string.cancel_option_note, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isSelectedAll()) {
            onOperationSelectAll();
        } else {
            clearSelection();
        }
    }

    public void onOperationSelectAll() {
        mCheckedFileNameList.clear();
        for (FileInfo f : mFileViewListener.getAllFiles()) {
            f.Selected = true;
            mCheckedFileNameList.add(f);
        }
        
        //*/ freeme.liuhaoran , 20160723 , add the new function about the page of internal and sd can click 
        if(mContext instanceof  IActionModeCtr){
            IActionModeCtr actionmodeCtr = ((IActionModeCtr) mContext);
            ActionMode mode = actionmodeCtr.getActionMode();
            if (mode == null) {
                mode = actionmodeCtr.startActionMode(new ModeCallback(mContext, this));
                actionmodeCtr.setActionMode(mode);
                Util.updateActionModeTitle(mode, mContext, getSelectedFileList().size());
            }
            //*/
        }
        mFileViewListener.onDataChanged();
    }

    public boolean onOperationUpLevel() {
//        showDropdownNavigation(false);
        if (mFileViewListener.onOperation(GlobalConsts.OPERATION_UP_LEVEL)) {
            return true;
        }

        if (!TextUtils.isEmpty(mRoot) && !mRoot.equals(mCurrentPath)) {
            String parentPath = new File(mCurrentPath).getParent();
            if(!ROOT_DIR.equals(parentPath)){
                mCurrentPath = parentPath;
                refreshFileList();
                return true;
            }
        }

        return false;
    }

    public void onOperationCreateFolder() {
        if(getCurMemoryFreeSize(mCurrentPath) == 0){
            Toast.makeText(mContext, R.string.insufficient_memory, Toast.LENGTH_SHORT).show();
            return;
        }
        TextInputDialog dialog = new TextInputDialog(mContext, mContext.getString(
                R.string.operation_create_folder), mContext.getString(R.string.operation_create_folder_message),
                mContext.getString(R.string.new_folder_name), new OnFinishListener() {
                    @Override
                    public boolean onFinish(String text) {
                        if (text.length() <= 0 || text.matches(".*[/\\\\:*?\"<>|].*")) {
                            if(text.length() <= 0){
                                Toast.makeText(mContext, R.string.invalid_empty_name, Toast.LENGTH_SHORT).show();
                            }
                            // characters not allowed
                            if (text.matches(".*[/\\\\:*?\"<>|].*")) {
                                Toast.makeText(mContext, R.string.warn_invalid_forlder_name, Toast.LENGTH_SHORT).show();
                            }
                            return false;
                        } 
                        return doCreateFolder(text);
                    }
                });
        //start
        // add by droi heqianqian on 20151223*/
        dialog.fileNamelenth=mContext.getString(R.string.new_folder_name).length();
        dialog.show();
        //end*/
    }

    private boolean doCreateFolder(String text) {
        if (TextUtils.isEmpty(text))
            return false;
        int textLength = text.length();
        if(textLength >= FILE_NAME_LENGTH){
            Toast.makeText(mContext, R.string.invalid_file_name, Toast.LENGTH_LONG).show();
            return false;
        }
        
        // if free is 0 bit
        if (mFileOperationHelper.CreateFolder(mCurrentPath, text.trim())) {
            Toast.makeText(mContext, mContext.getString(R.string.succeed_to_create_folder), Toast.LENGTH_SHORT).show();
            mFileViewListener.addSingleFile(Util.GetFileInfo(Util.makePath(mCurrentPath, text)));
            mFileListView.setSelection(mFileListView.getCount() - 1);
        } else {
            new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.fail_to_create_folder))
                    .setPositiveButton(R.string.confirm, null).create().show();
            return false;
        }
        refreshFileList();
        return true;
    }

    public void onOperationSearch() {
        Intent intent = new Intent(mContext, SearchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    public void onSortChanged(SortMethod s) {
        if (mFileSortHelper.getSortMethod() != s) {
            mFileSortHelper.setSortMethod(s);
            sortCurrentList();
        }
    }

    public void onOperationCopy() {
        onOperationCopy(getSelectedFileList());
    }

    public void onOperationCopy(ArrayList<FileInfo> files) {
        mFileOperationHelper.Copy(files);
        clearSelection();

        showConfirmOperationBar(true);
        Button confirmButton = (Button)mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        confirmButton.setEnabled(false);
        // refresh to hide selected files
        refreshFileList();
    }

    public void onOperationCompress() {
        if (mCheckedFileNameList.size() == 0){
            return;
        }
        if(getCurMemoryFreeSize(mCurrentPath) == 0){
            Toast.makeText(mContext, R.string.insufficient_memory, Toast.LENGTH_SHORT).show();
            return;
        }
        FileInfo fileinfo = getSelectedFileList().get(0);
        mFileOperationHelper.Copy(getSelectedFileList());
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.compress_dialog, null);
        EditText compressNameEdit = (EditText) view.findViewById(R.id.compress_name);
        String compressName = "";
        if(mCheckedFileNameList.size() == 1){
            compressName = Util.getFormatedFileName(mCheckedFileNameList.get(0).fileName);
        }else{
            compressName = mCurrentPath.substring(mCurrentPath.lastIndexOf("/")+1);
        }
        compressNameEdit.setText(compressName);
        compressNameEdit.setSelection(compressName.length());
        final String saveDir = mCurrentPath;
        clearSelection();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
        .setTitle(mContext.getString(R.string.operation_compress))
        .setView(view)
        .setPositiveButton(mContext.getString(R.string.ok), new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int id) {
                EditText compressName = (EditText) view.findViewById(R.id.compress_name);
                String saveName = compressName.getText().toString();
                if(TextUtils.isEmpty(saveName)){
                    Toast.makeText(mContext, R.string.operation_compress_invalid_name, Toast.LENGTH_LONG).show();
                    dialog.cancel();
                }else{
                    String savePath = saveDir+"/"+saveName+".zip";
                    if((new File(savePath)).exists()){
                        Toast.makeText(mContext, R.string.compress_failed_for_exists, Toast.LENGTH_LONG).show();
                        dialog.cancel();
                    }else{
                        if (mFileOperationHelper.compress(saveName)) {
                            showProgress(mContext.getString(R.string.operation_compress), mContext.getString(R.string.operation_compressing), true);
                        }
                    }
                }
            }   
        })
        .setNegativeButton(mContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface arg0) {
                onOperationButtonCancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    public void updateBarForDeCompress(){
        showConfirmOperationBar(true);
        Button confirmButton = (Button)mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        confirmButton.setText(mContext.getString(R.string.operation_decompress));
//        confirmButton.setEnabled(false);
    }
    
    public void onOperationDeCompress() {
        if(!TextUtils.isEmpty(mDeComPressFilePath)){
            if (mFileOperationHelper.deCompress(mDeComPressFilePath, mCurrentPath)) {
                showProgress(mContext.getString(R.string.operation_decompress), mContext.getString(R.string.operation_decompressing), true);
            }
            mDeComPressFilePath = null;
        }
    }
    
    private void copy(CharSequence text) {
        ClipboardManager cm = (ClipboardManager) mContext.getSystemService(
                Context.CLIPBOARD_SERVICE);
        cm.setText(text);
    }
    
    public long getFileListsSize(String path){
        long fileSize = 0;
        File file = new File(path);
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (listFiles == null){
                return fileSize;
            }
            for (File f : listFiles) {
                fileSize = fileSize + getFileListsSize(f.getPath());
            }
        } else {
            fileSize = file.length();
        }
        return fileSize;
    }
        
    public boolean isCopyFreeMemorySizeEnough(ArrayList<FileInfo> files, String path){
        long filesSize = 0;
        SDCardInfo sdCardInfo = Util.getSDCardInfo();
        MemoryCardInfo memoryCardInfo = Util.getMemoryCardInfo();
        UsbStrogeInfo usbStrogeInfo = Util.getUsbStorgeInfo();
        for(FileInfo f : files){
            if(f != null){
                filesSize = filesSize + getFileListsSize(f.filePath);
            }
        }
        //modify by tyd liuyong 20140806 for kk storage
        if(FeatureOption.MTK_MULTI_STORAGE_SUPPORT){
            if(Util.isSdcardExist()){
                if(path.startsWith(Util.MEMORY_DIR)){
                    if(filesSize < memoryCardInfo.free){
                        return true;
                    }else {
                        return false;
                    }
                }else if(path.startsWith(Util.SD_DIR)){
                    if(filesSize < sdCardInfo.free){
                        return true;
                    }else {
                        return false;
                    }
                }else if(path.startsWith(Util.USBOTG_DIR)){
                    if(filesSize < usbStrogeInfo.free){
                        return true;
                    }else {
                        return false;
                    }
                }
            }else {
                if(path.startsWith(Util.MEMORY_DIR)){
                    if(filesSize < memoryCardInfo.free){
                        return true;
                    }else {
                        return false;
                    }
                }else if(path.startsWith(Util.USBOTG_DIR)){
                    if(filesSize < usbStrogeInfo.free){
                        return true;
                    }else {
                        return false;
                    }
                }
            }
        }else{

            if(path.startsWith(Util.MEMORY_DIR)){
                if(filesSize < memoryCardInfo.free){
                    return true;
                }else {
                    return false;
                }
            }else if(path.startsWith(Util.USBOTG_DIR)){
                if(filesSize < usbStrogeInfo.free){
                    return true;
                }else {
                    return false;
                }
            }
        }
        return true;
    }
    
    private long getCurMemoryFreeSize(String currentPath){
        long freeSize = 0;
        SDCardInfo sdCardInfo = Util.getSDCardInfo();
        MemoryCardInfo memoryCardInfo = Util.getMemoryCardInfo();
        UsbStrogeInfo usbStrogeInfo = Util.getUsbStorgeInfo();
        //modify by droi mingjun 20160104 for storage
        if(true){
//          if(Util.isSdcardExist()){
                if(currentPath.startsWith(Util.MEMORY_DIR)){
                    freeSize = memoryCardInfo.free;
                }else if(currentPath.startsWith(Util.SD_DIR)){
                    freeSize = sdCardInfo.free;
                }else if(currentPath.startsWith(Util.USBOTG_DIR)){
                    freeSize = usbStrogeInfo.free;
                }
//          }else {
//              if(currentPath.startsWith("/storage/sdcard0")){
//                  freeSize = memoryCardInfo.free;
//              }else if(currentPath.startsWith("/storage/usbotg")){
//                  freeSize = usbStrogeInfo.free;
//              }
//          }
//         }else{
//          if(currentPath.startsWith("/storage/emulated/0")){
//              freeSize = memoryCardInfo.free;
//          }else if(currentPath.startsWith("/storage/usbotg")){
//              freeSize = usbStrogeInfo.free;
//          }
        }
        return freeSize;
     }


    private void onOperationPaste() {
        if (!isCopyFreeMemorySizeEnough(mFileOperationHelper.mCurFileNameList, mCurrentPath)) {
            Toast.makeText(mContext, R.string.insufficient_memory, Toast.LENGTH_LONG).show();
            onOperationButtonCancel();
            return;
        }
        if (mFileOperationHelper.Paste(mCurrentPath)) {
            showProgress(mContext.getString(R.string.operation_copy), mContext.getString(R.string.operation_pasting), true);
        }
    }

    public void onOperationMove() {
        if(isFavoritedFile()){
            Toast.makeText(mContext, R.string.removed_favorite_first, Toast.LENGTH_LONG).show();
            refreshFileList();
            return;
        }
        mFileOperationHelper.StartMove(getSelectedFileList());
        clearSelection();
        showConfirmOperationBar(true);
        View confirmButton = mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        confirmButton.setEnabled(false);
        // refresh to hide selected files
        refreshFileList();
    }
    
    public void refreshFileList() {
        clearSelection();
        // onRefreshFileList returns true indicates list has changed
        mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);
        // update move operation button state
        updateConfirmButtons();

    }

    private void updateConfirmButtons() {
        if (mConfirmOperationBar.getVisibility() == View.GONE)
            return;

        Button confirmButton = (Button) mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        int text = R.string.operation_paste;
        if (isSelectingFiles()) {
            confirmButton.setEnabled(mCheckedFileNameList.size() != 0);
            text = R.string.operation_send;
        }else if(!TextUtils.isEmpty(mDeComPressFilePath)){
            text = R.string.operation_decompress;
            confirmButton.setEnabled(true);
        } else if (isMoveState()) {
            confirmButton.setEnabled(mFileOperationHelper.canMove(mCurrentPath));
        }
        confirmButton.setText(text);
    }

    public void onOperationSend() {
        ArrayList<FileInfo> selectedFileList = getSelectedFileList();
        int listSize = selectedFileList.size();
        if(listSize > SEND_MAX_FILE_SIZE){
            Toast.makeText(mContext, R.string.send_file_max_size, Toast.LENGTH_LONG).show();    
            clearSelection();
            return;
        }
        for (FileInfo f : selectedFileList) {
            if (f.IsDir) {
                AlertDialog dialog = new AlertDialog.Builder(mContext).setMessage(
                        R.string.error_info_cant_send_folder).setPositiveButton(R.string.confirm, null).create();
                dialog.show();
                clearSelection();
                return;
            }
        }
            
        Intent intent = IntentBuilder.buildSendFile(selectedFileList);
        if (intent != null) {
            try {
                mFileViewListener.startActivity(Intent.createChooser(intent, mContext.getString(R.string.send_file)));
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, "fail to view file: " + e.toString());
            }
        }
        clearSelection();
    }
    public boolean isFavoritedFile(){
        String path = mCurrentPath;
        ArrayList<FileInfo> selectedFiles = new ArrayList<FileInfo>(getSelectedFileList());
        if(mFileViewListener == null){
            return false;           
        }
        if (mListViewContextMenuSelectedItem != -1) {
            if((mFileViewListener.getItem(mListViewContextMenuSelectedItem) != null)){
                path = mFileViewListener.getItem(mListViewContextMenuSelectedItem).filePath;
            }else {
                return false;                           
            }
        }
        
        FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper.getInstance();
        if (databaseHelper != null) {
             Log.i(LOG_TAG, "isFavoritedFile-->path is " + path);
            if (databaseHelper.isFavorite(path)) {
                return true;
            }
        }    
        
        if((databaseHelper != null) && selectedFiles != null){
            for (FileInfo f : selectedFiles){
                  Log.i(LOG_TAG, "isFavoritedFile-->f.filePath is " + f.filePath);
                if (databaseHelper.isFavorite(f.filePath)) {
                    return true;
                }
            }
        }    
        return false;
    }

    public void onOperationRename() {
        int pos = mListViewContextMenuSelectedItem;
        if(isFavoritedFile()){
            Toast.makeText(mContext, R.string.removed_favorite_first, Toast.LENGTH_LONG).show();
            refreshFileList();
            return;
        }
        if (pos == -1)
            return;

        if (getSelectedFileList().size() == 0)
            return;

        final FileInfo f = getSelectedFileList().get(0);
        clearSelection();
        //modify by tyd zhuya 20151210,if the filename is too long,it can't know  the type of file when you rename.
        final String extFromFilename = Util.getExtFromFilename(f.filePath);
        String  selectFileName;
        if(f.IsDir|| "".equals(extFromFilename)){
            selectFileName = f.fileName.toString();
        }else{
            selectFileName = Util.getNameFromFilename(f.fileName);
        }

        TextInputDialog dialog = new TextInputDialog(mContext, mContext.getString(R.string.operation_rename),
                mContext.getString(R.string.operation_rename_message), f.fileName, new OnFinishListener() {
            @Override
            public boolean onFinish(String text) {
                return doRename(f, text);
            }

        });
        dialog.fileNamelenth=selectFileName.length();
            dialog.show();
    }

    private boolean doRename(final FileInfo f, String text) {
        if (TextUtils.isEmpty(text))
            return false;

        int textLength = text.length();
        if(textLength >= FILE_NAME_LENGTH){
            Toast.makeText(mContext, R.string.invalid_file_rename, Toast.LENGTH_LONG).show();   
            return false;
        }
        
        if (mFileOperationHelper.Rename(f, text)) {
            f.fileName = text;
            mFileViewListener.onDataChanged();
        } else {
            new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.fail_to_rename))
                    .setPositiveButton(R.string.confirm, null).create().show();
            return false;
        }
        refreshFileList();
        return true;
    }
    
    public void onOperationDelete() {
        if(isFavoritedFile()){
            Toast.makeText(mContext, R.string.removed_favorite_first, Toast.LENGTH_LONG).show();
            refreshFileList();
            exitActionMode();
            return;
        }
        doOperationDelete(getSelectedFileList());
    }

    public void onOperationDelete(int position) {
        FileInfo file = mFileViewListener.getItem(position);
        if (file == null)
            return;

        ArrayList<FileInfo> selectedFileList = new ArrayList<FileInfo>();
        selectedFileList.add(file);
        doOperationDelete(selectedFileList);
    }

    private void doOperationDelete(final ArrayList<FileInfo> selectedFileList) {
        final ArrayList<FileInfo> selectedFiles = new ArrayList<FileInfo>(selectedFileList);
        Dialog dialog = new AlertDialog.Builder(mContext)
                .setMessage(mContext.getString(R.string.operation_delete_confirm_message))
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (mFileOperationHelper.Delete(selectedFiles)) {
                            showProgress(mContext.getString(R.string.operation_delete), mContext.getString(R.string.operation_deleting), true);
                            //*/ add by droi liuhaoran for add Toast on 20160408
                            Toast.makeText(mContext, mContext.getString(R.string.delete) + " " + selectedFiles.size() + " " + mContext.getString(R.string.item), Toast.LENGTH_SHORT).show();
                            //*/
                        }
                        clearSelection();
                        //*/ freeme.liuhaoran , 20160718 , refresh actionMode
                        if(mContext instanceof  IActionModeCtr){
                            ActionMode actionMode = ((IActionModeCtr) mContext).getActionMode();
                            if (actionMode != null) {
                           actionMode.finish();
                            }
                            Util.updateActionModeTitle(actionMode, mContext, mCheckedFileNameList.size());
                        }
                        //*/
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearSelection();
                    }
                }).create();
        dialog.show();
//modify by mingjun for useless refresh
//        refreshFileList();
    }

    public void onOperationInfo() {
        if (getSelectedFileList().size() == 0)
            return;

        FileInfo file = getSelectedFileList().get(0);
        if (file == null)
            return;

        InformationDialog dialog = new InformationDialog(mContext, file);
        dialog.show();
        clearSelection();
    }

    public void onOperationButtonConfirm() {
        if (isSelectingFiles()) {
            mSelectFilesCallback.selected(mCheckedFileNameList);
            mSelectFilesCallback = null;
            clearSelection();
        } else if (mFileOperationHelper.isMoveState()) {
            if (!isCopyFreeMemorySizeEnough(mFileOperationHelper.mCurFileNameList, mCurrentPath)) {
                Toast.makeText(mContext, R.string.insufficient_memory, Toast.LENGTH_LONG).show();
                onOperationButtonCancel();
                return;
            }
            if (mFileOperationHelper.EndMove(mCurrentPath)) {
                showProgress(mContext.getString(R.string.operation_move), mContext.getString(R.string.operation_moving), true);
            }
        } else {
            onOperationPaste();
        }
    }

    public void onOperationButtonCancel() {
        mDeComPressFilePath = null;
        mFileOperationHelper.clear();
        showConfirmOperationBar(false);
        if (isSelectingFiles()) {
            mSelectFilesCallback.selected(null);
            mSelectFilesCallback = null;
            clearSelection();
            refreshFileList();
        } else if (mFileOperationHelper.isMoveState()) {
            // refresh to show previously selected hidden files
            mFileOperationHelper.EndMove(null);
            refreshFileList();
        } else {
            refreshFileList();
        }
    }

    // context menu
    private OnCreateContextMenuListener mListViewContextMenuListener = new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            if (isInSelection() || isMoveState())
                return;

//            showDropdownNavigation(false);

            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

            FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper.getInstance();
            FileInfo file = mFileViewListener.getItem(info.position);
            if(file == null){
                mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);
                return;
            }
            
            if (FeatureOption.MTK_HOTKNOT_SUPPORT) {
                if (HotKnotAdapter.getDefaultAdapter(mContext) != null)
                        addMenuItem(menu, MENU_HOTKNOT, 0, R.string.operation_hotknot);
            }

            if (databaseHelper != null && file != null) {
                int stringId = databaseHelper.isFavorite(file.filePath) ? R.string.operation_unfavorite
                        : R.string.operation_favorite;
                addMenuItem(menu, GlobalConsts.MENU_FAVORITE, 0, stringId);
            }
            //*modify by tyd shixiaopeng 20140505 for delete the copy and move function when mTabIndex=0/
            if (!(mTabIndex == 0)) {
            addMenuItem(menu, GlobalConsts.MENU_COPY, 0, R.string.operation_copy);
            addMenuItem(menu, GlobalConsts.MENU_MOVE, 0, R.string.operation_move);
            }
            //*end/
            if (mTabIndex == 1) {
                if (!ArchiveHelper.checkIfArchive(file.filePath)) {
                    addMenuItem(menu, GlobalConsts.MENU_COMPRESS, 0, R.string.operation_compress);
                }else{
                    addMenuItem(menu, GlobalConsts.MENU_DECOMPRESS, 0, R.string.operation_decompress);
                }
            }
            addMenuItem(menu, MENU_SEND, 0, R.string.operation_send);
            addMenuItem(menu, MENU_RENAME, 0, R.string.operation_rename);
            addMenuItem(menu, MENU_DELETE, 0, R.string.operation_delete);
            addMenuItem(menu, MENU_INFO, 0, R.string.operation_info);

            if (FileManagerApplication.mIsNeedRingTone.equals("true")) {
                int dotPosition = file.fileName.lastIndexOf('.');
                String ext = file.fileName.substring(dotPosition + 1, file.fileName.length()).toLowerCase();
                String mimeType = MimeUtils.guessMimeTypeFromExtension(ext);

                if (mimeType != null) {
                    String strType = mimeType.substring(0, 5);
                    if ("audio".equals(strType)) {
                        addMenuItem(menu, MENU_SETTING, 0, R.string.menu_setting_ring);
                    }
                }
            }
            if (!canPaste()) {
                MenuItem menuItem = menu.findItem(GlobalConsts.MENU_PASTE);
                if (menuItem != null)
                    menuItem.setEnabled(false);
            }
        }
    };

    // File List view setup
    private ListView mFileListView;
    // modify by tyd liuyong 20140728 for fixbug[tyd00530266]
    private int mListViewContextMenuSelectedItem = -1;
    
    private void setupPathGallery() {
        mDropdownNavigation = mFileViewListener.getViewById(R.id.dropdown_navigation);
        mPathGallery = ((PathGallery) this.mFileViewListener.getViewById(R.id.path_gallery_nav));
        mRefreshProgressBar = ((ProgressBar) this.mFileViewListener.getViewById(R.id.refresh_progress));
        mPathGallery.setPathItemClickListener(this);
    }
    
    private void setupFileListView() {
        mFileListView = (ListView) mFileViewListener.getViewById(R.id.file_path_list);
        mFileListView.setLongClickable(true);
        mFileListView.setOnCreateContextMenuListener(mListViewContextMenuListener);
        mFileListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick(parent, view, position, id);
            }
        });
    }

    // menu
    private static final int MENU_SORT = 3;

    private static final int MENU_SEND = 7;

    private static final int MENU_RENAME = 8;

    private static final int MENU_DELETE = 9;

    private static final int MENU_INFO = 10;

    private static final int MENU_SORT_NAME = 11;

    private static final int MENU_SORT_SIZE = 12;

    private static final int MENU_SORT_DATE = 13;

    private static final int MENU_SORT_TYPE = 14;

    private static final int MENU_REFRESH = 15;

    private static final int MENU_SELECTALL = 16;

    private static final int MENU_EXIT = 18;

    private static final int MENU_HOTKNOT = 19;
    
    private static final int MENU_UPDATE = 30;
    
    private static final int MENU_APPABOUT = 31;

    private static final int MENU_SETTING = 32;

    private OnMenuItemClickListener menuItemClick = new OnMenuItemClickListener() {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            mListViewContextMenuSelectedItem = info != null ? info.position : -1;

            int itemId = item.getItemId();
            if (mFileViewListener.onOperation(itemId)) {
                return true;
            }
            addContextMenuSelectedItem();

            switch (itemId) {
                case GlobalConsts.MENU_SEARCH:
                    onOperationSearch();
                    break;
                case GlobalConsts.MENU_NEW_FOLDER:
                    onOperationCreateFolder();
                    break;
                case MENU_REFRESH:
                    onOperationReferesh();
                    break;
                case MENU_SELECTALL:
                    onOperationSelectAllOrCancel();
                    break;
                case GlobalConsts.MENU_SHOWHIDE:
                    onOperationShowSysFiles();
                    break;
                case GlobalConsts.MENU_FAVORITE:
                    onOperationFavorite();
                    break;
                case MENU_EXIT:
                    mFileViewListener.finish();
                    break;
                // sort
                case MENU_SORT_NAME:
                    item.setChecked(true);
                    onSortChanged(SortMethod.name);
                    refreshFileList();
                    break;
                case MENU_SORT_SIZE:
                    item.setChecked(true);
                    onSortChanged(SortMethod.size);
                    refreshFileList();
                    break;
                case MENU_SORT_DATE:
                    item.setChecked(true);
                    onSortChanged(SortMethod.date);
                    refreshFileList();
                    break;
                case MENU_SORT_TYPE:
                    item.setChecked(true);
                    onSortChanged(SortMethod.type);
                    refreshFileList();
                    break;

                case GlobalConsts.MENU_COPY:
                    onOperationCopy();
                    break;
                case GlobalConsts.MENU_COMPRESS:
                    onOperationCompress();
                    break;
                case GlobalConsts.MENU_DECOMPRESS:
                    FileInfo fileInfo = getSelectedFileList().get(0);
                    refreshFileList();
                    viewFile(fileInfo);
                    break;
                case GlobalConsts.MENU_PASTE:
                    onOperationPaste();
                    break;
                case GlobalConsts.MENU_MOVE:
                    onOperationMove();
                    break;
                case MENU_SEND:
                    onOperationSend();
                    break;
                case MENU_RENAME:
                    onOperationRename();  
                    break;
                case MENU_DELETE:
                    onOperationDelete();
                    break;
                case MENU_INFO:
                    onOperationInfo();
                    break;
                case MENU_HOTKNOT:
                    onOperationHotKnot();
                    break;
                case MENU_UPDATE:
                    onOperationUpdate();
                    break;
                case MENU_APPABOUT:
                    onOperationAppAbout();
                    break;
                case MENU_SETTING:
                    onOperationSettingRing();
                    break;
                default:
                    return false;
            }
            mListViewContextMenuSelectedItem = -1;
            return true;
        }

    };

    //*/ freeme.liuhaoran , 20161018 , for add ringtone
    private void onOperationSettingRing(){
        String path = mCurrentPath;

        if (mListViewContextMenuSelectedItem != -1) {
            path = mFileViewListener.getItem(mListViewContextMenuSelectedItem).filePath;
            Log.i(LOG_TAG, "path=" +path);
        }

        setVoice(path , AppConstant.RINGTONE);
        Toast.makeText(mContext,R.string.menu_setting_ring_ok , Toast.LENGTH_LONG).show();
    }

    public interface AppConstant {
        public static final int RINGTONE = 0;                   //铃声
        public static final int NOTIFICATION = 1;               //通知音
        public static final int ALARM = 2;                      //闹钟
        public static final int ALL = 3;                        //所有声音
    }

    private void setVoice(String path,int id){
        ContentValues cv = new ContentValues();
        Uri newUri = null;
//        Uri uri = MediaStore.Audio.Media.getContentUriForPath(path);
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Log.i("liuhaoran" , "path = " + path);
        Log.i("liuhaoran" , "uri = " + uri);
        // 查询音乐文件在媒体库是否存在
        Cursor cursor = mContext.getContentResolver().query(uri, null, MediaStore.MediaColumns.DATA + "=? " , new String[] { path },null);
        if (cursor.moveToFirst() && cursor.getCount() > 0){
            Log.i("liuhaoran" , "cursor = " + cursor);
            String _id = cursor.getString(0);
            switch (id) {
                case AppConstant.RINGTONE:
                    cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                    cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                    cv.put(MediaStore.Audio.Media.IS_ALARM, false);
                    cv.put(MediaStore.Audio.Media.IS_MUSIC, true);
                    break;
                case AppConstant.NOTIFICATION:
                    cv.put(MediaStore.Audio.Media.IS_RINGTONE, false);
                    cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
                    cv.put(MediaStore.Audio.Media.IS_ALARM, false);
                    cv.put(MediaStore.Audio.Media.IS_MUSIC, true);
                    break;
                case AppConstant.ALARM:
                    cv.put(MediaStore.Audio.Media.IS_RINGTONE, false);
                    cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                    cv.put(MediaStore.Audio.Media.IS_ALARM, true);
                    cv.put(MediaStore.Audio.Media.IS_MUSIC, true);
                    break;
                case AppConstant.ALL:
                    cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                    cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
                    cv.put(MediaStore.Audio.Media.IS_ALARM, true);
                    cv.put(MediaStore.Audio.Media.IS_MUSIC, true);
                    break;
                default:
                    break;
            }
            // 把需要设为铃声的歌曲更新铃声库
            mContext.getContentResolver().update(uri, cv, MediaStore.MediaColumns.DATA + "=?",new String[] { path });
            newUri = ContentUris.withAppendedId(uri, Long.valueOf(_id));
            Log.i("liuhaoran" , "newUri = " + newUri);
            // 一下为关键代码：
            switch (id) {
                case AppConstant.RINGTONE:
                    RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, newUri);
                    break;
                case AppConstant.NOTIFICATION:
                    RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_NOTIFICATION, newUri);
                    break;
                case AppConstant.ALARM:
                    RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALARM, newUri);
                    break;
                case AppConstant.ALL:
                    RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALL, newUri);
                    break;
                default:
                    break;

            }
            //播放铃声
//         Ringtone rt = RingtoneManager.getRingtone(mContext, newUri);
//         rt.play();
        }
    }
    //*/


    //*/ add by droi liuhaoran for add about
    private void onOperationAppAbout() {
        // TODO Auto-generated method stub
        Intent intent = new Intent();
        intent.setClassName(mContext, "com.freeme.filemanager.about.AboutAcitivity");
        mContext.startActivity(intent);
    }
    //*/

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    //*/add by droi mingjun for updateself on 20151221
    public void onOperationUpdate(){
   //modified by droi mingjun for updateself ui error
//    Intent intent = new Intent();
//  intent.setClass(mContext, UpdateSettingActivity.class);
//    mContext.startActivity(intent);
        checkUpdate();
    }
    private void checkUpdate() {
        UpdateMonitor.doManualUpdate(mContext);
    }
    //*/
    public void onOperationHotKnot() {
        if (mHotKnotWaitSend) {
                Log.d(LOG_TAG, "HotKnot: cancel action_hotknot");
                
                mFileViewListener.initHotKnot();
//                fileExplorerTabActivity.setHotknotCompleteListener(null);
//                fileExplorerTabActivity.hotKnotSend(null);
//                item.setIcon(R.drawable.ic_hotknot_press);
                mHotKnotWaitSend = false;
        } else {
                Log.d(LOG_TAG, "HotKnot: do action_hotknot");

//                ArrayList<FileInfo> selectedFileList = getSelectedFileList();
//                for (FileInfo f : selectedFileList) {
//                    if (!f.IsDir) {
//                        Uri contentUri = Uri.parse("file://"+f.filePath);//mCurrentPhoto.getContentUri();//Uri.parse("context:/"+f.filePath);
// Log.d(LOG_TAG, "HotKnot: do action_hotknot Uri:"+contentUri+" Activity:"+fileExplorerTabActivity);
//                    //modify by zhangmingjun 
//                        //mHotKnotWaitSend = true;
//                        fileExplorerTabActivity.setHotknotCompleteListener(this);
//                        //item.setIcon(R.drawable.ic_hotknot_press);
//                        fileExplorerTabActivity.hotKnotSend(contentUri);
//                    }else {
//                        Log.d(LOG_TAG, "HotKnot: do action_hotknot f.filePath:"+f.filePath);
//                        AlertDialog dialog = new AlertDialog.Builder(mContext).setMessage(
//                                R.string.error_info_cant_send_folder).setPositiveButton(R.string.confirm, null).create();
//                        dialog.show();
//                        clearSelection();
//                        return;
//                    }
//                }
                mFileViewListener.setHotKnot();
                clearSelection();
        }
    };

    public void onHotKnotSendComplete() {
        Log.d(LOG_TAG, "HotKnot: onHotKnotSendComplete");
        ///mHandler.obtainMessage(MSG_UPDATE_HOTKNOT_MENU);
        mHotKnotWaitSend = false;
    };

    private com.freeme.filemanager.controller.FileViewInteractionHub.Mode mCurrentMode;

    public String mCurrentPath;

    private String mRoot;

    private SelectFilesCallback mSelectFilesCallback;

    public boolean onCreateOptionsMenu(Menu menu) {
        //clearSelection();
//        showDropdownNavigation(false);
        menu.clear();
        //*/add by droi mingjun for search menu on 16-01-06
        menu.add(0, GlobalConsts.MENU_SEARCH, 1, mContext.getResources().getString(R.string.menu_item_search))
        .setIcon(R.drawable.ic_menu_searchs).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        //*/end
        addMenuItem(menu, MENU_SELECTALL, 0, R.string.operation_selectall,
                R.drawable.ic_menu_select_all);

        SubMenu sortMenu = menu.addSubMenu(1, MENU_SORT, 2,
                R.string.menu_item_sort).setIcon(R.drawable.ic_menu_sort);
        addMenuItem(sortMenu, MENU_SORT_NAME, 0, R.string.menu_item_sort_name);
        //addMenuItem(sortMenu, MENU_SORT_SIZE, 1, R.string.menu_item_sort_size);
        addMenuItem(sortMenu, MENU_SORT_DATE, 2, R.string.menu_item_sort_date);
        //addMenuItem(sortMenu, MENU_SORT_TYPE, 3, R.string.menu_item_sort_type);
        sortMenu.setGroupCheckable(0, true, true);
        sortMenu.getItem(mFileSortHelper.getIntSortMethod()).setChecked(true);

        addMenuItem(menu, GlobalConsts.MENU_NEW_FOLDER, 2,
                R.string.operation_create_folder, R.drawable.ic_menu_new_folder);
        addMenuItem(menu, GlobalConsts.MENU_SHOWHIDE, 3,
                R.string.operation_show_sysfile, R.drawable.ic_menu_show_sys);
        addMenuItem(menu, MENU_REFRESH, 4, R.string.operation_refresh,
                R.drawable.ic_menu_refresh);
//      addMenuItem(menu, GlobalConsts.MENU_SEARCH, 5, R.string.menu_item_search,
//              R.drawable.ic_menu_search);
        //*/add by droi mingjun for updateself on 20151221
        addMenuItem(menu, MENU_UPDATE, 5, R.string.software_update,
                R.drawable.ic_menu_refresh);
        //*/
        //*/ add by droi liuhaoran for app about on 20160616
        addMenuItem(menu, MENU_APPABOUT, 6, R.string.app_about,
                R.drawable.ic_menu_refresh);
        //*/
        return true;
    }

    private void addMenuItem(Menu menu, int itemId, int order, int string) {
        addMenuItem(menu, itemId, order, string, -1);
    }

    private void addMenuItem(Menu menu, int itemId, int order, int string, int iconRes) {
        if (!mFileViewListener.shouldHideMenu(itemId)) {
            MenuItem item = menu.add(0, itemId, order, string).setOnMenuItemClickListener(menuItemClick);
            if (iconRes > 0) {
                item.setIcon(iconRes);
            }
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            menu.clear();
            return false;
        }
        if(isInSelection()){
            return false;
        }
        updateMenuItems(menu);
        return true;
    }
    public void addMenuItems(Menu menu){
//        showDropdownNavigation(false);

        menu.clear();
        //*/add by droi mingjun for search menu on 16-01-06
        menu.add(0, GlobalConsts.MENU_SEARCH, 1, mContext.getResources().getString(R.string.menu_item_search))
         .setIcon(R.drawable.ic_menu_searchs).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        //*/end
        addMenuItem(menu, MENU_SELECTALL, 0, R.string.operation_selectall,
                R.drawable.ic_menu_select_all);
        SubMenu sortMenu = menu.addSubMenu(1, MENU_SORT, 2,
                R.string.menu_item_sort).setIcon(R.drawable.ic_menu_sort);
        addMenuItem(sortMenu, MENU_SORT_NAME, 0, R.string.menu_item_sort_name);
        //addMenuItem(sortMenu, MENU_SORT_SIZE, 1, R.string.menu_item_sort_size);
        addMenuItem(sortMenu, MENU_SORT_DATE, 2, R.string.menu_item_sort_date);
        //addMenuItem(sortMenu, MENU_SORT_TYPE, 3, R.string.menu_item_sort_type);
        sortMenu.setGroupCheckable(0, true, true);
        sortMenu.getItem(mFileSortHelper.getIntSortMethod()).setChecked(true);

        addMenuItem(menu, GlobalConsts.MENU_NEW_FOLDER, 2,
                R.string.operation_create_folder, R.drawable.ic_menu_new_folder);
        addMenuItem(menu, GlobalConsts.MENU_SHOWHIDE, 3,
                R.string.operation_show_sysfile, R.drawable.ic_menu_show_sys);
        addMenuItem(menu, MENU_REFRESH, 4, R.string.operation_refresh,
                R.drawable.ic_menu_refresh);
//      addMenuItem(menu, GlobalConsts.MENU_SEARCH, 5, R.string.menu_item_search,
//              R.drawable.ic_menu_search);
        //*/add by droi mingjun for updateself on 20151221
        addMenuItem(menu, MENU_UPDATE, 5, R.string.software_update,
                R.drawable.ic_menu_refresh);                        
        //*/
        //*/ add by droi liuhaoran for app about on 20160616
        addMenuItem(menu, MENU_APPABOUT, 6, R.string.app_about,
                R.drawable.ic_menu_refresh);
        //*/
    }

    private void updateMenuItems(Menu menu) {
        if(menu.size() <= 0){
            addMenuItems(menu);

        }
        if(menu.size() > 0&&menu.findItem(MENU_SELECTALL)!=null){
            menu.findItem(MENU_SELECTALL).setTitle(
                    isSelectedAll() ? R.string.operation_cancel_selectall : R.string.operation_selectall);
            menu.findItem(MENU_SELECTALL).setEnabled(mCurrentMode != Mode.Pick);
        
            MenuItem menuItem = menu.findItem(GlobalConsts.MENU_SHOWHIDE);
            if (menuItem != null) {
                menuItem.setTitle(Settings.instance().getShowDotAndHiddenFiles() ? R.string.operation_hide_sysfile
                        : R.string.operation_show_sysfile);
            }
        }
    }

    public boolean isFileSelected(String filePath) {
        return mFileOperationHelper.isFileSelected(filePath);
    }

    public void setMode(Mode m) {
        mCurrentMode = m;
    }

    public Mode getMode() {
        return mCurrentMode;
    }

    public void onListItemClick(AdapterView<?> parent, View view, int position, long id) {

        FileInfo lFileInfo = mFileViewListener.getItem(position);
//        showDropdownNavigation(false);
        if (lFileInfo == null) {
            mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);
            Log.e(LOG_TAG, "file does not exist on position:" + position);
            return;
        }
        if (isInSelection()) {
            boolean selected = lFileInfo.Selected;
            
           
            //ImageView checkBox = (ImageView) view.findViewById(R.id.file_checkbox);
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.file_checkbox);
            if (selected) {
                mCheckedFileNameList.remove(lFileInfo);
                //checkBox.setImageResource(R.drawable.btn_check_off_holo_light);
                checkBox.setChecked(false);
            } else {
                mCheckedFileNameList.add(lFileInfo);
                //checkBox.setImageResource(R.drawable.btn_check_on_holo_light);
                checkBox.setChecked(true);
            }
          //*/ freeme.liuhaoran , 20160723 , add the new function about the page of internal and sd can click 
            if(mContext instanceof  IActionModeCtr){
                ActionMode actionMode = ((IActionModeCtr) mContext).getActionMode();
                if (actionMode != null) {
                    if (mCheckedFileNameList.size() == 0) actionMode.finish();
                    else actionMode.invalidate();
                }
                Util.updateActionModeTitle(actionMode, mContext, mCheckedFileNameList.size());
            }
            //*/
            lFileInfo.Selected = !selected;
            return;
        }

        if (!lFileInfo.IsDir) {
            if (mCurrentMode == Mode.Pick) {
                mFileViewListener.onPick(lFileInfo);
            } else {
                viewFile(lFileInfo);
            }
            return;
        }
        setCurrentPath(getAbsoluteName(mCurrentPath, lFileInfo.fileName));
        
      //*/ freeme.liuhaoran , 20160723 , add the new function about the page of internal and sd can click 
        if(mContext instanceof  IActionModeCtr){
            ActionMode actionMode = ((IActionModeCtr) mContext).getActionMode();
       //*/
            if (actionMode != null) {
                actionMode.finish();
                //modified by mingjun for load file agin
                refreshFileList();
            }
        }
    }

    public void setRootPath(String path) {
        mRoot = path;
        if (1 == this.mTabIndex){
            StorageHelper.getInstance(this.mContext).setCurrentMountPoint(path);
        }
        setCurrentPath(path);
    }

    public String getRootPath() {
        return mRoot;
    }

    public String getCurrentPath() {
        return mCurrentPath;
    }

    public void setCurrentPath(String path) {
        Log.i(LOG_TAG, "mCurrentPath=" + mCurrentPath);
        mCurrentPath = path;
        mPathGallery.setPath(mFileViewListener.getDisplayPath(mCurrentPath));
        refreshFileList();
    }

    private String getAbsoluteName(String currentPath, String fielName) {
        if (currentPath.equals("/")){
            return currentPath + fielName;
        }
        return currentPath +"/" + fielName;
    }

    // check or uncheck
    public boolean onCheckItem(FileInfo f, View v) {
        /*modify by tyd sxp 20141022 for select error/
        if (isMoveState())
            return false;
        //*/
        if(mTabIndex == 1)
        {
            mFileViewListener.hideVolumesList();
        }
        
        if (inMoveState())
            return false;

        if(isSelectingFiles() && f.IsDir)
            return false;

        if (f.Selected) {
            mCheckedFileNameList.add(f);
        } else {
            mCheckedFileNameList.remove(f);
        }
        return true;
    }

    private boolean isSelectingFiles() {
        return mSelectFilesCallback != null;
    }

    public boolean isSelectedAll() {
        return mFileViewListener.getItemCount() != 0 && mCheckedFileNameList.size() == mFileViewListener.getItemCount();
    }
    
    public boolean isSelected() {
        return mCheckedFileNameList.size() != 0;
    }

    public void clearSelection() {
      //*/ freeme.liuhaoran , 20160723 , add the new function about the page of internal and sd can click 
        if(mContext instanceof  IActionModeCtr){
            ActionMode actionMode = ((IActionModeCtr) mContext).getActionMode();
      //*/
            if (actionMode == null) {
                if (mCheckedFileNameList.size() > 0) {
                    for (FileInfo f : mCheckedFileNameList) {
                        if (f == null) {
                            continue;
                        }
                        f.Selected = false;
                    }
                    mCheckedFileNameList.clear();
                    mFileViewListener.onDataChanged();
                }
            }
        }
        
    }

    public void actionModeClearSelection() {
        if (mCheckedFileNameList.size() > 0) {
            for (FileInfo f : mCheckedFileNameList) {
                if (f == null) {
                    continue;
                }
                f.Selected = false;
            }
            mCheckedFileNameList.clear();
            mFileViewListener.onDataChanged();
            //add by tyd heqianqian for change the selected state when pressback key on 20151214
            mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);
        }
    }
    

    public void exitActionMode(){
        actionModeClearSelection();
        //*/ freeme.liuhaoran , 20160723 , add the new function about the page of internal and sd can click 
         if(mContext instanceof  IActionModeCtr){
                ActionMode mode = ((IActionModeCtr) mContext).getActionMode();
                if(mode != null){
                    mode.finish();
                }
                ((IActionModeCtr) mContext).setActionMode(null);
         }
         //*/
    }
    
    private void viewFile(FileInfo lFileInfo) {
        if(!isCopy){
        try {
            IntentBuilder.viewFile(mContext, lFileInfo.filePath, FileViewInteractionHub.this);
        } catch (ActivityNotFoundException e) {
            Log.e(LOG_TAG, "fail to view file: " + e.toString());
        }}
    }
    
    public int getTabIndex() {
        return this.mTabIndex;
    }

    public boolean isRootPath() {
        return this.mRoot.equals(this.mCurrentPath);
    }

    public boolean onBackPressed() {
        if (mDropdownNavigation.getVisibility() == View.VISIBLE) {
            mDropdownNavigation.setVisibility(View.GONE);
        } else if (isInSelection()) {
            clearSelection();
        } else if (!onOperationUpLevel()) {
            return false;
        }
        if(mTabIndex == 0){
            mFileViewListener.showPathGalleryNavbar(false);
        }else{
            this.mPathGallery.setPath(this.mFileViewListener.getDisplayPath(this.mCurrentPath));
        }
        return true;
    }

    public void copyFile(ArrayList<FileInfo> files) {
        mFileOperationHelper.Copy(files);
    }

    public void moveFileFrom(ArrayList<FileInfo> files) {
        mFileOperationHelper.StartMove(files);
        showConfirmOperationBar(true);
        updateConfirmButtons();
        // refresh to hide selected files
        refreshFileList();
    }

    private static final int SHOW_NAVIGATION_TIMER = 1111;
    private static final int SHOW_NAVIGATION_WAITING_TIMER = 80;
    private Timer clickWaitTimer;
    boolean isShowNavigation = false;
    private Handler navigationhandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_NAVIGATION_TIMER:
                    isTimerShowDropdownNavigation();
                    break;
            }
            super.handleMessage(msg);
        }

    };
    
    public void isTimerShowDropdownNavigation(){
        mDropdownNavigation.setVisibility(isShowNavigation ? View.VISIBLE : View.GONE);
    }
    
    private void showDropdownNavigation(boolean show) {
        isShowNavigation = show;
        if (clickWaitTimer != null) {
            clickWaitTimer.cancel();
        }
        clickWaitTimer = new Timer();
        clickWaitTimer.schedule(new TimerTask() {
            public void run() {
                Message message = new Message();
                message.what = SHOW_NAVIGATION_TIMER;
                navigationhandler.sendMessage(message);     
            }
        }, SHOW_NAVIGATION_WAITING_TIMER);
    }

    @Override
    public void onFileChanged(int type) {
        if(type == GlobalConsts.TYPE_NOTIFY_REFRESH){
            refreshFileList();
            notifyRefreshViewInfo();
        }else if(type == GlobalConsts.TYPE_NOTIFY_SCAN){
            notifyFileChanged();
        }else if(type == GlobalConsts.TYPE_MOVE_NOTIFY_SCAN)
        {
            //added by tyd sxp for notify scan when move
            moveNotifyRefreshViewInfo();
        }
    }
    
    public void notifyRefreshViewInfo(){
        Intent intent = new Intent(GlobalConsts.BROADCAST_REFRESH, Uri.fromFile(new File(mCurrentPath)));
        Log.i(LOG_TAG, "mCurrentPath=" + mCurrentPath);
        if(mTabIndex == 0){
            intent.putExtra(GlobalConsts.BROADCAST_REFRESH_EXTRA, GlobalConsts.BROADCAST_REFRESH_TABCATEGORY);
        }else if(mTabIndex == 1){
            intent.putExtra(GlobalConsts.BROADCAST_REFRESH_EXTRA, GlobalConsts.BROADCAST_REFRESH_TABVIEW);
        }
        mContext.sendBroadcast(intent);
    }
    //added by tyd sxp for notify scan when move 
    public void moveNotifyRefreshViewInfo()
    {
        if (TextUtils.isEmpty(mCurrentPath)) {
            return;
        }
        Intent localIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        localIntent.setClassName("com.android.providers.media", "com.android.providers.media.MediaScannerReceiver");
        localIntent.setData(Uri.fromFile(new File(mCurrentPath)));
        this.mContext.sendBroadcast(localIntent);
        //modify by mingjun for notify scan
        //if(TextUtils.isEmpty(mFileOperationHelper.mCurFileNameList.get(0).filePath))
        //{
        //  return;
        //}
        //localIntent.setData(Uri.fromFile(new File(mFileOperationHelper.mCurFileNameList.get(0).filePath)));
        //this.mContext.sendBroadcast(localIntent);
        //end
    }
    
    private void notifyFileChanged() {
        if (TextUtils.isEmpty(mCurrentPath)) {
            return;
        }
        Intent localIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        localIntent.setClassName("com.android.providers.media", "com.android.providers.media.MediaScannerReceiver");
        localIntent.setData(Uri.fromFile(new File(mCurrentPath)));
        this.mContext.sendBroadcast(localIntent);
    }
    
    public void startSelectFiles(SelectFilesCallback callback) {
        mSelectFilesCallback = callback;
        showConfirmOperationBar(true);
        updateConfirmButtons();
    }
    
    public void showPathGallery(boolean paramBoolean) {
        View localView = this.mFileViewListener.getViewById(R.id.path_gallery_nav);
        if (paramBoolean){
            localView.setVisibility(View.VISIBLE);
        }else{
            localView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPathItemClickListener(String paramString) {
        String clickPath = this.mFileViewListener.getRealPath(paramString);
        if(mTabIndex == 0){
            if(ROOT_DIR.equals(clickPath)){
                exitActionMode();
                mFileViewListener.onOperation(GlobalConsts.OPERATION_UP_LEVEL);
            }
            return;
        }else if(mTabIndex == 1){
            if (this.mCurrentPath.equals(clickPath))
              return;
            exitActionMode();
            setCurrentPath(clickPath);
        //*/ freeme.liuhaoran , 20160723 , the tab can click 
        }else if (mTabIndex == 3){
            mFileViewListener.finish();
        }
        //*/
    }
    
    public boolean isLoadingShowing(){
        if(progressDialog!=null && progressDialog.isShowing()){
            return true;
        }else{
            return false;
        }
    }
    
    public void showRefreshProgress(boolean show) {
        if(show){
            mRefreshProgressBar.setVisibility(View.VISIBLE);
        }else{
            mRefreshProgressBar.setVisibility(View.GONE);
        }
    }

}
