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

public class MusicService extends Service {
    boolean mAllowRebind;       // indicates whether onRebind should be used
    /**
     * 工具实例
     **/
    //媒体播放器
    private MediaPlayer mediaplayer = new MediaPlayer();
    //处理器
    private Handler HandlerService = new Handler();
    //播放线程
    private Runnable RunnablePlay;
    //拖动条线程
    private Runnable RunnableSeekbar;
    //接收器
    protected MusicServiceReceiver musicServiceReceiver = new MusicServiceReceiver();

    /**
     * 自定义元素
     **/
    //播放列表
    public ArrayList<MusicBean> mMusicList = new ArrayList<>();
    //播放列表索引
    public int ItemLocationIndex = 0;
    //播放顺序数组
    private int[] PlayArray;
    //播放顺序数组索引
    private int PlayArrayIndex = 0;
    //播放模式序号
    private int mode = 0;

    /*****************************************************************************************
     * *************************************    分割线    **************************************
     *****************************************************************************************/

    @Override
    public void onCreate() {
        super.onCreate();

        //注册接收器
        IntentFilter intentFilter = new IntentFilter( TransportFlag.MainActivity );
        registerReceiver( musicServiceReceiver, intentFilter );

        LoadMusic();

        //设置播放线程
        RunnablePlay = new Runnable() {
            @Override
            public void run() {
                mediaplayer.start();
                HandlerService.post( RunnableSeekbar );
            }
        };

        //设置拖动条线程
        RunnableSeekbar = new Runnable() {
            @Override
            public void run() {
                try {
                    Intent Intent_UpdateSeekBar = new Intent( TransportFlag.MusicService );
                    Intent_UpdateSeekBar.putExtra( "SeekBarTo", mediaplayer.getCurrentPosition() );
                    Intent_UpdateSeekBar.putExtra( "TextViewTo", new SimpleDateFormat( "mm:ss" ).format( new Date( mediaplayer.getCurrentPosition() ) ) );
                    Intent_UpdateSeekBar.putExtra( TransportFlag.State, TransportFlag.SeekTo );
                    //更新拖动条信息给Activity      测试完毕
                    sendBroadcast( Intent_UpdateSeekBar );
                    HandlerService.postDelayed( RunnableSeekbar, 1000 );
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        };
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
        mediaplayer.stop();
        mediaplayer.release();
        //将线程销毁掉
        HandlerService.removeCallbacks( RunnableSeekbar );
        unregisterReceiver( musicServiceReceiver );
        super.onDestroy();
    }

    /*****************************************************************************************
     ************************************    自定义方法    ************************************
     *****************************************************************************************/

    /**
     * 载入歌曲
     **/
    public void LoadMusic() {
        new Thread( new Runnable() {
            @Override
            public void run() {
                mMusicList.clear();
                //利用游标查找媒体数据库中的音乐文件
                Cursor cursor = MusicService.this.getContentResolver().query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                        MediaStore.Audio.Media.DATA + " like ?",
                        new String[]{Environment.getExternalStorageDirectory() + File.separator + "%"},
                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER );
                if (cursor != null) {
                    for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                        String isMusic = cursor.getString( cursor.getColumnIndexOrThrow( MediaStore.Audio.Media.IS_MUSIC ) );
                        if (isMusic != null && isMusic.equals( "" )) continue;
                        String string;
                        MusicBean music;
                        if ((string = cursor.getString( cursor.getColumnIndexOrThrow( MediaStore.Audio.Media.DATA ) )).endsWith( ".mp3" )) {
                            music = new MusicBean();
                            music.setMusicPath( string );
                            string = getMusicAttribution( cursor, MediaStore.Audio.Media.TITLE );
                            music.setMusicName( (string != null) ? string : "null" );
                            string = getMusicAttribution( cursor, MediaStore.Audio.Media.ARTIST );
                            music.setMusicArtist( (string != null) ? string : "null" );
                            string = getMusicAttribution( cursor, MediaStore.Audio.Media.ALBUM );
                            music.setMusicAlbum( (string != null) ? string : "null" );
                            mMusicList.add( music );
                        }
                    }
                    cursor.close();

                    ModeSetting( mode );
                    sendMusicList( mMusicList );
                }
            }
        } ).start();
    }

    /**
     * 获取音乐属性
     **/
    public String getMusicAttribution(Cursor cursor, String type) {
        return cursor.getString( cursor.getColumnIndexOrThrow( type ) )
                .replaceAll( "(\\(.*?\\))?(\\[.*?\\])?(\\{.*?\\})?", "" )
                .replaceAll( ".mp3", "" );
    }

    /**
     * 发送列表给Service
     **/
    public void sendMusicList(ArrayList<MusicBean> MusicList) {
        Intent Intent_SendMusicList = new Intent( TransportFlag.MusicService );
        Intent_SendMusicList.putExtra( "mMusicList", MusicList );
        Intent_SendMusicList.putExtra( TransportFlag.State, TransportFlag.LoadMusic );
        //将播放列表发给Activity        测试完毕
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
            HandlerService.removeCallbacks( RunnableSeekbar );
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

                        Intent Intent_NextItem = new Intent( TransportFlag.MusicService );
                        Intent_NextItem.putExtra( TransportFlag.NextItem, mMusicList.get( ItemLocationIndex ).getMusicName() );
                        Intent_NextItem.putExtra( TransportFlag.State, TransportFlag.NextItem );
                        //发送下一首给Activity用于Toast     测试完毕
                        sendBroadcast( Intent_NextItem );
                        HandlerService.postDelayed( new Runnable() {
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
                        Intent Intent_SeekPrepare = new Intent( TransportFlag.MusicService );
                        Intent_SeekPrepare.putExtra( "SeekBarMax", mediaplayer.getDuration() );
                        Intent_SeekPrepare.putExtra( "TextViewTo", new SimpleDateFormat( "mm:ss" ).format( new Date( mediaplayer.getDuration() ) ) );
                        Intent_SeekPrepare.putExtra( TransportFlag.State, TransportFlag.SeekPrepare );
                        //发送拖动条最大值和置0给Activity      测试完毕
                        sendBroadcast( Intent_SeekPrepare );
                    }
                } );
            } catch (Exception e) {
                e.printStackTrace();
            }
            Intent Intent_CurrentItem = new Intent( TransportFlag.MusicService );
            Intent_CurrentItem.putExtra( TransportFlag.CurrentItem, mMusicList.get( ItemLocationIndex ) );
            Intent_CurrentItem.putExtra( TransportFlag.State, TransportFlag.CurrentItem );
            //发送当前播放条目给Activity     测试完毕
            sendBroadcast( Intent_CurrentItem );
            HandlerService.post( RunnablePlay );
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
            String state = intent.getStringExtra( TransportFlag.State );
            Log.e( TransportFlag.State, state );
            switch (state) {
                case TransportFlag.LoadMusic:                                   //接收加载音乐           测试完毕
                    mMusicList = (ArrayList<MusicBean>) (intent.getSerializableExtra( "mMusicList" ));
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
