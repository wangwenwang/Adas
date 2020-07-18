package com.minicreate.adas.transmission.protocol;

import android.content.Context;
import android.util.Log;

import java.nio.charset.Charset;

public class ParamQuery_0x75_0x69 extends BaseParamAdas {
    private static final String TAG = "ParamQuery_0x75_0x69";

    private String value1 = "";
    public ParamQuery_0x75_0x69(Context context) {
        super(0x69, context);
        setCommand(0x75);
    }
    @Override
    public void parseFromProtocol(byte[] src) {
        //子命令内容长度
        int subLen = getSubCommandLen(src);

        // 处理前面部分
        int deal_begin = 0;
        for (int i = (src.length - 1); i > 0; i--){
            if(src[i] == 0){
                deal_begin = i + 1;
                break;
            }
        }
        int deal_length = src.length - deal_begin;
        byte[] deal = new byte[deal_length];
        for (int i = 0; i < deal_length; i++){

            deal[i] = src[i + deal_begin];
            Log.d("LM", "deal[i] = " + deal[i]);
        }

        // 处理后面部分
        deal_begin = 0;
        for (int i = (deal.length - 1); i > 0; i--){
            if(deal[i] == 13){
                deal_begin = i + 1;
                break;
            }
        }
        int deal_lengt_h = deal_begin;
        byte[] dea_l = new byte[deal_lengt_h];
        for (int i = 0; i < deal_begin; i++){

            dea_l[i] = deal[i];
            Log.d("LM", "deal[i] = " + dea_l[i]);
        }
        String msgContent = new String(dea_l, Charset.forName("GB2312"));
        value1 = msgContent;
    }

    public String getValue1() {
        return value1;
    }

    @Override
    public String toString() {
        return "ParamQuery_0x75_0x69 [value1=" + value1 + "]";
    }
}
