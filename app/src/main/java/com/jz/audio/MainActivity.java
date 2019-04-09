package com.jz.audio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.jz.audio.GlobalConfig.AUDIO_FORMAT;
import static com.jz.audio.GlobalConfig.CHANNEL_CONFIG;
import static com.jz.audio.GlobalConfig.SAMPLE_RATE_INHZ;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MY_PERMISSIONS_REQUEST = 1001;
    private static final String TAG = "jqd";

    private Button mBtnControl;
    private Button mBtnPlay;

    /**
     * 需要申请的运行时权限
     */
    private String[] permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    /**
     * 被用户拒绝的权限列表
     */
    private List<String> mPermissionList = new ArrayList<>();
    private boolean isRecording;
    private AudioRecord audioRecord;
    private Button mBtnConvert;
    private AudioTrack audioTrack;
    private byte[] audioData;
    private FileInputStream fileInputStream;

    private int finish = 1;
    private int error = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnControl = (Button) findViewById(R.id.btn_control);
        mBtnControl.setOnClickListener(this);
        mBtnConvert = (Button) findViewById(R.id.btn_convert);
        mBtnConvert.setOnClickListener(this);
        mBtnPlay = (Button) findViewById(R.id.btn_play);
        mBtnPlay.setOnClickListener(this);

        checkPermissions();
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    Toast.makeText(MainActivity.this, "完成", Toast.LENGTH_SHORT).show();
                    return;

                case -1:
                    Toast.makeText(MainActivity.this, "失败", Toast.LENGTH_SHORT).show();
                    return;
            }
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_control:
                start("test.mp4", "test1.mp4");
                break;
            case R.id.btn_convert:
                startHasNoise("test.mp4", "test1.mp4");
                break;
            default:
                break;
        }
    }

    private void start(final String src, final String target){
        new Thread(){
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void run() {
                copyMp4(src, target);
            }
        }.start();
    }

    private void startHasNoise(final String src, final String target){
        new Thread(){
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void run() {
                copyMp4HasNoise(src, target);
            }
        }.start();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, permissions[i] + " 权限被用户禁止！");
                }
            }
            // 运行时权限的申请不是本demo的重点，所以不再做更多的处理，请同意权限申请。
        }
    }





    private void checkPermissions() {
        // Marshmallow开始才用申请运行时权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(this, permissions[i]) !=
                        PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);
                }
            }
            if (!mPermissionList.isEmpty()) {
                String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);
                ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST);
            }
        }
    }


    //有声视频
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void copyMp4HasNoise(String srcFIle, String targetFile){
        MediaExtractor extractor = null;
        MediaMuxer muxer = null;
        try {

            String basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            //分离器 可以将一个视频分离为音频和视频
            extractor = new MediaExtractor();
            extractor.setDataSource(basePath+"/"+srcFIle);

            int videoTrackIndex = -1;  //通道
            int frameRate = 0;  //帧率
            //得到源文件通道数
            for(int i = 0; i < extractor.getTrackCount(); i++){
                MediaFormat format = extractor.getTrackFormat(i);       //获取该通道的格式，视频、音频等
                String type = format.getString(MediaFormat.KEY_MIME);
                Log.i("j_tag", "channel " + i + "type  " + type);
//                if(!type.startsWith("video/")){          //必须是视频流
//                    continue;
//                }
                System.out.print("channel " + i + " " + type);
                frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);      //获取帧率 音频
                Log.i("j_tag", "channel " + i + " rate " + frameRate);

                extractor.selectTrack(i);                                       //指定通道
                if(muxer == null){
                    muxer = new MediaMuxer(basePath + "/" + targetFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                }
                videoTrackIndex = muxer.addTrack(format);
                muxer.start();

                //没有视频
//                if(muxer == null){
//                    System.out.print("muxer null");
//                    continue;
//                }

                //写入文件
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                info.presentationTimeUs = 0;
                ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);        //缓存区500k
                int sampleSize = 0;
                //readSampleData
                while ((sampleSize = extractor.readSampleData(buffer, 0)) >0){
                    info.offset = 0;
                    info.size = sampleSize;
                    info.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;     //这数据包含关键帧  乱猜的
                    info.presentationTimeUs += 1000 * 1000 / frameRate;     //时间戳
                    //将数据写入到指定通道去
                    muxer.writeSampleData(videoTrackIndex, buffer, info);
                    //读取下一帧
                    extractor.advance();        //
                }

                handler.sendEmptyMessage(finish);

            }




        } catch (IOException e) {
            e.printStackTrace();
            handler.sendEmptyMessage(error);
        } finally {
            if(extractor != null){
                extractor.release();
            }

            if(muxer != null){
                muxer.stop();
                muxer.release();
            }
        }

    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void copyMp4(String srcFIle, String targetFile){
        MediaExtractor extractor = null;
        MediaMuxer muxer = null;
        try {

            String basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            //分离器 可以将一个视频分离为音频和视频
            extractor = new MediaExtractor();
            extractor.setDataSource(basePath+"/"+srcFIle);

            int videoTrackIndex = -1;  //通道
            int frameRate = 0;  //帧率
            //得到源文件通道数
            for(int i = 0; i < extractor.getTrackCount(); i++){
                MediaFormat format = extractor.getTrackFormat(i);       //获取该通道的格式，视频、音频等
                String type = format.getString(MediaFormat.KEY_MIME);
                Log.i("j_tag", "channel " + i + "type  " + type);
                if(!type.startsWith("video/")){          //必须是视频流
                    continue;
                }
                System.out.print("channel " + i + " " + type);
                frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);      //获取帧率
                Log.i("j_tag", "channel " + i + " rate " + frameRate);

                extractor.selectTrack(i);                                       //指定通道
                muxer = new MediaMuxer(basePath + "/" + targetFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                videoTrackIndex = muxer.addTrack(format);
                muxer.start();
            }
            //没有视频
            if(muxer == null){
                System.out.print("muxer null");
                return;
            }

            //写入文件
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            info.presentationTimeUs = 0;
            ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);        //缓存区500k
            int sampleSize = 0;
            //readSampleData
            while ((sampleSize = extractor.readSampleData(buffer, 0)) >0){
                info.offset = 0;
                info.size = sampleSize;
                info.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;     //这数据包含关键帧  乱猜的
                info.presentationTimeUs += 1000 * 1000 / frameRate;     //时间戳
                //将数据写入到指定通道去
                muxer.writeSampleData(videoTrackIndex, buffer, info);
                //读取下一帧
                extractor.advance();        //
            }

            handler.sendEmptyMessage(finish);

        } catch (IOException e) {
            e.printStackTrace();
            handler.sendEmptyMessage(error);
        } finally {
            if(extractor != null){
                extractor.release();
            }

            if(muxer != null){
                muxer.stop();
                muxer.release();
            }
        }

    }


}
