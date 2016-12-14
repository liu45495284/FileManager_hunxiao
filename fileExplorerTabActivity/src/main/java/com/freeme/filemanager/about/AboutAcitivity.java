package com.freeme.filemanager.about;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.freeme.filemanager.R;
import com.umeng.analytics.MobclickAgent;

public class AboutAcitivity extends Activity implements OnClickListener{

    /**
     * @author heqianqian
     *
     */
    private ImageView freemeabout_app_icon_iv;
    private PackageInfo mPackageInfo;
    private TextView freemeabout_app_name_tv;
    private TextView freemeabout_versonname_tv;
    private String versionNameandCode;
    private int appicon;
    private String appname;
    private int mInfoCountDown=15;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_layout);
        freemeabout_app_icon_iv=(ImageView)findViewById(R.id.freemeabout_app_icon_iv);
        freemeabout_app_name_tv=(TextView)findViewById(R.id.freemeabout_app_name_tv);
        freemeabout_versonname_tv=(TextView)findViewById(R.id.freemeabout_versonname_tv);
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(getString(R.string.freemeabout));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        try {
            mPackageInfo=getPackageManager().getPackageInfo(getPackageName(), 0);
            appicon=mPackageInfo.applicationInfo.icon;
            freemeabout_app_icon_iv.setBackgroundResource(appicon);
            freemeabout_app_icon_iv.setOnClickListener(this);
            appname=mPackageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
            freemeabout_app_name_tv.setText(appname);
            versionNameandCode="V"+mPackageInfo.versionName;
            freemeabout_versonname_tv.setText(versionNameandCode);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        int i = view.getId();
        if (i == R.id.freemeabout_app_icon_iv) {
            if (mInfoCountDown > 0) {
                mInfoCountDown--;
                if(mInfoCountDown<=6) {
                    AboutToast.show(this, getString(R.string.freemeabout_click_times) + mInfoCountDown + getString(R.string.freemeabout_click_intent), Toast.LENGTH_SHORT);
                }
            } else if (mInfoCountDown == 0) {
                Intent detailIntent = new Intent();
                detailIntent.setClassName(getApplicationContext(), "com.freeme.filemanager.about.AboutDetailActivity");
                startActivity(detailIntent);
                mInfoCountDown = 15;
            }
        } else {
        }

    }
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // TODO Auto-generated method stub
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }
    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
