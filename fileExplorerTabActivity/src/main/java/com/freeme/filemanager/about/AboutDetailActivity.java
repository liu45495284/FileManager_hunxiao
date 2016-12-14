package com.freeme.filemanager.about;

import java.io.IOException;
import java.io.InputStream;

import com.freeme.filemanager.R;
import com.umeng.analytics.MobclickAgent;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

public class AboutDetailActivity extends Activity{
    
    private TextView freemeabout_channel_click_tv;
    private TextView freemeabout_customer_click_tv;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_detail_layout);
        freemeabout_channel_click_tv=(TextView)findViewById(R.id.freemeabout_channel_click_tv);
        freemeabout_customer_click_tv=(TextView)findViewById(R.id.freemeabout_customer_click_tv);
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(getString(R.string.freemeabout_detail_msg));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        freemeabout_channel_click_tv.setText(readAssetsFileString("cp"));
        freemeabout_customer_click_tv.setText(readAssetsFileString("td"));
    }

    //read assets cp and tp value
    public String readAssetsFileString(String Filename) {
        String str = null;
        try {
        InputStream is = getAssets().open(Filename);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        str = new String(buffer);
        } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        }
        return str;
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
