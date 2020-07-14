package com.minicreate.adas.transmission.strategy;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.minicreate.adas.transmission.protocol.BaseParamAdas;
import com.minicreate.adas.transmission.protocol.DsmParamCalibration_0x75_0x53;
import com.minicreate.adas.transmission.protocol.Heartbeat_0x75_0x31;
import com.minicreate.adas.transmission.strategy.Strategy;
import com.minicreate.adas.transmission.protocol.Param;

import com.minicreate.adas.utils.BytesUtil;

import com.minicreate.adas.utils.LogUtil;
import com.minicreate.adas.utils.NetUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 与Adas主机设备的交互协议策略类
 */
public class ProtocalAdasStrategy implements Strategy {
    //记录接收到的包序号
    private int currentPackage = 0;
    private static final String TAG = "ProtocalAdasStrategy";

    private long beginTime = 0;
    private long endTime = 0;


    //TODO 实时命令字列表，使用享元模式，保存所有实时命令字的协议类
    private static Map<Integer, BaseParamAdas> map = new HashMap<>();


    @Override
    public int getToken(Param param) {
        return param.getToken();
    }

    /**
     * 这一份协议的标识码就是子命令，也就是数组第8位
     * <p>
     * 记得先反转义
     *
     * @param src
     * @return
     */
    @Override
    public int getToken(byte[] src) {
        if (src.length < 8) {
            Log.e(TAG, "数据有问题，长度太短");
            return -1;
        }
        return src[8];
    }

    /**
     * 转义
     *
     * @param unescapedBuffer
     * @return
     */
    @Override
    public byte[] escape(byte[] unescapedBuffer) {
        return unescapedBuffer;
    }

    @Override
    public byte[] unescape(byte[] escapedBuffer) {
        return NetUtil.unescape_0x7e_0x7d(escapedBuffer);
    }

    @Override
    public byte[] parseToProtocol(Param param) {
        return param.parseToProtocol();
    }

    @Override
    public Param parseFromProtocol(byte[] src, Param param) {
        param.parseFromProtocol(src);
        return param;
    }

    @Override
    public boolean checkData(byte[] src) {
        return true;
    }

    @Override
    public List<byte[]> findPackage(byte[] src) {
        return NetUtil.findPackageBySameHeadAndEnd(src, 0x7e);
    }

    @Override
    public Param getHeartbeat(Context context) {
        Heartbeat_0x75_0x31 heartbeat = new Heartbeat_0x75_0x31(context);
        return heartbeat;
    }

    @Override
    public long getHeartbeatPeriod() {
        return 3000;
    }
    /**
     * @param src
     * @return
     */
    public boolean handleMessage(byte[] src, Context context){
        LogUtil.d(TAG, "handleMessage src=="+BytesUtil.BytestoHexStringPrintf(src));
        if (src.length < 7) {
            LogUtil.e(TAG, "数据过短，小于7");
            return false;
        }

        int commandIndex = 7;//命令类型下标
        int token = getToken(src) & 0xff;//获取子命令
        switch (src[commandIndex]) {
            case 0x75: {
                if (token == 0x53) {
                    DsmParamCalibration_0x75_0x53 param = new DsmParamCalibration_0x75_0x53(context);
                    param.parseFromProtocol(src);

                    Intent intent = new Intent("DsmParamCalibration");
                    intent.putExtra("angle", param.getAngle());
                    context.sendBroadcast(intent);
                    return true;
                }
            }
        }
        return false;
    }
}
