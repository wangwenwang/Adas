package com.minicreate.adas.transmission.protocol;

import android.content.Context;

import com.minicreate.adas.transmission.protocol.BaseParamAdas;

public class testmode_0x75_0x30 extends BaseParamAdas {
    private static final String TAG = "testmode_0x75_0x30";

    public testmode_0x75_0x30(Context context) {
        super(0x30, context);
        setCommand(0x75);
    }
}