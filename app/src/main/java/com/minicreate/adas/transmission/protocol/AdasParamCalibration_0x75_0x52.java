package com.minicreate.adas.transmission.protocol;

import android.content.Context;

import com.minicreate.adas.utils.BytesUtil;
import com.minicreate.adas.utils.LogUtil;

public class AdasParamCalibration_0x75_0x52 extends BaseParamAdas  {
    private static final String TAG = "AdasParamCalibration_0x75_0x52";

    private String distance;

    public AdasParamCalibration_0x75_0x52(Context context) {
        super(0x52, context);
        setCommand(0x75);
    }

    public void setdistance(String distance){
        this.distance = distance;
    }
    @Override
    protected void packContent() {
        content = new byte[12];

        if(distance.length() < 12){
            StringBuilder str = new StringBuilder();
            str.append(distance);
            for(int j = 0 ;j < 12 - distance.length(); j++ ){
                str.append("\0");
            }
            distance = str.toString();
        }
        System.arraycopy(distance.getBytes(), 0, content, 0, 12);
    }
    @Override
    public String toString() {
        return "AdasParamCalibration_0x75_0x52 [distance=" + distance + "]";
    }
}
