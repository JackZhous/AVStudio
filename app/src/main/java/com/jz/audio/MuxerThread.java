package com.jz.audio;

/**
 * @author jackzhous
 * @package com.jz.audio
 * @filename MuxerThread
 * date on 2019/4/11 11:28 AM
 * @describe 合并音视频线程
 * @email jackzhouyu@foxmail.com
 **/
public class MuxerThread {

    private AudioThread audioThread;
    private VideoThread videoThread;


    public MuxerThread() {

        audioThread = new AudioThread(this);

    }
}
