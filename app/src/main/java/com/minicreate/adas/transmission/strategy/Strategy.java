package com.minicreate.adas.transmission.strategy;

import android.content.Context;

import com.minicreate.adas.transmission.protocol.Param;

import java.util.List;

/**
 * 协议策略接口
 */
public interface Strategy {
    //从协议实体类中获取协议认证码
    public int getToken(Param param);

    /**
     * 从byte数组中获取协议认证码，认证码是用来寻找请求消息的
     * 协议的发送和应答的认证码可能不同，这个要注意
     * <p>
     * 记得一定要先反转义
     *
     * @param src
     * @return
     */
    public int getToken(byte[] src);

    //数据转义
    public byte[] escape(byte[] unescapedBuffer);

    //反转义
    public byte[] unescape(byte[] escapedBuffer);

    //解析协议实体类为可发送的byte数组
    public byte[] parseToProtocol(Param param);

    /**
     * 将byte数组解析为协议实体类
     * 返回的BaseParam应该与传递的BaseParam一致
     * <p>
     * 记得一定要先反转义
     *
     * @param src   需要解析的byte数组
     * @param param
     * @return
     */
    public Param parseFromProtocol(byte[] src, Param param);

    //判断数据是否完整，校验码是否正确等
    public boolean checkData(byte[] src);

    /**
     * 根据协议头寻找协议
     * <p>
     * 该方法是对接收线程接收的原始数组使用
     *
     * @param src
     * @return
     */
    public List<byte[]> findPackage(byte[] src);

    /**
     * 获取心跳包实体类，有些心跳包需要填写车辆编号，得用到Context
     *
     * @return
     */
    public Param getHeartbeat(Context context);

    /**
     * 获取心跳包的间隔，这是为了3520D专门做的接口
     *
     * @return
     */
    public long getHeartbeatPeriod();

    /**
     * @param src
     * @return
     */
    public boolean handleMessage(byte[] src, Context context);
}
