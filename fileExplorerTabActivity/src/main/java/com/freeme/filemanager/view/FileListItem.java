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

import java.util.ArrayList;

import com.freeme.filemanager.FileExplorerTabActivity;
import com.freeme.filemanager.R;
import com.freeme.filemanager.controller.FileViewInteractionHub;
import com.freeme.filemanager.controller.FileViewInteractionHub.Mode;
import com.freeme.filemanager.controller.IActionModeCtr;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.ArchiveHelper;
import com.freeme.filemanager.util.FavoriteDatabaseHelper;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.Util;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FileListItem extends LinearLayout {
    private static ArrayList<FileInfo> mCheckedFileNameList = new ArrayList<FileInfo>();

    public FileListItem(Context context) {
        super(context);
    }

    public FileListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public final void bind(Context context, FileInfo fileInfo, FileViewInteractionHub fileViewInteractionHub, FileIconHelper fileIconHelper) {
        FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper.getInstance();
        mCheckedFileNameList = fileViewInteractionHub.getSelectedFileList();
        if(mCheckedFileNameList.size() > 0){
            for (FileInfo f : mCheckedFileNameList) {
                if(f.filePath.equals(fileInfo.filePath)){
                    fileInfo.Selected = true;
                }
            }
        }

        // if in moving mode, show selected file always
        if (fileViewInteractionHub.isMoveState()) {
            fileInfo.Selected = fileViewInteractionHub.isFileSelected(fileInfo.filePath);
        }
        //*modify by tyd shixiaopeng 2014070424 for change ui/
        //ImageView checkbox = (ImageView) findViewById(R.id.file_checkbox);
        CheckBox checkbox = (CheckBox) findViewById(R.id.file_checkbox);
        if (fileViewInteractionHub.getMode() == Mode.Pick) {
            checkbox.setVisibility(View.GONE);
        } else {
            checkbox.setVisibility(fileViewInteractionHub.canShowCheckBox(fileViewInteractionHub.mCurrentPath) ? View.VISIBLE : View.GONE);
            if(fileInfo.Selected){
                //checkbox.setImageResource(R.drawable.btn_check_on_holo_light);
                checkbox.setChecked(true);
            }else{
                //checkbox.setImageResource(R.drawable.btn_check_off_holo_light);
                checkbox.setChecked(false);
            }
            checkbox.setTag(fileInfo);
            View checkArea = findViewById(R.id.file_checkbox_area);
            checkArea.setOnClickListener(new FileItemOnClickListener(context, fileViewInteractionHub));
            Log.i(VIEW_LOG_TAG, "checkArea");
            setSelected(fileInfo.Selected);
        }
        //*end/

        Util.setText(this, R.id.file_name, fileInfo.fileName);
        TextView file_count_view = (TextView) findViewById(R.id.file_count);
        TextView modified_time_view = (TextView) findViewById(R.id.modified_time);
        if(fileViewInteractionHub.mTabIndex == 1){
            TextView file_owner_view = (TextView) findViewById(R.id.file_owner);
            file_owner_view.setVisibility(View.VISIBLE);
            Util.setText(this, R.id.file_owner, fileInfo.owner == null ? "" : " | "+fileInfo.owner);
            int childCount = fileInfo.Count;
            //modify by droi heqianqian when count=1 show item count>1 show items
            String childItem = childCount == 0 ? getResources().getString(R.string.empty_folder) : (childCount==1?context.getResources().getString(R.string.child_count,childCount):childCount+" "+context.getResources().getString(R.string.child_item_count));
            Util.setText(this, R.id.file_count, fileInfo.IsDir ? childItem : (Util.convertStorage(fileInfo.fileSize)));
        }else{
            Util.setText(this, R.id.file_size, Util.convertStorage(fileInfo.fileSize));
        }
        // */modified by tyd wulianghuan 20130522 for fix bug[tyd00479821]
        String modifyDateTime = DateUtils.formatDateRange(context, fileInfo.ModifiedDate, fileInfo.ModifiedDate,
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_YEAR
                        | DateUtils.FORMAT_NUMERIC_DATE);
        Util.setText(this, R.id.modified_time, modifyDateTime);
        ImageView lFileImage = (ImageView) findViewById(R.id.file_image);
        ImageView lFavTagImage = (ImageView) findViewById(R.id.favorite_tag);

        lFileImage.setTag(fileInfo.filePath);
        lFavTagImage.setTag(fileInfo.filePath);
        if (fileInfo.IsDir) {
            lFavTagImage.setVisibility(View.GONE);
            if (databaseHelper != null) {
                if (databaseHelper.isFavorite(fileInfo.filePath.toString())) {
                    lFileImage.setImageResource(R.drawable.folder_fav);
                } else if (getFilePath(fileInfo.filePath.toString()).equalsIgnoreCase("HotKnot")) {
                    lFileImage.setImageResource(R.drawable.folder_hotknot);
                } else {
                    lFileImage.setImageResource(R.drawable.folder);
                }
            } else {
                lFileImage.setImageResource(R.drawable.folder);
            }
        } else {
            fileIconHelper.setIcon(fileInfo, lFileImage);
            if (databaseHelper != null) {
                if (databaseHelper.isFavorite(fileInfo.filePath.toString())) {
                    if (lFavTagImage.getTag().equals(fileInfo.filePath)) {
                        lFavTagImage.setVisibility(View.VISIBLE);
                    }
                } else {
                    lFavTagImage.setVisibility(View.GONE);
                }
            } else {
                lFavTagImage.setVisibility(View.GONE);
            }
        }
    }

    public static String getFilePath(String filePath) {
        int sepIndex = filePath.lastIndexOf("/");
        if (sepIndex >= 0) {
            return filePath.substring(sepIndex + 1);
        }
        return "";
    }
    
    private class FileItemOnClickListener implements OnClickListener{
        private Context mContext;
        private FileViewInteractionHub mFileViewInteractionHub;

        public FileItemOnClickListener(Context context, FileViewInteractionHub fileViewInteractionHub) {
            mContext = context;
            mFileViewInteractionHub = fileViewInteractionHub;
        }

        @Override
        public void onClick(View v) {
            Log.i(VIEW_LOG_TAG, "checkArea");
            CheckBox img = (CheckBox) v.findViewById(R.id.file_checkbox);
            assert (img != null && img.getTag() != null);

            FileInfo tag = (FileInfo) img.getTag();
            tag.Selected = !tag.Selected;
            if (mFileViewInteractionHub.onCheckItem(tag, v)) {
                //modify by tyd shixiaopeng 2014070424 for change ui
                //img.setImageResource(tag.Selected ? R.drawable.btn_check_on_holo_light : R.drawable.btn_check_off_holo_light); 
                img.setChecked(tag.Selected);
            } else {
                tag.Selected = !tag.Selected;
            }
             //*/ freeme.liuhaoran , 20160723 , add the new function about the page of internal and sd can click 
            if(mContext instanceof  IActionModeCtr){
                ActionMode actionMode = ((IActionModeCtr) mContext).getActionMode();
                if (actionMode == null) {
                    actionMode = ((IActionModeCtr) mContext).startActionMode(new ModeCallback(mContext, mFileViewInteractionHub));
                    ((IActionModeCtr) mContext).setActionMode(actionMode);
                } else {
                    actionMode.invalidate();
                } 
                Util.updateActionModeTitle(actionMode, mContext, mFileViewInteractionHub
                        .getSelectedFileList().size());
            }
            //*
        }
    }

    public static class ModeCallback implements ActionMode.Callback {
        private Menu mMenu;
        private Context mContext;
        private FileViewInteractionHub mFileViewInteractionHub;

        private void initMenuItemSelectAllOrCancel() {
            boolean isSelectedAll = mFileViewInteractionHub.isSelectedAll();
            mMenu.findItem(R.id.action_cancel).setVisible(isSelectedAll);
            mMenu.findItem(R.id.action_select_all).setVisible(!isSelectedAll);
        }
        
        /*/ freeme.liuhaoran , 20160723 , 
        private void scrollToSDcardTab() {
            ActionBar bar = ((FileExplorerTabActivity) mContext).getActionBar();
            if (((FileExplorerTabActivity) mContext).mViewPager.getCurrentItem() != Util.SDCARD_TAB_INDEX) {
//                bar.setSelectedNavigationItem(Util.SDCARD_TAB_INDEX);
                ((FileExplorerTabActivity) mContext).mViewPager.setCurrentItem(Util.SDCARD_TAB_INDEX);
            }
        }
        //*/
        
        public ModeCallback(Context context,
                FileViewInteractionHub fileViewInteractionHub) {
            mContext = context;
            mFileViewInteractionHub = fileViewInteractionHub;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = ((Activity) mContext).getMenuInflater();
            mMenu = menu;
            inflater.inflate(R.menu.operation_menu, mMenu);
            /*/ freeme.liuhaoran , 20160802 , popumenu repeat create result move
            initMenuItemSelectAllOrCancel();
            //*/
            //add by TYD mingjun for menu error
            onPrepareActionMode(mode, menu);
            //end
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if(mFileViewInteractionHub.mTabIndex == 0){
                mMenu.findItem(R.id.action_compress).setVisible(false);
                //add by tyd shixiaopeng 20140425 for FreeMe 3.0
                mMenu.findItem(R.id.action_move).setVisible(false);
                mMenu.findItem(R.id.action_copy).setVisible(false);
            }else if(mFileViewInteractionHub.mTabIndex == 1){
                ArrayList<FileInfo> checkedFileNameList = mFileViewInteractionHub.getSelectedFileList();
                if(checkedFileNameList.size()>0){
                    int zipFileCount = 0;
                    for (FileInfo fileInfo : checkedFileNameList) {
                        if (ArchiveHelper.checkIfArchive(fileInfo.filePath)) {
                            zipFileCount = zipFileCount +1;
                        }
                    }
                    if(zipFileCount == checkedFileNameList.size()){
                        mMenu.findItem(R.id.action_compress).setVisible(false);
                    }else{
                        mMenu.findItem(R.id.action_compress).setVisible(true);
                    }
                }else{
                    mMenu.findItem(R.id.action_compress).setVisible(false);
                }
            }
            /*/ modify by droi liuhaoran for popupMenu location moving on 20160512
            mMenu.findItem(R.id.action_cancel).setVisible(mFileViewInteractionHub.isSelected());
            mMenu.findItem(R.id.action_select_all).setVisible(!mFileViewInteractionHub.isSelectedAll());
            //*/
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    //modify by tyd liuyong 20140514 for action_delete NullPointerException
                    if(mFileViewInteractionHub.mDeleteFlag){
                    mFileViewInteractionHub.onOperationDelete();
                    /*/  freeme.liuhaoran , 20160718 ,  selected not clear after click menu 
                    mode.finish();
                    //*/
                    }
                    break;
                case R.id.action_copy:
                    mMenu.findItem(R.id.action_compress).setVisible(false);
                    //add by tyd shixiaopeng 20140425 for FreeMe 3.0
                    mMenu.findItem(R.id.action_move).setVisible(false);
                    mMenu.findItem(R.id.action_copy).setVisible(false);
                    mode.getMenu().clear();
                    mode.getMenu().close();
                    item.setVisible(false);
                     ((IActionModeCtr) mContext).setActionMode(null);
                      mFileViewInteractionHub.onOperationCopy();
                   // ((FileViewFragment) ((FileExplorerTabActivity) mContext).getFragment(Util.SDCARD_TAB_INDEX))
                    //        .copyFile(mFileViewInteractionHub.getSelectedFileList());
                    mode.finish();
                    //scrollToSDcardTab();
                    break;
                case R.id.action_move:
                    mFileViewInteractionHub.onOperationMove();
                    //((FileViewFragment) ((FileExplorerTabActivity) mContext).getFragment(Util.SDCARD_TAB_INDEX))
                    //        .moveToFile(mFileViewInteractionHub.getSelectedFileList());
                    mode.finish();
                   // scrollToSDcardTab();
                    break;
                case R.id.action_send:
                    mFileViewInteractionHub.onOperationSend();
                    //*/  freeme.liuhaoran , 20160718 ,  selected not clear after click menu
                    mode.finish();
                    //*/
                    break;
                case R.id.action_compress:
                    mFileViewInteractionHub.onOperationCompress();
                    mode.finish();
                    break;
                case R.id.action_cancel:
                    mFileViewInteractionHub.actionModeClearSelection();
                    /*/ freeme.liuhaoran , 20160802 , popumenu repeat create result move
                    initMenuItemSelectAllOrCancel();
                    //*/
                    mode.finish();
                    break;
                case R.id.action_select_all:
                    mFileViewInteractionHub.onOperationSelectAllOrCancel();
                    /*/ freeme.liuhaoran , 20160802 , popumenu repeat create result move
                    initMenuItemSelectAllOrCancel();
                    //*/
                    break;
            }
            Util.updateActionModeTitle(mode, mContext, mFileViewInteractionHub
                    .getSelectedFileList().size());
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mFileViewInteractionHub.actionModeClearSelection();
             //*/ freeme.liuhaoran , 20160723 , add the new function about the page of internal and sd can click 
            if(mContext instanceof IActionModeCtr){
                ((IActionModeCtr) mContext).setActionMode(null);
            }
           //*/
        }
    }
}
