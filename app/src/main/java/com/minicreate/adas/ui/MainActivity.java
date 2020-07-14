package com.minicreate.adas.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.minicreate.adas.App;
import com.minicreate.adas.ParamSettingActivity;
import com.minicreate.adas.R;
import com.minicreate.adas.transmission.TcpVideoEndpoint;
import com.minicreate.adas.transmission.WifiEndpoint;
import com.minicreate.adas.transmission.protocol.BaseParamAdas;
import com.minicreate.adas.transmission.protocol.OnResponseListener;
import com.minicreate.adas.transmission.protocol.Param;

import com.minicreate.adas.transmission.protocol.ResetDevice_0x75_0x55;
import com.minicreate.adas.transmission.protocol.VolumeAdjust_0x75_0x54;
import com.minicreate.adas.transmission.protocol.testmode_0x75_0x30;
import com.minicreate.adas.ui.dialog.DialogDataCallback;
import com.minicreate.adas.ui.dialog.VolumeAdjustDialog;
import com.minicreate.adas.utils.LogUtil;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.lang.Thread;

public class MainActivity extends Activity implements View.OnClickListener{
    private static final String TAG = "MainActivity";

    private WifiBroadcastReceiver mwifiBroadcastReceiver;
    private static TextView tv_wifi_connect_state;
    private Button mbtn_param_setting;
    private Button mbtn_test_mode;
    private Button mbtn_volume_adjust;
    private Button mbtn_video;
    private Button mbtn_reset;
    private VolumeAdjustDialog mVolumeAdjustDialog = null;
    private WifiManager mWifiManager;
    private SharedHelper sh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_wifi_connect_state = findViewById(R.id.wifi_connect_state);

        //动态注册wifi状态广播
        mwifiBroadcastReceiver = new WifiBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(mwifiBroadcastReceiver, intentFilter);

        mbtn_param_setting = (Button) findViewById(R.id.btn_param_setting);
        mbtn_test_mode = (Button) findViewById(R.id.btn_test_mode);
        mbtn_volume_adjust  = (Button) findViewById(R.id.btn_volume_adjust);
        mbtn_video = (Button) findViewById(R.id.btn_video);
        mbtn_reset = (Button) findViewById(R.id.btn_reset);
        mbtn_param_setting.setOnClickListener(this);
        mbtn_test_mode.setOnClickListener(this);
        mbtn_volume_adjust.setOnClickListener(this);
        mbtn_video.setOnClickListener(this);
        mbtn_reset.setOnClickListener(this);

        ((App.WifiThread)((App)getApplication()).getWifiThread()).start();

