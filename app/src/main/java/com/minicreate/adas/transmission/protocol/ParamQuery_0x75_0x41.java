package com.minicreate.adas.transmission.protocol;

import android.content.Context;

import com.minicreate.adas.transmission.protocol.BaseParamAdas;
import com.minicreate.adas.utils.LogUtil;

public class ParamQuery_0x75_0x41 extends BaseParamAdas {
    private static final String TAG = "ParamQuery_0x75_0x41";

    private String vehicleNo = "";
    private String phonenum = "";//手机号
    private String ip1 = "";
    private String port1 = "";
    private String ip2 = "";
    private String port2 = "";
    private int masterMode = 0;//主机模式
    //private int offMode;
    public ParamQuery_0x75_0x41(Context context) {
        super(0x41, context);
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
        //车辆编号
        for (int i = 0; i < 6; i++) {
            vehicleNo += "" + (char) src[11 + i];
        }
        LogUtil.d(TAG, "vehicleNo = " + vehicleNo);

        for (int i = 0; i < 11; i++) {
            phonenum += "" + (char) src[17 + i];
        }
        LogUtil.d(TAG, "phonenum = " + phonenum);
        for (int i = 0; i < 15; i++) {
            if(src[28 + i] == 0)
                continue;
            ip1 += "" + (char) src[28 + i];
        }
        LogUtil.d(TAG, "ip1 = " + ip1);
        for (int i = 0; i < 5; i++) {
            if(src[43 + i] == 0)
                continue;
            port1 += "" + (char) src[43 + i];
        }
        LogUtil.d(TAG, "port1 = " + port1);
        for (int i = 0; i < 15; i++) {
            if(src[48 + i] == 0)
                continue;
            ip2 += "" + (char) src[48 + i];
        }
        LogUtil.d(TAG, "ip2 = " + ip2);
        for (int i = 0; i < 5; i++) {
            if(src[63 + i] == 0)
                continue;
            port2 += "" + (char) src[63 + i];
        }
        LogUtil.d(TAG, "port2 = " + port2);
        if (subLen == (6 + 11 + 15 + 5 + 15 + 5 + 1))
            masterMode = (byte)src[68];
        LogUtil.d(TAG, "masterMode = " + masterMode);
        //offMode = (byte)src[74];
    }

    public String getVehicleNo() {
        return vehicleNo;
    }
    public String getphonenum() {
        return phonenum;
    }
    public String getIp1() {
        return ip1;
    }
    public String getPort1() {
        return port1;
    }
    public String getIp2() {
        return ip2;
    }
    public String getPort2() {
        return port2;
    }
    public int getMasterMode() {
        return masterMode;
    }
    //public int getOffMode() { return offMode; }
    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    @Override
    public String toString() {
        return "ParamQuery_0x75_0x41 [vehicleNo=" + vehicleNo + "]";
    }
}
