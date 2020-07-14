package com.minicreate.adas.transmission.protocol;

import android.content.Context;

import com.minicreate.adas.utils.BytesUtil;
import com.minicreate.adas.utils.LogUtil;

public class VideoStart_0x75_0x50 extends BaseParamAdas {
    private static final String TAG = "VideoStart_0x75_0x50";
    private String ip;
    private int port;
    private int channel = 0;


    public VideoStart_0x75_0x50(Context context) {
        super(0x50, context);
        setCommand(0x75);
    }

    public void setip(String ip){
        this.ip = ip;
    }
    public void setport(int port){
        this.port = port;
    }
    public void setchannel(int channel){
        this.channel = channel;
    }
    @Override
    protected void packContent() {
        content = new byte[15 + 4 + 1];

        if(ip.length() < 15){
            StringBuilder str = new StringBuilder();
            str.append(ip);
            for(int j = 0 ;j < 15 - ip.length(); j++ ){
                str.append("\0");
            }
            ip = str.toString();
        }
        System.arraycopy(ip.getBytes(), 0, content, 0, 15);

        System.arraycopy(BytesUtil.int2BytesArray(port), 0, content, 15, 4);

        content[15+4] = (byte) channel;

        LogUtil.d(TAG, "data = " + BytesUtil.BytestoHexStringPrintf(content));
    }

    @Override
    public String toString() {
        return "VideoStart_0x75_0x50{" +
                "ip='" + ip + '\'' +
                ", port=" + port + ", channel=" + channel+
                '}';
    }
}
