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


import com.freeme.filemanager.R;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.FileCategoryHelper.FileCategory;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import java.util.HashMap;

public class FileIconHelper {

    private static final String LOG_TAG = "FileIconHelper";

    private static HashMap<String, Integer> fileExtToIcons = new HashMap<String, Integer>();

    private FileIconLoader mIconLoader;

    static {
        addItem(new String[] {
            "mp3"
        }, R.drawable.file_icon_mp3);
        addItem(new String[] {
            "wma"
        }, R.drawable.file_icon_wma);
        addItem(new String[] {
            "wav"
        }, R.drawable.file_icon_wav);
        addItem(new String[] {
            "mid","midi"
        }, R.drawable.file_icon_mid);
        addItem(new String[] {
            "aac"
        },R.drawable.file_icon_aac);
        addItem(new String[] {
            "ogg"
        },R.drawable.file_icon_ogg);
        addItem(new String[] {
            "amr"
        },R.drawable.file_icon_amr);
        addItem(new String[] {
            "flac"
        },R.drawable.file_icon_flac);
        addItem(new String[] {
            "ape"
        },R.drawable.file_icon_ape);
        addItem(new String[] {
                "mp4", "wmv", "mpeg", "m4v", "3gp", "3gpp", "3g2", "3gpp2", "asf", "avi", "flv", "mov"
        }, R.drawable.file_icon_video);
        addItem(new String[] {
                "jpg", "jpeg", "gif", "png", "bmp", "wbmp"
        }, R.drawable.file_icon_picture);
        addItem(new String[] {
                "txt"
        }, R.drawable.file_icon_txt);
        addItem(new String[] {
                "log", "xml", "ini", "lrc"
        }, R.drawable.file_icon_txt_ext);
        addItem(new String[] {
                "doc", "docx"
        }, R.drawable.file_icon_office_word);
        addItem(new String[] {
                "ppt", "pptx"
        }, R.drawable.file_icon_office_ppt);
        addItem(new String[] {
                "xsl", "xlsx", "xls"
        }, R.drawable.file_icon_office_xls);
        addItem(new String[] {
            "pdf"
        }, R.drawable.file_icon_pdf);
        addItem(new String[] {
            "zip"
        }, R.drawable.file_icon_zip);
        addItem(new String[] {
            "mtz"
        }, R.drawable.file_icon_theme);
        addItem(new String[] {
            "rar"
        }, R.drawable.file_icon_rar);
        addItem(new String[] {
            "apk"
        }, R.drawable.file_icon_apk);
        addItem(new String[] {
            "vcf"
        }, R.drawable.file_icon_vcf);
        addItem(new String[] {
            "wps"
        }, R.drawable.file_icon_wps);
    }

    public FileIconHelper(Context context) {
        mIconLoader = new FileIconLoader(context);
    }

    private static void addItem(String[] exts, int resId) {
        if (exts != null) {
            for (String ext : exts) {
                fileExtToIcons.put(ext.toLowerCase(), resId);
            }
        }
    }

    public static int getFileIcon(String ext) {
        Integer i = fileExtToIcons.get(ext.toLowerCase());
        if (i != null) {
            return i.intValue();
        } else {
            return R.drawable.file_icon_default;
        }

    }

    public void setIcon(FileInfo fileInfo, ImageView fileImage) {
        String filePath = fileInfo.filePath;
        long fileId = fileInfo.dbId;
        String extFromFilename = Util.getExtFromFilename(filePath);
        FileCategory fc = FileCategoryHelper.getCategoryFromPath(filePath);
        boolean set = false;
        int id = getFileIcon(extFromFilename);
        fileImage.setImageResource(id);

        mIconLoader.cancelRequest(fileImage);
        switch (fc) {
            case Apk:
                set = mIconLoader.loadIcon(fileImage, filePath, fileId, fc);
                break;
            case Picture:
            case Video:
                set = mIconLoader.loadIcon(fileImage, filePath, fileId, fc);
                if(!set){
                    fileImage.setImageResource(fc == FileCategory.Picture ? R.drawable.file_icon_picture: R.drawable.file_icon_video);
                    set = true;
                }
                break;
            default:
                set = true;
                break;
        }
    }

}
