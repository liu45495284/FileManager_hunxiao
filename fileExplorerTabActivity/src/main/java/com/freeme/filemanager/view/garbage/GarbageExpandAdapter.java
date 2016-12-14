package com.freeme.filemanager.view.garbage;

import com.freeme.filemanager.R;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.garbage.AsyncGarbageCleanupHelper.GarbageItem;
import com.freeme.filemanager.util.FeatureOption;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class GarbageExpandAdapter extends BaseExpandableListAdapter implements
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "GarbageExpandAdapter";
    private Context mContext;
    private String mExternalPath = null;
    private boolean mFinishState = false;
    private boolean mCleanupState = false;
    private String[] mGroupItemName = null;
    private long[] mGroupItemSize = null;
    private HashMap<Integer, Boolean> mGroupMark = null;            // mark group be selected
    private LayoutInflater mInflater = null;
    private String mInternalPath = null;
    private List<GarbageItem> mListData;
    private OnUpdateButtonStateListener mListener;
    private String mNoFolderString = null;
    private String mNoGarbageString = null;
    private String[] mRootPathResources;
    private List<GarbageItem> mSelectedArray;
    private int[] mState;

    public GarbageExpandAdapter(Context paramContext) {
        Log.i("wulianghuanTag", "new GarbageExpandAdapter()");
        this.mContext = paramContext;
        this.mInflater = ((LayoutInflater) mContext.getSystemService("layout_inflater"));
        mGroupItemName = paramContext.getResources().getStringArray(R.array.group_item_names);
        this.mGroupItemSize = new long[mGroupItemName.length];
        mGroupMark = new HashMap();
        this.mSelectedArray = new ArrayList<GarbageItem>();
        this.mRootPathResources = new String[] {
                mContext.getResources().getString(R.string.storage_phone),
                mContext.getResources().getString(R.string.storage_sd_card) };
        
        //modify by tyd liuyong 20140806 for kk storage
        if (FeatureOption.MTK_MULTI_STORAGE_SUPPORT) {
            this.mInternalPath = "/storage/sdcard0/";
            this.mExternalPath = "/storage/sdcard1/";
        }else{
            this.mInternalPath = "/storage/emulated/0/";
        }
        
        this.mNoFolderString = mContext.getResources().getString(R.string.no_folder);
        this.mNoGarbageString = mContext.getResources().getString(R.string.no_garbage);
        mState = new int[mGroupItemName.length];
        for (int i = 0; i < mGroupItemSize.length; ++i) {
            mGroupMark.put(i, true);
        }
    }

    private ChildViewHolder findChildView(View paramView) {
        ChildViewHolder childViewHolder = new ChildViewHolder();
        childViewHolder.mAppName = ((TextView) paramView.findViewById(R.id.child_name));
        childViewHolder.mChildPath = ((TextView) paramView.findViewById(R.id.child_path));
        childViewHolder.mSizeInfo = ((TextView) paramView.findViewById(R.id.child_size));
        childViewHolder.mCheckBox = ((CheckBox) paramView.findViewById(R.id.child_picker));
        childViewHolder.mProgressBar = ((ProgressBar) paramView.findViewById(R.id.child_progress));
        childViewHolder.mCheckBox.setClickable(false);
        paramView.setTag(childViewHolder);
        return childViewHolder;
    }

    private GroupViewHolder findGroupView(View paramView) {
        GroupViewHolder groupViewHolder = new GroupViewHolder();
        groupViewHolder.mIndicatorView = ((ImageView) paramView.findViewById(R.id.group_image));
        groupViewHolder.mTitle = ((TextView) paramView.findViewById(R.id.group_name));
        groupViewHolder.mSizeInfo = ((TextView) paramView.findViewById(R.id.group_size));
        groupViewHolder.mCheckBox = ((CheckBox) paramView.findViewById(R.id.group_picker));
        groupViewHolder.mProgressBar = ((ProgressBar) paramView.findViewById(R.id.group_progress));
        groupViewHolder.mFinishView = ((ImageView) paramView.findViewById(R.id.finish_view));
        paramView.setTag(groupViewHolder);
        return groupViewHolder;
    }

    public void cancelAllMark() {
        mSelectedArray.clear();
        if (mGroupMark != null) {
            for (int i = 0; i < mGroupItemSize.length; i++) {
                mGroupMark.put(i, false);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public GarbageItem getChild(int groupPosition, int childPosition) {
        if ((mGroupItemName != null) && (groupPosition != mGroupItemName.length-1) && (mListData != null)) {
            return mListData.get(childPosition);
        }
        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public int getChildMarkItem() {
        if (mSelectedArray != null) {
            return mSelectedArray.size();
        }
        return 0;
    }

    public List<GarbageItem> getChildSelectedItems() {
        if ((mGroupMark != null) && (mGroupMark.containsKey(4)) && (mGroupMark.get(4)) && (mListData != null) && (mListData.size() > 0)) {
            mSelectedArray.clear();
            mSelectedArray.addAll(mListData);
        }
        return mSelectedArray;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean paramBoolean, View convertView, ViewGroup paramViewGroup) {
        ChildViewHolder childViewHolder;
        GarbageItem garbageItem = null;
        String str1 = null;
        String str2 = null;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.child_item_layout, null);
            childViewHolder = findChildView(convertView);
        }
        childViewHolder = (ChildViewHolder) convertView.getTag();
        
        garbageItem = mListData.get(childPosition);
        if (garbageItem != null) {
            if (mSelectedArray.contains(garbageItem)) {
                childViewHolder.mCheckBox.setChecked(true);
            } else {
                childViewHolder.mCheckBox.setChecked(false);
            }

            childViewHolder.mAppName.setText(garbageItem.appName);
            childViewHolder.mChildPath.setText("[" + garbageItem.rootPath + garbageItem.path + "]");
            long fileSize = (new DirectorySizeDetector(garbageItem.rootPath + garbageItem.path)).getSize();
            childViewHolder.mSizeInfo.setText(Util.convertStorage(fileSize));
        }
        
        if (mState[groupPosition] == 0) {
            childViewHolder.mCheckBox.setVisibility(View.GONE);
            childViewHolder.mProgressBar.setVisibility(View.VISIBLE);
        }
        
        if (this.mFinishState) {
            childViewHolder.mCheckBox.setVisibility(View.GONE);
            childViewHolder.mProgressBar.setVisibility(View.GONE);
        }
        
        return convertView;
    }

    @Override
    public int getChildrenCount(int paramInt) {
        if ((mGroupItemName != null) && (paramInt == mGroupItemName.length-1) && (mListData != null)) {
            return mListData.size();
        }
        return 0;
    }

    @Override
    public String getGroup(int position) {
        if (mGroupItemName != null) {
            return mGroupItemName[position];
        }
        return null;
    }

    @Override
    public int getGroupCount() {
        if (mGroupItemName != null) {
            return mGroupItemName.length;
        }
        return 0;
    }

    @Override
    public long getGroupId(int paramInt) {
        return paramInt;
    }

    public int getGroupItemProgressState(int paramInt) {
        return mState[paramInt];
    }
    
    public void markGroupItem(int groupPosition) {
        if (mGroupMark == null) {
            return;
        }
        if (mGroupMark.containsKey(groupPosition)) {
            boolean checked = mGroupMark.get(groupPosition);
            mGroupMark.put(groupPosition, !checked);
        }
        mListener.onUpdate();
        notifyDataSetChanged();
    }

    public ArrayList getGroupMarkItem() {
        ArrayList arrayList = new ArrayList();
        if (mGroupMark != null) {
            for (Entry<Integer, Boolean> entry : mGroupMark.entrySet()) {
                if (entry.getValue() == true) {
                    arrayList.add(entry.getKey());
                }
            }
        }
        return arrayList;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
            View paramView, ViewGroup paramViewGroup) {
        GroupViewHolder groupViewHolder;
        if (paramView == null) {
            paramView = this.mInflater.inflate(R.layout.group_item_layout, null);
            groupViewHolder = findGroupView(paramView);
        } else {
            groupViewHolder = (GroupViewHolder) paramView.getTag();
        }
        groupViewHolder.mTitle.setText(mGroupItemName[groupPosition]);
        groupViewHolder.mCheckBox.setTag(groupPosition);
         //modified by mingjun for clean up
        if(groupViewHolder.mCheckBox.isChecked()){
        if (groupPosition == mGroupItemName.length-1) {
            groupViewHolder.mIndicatorView.setVisibility(View.VISIBLE);
            groupViewHolder.mIndicatorView.setBackgroundDrawable(mContext
                    .getResources().getDrawable(isExpanded? R.drawable.btn_open_background : R.drawable.btn_close_background));
            if (mGroupItemSize[groupPosition] == 0) {
                if (mListData != null){
                }
                if (mListData != null && mListData.size() > 0) {
                    //*start/modify by tyd heqianqian for change itemtext value on 20151215
                    if(mListData.size()<=1){
                        groupViewHolder.mSizeInfo.setText(mListData.size() + " " + (mContext.getString(R.string.child_item_count)).toString().replace("s", ""));
                    }
                    else {
                       groupViewHolder.mSizeInfo.setText(mListData.size() + " " + mContext.getString(R.string.child_item_count).toString());
                }
                    //*end/
                } else {
                    groupViewHolder.mSizeInfo.setText(this.mNoGarbageString);
                }
            } else {
                if(this.mFinishState){
                    groupViewHolder.mSizeInfo.setText(this.mNoGarbageString);
                }else{
                    groupViewHolder.mSizeInfo.setText(Util.convertStorage(this.mGroupItemSize[groupPosition]));
                }
                
            }
        } else {
            groupViewHolder.mIndicatorView.setVisibility(View.GONE);
            if (mGroupItemSize[groupPosition] <= 0) {
                groupViewHolder.mSizeInfo.setText(this.mNoFolderString);
            } else {
                //modified by mingjun for emptyDir size
                if(groupPosition==0){
                    String emptyDir = Util.convertStorage(this.mGroupItemSize[groupPosition]);
                    if(emptyDir.equals("64.0 KB")||emptyDir.equals("32.0 KB")){
                        groupViewHolder.mSizeInfo.setText(this.mNoFolderString);
                    }else{
                        groupViewHolder.mSizeInfo.setText(Util.convertStorage(this.mGroupItemSize[groupPosition]));

                    }
                }else{
                groupViewHolder.mSizeInfo.setText(Util.convertStorage(this.mGroupItemSize[groupPosition]));
            }}
        }
        }
        groupViewHolder.mCheckBox.setClickable(true);
        groupViewHolder.mCheckBox.setOnCheckedChangeListener(this);

        if (mGroupMark == null) {
            groupViewHolder.mCheckBox.setChecked(false);
        } else {
            groupViewHolder.mCheckBox.setChecked(mGroupMark.get(groupPosition));
        }

        if (mState[groupPosition] == 0) {
            groupViewHolder.mCheckBox.setVisibility(View.GONE);
            groupViewHolder.mProgressBar.setVisibility(View.VISIBLE);
        } else {
            if(mCleanupState){
                groupViewHolder.mCheckBox.setClickable(false);
            }
            groupViewHolder.mCheckBox.setVisibility(View.VISIBLE);
            groupViewHolder.mProgressBar.setVisibility(View.GONE);
        }
        // if garbage clean up work done
        if (mFinishState) {
            groupViewHolder.mCheckBox.setVisibility(View.GONE);
            groupViewHolder.mProgressBar.setVisibility(View.GONE);
            if (groupViewHolder.mCheckBox.isChecked()) {
                groupViewHolder.mFinishView.setVisibility(View.VISIBLE);
            }
        }
        return paramView;
    }

    public boolean hasStableIds() {
        return false;
    }

    public boolean isChildSelectable(int paramInt1, int paramInt2) {
        return true;
    }

    public void markChildItem(int groupPosition, int childPosition) {
        if ((mGroupMark == null) || (groupPosition >= mGroupItemSize.length) || (mListData == null) || (childPosition >= mListData.size())) {
            return;
        }
        
        GarbageItem garbageItem = (GarbageItem) mListData.get(childPosition);
        if (mSelectedArray.contains(garbageItem)) {
            int location = mSelectedArray.indexOf(garbageItem);
            mSelectedArray.remove(location);
        } else {
            mSelectedArray.add(garbageItem);
        }
        
        if (mGroupMark != null) {
            if (mSelectedArray.size() == mListData.size()) {
                mGroupMark.put(groupPosition,true);
            } else {
                mGroupMark.put(groupPosition, false);
            }
        }
        
        notifyDataSetChanged();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int position = (Integer)buttonView.getTag();
        if(position == mGroupItemName.length-1) {
            if(isChecked){
                if (mListData != null && mListData.size() > 0) {
                    mSelectedArray.clear();
                    mSelectedArray.addAll(mListData);
                }
            }
            //*start/ add by droi heqianqian to keep checked same on 20151215
            else
            {
                if(getChildSelectedItems().size()==getChildrenCount(position)) {
                    getChildSelectedItems().clear();
                }
            }
            //*/end/
        }
        if (mGroupMark != null) {
            boolean contained = mGroupMark.containsKey(position);
            if (contained) {
                mGroupMark.put(position, isChecked);
            }
        }
        mListener.onUpdate();
        notifyDataSetChanged();
    }

    public void setAllGroupItemProgress(int paramInt) {
        for (int i = 0; i < mGroupItemName.length; ++i) {
            mState[i] = paramInt;
        }
        notifyDataSetChanged();
    }

    public void setChildData(List<GarbageItem> listData) {
        this.mListData = listData;
        this.mSelectedArray.clear();
        for(GarbageItem garbageItem : mListData){
            mSelectedArray.add(garbageItem);
        }
        notifyDataSetChanged();
    }

    public void setFinishState(boolean paramBoolean) {
        mFinishState = paramBoolean;
        notifyDataSetChanged();
    }
    public void setCleanupState(boolean paramBoolean) {
        mCleanupState = paramBoolean;
        notifyDataSetChanged();
    }
    public void setGroupData(int position, long size) {
        if (position >= mGroupItemSize.length) {
            return;
        }
        mGroupItemSize[position] = size;
        notifyDataSetChanged();
    }

    public void setGroupItemProgress(int position, int progress) {
        mState[position] = progress;
    }

    public void setOnUpdateButtonStateListener(OnUpdateButtonStateListener onUpdateButtonStateListener) {
        this.mListener = onUpdateButtonStateListener;
    }

    private class ChildViewHolder {
        ProgressBar mProgressBar;
        CheckBox mCheckBox;
        TextView mSizeInfo;
        TextView mAppName;
        TextView mChildPath;

        private ChildViewHolder() {
        }
    }

    private class GroupViewHolder {
        ProgressBar mProgressBar;
        CheckBox mCheckBox;
        ImageView mFinishView;
        ImageView mIndicatorView;
        TextView mSizeInfo;
        TextView mTitle;

        private GroupViewHolder() {
        }
    }

    public static abstract interface OnUpdateButtonStateListener {
        public abstract void onUpdate();
    }
}