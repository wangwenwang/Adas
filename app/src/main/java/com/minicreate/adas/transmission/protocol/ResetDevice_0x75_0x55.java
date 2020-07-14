package com.minicreate.adas.transmission.protocol;

import android.content.Context;

public class ResetDevice_0x75_0x55 extends BaseParamAdas  {
    private static final String TAG = "ResetDevice_0x75_0x55";

    public ResetDevice_0x75_0x55(Context context) {
        super(0x55, context);
        setCommand(0x75);
    }

}
