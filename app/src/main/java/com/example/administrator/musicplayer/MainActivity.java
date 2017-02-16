package com.example.administrator.musicplayer;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.connect.share.QQShare;
import com.tencent.tauth.Tencent;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        NavigationView.OnNavigationItemSelectedListener,
        AdapterView.OnItemClickListener,
        SearchView.OnQueryTextListener,
        SeekBar.OnSeekBarChangeListener {

    /*****************************************************************************************
     *************************************    全局变量    *************************************
     *****************************************************************************************/

    /**
     * 布局组件
     **/
    //按钮
    public Button mbtnMore, mbtnMode, mbtnLast, mbtnNext, mbtnPlay;
    //搜索视图
    public SearchView msvSearch;
    //文本视图
    private TextView mtvName, mtvCurrentProgress, mtvTotalProgress;
    //列表视图
    private ListView mlvList;
    //拖动条
    private SeekBar msbPlayer;
    //抽屉布局
    private DrawerLayout mdlMain;
    //导航视图
    public NavigationView mnvMain;
    /**
     * 工具实例
     **/
    //媒体播放器
    private MediaPlayer mediaplayer = new MediaPlayer();
    //列表适配器
    private ListAdapter mlaList;
    //列表管理器
    private Handler mHandlerList = new Handler();
    //拖动条管理器
    private Handler mHandlerSeekbar = new Handler();
    //播放线程
    private Runnable mRunnablePlay;
    //拖动条线程
    private Runnable mRunnableSeekbar;
    //腾讯API
    protected Tencent mTencent;

    /**
     * 自定义元素
     **/
    //播放列表
    public static ArrayList<MusicBean> mMusicList = new ArrayList<>();
    //播放列表索引，初始化为第一首
    private int ItemLocationIndex = 0;
    //播放顺序数组
    private int[] PlayArray;
    //播放顺序数组索引
    private int PlayArrayIndex = 0;
    //播放模式序号
    private int choice = 0;
    //应用运行状态
    private boolean isApplicationAlive;
    //分享类型
    final int ShareByQQ = 0, ShareByWechat = 1;

    /*****************************************************************************************
     * *************************************    分割线    **************************************
     *****************************************************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        mTencent = Tencent.createInstance( R.string.APPID + "", this );
        InitLayout();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (null != mTencent)
            Tencent.onActivityResultData( requestCode, resultCode, data, new ShareListener() );
    }

    @Override
    protected void onDestroy() {
        //将线程销毁掉
        mHandlerSeekbar.removeCallbacks( mRunnableSeekbar );
        super.onDestroy();
    }

    /**
     * 初始化布局
     **/
    public void InitLayout() {
        //设置应用运行状态
        isApplicationAlive = true;

        //设置播放线程
        mRunnablePlay = new Runnable() {
            @Override
            public void run() {
                mediaplayer.start();
                mHandlerSeekbar.post( mRunnableSeekbar );
            }
        };

        //设置拖动条线程
        mRunnableSeekbar = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isApplicationAlive) {
                        msbPlayer.setProgress( mediaplayer.getCurrentPosition() );
                        mtvCurrentProgress.setText( new SimpleDateFormat( "mm:ss" ).format( new Date( mediaplayer.getCurrentPosition() ) ) );
                        mHandlerSeekbar.postDelayed( mRunnableSeekbar, 1000 );
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        };

        //设置列表适配器
        mlaList = new ListAdapter( getApplicationContext(), R.layout.item_music_list_layout );

        //加载歌曲
        AsyncLoadMusic();

        //设置列表视图
        mlvList = (ListView) findViewById( R.id.lvList );
        mlvList.setAdapter( mlaList );
        mlvList.setTextFilterEnabled( true );
        mlvList.setOnItemClickListener( this );

        //设置搜索视图
        msvSearch = (SearchView) findViewById( R.id.svSearch );
        msvSearch.setOnQueryTextListener( this );
        msvSearch.setSubmitButtonEnabled( true );
        msvSearch.setFocusable( false );

        //设置拖动条
        msbPlayer = (SeekBar) findViewById( R.id.sb );
        msbPlayer.setOnSeekBarChangeListener( this );

        //设置抽屉视图
        mdlMain = (DrawerLayout) findViewById( R.id.drawer_layout );

        //设置文本视图
        mtvName = (TextView) findViewById( R.id.tvName );
        mtvCurrentProgress = (TextView) findViewById( R.id.tvCurrentProgress );
        mtvTotalProgress = (TextView) findViewById( R.id.tvTotalProgress );

        //设置按钮
        mbtnMore = (Button) findViewById( R.id.btnMore );
        mbtnMore.setOnClickListener( this );
        mbtnMode = (Button) findViewById( R.id.btnMode );
        mbtnMode.setOnClickListener( this );
        mbtnLast = (Button) findViewById( R.id.btnLast );
        mbtnLast.setOnClickListener( this );
        mbtnNext = (Button) findViewById( R.id.btnNext );
        mbtnNext.setOnClickListener( this );
        mbtnPlay = (Button) findViewById( R.id.btnPlay );
        mbtnPlay.setOnClickListener( this );

        //设置导航视图
        mnvMain = (NavigationView) findViewById( R.id.nav_view );
        mnvMain.setNavigationItemSelectedListener( this );
    }

    /*****************************************************************************************
     *************************************    组件接口    *************************************
     *****************************************************************************************/

    /**
     * Button设置
     **/
    @Override
    public void onClick(View v) {
        msvSearch.clearFocus();
        if (mMusicList != null && mMusicList.size() != 0) {
            switch (v.getId()) {
                case R.id.btnMore:          //扩展
                    if (!mdlMain.isDrawerOpen( GravityCompat.START )) {
                        mdlMain.openDrawer( GravityCompat.START );
                    }
                    break;
                case R.id.btnMode:          //模式
                    setPlayMode();
                    break;
                case R.id.btnLast:          //上一首
                    LastMusic();
                    break;
                case R.id.btnNext:          //下一首
                    NextMusic();
                    break;
                case R.id.btnPlay:          //播放
                    Play_Pause();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 返回键关闭抽屉界面
     **/
    @Override
    public void onBackPressed() {
        mdlMain = (DrawerLayout) findViewById( R.id.drawer_layout );
        if (mdlMain.isDrawerOpen( GravityCompat.START )) {
            mdlMain.closeDrawer( GravityCompat.START );
        } else {
            super.onBackPressed();
        }
    }

    /**
     * NavigationView设置
     **/
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_setToRingtone) {             //设为铃声
            MessageToUser();
            //setMusicTo( RingtoneManager.TYPE_RINGTONE );
        } else if (id == R.id.nav_setToNotification) {  //设为提示音
            MessageToUser();
            //setMusicTo( RingtoneManager.TYPE_RINGTONE );
        } else if (id == R.id.nav_setToAlarm) {         //设为闹钟
            MessageToUser();
            //setMusicTo( RingtoneManager.TYPE_RINGTONE );
        } else if (id == R.id.nav_sendByQQ) {           //通过QQ发送
            MessageToUser();
            //SendMusicTo();
        } else if (id == R.id.nav_sendByBluetooth) {    //通过蓝牙发送
            MessageToUser();
            //SendMusicTo();
        } else if (id == R.id.nav_shareByQQ) {          //通过QQ分享    已实现
            ShareMusicTo( ShareByQQ );
        } else if (id == R.id.nav_shareByWechat) {      //通过微信分享
            //ShareMusicTo(ShareByWechat);
            MessageToUser();
        }else if (id == R.id.nav_minimize) {            //最小化到后台播放
            PlayInBackground();
        }else if (id == R.id.nav_version) {             //版本号   已实现
            ShowVersion();
        } else if (id == R.id.nav_exit) {               //退出应用  已实现
            Exit();
        } else {
            mdlMain = (DrawerLayout) findViewById( R.id.drawer_layout );
            mdlMain.closeDrawer( GravityCompat.START );
        }
        return true;
    }

    /**
     * ListView设置
     **/
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        msvSearch.clearFocus();
        ItemLocationIndex = position;
        PlayArrayIndex = position;
        if ((mlaList.getItem( ItemLocationIndex )) != null) {
            playMusic( ((MusicBean) mlaList.getItem( ItemLocationIndex )).getMusicPath() );
            mtvName.setText( ((MusicBean) mlaList.getItem( ItemLocationIndex )).getMusicName() );
        }
    }

    /**
     * SearchView设置
     **/
    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.i( "Nomad", "onQueryTextSubmit" );
        if (!(TextUtils.isEmpty( query ))) {
            UpdateList( 0, query );
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty( newText )) {
            UpdateList( 1, newText );
        }
        return true;
    }

    /**
     * SeekBar设置
     **/
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        msvSearch.clearFocus();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {  //停止拖动
        mediaplayer.seekTo( seekBar.getProgress() );
    }

    /*****************************************************************************************
     ************************************    自定义方法    ************************************
     *****************************************************************************************/

    /**
     * 异步线程载入歌曲
     **/
    public void AsyncLoadMusic() {
        new Thread( new Runnable() {
            @Override
            public void run() {
                mMusicList.clear();
                LoadMusic( Environment.getExternalStorageDirectory() + File.separator );
                mHandlerList.post( new Runnable() {
                    @Override
                    public void run() {
                        mlaList.setList( mMusicList );
                    }
                } );
            }
        } ).start();
    }

    /**
     * 载入歌曲
     **/
    public void LoadMusic(String dirName) {
        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.DATA + " like ?",
                new String[]{dirName + "%"},
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER );
        if (cursor == null) return;

        MusicBean music;
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            //如果不是音乐
            String isMusic = cursor.getString( cursor.getColumnIndexOrThrow( MediaStore.Audio.Media.IS_MUSIC ) );
            if (isMusic != null && isMusic.equals( "" )) continue;
            music = new MusicBean();
            String path;
            if ((path = cursor.getString( cursor.getColumnIndexOrThrow( MediaStore.Audio.Media.DATA ) )).endsWith( ".mp3" )) {
                music.setMusicName( cursor.getString( cursor.getColumnIndexOrThrow( MediaStore.Audio.Media.TITLE ) ) );
                music.setMusicPath( path );
                mMusicList.add( music );
            }
        }
        cursor.close();
        //设置默认的播放顺序为顺序播放
        PlayArray = new int[mMusicList.size()];
        for (int i = 0; i < PlayArray.length; i++) {
            PlayArray[i] = i;
            Log.e( "Name  ", mMusicList.get( i ).getMusicName() );
            Log.e( "Path  ", mMusicList.get( i ).getMusicPath() );
        }
    }

    /**
     * 查找歌曲
     **/
    public ArrayList<MusicBean> Search(String strSearch) {
        ArrayList<MusicBean> SearchList = new ArrayList<>();
        for (int i = 0; i < mMusicList.size(); i++) {
            if (mMusicList.get( i ).getMusicName().contains( strSearch )) {
                SearchList.add( mMusicList.get( i ) );
            }
        }
        return SearchList;
    }

    /**
     * 更新列表
     **/
    public void UpdateList(int UpdateType, String query) {
        switch (UpdateType) {
            case 0:
                mlaList.setList( Search( query ) );
                break;
            case 1:
                mlaList.setList( mMusicList );
                break;
            default:
                break;
        }
        mlvList.setAdapter( mlaList );
        mlaList.notifyDataSetChanged();
        msvSearch.clearFocus();
    }

    /**
     * 播放模式设定
     **/
    public void setPlayMode() {
        /** 消息框形式弹出选项：顺序播放，单曲循环，随机播放。默认：顺序播放 **/
        new AlertDialog.Builder( MainActivity.this )
                .setTitle( "Set Mode" )
                .setIcon( android.R.drawable.ic_dialog_info )
                .setSingleChoiceItems( getResources().getStringArray( R.array.play_mode ), 0,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                choice = which;
                            }
                        }
                )
                .setPositiveButton( "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mbtnMode.setText( getResources().getStringArray( R.array.play_mode )[choice] );
                        dialog.dismiss();
                        if (choice == 2) {
                            int temp;
                            //生成随机播放列表
                            Random random = new Random();
                            for (int i = 0; i < PlayArray.length; i++) {
                                int j = random.nextInt( PlayArray.length );
                                temp = PlayArray[i];
                                PlayArray[i] = PlayArray[j];
                                PlayArray[j] = temp;
                            }
                        }
                    }
                } )
                .show();
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
     * 播放和暂停切换
     **/
    public void Play_Pause() {
        if (mtvName.getText().toString().equals( "Music Name" )) {
            playMusic( mMusicList.get( ItemLocationIndex ).getMusicPath() );
        } else {
            switch (mbtnPlay.getText().toString()) {
                case "PLAY":
                    mediaplayer.start();
                    mbtnPlay.setText( "PAUSE" );
                    break;
                case "PAUSE":
                    mediaplayer.pause();
                    mbtnPlay.setText( "PLAY" );
                    break;
                default:
                    break;
            }
        }
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
                    if (!(mbtnMode.getText().toString().equals( "Single Play" ))) {
                        PlayArrayIndex++;
                        PlayArrayIndex = PlayArrayIndex % mMusicList.size();
                        ItemLocationIndex = PlayArray[PlayArrayIndex];
                    }
                    Thread ThreadToast = new Thread( new Runnable() {
                        @Override
                        public void run() {
                            Looper.prepare();
                            Toast.makeText( getApplicationContext(), "Next: " + mMusicList.get( ItemLocationIndex ).getMusicName(), Toast.LENGTH_SHORT ).show();
                            Looper.loop();
                        }
                    } );
                    ThreadToast.start();
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
                    msbPlayer.setMax( mediaplayer.getDuration() );
                    mtvTotalProgress.setText( new SimpleDateFormat( "mm:ss" ).format( new Date( mediaplayer.getDuration() ) ) );
                }
            } );
        } catch (Exception e) {
            e.printStackTrace();
        }
        mHandlerSeekbar.post( mRunnablePlay );
        mbtnPlay.setText( "PAUSE" );
        mtvName.setText( mMusicList.get( ItemLocationIndex ).getMusicName() );

        /**  设置正在播放的条目背景颜色未实现 **/

    }

    /**
     * 用户提示
     **/
    public void MessageToUser() {
        new AlertDialog.Builder( this )
                .setTitle( "Unfinished" )
                .setMessage( "Application is upgrading! To be expect." )
                .setPositiveButton( "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                } ).show();
    }

    /**
     * 设置声音
     **/
    public void setMusicTo(int ringType) {
        if (mtvName.getText().toString().equals( "Music Name" )) {
            Toast.makeText( this, "Please choose music before sharing.", Toast.LENGTH_SHORT ).show();
        } else {
            boolean isRingtone = false, isNotification = false, isAlarm = false;
            final int RINGTONE = 1, NOTIFICATION = 2, ALARM = 4;
            String strDialog = "";
            switch (ringType) {
                case RINGTONE:
                    isRingtone = true;
                    strDialog = "Ringtone";
                    break;
                case NOTIFICATION:
                    isNotification = true;
                    strDialog = "Notification";
                    break;
                case ALARM:
                    isAlarm = true;
                    strDialog = "Alarm";
                    break;
                default:
                    break;
            }
            File file = new File( mMusicList.get( ItemLocationIndex ).getMusicPath() );
            ContentValues values = new ContentValues();
            values.put( MediaStore.MediaColumns.DATA, file.getAbsolutePath() );
            values.put( MediaStore.MediaColumns.TITLE, file.getName() );
            values.put( MediaStore.MediaColumns.MIME_TYPE, "audio/*" );
            values.put( MediaStore.Audio.Media.IS_RINGTONE, isRingtone );
            values.put( MediaStore.Audio.Media.IS_NOTIFICATION, isNotification );
            values.put( MediaStore.Audio.Media.IS_ALARM, isAlarm );
            values.put( MediaStore.Audio.Media.IS_MUSIC, false );
            Uri uri = MediaStore.Audio.Media.getContentUriForPath( file.getAbsolutePath() );

            Scan( new File( uri.getPath() ) );

            /**  getEncodedPath = /external/audio/media , getPath = /external/audio/media , newUri  =  null   **/

            Uri newUri = this.getContentResolver().insert( uri, values );
            RingtoneManager.setActualDefaultRingtoneUri( this, ringType, newUri );

            new AlertDialog.Builder( this )
                    .setTitle( strDialog )
                    .setMessage( mMusicList.get( ItemLocationIndex ).getMusicName() )
                    .setPositiveButton( "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    } ).show();

            Log.e( "ringtone:", RingtoneManager.getRingtone( this, newUri ).getTitle( this ) );
        }
    }

    public void Scan(File file) {
        Log.e( "---------------:", "00000000000000000000" );
        if (file.exists()) {
            Log.e( "111111111111111:", "00000000000000000000" );
        }
        if (file.isDirectory()) {
            Log.e( "Directory", file.getAbsolutePath() );
            for (File f : file.listFiles()) {
                Scan( f );
            }
            Log.e( "+++++++++++++++++", file.listFiles().length + "" );
        } else if (file.isFile()) {
            Log.e( "File", file.getAbsolutePath() );
        } else {
            Log.e( "Nothing", file.getAbsolutePath() );
        }
    }

    /**
     * 发送音乐
     **/
    public void SendMusicTo() {

    }

    /**
     * 分享音乐
     **/
    public void ShareMusicTo(int ShareBy) {
        if (mtvName.getText().equals( "Music Name" )) {
            Toast.makeText( this, "Please choose music before sharing.", Toast.LENGTH_SHORT ).show();
        } else {
            switch (ShareBy) {
                case ShareByQQ:
                    final Bundle params = new Bundle();
                    params.putInt( QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT );
                    params.putString( QQShare.SHARE_TO_QQ_TITLE, "Share music to friend" );
                    params.putString( QQShare.SHARE_TO_QQ_SUMMARY, mtvName.getText().toString() );
                    params.putString( QQShare.SHARE_TO_QQ_TARGET_URL, "https://y.qq.com/portal/search.html#page=1&searchid=1&remoteplace=txt.yqq.top&t=song&w=" + mtvName.getText().toString() );
                    params.putString( QQShare.SHARE_TO_QQ_APP_NAME, getResources().getString( R.string.app_name ) );
                    params.putInt( QQShare.SHARE_TO_QQ_EXT_INT, 0x00 );
                    mTencent.shareToQQ( this, params, new ShareListener() );
                    break;
                case ShareByWechat:

                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 后台播放
     **/
    public void PlayInBackground(){

    }


    /**
     * 显示版本号
     **/
    public void ShowVersion() {
        try {
            new AlertDialog.Builder( this )
                    .setTitle( "Version" )
                    .setMessage( getPackageManager().getPackageInfo( getPackageName(), 0 ).versionName )
                    .setPositiveButton( "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    } ).show();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 退出应用
     **/
    public void Exit() {
        if (mediaplayer.isPlaying()) {
            mediaplayer.stop();
            mediaplayer.release();
            isApplicationAlive = false;
        }
        MainActivity.this.finish();
    }
}
