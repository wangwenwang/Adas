package com.minicreate.adas.transmission.protocol;

import android.content.Context;
import android.util.Log;

import com.minicreate.adas.transmission.protocol.BaseParamAdas;
import com.minicreate.adas.utils.BytesUtil;
import com.minicreate.adas.utils.LogUtil;

/**
 * 设置参数
 */
public class SetParam_0x75_0x42 extends BaseParamAdas {
    private static final String TAG = "SetParam_0x75_0x42";
    private String vehicleNo;//车辆编号
    private String phonenum;//手机号
    private String ip1;
    private String port1;
    private String ip2;
    private String port2;
    private int masterMode;//主机模式
    //private int offMode;//关机模式
    private int result;

    public SetParam_0x75_0x42(Context context) {
        super(0x42, context);
        setCommand(0x75);
    }

    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }
    public void setPhonenum(String phonenum) {
        this.phonenum = phonenum;
    }
    public void setIp1(String ip1) { this.ip1 = ip1;}
    public void setPort1(String port1) { this.port1 = port1;}
    public void setIp2(String ip2) { this.ip2 = ip2;}
    public void setPort2(String port2) { this.port2 = port2;}
    public void setMasterMode(int masterMode) { this.masterMode = masterMode;}
    /*
    public void setOffMode(int offMode) {
        this.offMode = offMode;
    }*/


    @Override
    protected void packContent() {
        content = new byte[6 + 11 + 15 + 5 + 15 + 5 + 1];

        System.arraycopy(vehicleNo.getBytes(), 0, content, 0, 6);
        System.arraycopy(phonenum.getBytes(), 0, content, 6, 11);
        //String[]items=ip1.split(".");
        /*
        String[] tmp_ip1 = ip1.split("\\.");
        for(int i = 0; i < tmp_ip1.length; i++){
            if(tmp_ip1[i].length() < 3){
                StringBuilder str = new StringBuilder();
                str.append(tmp_ip1[i]);
               for(int j = 0 ;j < 3 - tmp_ip1[i].length(); j++ ){
                   str.append("\0");
               }
               tmp_ip1[i] = str.toString();
            }
        }
        StringBuilder ip1_after_fill = new StringBuilder();
        for(int i = 0; i < tmp_ip1.length; i++) {
            if(i < tmp_ip1.length - 1)
            {
                ip1_after_fill.append(tmp_ip1[i]+".");
            }
            else
                ip1_after_fill.append(tmp_ip1[i]);
        }
        System.arraycopy(ip1_after_fill.toString().getBytes(), 0, content, 17, 15);
        */
        if(ip1.length() < 15){
            StringBuilder str = new StringBuilder();
            str.append(ip1);
            for(int j = 0 ;j < 15 - ip1.length(); j++ ){
                str.append("\0");
            }
            ip1 = str.toString();
        }
        System.arraycopy(ip1.getBytes(), 0, content, 17, 15);

        if(port1.length() < 5){
            StringBuilder str = new StringBuilder();
            str.append(port1);
            for(int j = 0 ;j < 5 - port1.length(); j++ ){
                str.append("\0");
            }
            port1 = str.toString();
        }
        System.arraycopy(port1.getBytes(), 0, content, 32, 5);
/*
        String[] tmp_ip2 = ip2.split("\\.");
        for(int i = 0; i < tmp_ip2.length; i++){
            if(tmp_ip2[i].length() < 3){
                StringBuilder str = new StringBuilder();
                str.append(tmp_ip2[i]);
                for(int j = 0 ;j < 3 - tmp_ip2[i].length(); j++ ){
                    str.append("\0");
                }
                tmp_ip2[i] = str.toString();
            }
        }
        StringBuilder ip2_after_fill = new StringBuilder();
        for(int i = 0; i < tmp_ip2.length; i++) {
            if(i < tmp_ip2.length - 1)
            {
                ip2_after_fill.append(tmp_ip2[i]+".");
            }
            else
                ip2_after_fill.append(tmp_ip2[i]);
        }
        System.arraycopy(ip2_after_fill.toString().getBytes(), 0, content, 37, 15);
*/
        if(ip2.length() < 15){
            StringBuilder str = new StringBuilder();
            str.append(ip2);
            for(int j = 0 ;j < 15 - ip2.length(); j++ ){
                str.append("\0");
            }
            ip2 = str.toString();
        }
        System.arraycopy(ip2.getBytes(), 0, content, 37, 15);

        if(port2.length() < 5){
            StringBuilder str = new StringBuilder();
            str.append(port2);
            for(int j = 0 ;j < 5 - port2.length(); j++ ){
                str.append("\0");
            }
            port2 = str.toString();
        }
        System.arraycopy(port2.getBytes(), 0, content, 52, 5);

        System.arraycopy(String.valueOf(masterMode).getBytes(), 0, content, 57, 1);
        LogUtil.d(TAG, "data = " + BytesUtil.BytestoHexStringPrintf(content));

        //content[63] = (byte)offMode;
        //Log.d(TAG,"SetParam_0x75_0x42 packContent content::"+content.toString().toString());
    }

    @Override
    public void parseFromProtocol(byte[] src) {
        int subLen = getSubCommandLen(src);
        if (subLen != 1) {
            LogUtil.e(TAG, "长度不对，subLen = " + subLen);
            return;
        }
        result = src[11] & 0xff;
    }

    @Override
    public String toString() {
        return "SetParam_0x75_0x42{" +
                "vehicleNo='" + vehicleNo + '\'' +
                ", result=" + result +
                '}';
    }
}