        //setWifiSocketConnectState(((App)getApplication()).isWifiSocketConnected());
        sh = new SharedHelper(this);
    }
    @Override
    protected void onStart() {
        super.onStart();
    }

    //Activity从后台重新回到前台时被调用
    @Override
    protected void onRestart() {
        super.onRestart();

    }

    //Activity创建或者从被覆盖、后台重新回到前台时被调用
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"Enter MainActivity=> onResume()");
    }

    //Activity被覆盖到下面或者锁屏时被调用
    @Override
    protected void onPause() {
        super.onPause();
    }

    //退出当前Activity或者跳转到新Activity时被调用
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"Enter MainActivity=> onStop()");
    }

    //退出当前Activity时被调用,调用之后Activity就结束了
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //注销广播
        unregisterReceiver(mwifiBroadcastReceiver);
        Log.d(TAG,"Enter MainActivity=> onDestroy()");

        WifiEndpoint wifiEndpoint = ((App)getApplication()).getWifiEndpoint();
        if(wifiEndpoint != null)
            wifiEndpoint.closeEndpoint();

        ActivityManager manager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        manager.killBackgroundProcesses(getPackageName());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_param_setting:
            {
                Intent intent = new Intent(this, ParamSettingActivity.class);
                startActivity(intent);
            }
                break;

            case R.id.btn_test_mode:
            {
                testmode_0x75_0x30 param = new testmode_0x75_0x30(this);
                WifiEndpoint wifiEndpoint = ((App)getApplication()).getWifiEndpoint();
                if (wifiEndpoint != null) {
                    wifiEndpoint.send(new OnResponseListener() {
                        @Override
                        public void onResponse(Param result) {
                            if (result.isTimeOut()) {
                                LogUtil.d(TAG, "进入测试模式超时");
                            } else {
                                LogUtil.d(TAG, result.toString());
                            }
                        }
                    }, param);
                } else {
                    LogUtil.e(TAG, "WIFI Socket连接断开");
                }
            }
                break;

            case R.id.btn_volume_adjust:
                volumeAdjust();
                break;

            case R.id.btn_video:
                Intent intent = new Intent(this, VideoPlayerActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_reset:
                ResetDevice_0x75_0x55 param = new ResetDevice_0x75_0x55(MainActivity.this);
                WifiEndpoint wifiEndpoint = ((App)getApplication()).getWifiEndpoint();
                if (wifiEndpoint != null) {
                    wifiEndpoint.send(new OnResponseListener() {
                        @Override
                        public void onResponse(Param result) {
                        if (result.isTimeOut()) {
                            LogUtil.d(TAG, "音量调节超时！");
                        } else {
                            LogUtil.d(TAG, result.toString());
                        }
                        }
                    }, param);
                } else {
                    LogUtil.e(TAG, "WIFI Socket连接断开");
                }
                break;
        }
    }

    public static void setWifiSocketConnectState(boolean wifisocketconnectstate){
        if(wifisocketconnectstate){
            tv_wifi_connect_state.setText("设备::已连接");
        }
        else{
            tv_wifi_connect_state.setText("设备::未连接");
        }

    }

    private  void volumeAdjust(){
        mVolumeAdjustDialog = new VolumeAdjustDialog(this, R.layout.dialog_volume, getString(R.string.volume_adjust),sh.readVolume(),
                new DialogDataCallback<String>() {
                    @Override
                    public void onClick(View v, boolean bl, String data) {
                        LogUtil.d(TAG, "onClick bl = " + bl + " ,data = " + data);
                        if (bl) {
                            VolumeAdjust_0x75_0x54 param = new VolumeAdjust_0x75_0x54(MainActivity.this);
                            param.setvolume(Integer.valueOf(data));
                            WifiEndpoint wifiEndpoint = ((App)getApplication()).getWifiEndpoint();
                            if (wifiEndpoint != null) {
                                wifiEndpoint.send(new OnResponseListener() {
                                    @Override
                                    public void onResponse(Param result) {
                                        if (result.isTimeOut()) {
                                            LogUtil.d(TAG, "音量调节超时！");
                                        } else {
                                            LogUtil.d(TAG, result.toString());
                                        }
                                    }
                                }, param);
                            } else {
                                LogUtil.e(TAG, "WIFI Socket连接断开");
                            }
                            sh.saveVolume(Integer.valueOf(data));
                        } else {
                        }
                        mVolumeAdjustDialog = null;
                    }
                });
        mVolumeAdjustDialog.show();
    }

    private class WifiBroadcastReceiver extends BroadcastReceiver
    {
        //private WifiControlUtils wifiControlUtils;
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //wifiControlUtils = new WifiControlUtils(context);

            //wifi正在改变状态
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction()))
            {
                //获取wifi状态
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLING);
                switch (wifiState)
                {
                    case WifiManager.WIFI_STATE_DISABLED:
                        //wifi已经关闭
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        //wifi正在关闭
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        //wifi已经开启
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                        //wifi正在开启
                        break;
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction()))
            {
                //网络状态改变
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (NetworkInfo.State.DISCONNECTED.equals(info.getState()))
                {
                    //wifi网络连接断开
                    //((App) getApplication()).getWifiEndpoint().restartEndpoint();
                } else if (NetworkInfo.State.CONNECTED.equals(info.getState()))
                {
                    //获取当前网络，wifi名称
                    Toast.makeText(context, "wifi已连接", Toast.LENGTH_SHORT).show();

                }
            } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(intent.getAction()))
            {
                //wifi密码错误广播
                //SupplicantState netNewState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                //错误码
                int netConnectErrorCode = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, WifiManager.ERROR_AUTHENTICATING);
            }
        }
    }

    public class SharedHelper {
        private Context mContext;

        public SharedHelper(Context mContext) {
            this.mContext = mContext;
        }

        //定义一个保存数据的方法
        public void saveVolume(int volume_value) {
            SharedPreferences sp = mContext.getSharedPreferences("adas_sp", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("last_volume_value", volume_value);
            editor.commit();
        }

        //定义一个读取SP文件的方法
        public int readVolume() {
            SharedPreferences sp = mContext.getSharedPreferences("adas_sp", Context.MODE_PRIVATE);
            return sp.getInt("last_volume_value",0);
        }
    }
}
