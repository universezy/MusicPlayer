package com.example.administrator.musicplayer;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
    public TextView mtvName, mtvCurrentProgress, mtvTotalProgress;
    //列表视图
    public ListView mlvList;
    //拖动条
    public SeekBar msbPlayer;
    //抽屉布局
    public DrawerLayout mdlMain;
    //导航视图
    public NavigationView mnvMain;

    /**
     * 工具实例
     **/
    //列表管理器
    private Handler mHandlerList = new Handler();
    //列表适配器
    public ListAdapter mlaList;
    //腾讯API
    protected Tencent mTencent;
    //接收器
    MainActivityReceiver mainActivityReceiver = new MainActivityReceiver();

    /**
     * 自定义元素
     **/
    //播放列表
    public ArrayList<MusicBean> mMusicList = new ArrayList<>();
    //搜索列表
    public ArrayList<MusicBean> mSearchList = new ArrayList<>();
    //服务状态
    public String state;
    //播放模式序号
    private int Mode = 0, mode = 0;
    //分享类型
    final int ShareByQQ = 0, ShareByWechat = 1;
    //当前播放条目
    public MusicBean CurrentItem;

    /*****************************************************************************************
     * *************************************    分割线    **************************************
     *****************************************************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        //注册接收器
        IntentFilter intentFilter = new IntentFilter( TransportFlag.MainActivity );
        registerReceiver( mainActivityReceiver, intentFilter );

        //启动后台Service
        Intent ServiceIntent = new Intent( this, MusicService.class );
        startService( ServiceIntent );

        InitLayout();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (null != mTencent)
            Tencent.onActivityResultData( requestCode, resultCode, data, new ShareListener() );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 初始化布局
     **/
    public void InitLayout() {
        //设置列表适配器
        mlaList = new ListAdapter( getApplicationContext(), R.layout.item_music_list_layout );

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
                    Intent Intent_Last = new Intent( TransportFlag.MusicService );
                    Intent_Last.putExtra( TransportFlag.state, TransportFlag.Last );
                    sendBroadcast( Intent_Last );
                    break;
                case R.id.btnNext:          //下一首
                    Intent Intent_Next = new Intent( TransportFlag.MusicService );
                    Intent_Next.putExtra( TransportFlag.state, TransportFlag.Next );
                    sendBroadcast( Intent_Next );
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
        if (id == R.id.nav_setToRingtone) {             //设为铃声                  已实现
            setMusicTo( RingtoneManager.TYPE_RINGTONE );
        } else if (id == R.id.nav_setToNotification) {  //设为提示音                已实现
            setMusicTo( RingtoneManager.TYPE_RINGTONE );
        } else if (id == R.id.nav_setToAlarm) {         //设为闹钟                  已实现
            setMusicTo( RingtoneManager.TYPE_RINGTONE );
        } else if (id == R.id.nav_sendByQQ) {           //通过QQ发送
            MessageToUser();
            //SendMusicTo();
        } else if (id == R.id.nav_sendByBluetooth) {    //通过蓝牙发送
            MessageToUser();
            //SendMusicTo();
        } else if (id == R.id.nav_shareByQQ) {          //通过QQ分享                已实现
            ShareMusicTo( ShareByQQ );
        } else if (id == R.id.nav_shareByWechat) {      //通过微信分享
            //ShareMusicTo(ShareByWechat);
            MessageToUser();
        } else if (id == R.id.nav_minimize) {            //最小化到后台播放
            PlayInBackground();
        } else if (id == R.id.nav_version) {             //版本号                   已实现
            ShowVersion();
        } else if (id == R.id.nav_exit) {               //退出应用                  已实现
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
        if ((mlaList.getItem( position )) != null) {
            Intent Intent_onItemClick = new Intent( TransportFlag.MusicService );
            Intent_onItemClick.putExtra( "position", position );
            Intent_onItemClick.putExtra( "path", ((MusicBean) mlaList.getItem( position )).getMusicPath() );
            Intent_onItemClick.putExtra( TransportFlag.state, TransportFlag.PlayList );
            //Service播放选择条目     测试完毕
            sendBroadcast( Intent_onItemClick );
            mbtnPlay.setText( "PAUSE" );
        }
    }

    /**
     * SearchView设置
     **/
    @Override
    public boolean onQueryTextSubmit(String query) {
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
        Intent Intent_SeekTo = new Intent( TransportFlag.MusicService );
        Intent_SeekTo.putExtra( TransportFlag.SeekTo, seekBar.getProgress() );
        Intent_SeekTo.putExtra( TransportFlag.state, TransportFlag.SeekTo );
        //Service控制播放器跳转至       测试完毕
        sendBroadcast( Intent_SeekTo );
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
                mHandlerList.post( new Runnable() {
                    @Override
                    public void run() {
                        mlaList.setList( mMusicList );
                        mlaList.notifyDataSetChanged();
                        CurrentItem = mMusicList.get( 0 );
                    }
                } );
            }
        } ).start();
    }

    /**
     * 发送列表给Service
     **/
    public void sendMusicList(ArrayList<MusicBean> MusicList) {
        Intent Intent_SendMusicList = new Intent( TransportFlag.MusicService );
        Intent_SendMusicList.putParcelableArrayListExtra( "mMusicList", MusicList );
        Intent_SendMusicList.putExtra( TransportFlag.state, TransportFlag.LoadMusic );
        //将播放列表发给Service        测试完毕
        sendBroadcast( Intent_SendMusicList );
    }

    /**
     * 查找歌曲
     **/
    public ArrayList<MusicBean> Search(String strSearch) {
        if (mMusicList == null) {
            Log.e( "mMusicList", "null" );
        } else {
            mSearchList.clear();
            for (int i = 0; i < mMusicList.size(); i++) {
                if (mMusicList.get( i ).getMusicName().contains( strSearch )) {
                    mSearchList.add( mMusicList.get( i ) );
                }
            }
        }
        return mSearchList;
    }

    /**
     * 更新列表
     **/
    public void UpdateList(int UpdateType, String query) {
        switch (UpdateType) {
            case 0:
                mlaList.setList( Search( query ) );
                sendMusicList( mSearchList );
                break;
            case 1:
                if (mMusicList == null) {
                    Log.e( "mMusicList", "null" );
                } else {
                    mlaList.setList( mMusicList );
                    sendMusicList( mMusicList );
                }
                break;
            default:
                break;
        }
        mlvList.setAdapter( mlaList );
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
                .setSingleChoiceItems( getResources().getStringArray( R.array.play_mode ), Mode,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mode = which;
                            }
                        }
                )
                .setPositiveButton( "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mbtnMode.setText( getResources().getStringArray( R.array.play_mode )[mode] );
                        Mode = mode;
                        Intent Intent_PlayMode = new Intent( TransportFlag.MusicService );
                        Intent_PlayMode.putExtra( TransportFlag.Mode, Mode );
                        Intent_PlayMode.putExtra( TransportFlag.state, TransportFlag.Mode );
                        //将播放模式传给Service        测试完毕
                        sendBroadcast( Intent_PlayMode );
                        dialog.dismiss();
                    }
                } )
                .show();
    }

    /**
     * 播放和暂停切换
     **/
    public void Play_Pause() {
        Intent Intent_PlayPause = new Intent( TransportFlag.MusicService );
        if (mtvName.getText().toString().equals( "Music Name" )) {
            Intent_PlayPause.putExtra( TransportFlag.state, TransportFlag.PlayDefault );
            Log.e( TransportFlag.state, TransportFlag.PlayDefault );
        } else {
            switch (mbtnPlay.getText().toString()) {
                case "PLAY":
                    Intent_PlayPause.putExtra( TransportFlag.state, TransportFlag.Play );
                    mbtnPlay.setText( "PAUSE" );
                    break;
                case "PAUSE":
                    Intent_PlayPause.putExtra( TransportFlag.state, TransportFlag.Pause );
                    mbtnPlay.setText( "PLAY" );
                    break;
                default:
                    break;
            }
        }
        //Service播放或者暂停播放器      测试完毕
        sendBroadcast( Intent_PlayPause );
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
            File file = new File( CurrentItem.getMusicPath() );
            ContentValues values = new ContentValues();
            values.put( MediaStore.MediaColumns.DATA, file.getAbsolutePath() );
            values.put( MediaStore.MediaColumns.TITLE, file.getName() );
            values.put( MediaStore.MediaColumns.MIME_TYPE, "audio/*" );
            values.put( MediaStore.Audio.Media.IS_RINGTONE, isRingtone );
            values.put( MediaStore.Audio.Media.IS_NOTIFICATION, isNotification );
            values.put( MediaStore.Audio.Media.IS_ALARM, isAlarm );
            values.put( MediaStore.Audio.Media.IS_MUSIC, false );
            Uri uri = MediaStore.Audio.Media.getContentUriForPath( file.getAbsolutePath() );

            Cursor cursor = this.getContentResolver().query( uri, null, MediaStore.MediaColumns.DATA + "=?", new String[]{file.getAbsolutePath()}, null );
            Uri newUri = null;
            if (cursor.moveToFirst() && cursor.getCount() > 0) {
                String _id = cursor.getString( 0 );
                getContentResolver().update( uri, values, MediaStore.MediaColumns.DATA + "=?", new String[]{file.getAbsolutePath()} );
                newUri = ContentUris.withAppendedId( uri, Long.valueOf( _id ) );
            }

            RingtoneManager.setActualDefaultRingtoneUri( this, ringType, newUri );

            new AlertDialog.Builder( this )
                    .setTitle( strDialog )
                    .setMessage( RingtoneManager.getRingtone( this, newUri ).getTitle( this ) )
                    .setPositiveButton( "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    } ).show();
            Log.e( "ringtone:", RingtoneManager.getRingtone( this, newUri ).getTitle( this ) );
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
            mTencent = Tencent.createInstance( R.string.APPID + "", this );
            switch (ShareBy) {
                case ShareByQQ:
                    final Bundle params = new Bundle();
                    params.putInt( QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT );
                    params.putString( QQShare.SHARE_TO_QQ_TITLE, "Share music to friend" );
                    params.putString( QQShare.SHARE_TO_QQ_SUMMARY, mtvName.getText().toString() );
                    params.putString( QQShare.SHARE_TO_QQ_TARGET_URL,
                            "https://y.qq.com/portal/search.html#page=1&searchid=1&remoteplace=txt.yqq.top&t=song&w=" + mtvName.getText().toString()
                                    .replaceAll( "(\\(.*?\\))?(\\[.*?\\])?(\\{.*?\\})?", "" ).replaceAll( ".mp3", "" ) );
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
    public void PlayInBackground() {

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
        Intent Intent_Exit = new Intent( TransportFlag.MusicService );
        Intent_Exit.putExtra( TransportFlag.state, TransportFlag.Exit );
        //发送退出信号给Service        测试完毕
        sendBroadcast( Intent_Exit );
        unregisterReceiver( mainActivityReceiver );
        MainActivity.this.finish();
    }

    /**
     * 接收器
     **/
    class MainActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int SeekBarMax, SeekBarTo;
            String TextViewTo, NextItem;
            state = intent.getStringExtra( TransportFlag.state );
            Log.e( "state", state );
            switch (state) {
                case TransportFlag.LoadMusic:                                       //接收加载音乐
                    mMusicList = (ArrayList) (intent.getParcelableArrayListExtra( "mMusicList" ));
                    LoadMusic();
                    break;
                case TransportFlag.SeekTo:                                          //接收移动拖动条至    测试完毕
                    SeekBarTo = intent.getIntExtra( "SeekBarTo", 0 );
                    TextViewTo = intent.getStringExtra( "TextViewTo" );
                    msbPlayer.setProgress( SeekBarTo );
                    mtvCurrentProgress.setText( TextViewTo );
                    break;
                case TransportFlag.NextItem:                                        //接收下一首          测试完毕
                    NextItem = intent.getStringExtra( TransportFlag.NextItem );
                    Toast.makeText( MainActivity.this, "Next: " + NextItem, Toast.LENGTH_SHORT ).show();
                    break;
                case TransportFlag.SeekPrepare:                                     //接收播放准备        测试完毕
                    SeekBarMax = intent.getIntExtra( "SeekBarMax", 0 );
                    TextViewTo = intent.getStringExtra( "TextViewTo" );
                    msbPlayer.setMax( SeekBarMax );
                    mtvTotalProgress.setText( TextViewTo );
                    mtvCurrentProgress.setText( new SimpleDateFormat( "mm:ss" ).format( new Date( 0 ) ) );
                    mbtnPlay.setText( "PAUSE" );
                    break;
                case TransportFlag.CurrentItem:                                     //接收当前条目        测试完毕
                    CurrentItem = intent.getParcelableExtra( TransportFlag.CurrentItem );
                    mtvName.setText( CurrentItem.getMusicName() );
                default:
                    break;
            }
        }
    }
}
