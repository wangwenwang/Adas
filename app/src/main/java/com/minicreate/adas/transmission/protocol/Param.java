package com.minicreate.adas.transmission.protocol;

/**
 * 协议接口类
 */
public interface Param {
    /**
     * 将实体类转化为协议数据
     */
    public byte[] parseToProtocol();

    /**
     * 将协议数据解析成实体类
     * <p>
     * 记得先反转义
     */
    public void parseFromProtocol(byte[] src);

    /**
     * 获取协议标识符
     *
     * @return
     */
    public int getToken();

    /**
     * 判断是否超时
     *
     * @return
     */
    public boolean isTimeOut();

    /**
     * 设置超时状态，false表示未超时，true表示超时
     *
     * @param timeOut
     */
    public void setTimeOut(boolean timeOut);
}
