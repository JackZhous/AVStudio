package com.jz.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Vector;

/**
 * @author jackzhous
 * @package com.jz.audio
 * @filename VideoThread
 * date on 2019/4/11 11:28 AM
 * @describe 视频处理线程
 * @email jackzhouyu@foxmail.com
 **/
public class VideoThread extends Thread{

    private boolean running = false;
    private MuxerThread muxer;


    public VideoThread(MuxerThread muxer) {
        this.muxer = muxer;
        running = true;

    }




    @Override
    public void run() {

        while (running){

        }

        recycle();
    }

    private void recycle(){
        muxer = null;
    }

    public void stopThread(){
        running = false;
    }
}
