package com.minicreate.adas.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.minicreate.adas.App;
import com.minicreate.adas.R;
import com.minicreate.adas.transmission.WifiEndpoint;
import com.minicreate.adas.transmission.protocol.OnResponseListener;
import com.minicreate.adas.transmission.protocol.Param;
import com.minicreate.adas.transmission.protocol.ParamQuery_0x75_0x41;
import com.minicreate.adas.transmission.protocol.ParamQuery_0x75_0x56;
import com.minicreate.adas.utils.LogUtil;

import androidx.appcompat.app.AppCompatActivity;

public class FunctionTestActivity extends AppCompatActivity {
    private static final String TAG = "FunctionTestActivity";
    private TextView tv1;
    private TextView tv2;
    private TextView tv3;
    private TextView tv4;
    private TextView tv5;
    private TextView tv6;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.function_test);

        tv1 = (TextView)findViewById(R.id.textView1);
        tv2 = (TextView)findViewById(R.id.textView2);
        tv3 = (TextView)findViewById(R.id.textView3);
        tv4 = (TextView)findViewById(R.id.textView4);
        tv5 = (TextView)findViewById(R.id.textView5);
        tv6 = (TextView)findViewById(R.id.textView6);
        queryParam();
    }

    public void queryParam(){
        ParamQuery_0x75_0x56 param = new ParamQuery_0x75_0x56(this);
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
                        tv1.setText(((ParamQuery_0x75_0x56)result).getValue1());
                        tv2.setText(((ParamQuery_0x75_0x56)result).getValue2());
                        tv3.setText(((ParamQuery_0x75_0x56)result).getValue3());
                        tv4.setText(((ParamQuery_0x75_0x56)result).getValue4());
                        tv5.setText(((ParamQuery_0x75_0x56)result).getValue5());
                        tv6.setText(((ParamQuery_0x75_0x56)result).getValue6());
                    }
                }
            }, param);
        } else {
            LogUtil.e(TAG, "WIFI Socket连接断开");
        }
    }
}
