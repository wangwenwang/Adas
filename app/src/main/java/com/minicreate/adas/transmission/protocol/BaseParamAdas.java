package com.minicreate.adas.transmission.protocol;

import android.content.Context;
import android.util.Log;

import com.minicreate.adas.transmission.protocol.AbstractBaseParam;
import com.minicreate.adas.utils.NetUtil;

import java.io.UnsupportedEncodingException;

public class BaseParamAdas extends AbstractBaseParam {

    private static final String TAG = "BaseParamAdas";
    private Context context;
    protected int id = -1;//子命令类型，最好使用16进制
    private int command = -1;//命令类型
    protected volatile byte[] content;//子命令内容

    public void setCommand(int command) {
        this.command = command;
    }

    public BaseParamAdas(int id, Context context) {
        this.context = context;
        this.id = id;
    }

    @Override
    public synchronized byte[] parseToProtocol() {
        if (command == -1) {
            Log.d(TAG, "命令类型有问题");
        }
        //首先打包内容
        packContent();
        //总长度为：协议头（1）+校验码（1）+流水号（2）+厂商编号（2）+外设类型编号（1）+命令类型（1）+用户数据（n）+协议尾（1）
        int contentLen = 0;
        if (content != null) {
            contentLen = content.length;
        }

        byte[] data = new byte[12 + contentLen];
        int destPos = 0;//偏移位置
        //协议头
        data[destPos] = 0x7e;
        destPos++;
        //越过校验码，后面再补充
        destPos++;
        //流水号
        data[destPos++] = 0;
        data[destPos++] = 0;
        //厂商编号
        data[destPos++] = 0;
        data[destPos++] = 3;
        //外设类型编号
        data[destPos++] = 0x65;
        //命令类型，command
        data[destPos++] = (byte) command;

        if (content != null) {
            //子命令
            data[destPos++] = (byte) id;
            //子命令内容长度
            data[destPos++] = (byte) ((content.length >> 8) & 0xff);
            data[destPos++] = (byte) (content.length & 0xff);
            Log.d(TAG, "content.lenght = " + content.length);
        } else {
            //子命令
            data[destPos++] = (byte) id;
            //子命令内容长度
            data[destPos++] = (byte) ((0 >> 8) & 0xff);
            data[destPos++] = (byte) (0 & 0xff);
            Log.d(TAG, "content.lenght = 0");
        }

        if (content != null) {
            System.arraycopy(content, 0, data, destPos, content.length);
            destPos += content.length;
        }
        //协议尾
        data[destPos] = 0x7e;
        //计算校验码，从厂商编号到用户数据依次累加的累加和，然后取累加的低8位作为校验码
        int jiaoyan = NetUtil.jiaoyan(data, 4, destPos);
        data[1] = (byte) jiaoyan;
        //发送之前要转义
        //LogUtil.d(TAG, "data.size = " + data.length + " ,destPos = " + destPos + " ,data[dest] = " + data[destPos] + " data = " + Arrays.toString(data));
//        LogUtil.d(TAG, "data = " + BytesUtil.BytestoHexStringPrintf(data));
        byte[] result = escape(data);
        Log.d(TAG, "result = " + BytestoHexStringPrintf(result));
        return result;
    }

    @Override
    public void parseFromProtocol(byte[] src) {

    }

    /**
     * 转义
     *
     * @param unescapedBuffer
     * @return
     */
    private static byte[] escape(byte[] unescapedBuffer) {
        int num = 0;
        // 找出除了头尾之外所有的7e和7d
        for (int i = 1; i < unescapedBuffer.length - 1; i++) {
            if (((unescapedBuffer[i] & 0xff) == 0x7e) || ((unescapedBuffer[i] & 0xff) == 0x7d)) {
                num++;
            }
        }
        byte[] data = new byte[unescapedBuffer.length + num];
        int index = 1;
        for (int i = 1; i < unescapedBuffer.length - 1; i++) {
            if ((unescapedBuffer[i] & 0xff) == 0x7e) {
                data[index++] = 0x7d;
                data[index++] = 0x02;
            } else if ((unescapedBuffer[i] & 0xff) == 0x7d) {
                data[index++] = 0x7d;
                data[index++] = 0x01;
            } else {
                data[index++] = unescapedBuffer[i];
            }
        }
        // 前后补上7e
        data[0] = 0x7e;
        data[data.length - 1] = 0x7e;
        return data;
    }

    /**
     * 该协议的命令字就是id
     *
     * @return
     */
    @Override
    public int getToken() {
        return id;
    }

    /**
     * 打包子类的内容，这里使用模板方法模式
     */
    protected void packContent() {

    }

    /**
     * 获取数组中的子命令内容长度
     */
    protected int getSubCommandLen(byte[] src) {
        if (src != null) {
            //第9、10位
            int height = (src[9] << 8) & 0xff;
            int low = src[10] & 0xff;
            int subLen = height + low;
            return subLen;
        } else {
            return 0;
        }
    }

    public static String BytestoHexStringPrintf(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b) + " ");
        }

        return builder.toString();
    }

    public static String BytestoHexStringPrintf(byte[] bytes, int len) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            builder.append(String.format("%02x", bytes[i]) + " ");
        }

        return builder.toString();
    }

    /**
     * 累加和校验
     *
     * @param b
     * @param start 包含
     * @param end   不包含
     * @return
     */
    public static int checksum(byte[] b, int start, int end) {
        byte a = b[start];
        for (int i = start + 1; i < end; i++) {
            a += b[i];
        }
        return (a & 0xff);
    }
}
