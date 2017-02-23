package com.example.administrator.musicplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
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

import static com.example.administrator.musicplayer.TransportFlag.CurrentItem;

public class MusicService extends Service {
    boolean mAllowRebind;       // indicates whether onRebind should be used

    //媒体播放器
    public MediaPlayer mediaplayer = new MediaPlayer();
    //播放列表
    public ArrayList<MusicBean> mMusicList = new ArrayList<>();
    //播放列表索引
    public int ItemLocationIndex;
    //播放顺序数组
    public int[] PlayArray;
    //播放顺序数组索引
    public int PlayArrayIndex;
    //播放管理器
    public Handler HandlerPlay = new Handler();
    //拖动条管理器
    public Handler HandlerSeekbar = new Handler();
    //播放线程
    public Runnable RunnablePlay;
    //拖动条线程
    public Runnable RunnableSeekbar;
    //接收器
    public MusicServiceReceiver musicServiceReceiver = new MusicServiceReceiver();
    //服务状态
    public String state;
    //播放模式序号
    private int mode;

    @Override
    public void onCreate() {
        super.onCreate();

        //注册接收器
        IntentFilter intentFilter = new IntentFilter( TransportFlag.MusicService );
        registerReceiver( musicServiceReceiver, intentFilter );

        //设置播放线程
        RunnablePlay = new Runnable() {
            @Override
            public void run() {
                mediaplayer.start();
                HandlerSeekbar.post( RunnableSeekbar );
            }
        };

        //设置拖动条线程
        RunnableSeekbar = new Runnable() {
            @Override
            public void run() {
                try {
                    Intent Intent_UpdateSeekBar = new Intent( TransportFlag.MainActivity );
                    Intent_UpdateSeekBar.putExtra( "SeekBarTo", mediaplayer.getCurrentPosition() );
                    Intent_UpdateSeekBar.putExtra( "TextViewTo", new SimpleDateFormat( "mm:ss" ).format( new Date( mediaplayer.getCurrentPosition() ) ) );
                    Intent_UpdateSeekBar.putExtra( TransportFlag.state, TransportFlag.SeekTo );
                    //更新拖动条信息给Activity      测试完毕
                    sendBroadcast( Intent_UpdateSeekBar );
                    HandlerSeekbar.postDelayed( RunnableSeekbar, 1000 );
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        };
        PlayArrayIndex = 0;
        ItemLocationIndex = 0;
        mode = 0;
        LoadMusic();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand( intent, flags, startId );
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaplayer.stop();
        mediaplayer.release();
        //将线程销毁掉
        HandlerSeekbar.removeCallbacks( RunnableSeekbar );
        unregisterReceiver( musicServiceReceiver );
    }

    /**
     * 载入歌曲
     **/
    public void LoadMusic() {
        mMusicList.clear();
        //利用游标查找媒体数据库中的音乐文件
        Cursor cursor = MusicService.this.getContentResolver().query(
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
                    music.setMusicName( cursor.getString( cursor.getColumnIndexOrThrow( MediaStore.Audio.Media.TITLE ) ).replaceAll( "(\\(.*?\\))?(\\[.*?\\])?(\\{.*?\\})?", "" )
                            .replaceAll( ".mp3", "" ) );
                    music.setMusicPath( path );
                    mMusicList.add( music );
                }
            }
            cursor.close();

            ModeSetting( mode );
            sendMusicList( mMusicList );
        }
    }

    /**
     * 发送列表给Service
     **/
    public void sendMusicList(ArrayList<MusicBean> MusicList) {
        Intent Intent_SendMusicList = new Intent( TransportFlag.MainActivity );
        Intent_SendMusicList.putParcelableArrayListExtra( "mMusicList", MusicList );
        Intent_SendMusicList.putExtra( TransportFlag.state, TransportFlag.LoadMusic );
        //将播放列表发给Service        测试完毕
        sendBroadcast( Intent_SendMusicList );
    }

    /**
     * 上一首
     **/
    public void LastMusic() {
        PlayArrayIndex--;
        PlayArrayIndex = (PlayArrayIndex + mMusicList.size()) % mMusicList.size();
        ItemLocationIndex = PlayArray[PlayArrayIndex];
        mediaplayer.stop();
        playMusic( mMusicList.get( ItemLocationIndex ).getMusicPath() );
    }

    /**
     * 下一首
     **/
    public void NextMusic() {
        PlayArrayIndex++;
        PlayArrayIndex = PlayArrayIndex % mMusicList.size();
        ItemLocationIndex = PlayArray[PlayArrayIndex];
        mediaplayer.stop();
        playMusic( mMusicList.get( ItemLocationIndex ).getMusicPath() );
    }

