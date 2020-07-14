package com.minicreate.adas.control;

import android.util.Log;

import com.minicreate.adas.model.VideoData;
import com.minicreate.adas.utils.KMPMatch;
import com.minicreate.adas.utils.LogUtil;
import com.minicreate.adas.utils.NArchive;

import java.io.ByteArrayOutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VideoDataHandler {
    private static VideoDataHandler instence;
    protected Queue<VideoData> m_videoframePool = new ConcurrentLinkedQueue<VideoData>();
    protected Queue<VideoData> m_audioframePool = new ConcurrentLinkedQueue<VideoData>();
    private boolean foundStartCode = false;
    private byte[] h264Header = {0x00, 0x00, 0x00, 0x01};
    private byte[] frameHeader = {(byte) 0xA3, (byte) 0x9b, (byte) 0xd1, (byte) 0xe5};
    private String tag = "VideoDataHandler";
    private ByteArrayOutputStream mOutput = new ByteArrayOutputStream();
    private boolean saveFile = false;
    byte[] frameheader = new byte[32];
    private NArchive archiveH264;
    private long lastAudioTime = 0;
    private int lastAudioSequence = 0;
    private final int AUDIO_FRAME = 0;
    private final int I_FRAME = 1;
    private final int P_FRAME = 2;
    private boolean needBuffering = true;
    private boolean hasAudio = false;
    private static int bufferTime = 10; // seconds
    private boolean getIFrame = false;
    private long currentTimeStamp = 0;  //当前已经播放的时间戳

    public synchronized static VideoDataHandler getInstence() {
        if (instence == null) {
            instence = new VideoDataHandler();
//			PlayerDB.Settings settings = PlayerDB.getInstance(ZPlayerApp.getContext()).getSettings();
//			if (settings != null)
//				bufferTime = settings.audio_buffer_time;
            Log.d("audiobuffer", "audio buffer=" + bufferTime);
        }
        return instence;
    }

    public void setAudioBufferTime(int time) {
        bufferTime = time;
        Log.d(tag, "audio buffer = " + time);
    }

    private void saveFile(byte[] buffer, int len) {
        if (!saveFile) {
            archiveH264 = NArchive.createFile("/sdcard/test-onlyaudio.recv", false);
            archiveH264.open();
            saveFile = true;
        }
        archiveH264.writeBytes(buffer, len);
    }

    /*
     * adpcm audio len = 164
     */
    public synchronized void pushData(byte[] buffer, int len) {
        // saveFile(buffer,len);
        int index = 0;
        mOutput.write(buffer, 0, len);
        byte[] src = mOutput.toByteArray();
        if (!foundStartCode) {
            index = KMPMatch.indexOf(src, frameHeader);
            if (index != -1) {
                //foundStartCode=true;
                Log.i(tag, "found start code, index = " + index);
            } else {
                Log.e(tag, "cannot found startcode,data len =" + mOutput.size());
                mOutput.reset();
                return;
            }
        }
        //Log.d(tag,"dataSize = " + src.length + " index = " + index);
        int leave = mOutput.size() - index;
        LogUtil.i(tag, "leave: " + (leave - 32));
        while (true) {
            if (leave > 32)  //header size
            {
                System.arraycopy(src, index, frameheader, 0, 32);
                NArchive arIn = NArchive.createFromBuffer(frameheader, true);
                //		byte[] part =new byte[24];
                int mark = arIn.readInt();
                byte ch = arIn.readByte();
                byte codeType = arIn.readByte();
                byte resolution = arIn.readByte();
                byte vopType = arIn.readByte();
                if (vopType == AUDIO_FRAME) {
                    Log.d(tag, "codectype AUDIO_FRAME= " + codeType);
//					AudioPlay.audioCodec = codeType;
                    hasAudio = true;
                } else if (vopType == I_FRAME) {
                    Log.d(tag, "codectype I_FRAME = " + codeType);
                    getIFrame = true;
                }
                byte videMode = arIn.readByte();
                byte streamType = arIn.readByte();
                short rate = arIn.readShort();
                int dataSize = arIn.readInt();
                int sequence = arIn.readInt();
                int currenttime = arIn.readInt();
                long timestamp = arIn.readLong();
                LogUtil.i(tag, "dataSize: " + dataSize);
                if (leave - 32 >= dataSize) {
                    { //video
                        //Log.d(tag,"push video data ,size = " + (dataSize) + " timestamp = " + timestamp + "sequence = " + sequence + "  mark = " + Integer.toHexString(mark));
                        if (dataSize < 0) {
                            Log.e(tag, "video data exception,leave data = " + leave);
                            leave -= 32 + dataSize;
                            break;
                        }
                        Log.d(tag, "timestamp = " + timestamp + "     sequence = " + sequence + " videoSize = " + dataSize);
                        if (getIFrame) {
                            pushVideoData(src, index + 32, dataSize, timestamp);
                            if (getVideoQueueSize() >= 2 && getAudioQueueSize() == 0) {
                                hasAudio = false;
                                needBuffering = false;
                                Log.d(tag, "no audio");
                            }
                            Log.d(tag, "timestamp diff= " + (timestamp - lastAudioTime) + " video time = " + timestamp + " AudioTime = " + lastAudioTime);
                        }
                    }
                    index += (32 + dataSize);
                    leave -= (32 + dataSize);
                } else { //dataSize > leave-32
                    Log.w(tag, "data is not enough,need read more data...");
                    break;
                }
            } else {
                LogUtil.e(tag, " leve 无效");
            }
            break;
        }//end while
        if (leave > 0) {
            byte[] temp = new byte[leave];
            System.arraycopy(src, index, temp, 0, leave);
            mOutput.reset();
            mOutput.write(temp, 0, leave);
            //Log.d(tag,"leave Size = " + leave);
        } else {
            mOutput.reset();
        }
    }

    protected synchronized void pushVideoData(byte[] buffer, int offset, int len, long timestamp) {

        VideoData data = new VideoData();
        data.buffer = new byte[len];
        data.size = len;
        data.timestamp = timestamp;
        System.arraycopy(buffer, offset, data.buffer, 0, len);
        LogUtil.w(tag, "pushVideoData : " + len);
        m_videoframePool.add(data);
    }

    public synchronized void setAudioBuffer() {
        Log.d(tag, "setAudioBuffer");
        needBuffering = true;
    }

    protected synchronized void pushAudioData(byte[] buffer, int offset, int len, long timestamp, int sequence) {
        if (m_audioframePool.size() == 0) {
            needBuffering = true;
        }
        VideoData data = new VideoData();
        data.buffer = new byte[len];
        data.size = len;
        data.timestamp = timestamp;
        data.sequence = sequence;
        System.arraycopy(buffer, offset, data.buffer, 0, len);
        m_audioframePool.add(data);
        if (m_audioframePool.size() >= 1000 * bufferTime / 40 && needBuffering) {
            needBuffering = false;
        }

    }

    /*
     * get a data from queue;
     */
    public synchronized VideoData popVideoData() {
        if (needBuffering)
            return null;
        LogUtil.e(tag, "popVideoData poll");
        return m_videoframePool.poll();
    }

    /* get a data from queue;
     */
    public synchronized VideoData popAudioData() {
        if (needBuffering || !hasAudio) //only has video ?
            return null;
        if (m_audioframePool.size() == 0) {
            Log.d(tag, "audio size = 0,need buffer");
            needBuffering = true;
        }
        return m_audioframePool.poll();
    }


    public synchronized void clear() {
        foundStartCode = false;
        lastAudioTime = 0;
        lastAudioSequence = 0;
        getIFrame = false;
        m_videoframePool.clear();
        m_audioframePool.clear();
        mOutput.reset();
    }

    /*
     *
     * 是否有音频数据
     */
    public synchronized boolean hasAudio() {
        return hasAudio;
    }

    public synchronized boolean isBuffering() {
        return needBuffering;
    }

    public synchronized int getVideoQueueSize() {
        return m_videoframePool.size();
    }

    public synchronized int getAudioQueueSize() {
        return m_audioframePool.size();
    }

    public synchronized void setAudioTimeStamp(long timeStamp) {
        currentTimeStamp = timeStamp;
    }

    public synchronized long getAudioTimeStamp() {
        return currentTimeStamp;
    }

}