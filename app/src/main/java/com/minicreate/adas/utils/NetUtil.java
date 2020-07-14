package com.minicreate.adas.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.ArrayList;
import java.util.List;

public class NetUtil {
    private static final String TAG = "NetUtil";
    private static Context mContext = null;
    public static String mPrevSsid = null;

    /**
     * 反转义，0x7e和0x7d版本
     *
     * @param escapedBuffer
     * @return
     */
    public static byte[] unescape_0x7e_0x7d(byte[] escapedBuffer) {
        int num = 0;
        // 首先找出有几个7d
        for (int i = 0; i < escapedBuffer.length; i++) {
            if (escapedBuffer[i] == 0x7d) {
                num++;
            }
        }
        byte[] newBytes = new byte[escapedBuffer.length - num];
        int index = 0;

        for (int i = 0; i < escapedBuffer.length - 1; i++) {
            if (escapedBuffer[i] == 0x7d) {
                if (escapedBuffer[i + 1] == 0x01) {
                    // 转换成0x7d
                    newBytes[index++] = 0x7d;
                    i++;
                } else if (escapedBuffer[i + 1] == 0x02) {
                    // 转换为0x7e
                    newBytes[index++] = 0x7e;
                    i++;
                } else {
                    // 错误字符
                    LogUtil.e(TAG, "错误字符 array[" + i + "] = " + escapedBuffer[i] + " ,array[" + (i + 1) + "] = "
                            + escapedBuffer[i + 1]);
                }
            } else {
                newBytes[index++] = escapedBuffer[i];
            }
        }
        // 还要记得补上最后一个
        newBytes[index++] = escapedBuffer[escapedBuffer.length - 1];
        return newBytes;
    }

    /**
     * 累加和校验
     *
     * @param b
     * @param start 包含
     * @param end   不包含
     * @return
     */
    public static int jiaoyan(byte[] b, int start, int end) {
        byte a = b[start];
        for (int i = start + 1; i < end; i++) {
            a += b[i];
        }
        return (a & 0xff);
    }

    /**
     * 异或校验和
     *
     * @param b
     * @param start 包含
     * @param end   不包含
     * @return
     */
    public static int xor_jiaoyan(byte[] b, int start, int end) {
        byte a = b[start];
        for (int i = start + 1; i < end; i++) {
            a ^= b[i];
        }
        return (a & 0xff);
    }

    /**
     * 寻找协议数据，协议头尾相同
     *
     * @return
     */
    public static List<byte[]> findPackageBySameHeadAndEnd(byte[] src, int head) {
        List<byte[]> resultList = new ArrayList<>();
        boolean findHead = false;
        int headIndex = 0;
        for (int i = 0; i < src.length; i++) {
            if (src[i] == head) {
                if (!findHead) {
                    //遇到了协议头
                    findHead = true;
                    headIndex = i;
                } else {
                    //遇到了协议尾
                    findHead = false;
                    byte[] tmp = new byte[i - headIndex + 1];
                    System.arraycopy(src, 0, tmp, 0, tmp.length);
                    resultList.add(tmp);
                }
            }
        }
        return resultList;
    }

    /**
     * 寻找协议数据，协议头尾不同。
     * 如果协议内容中没有对协议头尾相同的数据进行转义的话，那么就不要用这个方法
     * 比如点创的协议（0x24）协议头尾分别是0x24和0x0A，但是并没有对两者进行转义，这个方法有可能截取到错误数据
     *
     * @return
     */
    public static List<byte[]> findPackageByDifferentHeadAndEnd(byte[] src, int head, int end) {
        LogUtil.d(TAG, "src = " + BytesUtil.BytestoHexStringPrintf(src));
        List<byte[]> resultList = new ArrayList<>();
        boolean findHead = false;
        int headIndex = 0;
        for (int i = 0; i < src.length; i++) {
            if (src[i] == head) {
                findHead = true;
                headIndex = i;
            } else if (src[i] == end) {
                //找到协议尾之后，将头尾之间的数据抽取出来
                if (findHead) {
                    byte[] tmp = new byte[i - headIndex + 1];
                    System.arraycopy(src, headIndex, tmp, 0, tmp.length);
                    LogUtil.d(TAG, "\ttmp = " + BytesUtil.BytestoHexStringPrintf(tmp));
                    resultList.add(tmp);
                } else {
                    //这种情况是，找到了协议尾却没找到协议头，是废数据
                }
                //重新将findHead置位false
                findHead = false;
            }
        }
        return resultList;
    }

    public static boolean isAvailable(Context context) {
        if (mContext == null) {
            return false;
        }
        boolean result = false;
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.getActiveNetworkInfo();
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null) {
                return result = networkInfo.isAvailable();
            }
        }

        return result;
    }

    private static String getSSIDWithoutQuotation(String ssidWithQuotation) {
        if (ssidWithQuotation == null) {
            return null;
        }
        if (ssidWithQuotation.startsWith("\"") && ssidWithQuotation.endsWith("\"")) {
            ssidWithQuotation = ssidWithQuotation.substring(1, ssidWithQuotation.length() - 1);
        }
        return ssidWithQuotation;
    }

    public static final int NETWORN_NONE = 0;
    public static final int NETWORN_MOBILE = 1;
    public static final int NETWORN_WIFI = 2;
    public static final int NETWORN_BOTH = 3;
    public static int mPrevNetStatus = NETWORN_NONE;
}
