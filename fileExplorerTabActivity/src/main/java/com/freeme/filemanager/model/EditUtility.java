/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.freeme.filemanager.model;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.drm.DrmManagerClient;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.media.MediaFile;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.freeme.filemanager.R;
import com.freeme.filemanager.controller.FileInfoComparator;
import com.freeme.filemanager.controller.FileManagerOperationActivity;
import com.freeme.filemanager.controller.FileManagerOperationActivity.FileTask;
import com.freeme.filemanager.controller.FileManagerOperationActivity.ScannerClient;
import com.freeme.filemanager.util.MountPointHelper;
import com.freeme.filemanager.util.OptionsUtil;
import com.freeme.filemanager.view.FileManagerBaseActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import android.content.ContentUris;
import android.content.ContentValues;


public final class EditUtility {
    private static final String TAG = "EditUtility";
    private static final String UNIT_KB = "KB";
    private static final String UNIT_MB = "MB";
    private static final String UNIT_GB = "GB";
    private static final String UNIT_TB = "TB";

    // operation constant for navigation view
    public static final int CREATE_FOLDER = 0;
    public static final int EDIT = 1;
    public static final int SORTY_BY = 2;

    // operation constant for edit view
    public static final int COPY = 0;
    public static final int DELETE = 1;
    public static final int CUT = 2;
    public static final int SHARE = 3;
    public static final int PASTE = 4;
    public static final int RENAME = 5;
    public static final int DETAILS = 6;
    public static final int PROTECTION_INFO = 7;
    public static final int NO_OPERATION = 8;

    public static final String EXT_DRM_CONTENT = "dcf";
    public static final String UNRECOGNIZED_FILE_MIME_TYPE = "application/zip";
    public static final String ACTION_DELETE = "com.mediatek.filemanager.ACTION_DELETE";
    public static final int FILENAME_MAX_LENGTH = 255;
    private static final int IO_BUFFER_LENGTH = 256 * 1024;
    private static final int OPERATION_COMPLETE_PERCENTAGE = 100;
    private static final int COPY_COMPLETE_PERCENTAGE = 90;
    private static final int DELETE_COMPLETE_PERCENTAGE = 10;

    private static int sLastOperation = NO_OPERATION;
    private static boolean sShowProgressDialog = false;

    private static byte[] mIOBuffer = null;

    private static final EditUtility INSTANCE = new EditUtility();

    /**
     * private constructor here, It is a singleton class.
     */
    private EditUtility() {
    }

    /**
     * The EditUtility is a singleton class, this static method can be used to obtain the unique
     * instance of the class.
     * @return The unique instance of EditUtility.
     */
    public static EditUtility getInstance() {
        return INSTANCE;
    }

    /**
     * This method creates a new folder
     * @param name the name of the folder to be created
     * @return true if the folder is created successfully, false otherwise
     */
    public static boolean createFolder(Context context, String toCreateDirPath) {
        boolean result = false;
        String dirPath = toCreateDirPath;

        FileManagerLog.d(TAG, "Create a new folder");
        try {
            dirPath = dirPath.trim();
            File dir = new File(dirPath);

            FileManagerLog.d(TAG, "The folder to be created exist: " + dir.exists());
            if (!dir.exists()) {
                result = dir.mkdirs();
                if (!result) {
                    showToast(context, R.string.msg_create_fail);
                }
            } else {
                showToast(context, R.string.msg_change_name);
            }
        } catch (Exception e) {
            FileManagerLog.e(TAG, "Failed to create a folder", e);
            showToast(context, R.string.msg_create_fail);
        }
        return result;
    }

