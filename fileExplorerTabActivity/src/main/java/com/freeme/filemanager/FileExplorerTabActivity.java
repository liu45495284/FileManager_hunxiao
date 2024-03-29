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
package com.freeme.filemanager;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.widget.TextView;
import android.content.pm.PackageManager;

import com.freeme.filemanager.R;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.IsFreemeOs;
import com.freeme.filemanager.util.PullParseXML;
import com.freeme.filemanager.util.StorageHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.FileCategoryContainerFragment;
import com.freeme.filemanager.view.FileViewFragment;
import com.freeme.filemanager.view.SearchActivity;
import com.freeme.filemanager.view.ServerControlFragment;
import com.freeme.filemanager.view.garbage.AsyncGarbageCleanupHelper;
import com.freeme.filemanager.view.garbage.AsyncGarbageCleanupHelper.GarbageCleanupStatesListener;
import com.freeme.filemanager.view.garbage.AsyncGarbageCleanupHelper.GarbageItem;
import com.freeme.filemanager.view.garbage.GarbageCleanupActivity;
import com.freeme.filemanager.bean.General_config;
import com.freeme.filemanager.controller.FTPServerService;
import com.freeme.filemanager.controller.IActionModeCtr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import android.Manifest;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.provider.SyncStateContract.Constants;
import android.content.SharedPreferences;

import com.mediatek.hotknot.*;
import android.app.Activity;
import android.widget.ActivityChooserView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.freeme.filemanager.util.FeatureOption;
import com.freeme.filemanager.util.Util.MemoryCardInfo;
import com.freeme.updateself.update.UpdateMonitor;
import com.umeng.analytics.MobclickAgent;


import android.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import com.umeng.analytics.MobclickAgent;
import com.umeng.analytics.MobclickAgent.EScenarioType;
import com.umeng.analytics.MobclickAgent.UMAnalyticsConfig;


public class FileExplorerTabActivity extends BaseActivity implements IActionModeCtr{
   

    
    private static final String TAG = "FileExplorerTabActivity";
    private static final String INSTANCESTATE_TAB = "tab";
    private static final int DEFAULT_OFFSCREEN_PAGES = 2;
    private static final int OPERATION_MENU_CLEAN = 20;
    public ViewPager mViewPager;
    TabsAdapter mTabsAdapter;
    ActionMode mActionMode;
    private RadioGroup mTabHost;
    private static RadioButton mTabBtnOne;
    private static RadioButton mTabBtnTwo;
    private static RadioButton mTabBtnThree;
    private boolean isSearch=false;
    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor editor;
    private Menu mMenu = null;
    private static final int SCOPE_HOURS = 24 * 60 * 60 * 1000;
    private static final int PERMISSION_REQUEST_CODE_RECORDING = 100;
    private Context mContext = null;
     
    //*/ add by droi liuhaoran for add the Notification of clean on 20160413
    NotificationCompat.Builder mBuilder;
    public ButtonBroadcastReceiver bReceiver;
    public final static String ACTION_BUTTON = "com.notifications.intent.action.ButtonClick";
    //*/ 


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //*/ add by xueweili for after Switch the language, file manager  stop running on 20160518
        if(savedInstanceState != null){
           savedInstanceState.remove("android:fragments");
        }
        //*/
        Log.i("liuhaoran" , "oncreate");
        super.onCreate(savedInstanceState);

        mContext = this;

        MobclickAgent.setDebugMode(true);
        MobclickAgent.openActivityDurationTrack(false);
        MobclickAgent.startWithConfigure(new UMAnalyticsConfig(mContext, "57dfa18e67e58e7d2b003625","hwdroi", EScenarioType.E_UM_ANALYTICS_OEM,false));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        StorageHelper.getInstance(this).setCurrentMountPoint(Environment.getExternalStorageDirectory().getPath());
        setContentView(R.layout.fragment_pager);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(DEFAULT_OFFSCREEN_PAGES);

        mTabsAdapter = new TabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(null, FileCategoryContainerFragment.class, null);
        mTabsAdapter.addTab(null, FileViewFragment.class, null);
        mTabsAdapter.addTab(null, ServerControlFragment.class, null);
        mViewPager.setAdapter(mTabsAdapter);
      //*/ modified by tyd wulianghuan 2013-07-15 for: make the second tab be selected when usbStorge mounted
      //*/add by droi mingjun for updateself on 20151221
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = mSharedPref.edit();
       
        
       //*/end
        mTabHost = (RadioGroup) findViewById(R.id.home_group);
        mTabBtnOne = (RadioButton) findViewById(R.id.home_radio_one);
        mTabBtnTwo = (RadioButton) findViewById(R.id.home_radio_two);
        mTabBtnThree = (RadioButton) findViewById(R.id.home_radio_three);
        
