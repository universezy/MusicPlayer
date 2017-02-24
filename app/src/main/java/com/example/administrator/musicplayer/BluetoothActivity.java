package com.example.administrator.musicplayer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity implements View.OnClickListener, ListView.OnItemClickListener {
    //发送的文件路径
    private String filePath;
    //接收器
    protected BluetoothReceiver bluetoothReceiver = new BluetoothReceiver();
    //蓝牙适配器
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    //蓝牙设备列表
    private List<BluetoothDevice> mBluetoothDeviceList = new ArrayList<>();
    //建立连接的蓝牙
    private BluetoothDevice bluetoothDevice;
    //蓝牙列表适配器
    private ArrayAdapter arrayAdapter;
    //蓝牙信息列表
    private List<String> mDeviceList = new ArrayList<>();
    //蓝牙Socket连接
    private BluetoothSocket bluetoothSocket;
    //蓝牙成功打开
    final static int REQUEST_ENABLE_BT = 0;
    //蓝牙开启状态
    boolean isBluetoothOpen;
    //列表管理器
    private Handler HandlerList = new Handler();
    //列表视图
    private ListView listView;
    //按钮
    private Button mbtnBack, mbtnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_bluetooth );

        Bundle bundle = this.getIntent().getExtras();
        filePath = bundle.getString( "filePath" );

        InitLayout();
        ScanBluetooth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                //成功
                Toast.makeText( this, "Start bluetooth successfully.", Toast.LENGTH_SHORT ).show();
                isBluetoothOpen = true;
            } else {
                //失败
                Toast.makeText( this, "Start bluetooth failed.", Toast.LENGTH_SHORT ).show();
                isBluetoothOpen = false;
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver( bluetoothReceiver );
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnBack:
                bluetoothAdapter.disable();
                BluetoothActivity.this.finish();
                break;
            case R.id.btnSend:
                SendData();
                break;
            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if ((arrayAdapter.getItem( position )) != null) {
            for (BluetoothDevice bluetoothDevice : mBluetoothDeviceList) {
                if (arrayAdapter.getItem( position ).equals( bluetoothDevice.getName() )) {
                    ConnectDevice( bluetoothDevice );
                    break;
                }
            }
        }
    }

    private void InitLayout() {
        arrayAdapter = new ArrayAdapter( getApplicationContext(), R.layout.item_music_list_layout );

        listView = (ListView) findViewById( R.id.tvDeviceList );
        listView.setAdapter( arrayAdapter );
        listView.setOnItemClickListener( this );

        mbtnBack = (Button) findViewById( R.id.btnBack );
        mbtnBack.setOnClickListener( this );
        mbtnSend = (Button) findViewById( R.id.btnSend );
        mbtnSend.setOnClickListener( this );
    }

    public void ScanBluetooth() {
        //检测手机是否有蓝牙模块
        if (bluetoothAdapter == null) {
            Toast.makeText( this, "No bluetooth device found.", Toast.LENGTH_SHORT ).show();
            return;
        }
        if(bluetoothAdapter.isEnabled()){
            Log.e( "Enabled","Enabled" );
        }else{
            bluetoothAdapter.enable();
            Log.e( "Disabled","Disabled" );
        }
        //打开系统的蓝牙设置
        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
            startActivityForResult( intent, REQUEST_ENABLE_BT );
        }
        if (!isBluetoothOpen) {
            Toast.makeText( this, "Fail to start bluetooth.", Toast.LENGTH_SHORT ).show();
            //return;
        }
        //设置过滤器
        IntentFilter intentFilter = new IntentFilter( BluetoothDevice.ACTION_FOUND );
        //注册接收器
        registerReceiver( bluetoothReceiver, intentFilter );
        //调用startDiscovery()来扫描周围可见设备
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
    }

    public void LoadDevice() {
        new Thread( new Runnable() {
            @Override
            public void run() {
                HandlerList.post( new Runnable() {
                    @Override
                    public void run() {
                        arrayAdapter.add( mDeviceList );
                        arrayAdapter.notifyDataSetChanged();
                    }
                } );
            }
        } ).start();
    }

    private void ConnectDevice(BluetoothDevice bd) {
        this.bluetoothDevice = bd;
        bluetoothAdapter.cancelDiscovery();
        new Thread( new Runnable() {
            @Override
            public void run() {
                bluetoothDevice = bluetoothAdapter.getRemoteDevice( bluetoothDevice.getAddress() );
                // 连接建立之前的先配对
                try {
                    if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                        Method creMethod = BluetoothDevice.class.getMethod( "createBond" );
                        Toast.makeText( BluetoothActivity.this, "Matching.", Toast.LENGTH_SHORT ).show();
                        creMethod.invoke( bluetoothDevice );
                    } else {
                        Toast.makeText( BluetoothActivity.this, "Matched.", Toast.LENGTH_SHORT ).show();
                    }
                } catch (Exception e) {
                    Toast.makeText( BluetoothActivity.this, "Fail to match.", Toast.LENGTH_SHORT ).show();
                }
                try {
                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord( UUID.randomUUID() );
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    bluetoothSocket.connect();
                    Toast.makeText( BluetoothActivity.this, "Connect successful." + bluetoothSocket.isConnected(), Toast.LENGTH_SHORT ).show();
                } catch (IOException e) {
                    Toast.makeText( BluetoothActivity.this, "Connect failed." + e.toString() + bluetoothSocket.isConnected(), Toast.LENGTH_SHORT ).show();
                    try {
                        if (bluetoothSocket != null) {
                            bluetoothSocket.close();
                            bluetoothSocket = null;
                        }
                    } catch (IOException e2) {
                        Toast.makeText( BluetoothActivity.this, "Fail to close socket." + bluetoothSocket.isConnected(), Toast.LENGTH_SHORT ).show();
                    }
                }
            }
        } ).start();

    }

    private void SendData() {
        if (filePath == null) {
            Toast.makeText( this, "No file found.", Toast.LENGTH_SHORT ).show();
            return;
        }
        if (!bluetoothSocket.isConnected()) {
            Toast.makeText( this, "Bluetooth is disconnected", Toast.LENGTH_SHORT ).show();
            return;
        }
        // 传输音乐
        // 获取Socket的OutputStream对象用于发送数据。
        // 创建一个InputStream用户读取要发送的文件。
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream( filePath );
            // 获取Socket的OutputStream对象用于发送数据。
            outputStream = bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 创建一个byte类型的buffer字节数组，用于存放读取的本地文件
        byte[] buffer = new byte[1024];
        int temp;
        // 循环读取文件
        try {
            while ((temp = inputStream.read( buffer )) != -1) {
                // 把数据写入到OuputStream对象中
                outputStream.write( temp );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 发送读取的数据到服务端
        try {
            outputStream.flush();
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //接受intent
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals( action )) {
                //从intent取出远程蓝牙设备
                BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
                mDeviceList.add( device.getName() );
                mBluetoothDeviceList.add( device );
                for (BluetoothDevice s : mBluetoothDeviceList) {
                    Log.e( "Name", s.getName() );
                    Log.e( "Address", s.getAddress() );
                }
                LoadDevice();
            }
        }
    }
}
