package com.example.administrator.musicplayer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.administrator.musicplayer.R;
import com.example.administrator.musicplayer.datastructure.LyricView;
import com.example.administrator.musicplayer.datastructure.MusicBean;
import com.example.administrator.musicplayer.tool.TransportFlag;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class LyricActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    /*****************************************************************************************
     *************************************    全局变量    *************************************
     *****************************************************************************************/

    /**
     * 布局组件
     **/
    //按钮
    private Button mbtnBack, mbtnMode, mbtnLast, mbtnNext, mbtnPlay;
    //文本视图
    private TextView mtvName, mtvArtist, mtvAlbum, mtvCurrentProgress, mtvTotalProgress;
    //视图
    private LyricView lyricView;
    //拖动条
    private SeekBar seekBar;

    /**
     * 工具实例
     **/
    //主Activity实例
    private MainActivity mainActivity;
    //播放列表
    private ArrayList<MusicBean> mMusicList = new ArrayList<>();
    //当前播放条目
    private MusicBean CurrentMusicItem;
    //接收器
    private LyricActivityReceiver lyricActivityReceiver = new LyricActivityReceiver();
    //处理器
    private Handler HandlerLyric = new Handler();


    /**
     * 自定义元素
     **/
    //歌词内容索引
    private int Index;
    //歌词加载标识
    private boolean isLyricPrepared = false;
    //播放模式序号
    public int PlayMode = 0, mode = 0;
    //按钮锁
    public boolean isComponentLocked = true;
    //歌词数组大小
    public int sizeOfList = 0;

    /*****************************************************************************************
     * *************************************    分割线    **************************************
     *****************************************************************************************/

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_music_item);
        this.mainActivity = MainActivity.mainActivity;
        InitLayout();

        //注册接收器
        IntentFilter intentFilter = new IntentFilter(TransportFlag.MusicService);
        registerReceiver(lyricActivityReceiver, intentFilter);

        InitComponent();
    }

    /**
     * 初始化布局
     **/
    public void InitLayout() {
        //设置视图
        lyricView = (LyricView) findViewById(R.id.vLyric);

        //设置拖动条
        seekBar = (SeekBar) findViewById(R.id.sb);
        seekBar.setOnSeekBarChangeListener(this);

        //设置文本视图
        mtvName = (TextView) findViewById(R.id.tvName);
        mtvArtist = (TextView) findViewById(R.id.tvArtist);
        mtvAlbum = (TextView) findViewById(R.id.tvAlbum);
        mtvCurrentProgress = (TextView) findViewById(R.id.tvCurrentProgress);
        mtvTotalProgress = (TextView) findViewById(R.id.tvTotalProgress);

        //设置按钮
        mbtnBack = (Button) findViewById(R.id.btnBack);
        mbtnBack.setOnClickListener(this);
        mbtnMode = (Button) findViewById(R.id.btnMode);
        mbtnMode.setOnClickListener(this);
        mbtnLast = (Button) findViewById(R.id.btnLast);
        mbtnLast.setOnClickListener(this);
        mbtnNext = (Button) findViewById(R.id.btnNext);
        mbtnNext.setOnClickListener(this);
        mbtnPlay = (Button) findViewById(R.id.btnPlay);
        mbtnPlay.setOnClickListener(this);
    }

    /*****************************************************************************************
     *************************************    组件接口    *************************************
     *****************************************************************************************/

    /**
     * Button设置
     **/
    @Override
    public void onClick(View v) {
        if (isComponentLocked) return;
        switch (v.getId()) {
            case R.id.btnBack:
                unregisterReceiver(lyricActivityReceiver);
                LyricActivity.this.finish();
                break;
            case R.id.btnMode:
                setPlayMode();
                break;
            case R.id.btnLast:
                mainActivity.LastItem();
                break;
            case R.id.btnNext:
                mainActivity.NextItem();
                break;
            case R.id.btnPlay:
                Play_Pause();
                break;
            default:
                break;
        }
    }

    /**
     * 返回键关闭抽屉界面
     **/
    @Override
    public void onBackPressed() {
        unregisterReceiver(lyricActivityReceiver);
        LyricActivity.this.finish();
    }

    /**
     * SeekBar设置
     **/
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mainActivity.UpdateSeekbar(seekBar);
    }

    /*****************************************************************************************
     ************************************    自定义方法    ************************************
     *****************************************************************************************/

    /**
     * *跳转页面后初始化
     */
    public void InitComponent() {
        CurrentMusicItem = mainActivity.CurrentMusicItem;
        HandlerLyric.post(new Runnable() {
            @Override
            public void run() {
                mtvCurrentProgress.setText(mainActivity.mtvCurrentProgress.getText());
                mtvTotalProgress.setText(mainActivity.mtvTotalProgress.getText());
                seekBar.setProgress(mainActivity.seekBar.getProgress());
                seekBar.setMax(mainActivity.seekBar.getMax());
                mbtnPlay.setText(mainActivity.mbtnPlay.getText());
                mtvName.setText("Name : " + CurrentMusicItem.getMusicName());
                mtvArtist.setText("Artist : " + CurrentMusicItem.getMusicArtist());
                mtvAlbum.setText("Album : " + CurrentMusicItem.getMusicAlbum());
                mode = mainActivity.mode;
                LoadLyric();
            }
        });
    }

    /**
     * 加载歌词资源
     **/
    public void LoadLyric() {
        isComponentLocked = true;
        mtvName.setText("Name : " + CurrentMusicItem.getMusicName());
        mtvArtist.setText("Artist : " + CurrentMusicItem.getMusicArtist());
        mtvAlbum.setText("Album : " + CurrentMusicItem.getMusicAlbum());
        if (CurrentMusicItem.getLyricList() != null) {
            lyricView.setLyric("Searching local lyric ......");
            isLyricPrepared = false;
            lyricView.invalidate();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (CurrentMusicItem.getLyricList().size() == 0) {
                        HandlerLyric.post(new Runnable() {
                            @Override
                            public void run() {
                                lyricView.setLyric("No match to lyric.");
                                lyricView.invalidate();
                                Index = 0;
                            }
                        });
                    } else {
                        sizeOfList = CurrentMusicItem.getLyricList().size();
                        isLyricPrepared = true;
                    }
                    isComponentLocked = false;
                }
            }).start();
        }
    }

    /**
     * 绘制歌词
     **/
    public void DrawLyric() {
        HandlerLyric.post(new Runnable() {
            @Override
            public void run() {
                lyricView.setLyric(CurrentMusicItem.getLyricList().get(Index).getLyric());
                lyricView.invalidate();
            }
        });
    }

    /**
     * 调整索引位置
     **/
    public void AdjustIndex(int CurrentTime) {
        Log.e("CurrentTime", CurrentTime + "");
        Log.e("Index", Index + "");
        if (Index == 0) {                           //索引位于数组首位
            if (CurrentTime < CurrentMusicItem.getLyricList().get(Index + 1).getTime()) {
                DrawLyric();
            } else {
                Index++;
                AdjustIndex(CurrentTime);
            }
        } else if (Index == sizeOfList - 1) {       //索引位于数组末位
            if (CurrentTime < CurrentMusicItem.getLyricList().get(Index).getTime() - 300) {
                Index--;
                AdjustIndex(CurrentTime);
            } else {
                DrawLyric();
            }
        } else {                                    //索引位于数组中间
            if (CurrentTime < CurrentMusicItem.getLyricList().get(Index).getTime() - 300) {
                Index--;
                AdjustIndex(CurrentTime);
            } else if (CurrentTime < CurrentMusicItem.getLyricList().get(Index + 1).getTime() - 300) {
                DrawLyric();
            } else {
                Index++;
                AdjustIndex(CurrentTime);
            }
        }
    }

    /**
     * 播放模式设定
     **/
    public void setPlayMode() {
        /** 消息框形式弹出选项：顺序播放，单曲循环，随机播放。默认：顺序播放 **/
        new AlertDialog.Builder(LyricActivity.this)
                .setTitle("Set PlayMode")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setSingleChoiceItems(getResources().getStringArray(R.array.play_mode), PlayMode,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mode = which;
                            }
                        }
                )
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mbtnMode.setText(getResources().getStringArray(R.array.play_mode)[mode]);
                        PlayMode = mode;
                        mainActivity.mode = mode;
                        mainActivity.PlayMode = mode;
                        Intent Intent_PlayMode = new Intent(TransportFlag.MainActivity);
                        Intent_PlayMode.putExtra(TransportFlag.Mode, PlayMode);
                        Intent_PlayMode.putExtra(TransportFlag.State, TransportFlag.Mode);
                        //将播放模式传给Service        测试完毕
                        sendBroadcast(Intent_PlayMode);
                        dialog.dismiss();
                    }
                })
                .show();
    }

    /**
     * 播放和暂停切换
     **/
    public void Play_Pause() {
        Intent Intent_PlayPause = new Intent(TransportFlag.MainActivity);
        switch (mbtnPlay.getText().toString()) {
            case "PLAY":
                Intent_PlayPause.putExtra(TransportFlag.State, TransportFlag.Play);
                mbtnPlay.setText("PAUSE");
                mainActivity.mbtnPlay.setText("PAUSE");
                break;
            case "PAUSE":
                Intent_PlayPause.putExtra(TransportFlag.State, TransportFlag.Pause);
                mbtnPlay.setText("PLAY");
                mainActivity.mbtnPlay.setText("PLAY");
                break;
            default:
                break;
        }

        //Service播放或者暂停播放器      测试完毕
        sendBroadcast(Intent_PlayPause);
    }

    /**
     * 接收器
     **/
    class LyricActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int SeekBarMax, SeekBarTo;
            String strTextViewTo;
            String strState = intent.getStringExtra(TransportFlag.State);
            Log.e( TransportFlag.State, strState +"");
            switch (strState) {
                case TransportFlag.LoadMusic:                                           //接收加载音乐列表    测试完毕
                    mMusicList = (ArrayList<MusicBean>) (intent.getSerializableExtra("mMusicList"));
                    CurrentMusicItem = mMusicList.get(0);
                    break;
                case TransportFlag.SeekTo:                                              //接收移动拖动条至    测试完毕
                    SeekBarTo = intent.getIntExtra("SeekBarTo", 0);
                    strTextViewTo = intent.getStringExtra("TextViewTo");
                    seekBar.setProgress(SeekBarTo);
                    mtvCurrentProgress.setText(strTextViewTo);
                    break;
                case TransportFlag.SeekPrepare:                                         //接收播放准备        测试完毕
                    SeekBarMax = intent.getIntExtra("SeekBarMax", 0);
                    strTextViewTo = intent.getStringExtra("TextViewTo");
                    seekBar.setMax(SeekBarMax);
                    mtvTotalProgress.setText(strTextViewTo);
                    mtvCurrentProgress.setText(new SimpleDateFormat("mm:ss").format(new Date(0)));
                    mbtnPlay.setText("PAUSE");
                    break;
                case TransportFlag.NextItem:                                            //接收下一首          测试完毕
                    isLyricPrepared = false;
                    break;
                case TransportFlag.LyricTo:                                             //接收当前歌词位置    测试完毕
                    int CurrentPosition = intent.getIntExtra("CurrentPosition", 0);
                    if (isLyricPrepared) {
                        AdjustIndex(CurrentPosition);
                    }
                    break;
                case TransportFlag.Prepare:                                             //接收播放准备        测试完毕
                    CurrentMusicItem = (MusicBean) intent.getSerializableExtra(TransportFlag.Prepare);
                    mtvName.setText(CurrentMusicItem.getMusicName());
                    LoadLyric();
                    break;
                default:
                    break;
            }
        }
    }
}