    /**
     * This method is used to judge whether a fileName is legal. It is designed just to judge the
     * filename length, that can't be zero and can't too long either.
     * @param fileName Filename string to be judged.
     * @return If the length is in the range of 0 ~ DIALOG_FILENAME_MAX_SIZE, return true, otherwise
     *         return false.
     */
    public static boolean isValidName(Context context, String fileName) {
        String msg = null;

        if (fileName.trim().length() == 0) {
            showToast(context, R.string.invalid_empty_name);
            return false;
        } else {
            if (fileName.getBytes().length > FILENAME_MAX_LENGTH) {
                // show a toast notification if the folder name is too long
                showToast(context, R.string.invalid_file_name);
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * This method copies a list of files to the clipboard.
     * @param context the context of FileManagerOperationActivity
     * @param copyFiles a list of files to be copied
     */
    public static void copy(Context context, final List<FileInfo> copyFileInfos) {
        Clipboard.setContents(null);
        ArrayList<File> files = new ArrayList<File>();
        for (FileInfo fileInfo : copyFileInfos) {
            files.add(fileInfo.getFile());
        }
        Clipboard.setContents(files);
    }

    public static boolean isDrmFile(File file) {
        if (OptionsUtil.isDrmSupported()) {
            String extension = getFileExtension(file.getName());
            if (extension != null && extension.equalsIgnoreCase(EXT_DRM_CONTENT)) {
                return true; // all drm files cannot be copied
            }
        }
        return false;
    }

    /**
     * This method performs the paste operation
     * @param context the context of FileManagerOperationActivity
     * @param targetDir the target directory that files are pasted to
     * @param callBack the FileTask object
     * @return true if paste succeeds, false otherwise
     */
    public static boolean paste(Context context, String targetDir, FileTask callBack) {
        FileManagerLog.d(TAG, "Paste, thread id: " + Thread.currentThread().getId());
        sShowProgressDialog = false;

        if (Clipboard.getContents() == null) {
            return false;
        } else {
            List<File> pastedFiles = Clipboard.getContents();
            File target = new File(targetDir);

            if (sLastOperation == CUT) {
                FileManagerLog.d(TAG, "Paste previous cut files, thread id: "
                        + Thread.currentThread().getId());
                String parent = pastedFiles.get(0).getParent();
                if (!(parent != null && parent.equalsIgnoreCase(targetDir))) {
                    // if cut and paste operations are performed in the same folder, do nothing.
                    return commitCut(context, target, pastedFiles, callBack);
                }
            } else {
                FileManagerLog.d(TAG, "Paste previous copied files, thread id: "
                        + Thread.currentThread().getId());
                Map<File, Long> pastedItemsSizeArray = new HashMap<File, Long>();
                if (pastedItemsSizeArray == null) {
                    FileManagerLog.e(TAG, "Create HashMap failed in paste().");
                    return false;
                }
                boolean canPaste = isSufficientMemory(callBack, target, pastedFiles,
                        pastedItemsSizeArray);

                if (canPaste) {
                    /*
                     * pasteCounter[0]: increment number of selected items already copied
                     * pasteCounter[1]: total number of the selected items
                     */
                    int[] pasteCounter = new int[2];
                    pasteCounter[0] = 0;
                    pasteCounter[1] = pastedFiles.size();
                    ArrayList<File> sourceArrayList = new ArrayList<File>();
                    ArrayList<File> targetArrayList = new ArrayList<File>();

                    for (File source : pastedFiles) {
                        sourceArrayList.clear();
                        sourceArrayList.add(source);

                        targetArrayList.clear();
                        targetArrayList.add(target);

                        /*
                         * contentSize[0]: increment size of files already copiedcontentSize[1]:
                         * total size of files to be copied
                         */
                        long[] contentSize = new long[2];
                        contentSize[0] = 0;
                        contentSize[1] = pastedItemsSizeArray.get(source);

                        FileManagerLog.d(TAG, "Paste total file size: " + contentSize[1]
                                + "thread id: " + Thread.currentThread().getId());

                        if (callBack.isCancelled()) {
                            FileManagerLog.d(TAG, "Paste cancelled; break for loop, thread id: "
                                    + Thread.currentThread().getId());
                            break;
                        }

                        ++(pasteCounter[0]);

                        try {
                            commitCopy(context, sourceArrayList, targetArrayList, pasteCounter,
                                    contentSize, source.getName(), callBack);
                        } catch (IOException e) {
                            FileManagerLog.e(TAG, "Fn-paste(): " + e.toString() + "thread id: "
                                    + Thread.currentThread().getId());
                        }
                    }

                    // release pastedItemsSizeArray
                    if (pastedItemsSizeArray != null) {
                        pastedItemsSizeArray.clear();
                        pastedItemsSizeArray = null;
                    }

                    // release sourceArrayList
                    if (sourceArrayList != null) {
                        sourceArrayList.clear();
                        sourceArrayList = null;
                    }

                    // release targetArrayList
                    if (targetArrayList != null) {
                        targetArrayList.clear();
                        targetArrayList = null;
                    }

                    if (callBack.isCancelled()) {
                        return false;
                    }
                } else {
                    // notify UI thread about the insufficient memory
                    callBack.onUpateProgessBar(0, null, null,
                            FileManagerOperationActivity.INSUFFICIENT_MEMORY);
                    return false;
                }
            }
            return true;
        }
    }

    private static boolean commitCopy(Context context, ArrayList<File> sourceArrayList,
            ArrayList<File> targetArrayList, int[] pasteCounter, long[] contentSize,
            String pasteTag, FileTask callBack) throws IOException {
        boolean success = false;

        while (!sourceArrayList.isEmpty()) { // The copy operation will be performed iteratively.
            File source = sourceArrayList.get(0);
            File targetDir = targetArrayList.get(0);
            FileManagerLog.d(TAG, "Commit source file: " + source.getPath() + "; dir: "
                    + source.isDirectory() + "thread id: " + Thread.currentThread().getId());

            if (source.isDirectory()) {
                if ((!targetDir.isDirectory()) || targetDir.getPath().equals(source.getPath())
                        || targetDir.getPath().startsWith(source.getPath() + "/")) {
                    FileManagerLog.w(TAG, "Failed to paste: "
                            + "source directory is the same as target directory" + " thread id: "
                            + Thread.currentThread().getId());

                    callBack.onUpateProgessBar((int) (contentSize[0]
                            * OPERATION_COMPLETE_PERCENTAGE / contentSize[1]), pasteTag, Integer
                            .toString(pasteCounter[0])
                            + "/" + Integer.toString(pasteCounter[1]),
                            FileManagerOperationActivity.PASTE_SAME_FOLDER);

                } else {
                    if (!sShowProgressDialog) {
                        // notify UI thread to show progress dialog and initialize it
                        callBack.onUpateProgessBar(0, null, null,
                                FileManagerOperationActivity.SHOW_PROGRESS_DIALOG);
                        sShowProgressDialog = true;
                        callBack.onUpateProgessBar(0, pasteTag, Integer.toString(pasteCounter[0])
                                + "/" + Integer.toString(pasteCounter[1]),
                                FileManagerOperationActivity.OPERATION_SUCCESS);
                    }

                    File target = null;
                    if (!source.canWrite() || !source.canRead()) {
                        success = false;
                    } else {
                        target = new File(targetDir, source.getName());
                        if (target.exists()) {
                            target = new File(autoGenerateName(target));
                        }

                        if (target != null
                                && target.getName().getBytes().length <= FILENAME_MAX_LENGTH) {
                            success = target.mkdir();
                            FileManagerLog.d(TAG, "Create target: " + target.getPath()
                                    + "; result: " + success + "thread id: "
                                    + Thread.currentThread().getId());
                        } else {
                            success = false;
                            FileManagerLog.w(TAG, "Create target File object failed: "
                                    + target.getPath() + "thread id: "
                                    + Thread.currentThread().getId());
                        }
                    }

                    if (success) {
                        contentSize[0] += source.length();
                        callBack.onUpateProgessBar((int) (contentSize[0]
                                * OPERATION_COMPLETE_PERCENTAGE / contentSize[1]), pasteTag,
                                Integer.toString(pasteCounter[0]) + "/"
                                        + Integer.toString(pasteCounter[1]),
                                FileManagerOperationActivity.OPERATION_SUCCESS);
                        String[] children = source.list();
                        if (children != null) {
                            for (int i = 0; i < children.length; i++) {
                                if (callBack.isCancelled()) {
                                    FileManagerLog.d(TAG, "commit copy file cancelled;"
                                            + " break for loop" + " thread id: "
                                            + Thread.currentThread().getId());
                                    break; // break for loop if cancel is true
                                }
                                sourceArrayList.add(new File(source, children[i]));
                                targetArrayList.add(target);
                            }
                        }

                        if (callBack.isCancelled()) {
                            return false;
                        }
                    } else {
                        callBack.onUpateProgessBar((int) (contentSize[0]
                                * OPERATION_COMPLETE_PERCENTAGE / contentSize[1]), pasteTag,
                                Integer.toString(pasteCounter[0]) + "/"
                                        + Integer.toString(pasteCounter[1]),
                                FileManagerOperationActivity.PASTE_FAIL);
                    }
                }
            } else {
                if (!sShowProgressDialog) {
                    // notify UI thread to show progress dialog
                    callBack.onUpateProgessBar(0, null, null,
                            FileManagerOperationActivity.SHOW_PROGRESS_DIALOG);
                    sShowProgressDialog = true;
                    callBack.onUpateProgessBar(0, pasteTag, Integer.toString(pasteCounter[0]) + "/"
                            + Integer.toString(pasteCounter[1]),
                            FileManagerOperationActivity.OPERATION_SUCCESS);
                }

                InputStream in = null;
                OutputStream out = null;
                File target = null;

                try {
                    if (!source.canWrite() || !source.canRead()) {
                        success = false;
                    } else {
                        target = new File(targetDir, source.getName());
                        FileManagerLog.d(TAG, "Copy to target directory: " + targetDir
                                + "thread id: " + Thread.currentThread().getId());
                        FileManagerLog.d(TAG, "Copy from source name: " + source.getName()
                                + "thread id: " + Thread.currentThread().getId());

                        if (target.exists()) {
                            target = new File(autoGenerateName(target));
                        }
                        if (!isDrmFile(source) && target != null
                                && target.getName().getBytes().length <= FILENAME_MAX_LENGTH) {
                            success = target.createNewFile();
                        } else {
                            success = false;
                        }
                    }

                    if (success) {
                        try {
                            in = new FileInputStream(source);
                            FileManagerLog.d(TAG, "FileInputStream created " + "thread id: "
                                    + Thread.currentThread().getId());

                            try {
                                out = new FileOutputStream(target);
                                FileManagerLog.d(TAG, "FileOutputStream created " + "thread id: "
                                        + Thread.currentThread().getId());

                                // Copy data from in stream to out stream
                                if (null == mIOBuffer) {
                                    mIOBuffer = new byte[IO_BUFFER_LENGTH];
                                }
                                int len;

                                while ((len = in.read(mIOBuffer)) > 0) {
                                    if (callBack.isCancelled()) {
                                        FileManagerLog.d(TAG, "commit copy file cancelled; "
                                                + "break while loop " + "thread id: "
                                                + Thread.currentThread().getId());
                                        break; // break for loop if cancel is true
                                    }
                                    contentSize[0] += len;
                                    out.write(mIOBuffer, 0, len);

                                    // update progress bar on UI thread
                                    callBack.onUpateProgessBar((int) (contentSize[0]
                                            * OPERATION_COMPLETE_PERCENTAGE / contentSize[1]),
                                            pasteTag, Integer.toString(pasteCounter[0]) + "/"
                                                    + Integer.toString(pasteCounter[1]),
                                            FileManagerOperationActivity.OPERATION_SUCCESS);
                                }
                                ScannerClient.getInstance().scanPath(target.getAbsolutePath());
                                FileManagerLog.v(TAG, "scan file: " + target.getAbsolutePath());
                            } finally {
                                if (out != null) {
                                    out.close();
                                }
                            }
                        } finally {
                            if (in != null) {
                                in.close();
                            }
                        }

                        if (callBack.isCancelled()) {
                            boolean result = target.delete();
                            FileManagerLog.d(TAG, "Callback is cancelled "
                                    + "and a file/folder is deleted: " + result + " thread id: "
                                    + Thread.currentThread().getId());
                            return false;
                        }
                    } else {
                        callBack.onUpateProgessBar(0, pasteTag, Integer.toString(pasteCounter[0])
                                + "/" + Integer.toString(pasteCounter[1]),
                                FileManagerOperationActivity.PASTE_FAIL);
                    }
                } catch (FileNotFoundException e) {
                    FileManagerLog.e(TAG, "Fn-commitCopy(): " + e.toString() + "thread id: "
                            + Thread.currentThread().getId());
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    FileManagerLog.e(TAG, "Fn-commitCopy(): " + e.toString() + "thread id: "
                            + Thread.currentThread().getId());
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                }
            }
            sourceArrayList.remove(0);
            targetArrayList.remove(0);
        }
        return true;
    }

    /**
     * This method generates a new suffix if a name conflict occurs, ex: paste a file named
     * "stars.txt", the target file name would be "stars(1).txt"
     * @param conflictFile the conflict file
     * @return a new name for the conflict file
     */
    public static String autoGenerateName(File conflictFile) {
        int prevMax = 0;
        int newMax = 0;
        int leftBracketIndex = 0;
        int rightBracketIndex = 0;
        String tmp = null;
        String numeric = null;
        String fileName = null;
        File dir = null;
        File[] files = null;
        String parentDir = conflictFile.getParent();
        String conflictName = conflictFile.getName();

        if (parentDir != null) {
            dir = new File(parentDir);
            files = dir.listFiles();
        }

        if (conflictFile.isDirectory()) {
            // check if source folder already contains "(x)", e.g. /sdcard/starsDir(3)
            if (conflictName.endsWith(")")) {
                leftBracketIndex = conflictName.lastIndexOf("(");
                if (leftBracketIndex != -1) {
                    numeric = conflictName.substring(leftBracketIndex + 1,
                            conflictName.length() - 1);
                    if (numeric.matches("[0-9]+")) {
                        FileManagerLog.d(TAG, "Conflict folder name already contains (): "
                                + conflictName + "thread id: " + Thread.currentThread().getId());
                        newMax = findSuffixNumber(conflictName, prevMax);
                        prevMax = newMax;
                        conflictName = conflictName.substring(0, leftBracketIndex);
                    }
                }
            }

            if (files != null) {
                for (File file : files) {
                    fileName = file.getName();
                    if (fileName.endsWith(")")) {
                        leftBracketIndex = fileName.lastIndexOf("(");
                        if (leftBracketIndex != -1) {
                            tmp = fileName.substring(0, leftBracketIndex);
                            if (tmp.equalsIgnoreCase(conflictName)) {
                                numeric = fileName.substring(leftBracketIndex + 1, fileName
                                        .length() - 1);
                                if (numeric.matches("[0-9]+")) {
                                    FileManagerLog.d(TAG, "File name contains () match: "
                                            + fileName + "thread id: "
                                            + Thread.currentThread().getId());
                                    newMax = findSuffixNumber(fileName, prevMax);
                                    prevMax = newMax;
                                }
                            }
                        }
                    }
                }
            }
            return parentDir + "/" + conflictName + "(" + Integer.toString(newMax + 1) + ")";
        } else {
            // check if source file already contains "(x)", e.g. /sdcard/stars(3).jpg
            String ext = "";
            int extIndex = conflictName.lastIndexOf(".");
            if (extIndex == -1) {
                extIndex = conflictName.length(); // this file has no extension
            } else {
                ext = conflictName.substring(extIndex);
            }

            String prefix = conflictName.substring(0, extIndex);
            if (prefix.endsWith(")")) {
                leftBracketIndex = prefix.lastIndexOf("(");
                if (leftBracketIndex != -1) {
                    numeric = prefix.substring(leftBracketIndex + 1, prefix.length() - 1);
                    if (numeric.matches("[0-9]+")) {
                        FileManagerLog.d(TAG, "Conflict file name already contains (): "
                                + conflictName + "thread id: " + Thread.currentThread().getId());
                        newMax = findSuffixNumber(conflictName, prevMax);
                        prevMax = newMax;
                        prefix = prefix.substring(0, leftBracketIndex);
                    } 
                } 
            }

            if (files != null) {
                for (File file : files) {
                    fileName = file.getName();
                    if (fileName.endsWith(")" + ext)) {
                        leftBracketIndex = fileName.lastIndexOf("(");
                        rightBracketIndex = fileName.lastIndexOf(")");
                        if (leftBracketIndex != -1) {
                            tmp = fileName.substring(0, leftBracketIndex);
                            if (tmp.equalsIgnoreCase(prefix)) {
                                numeric = fileName.substring(leftBracketIndex + 1, 
                                    rightBracketIndex);
                                if (numeric.matches("[0-9]+")) {
                                    FileManagerLog.d(TAG, "file name contains () match: "
                                            + fileName + "thread id: "
                                            + Thread.currentThread().getId());
                                    newMax = findSuffixNumber(fileName, prevMax);
                                    prevMax = newMax;
                                }
                            }
                        }
                    }
                }
            }
            return parentDir + "/" + prefix + "(" + Integer.toString(newMax + 1) + ")" + ext;
        }
    }

    /**
     * This method finds the current max number of suffix for a conflict file ex: there are
     * A(1).txt, A(2).txt, then the max number of suffix is 2
     * @param fileName the conflict file
     * @param maxVal the old max number of suffix
     * @return the new max number of suffix
     */
    private static int findSuffixNumber(String fileName, int maxVal) {
        int val = 0;
        int leftBracket = fileName.lastIndexOf("(");
        int rightBracket = fileName.lastIndexOf(")");

        String s = fileName.substring(leftBracket + 1, rightBracket);

        try {
            val = Integer.parseInt(s);
            if (val > maxVal) {
                return val;
            }
        } catch (NumberFormatException e) {
            FileManagerLog.e(TAG, "Fn-findSuffixNumber(): " + e.toString());
        }
        return maxVal;
    }

    /**
     * This method counts the number of files in a directory (recursively to the deepest level)
     * @param root the directory
     * @param countArr the array to store results countArr[0]: total number of folders countArr[1]:
     *            total number of files
     */
    private static void countDirFiles(File root, int[] countArr) {
        if (root.isDirectory()) {
            ++(countArr[0]);
            File[] files = root.listFiles();
            if (files != null) {
                for (File file : files) {
                    countDirFiles(file, countArr);
                }
            }
        } else {
            ++(countArr[1]);
        }
    }

    private static int countDirFiles(File root) {
        int number = 0;
        if (root.isDirectory()) {
            File[] files = root.listFiles();
            number += files.length;
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) number += countDirFiles(file);
                }
            }
        }
        return number;
    }