        if(FileManagerApplication.mIsHideFTP.equals("false")){
            mTabBtnThree.setVisibility(View.GONE);
        }
        
        mTabHost.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup arg0, int arg1) {
                // TODO Auto-generated method stub
                switch (arg0.getCheckedRadioButtonId()) {
                case R.id.home_radio_one:
                    Log.i("home_radio_one", "home_radio_one");
                    // hometype = 1;
                    if((FileViewFragment)mTabsAdapter.getItem(1)!=null&&((FileViewFragment)mTabsAdapter.getItem(1)).mFileViewInteractionHub!=null){
                    ((FileViewFragment)mTabsAdapter.getItem(1)).mFileViewInteractionHub.exitActionMode();
                    //((FileCategoryContainerFragment)mTabsAdapter.getItem(0)).setStorageDeviceInfo();
                    }
                    mViewPager.setCurrentItem(0, false);
                    break;

                case R.id.home_radio_two:
                    // hometype = 2;
                    if((FileCategoryContainerFragment)mTabsAdapter.getItem(0)!=null&&((FileCategoryContainerFragment)mTabsAdapter.getItem(0)).mFileViewInteractionHub!=null){

                    ((FileCategoryContainerFragment)mTabsAdapter.getItem(0)).mFileViewInteractionHub.exitActionMode();
                    }
                    mViewPager.setCurrentItem(1, false);
                    break;

                case R.id.home_radio_three:
                    // hometype = 3;
                    if((FileViewFragment)mTabsAdapter.getItem(1)!=null&&((FileViewFragment)mTabsAdapter.getItem(1)).mFileViewInteractionHub!=null){
                    ((FileViewFragment)mTabsAdapter.getItem(1)).mFileViewInteractionHub.exitActionMode();
                    }
                    mViewPager.setCurrentItem(2, false);
                    break;
                default:
                    break;
                }
            }
        });
        int tabindex = getIntent().getIntExtra("TAB", Util.CATEGORY_TAB_INDEX);
        if(tabindex != 2)
        {
        int index = getIntent().getIntExtra("tab_index", Util.CATEGORY_TAB_INDEX);
        }
        //*/ modify end


        initButtonReceiver();

        
        UpdateMonitor.Builder
        //*/ init UpdateMonitor
        .getInstance(this)
        //*/ register you Application to obsever
        .registerApplication(getApplication())
        //*/ register you Application is Service or hasEnrtyActivity
        .setApplicationIsServices(true)
        //*/ default notify small icon, ifnot set use updateself_ic_notify_small
        .setDefaultNotifyIcon(R.drawable.updateself_ic_notify_small)
        .complete();

        checkSecurityPermissions();
