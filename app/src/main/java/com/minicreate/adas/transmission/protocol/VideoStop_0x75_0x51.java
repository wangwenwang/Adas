package com.minicreate.adas.transmission.protocol;

import android.content.Context;

import com.minicreate.adas.utils.BytesUtil;
import com.minicreate.adas.utils.LogUtil;

public class VideoStop_0x75_0x51 extends BaseParamAdas  {
    private static final String TAG = "VideoStop_0x75_0x51";

    private int channel = 0;

    public VideoStop_0x75_0x51(Context context) {
        super(0x51, context);
        setCommand(0x75);
    }

    public void setchannel(int channel){
        this.channel = channel;
    }
    @Override
    protected void packContent() {
        content = new byte[1];
        content[0] = (byte) channel;
        LogUtil.d(TAG, "data = " + BytesUtil.BytestoHexStringPrintf(content));
    }
}
