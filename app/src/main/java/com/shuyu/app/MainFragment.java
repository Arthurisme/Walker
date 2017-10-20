package com.shuyu.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.czt.mp3recorder.MP3Recorder;
import com.piterwilson.audio.MP3RadioStreamDelegate;
import com.piterwilson.audio.MP3RadioStreamPlayer;
import com.shuyu.waveview.AudioPlayer;
import com.shuyu.waveview.AudioWaveView;
import com.shuyu.waveview.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;



/**
 * Created by shuyu on 2016/12/16.
 */

public class MainFragment extends Fragment implements MP3RadioStreamDelegate, View.OnClickListener {

    private static final String LOG_TAG = MainFragment.class.getSimpleName();

    //    @BindView(R.id.audioWave)
    AudioWaveView audioWaveForRecord;
//    @BindView(R.id.record)
    Button record;
//    @BindView(R.id.stop)
    Button stop;
//    @BindView(R.id.play)
    Button play;
//    @BindView(R.id.reset)
    Button reset;
//    @BindView(R.id.wavePlay)
    Button wavePlay;
//    @BindView(R.id.playText)
    TextView playSimpleInfoText;
//    @BindView(R.id.colorImg)
    ImageView colorImg;
//    @BindView(R.id.recordPause)
    Button recordPause;


    MP3Recorder mRecorder;
    AudioPlayer audioPlayerSimple;

    String filePath;

    boolean mIsRecord = false;

    boolean mIsPlaySimple = false;

    int duration;
    int curPosition;

    //    @BindView(R.id.audioWave)
    AudioWaveView audioWaveForPlay;
    //    @BindView(R.id.activity_wave_play)
    RelativeLayout wave_and_info_section;
    //    @BindView(R.id.playBtn)
    Button playBtn;
    //    @BindView(R.id.seekBar)
    SeekBar seekBar;


    MP3RadioStreamPlayer player;

    Timer timer;

    boolean playeEnd;

    boolean seekBarTouch;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
//        ButterKnife.bind(this, view);


        //binding view:
        audioWaveForRecord = (AudioWaveView)view.findViewById (R.id.audioWaveForRecord);
        record=(Button)view.findViewById(R.id.record);
        record.setOnClickListener(this);
        stop=(Button)view.findViewById(R.id.stop);
        stop.setOnClickListener(this);

        play=(Button)view.findViewById(R.id.play_without_waveform);
        play.setOnClickListener(this);

        reset=(Button)view.findViewById(R.id.reset);
        reset.setOnClickListener(this);

        wavePlay   =(Button)view.findViewById(R.id.wavePlay);
        wavePlay.setOnClickListener(this);

        playSimpleInfoText =(TextView)view.findViewById(R.id.playText);
        colorImg   =(ImageView)view.findViewById(R.id.colorImg);
        recordPause   =(Button)view.findViewById(R.id.recordPause);
        recordPause.setOnClickListener(this);


        //binding view play wave:
        audioWaveForPlay = (AudioWaveView)view.findViewById (R.id.audioWaveForPlay);
        wave_and_info_section =(RelativeLayout)view.findViewById(R.id.wave_and_info_section);
        playBtn=(Button)view.findViewById(R.id.playBtn);
        playBtn.setOnClickListener(this);