//        requestPermissionsMonery();
    }
    public static boolean isWifi(Context context) {  
        ConnectivityManager connectivityManager = (ConnectivityManager) context  
                .getSystemService(Context.CONNECTIVITY_SERVICE);  
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();  
        if (activeNetInfo != null  
                && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {  
            return true;  
        }  
        return false;  
    }   
    //*/ add by droi liuhaoran for add the Notification of clean on 20160413
    public void showButtonNotify() {
        mBuilder = new Builder(this);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.view_custom_button);
        if(IsFreemeOs.isFreemeOs()){
            Bitmap bitMap = getApplicationContext().getPackageManager().getIconBitmapWithThemeBg(getResources().getDrawable(R.drawable.notification_clean), getApplicationContext().getPackageName(), 0);
            remoteViews.setImageViewBitmap(R.id.filemanager_notification_clean, bitMap);
            }else {
            remoteViews.setImageViewResource(R.id.filemanager_notification_clean, R.drawable.notification_clean);
            }
        remoteViews.setTextViewText(R.id.tv_custom_title, getString(R.string.fileManager));
        remoteViews.setTextViewText(R.id.tv_custom_text, getString(R.string.clean_text));
        
        Intent buttonIntent = new Intent(ACTION_BUTTON);
        buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_CLEAN_ID);
        PendingIntent intent_clean = PendingIntent.getBroadcast(this, 1, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.btn_custom, intent_clean);

        mBuilder.setContent(remoteViews).setContentIntent(getDefalutIntent(Notification.FLAG_AUTO_CANCEL))
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setSmallIcon(R.drawable.notification_small_clean);
        notify = mBuilder.build();
        notify.flags = Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(200, notify);
    }

 
    
    public void initButtonReceiver() {
        bReceiver = new ButtonBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_BUTTON);
        registerReceiver(bReceiver, intentFilter);
    }

    public final static String INTENT_BUTTONID_TAG = "ButtonId";

    public final static int BUTTON_CLEAN_ID = 1;

    public class ButtonBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_BUTTON)) {
                
                collapseStatusBar(context);
                
                Intent resultIntent = new Intent(FileExplorerTabActivity.this, GarbageCleanupActivity.class);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(resultIntent);
                
                mNotificationManager.cancel(200);
            }
           
        }
    }
    
    public static void collapseStatusBar(Context context) {
        try {
            Object statusBarManager = context.getSystemService("statusbar");
            Method collapse;

            if (Build.VERSION.SDK_INT <= 16) {
                collapse = statusBarManager.getClass().getMethod("collapse");
            } else {
                collapse = statusBarManager.getClass().getMethod("collapsePanels");
            }
            collapse.invoke(statusBarManager);
        } catch (Exception localException) {
            localException.printStackTrace();
        }
    }
    //*/
    
    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        Log.i("liuhaoran", "onStart");
    }
    
    @Override
    protected void onRestart() {
        // TODO Auto-generated method stub
        super.onRestart();
        Log.i("liuhaoran", "onRestart");
    }
   
    
    //*/ Added by Tyd Linguanrong for [tyd00520064] refresh option menu, 2014-5-8
    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        WindowManager wm = this.getWindowManager();
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();
        Log.i("liuhaoran", "width = " + width + "height = " + height);
        Log.i("liuhaoran", "onResume");
        invalidateOptionsMenu();
        isSearch = getIntent().getBooleanExtra("isSearch", false);
        if(getIntent()!=null&&isSearch){
            isSearch =false;
            mViewPager.setCurrentItem(1);
            getIntent().putExtra("isSearch", false);
        }
        
      //*/ Added by Droi Kimi Wu on 20160413 [begin] for scanning garbage items size
         scanGarbageItems();
        //*/
         
    }
    //*/
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(mViewPager.getCurrentItem()!=1){
        menu.add(0, GlobalConsts.MENU_SEARCH, 1, getResources().getString(R.string.menu_item_search))
        .setIcon(R.drawable.ic_menu_searchs)
       .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        if(!FeatureOption.CLEAN_BUTTON_SUPPORT){
        menu.add(0, OPERATION_MENU_CLEAN, 1, getResources().getString(R.string.garbage_clean_title))
                .setIcon(R.drawable.ic_menu_clean)
               .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case OPERATION_MENU_CLEAN:
            Intent intent = new Intent(FileExplorerTabActivity.this, GarbageCleanupActivity.class);
            startActivity(intent);
            break;
        case GlobalConsts.MENU_SEARCH:
            Intent intent1 = new Intent(FileExplorerTabActivity.this, SearchActivity.class);
            startActivity(intent1);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent paramIntent) {
        // TODO Auto-generated method stub
        super.onNewIntent(paramIntent);
        setIntent(paramIntent);
        Uri pathUri = paramIntent.getData();
        if ((pathUri != null) && (!TextUtils.isEmpty(pathUri.getPath()))) {
            ((FileViewFragment) this.mTabsAdapter.getItem(1)).setPath(pathUri.getPath());
            return;
        }
         
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        Log.i("liuhaoran", "onStop");
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.i("liuhaoran", "onPause");
        MobclickAgent.onResume(this);
        hotKnotDismissDialog();
    }
    

    //add by mingjun for memory leak
    @Override
     public void onDestroy() {
         super.onDestroy();
         //*/ modify by freeme.liuhaoran on 20160621for start filemanager and then insert sim,filemanager to flash exit 
         if(FTPServerService.isRunning()){
         //*/
        android.os.Process.killProcess(android.os.Process.myPid());
         }
         mNotificationManager.cancelAll();
         if (bReceiver != null) {
             unregisterReceiver(bReceiver);
         }
     }
      //end
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG, "onConfigurationChanged ---- ");
        if (getActionBar().getSelectedNavigationIndex() == Util.CATEGORY_TAB_INDEX) {
            FileCategoryContainerFragment categoryFragement =(FileCategoryContainerFragment) mTabsAdapter.getItem(Util.CATEGORY_TAB_INDEX);
            if (categoryFragement.isHomePage()) {
                reInstantiateCategoryTab();
            } else {
                categoryFragement.setConfigurationChanged(true);
            }
        }
        super.onConfigurationChanged(newConfig);
    }

    public void reInstantiateCategoryTab() {
        mTabsAdapter.destroyItem(mViewPager, Util.CATEGORY_TAB_INDEX,
                mTabsAdapter.getItem(Util.CATEGORY_TAB_INDEX));
        mTabsAdapter.instantiateItem(mViewPager, Util.CATEGORY_TAB_INDEX);
    }

    @Override
    public void onBackPressed() {
        IBackPressedListener backPressedListener = (IBackPressedListener) mTabsAdapter
                .getItem(mViewPager.getCurrentItem());
        if (!backPressedListener.onBack()) {
            MobclickAgent.onKillProcess(mContext);
            //*/ add by droi liuhaoran for the function of onBack to background  on 20160406
            moveTaskToBack(true); 
            //*/
            //finish();
            //end
            //super.onBackPressed();
        }
    }
    

    
    public interface IBackPressedListener {
        boolean onBack();
    }

    public void setActionMode(ActionMode actionMode) {
        mActionMode = actionMode;
    }

    public ActionMode getActionMode() {
        return mActionMode;
    }

    public Fragment getFragment(int tabIndex) {
        return mTabsAdapter.getItem(tabIndex);
    }

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        static final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;
            private Fragment fragment;

            TabInfo(Class<?> _class, Bundle _args) {
                clss = _class;
                args = _args;
            }
        }

        public TabsAdapter(Activity activity, ViewPager pager) {
            super(activity.getFragmentManager());
            mContext = activity;
            mViewPager = pager;
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
            TabInfo info = new TabInfo(clss, args);
            mTabs.add(info);
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            if (info.fragment == null) {
                info.fragment = Fragment.instantiate(mContext, info.clss.getName(), info.args);
            }
            return info.fragment;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
//            mActionBar.setSelectedNavigationItem(position);
            switch (position) {
            case 0:
                // hometype = 1;
                mTabBtnOne.setChecked(true);
                mTabBtnTwo.setChecked(false);
                //*/ modify by droi liuhaoran for add customized configuration file on 20160428
                if(FileManagerApplication.mIsHideFTP.equals("true")){
                    mTabBtnThree.setChecked(false);
                }
                break;

            case 1:
                // hometype = 2;
                mTabBtnOne.setChecked(false);
                mTabBtnTwo.setChecked(true);
                if(FileManagerApplication.mIsHideFTP.equals("true")){
                    mTabBtnThree.setChecked(false);
                }
                break;
                //*/
            case 2:
                // hometype = 3;
                mTabBtnOne.setChecked(false);
                mTabBtnTwo.setChecked(false);
                mTabBtnThree.setChecked(true);
                break;
            default:
                break;
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            Object tag = tab.getTag();
            for (int i=0; i<mTabs.size(); i++) {
                if (mTabs.get(i) == tag) {
                    mViewPager.setCurrentItem(i,false);
                }
            }
            if(mContext instanceof  IActionModeCtr){
                ActionMode actionMode = ((IActionModeCtr) mContext).getActionMode();
                if (actionMode != null) {
                    actionMode.finish();
                }
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    }

    
    
    //added for HotKnot
    private HotKnotAdapter mHotKnotAdapter = null;
    private Uri[] mHotKnotUris = null;
    private Activity mHotContext = null;
    private boolean mHotKnotEnable = false;
    private AlertDialog mHotKnotDialog = null;
    private Toast mHotKnotToast = null;
    MenuItem mHotKnotItem = null;
    private HotknotCompleteListener mHotKnotListener = null;
    
    
    private void hotKnotInit(Activity activity) {
        Log.d(TAG, "hotKnotInit");
        mHotContext = activity;
        mHotKnotAdapter = HotKnotAdapter.getDefaultAdapter(mHotContext);
        if (mHotKnotAdapter == null) {
            mHotKnotEnable = false;
            Log.d(TAG, "hotKnotInit, mHotKnotAdapter is null, disable hotKnot feature");
            return;
        }
        mHotKnotEnable = true;
        mHotKnotAdapter.setOnHotKnotCompleteCallback(
                new HotKnotAdapter.OnHotKnotCompleteCallback() {
                    public void onHotKnotComplete(int reason) {
                        Log.d(TAG, "onHotKnotComplete reason:" + reason);
                        mHotKnotAdapter.setHotKnotBeamUris(null, mHotContext);
                        if (mHotKnotListener != null){ 
                            setHotknotCompleteListener(null);
                            mHotKnotListener.onHotKnotSendComplete();
                        }
                    }
                }, mHotContext);

        OnClickListener onClick = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (DialogInterface.BUTTON_POSITIVE == which) {
                    Log.d(TAG, "hotKnot start setting");
                    Intent intent = new Intent(
                            "mediatek.settings.HOTKNOT_SETTINGS");
                    startActivity(intent);
                    dialog.cancel();
                } else {
                    Log.d(TAG, "onClick cancel dialog");
                    dialog.cancel();
                }
            }
        };
        mHotKnotDialog = new AlertDialog.Builder(mHotContext)
                .setMessage(R.string.turn_on_hotknot)
                .setNegativeButton(android.R.string.cancel, onClick)
                .setPositiveButton(android.R.string.ok, onClick).create();
    }

    public boolean hotKnotIsEnable() {
        if (FeatureOption.MTK_HOTKNOT_SUPPORT && mHotKnotEnable) {
            return true;
        } else {
            return false;
        }
    }

    public void hotKnotSetUris(Uri[] uris) {
        Log.d(TAG, "hotKnotSetUris");
        if (uris != null) {
            for (Uri uri : uris) {
                Log.d(TAG, "HotKnot uri:" + uri);
            }
        }
        mHotKnotUris = uris;
    }

    public void hotKnotStart() {
        Log.d(TAG, "hotKnotStart");

        if (mHotKnotAdapter.isEnabled()) {
            Log.d(TAG, "hotKnotAdapter is Enable");
            mHotKnotAdapter.setHotKnotBeamUris(mHotKnotUris, mHotContext);
            if (mHotKnotToast == null) 
                mHotKnotToast = Toast.makeText(mHotContext, R.string.hotknot_toast, Toast.LENGTH_SHORT);
            if (mHotKnotUris == null) {
                mHotKnotToast.cancel();
            } else {
                mHotKnotToast.show();
            }
            return;
        }

        if (mHotKnotDialog != null) {
            Log.d(TAG, "hotKnotStart show dialog");
            mHotKnotDialog.show();

            if (mHotKnotListener != null){
                setHotknotCompleteListener(null);
                mHotKnotListener.onHotKnotSendComplete();
            }
        }
        else {
                Intent intent = new Intent("mediatek.settings.HOTKNOT_SETTINGS");
                startActivity(intent);
        }
    }

    public void hotKnotSend(Uri uri) {
        Log.d(TAG, "hotKnotSend:" + uri);
        if (uri == null) {
            hotKnotSetUris(null);
            hotKnotStart();
        } else {
            Uri uris[] = new Uri[1];
            uris[0] = uri;
            hotKnotSetUris(uris);
            hotKnotStart();
        }
    }

    public void hotKnotUpdateMenu(Menu menu, int shareAction, int hotKnotAction) {
        if (menu == null) {
            Log.d(TAG, "hotKnotUpdateMenu: menu is null");
            return;
        }
        mHotKnotItem = menu.findItem(hotKnotAction);
        MenuItem shareItem = menu.findItem(shareAction);
        boolean enable = hotKnotIsEnable();
        Log.d(TAG, "hotKnotUpdateMenu, Enable:" + enable);

        if (mHotKnotItem != null && shareItem != null) {
            mHotKnotItem.setVisible(enable);
            ((ActivityChooserView) shareItem.getActionView())
                    .setRecentButtonEnabled(!enable);
            Log.d(TAG, "hotKnotUpdateMenu, success");
        }
    }

    public void hotKnotShowIcon(boolean enable) {
        if (mHotKnotItem != null && hotKnotIsEnable()) {
            mHotKnotItem.setEnabled(enable);
            mHotKnotItem.setVisible(enable);
            Log.d(TAG, "hotKnotShowIcon:" + enable);
        }
    }
    
    public void hotKnotDismissDialog() {
        if (mHotKnotDialog != null) {
            mHotKnotDialog.dismiss();
        }
    }
    
    public static interface HotknotCompleteListener {
        public void onHotKnotSendComplete();
    }
    
    public void setHotknotCompleteListener(HotknotCompleteListener listener) {
        mHotKnotListener = listener;
        Log.d(TAG, "setHotknotCompleteListener:" + mHotKnotListener);
    }

    //add by droi heqianqian for updateself save and read on 20151221

    private void checkPermission() {
        int readExtStorage = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeExtStorage = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int mediaExtStorage = checkSelfPermission(Manifest.permission.WRITE_MEDIA_STORAGE);
        int phoneExtStorage = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
        List<String> mPermissionStrings = new ArrayList<String>();
        boolean mRequest = false;

        if (writeExtStorage != PackageManager.PERMISSION_GRANTED) {
            mPermissionStrings.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            mRequest = true;
        }
        if (mRequest == true) {
           String[] mPermissionList = new String[mPermissionStrings.size()];
            mPermissionList = mPermissionStrings.toArray(mPermissionList);
            requestPermissions(mPermissionList, PERMISSION_REQUEST_CODE_RECORDING);
            return;
        }
    }
       
     //*/ Added by Droi Kimi Wu on 20160413 [begin] for scanning garbage items size
       private static final int GARBAGE_ITEMS = 5;
       private static final long GARBAGE_SIZE_THRESHOLD = 256 * 1024 * 1024;
       private static final String MY_TAG = "DROI_DBG";
       private long mGarbageSize;
       private boolean mCleanFinished;
       private Notification notify;
       
       public void scanGarbageItems() {
           AsyncGarbageCleanupHelper cleanHelper = new AsyncGarbageCleanupHelper(FileExplorerTabActivity.this);
           mGarbageSize = 0;
           mCleanFinished = false;
           cleanHelper.setState(AsyncGarbageCleanupHelper.STATE_START_SCAN);
           
           ArrayList<Integer> list = new ArrayList<Integer>(GARBAGE_ITEMS);
           for (int i = 0; i < GARBAGE_ITEMS; i++) {
               list.add(i);
           }
           cleanHelper.setActionOperate(list);
           
           cleanHelper.setGarbageCleanupStatesListener(new AsyncGarbageCleanupHelper.GarbageCleanupStatesListener() {
               @Override
               public void onUpdateUI(int i) {
               }
               
               @Override
               public void onFinish(int i, long l, int j) {
                   mGarbageSize += l;
                   if (mCleanFinished) {
                       if (GARBAGE_SIZE_THRESHOLD <= mGarbageSize) {
                           showButtonNotify();
                       }
                   }
               }
               
               @Override
               public void onAppGarbageFinish(List<GarbageItem> list) {
                   mCleanFinished = true;
               }
           });
           
           cleanHelper.cleanUp();
       }
       //*/



    private final int CHECK_PERMISSIONS_REQUEST = 10010;

    //检查权限
    private void checkSecurityPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissionForM();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissionForM() {
        String[] PERMISSIONS = new String[]{

                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };

        String[] PERMISSIONS_ringtone = new String[]{

                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_SETTINGS
        };

        List<String> perList = new ArrayList<>();
        if (FileManagerApplication.mIsDaMi.equals("true")) {
            int length = PERMISSIONS_ringtone.length;
            for (int i = 0; i < length; i++) {
                //检查被拒绝的权限,加入授权框
                if (ActivityCompat.checkSelfPermission(this, PERMISSIONS_ringtone[i]) != PackageManager.PERMISSION_GRANTED) {
                    perList.add(PERMISSIONS_ringtone[i]);
                }
            }
        }else {
            int length = PERMISSIONS.length;
            for (int i = 0; i < length; i++) {
                if (ActivityCompat.checkSelfPermission(this, PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                    perList.add(PERMISSIONS[i]);
                }
            }
        }
        int size = perList.size();
        if (size > 0) {
            String[] permisGroup = (String[]) perList.toArray(new String[size]);
            Log.i("liuhaoran" , Log.getStackTraceString(new NullPointerException("li")));
            requestPermissions(permisGroup, CHECK_PERMISSIONS_REQUEST);

        }

    }

    //权限授权结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case CHECK_PERMISSIONS_REQUEST:
                String str = "";
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
//                        str += "\n" + permissions[i];
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                        showDialog();
                            return;
                    }
                        finish();
                    }
                }
//                if (!TextUtils.isEmpty(str)) {
////                    showDialog(str);
//                    showDialog();
//                }
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
////                // Permission Granted
//            } else {
//                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                        showDialog();
//                    }
//
//                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
//                        showDialog();
//                    }
//                    finish();
//                }
                break;
        }
    }

    //权限拒绝后,提示
    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.need_permission);
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
        builder.show().setCanceledOnTouchOutside(false);
    }

}