    /**
     * This method gets the total size (bytes) of a file/folder (recursively to the deepest level)
     * @param root the file/folder
     * @return the size
     */
    public static long getContentSize(File root) {
        // Note that this function is written in iteration instead of recursion.
        // This is because recursion may cause stack overflow if folders are very deep.
        long size = root.length();

        if (root.isDirectory()) {
            ArrayList<File> folderArrayList = new ArrayList<File>();
            folderArrayList.add(root);
            while (!folderArrayList.isEmpty()) {
                File folder = folderArrayList.get(0);
                File[] files = folder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            folderArrayList.add(file);
                        }
                        size += file.length();
                    }
                }
                folderArrayList.remove(0);
            }
            folderArrayList = null; // release reference
        }

        return size;
    }

    /**
     * This method updates the total size (bytes) of a file/folder to details dialog
     * @param root the file/folder
     * @param callBack the FileTask object
     * @return the size
     */
    public static long updateContentSize(FileInfo fileInfo, FileTask callBack) {
        File root = fileInfo.getFile();
        long size = root.length();
        callBack.onUpateProgessBar(0, null, sizeToString(size),
                FileManagerOperationActivity.OPERATION_SUCCESS);
        if (root.isDirectory()) {
            File[] files = root.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (callBack.isCancelled()) {
                        FileManagerLog.d(TAG, "Get details AsyncTask is Cancelled(ID): "
                                + Thread.currentThread().getId());
                        return size;
                    }
                    size += getContentSize(file);
                    callBack.onUpateProgessBar(0, null, sizeToString(size),
                            FileManagerOperationActivity.OPERATION_SUCCESS);
                }
            }
        }
        return size;
    }

    /**
     * This method checks whether there is sufficient memory
     * @param callBack the FileTask object
     * @param targetDir the target directory that the files/folders are pasted to
     * @return true if there is sufficient memory, false otherwise
     */
    public static boolean isSufficientMemory(FileTask callBack, File targetDir,
            List<File> pastedFiles, Map<File, Long> pastedItemsSizeArray) {
        long totalFileSize = 0;

        if (Clipboard.getContents() != null) {
            List<File> pasteFiles = pastedFiles;

            long freeSpace = getFreeSpace(targetDir.getAbsolutePath());
            if (freeSpace == 0) {
                return false;
            }

            callBack.onUpateProgessBar(0, null, null,
                    FileManagerOperationActivity.SHOW_WAITING_DIALOG);

            for (File file : pasteFiles) {
                long currentItemContentSize = getContentSize(file);
                pastedItemsSizeArray.put(file, Long.valueOf(currentItemContentSize));
                totalFileSize = totalFileSize + currentItemContentSize;

                // insufficient memory
                if (freeSpace < totalFileSize) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This method gets total free space in the sdcard
     * @param path the path of target directory that to be got free space
     * @return total free space
     */
    public static long getFreeSpace(String path) {
        long freeSpace = 0;
        try {
            // if lost sdcard, it may caused IllegalArgumentException to create a StatFs Object.
            StatFs stat = new StatFs(path);
            freeSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {}
        return freeSpace;
    }

    /**
     * This method performs the cut operation, i.e. save a list of files to the clipboard. filter
     * the file that can not be cut
     * @param context the context of FileManagerOperationActivity
     * @param cutFiles a list of files to be cut
     * @param grayOutItem the gray out item list should be updated when the cut files list is put in
     *            clipboard
     */
    public static void cut(Context context, List<FileInfo> cutFileInfos) {
        Clipboard.setContents(null);
        ArrayList<File> cutFiles = new ArrayList<File>();
        for (FileInfo fileInfo : cutFileInfos) {
            File file = fileInfo.getFile();
            fileInfo.setCut(true);
            cutFiles.add(file);
        }
        Clipboard.setContents(cutFiles);
    }

    public static int getDeviceNumber(Context context, String filePath) {
        StorageManager storageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        String[] storagePathList = storageManager.getVolumePaths();
        if (null != storagePathList) {
            for (int i = 0; i < storagePathList.length; i++) {
                if ((filePath + "/").startsWith(storagePathList[i] + "/")
                        || (filePath + "/").equals(storagePathList[i] + "/")) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * This method commits the cut operation (when paste icon is pressed)
     * @param context the context of FileManagerOperationActivity
     * @param targetDir the target directory that files/folders are pasted to
     * @param cutFiles the files to be cut
     * @param callBack the FileTask object
     * @return true if cut operation succeeds, false otherwise
     */
    private static boolean commitCut(Context context, File targetDir, List<File> cutFiles,
            FileTask callBack) {
        boolean success = false;
        boolean sameDevice = true;
        FileManagerBaseActivity activity = (FileManagerBaseActivity) context;

        int deviceID1 = -1;
        int deviceID2 = -1;

        deviceID1 = getDeviceNumber(context, cutFiles.get(0).getAbsolutePath());
        deviceID2 = getDeviceNumber(context, targetDir.getAbsolutePath());

        if (deviceID1 == -1 || deviceID2 == -1) {
            FileManagerLog.e(TAG, "commitCut: Illeagal argument");
            FileManagerLog.w(TAG, "source path: " + cutFiles.get(0).getAbsolutePath());
            FileManagerLog.w(TAG, "source device number: " + deviceID1);
            FileManagerLog.w(TAG, "target path: " + targetDir.getAbsolutePath());
            FileManagerLog.w(TAG, "target device number: " + deviceID2);
            return false;
        }

        sameDevice = (deviceID1 == deviceID2);

        int[] pasteCounter = new int[2];
        /*
         * pasteCounter[0]: increment number of selected items already copied pasteCounter[1]: total
         * number of the selected items
         */
        pasteCounter[0] = 0;
        pasteCounter[1] = cutFiles.size();

        if (!sShowProgressDialog) {
            // notify UI thread to show progress dialog
            callBack.onUpateProgessBar(0, null, null,
                    FileManagerOperationActivity.SHOW_PROGRESS_DIALOG);
            sShowProgressDialog = true;
        }

        if (sameDevice) {
            for (File file : cutFiles) {
                if (callBack.isCancelled()) {
                    FileManagerLog.d(TAG, "Commit cut cancelled; break for loop: " + file.getName()
                            + "thread id: " + Thread.currentThread().getId());
                    break;
                }
                File newFile = null;
                if (file.canWrite() && file.canRead()) { // check cut permission
                    newFile = new File(targetDir, file.getName());
                    if (newFile.exists()) {
                        newFile = new File(autoGenerateName(newFile));
                    }

                    if (newFile != null
                            && newFile.getName().getBytes().length <= FILENAME_MAX_LENGTH) {
                        success = file.renameTo(newFile);
                    } else {
                        success = false;
                    }
                }

                if (success) {
                    notifyUpdates(context, ACTION_DELETE, file);
                    ScannerClient.getInstance().scanPath(newFile.getAbsolutePath());
                    FileManagerLog.v(TAG, "scan file: " + newFile.getAbsolutePath());
                    // update progress bar on UI thread
                    ++(pasteCounter[0]);
                    callBack.onUpateProgessBar((int) (pasteCounter[0]
                            * OPERATION_COMPLETE_PERCENTAGE / pasteCounter[1]), file.getName(),
                            Integer.toString(pasteCounter[0]) + "/"
                                    + Integer.toString(pasteCounter[1]),
                            FileManagerOperationActivity.OPERATION_SUCCESS);
                } else {
                    FileManagerLog.w(TAG, "Failed to cut: " + file.getName() + "thread id: "
                            + Thread.currentThread().getId());
                    callBack.onUpateProgessBar((int) (pasteCounter[0]
                            * OPERATION_COMPLETE_PERCENTAGE / pasteCounter[1]), file.getName(),
                            Integer.toString(pasteCounter[0]) + "/"
                                    + Integer.toString(pasteCounter[1]),
                            FileManagerOperationActivity.CUT_FAIL);
                }

                success = false; // reset success
            }
        } else {
            FileManagerLog.d(TAG, "Paste previous cutted files "
                    + "just when source directory is different from target directory"
                    + "thread id: " + Thread.currentThread().getId());

            Map<File, Long> pastedItemsSizeArray = new HashMap<File, Long>();
            if (pastedItemsSizeArray == null) {
                FileManagerLog.e(TAG, "Create HashMap failed in commitCut().");
                return false;
            }
            boolean canPaste = isSufficientMemory(callBack, targetDir, cutFiles,
                    pastedItemsSizeArray);

            if (canPaste) {
                for (File source : cutFiles) {
                    /*
                     * contentSize[0]: increment size of files already copiedcontentSize[1]: total
                     * size of files to be copied
                     */
                    long[] contentSize = new long[2];
                    contentSize[0] = 0;
                    contentSize[1] = pastedItemsSizeArray.get(source);
                    FileManagerLog.d(TAG, "Paste total file size(cut): " + contentSize[1]
                            + "thread id: " + Thread.currentThread().getId());

                    if (callBack.isCancelled()) {
                        success = false;
                        FileManagerLog.d(TAG, "Paste(cut) cancelled; break for loop "
                                + "thread id: " + Thread.currentThread().getId());
                        break;
                    }
                    ++(pasteCounter[0]);

                    if (!source.canWrite() || !source.canRead()) {
                        success = false;
                    } else {
                        try {
                            success = diffDevCopy(context, source, targetDir, pasteCounter,
                                    contentSize, callBack);
                        } catch (IOException e) {
                            FileManagerLog.e(TAG, "Fn-commitCut(): " + e.toString() + "thread id: "
                                    + Thread.currentThread().getId());
                        }
                    }
                    if (success) {
                        success = diffDevDelete(context, source, pasteCounter, callBack);
                    }
                }

            } else {
                // notify UI thread about the insufficient memory
                callBack.onUpateProgessBar(0, null, null,
                        FileManagerOperationActivity.INSUFFICIENT_MEMORY);
                return false;
            }
            // release pastedItemsSizeArray
            if (pastedItemsSizeArray != null) {
                pastedItemsSizeArray.clear();
                pastedItemsSizeArray = null;
            }
        }
        return success;
    }

    /**
     * This method copy files for cut->paste operation (when paste menu item is selected). The
     * source directory and target directory are on different SD cards.
     * @param context the context of FileManagerOperationActivity
     * @param copiedFile the copied file
     * @param targetDir the target directory that the files/folders are copied to
     * @param pasteCounter the number of selected items to be pasted (for fraction to show in UI)
     *            pasteCounter[0]: increment number of selected items already copied
     *            pasteCounter[1]: total number of the selected items
     * @param contentSize the size of files in the selected item (for calculating percentage)
     *            contentSize[0]: increment size of files already copied contentSize[1]: total size
     *            of files to be copied
     * @param callBack the FileTask object
     * @throws IOException On most conditions, diffDevCopy() will catch and handle IOExceptions
     *             caused by File operations itself, and won't throw any Exception. If throws, means
     *             there might be something wrong with target.
     * @return true if commit copy succeeds, false otherwise
     */
    private static boolean diffDevCopy(Context context, File copiedFile, File targetDir,
            int[] pasteCounter, long[] contentSize, FileTask callBack) throws IOException {
        boolean isSuccess = false;
        String pasteTag = copiedFile.getName();

        if ((!targetDir.isDirectory()) || copiedFile.getPath().equals(targetDir.getPath())
                || targetDir.getPath().startsWith(copiedFile.getPath() + "/")) {
            FileManagerLog.w(TAG, "Failed to paste: target directory "
                    + "is a subdirectory of source directory " + "thread id: "
                    + Thread.currentThread().getId());
            callBack.onUpateProgessBar(
                    (int) (contentSize[0] * OPERATION_COMPLETE_PERCENTAGE / contentSize[1]),
                    pasteTag, Integer.toString(pasteCounter[0]) + "/"
                            + Integer.toString(pasteCounter[1]),
                    FileManagerOperationActivity.PASTE_SAME_FOLDER);
            return isSuccess;
        }

        // Initialize the queue of files for copying.
        ArrayList<File> sourceArrayList = new ArrayList<File>();
        ArrayList<File> targetArrayList = new ArrayList<File>();
        sourceArrayList.add(copiedFile);
        targetArrayList.add(targetDir);

        while (!sourceArrayList.isEmpty()) {
            // Get the top(highest level) file or directory to copy.
            File source = sourceArrayList.get(0);
            File currentTargetDir = targetArrayList.get(0);
            FileManagerLog.d(TAG, "Commit copy(in cut) source file: " + source.getPath() + ";"
                    + " dir: " + source.isDirectory() + " thread id: "
                    + Thread.currentThread().getId());

            if (source.isDirectory()) {
                File target = new File(currentTargetDir, source.getName());
                if (target != null && target.exists()) {
                    target = new File(autoGenerateName(target));
                }
                if (target != null && target.getName().getBytes().length <= FILENAME_MAX_LENGTH) {
                    isSuccess = target.mkdir();
                    FileManagerLog.d(TAG, "Create target: " + target.getPath() + "; result: "
                            + isSuccess + "thread id: " + Thread.currentThread().getId());
                } else {
                    isSuccess = false;
                }

                if (isSuccess) {
                    contentSize[0] += source.length();
                    callBack.onUpateProgessBar(
                            (int) (contentSize[0] * COPY_COMPLETE_PERCENTAGE / contentSize[1]),
                            pasteTag, Integer.toString(pasteCounter[0]) + "/"
                                    + Integer.toString(pasteCounter[1]),
                            FileManagerOperationActivity.OPERATION_SUCCESS);
                    File[] children = source.listFiles();
                    if (children != null) {
                        for (int i = 0; i < children.length; i++) {
                            if (callBack.isCancelled()) {
                                FileManagerLog.d(TAG, "commit copy cancelled; break for loop"
                                        + "thread id: " + Thread.currentThread().getId());
                                break; // break for loop if cancel is true
                            }
                            sourceArrayList.add(children[i]);
                            targetArrayList.add(target);
                        }
                    }
                    if (callBack.isCancelled()) {
                        return false;
                    }
                } else {
                    callBack.onUpateProgessBar(
                            (int) (contentSize[0] * COPY_COMPLETE_PERCENTAGE / contentSize[1]),
                            pasteTag, Integer.toString(pasteCounter[0]) + "/"
                                    + Integer.toString(pasteCounter[1]),
                            FileManagerOperationActivity.PASTE_FAIL);
                }
            } else {
                InputStream in = null;
                OutputStream out = null;
                try {
                    File target = new File(currentTargetDir, source.getName());
                    FileManagerLog.d(TAG, "Copy(cut) to target directory: " + currentTargetDir
                            + "thread id: " + Thread.currentThread().getId());
                    FileManagerLog.d(TAG, "Copy(cut) from source name: " + source.getName()
                            + "thread id: " + Thread.currentThread().getId());

                    if (target != null && target.exists()) {
                        target = new File(autoGenerateName(target));
                    }

                    if (target != null && target.getName().getBytes().length <= FILENAME_MAX_LENGTH) {
                        isSuccess = target.createNewFile();
                    } else {
                        isSuccess = false;
                    }

                    if (isSuccess) {
                        try {
                            in = new FileInputStream(source);
                            FileManagerLog.d(TAG, "FileInputStream(cut) created" + "thread id: "
                                    + Thread.currentThread().getId());

                            try {
                                out = new FileOutputStream(target);
                                FileManagerLog.d(TAG, "FileOutputStream(cut) created"
                                        + "thread id: " + Thread.currentThread().getId());

                                // Copy data from in stream to out stream
                                if (null == mIOBuffer) {
                                    mIOBuffer = new byte[IO_BUFFER_LENGTH];
                                }
                                int len;

                                while ((len = in.read(mIOBuffer)) > 0) {
                                    if (callBack.isCancelled()) {
                                        FileManagerLog.d(TAG, "commit copy file cancelled; "
                                                + "break while loop" + "thread id: "
                                                + Thread.currentThread().getId());
                                        break; // break for loop if cancel is true
                                    }
                                    contentSize[0] += len;
                                    out.write(mIOBuffer, 0, len);

                                    // update progress bar on UI thread
                                    callBack.onUpateProgessBar((int) (contentSize[0]
                                            * COPY_COMPLETE_PERCENTAGE / contentSize[1]), pasteTag,
                                            Integer.toString(pasteCounter[0]) + "/"
                                                    + Integer.toString(pasteCounter[1]),
                                            FileManagerOperationActivity.OPERATION_SUCCESS);
                                }
                                ScannerClient.getInstance().scanPath(target.getAbsolutePath());
                                FileManagerLog.v(TAG, "scan file: " + target.getAbsolutePath());
                            } finally {
                                if (out != null) {
                                    out.close();
                                }
                            }
                        } finally {
                            if (in != null) {
                                in.close();
                            }
                        }

                        if (callBack.isCancelled()) {
                            boolean result = target.delete();
                            FileManagerLog.d(TAG, "Callback is cancelled and "
                                    + "a file/folder is deleted: " + result + "thread id: "
                                    + Thread.currentThread().getId());
                            return false;
                        }
                    } else {
                        contentSize[0] += source.length();
                        callBack.onUpateProgessBar(
                                (int) (contentSize[0] * COPY_COMPLETE_PERCENTAGE / contentSize[1]),
                                pasteTag, Integer.toString(pasteCounter[0]) + "/"
                                        + Integer.toString(pasteCounter[1]),
                                FileManagerOperationActivity.PASTE_FAIL);
                    }
                } catch (FileNotFoundException e) {
                    FileManagerLog.e(TAG, "Fn-diffDevCopy(): " + e.toString() + "thread id: "
                            + Thread.currentThread().getId());
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    FileManagerLog.e(TAG, "Fn-diffDevCopy(): " + e.toString() + "thread id: "
                            + Thread.currentThread().getId());
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                }
            }
            sourceArrayList.remove(0);
            targetArrayList.remove(0);
        }
        sourceArrayList = null;
        targetArrayList = null;
        return true;
    }

    /**
     * This method performs the delete operation for cutting when source directory and target
     * directory are on different SD cards.
     * @param context the context of FileManagerOperationActivity
     * @param deleteFile the file to be deleted
     * @param pasteCounter the number of selected items to be pasted (for fraction to show in UI)
     *            pasteCounter[0]: increment number of selected items already copied
     *            pasteCounter[1]: total number of the selected items
     * @param callBack a list of files to be deleted
     * @return true if all files are deleted successfully, false otherwise
     */
    private static boolean diffDevDelete(Context context, File deleteFile, int pasteCounter[],
            FileTask callBack) {
        boolean success = true;
        File currentDeletedFile = null;
        String deleteTag = deleteFile.getName();
        ArrayList<File> deletedFileList = new ArrayList<File>();
        FileManagerLog.d(TAG, "Delete file after copying in cutting operation "
                + "when sourceDir and targetDir are on different SD cards: " + deleteTag
                + "thread id: " + Thread.currentThread().getId());

        // Calculate the number of files and folders who will be deleted for
        // updating the ProgressBar.
        int[] tmp = new int[2];
        countDirFiles(deleteFile, tmp);

        /*
         * filesCounter[0]: increment number of files already deleted filesCounter[1]: total number
         * of files to be deleted
         */
        int[] filesCounter = new int[2];
        filesCounter[0] = 0;
        filesCounter[1] = tmp[0] + tmp[1];
        tmp = null;
        if (deletedFileList == null) {
            return false;
        }

        deletedFileList.add(deleteFile);
        while (!deletedFileList.isEmpty()) {
            if (callBack.isCancelled()) {
                FileManagerLog.d(TAG, "Delete cancelled; break for loop" + "thread id: "
                        + Thread.currentThread().getId());
                success = false;
                break;
            }
            // Pop up the file on the top of stack.
            currentDeletedFile = deletedFileList.get(deletedFileList.size() - 1);
            if (!currentDeletedFile.canRead() || !currentDeletedFile.canWrite()) {
                callBack.onUpateProgessBar(COPY_COMPLETE_PERCENTAGE
                        + (int) (DELETE_COMPLETE_PERCENTAGE * filesCounter[0] / filesCounter[1]),
                        deleteTag, Integer.toString(pasteCounter[0]) + "/"
                                + Integer.toString(pasteCounter[1]),
                        FileManagerOperationActivity.DELETE_FAIL);
                return false;
            }
            if (currentDeletedFile.isDirectory() && (currentDeletedFile.list().length > 0)) {
                File[] files = currentDeletedFile.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deletedFileList.add(files[i]);
                }
            } else { // If the file on top of stack is an empty directory or it is a general
                // file, it will be deleted directly.
                filesCounter[0]++;
                deletedFileList.remove(deletedFileList.size() - 1);
                if (currentDeletedFile.canWrite()) {
                    success = currentDeletedFile.delete();
                    if (success) {
                        notifyUpdates(context, ACTION_DELETE, currentDeletedFile);
                        // update progress bar on UI thread
                        callBack
                                .onUpateProgessBar(
                                        COPY_COMPLETE_PERCENTAGE
                                                + (int) (DELETE_COMPLETE_PERCENTAGE
                                                        * filesCounter[0] / filesCounter[1]),
                                        deleteTag, Integer.toString(pasteCounter[0]) + "/"
                                                + Integer.toString(pasteCounter[1]),
                                        FileManagerOperationActivity.OPERATION_SUCCESS);
                    } else {
                        FileManagerLog.d(TAG, "Failed to delete: "
                                + currentDeletedFile.getAbsolutePath() + "thread id: "
                                + Thread.currentThread().getId());
                        callBack
                                .onUpateProgessBar(
                                        COPY_COMPLETE_PERCENTAGE
                                                + (int) (DELETE_COMPLETE_PERCENTAGE
                                                        * filesCounter[0] / filesCounter[1]),
                                        deleteTag, Integer.toString(pasteCounter[0]) + "/"
                                                + Integer.toString(pasteCounter[1]),
                                        FileManagerOperationActivity.DELETE_FAIL);
                        success = false;
                        break;
                    }
                } else {
                    FileManagerLog.d(TAG, "Failed to delete: "
                            + currentDeletedFile.getAbsolutePath() + "thread id: "
                            + Thread.currentThread().getId());
                    callBack
                            .onUpateProgessBar(
                                    COPY_COMPLETE_PERCENTAGE
                                            + (int) (DELETE_COMPLETE_PERCENTAGE * filesCounter[0] / filesCounter[1]),
                                    deleteTag, Integer.toString(pasteCounter[0]) + "/"
                                            + Integer.toString(pasteCounter[1]),
                                    FileManagerOperationActivity.DELETE_DENY);
                    success = false;
                    break;
                }
            }
        }
        return success;
    }

    /**
     * This method notifies the updates of sdcard content
     * @param context the context of FileManagerOperationActivity
     * @param action broadcast action
     * @param file the changed file to be notified about
     */
    public static void notifyUpdates(Context context, String action, File file) {
        FileManagerLog.d(TAG, "Broadcasting action: " + action);

        if ("com.mediatek.filemanager.ACTION_DELETE".equals(action)) {
            // specific solution for music application
            Uri data = Uri.fromFile(file);
            context.sendBroadcast(new Intent(action, data));
        } else {
            if (file != null) {
                final String mountPath = MountPointHelper.getInstance().getRealMountPointPath(
                        file.getPath());
                if (mountPath != null) {
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
                            .parse("file://" + mountPath)));
                }
            }
        }
    }

    /**
     * This method performs the delete operation
     * @param context the context of FileManagerOperationActivity
     * @param deleteFiles a list of files to be deleted
     * @param callBack the object of FileTask
     */
    public static List<FileInfo> delete(Context context, List<FileInfo> toDeleteFilesInfo,
            FileTask callBack) {
        callBack.onUpateProgessBar(0, null, null, FileManagerOperationActivity.SHOW_PROGRESS_DIALOG);
        FileManagerLog.d(TAG, "To delete files number: " + toDeleteFilesInfo.size());
        FileManagerLog.d(TAG, "To delete thread id: " + Thread.currentThread().getId());
        /*
         * deletedCounter[0]: increment number of selected items already deleteddeletedCounter[1]:
         * total number of the selected items
         */
        int[] deletedCounter = new int[2];
        deletedCounter[0] = 0;
        deletedCounter[1] = toDeleteFilesInfo.size();

        List<FileInfo> deletedFilesInfo = new ArrayList<FileInfo>();
        for (int i = 0; i < toDeleteFilesInfo.size(); i++) {
            File file = toDeleteFilesInfo.get(i).getFile();
            if (!file.exists()) {
                FileManagerLog.d(TAG, "Delete file (not exsits):" + file.getName());
                // assume the not exists file is deleted success.
                ++(deletedCounter[0]);
                deletedFilesInfo.add(toDeleteFilesInfo.get(i));

                String count = deletedCounter[0] + "/" + deletedCounter[1];
                callBack.onUpateProgessBar(OPERATION_COMPLETE_PERCENTAGE, file.getName(), count,
                        FileManagerOperationActivity.OPERATION_SUCCESS);
                continue;
            }
            if (file.canWrite()) {
                if (callBack.isCancelled()) {
                    FileManagerLog.d(TAG, "Delete cancelled");
                    break;
                }

                ++(deletedCounter[0]);
                boolean success = deleteFile(context, file, deletedCounter, callBack);
                if (success) {
                    deletedFilesInfo.add(toDeleteFilesInfo.get(i));
                    // update progress bar on UI thread
                    String count = deletedCounter[0] + "/" + deletedCounter[1];
                    callBack.onUpateProgessBar(OPERATION_COMPLETE_PERCENTAGE, file.getName(),
                            count, FileManagerOperationActivity.OPERATION_SUCCESS);
                } else {
                    FileManagerLog.d(TAG, "Delete Failed:" + file.getName());
                    String count = deletedCounter[0] + "/" + deletedCounter[1];
                    callBack.onUpateProgessBar(OPERATION_COMPLETE_PERCENTAGE, file.getName(),
                            count, FileManagerOperationActivity.DELETE_FAIL);
                }
            } else {
                FileManagerLog.d(TAG, "Delete failed(Permission deny):" + file.getName());
                String count = deletedCounter[0] + "/" + deletedCounter[1];
                callBack.onUpateProgessBar(OPERATION_COMPLETE_PERCENTAGE, file.getName(), count,
                        FileManagerOperationActivity.DELETE_DENY);
            }
        }
        return deletedFilesInfo;
    }

    /**
     * This method performs the delete operation
     * @param context the context of FileManagerOperationActivity
     * @param deletedFile the file to be deleted
     * @param deletedCounter the number of selected items to be deleted (for fraction to show in UI)
     *            deletedCounter[0]: increment number of selected items already deleted.
     *            deletedCounter[1]: total number of the selected items
     * @param callBack the object of FileTask
     * @return true if the file is deleted successfully, false otherwise
     */
    private static boolean deleteFile(Context context, File deletedFile, int deletedCounter[],
            FileTask callBack) {
        FileManagerLog.d(TAG, "To delete file: " + deletedFile.getName());
        FileManagerLog.d(TAG, "Thread id: " + Thread.currentThread().getId());

        boolean success = true;
        ArrayList<File> deletedFileList = new ArrayList<File>();
        if (deletedFileList == null) {
            return false;
        }

        int[] filesCounter = new int[2];
        filesCounter[0] = 0;
        filesCounter[1] = countDirFiles(deletedFile) + 1;

        deletedFileList.add(deletedFile);
        while (!deletedFileList.isEmpty()) {
            if (callBack.isCancelled()) {
                FileManagerLog.d(TAG, "Delete cancelled");
                success = false;
                break;
            }
            File file = deletedFileList.get(deletedFileList.size() - 1);
            if (!file.exists()) {
                // file is not exists, remove it from the list
                filesCounter[0]++;
                deletedFileList.remove(deletedFileList.size() - 1);

                // assume the not exists file is deleted success.
                FileManagerLog.d(TAG, "Delete Failed(not exsits):" + file.getName());
                int percentage = (int) (filesCounter[0] * OPERATION_COMPLETE_PERCENTAGE / filesCounter[1]);
                String count = deletedCounter[0] + "/" + deletedCounter[1];
                callBack.onUpateProgessBar(OPERATION_COMPLETE_PERCENTAGE, file.getName(), count,
                        FileManagerOperationActivity.OPERATION_SUCCESS);
                continue;
            }
            if (!file.canRead() || !file.canWrite()) {
                FileManagerLog.w(TAG, "Delete failed(Permission deny): " + file.getAbsolutePath());
                if (filesCounter[0] > filesCounter[1]) {
                    filesCounter[0] = filesCounter[1];
                }
                int percentage = (int) (filesCounter[0] * OPERATION_COMPLETE_PERCENTAGE / filesCounter[1]);
                String count = deletedCounter[0] + "/" + deletedCounter[1];
                callBack.onUpateProgessBar(percentage, file.getName(), count,
                        FileManagerOperationActivity.DELETE_FAIL);
                return false;
            }
            if (file.isDirectory() && (file.list().length > 0)) {
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deletedFileList.add(files[i]);
                }
            } else { 
                // If the file on top of stack is an empty directory or it is a general
                // file, it will be deleted directly.
                filesCounter[0]++;
                deletedFileList.remove(deletedFileList.size() - 1);
                if (file.canWrite()) {
                    success = file.delete();
                    if (success) {
                        notifyUpdates(context, ACTION_DELETE, file);
                        if (filesCounter[0] > filesCounter[1]) {
                            filesCounter[0] = filesCounter[1];
                        }
                        // update progress bar on UI thread
                        int percentage = (int) (filesCounter[0] * OPERATION_COMPLETE_PERCENTAGE / filesCounter[1]);
                        String count = deletedCounter[0] + "/" + deletedCounter[1];
                        callBack.onUpateProgessBar(percentage, file.getName(), count,
                                FileManagerOperationActivity.OPERATION_SUCCESS);
                    } else {
                        FileManagerLog.d(TAG, "Delete Failed:" + file.getAbsolutePath());
                        if (filesCounter[0] > filesCounter[1]) {
                            filesCounter[0] = filesCounter[1];
                        }
                        int percentage = (int) (filesCounter[0] * OPERATION_COMPLETE_PERCENTAGE / filesCounter[1]);
                        String count = deletedCounter[0] + "/" + deletedCounter[1];
                        callBack.onUpateProgessBar(percentage, file.getName(), count,
                                FileManagerOperationActivity.DELETE_FAIL);
                        success = false;
                        break;
                    }
                } else {
                    FileManagerLog.w(TAG, "Delete failed(Permission deny): " + file.getAbsolutePath());
                    if (filesCounter[0] > filesCounter[1]) {
                        filesCounter[0] = filesCounter[1];
                    }
                    int percentage = (int) (filesCounter[0] * OPERATION_COMPLETE_PERCENTAGE / filesCounter[1]);
                    String count = deletedCounter[0] + "/" + deletedCounter[1];
                    callBack.onUpateProgessBar(percentage, file.getName(), count,
                            FileManagerOperationActivity.DELETE_DENY);
                    success = false;
                    break;
                }
            }
        }
        deletedFileList = null;
        return success;
    }
    

    //*/ Add by xiaocui 2012-08-02 for:[tyd00436129 ] rename datebase of music 
    public static void renameToDatabase(Context context, String oldPath, String newPath, String fileName) {
        ContentResolver resolver = context.getContentResolver();
        String where = MediaStore.Audio.Media.DATA + "=?";
        String columns[] = new String[] { MediaStore.Audio.Media._ID };
        String selectionArgs[] = new String[] { oldPath };
        Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns, where, selectionArgs, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                ContentValues values = new ContentValues(2);
                values.put(MediaStore.Audio.Media.DATA, newPath);
                //*/ add by freemeos.liuhaorao on 20160709 on stop run about music ring
                values.put(MediaStore.Audio.Media.DATE_MODIFIED, System.currentTimeMillis()/1000);
                //*/
                //values.put(MediaStore.Audio.Media.TITLE, fileName);
                resolver.update(uri, values, null, null);
            }
            cursor.close();
        }
    }
    //*/  

    /**
     * This method converts a size to a string
     * @param size the size of a file
     * @return the string represents the size
     */
    public static String sizeToString(long size) {
        String unit = UNIT_KB;
        double sizeDouble = (double) size / (double) 1024;
        if (sizeDouble > 1024) {
            sizeDouble = (double) sizeDouble / (double) 1024;
            unit = UNIT_MB;
        }
        if (sizeDouble > 1024) {
            sizeDouble = (double) sizeDouble / (double) 1024;
            unit = UNIT_GB;
        }
        if (sizeDouble > 1024) {
            sizeDouble = (double) sizeDouble / (double) 1024;
            unit = UNIT_TB;
        }

        // Add 0.005 for rounding-off.
        long sizeInt = (long) ((sizeDouble + 0.005) * 100.0); // strict to two
        // decimal places
        double formatedSize = ((double) sizeInt) / 100.0;

        if (formatedSize == 0) {
            return "0" + " " + unit;
        } else {
            return Double.toString(formatedSize) + " " + unit;
        }
    }

    /**
     * This method gets the detail information of a file/folder
     * @param context the context of FileManagerOperationActivity
     * @param file the file
     * @param detailsParts used to store file information
     * @return the details of the file
     */
    public static String getDetails(Context context, FileInfo fileInfo, String[] detailsParts) {
        FileManagerLog.d(TAG, "Get file details");
        Resources res = context.getResources();
        File file = fileInfo.getFile();
        // name
        detailsParts[0] = res.getString(R.string.name) + ": " + fileInfo.getFileDescription()
                + "\n";

        // size
        detailsParts[1] = res.getString(R.string.size) + ": " + 0 + "\n";

        // time last modified and DRW permission
        long time = file.lastModified();

        //*/modified by tyd wulianghuan 20130522 for fix bug[tyd00479821]
        String modifyDateTime = DateUtils.formatDateRange(context, time, time,
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | 
                DateUtils.FORMAT_NUMERIC_DATE );
        
        detailsParts[2] = res.getString(R.string.modified_time) + ": "
                + modifyDateTime + "\n"
                + getPermission(res, file);

        return detailsParts[0] + detailsParts[1] + detailsParts[2];
    }

    /**
     * This method gets the permission information of a file/folder
     * @param res used to get resources from resources file
     * @param file the file
     * @return the permission of the file (in form: drw)
     */
    public static String getPermission(Resources res, File file) {
        String permission = "";

        permission = permission.concat(res.getString(R.string.readable) + ": ");
        if (file.canRead()) {
            permission = permission.concat(res.getString(R.string.yes));
        } else {
            permission = permission.concat(res.getString(R.string.no));
        }

        permission = permission.concat("\n" + res.getString(R.string.writable) + ": ");
        if (file.canWrite()) {
            permission = permission.concat(res.getString(R.string.yes));
        } else {
            permission = permission.concat(res.getString(R.string.no));
        }

        permission = permission.concat("\n" + res.getString(R.string.executable) + ": ");
        if (file.canExecute()) {
            permission = permission.concat(res.getString(R.string.yes));
        } else {
            permission = permission.concat(res.getString(R.string.no));
        }
        return permission;
    }

    /**
     * This method gets the extension of a file
     * @param fileName the name of the file
     * @return the extension of the file
     */
    public static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        String extension = null;

        if ((lastDot > 0) && (lastDot < fileName.length() - 1)) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }
        return extension;
    }

    /**
     * This method gets the mime type based on the extension of a file
     * @param file the target file
     * @return the mime type of the file/folder
     */
    public static String getMimeTypeForFile(Context context, File file) {
        FileManagerLog.d(TAG, "getMimeTypeForFile");
        String mimeType;
        String fileName = file.getName();
        String extension = getFileExtension(fileName);

        if (extension == null) {
            return "unknown_ext_null_mimeType";
        }

        if (OptionsUtil.isDrmSupported() && extension.equalsIgnoreCase(EXT_DRM_CONTENT)) {
            return "application/vnd.oma.drm.content";
        }

        //mimeType = MediaFile.getMimeTypeBySuffix(fileName);
        mimeType = null;
        if (mimeType == null) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        if (mimeType == null) {
            return "unknown_ext_mimeType";
        }

        // special solution for checking 3gpp original mimetype
        // 3gpp extension could be video/3gpp or audio/3gpp
        if (mimeType.equalsIgnoreCase("video/3gpp") || mimeType.equalsIgnoreCase("video/3gpp2")) {
            FileManagerLog.d(TAG, "getMimeTypeForFile, a 3gpp or 3g2 file");
            return get3gppOriginalMimetype(context.getContentResolver(), file);
        }
        return mimeType;
    }

    /**
     * The file whose extension is 3gpp may be a audio file or a video file, so its mimeType should
     * get by querying.
     * @param file the file for querying
     * @return the mimeType of the 3gpp file
     */
    private static String get3gppOriginalMimetype(ContentResolver resolver, File file) {
        FileManagerLog.d(TAG, "get3gppOriginalMimetype");
        String mimeType = "video/3gpp";

        if (resolver == null) {
            return "video/3gpp";
        }

        while (!ScannerClient.getInstance().waitForScanningCompleted());

        // since 3gpp could be video or audio type,
        // we need to check audio and video content provider to find out its
        // real mimetype
        Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.MediaColumns.MIME_TYPE }, MediaStore.MediaColumns.DATA
                        + "=?", new String[] { file.getPath() }, null);

        try {
            if (cursor == null) {
                FileManagerLog.d(TAG, "get3gppOriginalMimetype " + "cursor is null");
            }
            if (cursor != null && cursor.moveToFirst()) {
                FileManagerLog.d(TAG, "get3gppOriginalMimetype " + "cursor is not null");
                mimeType = cursor.getString(0);

                if (mimeType != null) {
                    FileManagerLog.d(TAG, "Found " + file.getPath()
                            + " in: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mimetype: "
                            + mimeType);
                    return mimeType;
                } else {
                    FileManagerLog.d(TAG, "get3gppOriginalMimetype " + "get mime null from media");
                    return "video/3gpp";
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (mimeType == null) {
                return "unknown_3pgg_mimeType";
            } else {
                return mimeType;
            }
        }
    }

    public static int getIconIdForDirectory(Context context, FileInfo fileInfo) {
        if (!fileInfo.getFile().isDirectory()) {
            FileManagerLog.e(TAG, "getIconIdForDirectory, Illegal argument: not dir");
            return -1;
        }

        String fileName = fileInfo.getFile().getName();
        

        if (isCategoryFolder(fileInfo.getFile())) {
            return getCategoryFolderIcon(fileName);
        }
        return R.drawable.folder;

    }

    private static boolean isCategoryFolder(File file) {
        return FileManagerOperationActivity.sCategoryFoldersPath != null
                && !FileManagerOperationActivity.sCategoryFoldersPath.isEmpty()
                && FileInfoComparator.sequenceSearch(
                        FileManagerOperationActivity.sCategoryFoldersPath, file.getAbsolutePath()) >= 0;
    }

    /**
     * This method gets the drawable id based on the mimetype
     * @param mimeType the mimeType of a file/folder
     * @return the drawable icon id based on the mimetype
     */
    public static int getDrawableId(String mimeType) {
        if (mimeType.startsWith("application/vnd.android.package-archive")) {
            return R.drawable.fm_apk;
        } else if (mimeType.startsWith("application/zip")) {
            return R.drawable.fm_zip;
        } else if (mimeType.startsWith("application/ogg")) {
            return R.drawable.fm_audio;
        } else if (mimeType.startsWith("audio/")) {
            return R.drawable.fm_audio;
        } else if (mimeType.startsWith("image/")) {
            return R.drawable.fm_picture;
        } else if (mimeType.startsWith("text/")) {
            return R.drawable.fm_doc;
            //*/added by tyd carl,20120704,[tyd00429785],icons display
        }else if (mimeType.startsWith("application/msword")) {
            return R.drawable.fm_doc;
        }else if (mimeType.startsWith("application/mspowerpoint")) {
            return R.drawable.fm_doc;
        }else if (mimeType.startsWith("application/vnd.ms-excel")) {
            return R.drawable.fm_doc;
            //*/
        } else if (mimeType.startsWith("video/")) {
            return R.drawable.fm_video;
        } else {
            return R.drawable.fm_unknown;
        }
    }

    /**
     * This method gets the mime type from multiple files (order to return: image->video->other)
     * @param drmManagerClient for get some services from DrmManagerClient
     * @param currentDirPath the current directory
     * @param files a list of files
     * @return the mime type of the multiple files
     */

    public static String getShareMultipleMimeType(Context context,
            DrmManagerClient drmManagerClient, String currentDirPath, List<String> files) {
        String mimeType = null;
        String path = null;

        for (String s : files) {
            mimeType = getMimeTypeForFile(context, new File(currentDirPath + "/" + s));
            FileManagerLog.d(TAG, "Get multiple files mimetype: " + mimeType);

            if (OptionsUtil.isDrmSupported()) {
                if (mimeType.equalsIgnoreCase("application/vnd.oma.drm.content")) {
                    path = currentDirPath + "/" + s;
                    mimeType = drmManagerClient.getOriginalMimeType(path);
                }
            }
            if (null != mimeType) {
                if (mimeType.startsWith("image/")) {
                    break;
                } else if (mimeType.startsWith("video/")) {
                    break;
                }
            }
        }

        if (mimeType == null || mimeType.startsWith("unknown")) {
            mimeType = UNRECOGNIZED_FILE_MIME_TYPE;
        }
        FileManagerLog.d(TAG, "Multiple files' mimetype is " + mimeType);
        return mimeType;
    }

    /**
     * This method gets the category icon id for a category folder
     * @param fileName the name of the category folder
     * @return the category icon id
     */
    private static int getCategoryFolderIcon(String fileName) {
        FileManagerLog.d(TAG, "getcategoryFolderIcon, fileName: " + fileName);
        if (fileName.equalsIgnoreCase("Document")) {
            return R.drawable.fm_document_folder;
        } else if (fileName.equalsIgnoreCase("Download")) {
            return R.drawable.fm_download_folder;
        } else if (fileName.equalsIgnoreCase("Music")) {
            return R.drawable.fm_music_folder;
        } else if (fileName.equalsIgnoreCase("Photo")) {
            return R.drawable.fm_photo_folder;
        } else if (fileName.equalsIgnoreCase("Received File")) {
            return R.drawable.fm_received_folder;
        } else if (fileName.equalsIgnoreCase("Video")) {
            return R.drawable.fm_video_folder;
        } else {
            return R.drawable.fm_folder;
        }
    }

    /**
     * This method sets the last operation
     * @param op the last operation performed
     */
    public static void setLastOperation(int op) {
        FileManagerLog.d(TAG, "set last operation: " + op);
        sLastOperation = op;
    }

    /**
     * This method gets the last operation
     * @return the last operation
     */
    public static int getLastOperation() {
        return sLastOperation;
    }

    public static Bitmap createSDCardIcon(Resources res, Bitmap defIcon) {
        Bitmap temp = BitmapFactory.decodeResource(res, R.drawable.fm_file_location_icon);
        int offx = temp.getWidth() / 4;
        int width = offx + defIcon.getWidth();
        int height = defIcon.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        c.drawBitmap(defIcon, offx, 0, null);
        c.drawBitmap(temp, 0, 0, null);
        c.save(Canvas.ALL_SAVE_FLAG);
        c.restore();
        return bitmap;
    }

    private static Toast sToast = null;

    public static void showToast(Context context, String msg) {
        if (sToast == null) {
            sToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        } else {
            sToast.setText(msg);
        }
        sToast.show();
    }

    public static void showToast(Context context, int msgId) {
        if (sToast == null) {
            sToast = Toast.makeText(context, msgId, Toast.LENGTH_SHORT);
        } else {
            sToast.setText(msgId);
        }
        sToast.show();
    }
}
