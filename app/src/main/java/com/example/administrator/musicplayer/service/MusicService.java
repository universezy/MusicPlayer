package com.example.administrator.musicplayer.service;

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
import android.telephony.TelephonyManager;

import com.example.administrator.musicplayer.activity.MainActivity;
import com.example.administrator.musicplayer.datastructure.LyricItem;
import com.example.administrator.musicplayer.datastructure.MusicBean;
import com.example.administrator.musicplayer.tool.TransportFlag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Random;

public class MusicService extends Service {
    boolean mAllowRebind;       // indicates whether onRebind should be used
    /**
     * 工具实例
     **/
    //主Activity实例
    private MainActivity mainActivity;
    //媒体播放器
    private MediaPlayer mediaplayer = new MediaPlayer();
    //处理器
    private Handler HandlerService = new Handler();
    //播放线程
    private Runnable RunnablePlay;
    //拖动条线程
    private Runnable RunnableSeekbar;
    //歌词线程
    private Runnable RunnableLyric;
    //接收器
    protected MusicServiceReceiver musicServiceReceiver = new MusicServiceReceiver();

    /**
     * 自定义元素
     **/
    //播放列表
    public ArrayList<MusicBean> mMusicList = new ArrayList<>();
    //歌词列表
    public ArrayList<File> LyricList = new ArrayList<>();
    //播放列表索引
    public int ItemLocationIndex = 0;
    //播放顺序数组
    private int[] PlayArray;
    //播放顺序数组索引
    private int PlayArrayIndex = 0;
    //播放模式序号
    private int mode = 0;
    //状态码
    boolean STATUS_UNFINISH = false, STATUS_FINISH = true, STATUS_FAILURE = false, STATUS_SUCCESSFUL = true;
    //扫描音乐线程结束标识
    boolean isScanMusicItemFinished = STATUS_UNFINISH;
    //扫描歌词线程结束标识
    boolean isScanLyricFinished = STATUS_UNFINISH;
    //匹配线程结束标识
    boolean isMatchFinished = STATUS_UNFINISH;
    //扫描音乐线程结果标识
    boolean Status_MusicItem = STATUS_FAILURE;
    //扫描歌词线程结果标识
    boolean Status_Lyric = STATUS_SUCCESSFUL;
    //播放线程标识
    boolean play = false;

    /*****************************************************************************************
     * *************************************    分割线    **************************************
     *****************************************************************************************/

    @Override
    public void onCreate() {
        super.onCreate();

        this.mainActivity = MainActivity.mainActivity;

        //注册接收器
        IntentFilter intentFilter = new IntentFilter( TransportFlag.MainActivity );
        registerReceiver( musicServiceReceiver, intentFilter );

        LoadMusic();

        //设置播放线程
        RunnablePlay = new Runnable() {
            @Override
            public void run() {
                play = true;
                mediaplayer.start();
                HandlerService.post( RunnableSeekbar );
                HandlerService.post( RunnableLyric );
            }
        };

        //设置拖动条线程
        RunnableSeekbar = new Runnable() {
            @Override
            public void run() {
                if (play) {
                    try {
                        Intent Intent_UpdateSeekBar = new Intent( TransportFlag.MusicService );
                        Intent_UpdateSeekBar.putExtra( "SeekBarTo", mediaplayer.getCurrentPosition() );
                        Intent_UpdateSeekBar.putExtra( "TextViewTo", new SimpleDateFormat( "mm:ss" ).format( new Date( mediaplayer.getCurrentPosition() ) ) );
                        Intent_UpdateSeekBar.putExtra( TransportFlag.State, TransportFlag.SeekTo );
                        //更新拖动条信息给MainActivity      测试完毕
                        sendBroadcast( Intent_UpdateSeekBar );
                        HandlerService.postDelayed( RunnableSeekbar, 1000 );
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        //设置歌词线程
        RunnableLyric = new Runnable() {
            @Override
            public void run() {
                if (play) {
                    Intent Intent_UpdateLyric = new Intent( TransportFlag.MusicService );
                    Intent_UpdateLyric.putExtra( "CurrentPosition", mediaplayer.getCurrentPosition() );
                    Intent_UpdateLyric.putExtra( TransportFlag.State, TransportFlag.LyricTo );
                    //更新歌词给LyricActivity
                    sendBroadcast( Intent_UpdateLyric );
                    HandlerService.postDelayed( RunnableLyric, 300 );
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
        HandlerService.removeCallbacks( RunnableLyric );
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
        ScanMusicItem();
        ScanLyric();
        while (!(isScanMusicItemFinished && isScanLyricFinished)) {
        }
        MatchMusicItemWithLyric();
        while (!isMatchFinished) {
        }
        sendMusicList();
        ModeSetting( mode );
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
     * 扫描音乐信息
     **/
    public void ScanMusicItem() {
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
                    Status_MusicItem = STATUS_SUCCESSFUL;
                }
                isScanMusicItemFinished = STATUS_FINISH;
            }
        } ).start();
    }

    /**
     * 扫描歌词文件
     **/
    public void ScanLyric() {
        //检测SD卡是否存在
        new Thread( new Runnable() {
            @Override
            public void run() {
                if (Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED )) {
                    Traverse( Environment.getExternalStorageDirectory() );
                    isScanLyricFinished = STATUS_FINISH;
                }
            }
        } ).start();
    }

    /**
     * 遍历手机SD卡查找匹配的歌词文件
     **/
    private void Traverse(File root) {
        File files[] = root.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    Traverse( f );
                } else {
                    if (f.getName().endsWith( ".lrc" )) {
                        LyricList.add( f );
                        Status_Lyric = STATUS_SUCCESSFUL;
                    }
                }
            }
        }
    }

