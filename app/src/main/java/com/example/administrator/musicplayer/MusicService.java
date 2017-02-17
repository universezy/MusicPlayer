package com.example.administrator.musicplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class MusicService extends Service {
    public IBinder mBinder;      // interface for clients that bind
    boolean mAllowRebind; // indicates whether onRebind should be used

    //媒体播放器
    public MediaPlayer mediaplayer = new MediaPlayer();
    //播放列表
    public static ArrayList<MusicBean> mMusicList = new ArrayList<>();
    //播放列表索引，初始化为第一首
    public int ItemLocationIndex = 0;
    //播放顺序数组
    public int[] PlayArray;
    //播放顺序数组索引
    public int PlayArrayIndex = 0;
    //拖动条管理器
    public Handler mHandlerSeekbar = new Handler();
    //播放线程
    //public Runnable mRunnablePlay;
    //拖动条线程
    public Runnable mRunnableSeekbar;
    //接收器
    MusicServiceReceiver musicServiceReceiver = new MusicServiceReceiver();
    //服务状态
    public String state;
    //播放模式序号
    private int mode = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        //注册接收器
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction( TransportFlag.MusicService );
        registerReceiver( musicServiceReceiver, intentFilter );

        //设置拖动条线程
        mRunnableSeekbar = new Runnable() {
            @Override
            public void run() {
                try {
                    Intent Intent_UpdateSeekBar = new Intent();
                    Intent_UpdateSeekBar.putExtra( "SeekBarTo", mediaplayer.getCurrentPosition() );
                    Intent_UpdateSeekBar.putExtra( "TextViewTo", new SimpleDateFormat( "mm:ss" ).format( new Date( mediaplayer.getCurrentPosition() ) ) );
                    Intent_UpdateSeekBar.setAction( TransportFlag.MusicService );
                    sendBroadcast( Intent_UpdateSeekBar );
                    mHandlerSeekbar.postDelayed( mRunnableSeekbar, 500 );
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        };

        LoadMusic();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        return super.onStartCommand( intent, flags, startId );
    }

    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // The service is no longer used and is being destroyed
        //将线程销毁掉
        mHandlerSeekbar.removeCallbacks( mRunnableSeekbar );
        mediaplayer.stop();
        mediaplayer.release();
        unregisterReceiver( musicServiceReceiver );
    }


    /**
     * 载入歌曲
     **/
    public void LoadMusic() {
        mMusicList.clear();
        Cursor cursor = this.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.DATA + " like ?",
                new String[]{Environment.getExternalStorageDirectory() + File.separator + "%"},
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER );
        if (cursor != null) {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                //如果不是音乐
                String isMusic = cursor.getString( cursor.getColumnIndexOrThrow( MediaStore.Audio.Media.IS_MUSIC ) );
                if (isMusic != null && isMusic.equals( "" )) continue;
                String path;
                MusicBean music;
                if ((path = cursor.getString( cursor.getColumnIndexOrThrow( MediaStore.Audio.Media.DATA ) )).endsWith( ".mp3" )) {
                    music = new MusicBean();
                    music.setMusicName( cursor.getString( cursor.getColumnIndexOrThrow( MediaStore.Audio.Media.TITLE ) ) );
                    music.setMusicPath( path );
                    mMusicList.add( music );
                    Log.e( music.getMusicName(),music.getMusicPath());
                }
            }
            cursor.close();
            //设置默认的播放顺序为顺序播放
            PlayArray = new int[mMusicList.size()];
            for (int i = 0; i < PlayArray.length; i++) {
                PlayArray[i] = i;
            }
            Intent Intent_SendMusicList = new Intent();
            Intent_SendMusicList.putParcelableArrayListExtra( "mMusicList", mMusicList );
            Intent_SendMusicList.putExtra( TransportFlag.state, TransportFlag.LoadMusic );
            Intent_SendMusicList.setAction( TransportFlag.MusicService );
            sendBroadcast( Intent_SendMusicList );
        }
    }

    /**
     * 上一首
     **/
    public void LastMusic() {
        ItemLocationIndex--;
        ItemLocationIndex = (ItemLocationIndex + mMusicList.size()) % mMusicList.size();
        mediaplayer.stop();
        playMusic( mMusicList.get( ItemLocationIndex ).getMusicPath() );
    }

    /**
     * 下一首
     **/
    public void NextMusic() {
        ItemLocationIndex++;
        ItemLocationIndex = ItemLocationIndex % mMusicList.size();
        mediaplayer.stop();
        playMusic( mMusicList.get( ItemLocationIndex ).getMusicPath() );
    }

    /**
     * 播放音乐
     **/
    public void playMusic(String path) {
        try {
            mediaplayer.reset();
            mediaplayer.setDataSource( path );
            mediaplayer.setAudioStreamType( AudioManager.STREAM_MUSIC );
            mediaplayer.prepare();
            mediaplayer.setOnCompletionListener( new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    switch (mode) {
                        case TransportFlag.OrderPlay:
                            break;
                        case TransportFlag.SinglePlay:
                            for (int i = 0; i < PlayArray.length; i++) {
                                PlayArray[i] = ItemLocationIndex;
                            }
                            break;
                        case TransportFlag.RandomPlay:
                            int temp;
                            //生成随机播放列表
                            Random random = new Random();
                            for (int i = 0; i < PlayArray.length; i++) {
                                int j = random.nextInt( PlayArray.length );
                                temp = PlayArray[i];
                                PlayArray[i] = PlayArray[j];
                                PlayArray[j] = temp;
                            }
                            break;
                        default:
                            break;
                    }
                    PlayArrayIndex++;
                    PlayArrayIndex = PlayArrayIndex % mMusicList.size();
                    ItemLocationIndex = PlayArray[PlayArrayIndex];
                    Intent Intent_NextItem = new Intent();
                    Intent_NextItem.putExtra( TransportFlag.NextItem, mMusicList.get( ItemLocationIndex ).getMusicName() );
                    Intent_NextItem.putExtra( TransportFlag.state, TransportFlag.NextItem );
                    Intent_NextItem.setAction( TransportFlag.MusicService );
                    sendBroadcast( Intent_NextItem );
                    try {
                        Thread.sleep( 3000 );
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    playMusic( mMusicList.get( ItemLocationIndex ).getMusicPath() );

                }
            } );
            mediaplayer.setOnPreparedListener( new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Intent Intent_UpdateSeekBar = new Intent();
                    Intent_UpdateSeekBar.putExtra( "SeekBarMax", mediaplayer.getDuration() );
                    Intent_UpdateSeekBar.putExtra( "TextViewTo", new SimpleDateFormat( "mm:ss" ).format( new Date( mediaplayer.getCurrentPosition() ) ) );
                    Intent_UpdateSeekBar.putExtra( TransportFlag.state, TransportFlag.SeekPrepare );
                    Intent_UpdateSeekBar.setAction( TransportFlag.MusicService );
                    sendBroadcast( Intent_UpdateSeekBar );
                }
            } );
        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent Intent_CurrentItem = new Intent();
        Intent_CurrentItem.putExtra( TransportFlag.CurrentItem, mMusicList.get( ItemLocationIndex ) );
        Intent_CurrentItem.putExtra( TransportFlag.state, TransportFlag.CurrentItem );
        Intent_CurrentItem.setAction( TransportFlag.MusicService );
        sendBroadcast( Intent_CurrentItem );
        mHandlerSeekbar.post( mRunnableSeekbar );
    }

    /**
     * 接收器
     **/
    class MusicServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ItemLocationIndex = intent.getIntExtra( "position", 0 );
            PlayArrayIndex = intent.getIntExtra( "position", 0 );
            String path = intent.getStringExtra( "path" );
            int progress = intent.getIntExtra( "SeekTo", 0 );
            mode = intent.getIntExtra( "mode", 0 );
            state = intent.getStringExtra( "state" );
            switch (state) {
                case TransportFlag.PlayDefault:
                    playMusic( mMusicList.get( ItemLocationIndex ).getMusicPath() );
                    break;
                case TransportFlag.PlayList:
                    playMusic( path );
                    break;
                case TransportFlag.Play:
                    mediaplayer.start();
                    break;
                case TransportFlag.Pause:
                    mediaplayer.pause();
                    break;
                case TransportFlag.Last:
                    LastMusic();
                    break;
                case TransportFlag.Next:
                    NextMusic();
                    break;
                case TransportFlag.SeekTo:
                    mediaplayer.seekTo( progress );
                    break;
                case TransportFlag.Exit:
                    MusicService.this.onDestroy();
                default:
                    break;
            }
        }
    }
}
