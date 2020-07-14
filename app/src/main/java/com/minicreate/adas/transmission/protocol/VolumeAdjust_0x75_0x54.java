package com.minicreate.adas.transmission.protocol;

import android.content.Context;

import com.minicreate.adas.utils.BytesUtil;
import com.minicreate.adas.utils.LogUtil;

public class VolumeAdjust_0x75_0x54 extends BaseParamAdas {
    private static final String TAG = "VolumeAdjust_0x75_0x54";
    private int volume;

    public VolumeAdjust_0x75_0x54(Context context) {
        super(0x54, context);
        setCommand(0x75);
    }

    public void setvolume(int volume){
        this.volume = volume;
    }

    @Override
    protected void packContent() {
        content = new byte[4];
        System.arraycopy(BytesUtil.int2BytesArray(volume), 0, content, 0, 4);
        LogUtil.d(TAG, "data = " + BytesUtil.BytestoHexStringPrintf(content));
    }

}
