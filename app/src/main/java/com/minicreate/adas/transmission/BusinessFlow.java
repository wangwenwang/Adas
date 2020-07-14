package com.minicreate.adas.transmission;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.minicreate.adas.transmission.protocol.OnResponseListener;
import com.minicreate.adas.transmission.protocol.Param;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BusinessFlow {

    private final String TAG = BusinessFlow.class.getSimpleName();


    //应答实体，里面包含OnResponseListener和BaseParam
    public static class ResponseEntry {
        public OnResponseListener onResponseListener;
        public Param param;

        public ResponseEntry(OnResponseListener onResponseListener, Param param) {
            this.onResponseListener = onResponseListener;
            this.param = param;
        }
    }

    public BusinessFlow() {
    }



}