    /**
     * 播放音乐
     **/
    public void playMusic(String path) {
        if (path != null) {
            HandlerSeekbar.removeCallbacks( RunnableSeekbar );
            try {
                mediaplayer.reset();
                mediaplayer.setDataSource( path );
                mediaplayer.setAudioStreamType( AudioManager.STREAM_MUSIC );
                mediaplayer.prepare();
                mediaplayer.setOnCompletionListener( new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        PlayArrayIndex++;
                        PlayArrayIndex = PlayArrayIndex % mMusicList.size();
                        ItemLocationIndex = PlayArray[PlayArrayIndex];

                        Intent Intent_NextItem = new Intent( TransportFlag.MainActivity );
                        Intent_NextItem.putExtra( TransportFlag.NextItem, mMusicList.get( ItemLocationIndex ).getMusicName() );
                        Intent_NextItem.putExtra( TransportFlag.state, TransportFlag.NextItem );
                        //发送下一首给Activity用于Toast     测试完毕
                        sendBroadcast( Intent_NextItem );
                        HandlerSeekbar.postDelayed( new Runnable() {
                            @Override
                            public void run() {
                                playMusic( mMusicList.get( ItemLocationIndex ).getMusicPath() );
                            }
                        }, 3000 );
                    }
                } );
                mediaplayer.setOnPreparedListener( new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        Intent Intent_SeekPrepare = new Intent( TransportFlag.MainActivity );
                        Intent_SeekPrepare.putExtra( "SeekBarMax", mediaplayer.getDuration() );
                        Intent_SeekPrepare.putExtra( "TextViewTo", new SimpleDateFormat( "mm:ss" ).format( new Date( mediaplayer.getDuration() ) ) );
                        Intent_SeekPrepare.putExtra( TransportFlag.state, TransportFlag.SeekPrepare );
                        //发送拖动条最大值和置0给Activity      测试完毕
                        sendBroadcast( Intent_SeekPrepare );
                    }
                } );
            } catch (Exception e) {
                e.printStackTrace();
            }
            Intent Intent_CurrentItem = new Intent( TransportFlag.MainActivity );
            Intent_CurrentItem.putExtra( CurrentItem, mMusicList.get( ItemLocationIndex ) );
            Intent_CurrentItem.putExtra( TransportFlag.state, CurrentItem );
            //发送当前播放条目给Activity     测试完毕
            sendBroadcast( Intent_CurrentItem );
            HandlerPlay.post( RunnablePlay );
        }
    }

    /**
     * 播放模式设置
     **/
    public void ModeSetting(int mode) {
        PlayArray = new int[mMusicList.size()];
        switch (mode) {
            case TransportFlag.OrderPlay:
                for (int i = 0; i < PlayArray.length; i++) {
                    PlayArray[i] = i;
                }
                break;
            case TransportFlag.SinglePlay:
                for (int i = 0; i < PlayArray.length; i++) {
                    PlayArray[i] = ItemLocationIndex;
                }
                break;
            case TransportFlag.RandomPlay:
                ModeSetting( TransportFlag.OrderPlay );
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
    }

    /**
     * 接收器
     **/
    class MusicServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String path;
            int progress;
            state = intent.getStringExtra( TransportFlag.state );
            Log.e( "state", state );
            switch (state) {
                case TransportFlag.LoadMusic:                                   //接收加载音乐           测试完毕
                    mMusicList = (ArrayList) (intent.getParcelableArrayListExtra( "mMusicList" ));
                    ModeSetting( mode );
                    break;
                case TransportFlag.PlayDefault:                                 //接收默认播放曲目       测试完毕
                    playMusic( mMusicList.get( ItemLocationIndex ).getMusicPath() );
                    break;
                case TransportFlag.PlayList:                                    //接收按列表播放         测试完毕
                    ItemLocationIndex = intent.getIntExtra( "position", ItemLocationIndex );
                    PlayArrayIndex = intent.getIntExtra( "position", PlayArrayIndex );
                    path = intent.getStringExtra( "path" );
                    playMusic( path );
                    break;
                case TransportFlag.Play:                                        //接收媒体播放器播放     测试完毕
                    mediaplayer.start();
                    break;
                case TransportFlag.Pause:                                       //接收媒体播放器暂停     测试完毕
                    mediaplayer.pause();
                    break;
                case TransportFlag.Last:                                        //接收上一首             测试完毕
                    LastMusic();
                    break;
                case TransportFlag.Next:                                        //接收下一首             测试完毕
                    NextMusic();
                    break;
                case TransportFlag.SeekTo:                                      //接收播放器跳转至       测试完毕
                    progress = intent.getIntExtra( TransportFlag.SeekTo, 0 );
                    mediaplayer.seekTo( progress );
                    break;
                case TransportFlag.Mode:                                        //接收播放器模式设置     测试完毕
                    mode = intent.getIntExtra( TransportFlag.Mode, 0 );
                    ModeSetting( mode );
                    break;
                case TransportFlag.Exit:                                        //接收退出信号           测试完毕
                    MusicService.this.stopSelf();
                default:
                    break;
            }
        }
    }

    /**
     * 绑定类
     **/
    public class ServiceBinder extends Binder {
    }
}
