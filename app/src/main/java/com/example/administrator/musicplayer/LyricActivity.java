package com.example.administrator.musicplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by Administrator on 2017/2/28.
 */

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
    private TextView mtvName, mtvCurrentProgress, mtvTotalProgress;
    //画布
    private View view;
    //拖动条
    private SeekBar seekBar;

    MainActivity mainActivity;

    /*****************************************************************************************
     * *************************************    分割线    **************************************
     *****************************************************************************************/

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate( savedInstance );
        setContentView( R.layout.activity_music_item );

        this.mainActivity = getIntent().getParcelableExtra( "MainActivity" );
        InitLayout();
    }

    /**
     * 初始化布局
     **/
    public void InitLayout() {
        //设置画布
        view = findViewById( R.id.vLyric );

        //设置拖动条
        seekBar = (SeekBar) findViewById( R.id.sb );
        seekBar.setOnSeekBarChangeListener( this );

        //设置文本视图
        mtvName = (TextView) findViewById( R.id.tvName );
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

        UpdateUI();
    }

    /**
     * Button设置
     **/
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnBack:

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
     * ***********************************    自定义方法    ************************************
     *****************************************************************************************/

    public void UpdateUI() {
        new Thread( new Runnable() {
            @Override
            public void run() {
                mtvName.setText( mainActivity.mtvName.getText() );
                mtvCurrentProgress.setText( mainActivity.mtvCurrentProgress.getText() );
                mtvTotalProgress.setText( mainActivity.mtvTotalProgress.getText() );
                seekBar.setProgress( mainActivity.seekBar.getProgress() );
                seekBar.setMax( mainActivity.seekBar.getMax() );
                mbtnPlay.setText( mainActivity.mbtnPlay.getText() );
            }
        } ).start();
    }
}
