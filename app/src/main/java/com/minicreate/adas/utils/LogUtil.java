package com.minicreate.adas.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Log工具类
 */
public class LogUtil {
    private static final boolean DEBUG = true;//log开关，true表示打开，false表示关闭
    //只有这些Tag是需要显示的。TODO 暂时不使用
    private static ArrayList<String> tagList = new ArrayList<>();

    static {
        tagList.add("TestActivity");
        tagList.add("App");
        tagList.add("WIFIEndpoint");
        tagList.add("all");
    }

    public LogUtil() {

    }

    public static void v(String tag, String msg) {
        if (!DEBUG) {
            return;
        }
        Log.v(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (!DEBUG) {
            return;
        }
        Log.d(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (!DEBUG) {
            return;
        }
        Log.e(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (!DEBUG) {
            return;
        }
        Log.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (!DEBUG) {
            return;
        }
        Log.w(tag, msg);
    }

    /**
     * 将log保存到sdcard中，默认存放路径为Vehicle_DC_log
     * <p>
     * 发布给用户的时候得记得关闭，自己测试需要抓取log的时候再打开
     * <p>
     * 除了日志之外，还包括tag以及日志等级，还得加上时间
     * <p>
     * 只接收特定类型的tag，不然的话log文件会相当大。
     *
     * @param tag
     * @param message
     */
    public static void saveLogToSDCard(String level, String tag, String message) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "Onlinetaxi_log");
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStream outputStream = new FileOutputStream(file, true);
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            }
            outputStream.write(("[" + dateFormat.format(System.currentTimeMillis()) + " " + level + "/" + tag + "] : " + message + "\n").getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
