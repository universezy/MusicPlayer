package com.example.administrator.musicplayer.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import com.example.administrator.musicplayer.R;
import com.example.administrator.musicplayer.datastructure.MusicBean;
import com.example.administrator.musicplayer.service.MusicService;
import com.example.administrator.musicplayer.tool.ListAdapter;
import com.example.administrator.musicplayer.tool.ShareListener;
import com.example.administrator.musicplayer.tool.TransportFlag;
import com.example.administrator.musicplayer.wxapi.WXEntryActivity;
import com.tencent.connect.share.QQShare;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.tauth.Tencent;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private SearchView searchView;
    //文本视图
    public TextView mtvName, mtvCurrentProgress, mtvTotalProgress;
    //列表视图
    private ListView listView;
    //拖动条
    public SeekBar seekBar;
    //抽屉布局
    private DrawerLayout drawerLayout;
    //导航视图
    private NavigationView navigationView;

    /**
     * 工具实例
     **/
    //绑定对象
    protected MusicService.ServiceBinder binder;
    //处理器
    private Handler HandlerMain = new Handler();
    //音乐列表适配器
    private ListAdapter listAdapter;
    //QQAPI
    protected Tencent tencent;
    //微信API
    protected IWXAPI iwxapi;
    //微信接收结果类
    private WXEntryActivity wxEntryActivity = new WXEntryActivity();
    //接收器
    protected MainActivityReceiver mainActivityReceiver = new MainActivityReceiver();

    /**
     * 自定义元素
     **/
    public static MainActivity mainActivity;
    //播放列表
    private ArrayList<MusicBean> mMusicList = new ArrayList<>();
    //搜索列表
    private ArrayList<MusicBean> mSearchList = new ArrayList<>();
    //当前播放条目
    public MusicBean CurrentMusicItem;
    //播放模式序号
    public int PlayMode = 0, mode = 0;
    //分享类型
    final static int ShareByQQ = 0, ShareByWechat = 1;
    //发送类型
    final static int SendByQQ = 0, SendByWechat = 1, SendByBluetooth = 2;
    //按钮锁
    public boolean isComponentLocked = true;

    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 1;

    /*****************************************************************************************
     * *************************************    分割线    **************************************
     *****************************************************************************************/

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        this.mainActivity = this;

        //显示欢迎界面
        setContentView(R.layout.activity_main);
        findViewById(R.id.switch_main).setVisibility(View.GONE);
        //设置抽屉视图关闭手势滑动
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        initLayout();

        checkPermission();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (tencent != null)
            Tencent.onActivityResultData(requestCode, resultCode, data, new ShareListener());
    }

    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_STORAGE);
        } else {
            startService();
        }
    }

    public void startService() {
        //延迟显示主界面
        HandlerMain.postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.switch_welcome).setVisibility(View.GONE);
                findViewById(R.id.switch_main).setVisibility(View.VISIBLE);

                Toast.makeText(MainActivity.this, "Loading music resource, please wait ...", Toast.LENGTH_SHORT).show();

                //启动Service
                HandlerMain.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //注册接收器
                        IntentFilter intentFilter = new IntentFilter(TransportFlag.MusicService);
                        registerReceiver(mainActivityReceiver, intentFilter);
                        //绑定服务
                        Intent intent = new Intent(MainActivity.this, MusicService.class);
                        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
                    }
                }, 500);
            }
        }, 2000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST_READ_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startService();
            } else {
                // Permission Denied
                Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mainActivityReceiver);
        unbindService(serviceConnection);
        super.onDestroy();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (MusicService.ServiceBinder) service;  //获取其实例
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    /**
     * 初始化布局
     **/
    public void initLayout() {
        //设置列表适配器
        listAdapter = new ListAdapter(getApplicationContext(), R.layout.item_music_list_layout);
        listAdapter.setList(mMusicList);

        //设置列表视图
        listView = (ListView) findViewById(R.id.lvList);
        listView.setAdapter(listAdapter);
        listView.setTextFilterEnabled(true);
        listView.setOnItemClickListener(this);

        //设置搜索视图
        searchView = (SearchView) findViewById(R.id.svSearch);
        searchView.setOnQueryTextListener(this);
        searchView.setSubmitButtonEnabled(true);
        searchView.setFocusable(false);

        //设置拖动条
        seekBar = (SeekBar) findViewById(R.id.sb);
        seekBar.setOnSeekBarChangeListener(this);

        //设置文本视图
        mtvName = (TextView) findViewById(R.id.tvName);
        mtvName.setOnClickListener(this);
        mtvCurrentProgress = (TextView) findViewById(R.id.tvCurrentProgress);
        mtvTotalProgress = (TextView) findViewById(R.id.tvTotalProgress);

        //设置按钮
        mbtnMore = (Button) findViewById(R.id.btnMore);
        mbtnMore.setOnClickListener(this);
        mbtnMode = (Button) findViewById(R.id.btnMode);
        mbtnMode.setOnClickListener(this);
        mbtnLast = (Button) findViewById(R.id.btnLast);
        mbtnLast.setOnClickListener(this);
        mbtnNext = (Button) findViewById(R.id.btnNext);
        mbtnNext.setOnClickListener(this);
        mbtnPlay = (Button) findViewById(R.id.btnPlay);
        mbtnPlay.setOnClickListener(this);

        //设置导航视图
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //开启手势滑动
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    /*****************************************************************************************
     *************************************    组件接口    *************************************
     *****************************************************************************************/

    /**
     * Button设置
     **/
    @Override
    public void onClick(View v) {
        searchView.clearFocus();
        if (isComponentLocked) return;
        switch (v.getId()) {
            case R.id.tvName:           //歌词页
                if (mtvName.getText().toString().equals("Music Name")) return;
                Intent intent_LyricActivity = new Intent(MainActivity.this, LyricActivity.class);
                startActivity(intent_LyricActivity);
                break;
            case R.id.btnMore:          //扩展
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
                break;
            case R.id.btnMode:          //模式
                setPlayMode();
                break;
            case R.id.btnLast:          //上一首
                lastItem();
                break;
            case R.id.btnNext:          //下一首
                nextItem();
                break;
            case R.id.btnPlay:          //播放和暂停
                alterPlayAndPause();
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
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
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
        if (id == R.id.nav_shareByQQ) {                 //通过QQ分享            已实现
            shareMusicTo(ShareByQQ);
        } else if (id == R.id.nav_shareByWechat) {      //通过微信分享
            shareMusicTo(ShareByWechat);
            //messageToUser();
        } else if (id == R.id.nav_sendByQQ) {           //通过QQ发送            已实现
            sendMusicTo(SendByQQ);
        } else if (id == R.id.nav_sendByWechat) {       //通过微信发送          已实现
            sendMusicTo(SendByWechat);
        } else if (id == R.id.nav_sendByBluetooth) {    //通过蓝牙发送          已实现
            sendMusicTo(SendByBluetooth);
        } else if (id == R.id.nav_setToRingtone) {      //设为铃声              已实现
            setRingtone();
        } else if (id == R.id.nav_version) {            //版本号                已实现
            showVersion();
        } else if (id == R.id.nav_exit) {               //退出应用              已实现
            exit();
        } else {
            drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    /**
     * ListView设置
     **/
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        searchView.clearFocus();
        if ((listAdapter.getItem(position)) != null) {
            Intent Intent_onItemClick = new Intent(TransportFlag.MainActivity);
            Intent_onItemClick.putExtra("position", position);
            Intent_onItemClick.putExtra("path", ((MusicBean) listAdapter.getItem(position)).getMusicPath());
            Intent_onItemClick.putExtra(TransportFlag.State, TransportFlag.PlayList);
            //Service播放选择条目     测试完毕
            sendBroadcast(Intent_onItemClick);
            mbtnPlay.setText("PAUSE");
        }
    }

    /**
     * SearchView设置
     **/
    @Override
    public boolean onQueryTextSubmit(String query) {
        if (!(TextUtils.isEmpty(query))) {
            updateList(0, query);
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            updateList(1, newText);
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
        searchView.clearFocus();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {  //停止拖动
        updateSeekbar(seekBar);
    }

    /*****************************************************************************************
     ************************************    自定义方法    ************************************
     *****************************************************************************************/

    /**
     * 载入歌曲
     **/
    public void loadMusic() {
        CurrentMusicItem = mMusicList.get(0);
        HandlerMain.post(new Runnable() {
            @Override
            public void run() {
                listAdapter.setList(mMusicList);
                isComponentLocked = false;
            }
        });
    }

    /**
     * 发送列表给Service
     **/
    public void sendMusicList(ArrayList<MusicBean> MusicList) {
        Intent Intent_SendMusicList = new Intent(TransportFlag.MainActivity);
        Intent_SendMusicList.putExtra("mMusicList", MusicList);
        Intent_SendMusicList.putExtra(TransportFlag.State, TransportFlag.LoadMusic);
        //将播放列表发给Service        测试完毕
        sendBroadcast(Intent_SendMusicList);
    }

    /**
     * 查找歌曲
     **/
    public ArrayList<MusicBean> search(String strSearch) {
        if (mMusicList == null) {
            Log.e("mMusicList", "null");
        } else {
            mSearchList.clear();
            for (int i = 0; i < mMusicList.size(); i++) {
                if (mMusicList.get(i).getMusicName().contains(strSearch)) {
                    mSearchList.add(mMusicList.get(i));
                }
            }
        }
        return mSearchList;
    }

    /**
     * 更新列表
     **/
    public void updateList(int UpdateType, String query) {
        switch (UpdateType) {
            case 0:
                listAdapter.setList(search(query));
                sendMusicList(mSearchList);
                break;
            case 1:
                if (mMusicList == null) {
                    Log.e("mMusicList", "null");
                } else {
                    listAdapter.setList(mMusicList);
                    sendMusicList(mMusicList);
                }
                break;
            default:
                break;
        }
        listView.setAdapter(listAdapter);
        searchView.clearFocus();
    }

    /**
     * 播放模式设定
     **/
    public void setPlayMode() {
        if (mMusicList == null || mMusicList.size() == 0) {
            Toast.makeText(this, "Music list is empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        /** 消息框形式弹出选项：顺序播放，单曲循环，随机播放。默认：顺序播放 **/
        new AlertDialog.Builder(MainActivity.this)
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
     * 上一首
     **/
    public void lastItem() {
        if (mMusicList == null || mMusicList.size() == 0) {
            Toast.makeText(this, "Music list is empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent Intent_Last = new Intent(TransportFlag.MainActivity);
        Intent_Last.putExtra(TransportFlag.State, TransportFlag.Last);
        sendBroadcast(Intent_Last);
    }

    /**
     * 下一首
     **/
    public void nextItem() {
        if (mMusicList == null || mMusicList.size() == 0) {
            Toast.makeText(this, "Music list is empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent Intent_Next = new Intent(TransportFlag.MainActivity);
        Intent_Next.putExtra(TransportFlag.State, TransportFlag.Next);
        sendBroadcast(Intent_Next);
    }

    /**
     * 播放和暂停切换
     **/
    public void alterPlayAndPause() {
        if (mMusicList == null || mMusicList.size() == 0) {
            Toast.makeText(this, "Music list is empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent Intent_PlayPause = new Intent(TransportFlag.MainActivity);
        if (mtvName.getText().toString().equals("Music Name")) {
            Intent_PlayPause.putExtra(TransportFlag.State, TransportFlag.PlayDefault);
        } else {
            switch (mbtnPlay.getText().toString()) {
                case "PLAY":
                    Intent_PlayPause.putExtra(TransportFlag.State, TransportFlag.Play);
                    mbtnPlay.setText("PAUSE");
                    break;
                case "PAUSE":
                    Intent_PlayPause.putExtra(TransportFlag.State, TransportFlag.Pause);
                    mbtnPlay.setText("PLAY");
                    break;
                default:
                    break;
            }
        }
        //Service播放或者暂停播放器      测试完毕
        sendBroadcast(Intent_PlayPause);
    }

    /**
     * 发送更新拖动条给Service
     **/
    public void updateSeekbar(SeekBar seekBar) {
        Intent Intent_SeekTo = new Intent(TransportFlag.MainActivity);
        Intent_SeekTo.putExtra(TransportFlag.SeekTo, seekBar.getProgress());
        Intent_SeekTo.putExtra(TransportFlag.State, TransportFlag.SeekTo);
        //Service控制播放器跳转至       测试完毕
        sendBroadcast(Intent_SeekTo);
    }

    /**
     * 用户提示
     **/
    public void messageToUser() {
        new AlertDialog.Builder(this)
                .setTitle("Unfinished")
                .setMessage("Application is upgrading! To be expect.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    /**
     * 分享音乐
     **/
    public void shareMusicTo(int ShareBy) {
        if (mtvName.getText().equals("Music Name")) {
            Toast.makeText(this, "Please choose music before sharing.", Toast.LENGTH_SHORT).show();
            return;
        }
        final MusicBean shareItem = CurrentMusicItem;
        switch (ShareBy) {
            case ShareByQQ:
                final String strUrl1 = "https://y.qq.com/portal/search.html#page=1&searchid=1&remoteplace=txt.yqq.top&t=song&w=" +
                        shareItem.getMusicName().replaceAll("(\\(.*?\\))?(\\[.*?\\])?(\\{.*?\\})?", "")
                                .replaceAll(".mp3", "").replace("-", "").replace(" ", "%20");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        tencent = Tencent.createInstance(String.valueOf(R.string.APP_ID_QQ), MainActivity.this);
                        final Bundle params = new Bundle();
                        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
                        params.putString(QQShare.SHARE_TO_QQ_TITLE, "Share music to friend");
                        params.putString(QQShare.SHARE_TO_QQ_SUMMARY, shareItem.getMusicName());
                        params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, strUrl1);
                        params.putString(QQShare.SHARE_TO_QQ_APP_NAME, getResources().getString(R.string.app_name));
                        params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, 0x00);
                        tencent.shareToQQ(MainActivity.this, params, new ShareListener());
                    }
                }).start();
                break;
            case ShareByWechat:
                final String strUrl2 = "音乐分享 ：\n\t" + shareItem.getMusicName() + getResources().getString(R.string.share_url) + "\n- 来自 MusicPlayerOfZengYu";
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //审核分享
//                        iwxapi = WXAPIFactory.createWXAPI(MainActivity.this, String.valueOf(R.string.APP_ID_WX), true);
//                        iwxapi.handleIntent(getIntent(), wxEntryActivity);
//                        iwxapi.registerApp(String.valueOf(R.string.APP_ID_WX));
//                        if (!iwxapi.isWXAppInstalled()) {
//                            Toast.makeText(MainActivity.this, "You haven't installed Wechat",
//                                    Toast.LENGTH_SHORT).show();
//                            return;
//                        }
//                        WXWebpageObject webpageObject = new WXWebpageObject();
//                        webpageObject.webpageUrl = strUrl1;
//                        WXMediaMessage msg = new WXMediaMessage(webpageObject);
//                        msg.title = "title";
//                        msg.description = "description";
//                        SendMessageToWX.Req req = new SendMessageToWX.Req();
//                        req.transaction = String.valueOf(System.currentTimeMillis());
//                        req.message = msg;
//                        req.scene = SendMessageToWX.Req.WXSceneSession;
//                        iwxapi.sendReq(req);

                        //绕过审核分享
                        List<PackageInfo> infoList = getPackageManager().getInstalledPackages(0);
                        boolean isTargetExit = false;
                        if (!infoList.isEmpty()) {
                            for (PackageInfo packageInfo : infoList) {
                                if (packageInfo.packageName.equalsIgnoreCase("com.tencent.mm")) {
                                    isTargetExit = true;
                                    break;
                                }
                            }
                        }
                        if (isTargetExit) {
                            Intent Intent_target = new Intent(Intent.ACTION_SEND);
                            Intent_target.setType("text/plain");
                            Intent_target.putExtra(Intent.EXTRA_TEXT, strUrl2);
                            Intent_target.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI"));
                            startActivity(Intent_target);
                        } else {
                            Toast.makeText(MainActivity.this, "Please install Wechat.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).start();
                break;
            default:
                break;
        }
    }

    /**
     * 发送音乐
     **/
    public void sendMusicTo(final int SendBy) {
        if (mtvName.getText().equals("Music Name")) {
            Toast.makeText(this, "Please choose music before sharing.", Toast.LENGTH_SHORT).show();
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String filePath = CurrentMusicItem.getMusicPath();
                    File file = new File(filePath);
                    Intent Intent_target = new Intent(Intent.ACTION_SEND);
                    Intent_target.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                    Intent_target.setType("*/*");
                    List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(Intent_target, 0);
                    if (!resInfo.isEmpty()) {
                        boolean isTargetExit = false;
                        switch (SendBy) {
                            case SendByQQ:
                                for (ResolveInfo info : resInfo) {
                                    ActivityInfo activityInfo = info.activityInfo;
                                    if (activityInfo.packageName.contains("com.tencent.mobileqq")) {
                                        Intent_target.setPackage(activityInfo.packageName);
                                        isTargetExit = true;
                                        break;
                                    }
                                }
                                break;
                            case SendByWechat:
                                for (ResolveInfo info : resInfo) {
                                    ActivityInfo activityInfo = info.activityInfo;
                                    if (activityInfo.name.contains("com.tencent.mm.ui.tools.ShareImgUI")) {
                                        Intent_target.setClassName(activityInfo.packageName, activityInfo.name);
                                        isTargetExit = true;
                                        break;
                                    }
                                }
                                break;
                            case SendByBluetooth:
                                for (ResolveInfo info : resInfo) {
                                    ActivityInfo activityInfo = info.activityInfo;
                                    if (activityInfo.packageName.contains("com.android.bluetooth")) {
                                        Intent_target.setPackage(activityInfo.packageName);
                                        isTargetExit = true;
                                        break;
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                        if (isTargetExit) {
                            Intent Intent_chooser = Intent.createChooser(Intent_target, "Send Music :");
                            startActivity(Intent_chooser);
                        } else {
                            Toast.makeText(MainActivity.this, "No program to choose.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }).start();
        }
    }

    /**
     * 设置铃声
     **/
    public void setRingtone() {
        final MusicBean ringtoneMusic = CurrentMusicItem;
        if (mtvName.getText().toString().equals("Music Name")) {
            Toast.makeText(this, "Please choose music before sharing.", Toast.LENGTH_SHORT).show();
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File file = new File(ringtoneMusic.getMusicPath());
                    if (!file.exists()) {
                        Toast.makeText(MainActivity.this, "File doesn't exist.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
                    values.put(MediaStore.MediaColumns.TITLE, file.getName());
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
                    values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                    values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                    values.put(MediaStore.Audio.Media.IS_ALARM, false);
                    values.put(MediaStore.Audio.Media.IS_MUSIC, false);
                    Uri uri = MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath());
                    Cursor cursor = getContentResolver().query(uri, null, MediaStore.MediaColumns.DATA + "=?", new String[]{file.getAbsolutePath()}, null);
                    Uri newUri = null;
                    if (cursor.moveToFirst() && cursor.getCount() > 0) {
                        String _id = cursor.getString(0);
                        getContentResolver().update(uri, values, MediaStore.MediaColumns.DATA + "=?", new String[]{file.getAbsolutePath()});
                        newUri = ContentUris.withAppendedId(uri, Long.valueOf(_id));
                    }
                    final Uri NewUri = newUri;
                    Looper.prepare();
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Are you sure to set the music as ringtone ?")
                            .setMessage(RingtoneManager.getRingtone(MainActivity.this, NewUri).getTitle(MainActivity.this))
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    RingtoneManager.setActualDefaultRingtoneUri(MainActivity.this, 1, NewUri);
                                    if (RingtoneManager.getRingtone(MainActivity.this, NewUri).getTitle(MainActivity.this).replace(".mp3", "").equals(ringtoneMusic.getMusicName())) {
                                        Toast.makeText(MainActivity.this, "Set ringtone successful!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Set ringtone failed.", Toast.LENGTH_SHORT).show();
                                    }
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                    Looper.loop();
                }
            }).start();
        }
    }

    /**
     * 显示版本号
     **/
    public void showVersion() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Version")
                    .setMessage(getPackageManager().getPackageInfo(getPackageName(), 0).versionName)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 退出应用
     **/
    public void exit() {
        Intent Intent_Exit = new Intent(TransportFlag.MusicService);
        Intent_Exit.putExtra(TransportFlag.State, TransportFlag.Exit);
        //发送退出信号给Service        测试完毕
        sendBroadcast(Intent_Exit);
        MainActivity.this.finish();
    }

    /**
     * 接收器
     **/
    class MainActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int SeekBarMax, SeekBarTo;
            String strTextViewTo, strNextItem;
            String strState = intent.getStringExtra(TransportFlag.State);
            Log.e(TransportFlag.State, strState + "");
            switch (strState) {
                case TransportFlag.LoadMusic:                                       //接收加载音乐列表    测试完毕
                    mMusicList = (ArrayList<MusicBean>) (intent.getSerializableExtra("mMusicList"));
                    loadMusic();
                    break;
                case TransportFlag.SeekTo:                                          //接收移动拖动条至    测试完毕
                    SeekBarTo = intent.getIntExtra("SeekBarTo", 0);
                    strTextViewTo = intent.getStringExtra("TextViewTo");
                    seekBar.setProgress(SeekBarTo);
                    mtvCurrentProgress.setText(strTextViewTo);
                    break;
                case TransportFlag.NextItem:                                        //接收下一首          测试完毕
                    strNextItem = intent.getStringExtra(TransportFlag.NextItem);
                    isComponentLocked = true;
                    Toast.makeText(MainActivity.this, "Next: " + strNextItem, Toast.LENGTH_SHORT).show();
                    break;
                case TransportFlag.SeekPrepare:                                     //接收播放准备        测试完毕
                    SeekBarMax = intent.getIntExtra("SeekBarMax", 0);
                    strTextViewTo = intent.getStringExtra("TextViewTo");
                    seekBar.setMax(SeekBarMax);
                    mtvTotalProgress.setText(strTextViewTo);
                    mtvCurrentProgress.setText(new SimpleDateFormat("mm:ss").format(new Date(0)));
                    mbtnPlay.setText("PAUSE");
                    break;
                case TransportFlag.Prepare:                                         //接收当前条目        测试完毕
                    CurrentMusicItem = (MusicBean) intent.getSerializableExtra(TransportFlag.Prepare);
                    isComponentLocked = false;
                    mtvName.setText(CurrentMusicItem.getMusicName());
                    break;
                default:
                    break;
            }
        }
    }
}