        seekBar=(SeekBar)view.findViewById(R.id.seekBar);





//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                play();
//            }
//        }, 1000);
//        playBtn.setEnabled(false);
//        seekBar.setEnabled(false);


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBarTouch = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarTouch = false;
                if (!playeEnd) {
                    player.seekTo(seekBar.getProgress());
                }
            }
        });

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (playeEnd || player == null || !seekBar.isEnabled()) {
                    return;
                }
                long position = player.getCurPosition();
                if (position > 0 && !seekBarTouch) {
                    seekBar.setProgress((int) position);
                }
            }
        }, 1000, 1000);




        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        resolveNormalUI();

        audioPlayerSimple = new AudioPlayer(getActivity(), new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case AudioPlayer.HANDLER_CUR_TIME://更新的时间
                        curPosition = (int) msg.obj;
                        playSimpleInfoText.setText(toTime(curPosition) + " / " + toTime(duration));
                        break;
                    case AudioPlayer.HANDLER_COMPLETE://播放结束
                        playSimpleInfoText.setText(" ");
                        mIsPlaySimple = false;
                        break;
                    case AudioPlayer.HANDLER_PREPARED://播放开始
                        duration = (int) msg.obj;
                        playSimpleInfoText.setText(toTime(curPosition) + " / " + toTime(duration));
                        break;
                    case AudioPlayer.HANDLER_ERROR://播放错误
                        resolveResetPlay();
                        break;
                }

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy:0::release test 1");



        if (timer != null) {
            Log.d(LOG_TAG, "onDestroy:0::release test 2");

            timer.cancel();
            timer = null;
        }
        Log.d(LOG_TAG, "onDestroy:0::release test 3");

        stopPlayer();
        Log.d(LOG_TAG, "onDestroy:0::release test 4");

        releasePlayer();
        Log.d(LOG_TAG, "onDestroy:0::release test 5");

        audioWaveForPlay.stopView();


    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause:0::release test 1");

        if (mIsRecord) {
            Log.d(LOG_TAG, "onPause:0::release test 2");

            resolveStopRecord();
        }
        if (mIsPlaySimple) {
            Log.d(LOG_TAG, "onPause:0::release test 2");

            audioPlayerSimple.pause();
            audioPlayerSimple.stop();
        }
    }


    @Override
    public void onClick(View view) {
        Log.d(LOG_TAG, "onClick:1");


        switch (view.getId())
        {
            case R.id.record:
            {

                Log.d(LOG_TAG, "onClick:record:0");
                resolveRecord();
                audioWaveForPlay.setVisibility(View.GONE);
                seekBar.setVisibility(View.GONE);


            }
                break;

            case R.id.recordPause:
            {
                Log.d(LOG_TAG, "onClick:recordPause:0");

                resolvePause();

            }
            break;

            case R.id.stop:
                {
                Log.d(LOG_TAG, "onClick:stop:0");
                resolveStopRecord();

                    this.getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            playeEnd = true;
                            playBtn.setText("Play");
                            playBtn.setEnabled(true);
                            seekBar.setEnabled(false);
                        }
                    });
            }
                break;
            case R.id.play_without_waveform:
            {
                Log.d(LOG_TAG, "onClick:play:0");
                resolvePlaySimple();

            }
                break;
            case R.id.reset:
            {
                Log.d(LOG_TAG, "onClick:reset:0");
                resolveResetPlay();

            }
            break;

            case R.id.wavePlay:
            {
                Log.d(LOG_TAG, "onClick:wavePlay:0");
                resolvePlayWaveRecordInNewActivity();
            }
            break;




            case R.id.playBtn:
            {




                Log.d(LOG_TAG, "onClick:playBtn:0");


//                {
//                    Log.d(LOG_TAG, "playBtn:T0");
//
//                    playBtn.setText("暂停");
//                    seekBar.setEnabled(true);
//                    play();
//
//
//
//                }

                if (playeEnd) {
                    Log.d(LOG_TAG, "playBtn:1");

                    stopPlayer();
                    playBtn.setText("Pause Record");
                    seekBar.setEnabled(true);
                    play();
                    audioWaveForPlay.setVisibility(View.VISIBLE);
                    seekBar.setVisibility(View.VISIBLE);
                    return;
                }

                if(player!=null){
                    if (player.isPause()) {
                        Log.d(LOG_TAG, "playBtn:2");

                        playBtn.setText("Pause Record");
                        player.setPause(false);
                        seekBar.setEnabled(false);
                    } else {
                        Log.d(LOG_TAG, "playBtn:3");

                        playBtn.setText("Play");
                        player.setPause(true);
                        seekBar.setEnabled(true);
                    }
                }else {
                    Toast.makeText(getActivity(), "The player is not yet started.", Toast.LENGTH_SHORT).show();

                }













            }
                break;
        }






    }

    /**
     * 开始录音
     */
    private void resolveRecord() {
        filePath = FileUtils.getAppPath();
        File file = new File(filePath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Toast.makeText(getActivity(), "Fail to create the file", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int offset = dip2px(getActivity(), 5);
        filePath = FileUtils.getAppPath() + UUID.randomUUID().toString() + ".mp3";
        mRecorder = new MP3Recorder(new File(filePath));
        int size = getScreenWidth(getActivity()) / offset;//控件默认的间隔是1
        mRecorder.setDataList(audioWaveForRecord.getRecList(), size);

        //高级用法
        //int size = (getScreenWidth(getActivity()) / 2) / dip2px(getActivity(), 1);
        //mRecorder.setWaveSpeed(600);
        //mRecorder.setDataList(audioWave.getRecList(), size);
        //audioWave.setDrawStartOffset((getScreenWidth(getActivity()) / 2));
        //audioWave.setDrawReverse(true);
        //audioWave.setDataReverse(true);

        //自定义paint
        //Paint paint = new Paint();
        //paint.setColor(Color.GRAY);
        //paint.setStrokeWidth(4);
        //audioWave.setLinePaint(paint);
        //audioWave.setOffset(offset);

        mRecorder.setErrorHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == MP3Recorder.ERROR_TYPE) {
                    Toast.makeText(getActivity(), "No mic  permission", Toast.LENGTH_SHORT).show();
                    resolveError();
                }
            }
        });

        //audioWave.setBaseRecorder(mRecorder);

        try {
            mRecorder.start();
            audioWaveForRecord.startView();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "error on recording", Toast.LENGTH_SHORT).show();
            resolveError();
            return;
        }
        resolveRecordUI();
        mIsRecord = true;
    }

    /**
     * 停止录音
     */
    private void resolveStopRecord() {
        resolveStopUI();
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.setPause(false);
            mRecorder.stop();
            audioWaveForRecord.stopView();
        }
        mIsRecord = false;
        recordPause.setText("Pause Record");

    }

    /**
     * 录音异常
     */
    private void resolveError() {
        resolveNormalUI();
        FileUtils.deleteFile(filePath);
        filePath = "";
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.stop();
            audioWaveForRecord.stopView();
        }
    }

    /**
     * 播放
     */
    private void resolvePlaySimple() {
        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            Toast.makeText(getActivity(), "No file find", Toast.LENGTH_SHORT).show();
            return;
        }
        playSimpleInfoText.setText(" ");
        mIsPlaySimple = true;
        audioPlayerSimple.playUrl(filePath);
        resolvePlaySimpleUI();
    }

    /**
     * 播放
     */
    private void resolvePlayWaveRecordInNewActivity() {
        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            Toast.makeText(getActivity(), "No file find", Toast.LENGTH_SHORT).show();
            return;
        }
        resolvePlaySimpleUI();
        Intent intent = new Intent(getActivity(), WavePlayActivity.class);
        intent.putExtra("uri", filePath);
        startActivity(intent);
    }

    /**
     * 重置
     */
    private void resolveResetPlay() {
        filePath = "";
        playSimpleInfoText.setText("");
        if (mIsPlaySimple) {
            mIsPlaySimple = false;
            audioPlayerSimple.pause();
        }
        resolveNormalUI();
    }

    /**
     * 暂停
     */
    private void resolvePause() {
        if (!mIsRecord)
            return;
        resolvePauseUI();
        if (mRecorder.isPause()) {
            resolveRecordUI();
            mRecorder.setPause(false);
            recordPause.setText("Pause Record");
        } else {
            mRecorder.setPause(true);
            recordPause.setText("Continu Record");
        }
    }

    private String toTime(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
        String dateString = formatter.format(time);
        return dateString;
    }

    private void resolveNormalUI() {
        record.setEnabled(true);
        recordPause.setEnabled(false);
        stop.setEnabled(false);
        play.setEnabled(false);
        wavePlay.setEnabled(false);
        reset.setEnabled(false);
    }

    private void resolveRecordUI() {
        record.setEnabled(false);
        recordPause.setEnabled(true);
        stop.setEnabled(true);
        play.setEnabled(false);
        wavePlay.setEnabled(false);
        reset.setEnabled(false);
    }

    private void resolveStopUI() {
        record.setEnabled(true);
        stop.setEnabled(false);
        recordPause.setEnabled(false);
        play.setEnabled(true);
        wavePlay.setEnabled(true);
        reset.setEnabled(true);
    }

    private void resolvePlaySimpleUI() {
        record.setEnabled(false);
        stop.setEnabled(false);
        recordPause.setEnabled(false);
        play.setEnabled(true);
        wavePlay.setEnabled(true);
        reset.setEnabled(true);
    }

    private void resolvePauseUI() {
        record.setEnabled(false);
        recordPause.setEnabled(true);
        stop.setEnabled(false);
        play.setEnabled(false);
        wavePlay.setEnabled(false);
        reset.setEnabled(false);
    }


    /**
     * 获取屏幕的宽度px
     *
     * @param context 上下文
     * @return 屏幕宽px
     */
    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.widthPixels;
    }

    /**
     * 获取屏幕的高度px
     *
     * @param context 上下文
     * @return 屏幕高px
     */
    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.heightPixels;
    }

    /**
     * dip转为PX
     */
    public static int dip2px(Context context, float dipValue) {
        float fontScale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * fontScale + 0.5f);
    }


    private void play() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        player = new MP3RadioStreamPlayer();
        //player.setUrlString(this, true, "http://www.stephaniequinn.com/Music/Commercial%20DEMO%20-%2005.mp3");
