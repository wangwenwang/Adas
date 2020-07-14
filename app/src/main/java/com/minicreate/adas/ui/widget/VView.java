package com.minicreate.adas.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.ffmpeg.H264Decode;
import com.minicreate.adas.control.VideoDataHandler;
import com.minicreate.adas.model.VideoData;
import com.minicreate.adas.utils.LogUtil;

import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VView extends View implements Runnable {

    private final String TAG = "VView";

    Bitmap mBitQQ = null;

    Paint mPaint = null;

    Bitmap mSCBitmap = null;

    //PlayerDB.Settings settings = PlayerDB.getInstance(Setting.this).getSettings();

    int width = 352;   //default 
    int height = 288; //default
    int maxWidth = 640;
    int maxHeight = 480;
    byte[] mPixel = null;
    int[] mIntPixel = null;
    int[] mInitPixel = null;
    // ByteBuffer buffer = null;//ByteBuffer.wrap( mPixel );
    Bitmap VideoBit = null;//Bitmap.createBitmap(width, height, Config.RGB_565);
    //Bitmap VideoBitDest = Bitmap.createBitmap(320, 240, Config.RGB_565);

    int mTrans = 0x0F0F0F0F;

    String PathFileName;
    //private int displayWidth;
    //private int displayHeight;
    long lastDecodeTime = System.currentTimeMillis();
    protected Queue<VideoData> m_framePool = new ConcurrentLinkedQueue<VideoData>();

//    public native int InitDecoder(int width, int height);
//    public native int UninitDecoder();
//    public native int DecoderNal(byte[] in, int insize, byte[] out);

    private boolean mStop = false;
    private boolean mPlayEnd = true;
    private long lastShowTime = 0;

    private Handler mHandler;
    private Context mContext;
    private long sleepTime = 0;
    private boolean isPrior = false;
    static final int MAX_TIME_INTERVAL_MS = 2000000;
    private int rejectNum = 0;
    private int totalNum = 0;
    //    static {
//        System.loadLibrary("H264Android");
//    }
    private H264Decode mDecode;

    public VView(Context context) {
        super(context);
        mContext = context;
        setFocusable(true);
        initVideoInfo(context);
        /*
        int i = mPixel.length;

        for (i = 0; i < mPixel.length; i++) {
            mPixel[i] = (byte) 0x00;
        }
        */
    }

    public void setVideoScale(int width, int height) {
        LayoutParams lp = getLayoutParams();
        lp.height = height;
        lp.width = width;
        Log.e(TAG, "videoScale " + " width = " + width + " heght = " + height);
        setLayoutParams(lp);
    }

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    public final int getVideoWidth() {
        return width;
    }

    public final int getVideoHeight() {
        return height;
    }

    public VView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initVideoInfo(context);
        /*
        int i = mPixel.length;

        for (i = 0; i < mPixel.length; i++) {
            mPixel[i] = (byte) 0x00;
        }
        */
    }

    public VView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initVideoInfo(context);
        /*
        int i = mPixel.length;

        for (i = 0; i < mPixel.length; i++) {
            mPixel[i] = (byte) 0x00;
        }
        */
    }

    private void initVideoInfo(Context context) {
//		PlayerDB.Settings settings = PlayerDB.getInstance(context).getSettings();
//		if (settings.resolution.endsWith("QCIF")) {
//			width = 176;
//			height = 144;
//		}
//		else if (settings.resolution.endsWith("CIF")) {
//			width = 352;
//			height = 288;
//		}
//		if (settings.resolution.endsWith("QVGA")) {
//			width = 320;
//			height = 240;
//		}
//		if (settings.resolution.endsWith("VGA")) {
//			width = 640;
//			height = 480;
//		}


        //mPixel = new byte[maxWidth * maxHeight * 3];
        mIntPixel = new int[maxWidth * maxHeight * 3];
        //mInitPixel = new int[maxWidth * maxHeight * 3];
        //  buffer = ByteBuffer.wrap( mPixel );
        VideoBit = Bitmap.createBitmap(width, height, Config.RGB_565);
    }

    public void PlayVideo(String file) {
        PathFileName = file;

        new Thread(this).start();
    }

    public void startPlayThread() {
        new Thread(this).start();
    }

    public void stopPlayThread() {
        mStop = true;
        while (!mPlayEnd) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "play thread end!!");
    }

    public synchronized VideoData popData() {
        //return m_framePool.poll();
        return VideoDataHandler.getInstence().popVideoData();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long start = System.currentTimeMillis();
        //mDecode.GetPixel(1, mIntPixel);
        VideoBit.setPixels(mIntPixel, 0, width, 0, 0, width, height);
        LayoutParams lp = getLayoutParams();
        Bitmap mBitmap = Bitmap.createScaledBitmap(VideoBit, lp.width, lp.height, false);
        LogUtil.d(TAG, "onDraw mBitmap: " + mBitmap.getByteCount()+" width::"+lp.width+" height::"+lp.height);
        canvas.drawBitmap(mBitmap, 0, 0, null);
        mBitmap.recycle();//回收图片

        lastShowTime = System.currentTimeMillis() - start;
    }

    int MergeBuffer(byte[] NalBuf, int NalBufUsed, byte[] SockBuf, int SockBufUsed, int SockRemain) {
        int i = 0;
        byte Temp;

        for (i = 0; i < SockRemain; i++) {
            Temp = SockBuf[i + SockBufUsed];
            NalBuf[i + NalBufUsed] = Temp;

            mTrans <<= 8;
            mTrans |= Temp;

            if (mTrans == 1) // 找到一个开始字
            {
                i++;
                break;
            }
        }

        return i;
    }

    void decode() {
        InputStream is = null;
        int iTemp = 0;
        int nalLen;

        boolean bFirst = true;
        boolean bFindPPS = true;

        int bytesRead = 0;
        int NalBufUsed = 0;
        int SockBufUsed = 0;

        //byte[] NalBuf = new byte[40980]; // 40k
        //	byte [] SockBuf = new byte[2048];

        boolean hasFindIDR = false;
        mDecode.Initialize(1);
        //InitDecoder(width, height);
        boolean invalidPixelWarning = false;
        long lastTimeStamp = 0;
        long lastDecodeTime = 0;
        while (!mStop) {
            if (VideoDataHandler.getInstence().getVideoQueueSize() == 0 ||
                    VideoDataHandler.getInstence().isBuffering()) {
                try {
        			Log.d(TAG,"is buffer");
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            //Log.d(TAG,"video size = " +VideoDataHandler.getInstence().getVideoQueueSize() );
            VideoData data = popData();
            bytesRead = data.size;

            boolean bIsCanPlay = true;
            boolean needDrop = false;

            long startTime = System.currentTimeMillis();
            //iTemp=DecoderNal(NalBuf, NalBufUsed-4, mPixel);
            iTemp = mDecode.DecodeOneFrame(1, data.buffer, 0, data.size);
            int getpix = mDecode.GetPixel(1, mIntPixel);
            if (iTemp > 0) {
                final int w = mDecode.GetWidth(1);
                final int h = mDecode.GetHeight(1);
                if ((w != width || h != height) && mHandler != null) {
                    Log.e(TAG, "recreate bitmap ,w = " + w + " ,h = " + h + " ,width = " + width + " ,height = " + height);
                    //视频分辨率以服务器传过来的为主，不同的话就重新构建Bitmap
                    width = w;
                    height = h;
                    VideoBit = Bitmap.createBitmap(width, height, Config.RGB_565);
//	        		Intent intent = new Intent(ZPlayerBroadcast.INVALID_VIDEO_SIZE);
//	        		intent.putExtra("msg", "当前图像设置错误，图像的实际大小为"+w+"x"+h+",请在设置中进行修改");
//	    	    	mContext.sendBroadcast(intent);
                }
            }
            //Log.d(TAG,"lastDecodeTime = " + (System.currentTimeMillis() - startTime) );
            /*if (!VideoDataHandler.getInstence().hasAudio())
            {
                lastDecodeTime = System.currentTimeMillis() - startTime;
                //long sleep = lastTimeStamp==0? (200-lastDecodeTime):((data.timestamp - lastTimeStamp)/1000  -lastDecodeTime);
                long sleep = 200 - lastDecodeTime - 10;
                if (sleep > 0) {
                    SystemClock.sleep(sleep);
                    Log.d(TAG,"need sleep =" + sleep);
                }
                lastTimeStamp = data.timestamp;
            }
            */

//			 if (!needDrop)
            postInvalidate();  //使用postInvalidate可以直接在线程中更新界面    // postInvalidate();
        }

        //最后把mIntPixel（注意下面的两个数组名字是不同的）清空，让屏幕清空
        //System.arraycopy(mInitPixel, 0, mIntPixel, 0, mIntPixel.length);
        for (int i = 0; i < mIntPixel.length; i++) {
            mIntPixel[i] = (byte) 0x00;
        }
        postInvalidate();
        mDecode.Destory(1);
        //UninitDecoder();
        mPlayEnd = true;

    }

    private boolean isCanPlayVideoEx(long videoTime) {
        boolean bRet = false;

        //	视频的时间戳 － 音频的时间戳 <= 300毫秒时，播放
        isPrior = false;
        long iDis = VideoDataHandler.getInstence().getAudioTimeStamp() - videoTime;
        //Log.d(tag,"difference time = " + iDis + " audio time = "  + VideoDataHandler.getInstence().getAudioTimeStamp() + " video time = " + videoTime);

        if (Math.abs(iDis) <= MAX_TIME_INTERVAL_MS) {
            if (iDis < 0) {
                sleepTime = Math.abs(iDis);
                isPrior = true;
            } else {
                sleepTime = Math.abs(iDis);
            }
        } else {//在不能容许的范围之内，差距很大
            if (iDis < 0) //视频超前
            {
                bRet = true;
                isPrior = true;
                sleepTime = Math.abs(iDis);
            } else { //      视频严重落后
                bRet = false; //
                rejectNum++;
                float fRejectRate = (float) rejectNum / (float) totalNum;
                sleepTime = Math.abs(iDis);
            }
        }
        return bRet;

    }

    public void run() {
        mStop = false;
        mPlayEnd = false;
        decode();
    }


}