    /**
     * 解析歌词文件
     **/
    public void Parsing(MusicBean music) {
        final MusicBean musicBean = music;
        File file = new File( musicBean.getLyricPath() );
        ArrayList<LyricItem> LyricArray = new ArrayList<>();
        try {
            FileInputStream fileInputStream = new FileInputStream( file );
            InputStreamReader inputStreamReader = new InputStreamReader( fileInputStream, "utf-8" );
            BufferedReader bufferedReader = new BufferedReader( inputStreamReader );
            String s;
            int index;
            while ((s = bufferedReader.readLine()) != null) {
                if ((index = s.indexOf( "[ar:" )) != -1) {
                    //     this.strArtist = s.substring( index + 4, s.indexOf( "]" ) );
                } else if ((index = s.indexOf( "[ti:" )) != -1) {
                    //     this.strTitle = s.substring( index + 4, s.indexOf( "]" ) );
                } else if ((index = s.indexOf( "[al:" )) != -1) {
                    //     this.strAlbum = s.substring( index + 4, s.indexOf( "]" ) );
                } else if ((index = s.indexOf( "[by:" )) != -1) {
                    //     this.strBy = s.substring( index + 4, s.indexOf( "]" ) );
                } else if ((index = s.indexOf( "[offset:" )) != -1) {
                    //     this.offset = Integer.parseInt( s.substring( index + 8, s.indexOf( "]" ) ) );
                } else if (s.indexOf( ":" ) != -1 && s.indexOf( "." ) != -1) {
                    //分离出歌词内容
                    String StrLyric = s.substring( s.lastIndexOf( "]" ) + 1 );
                    //分离出时间
                    String tempTime = s.substring( 0, s.lastIndexOf( "]" ) );
                    //多个时间点重复相同歌词
                    if (tempTime.indexOf( "][" ) != -1) {
                        String[] temp1 = tempTime.split( "]" );
                        for (String str : temp1) {
                            LyricItem lyricItem = new LyricItem();
                            int time;
                            int minute = Integer.parseInt( str.substring( str.indexOf( "[" ) + 1, str.indexOf( ":" ) ) );
                            int second = Integer.parseInt( str.substring( str.indexOf( ":" ) + 1, str.indexOf( "." ) ) );
                            int millisecond = Integer.parseInt( str.substring( str.indexOf( "." ) + 1 ) );
                            time = minute * 60000 + second * 1000 + millisecond;
                            lyricItem.setLyric( StrLyric );
                            lyricItem.setTime( time );
                            LyricArray.add( lyricItem );
                        }
                    }
                    //一个时间点对应一个歌词
                    else {
                        LyricItem lyricItem = new LyricItem();
                        int time;
                        int minute = Integer.parseInt( tempTime.substring( tempTime.indexOf( "[" ) + 1, tempTime.indexOf( ":" ) ) );
                        int second = Integer.parseInt( tempTime.substring( tempTime.indexOf( ":" ) + 1, tempTime.indexOf( "." ) ) );
                        int millisecond = Integer.parseInt( tempTime.substring( tempTime.indexOf( "." ) + 1 ) );
                        time = minute * 60000 + second * 1000 + millisecond;
                        lyricItem.setLyric( StrLyric );
                        lyricItem.setTime( time );
                        LyricArray.add( lyricItem );
                    }
                }
            }
            bufferedReader.close();
            inputStreamReader.close();
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collections.sort( LyricArray, new SortByTime() );
        musicBean.setLyricList( LyricArray );
    }

    /**
     * 将歌词和音乐匹配
     **/
    public void MatchMusicItemWithLyric() {
        if (Status_MusicItem && Status_Lyric) {
            new Thread( new Runnable() {
                @Override
                public void run() {
                    for (MusicBean musicBean : mMusicList) {
                        for (File file : LyricList) {
                            if (musicBean.getMusicName().replace( " ", "" ).contains( file.getName().replace( " ", "" ).replace( ".lrc", "" ) )
                                    || file.getName().replace( " ", "" ).replace( ".lrc", "" ).contains( musicBean.getMusicName().replace( " ", "" ) )) {
                                musicBean.setLyricPath( file.getAbsolutePath() );
                                Parsing( musicBean );
                                break;
                            }
                        }
                    }
                    isMatchFinished = STATUS_FINISH;
                }
            } ).start();
        }
    }

    /**
     * 发送列表给Service
     **/
    public void sendMusicList() {
        Intent Intent_SendMusicList = new Intent( TransportFlag.MusicService );
        Intent_SendMusicList.putExtra( "mMusicList", mMusicList );
        Intent_SendMusicList.putExtra( TransportFlag.State, TransportFlag.LoadMusic );
        //将播放列表发给MainActivity        测试完毕
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
        play = true;
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
        play = true;
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
                mediaplayer.setOnPreparedListener( new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        Intent Intent_SeekPrepare = new Intent( TransportFlag.MusicService );
                        Intent_SeekPrepare.putExtra( "SeekBarMax", mediaplayer.getDuration() );
                        Intent_SeekPrepare.putExtra( "TextViewTo", new SimpleDateFormat( "mm:ss" ).format( new Date( mediaplayer.getDuration() ) ) );
                        Intent_SeekPrepare.putExtra( TransportFlag.State, TransportFlag.SeekPrepare );
                        //发送拖动条最大值和置0给Activity      测试完毕
                        sendBroadcast( Intent_SeekPrepare );

                        Intent Intent_Prepare = new Intent( TransportFlag.MusicService );
                        Intent_Prepare.putExtra( TransportFlag.Prepare, mMusicList.get( ItemLocationIndex ) );
                        Intent_Prepare.putExtra( TransportFlag.State, TransportFlag.Prepare );
                        //发送准备指令给Activity
                        sendBroadcast( Intent_Prepare );
                    }
                } );
                mediaplayer.prepare();
                mediaplayer.setOnCompletionListener( new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        play = false;
                        PlayArrayIndex++;
                        PlayArrayIndex = PlayArrayIndex % mMusicList.size();
                        ItemLocationIndex = PlayArray[PlayArrayIndex];

                        Intent Intent_NextItem = new Intent( TransportFlag.MusicService );
                        Intent_NextItem.putExtra( TransportFlag.NextItem, mMusicList.get( ItemLocationIndex ).getMusicName() );
                        Intent_NextItem.putExtra( TransportFlag.State, TransportFlag.NextItem );
                        //发送下一首给MainActivity用于Toast     测试完毕
                        sendBroadcast( Intent_NextItem );
                        HandlerService.postDelayed( new Runnable() {
                            @Override
                            public void run() {
                                playMusic( mMusicList.get( ItemLocationIndex ).getMusicPath() );
                            }
                        }, 2000 );
                    }
                } );
            } catch (Exception e) {
                e.printStackTrace();
            }
            HandlerService.postDelayed( RunnablePlay, 1000 );
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
            // 如果是拨打电话
            if (intent.getAction().equals( Intent.ACTION_NEW_OUTGOING_CALL )) {
                mediaplayer.pause();
                mainActivity.mbtnPlay.setText( "PAUSE" );
            }
            // 如果是来电
            else {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService( Service.TELEPHONY_SERVICE );
                switch (telephonyManager.getCallState()) {
                    case TelephonyManager.CALL_STATE_RINGING:       //响铃
                        mediaplayer.pause();
                        mainActivity.mbtnPlay.setText( "PAUSE" );
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:       //接听
                        mediaplayer.pause();
                        mainActivity.mbtnPlay.setText( "PAUSE" );
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:          //挂断
                        mediaplayer.start();
                        mainActivity.mbtnPlay.setText( "PLAY" );
                        break;
                }
            }
            String path;
            int progress;
            String state = intent.getStringExtra( TransportFlag.State );
            //Log.e( TransportFlag.State, state );
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
                    play = true;
                    mediaplayer.start();
                    break;
                case TransportFlag.Pause:                                       //接收媒体播放器暂停     测试完毕
                    mediaplayer.pause();
                    play = false;
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

    /**
     * 按时间排序
     **/
    class SortByTime implements Comparator {
        public int compare(Object o1, Object o2) {
            LyricItem l1 = (LyricItem) o1;
            LyricItem l2 = (LyricItem) o2;
            if (l1.getTime() > l2.getTime())
                return 1;
            else if (l1.getTime() == l2.getTime()) {
                return 0;
            }
            return -1;
        }
    }
}
