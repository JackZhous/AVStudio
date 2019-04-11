package com.jz.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author jackzhous
 * @package com.jz.audio
 * @filename AudioThread
 * date on 2019/4/11 11:28 AM
 * @describe 音频录制线程
 * @email jackzhouyu@foxmail.com
 **/
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class AudioThread extends Thread{

    private static final String TAG = "audio";
    private boolean running = false;
    private MuxerThread muxer;              //混合器
    private AudioRecord record;             //录音器
    private MediaCodec  mCodec;             //编码器
    private MediaFormat audioFormat;        //编码参数配置

    //录音配置参数
    private int sample_rate = 44100;            //采样率，每秒从音源中采集多少个数据
    private static final int BIT_RATE = 64000;
    private int channel_config = AudioFormat.CHANNEL_IN_MONO;
    private int audio_format = AudioFormat.ENCODING_PCM_16BIT;      //输出格式
    private String mimeType = "audio/mp4a-latm";            //编码格式

    /**
     * 缓存数据
     */
    private ByteBuffer bytes;
    private int SAMPLE_LENGTH_PER = 1024;
    private long preOutput = 0;

    public AudioThread(MuxerThread muxer) {
        this.muxer = muxer;
        running = true;
        init();
    }

    private void initMediaCodec() throws IOException {
        checkEncodeSupport();
        //1是通道数
        audioFormat = MediaFormat.createAudioFormat(mimeType, sample_rate, 1);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sample_rate);
        //未初始化状态
        mCodec = MediaCodec.createEncoderByType(mimeType);
        //配置状态
        mCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //executing status
        mCodec.start();
        Log.i(TAG, "audio start....");
    }


    /**
     * 检查是硬件编码是否支持
     */
    private void checkEncodeSupport(){
        MediaCodecInfo info = null;

        int countSize = MediaCodecList.getCodecCount();
        for(int i = 0; i <countSize; i++){
            info = MediaCodecList.getCodecInfoAt(i);

            if(!info.isEncoder()){
                return;
            }

            String types[] = info.getSupportedTypes();
            for(String type : types){
                if(type.equalsIgnoreCase(mimeType)){
                    Log.i(TAG, "audio encoder " + type);
                    return;
                }
            }
        }
        Log.i(TAG, "not support " + mimeType + "encode");
        throw new RuntimeException("not support "+mimeType);
    }

    private void init(){
        int bufferSize = AudioRecord.getMinBufferSize(sample_rate, channel_config, audio_format);
        record = new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate, channel_config,
                audio_format, bufferSize);
        //bufferSize大长了，会造成数组很大
        bytes = ByteBuffer.allocate(SAMPLE_LENGTH_PER);
        Log.i("j_tag", "min buffer size " + bufferSize);

        try {
            initMediaCodec();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "audio codec error");
        }
    }

    @Override
    public void run() {
        if(record == null){
            return;
        }
        record.startRecording();
        int len = 0;
        while (running){
            bytes.clear();
            len = record.read(bytes, SAMPLE_LENGTH_PER);
            if(len > 0){
                bytes.position(len);
                bytes.flip();           //将bytes数组限制在当前位置大小
                encodeData(bytes, len, getCurrentTime());
            }

        }

        recycle();
    }


    /**
     * 编码音频数据
     * @param data
     * @param len
     * @param time
     */
    private void encodeData(ByteBuffer data, int len, long time){

    }


    /**
     * 不理解
     * @return
     */
    private long getCurrentTime(){
        long time = System.nanoTime() / 1000L;
        if(time < preOutput){
            time = ( preOutput - time ) + time;
        }
        return time;
    }

    private void recycle(){
        record.stop();
        record.release();
        muxer = null;
        record = null;
    }


    public void stopThread(){
        running = false;
    }
}