//        player.setUrlString(getActivity().getIntent().getStringExtra("uri"));
        player.setUrlString(filePath);
        player.setDelegate(this);

        int size = getScreenWidth(getActivity()) / dip2px(getActivity(), 1);//控件默认的间隔是1
        player.setDataList(audioWaveForPlay.getRecList(), size);

        //player.setStartWaveTime(5000);
        //audioWave.setDrawBase(false);
        audioWaveForPlay.setBaseRecorder(player);
        audioWaveForPlay.startView();
        try {
            player.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void stopPlayer() {
        Log.d(LOG_TAG, "stopPlayer:0::release test 1");
        Log.d(LOG_TAG, "stopPlayer:0::release test 2 player= "+player);

        if(player!=null){
            Log.d(LOG_TAG, "stopPlayer:0::release test 2");
//            player.setPause(true);// test 176.
            player.stop();

        }
    }

    private void releasePlayer() {
        Log.d(LOG_TAG, "releasePlayer:0::release test 1");

        if(player!=null){
            Log.d(LOG_TAG, "releasePlayer:0::release test 1");

            player.release();

        }
    }



    /****************************************
     * Delegate methods. These are all fired from a background thread so we have to call any GUI code on the main thread.
     ****************************************/

    @Override
    public void onRadioPlayerPlaybackStarted(final MP3RadioStreamPlayer player) {
        Log.i(LOG_TAG, "onRadioPlayerPlaybackStarted");
        this.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                playeEnd = false;
                playBtn.setEnabled(true);
                seekBar.setMax((int) player.getDuration());
                seekBar.setEnabled(true);
            }
        });
    }

    @Override
    public void onRadioPlayerStopped(MP3RadioStreamPlayer player) {
        Log.i(LOG_TAG, "onRadioPlayerStopped");
        if(this.getActivity()!=null){
            this.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    playeEnd = true;
                    playBtn.setText("Play");
                    playBtn.setEnabled(true);
                    seekBar.setEnabled(false);
                }
            });
        }


    }

    @Override
    public void onRadioPlayerError(MP3RadioStreamPlayer player) {
        Log.i(LOG_TAG, "onRadioPlayerError");
        this.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                playeEnd = false;
                playBtn.setEnabled(true);
                seekBar.setEnabled(false);
            }
        });

    }

    @Override
    public void onRadioPlayerBuffering(MP3RadioStreamPlayer player) {
        Log.i(LOG_TAG, "onRadioPlayerBuffering");
        this.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                playBtn.setEnabled(false);
                seekBar.setEnabled(false);
            }
        });

    }



}
