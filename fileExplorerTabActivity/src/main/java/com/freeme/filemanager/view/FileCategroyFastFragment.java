package com.freeme.filemanager.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.freeme.filemanager.FileExplorerTabActivity;
import com.freeme.filemanager.FileManagerApplication;
import com.freeme.filemanager.R;

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.storage.StorageVolume;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.freeme.filemanager.FileExplorerTabActivity.IBackPressedListener;
import com.freeme.filemanager.FileManagerApplication.SDCardChangeListener;
import com.freeme.filemanager.controller.FileViewInteractionHub;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.FavoriteDatabaseHelper.FavoriteDatabaseListener;
import com.freeme.filemanager.util.FileCategoryHelper.CategoryInfo;
import com.freeme.filemanager.util.FileCategoryHelper.FileCategory;
import com.freeme.filemanager.util.Util.MemoryCardInfo;
import com.freeme.filemanager.util.Util.SDCardInfo;
import com.freeme.filemanager.util.FileCategoryHelper;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.StorageHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.util.featureoption.FeatureOption;

/**
 * Added by droi xueweili for category fragment when app launch and origin is Fragment FragmenCategoryFragment 20160509
 * @author xueweili
 *
 */
public class FileCategroyFastFragment extends BaseCategoryFragment implements
        FavoriteDatabaseListener, IBackPressedListener, SDCardChangeListener,
        OnItemClickListener, OnClickListener {

    protected static final String LOG_TAG = "FileCategroyFastFragment";
    private static final String BACKSTACK_TAG = "tag";
    protected static final int SET_CATEGORY_INFO = 0x01;
    public static final String CATEGORY_TAG = "category";
    private View mRootView = null;
    private RoundProgressBar mRoundProgressBar3, mRoundProgressBar2;
    public FileViewInteractionHub mFileViewInteractionHub;
    private FileExplorerTabActivity mActivity;
    private boolean noSdCard = false;
    private long sdCardUsed = 0;
    private long sdCard = 0;
    private long memoryCardUsed = 0;
    private long memoryCard = 0;
    private String sdCardUseds;
    private long sdCardUsedl;
    private String sdCards;
    private String sdCardFrees;
    private long memoryCardUsedl;
    private String memoryCards;
    private long progress = 0;
    private long progress1 = 0;
    private String memoryFrees;
    private FileManagerApplication mApplication;
    private TextView sdCardView, sdCardinfoView, sdPercentView, memoryCardView,
            memoryCardinfoView, memoryPercentView;
    private FileCategoryHelper mFileCagetoryHelper;
    private AsyncTask<Void, Void, Void> mRefreshCategoryInfoTask;
    private static final String ROOT_DIR = "/mnt";
    private CategoryItemAdapter mCategoryAdapter;
    private FileIconHelper mFileIconHelper;
    private FavoriteList mFavoriteList;
    private MemoryCardInfo memoryCardInfo;

    private static HashMap<Integer, FileCategory> button2Category = new HashMap<Integer, FileCategory>();

    private static List<CategoryItem> mCategoryItems = new ArrayList<FileCategroyFastFragment.CategoryItem>();
    private static int[] icons = new int[] { R.drawable.category_icon_music,
            R.drawable.category_icon_video, R.drawable.category_icon_picture,
            R.drawable.category_icon_apk, R.drawable.category_icon_document,
            R.drawable.category_icon_favorite };
    private static int[] mTexts = new int[] { R.string.category_music,
            R.string.category_video, R.string.category_picture,
            R.string.category_apk, R.string.category_document,
            R.string.category_favorite };
    private static FileCategory[] categories = { FileCategory.Music,
            FileCategory.Video, FileCategory.Picture, FileCategory.Apk,
            FileCategory.Doc, FileCategory.Favorite };

    static class CategoryItem {
        public int iconId;
        public int textStringId;
        public int countId;
        public long count = 0;
        public FileCategory category;
    }

    Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            int what = msg.what;
            switch (what) {
            case SET_CATEGORY_INFO:
                setCategoryInfo();
                break;

            default:
                break;
            }
        };
    };
    private long sdCardFree;
    private long memoryFree;
    private long memoryCardss;
    private long sdCardss;
    

    static {
        for (int i = 0; i < mTexts.length; i++) {
            CategoryItem item = new CategoryItem();
            item.textStringId = mTexts[i];
            item.iconId = icons[i];
            item.category = categories[i];
            mCategoryItems.add(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mActivity = ((FileExplorerTabActivity) getActivity());

        mRootView = inflater
                .inflate(R.layout.file_explorer_fast_category, null);
        mRoundProgressBar2 = (RoundProgressBar) mRootView
                .findViewById(R.id.mainroundProgressBar2);
        mRoundProgressBar3 = (RoundProgressBar) mRootView
                .findViewById(R.id.mainroundProgressBar3);

        mFavoriteList = new FavoriteList(mActivity, this);

        mApplication = (FileManagerApplication) mActivity.getApplication();
        mRoundProgressBar3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.i(LOG_TAG, "memoryCard");
                Intent info = new Intent(mActivity, MoneyInfoActivity.class);
                info.putExtra("sdMoneryCard", memoryCard);
                info.putExtra("sdMoneryCardused", memoryCardUsed);
                info.putExtra("sdMoneryCards", memoryCardss);
                info.putExtra("sdMoneryFrees", memoryFree);
                info.putExtra("sdMoneryCardusedl", memoryCardUsedl);
                
                if (noSdCard) {
                    info.putExtra("isCard", GlobalConsts.IS_CATEGORY_FRAGMENT);
                } else {
                    info.putExtra("isCard", GlobalConsts.IS_MEMORY_CARD);
                }

                startActivity(info);
            }
        });
        /*/ modify by freemeos.liuhaoran on 20160707 for not click mRoundProgressBar2 after upload sd
        mRoundProgressBar2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.i(LOG_TAG, "sdCard");
                Intent info = new Intent(mActivity, MoneyInfoActivity.class);
                info.putExtra("sdMoneryCard", sdCard);
                info.putExtra("sdMoneryCardused", sdCardUsed);
                info.putExtra("sdMoneryCards", sdCards);
                info.putExtra("sdMoneryFrees", sdCardFrees);
                info.putExtra("sdMoneryCardusedl", sdCardUsedl);
                info.putExtra("isCard", GlobalConsts.IS_SD_CARD);
                startActivity(info);
            }
        });
        //*/

        mApplication.addSDCardChangeListener(this);
        sdCardView = (TextView) mRootView.findViewById(R.id.sd_card);
        sdCardinfoView = (TextView) mRootView.findViewById(R.id.sd_card_info);
        sdPercentView = (TextView) mRootView.findViewById(R.id.sd_percent);
        memoryCardView = (TextView) mRootView.findViewById(R.id.memory_card);
        memoryCardinfoView = (TextView) mRootView
                .findViewById(R.id.memory_card_info);
        memoryPercentView = (TextView) mRootView
                .findViewById(R.id.memory_percent);

        mCategoryAdapter = new CategoryItemAdapter(mActivity, mCategoryItems);
        GridView gridView = (GridView) mRootView
                .findViewById(R.id.category_buttons);
        gridView.setOnItemClickListener(this);
        //*/ freeme.liuhaoran , 20160811 , no grab focus
        gridView.setFocusable(false);
        //*/
        gridView.setAdapter(mCategoryAdapter);

        // mFileViewInteractionHub.setRootPath(ROOT_DIR);
        
        /*/ freeme.liuhaoran , 20160718 , repeat
        mRoundProgressBar2 = (RoundProgressBar) mRootView
                .findViewById(R.id.mainroundProgressBar2);
        mRoundProgressBar3 = (RoundProgressBar) mRootView
                .findViewById(R.id.mainroundProgressBar3);
        //*/
        setupCategoryInfo();
        
        
        return mRootView;
    }

    private void setupCategoryInfo() {
        Log.i("xueweili", "setupCategoryInfo ");
        mFileCagetoryHelper = new FileCategoryHelper(mActivity);
        refreshCategoryInfo();
    }

    public boolean isHomePage() {
        return false;
    }

    public void setConfigurationChanged(boolean isChange) {

    }

    private void setupClick(int id) {
        View button = mRootView.findViewById(id);

        Log.i(LOG_TAG, "id=" + id);
    }

    @Override
    public void onResume() {
        super.onResume();
        //Debug.stopMethodTracing();
        
        //*/ freeme.liuhaoran , 20160718 , judge sd progressbar whether can click 
        ArrayList<StorageVolume> mountedVolumeList = StorageHelper.getInstance(mActivity).getMountedVolumeList();
        //Log.i("liuhaoran2", "mountedVolumeList = " + mountedVolumeList.size());
        if (mountedVolumeList.size() > 1) {
            mRoundProgressBar2.setOnClickListener(this);
        } else {
            mRoundProgressBar2.setOnClickListener(null);
        }
        //*/
        
        refreshCategoryInfo();
        
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.i("xueweili", "onPause" + (isVisible())+(isHidden()) + (isAdded())+(isResumed()));
    }
    @Override
    public void pagerUserHide() {
        super.pagerUserHide();
        Log.i("xueweili", "pagerUserHide" + (isVisible())+(isHidden()) + (isAdded())+(isResumed()));
    }
    @Override
    public void pagerUserVisible() {
        super.pagerUserVisible();
        refreshCategoryInfo();
        Log.i("xueweili", "pagerUserVisible" + (isVisible())+(isHidden()) + (isAdded())+(isResumed()));
    }
    
    
    @Override
    public void onMountStateChange(int flag) {
        refreshCategoryInfo();
        if (flag == SDCardChangeListener.flag_INJECT) {
            mRoundProgressBar2.setOnClickListener(this);
        } else if (flag == SDCardChangeListener.flag_UMMOUNT) {
            mRoundProgressBar2.setOnClickListener(null);
        }
    }
    
    @Override
    public boolean onBack() {
        return false;
    }

    public void onDataChanged() {
        mHandler.sendEmptyMessage(SET_CATEGORY_INFO);
    }

    public FileIconHelper getFileIconHelper() {
        return null;
    }

    class CategoryItemAdapter extends BaseAdapter {

        private List<CategoryItem> mItems = null;
        private Context mContext;

        public CategoryItemAdapter(Context context, List<CategoryItem> items) {
            mItems = items;
            mContext = context;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return -1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(mContext).inflate(
                    R.layout.file_explorer_fast_category_item, null);
            ImageView imageView = (ImageView) view
                    .findViewById(R.id.category_icon);
            TextView tvName = (TextView) view.findViewById(R.id.category_text);
            TextView tvConunt = (TextView) view
                    .findViewById(R.id.category_count_tv);
            CategoryItem item = mItems.get(position);
            imageView.setImageResource(item.iconId);
            tvName.setText(mItems.get(position).textStringId);

            String countStr = "";
            long count = item.count;
            if (count <= 1) {
                countStr = count + " " + mActivity.getString(R.string.child_item_count).toString().replace("s", "");
            } else {
                countStr = count + " " + mActivity.getString(R.string.child_item_count);
            }
            tvConunt.setText(countStr);
            view.setTag(item);
            return view;
        }
    }

    public void setStorageDeviceInfo() {
        //*/ freeme.liuhaoran , 20160719 , fragment not attached to activity
        if (isAdded()) {
        //*/
        final SDCardInfo sdCardInfo = Util.getSDCardInfo();
        // if (sdCardInfo != null && sdCardInfo.total >
        // Util.USBOTG_DEFAULT_SIZE) {
        Log.i("liuhaoran1", "sdCardInfo=" + sdCardInfo);
        if (sdCardInfo != null && sdCardInfo.total != sdCardInfo.free) {

            sdCardinfoView.setText(Util.convertStorage(sdCardInfo.total
                    - sdCardInfo.free)
                    + "/" + Util.convertStorage(sdCardInfo.total));
            sdCardinfoView.setTextScaleX(0.9f);
            sdPercentView.setVisibility(View.GONE);
            sdCardinfoView.setTextColor(mActivity.getResources().getColor(
                    R.color.textColor));
            sdCardView.setTextColor(mActivity.getResources().getColor(
                    R.color.textColor));
            sdCardView.setText(mActivity.getResources().getString(
                    R.string.sd_info_storage));
//          memoryCardView.setText(mActivity.getResources().getString(
//                  R.string.interior_info_storage));
            noSdCard = false;
            sdCardUsedl = sdCardInfo.total - sdCardInfo.free;
            sdCardss = sdCardInfo.total;
            sdCards = Util.convertStorage(sdCardss);
            sdCardFree = sdCardInfo.free;
            sdCardFrees = Util.convertStorage(sdCardFree);
            if ((sdCardInfo.total - sdCardInfo.free) > 100000) {
                sdCardUsed = (sdCardInfo.total - sdCardInfo.free) / 100000;
            }
            if (sdCardInfo.total > 100000) {
                sdCard = sdCardInfo.total / 100000;
            }
            if (progress > sdCardUsed) {
                progress = 0;
            }

        } else {
            noSdCard = true;
            sdCardinfoView.setText(mActivity.getResources().getString(
                    R.string.enable_sd_card));
            sdCardView.setText(mActivity.getResources().getString(
                    R.string.sd_info_storage));
            sdPercentView.setTextSize(getResources().getDimension(R.dimen.sd_percent_textsize));
            sdPercentView.setVisibility(View.VISIBLE);
            sdCardUsed = 0;
            sdCard = 0;
            sdPercentView.setTextColor(mActivity.getResources().getColor(
                    R.color.notextColor));
            sdCardinfoView.setTextColor(mActivity.getResources().getColor(
                    R.color.notextColor));
            sdCardView.setTextColor(mActivity.getResources().getColor(
                    R.color.notextColor));
        }
        // modify by droi heqianqian for NullPointerException
        // if (sdCardInfo != null) {
        if (noSdCard) {
            sdCard = 0;
        }
        Log.i("xueweili", "sdCard = " + sdCard + " sdCards = " + sdCards
                + " sdCardFrees = " + sdCardFrees);
        mRoundProgressBar2.setMax(sdCard, sdCardss, sdCardFree,
                GlobalConsts.IS_SD_CARD, false);
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (progress <= sdCardUsed) {
                    progress += 5;

                    mRoundProgressBar2.setProgress(progress);
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();
        // }

        mRoundProgressBar2.invalidate();

        memoryCardInfo = Util.getMemoryCardInfo();
        if (memoryCardInfo != null) {
            memoryPercentView.setVisibility(View.GONE);
            
             //*/ add by droi liuhaoran for RAM fake on 20160531
            if(FileManagerApplication.mMemoryCardInfo != 0){
                
            //*/ freeme.liuhaoran , 20160805 , ROM fake
            if (FileManagerApplication.mIsFeiMa.equals("true")) {
                if(com.freeme.filemanager.util.featureoption.FeatureOption.TYD_TOOL_TEST_ROM_16G ||com.freeme.filemanager.util.featureoption.FeatureOption.TYD_TOOL_TEST_ROM_32G || 
                        com.freeme.filemanager.util.featureoption.FeatureOption.TYD_TOOL_TEST_ROM_64G || com.freeme.filemanager.util.featureoption.FeatureOption.TYD_TOOL_TEST_ROM_128G){
                    memoryCardinfoView.setText(Util.convertStorage( memoryCardInfo.total - memoryCardInfo.free) + "/" + FeatureOption.FAKE_ROM_SIZE + " GB");
                    memoryCard = (Long.parseLong(FeatureOption.FAKE_ROM_SIZE) * 1024 * 1024 * 1024 )/ 100000;
                    Log.i("FeatureOption", "fake");
                }else{
                    memoryCardinfoView.setText(Util.convertStorage(memoryCardInfo.total- memoryCardInfo.free)+ "/" + Util.convertStorage(memoryCardInfo.total));
                    memoryCard = memoryCardInfo.total / 100000;
                    Log.i("FeatureOption", "real ");
                }
            }else {
                memoryCardinfoView.setText(Util.convertStorage( memoryCardInfo.total - memoryCardInfo.free) + "/" + FileManagerApplication.mMemoryCardInfo + " GB");
                memoryCard = (FileManagerApplication.mMemoryCardInfo * 1024 * 1024 * 1024 )/ 100000;
            }
            //*/
            //*/
            }else {
            memoryCardinfoView.setText(Util.convertStorage(memoryCardInfo.total
                    - memoryCardInfo.free)
                    + "/" + Util.convertStorage(memoryCardInfo.total));
            memoryCard = memoryCardInfo.total / 100000;
            }
            memoryCardView.setText(mActivity.getResources().getString(
                    R.string.interior_info_storage));
            memoryCardss = memoryCardInfo.total;
            memoryCards = Util.convertStorage(memoryCardss);
            Log.i("liuhaoran", "memoryCards = " + memoryCards);
            memoryFree = memoryCardInfo.free;
            memoryFrees = Util.convertStorage(memoryFree);
            memoryCardUsed = (memoryCardInfo.total - memoryCardInfo.free) / 100000;
            memoryCardUsedl = memoryCardInfo.total - memoryCardInfo.free;
            memoryCardinfoView.setTextScaleX(0.9f);
            memoryCardinfoView.setTextColor(mActivity.getResources().getColor(
                    R.color.textColor));
            memoryCardView.setTextColor(mActivity.getResources().getColor(
                    R.color.textColor));
            if (progress1 > memoryCardUsed) {
                progress1 = 0;
            }
        } else {
            sdCardView.setText(mActivity.getResources().getString(
                    R.string.sd_info_storage));
            memoryCardView.setText(mActivity.getResources().getString(
                    R.string.interior_info_storage));
            memoryCardinfoView.setTextColor(mActivity.getResources().getColor(
                    R.color.notextColor));
            memoryCardView.setTextColor(mActivity.getResources().getColor(
                    R.color.notextColor));
            memoryPercentView.setTextColor(mActivity.getResources().getColor(
                    R.color.notextColor));
            memoryPercentView.setVisibility(View.VISIBLE);
        }

        Log.i(LOG_TAG, "sdCard = " + sdCard + " sdCards = " + sdCards
                + " sdCardFrees = " + sdCardFrees);
        mRoundProgressBar3.setMax(memoryCard, memoryCardss, memoryFree,
                GlobalConsts.IS_MEMORY_CARD, false);
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (progress1 <= memoryCardUsed) {
                    progress1 += 5;
                    mRoundProgressBar3.setProgress(progress1);
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();

        mRoundProgressBar3.invalidate();
    }
}

    public void refreshCategoryInfo() {
        //*/ freeme.liuhaoran , 20161008 , for permission
        int writeExtStorage = ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (writeExtStorage != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //*/
            mRefreshCategoryInfoTask = new AsyncTask<Void, Void, Void>() {

                protected void onPostExecute(Void paramVoid) {
                    onDataChanged();
                }

                @Override
                protected Void doInBackground(Void... params) {
                    mFileCagetoryHelper.refreshCategoryInfo(
                            GlobalConsts.IS_CATEGORY_FRAGMENT, false);
                    mFavoriteList.initList();
                    return null;
                }
            };
            mRefreshCategoryInfoTask.execute();
        }

    private void setCategoryInfo() {
        StorageHelper.MountedStorageInfo mountedStorageInfo = StorageHelper
                .getInstance(mActivity).getMountedStorageInfo();
        setStorageDeviceInfo();
        if (mountedStorageInfo != null) {
            // this.mCategoryBar.setFullValue(mountedStorageInfo.total);
            long size = 0;
            if (FileCategoryHelper.sCategories != null) {
                for (CategoryItem fc : mCategoryItems) {
                    if (fc.category.equals(FileCategory.Favorite)) {
                        fc.count = mFavoriteList.getCount();
                        continue;
                    }
                    CategoryInfo categoryInfo = mFileCagetoryHelper
                            .getCategoryInfos().get(fc.category);
                    fc.count = categoryInfo.count;
                }
                mCategoryAdapter.notifyDataSetChanged();

            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        CategoryItem selectItem = mCategoryItems.get(position);
        FileCategoryFragment fragment = new FileCategoryFragment();
        FragmentManager fm = getFragmentManager();
        Bundle bundle = new Bundle();
        bundle.putSerializable(CATEGORY_TAG, selectItem.category);
        fragment.setArguments(bundle);
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(BACKSTACK_TAG);
        ft.commit();
    }

    @Override
    public void onFavoriteDatabaseChanged() {

    }
    
    //*/ modify by freemeos.liuhaoran on 20160707 for not click mRoundProgressBar2 after upload sd
    @Override
    public void onClick(View arg0) {
        // TODO Auto-generated method stub
        Intent info = new Intent(mActivity, MoneyInfoActivity.class);
        info.putExtra("sdMoneryCard", sdCard);
        info.putExtra("sdMoneryCardused", sdCardUsed);
        info.putExtra("sdMoneryCards", sdCardss);
        info.putExtra("sdMoneryFrees", sdCardFree);
        info.putExtra("sdMoneryCardusedl", sdCardUsedl);
        info.putExtra("isCard", GlobalConsts.IS_SD_CARD);
        startActivity(info);
    }
    
    //*/
}
