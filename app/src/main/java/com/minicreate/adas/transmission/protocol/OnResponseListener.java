package com.minicreate.adas.transmission.protocol;

/**
 * 数据回调
 *
 * @param
 * @author cjd
 */
public interface OnResponseListener {
    /**
     * 协议接收到之后，会回调该方法，resultSrc是原始数据，我们拿到手之后，应该进一步判断该数据是否完整，是否是我们需要的
     *
     * @param result
     */
    void onResponse(Param result);
}
