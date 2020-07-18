package com.minicreate.adas.ui;

import android.app.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.minicreate.adas.App;
import com.minicreate.adas.R;

import com.minicreate.adas.control.VideoDataHandler;

import com.minicreate.adas.transmission.TcpVideoEndpoint;
import com.minicreate.adas.transmission.TcpVideoReceiver;
import com.minicreate.adas.transmission.WifiEndpoint;
import com.minicreate.adas.transmission.listener.TcpConnectListenerAdapter;
import com.minicreate.adas.transmission.listener.TcpRecvListener;
import com.minicreate.adas.transmission.protocol.AdasParamCalibration_0x75_0x52;
import com.minicreate.adas.transmission.protocol.DsmParamCalibration_0x75_0x53;
import com.minicreate.adas.transmission.protocol.OnResponseListener;

import com.minicreate.adas.transmission.protocol.Param;
import com.minicreate.adas.transmission.protocol.ParamQuery_0x75_0x69;
import com.minicreate.adas.transmission.protocol.VideoStart_0x75_0x50;
import com.minicreate.adas.transmission.protocol.VideoStop_0x75_0x51;
import com.minicreate.adas.ui.widget.VView;
import com.minicreate.adas.utils.LogUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class VideoPlayerActivity extends Activity {
    private static final String TAG = "VideoPlayerActivity";
    private TcpVideoEndpoint mVideoServer;
    public int mChannel = -1;
    VView vv;
    TextView info, playTime;
    private TcpVideoReceiver tcpVideoReceiver;

    private FileInputStream inStream;
    Handler m_handler;
    private boolean mFirstPlay = true;
    private int mPlayTime = 0;
    private long startTime = 0;
    private Timer statusTimer;
    private TimerTask statusTimerTask;
    RadioButton[] radioChannel = new RadioButton[2];
    private LinearLayout ll_adas;
    private LinearLayout ll_dsm;
    private EditText edit_adas_value;
    private TextView tv_angle_value;
    private Button btn_sure;
    private TextView tv_prompt;

    private View.OnClickListener btnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int channel = 0;
            switch (v.getId()) {
                case R.id.btn_channel1:
                    channel = 1;
                    break;
                case R.id.btn_channel2:
                    channel = 2;
                    break;
            }
            switchVideoChannel(channel);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.videoplay);

        info = (TextView) findViewById(R.id.infor);
        playTime = (TextView) findViewById(R.id.play_time);
        tv_prompt = (TextView) findViewById(R.id.prompt);

        vv = (VView) findViewById(R.id.decodeview);
        radioChannel[0] = (RadioButton) findViewById(R.id.btn_channel1);
        radioChannel[1] = (RadioButton) findViewById(R.id.btn_channel2);
        radioChannel[0].setOnClickListener(btnListener);
        radioChannel[1].setOnClickListener(btnListener);



        info.setText(" 当前播放通道："/* + "  接收数据：0"*/);
        m_handler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        info.setText("" + (String) msg.obj);
                        //radioChannel[mChannel-1].setChecked(true);
                        break;
                    case 2:
                        playTime.setText("正在缓冲...");
                        break;
                    case 3:
                        mPlayTime = (Integer) msg.obj;
                        playTime.setText("播放时间：" + mPlayTime + "秒");
                        break;
                }
            }
        };
        ll_adas = (LinearLayout) findViewById(R.id.ll_adas);
        ll_dsm = (LinearLayout) findViewById(R.id.ll_dsm);
        edit_adas_value = (EditText) findViewById(R.id.edit_adas_value);
        tv_angle_value = (TextView) findViewById(R.id.tv_angle_value);
        btn_sure = (Button) findViewById(R.id.btn_sure);
        btn_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processActionItems();
            }
        });

        vv.setHandler(m_handler);
        adjustViewParam();
        Toast toast = Toast.makeText(this, "请选择通道进行播放", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
        mChannel = -1;

        IntentFilter intentFilter = new IntentFilter("DsmParamCalibration");
        registerReceiver(m_DsmParamCalibrationReceiver, intentFilter);

        queryParam();
    }

    public void queryParam(){
        ParamQuery_0x75_0x69 param = new ParamQuery_0x75_0x69(this);
        WifiEndpoint wifiEndpoint = ((App)getApplication()).getWifiEndpoint();
        if (wifiEndpoint != null) {
            wifiEndpoint.send(new OnResponseListener() {
                @Override
                public void onResponse(Param result) {
                    if (result.isTimeOut()) {
                        LogUtil.d(TAG, "参数查询超时");
                    } else {
                        LogUtil.d(TAG, result.toString());
                        String msg = ((ParamQuery_0x75_0x69) result).getValue1();
                        LogUtil.d(TAG, result.toString());
                        tv_prompt.setText(msg);
                    }
                    queryParam();
                }
            }, param);
        } else {
            LogUtil.e(TAG, "WIFI Socket连接断开");
            queryParam();
        }
    }

    private void initVideoEndpoint() {
        // 创建一个网络端点
        mVideoServer = new TcpVideoEndpoint(VideoPlayerActivity.this);
        //mVideoClient.addEndpointListener(this);
        mVideoServer.startEndpoint(new TcpConnectListenerAdapter() {
            @Override
            public void onConnectError(int code, String server, int port) {
                LogUtil.d(TAG, "onConnectError");

            }

            @Override
            public void onConnectOk() {
                LogUtil.d(TAG, "TcpVideoEndpoint onConnectOk");

            }
        });

    }
    //调整视频控件的参数，比如宽和高
    private void adjustViewParam() {
        Display display = getWindowManager().getDefaultDisplay();
        int screenHeight = display.getHeight();
        int screenWidth = display.getWidth();

        int videoWidth = vv.getVideoWidth();
        int videoHeight = vv.getVideoHeight();
        int mWidth = screenWidth;
        int mHeight = screenHeight - 25;

        if (videoWidth > 0 && videoHeight > 0) {
            if (videoWidth * mHeight > mWidth * videoHeight) {
                //Log.i("@@@", "image too tall, correcting");
                mHeight = mWidth * videoHeight / videoWidth;
            } else if (videoWidth * mHeight < mWidth * videoHeight) {
                //Log.i("@@@", "image too wide, correcting");
                mWidth = mHeight * videoWidth / videoHeight;
            } else {

            }
        }
        ///mVideoInfo.setText("VideoSize:"+videoWidth+"x"+videoHeight);
        vv.setVideoScale(mWidth, mHeight);
    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart called.");
    }

    //Activity从后台重新回到前台时被调用
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart called.");
    }

    //Activity创建或者从被覆盖、后台重新回到前台时被调用
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume called.");
        //test_videoplayer();
    }

    //Activity被覆盖到下面或者锁屏时被调用
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause called.");
        //有可能在执行完onPause或onStop后,系统资源紧张将Activity杀死,所以有必要在此保存持久数据
    }

    //退出当前Activity或者跳转到新Activity时被调用
    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop called.");

    }

    //退出当前Activity时被调用,调用之后Activity就结束了
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestory called.");
        vv.stopPlayThread();
        if(vv != null)
            vv = null;
        stopVideo(0);//cloes all channel

        unregisterReceiver(m_DsmParamCalibrationReceiver);
    }

    private void switchVideoChannel(int channel) {
        if (channel == mChannel) {
            return;
        }
        final int currentChannel = channel;

        if (startTime != 0 && System.currentTimeMillis() - startTime < 1000 * 10) {
            Toast toast = Toast.makeText(this, "请在10秒后切换其他通道", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            radioChannel[mChannel - 1].setChecked(true);
            return;
        }
        startTime = System.currentTimeMillis();
        if (!mFirstPlay && mPlayTime <= 10) {
            Toast.makeText(this, "请在播放10秒视频后，再切换通道，否则可能出现切换通道和播放视频不相同的情况", Toast.LENGTH_SHORT).show();
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                //如果是第一次播放，则不在请求停止之前的数据，减少延迟
                if (!mFirstPlay) {
                    LogUtil.d(TAG, "stopPlayThread");
                    vv.stopPlayThread();
//                    audioPlay.stopPlay();
                    stopVideo(mChannel); //last channel
                    stopStatusTimer();

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtil.d(TAG, "not mFirstPlay");
                }

                reqSendVideo(currentChannel);
                mChannel = currentChannel;
                VideoDataHandler.getInstence().clear();
                sendMessage(" 当前播放通道：" + mChannel);
                vv.startPlayThread(); //start a new thread
//                audioPlay.startPlay();
                mFirstPlay = false;
                startStatusTimer();

                //test_videoplayer();
            }
        };
        thread.start();
        updateActionItems(channel);
        Toast.makeText(getApplicationContext(), "切换到通道" + channel, Toast.LENGTH_LONG).show();
    }

    private void updateActionItems(int channel){
        LogUtil.d(TAG, "updateActionItems channel == "+channel);
        switch (channel) {
            case 1:
                ll_adas.setVisibility(View.VISIBLE);
                ll_dsm.setVisibility(View.GONE);
                edit_adas_value.setText("");
                btn_sure.setVisibility(View.VISIBLE);
                btn_sure.setText(R.string.adas_param_calibration);
                break;
            case 2:
                ll_adas.setVisibility(View.GONE);
                ll_dsm.setVisibility(View.VISIBLE);
                tv_angle_value.setText("");
                btn_sure.setVisibility(View.VISIBLE);
                btn_sure.setText(R.string.dsm_param_calibration);
                break;
        }
    }

    private void processActionItems(){
        LogUtil.d(TAG, "processActionItems channel == "+ mChannel);
        if(mChannel == 1){
            AdasParamCalibration_0x75_0x52 param = new AdasParamCalibration_0x75_0x52(this);
            param.setdistance(edit_adas_value.getText().toString());
            WifiEndpoint wifiEndpoint = ((App)getApplication()).getWifiEndpoint();
            if (wifiEndpoint != null) {
                wifiEndpoint.send(new OnResponseListener() {
                    @Override
                    public void onResponse(Param result) {
                        if (result.isTimeOut()) {
                            LogUtil.d(TAG, " Adas参数校准超时！");
                        } else {
                            LogUtil.d(TAG, result.toString());
                        }
                    }
                }, param);
            } else {
                LogUtil.e(TAG, "WIFI Socket连接断开");
            }
        }else if(mChannel == 2){
            DsmParamCalibration_0x75_0x53 param = new DsmParamCalibration_0x75_0x53(this);
            WifiEndpoint wifiEndpoint = ((App)getApplication()).getWifiEndpoint();
            if (wifiEndpoint != null) {
                wifiEndpoint.send(new OnResponseListener() {
                    @Override
                    public void onResponse(Param result) {
                        if (result.isTimeOut()) {
                            LogUtil.d(TAG, "Dsm 参数校准超时！");
                        } else {
                            LogUtil.d(TAG, result.toString());
                            tv_angle_value.setText(((DsmParamCalibration_0x75_0x53)result).getAngle());
                        }
                    }
                }, param);
            } else {
                LogUtil.e(TAG, "WIFI Socket连接断开");
            }
        }
    }

    private void startStatusTimer() {
        Log.d(TAG, "start startStatusTimer");
        statusTimer = new Timer();
        statusTimerTask = new TimerTask() {
            private int durationTime = 0;

            @Override
            public void run() {
                if (VideoDataHandler.getInstence().isBuffering()) {
                    m_handler.sendEmptyMessage(2);
                    durationTime = 0;
                } else {
                    durationTime++;
                    Message msg = m_handler.obtainMessage(3, durationTime);
                    m_handler.sendMessage(msg);
                }
            }
        };
        statusTimer.schedule(statusTimerTask, 1000, 1000);
    }

    private void stopStatusTimer() {
        try {
            statusTimer.cancel();
            statusTimerTask.cancel();
            m_handler.sendEmptyMessage(2);
        } catch (Exception e) {

        }
    }

    private void reqSendVideo(int channel) {
        VideoStart_0x75_0x50 param = new VideoStart_0x75_0x50(this);
        WifiEndpoint wifiEndpoint = ((App)getApplication()).getWifiEndpoint();
        if (wifiEndpoint != null) {
            param.setip(wifiEndpoint.getIp());
            param.setport(TcpVideoEndpoint.PORT);
            param.setchannel(channel);
            LogUtil.d(TAG, "reqSendVideo param::"+param.toString());
            wifiEndpoint.send(new OnResponseListener() {
                @Override
                public void onResponse(Param result) {
                    if (result.isTimeOut()) {
                        LogUtil.d(TAG, "视频申请超时！");
                    } else {
                        LogUtil.d(TAG, result.toString());
                    }
                }
            }, param);
        } else {
            LogUtil.e(TAG, "WIFI Socket连接断开");
        }

        initVideoEndpoint();
    }

    private void stopVideo(int channel) {
        VideoStop_0x75_0x51 param = new VideoStop_0x75_0x51(this);
        param.setchannel(channel);
        WifiEndpoint wifiEndpoint = ((App)getApplication()).getWifiEndpoint();
        if (wifiEndpoint != null) {
            wifiEndpoint.send(new OnResponseListener() {
                @Override
                public void onResponse(Param result) {
                    if (result.isTimeOut()) {
                        LogUtil.d(TAG, "视频关闭超时！");
                    } else {
                        LogUtil.d(TAG, result.toString());
                    }
                }
            }, param);
        } else {
            LogUtil.e(TAG, "WIFI Socket连接断开");
        }

        if(mVideoServer != null) {
            mVideoServer.closeEndpoint();
        }
    }

    void sendMessage(String str) {
        Message msg = m_handler.obtainMessage(1, 1, 1, str);
        m_handler.sendMessage(msg); //发送消息
    }

    private void test_videoplayer(){
        VideoDataHandler.getInstence().clear();
        sendMessage(" 通道" + mChannel + "  接收数据：" + 0);
        vv.startPlayThread(); //start a new thread

        /*tmp test start*/
        File file = new File("/data/local/tmp/01007D_0.h264");
        try{
            inStream = new FileInputStream(file);
        }catch (Exception e){
            e.printStackTrace();
        }
        /*tmp test end*/
        //打开Receiver线程
        tcpVideoReceiver = new TcpVideoReceiver(inStream, new TcpRecvListener() {
            @Override
            public void onReceive(byte[] src, int len) {
                if (src == null) {
                    LogUtil.e(TAG, "src == null");
                    return;
                }
            }

            @Override
            public void onReceiveBegin() {

            }

            @Override
            public void onReceiveEnd() {

            }

            @Override
            public void onRecvNetException() {
                LogUtil.e(TAG, "onRecvNetException");
            }

            @Override
            public String getTcpRecvListenerName() {
                return "USBEndpointListener";
            }
        });
        tcpVideoReceiver.start();
    }

    private BroadcastReceiver m_DsmParamCalibrationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("DsmParamCalibration")){
                String angle = intent.getStringExtra("angle");
                if(tv_angle_value != null)
                    tv_angle_value.setText(angle);
                Log.d(TAG,"m_DsmParamCalibrationReceiver angle="+angle);
            }
        }
    };
}
