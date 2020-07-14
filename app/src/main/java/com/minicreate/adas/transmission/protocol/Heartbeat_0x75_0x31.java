package com.minicreate.adas.transmission.protocol;

import android.content.Context;

import com.minicreate.adas.transmission.protocol.BaseParamAdas;

public class Heartbeat_0x75_0x31 extends BaseParamAdas {
    public Heartbeat_0x75_0x31(Context context) {
        super(0x31, context);
        setCommand(0x75);
    }
}