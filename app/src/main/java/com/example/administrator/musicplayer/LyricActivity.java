package com.example.administrator.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
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
     * 其他
     **/
    //主Activity实例
    private MainActivity mainActivity;
    //歌词解析实例
    private LyricParsing lyricParsing;
    //当前播放条目
    private MusicBean CurrentMusicItem;
    //接收器
    private LyricActivityReceiver lyricActivityReceiver = new LyricActivityReceiver();
    //处理器
    private Handler HandlerComponent = new Handler();

    private Handler HandlerParsing = new Handler();

    private Runnable RunnableDraw;

    int iii = 0;

    /*****************************************************************************************
     * *************************************    分割线    **************************************
     *****************************************************************************************/

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate( savedInstance );
        setContentView( R.layout.activity_music_item );

        this.mainActivity = MainActivity.mainActivity;
        InitLayout();

        //注册接收器
        IntentFilter intentFilter = new IntentFilter( TransportFlag.MusicService );
        registerReceiver( lyricActivityReceiver, intentFilter );

        InitComponent();
        DrawLyric();
    }

    /**
     * 初始化布局
     **/
    public void InitLayout() {
        //设置视图
        lyricView = (LyricView) findViewById( R.id.vLyric );

        //设置拖动条
        seekBar = (SeekBar) findViewById( R.id.sb );
        seekBar.setOnSeekBarChangeListener( this );

        //设置文本视图
        mtvName = (TextView) findViewById( R.id.tvName );
        mtvArtist = (TextView) findViewById( R.id.tvArtist );
        mtvAlbum = (TextView) findViewById( R.id.tvAlbum );
        mtvCurrentProgress = (TextView) findViewById( R.id.tvCurrentProgress );
        mtvTotalProgress = (TextView) findViewById( R.id.tvTotalProgress );

        //设置按钮
        mbtnBack = (Button) findViewById( R.id.btnBack );
        mbtnBack.setOnClickListener( this );
        mbtnMode = (Button) findViewById( R.id.btnMode );
        mbtnMode.setOnClickListener( this );
        mbtnLast = (Button) findViewById( R.id.btnLast );
        mbtnLast.setOnClickListener( this );
        mbtnNext = (Button) findViewById( R.id.btnNext );
        mbtnNext.setOnClickListener( this );
        mbtnPlay = (Button) findViewById( R.id.btnPlay );
        mbtnPlay.setOnClickListener( this );
    }

    /*****************************************************************************************
     *************************************    组件接口    *************************************
     *****************************************************************************************/

    /**
     * Button设置
     **/
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnBack:
                LyricActivity.this.finish();
                break;
            case R.id.btnMode:
                mainActivity.setPlayMode();
                break;
            case R.id.btnLast:
                mainActivity.LastItem();
                break;
            case R.id.btnNext:
                mainActivity.NextItem();
                break;
            case R.id.btnPlay:
                mainActivity.Play_Pause();
                break;
            default:
                break;
        }
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
        mainActivity.UpdateSeekbar( seekBar );
    }

    /*****************************************************************************************
     ************************************    自定义方法    ************************************
     *****************************************************************************************/

    /**
     * *跳转页面后初始化
     */
    public void InitComponent() {
        CurrentMusicItem = mainActivity.CurrentMusicItem;
        HandlerComponent.post( new Runnable() {
            @Override
            public void run() {
                mtvCurrentProgress.setText( mainActivity.mtvCurrentProgress.getText() );
                mtvTotalProgress.setText( mainActivity.mtvTotalProgress.getText() );
                seekBar.setProgress( mainActivity.seekBar.getProgress() );
                seekBar.setMax( mainActivity.seekBar.getMax() );
                mbtnPlay.setText( mainActivity.mbtnPlay.getText() );
                mtvName.setText( "Name : " + CurrentMusicItem.getMusicName() );
                mtvArtist.setText( "Artist : " + CurrentMusicItem.getMusicArtist() );
                mtvAlbum.setText( "Album : " + CurrentMusicItem.getMusicAlbum() );
            }
        } );
    }

    /**
     * 绘制歌词
     **/
    public void DrawLyric() {
        new Thread( new Runnable() {
            @Override
            public void run() {
                lyricParsing = new LyricParsing( CurrentMusicItem.getMusicName() );
            }
        } ).start();
        RunnableDraw = new Runnable() {
            @Override
            public void run() {
                iii++;
                lyricView.setLyric( iii + "" );
                lyricView.invalidate();
                HandlerComponent.postDelayed( RunnableDraw, 1000 );
            }
        };
        HandlerComponent.post( RunnableDraw );
    }

    /**
     * 接收器
     **/
    class LyricActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int SeekBarMax, SeekBarTo;
            String strTextViewTo, strNextItem;
            String strState = intent.getStringExtra( TransportFlag.State );
            Log.e( TransportFlag.State, strState );
            switch (strState) {
                case TransportFlag.SeekTo:                                          //接收移动拖动条至    测试完毕
                    SeekBarTo = intent.getIntExtra( "SeekBarTo", 0 );
                    strTextViewTo = intent.getStringExtra( "TextViewTo" );
                    seekBar.setProgress( SeekBarTo );
                    mtvCurrentProgress.setText( strTextViewTo );
                    break;
                case TransportFlag.SeekPrepare:                                     //接收播放准备        测试完毕
                    SeekBarMax = intent.getIntExtra( "SeekBarMax", 0 );
                    strTextViewTo = intent.getStringExtra( "TextViewTo" );
                    seekBar.setMax( SeekBarMax );
                    mtvTotalProgress.setText( strTextViewTo );
                    mtvCurrentProgress.setText( new SimpleDateFormat( "mm:ss" ).format( new Date( 0 ) ) );
                    mbtnPlay.setText( "PAUSE" );
                    break;
                case TransportFlag.CurrentItem:                                     //接收当前条目        测试完毕
                    CurrentMusicItem = (MusicBean) intent.getSerializableExtra( TransportFlag.CurrentItem );
                    mtvName.setText( CurrentMusicItem.getMusicName() );
                default:
                    break;
            }
        }
    }
}
