package com.minicreate.adas.utils;

public class BytesUtil {
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
     * 把int转换成byte数组
     *
     * @param
     * @return 返回的byte数组
     */
    public static byte[] int2BytesArray(int n) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (n >> (24 - i * 8));
        }
        return b;
    }
}
