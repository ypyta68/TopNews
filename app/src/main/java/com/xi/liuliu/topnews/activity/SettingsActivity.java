package com.xi.liuliu.topnews.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xi.liuliu.topnews.R;
import com.xi.liuliu.topnews.constants.Constants;
import com.xi.liuliu.topnews.dialog.ClearCacheDialog;
import com.xi.liuliu.topnews.dialog.LogoutDialog;
import com.xi.liuliu.topnews.dialog.NotWifiWarnDialog;
import com.xi.liuliu.topnews.event.ClearCacheEvent;
import com.xi.liuliu.topnews.event.NotWifiWarnEvent;
import com.xi.liuliu.topnews.utils.AppUtil;
import com.xi.liuliu.topnews.utils.FileUtils;
import com.xi.liuliu.topnews.utils.SharedPrefUtil;
import com.xi.liuliu.topnews.utils.ToastUtil;

import de.greenrobot.event.EventBus;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {
    private RelativeLayout mGoBack;
    private RelativeLayout mEditUserInfo;
    private RelativeLayout mClearCache;
    private RelativeLayout mCheckVersion;
    private RelativeLayout mVideoNoticeNoWifi;
    private TextView mLogout;
    private TextView mUserAgreement;
    private TextView mCacheSize;
    private TextView mNotWifiWarnTag;
    private NotWifiWarnDialog mNotWifiWarnDialog;
    private TextView mVersion;
    private String mCurrentVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mGoBack = (RelativeLayout) findViewById(R.id.settings_left_back_icon);
        mGoBack.setOnClickListener(this);
        mEditUserInfo = (RelativeLayout) findViewById(R.id.edit_personal_info_rl);
        mEditUserInfo.setOnClickListener(this);
        mClearCache = (RelativeLayout) findViewById(R.id.clear_cache_rl);
        mClearCache.setOnClickListener(this);
        mCacheSize = (TextView) findViewById(R.id.cache_size);
        mCheckVersion = (RelativeLayout) findViewById(R.id.check_version_rl);
        mCheckVersion.setOnClickListener(this);
        mVideoNoticeNoWifi = (RelativeLayout) findViewById(R.id.video_notice_no_wifi);
        mVideoNoticeNoWifi.setOnClickListener(this);
        mNotWifiWarnTag = (TextView) findViewById(R.id.not_wifi_warn_tag);
        mLogout = (TextView) findViewById(R.id.settings_log_out);
        mLogout.setOnClickListener(this);
        mUserAgreement = (TextView) findViewById(R.id.user_agreement_activity_settings);
        mUserAgreement.setOnClickListener(this);
        mVersion = (TextView) findViewById(R.id.version_settings);
        //判断是否登录，显示"编辑资料"、"退出登录"
        boolean isLoggedIn = SharedPrefUtil.getInstance(this).getBoolean(Constants.LOGIN_SP_KEY);
        if (isLoggedIn) {
            mLogout.setVisibility(View.VISIBLE);
            mEditUserInfo.setVisibility(View.VISIBLE);
        } else {
            mLogout.setVisibility(View.GONE);
            mEditUserInfo.setVisibility(View.GONE);
        }
        //播放提醒
        int notWifiWarnTag = SharedPrefUtil.getInstance(this).getInt(Constants.NOT_WIFI_WARN_SP_KEY);
        setNotWifiWarnTag(notWifiWarnTag);
        //检查版本
        mCurrentVersion = AppUtil.getVersionCode(this) + "." + AppUtil.getVersionName(this);
        mVersion.setText(mCurrentVersion);
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        calculateCacheSize();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.settings_left_back_icon:
                finish();
                break;
            case R.id.settings_log_out:
                new LogoutDialog(this).show();
                break;
            case R.id.user_agreement_activity_settings:
                showUserAgreement();
                break;
            case R.id.edit_personal_info_rl:
                Intent intent = new Intent(this, UserInfoActivity.class);
                startActivity(intent);
                break;
            case R.id.clear_cache_rl:
                //缓存为0或者正在计算缓存时，不弹出dialog
                if (!mCacheSize.getText().toString().equals("0.0MB") && !mCacheSize.getText().toString().equals("正在计算...")) {
                    ClearCacheDialog clearCacheDialog = new ClearCacheDialog(this);
                    clearCacheDialog.setClearCacheListener(new ClearCacheListener() {
                        @Override
                        public void onClearCacheFinished() {
                            //ClearCacheListener.onClearCacheFinished运行在子线程中，不能在子线程中进行UI操作，引发crash
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ToastUtil.toastInCenter(SettingsActivity.this, R.string.clear_cache_toast_success);
                                    //MineFragment接收，更新顶部用户头像
                                    EventBus.getDefault().post(new ClearCacheEvent());
                                    mCacheSize.setText("0.0MB");
                                }
                            });
                        }
                    });
                    clearCacheDialog.show();
                }
                break;
            case R.id.check_version_rl:
                //向后端发请求，判断是否是最新版本
                checkVersion();
                break;
            case R.id.video_notice_no_wifi:
                if (mNotWifiWarnDialog == null) {
                    mNotWifiWarnDialog = new NotWifiWarnDialog(this);
                }
                mNotWifiWarnDialog.show();
                break;

        }
    }

    private void showUserAgreement() {
        Intent userAgreeIntent = new Intent(this, UserAgreementActivity.class);
        startActivity(userAgreeIntent);
    }

    private void checkVersion() {
        //newestVersion从服务端获取
        String newestVersion = "1.1.0";
        if (newestVersion.equals(mCurrentVersion)) {
            ToastUtil.toastInCenter(this, R.string.check_version_toast_newest);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    public void onEventMainThread(NotWifiWarnEvent event) {
        if (event != null) {
            int tag = event.getNotWifiWarnTag();
            SharedPrefUtil.getInstance(this).putInt(Constants.NOT_WIFI_WARN_SP_KEY, tag);
            setNotWifiWarnTag(tag);
        }
    }

    private void setNotWifiWarnTag(int tag) {
        if (tag == NotWifiWarnDialog.NOT_WIFI_WARN_EVERY_TIME) {
            mNotWifiWarnTag.setText(R.string.not_wifi_dialog_warn_every_time);
        } else if (tag == NotWifiWarnDialog.NOT_WIFI_WARN_ONCE) {
            mNotWifiWarnTag.setText(R.string.not_wifi_dialog_warn_once);
        }
    }

    public void calculateCacheSize() {
        CacheSizeCalculateListener listener = new CacheSizeCalculateListener() {
            @Override
            public void onCalculateFinished(long cacheSize) {
                String size = FileUtils.formatFileSize(cacheSize, FileUtils.SIZETYPE_MB) + "MB";
                mCacheSize.setText(size);
            }
        };
        CaculateCacheSizeRunnable runnable = new CaculateCacheSizeRunnable(listener);
        Thread thread = new Thread(runnable);
        thread.start();
    }

    static class CaculateCacheSizeRunnable implements Runnable {
        CacheSizeCalculateListener listener;

        public CaculateCacheSizeRunnable(CacheSizeCalculateListener listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            long cacheSize = 0;
            try {
                cacheSize = FileUtils.getCacheSize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            listener.onCalculateFinished(cacheSize);
        }
    }

    interface CacheSizeCalculateListener {
        void onCalculateFinished(long cacheSize);
    }

    public static class ClearCacheRunnable implements Runnable {
        ClearCacheListener listener;

        public ClearCacheRunnable(ClearCacheListener listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            FileUtils.clearCache();
            listener.onClearCacheFinished();
        }
    }

    public interface ClearCacheListener {
        void onClearCacheFinished();
    }
}
