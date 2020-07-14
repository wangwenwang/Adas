package com.minicreate.adas.transmission.protocol;

/**
 * 抽象协议类，包含有超时判断方法
 */
public abstract class AbstractBaseParam implements Param {
    private boolean isTimeOut = false;//超时标记，false表示未超时，true表示超时

    public void setTimeOut(boolean timeOut) {
        isTimeOut = timeOut;
    }

    public boolean isTimeOut() {
        return isTimeOut;
    }
}
