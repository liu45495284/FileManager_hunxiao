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

import android.content.Context;
import android.content.Intent;
import android.view.View;

import java.util.Collection;

import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.FileSortHelper;

public interface IFileInteractionListener {

    public View getViewById(int id);

    public Context getContext();

    public void startActivity(Intent intent);

    public void onDataChanged();

    public void onPick(FileInfo f);

    public boolean shouldShowOperationPane();

    public boolean onOperation(int id);

    public String getDisplayPath(String path);

    public String getRealPath(String displayPath);

    public void runOnUiThread(Runnable r);
    
    public boolean shouldHideMenu(int menu);
    
    public void showPathGalleryNavbar(boolean show);

    public FileIconHelper getFileIconHelper();

    public FileInfo getItem(int pos);

    public void sortCurrentList(FileSortHelper sort);

    public Collection<FileInfo> getAllFiles();

    public void addSingleFile(FileInfo file);

    public boolean onRefreshFileList(String path, FileSortHelper sort);
    
    public void onRefreshMenu(boolean visible);
    
    public int getItemCount();
    
    public void hideVolumesList();
    
    public void finish();
    
    public void initHotKnot();
    
    public void setHotKnot();
    
}
