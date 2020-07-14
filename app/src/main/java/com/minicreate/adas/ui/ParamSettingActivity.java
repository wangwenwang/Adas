package com.minicreate.adas.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;

import com.minicreate.adas.App;
import com.minicreate.adas.R;
import com.minicreate.adas.transmission.WifiEndpoint;

import com.minicreate.adas.transmission.protocol.OnResponseListener;
import com.minicreate.adas.transmission.protocol.Param;
import com.minicreate.adas.transmission.protocol.ParamQuery_0x75_0x41;
import com.minicreate.adas.transmission.protocol.SetParam_0x75_0x42;
import com.minicreate.adas.utils.LogUtil;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.text.method.ReplacementTransformationMethod;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;


public class ParamSettingActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "ParamSettingActivity";
    private EditText medit_no;
    private EditText medit_phonenum;
    private EditText medit_ip1;
    private EditText medit_port1;
    private EditText medit_ip2;
    private EditText medit_port2;
    private EditText medit_master_mode;

    private Button mbtn_sure;
    private Button mbtn_back;

    public static class UpperTransform extends ReplacementTransformationMethod {
        @Override
        protected char[] getOriginal() {
            char[] ori = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
            return ori;
        }

        @Override
        protected char[] getReplacement() {
            char[] dis = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
            return dis;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_param_setting);
        medit_no = (EditText)findViewById(R.id.edit_no);
        medit_no.setTransformationMethod(new UpperTransform());

        medit_phonenum = (EditText)findViewById(R.id.edit_phonenum);
        medit_ip1 = (EditText)findViewById(R.id.edit_ip1);
        medit_port1 = (EditText)findViewById(R.id.edit_port1);
        medit_ip2 = (EditText)findViewById(R.id.edit_ip2);
        medit_port2 = (EditText)findViewById(R.id.edit_port2);
        medit_master_mode  = (EditText)findViewById(R.id.edit_master_mode);

        mbtn_sure = (Button)findViewById(R.id.btn_sure);
        mbtn_back = (Button)findViewById(R.id.btn_back);
        mbtn_sure.setOnClickListener(this);
        mbtn_back.setOnClickListener(this);
        queryParam();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_sure:
                if(checkValid()){
                    SetParam_0x75_0x42 param = new SetParam_0x75_0x42(this);
                    param.setVehicleNo(medit_no.getText().toString().trim().toUpperCase());
                    param.setPhonenum(medit_phonenum.getText().toString().trim());
                    param.setIp1(medit_ip1.getText().toString().trim());
                    param.setPort1(medit_port1.getText().toString().trim());
                    param.setIp2(medit_ip2.getText().toString().trim());
                    param.setPort2(medit_port2.getText().toString().trim());
                    param.setMasterMode(Integer.valueOf(medit_master_mode.getText().toString().trim()));

                    WifiEndpoint wifiEndpoint = ((App)getApplication()).getWifiEndpoint();
                    if (wifiEndpoint != null) {
                        wifiEndpoint.send(new OnResponseListener() {
                            @Override
                            public void onResponse(Param result) {
                                if (result.isTimeOut()) {
                                    LogUtil.d(TAG, "设置关机模式超时");
                                } else {
                                    LogUtil.d(TAG, result.toString());
                                }
                            }
                        }, param);
                    } else {
                        LogUtil.e(TAG, "Wifi Socket连接断开");
                    }
                }
                break;
            case R.id.btn_back:
                finish();
                break;

        }
    }
    public boolean checkValid(){
        if(medit_no.getText().toString().trim().length() != 6){
            creatAlertDialog("车辆编号必须为6位！");
            return false;
        }
        if(medit_phonenum.getText().toString().trim().length() != 11){
            creatAlertDialog("手机号必须为11位！");
            return false;
        }
        /*
        if(medit_ip1.getText().toString().trim().length()  < 7){
            creatAlertDialog("IP1地址不合法！");
            return false;
        }

        if(medit_port1.getText().toString().trim().length() != 5){
            creatAlertDialog("Port1必须为5位！");
            return false;
        }
        */
        /*
        if(medit_ip2.getText().toString().trim().length() < 7){
            creatAlertDialog("IP1地址不合法！");
            return false;
        }*/
        /*
        if(medit_port2.getText().toString().trim().length() != 5){
            creatAlertDialog("Port2必须为5位！");
            return false;
        }
        */
        return true;
    }

    private void creatAlertDialog(String message){
        AlertDialog alert = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        alert = builder.setIcon(R.drawable.ico_skip_failed)
                .setTitle("提示：")
                .setMessage(message)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create();
        alert.show();

    }

    public void queryParam(){
        ParamQuery_0x75_0x41 param = new ParamQuery_0x75_0x41(this);
        WifiEndpoint wifiEndpoint = ((App)getApplication()).getWifiEndpoint();
        if (wifiEndpoint != null) {
            wifiEndpoint.send(new OnResponseListener() {
                @Override
                public void onResponse(Param result) {
                    if (result.isTimeOut()) {
                        LogUtil.d(TAG, "参数查询超时");
                    } else {
                        LogUtil.d(TAG, result.toString());
                        LogUtil.d(TAG, "成功 getVehicleNo = " + ((ParamQuery_0x75_0x41)result).getVehicleNo());
                        medit_no.setText(((ParamQuery_0x75_0x41)result).getVehicleNo());
                        medit_phonenum.setText(((ParamQuery_0x75_0x41)result).getphonenum());
                        medit_ip1.setText(((ParamQuery_0x75_0x41)result).getIp1());
                        medit_port1.setText(((ParamQuery_0x75_0x41)result).getPort1());
                        medit_ip2.setText(((ParamQuery_0x75_0x41)result).getIp2());
                        medit_port2.setText(((ParamQuery_0x75_0x41)result).getPort2());
                        medit_master_mode.setText(((ParamQuery_0x75_0x41)result).getMasterMode() == 0 ? "" : String.valueOf(((ParamQuery_0x75_0x41)result).getMasterMode()));
                    }
                }
            }, param);
        } else {
            LogUtil.e(TAG, "WIFI Socket连接断开");
        }
    }
}
