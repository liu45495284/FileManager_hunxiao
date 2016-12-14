package com.freeme.filemanager.view.garbage;

public class GarbageCleanupItem {
    public static final int STATUS_CLEANED = 7;
    public static final int STATUS_CLEANING = 6;
    public static final int STATUS_SCANNED = 3;
    public static final int STATUS_SCANNING = 2;
    public static final int STATUS_SELECTION = 4;
    public static final int STATUS_WAITING = 0;
    public static final int STATUS_WAITING_FOR_CLEANING = 5;
    public static final int STATUS_WAITING_FOR_SCANNING = 1;
    public boolean mCollapsed;
    public String mCurrentFile;
    public int mFileCount;
    public String mFormatString;
    public int mFormatStringResId;
    public String mName;
    public int mNameResId;
    public String mNoGarbageString;
    public int mNoGarbageStringResId;
    public boolean mSelected;
    public long mSizeCount;
    public int mStatus;
    public int mType;

    public GarbageCleanupItem() {
        mCollapsed = true;
    }

    public GarbageCleanupItem(int i, int j, int k, int l) {
        mCollapsed = true;
        mNameResId = i;
        mFormatStringResId = j;
        mNoGarbageStringResId = k;
        mType = l;
        mStatus = 0;
        mFileCount = 0;
        mSizeCount = 0L;
    }

    public void reset() {
        mStatus = 0;
        mFileCount = 0;
        mSizeCount = 0L;
    }

}
