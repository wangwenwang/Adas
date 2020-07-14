package com.minicreate.adas.transmission.protocol;

import android.content.Context;

import com.minicreate.adas.utils.BytesUtil;
import com.minicreate.adas.utils.LogUtil;

public class DsmParamCalibration_0x75_0x53 extends BaseParamAdas  {
    private static final String TAG = "DsmParamCalibration_0x75_0x53";
    private String angle = "";

    public DsmParamCalibration_0x75_0x53(Context context) {
        super(0x53, context);
        setCommand(0x75);
    }
    public String getAngle(){
        return angle;
    }

    @Override
    public void parseFromProtocol(byte[] src) {
        //子命令内容长度
        int subLen = getSubCommandLen(src);
        LogUtil.d(TAG, "subLen = " + subLen);
        //子命令内容，从数组11开始
        for (int i = 0; i < subLen; i++) {
            angle += "" + (char) src[11 + i];
        }
        LogUtil.d(TAG, "angle = " + angle);
    }
    @Override
    public String toString() {
        return "DsmParamCalibration_0x75_0x53 [angle=" + angle + "]";
    }
}
