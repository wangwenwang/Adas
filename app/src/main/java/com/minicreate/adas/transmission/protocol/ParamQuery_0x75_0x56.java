package com.minicreate.adas.transmission.protocol;

import android.content.Context;

import com.minicreate.adas.utils.LogUtil;

public class ParamQuery_0x75_0x56 extends BaseParamAdas {
    private static final String TAG = "ParamQuery_0x75_0x56";

    private String value1 = "";
    private String value2 = "";
    private String value3 = "";
    private String value4 = "";
    private String value5 = "";
    private String value6 = "";
    //private int offMode;
    public ParamQuery_0x75_0x56(Context context) {
        super(0x56, context);
        setCommand(0x75);
    }
    @Override
    public void parseFromProtocol(byte[] src) {
        //子命令内容长度
        int subLen = getSubCommandLen(src);
        LogUtil.d(TAG, "subLen = " + subLen);
        if (subLen < 6) {
            LogUtil.e(TAG, "长度过短，subLen = " + subLen);
            return;
        }
        //子命令内容，从数组11开始
        for (int i = 0; i < 6; i++) {
            value1 += "" + (char) src[11 + i];
        }
        LogUtil.d(TAG, "value1 = " + value1);

        for (int i = 0; i < 11; i++) {
            value2 += "" + (char) src[17 + i];
        }
        LogUtil.d(TAG, "value2 = " + value2);
        for (int i = 0; i < 15; i++) {
            if(src[28 + i] == 0)
                continue;
            value3 += "" + (char) src[28 + i];
        }
        LogUtil.d(TAG, "value3 = " + value3);
        for (int i = 0; i < 5; i++) {
            if(src[43 + i] == 0)
                continue;
            value4 += "" + (char) src[43 + i];
        }
        LogUtil.d(TAG, "value4 = " + value4);
        for (int i = 0; i < 15; i++) {
            if(src[48 + i] == 0)
                continue;
            value5 += "" + (char) src[48 + i];
        }
        LogUtil.d(TAG, "value5 = " + value5);
        for (int i = 0; i < 5; i++) {
            if(src[63 + i] == 0)
                continue;
            value6 += "" + (char) src[63 + i];
        }
        LogUtil.d(TAG, "value6 = " + value6);
        //offMode = (byte)src[74];
    }

    public String getValue1() {
        return value1;
    }
    public String getValue2() {
        return value2;
    }
    public String getValue3() {
        return value3;
    }
    public String getValue4() {
        return value4;
    }
    public String getValue5() {
        return value5;
    }
    public String getValue6() {
        return value6;
    }

    @Override
    public String toString() {
        return "ParamQuery_0x75_0x41 [vehicleNo=" + value1 + "]";
    }
}
