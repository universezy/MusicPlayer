package com.example.administrator.musicplayer;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class LyricParsing {
    private String MusicName;
    private String strArtist;
    private String strTitle;
    private String strAlbum;
    private String strBy;
    private int offset;
    public ArrayList<LyricItem> LyricArray = new ArrayList<>();
    private String pathLyric;

    public LyricParsing(String MusicName) {
        this.MusicName = MusicName;
        //检测SD卡是否存在
        if (Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED )) {
            Traverse( Environment.getExternalStorageDirectory() );
        }
        if (pathLyric != null) {
            Parsing();
        }
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
                        if (f.getName().replace( ".lrc","" ).replace( " ","" ).contains( MusicName.replace( " ","" ) )) {
                            this.pathLyric = f.getAbsolutePath();
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 解析歌词文件
     **/
    public void Parsing() {
        File file = new File( this.pathLyric );
        try {
            FileInputStream fileInputStream = new FileInputStream( file );
            InputStreamReader inputStreamReader = new InputStreamReader( fileInputStream, "utf-8" );
            BufferedReader bufferedReader = new BufferedReader( inputStreamReader );
            String s;
            int index;
            while ((s = bufferedReader.readLine()) != null) {
                if ((index = s.indexOf( "[ar:" )) != -1) {
                    this.strArtist = s.substring( index + 4, s.indexOf( "]" ) );
                } else if ((index = s.indexOf( "[ti:" )) != -1) {
                    this.strTitle = s.substring( index + 4, s.indexOf( "]" ) );
                } else if ((index = s.indexOf( "[al:" )) != -1) {
                    this.strAlbum = s.substring( index + 4, s.indexOf( "]" ) );
                } else if ((index = s.indexOf( "[by:" )) != -1) {
                    this.strBy = s.substring( index + 4, s.indexOf( "]" ) );
                } else if ((index = s.indexOf( "[offset:" )) != -1) {
                    this.offset = Integer.parseInt( s.substring( index + 8, s.indexOf( "]" ) ) );
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
                            time = minute * 3600 + second * 60 + millisecond;
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
                        int millisecond = Integer.parseInt( tempTime.substring( tempTime.indexOf( "." ) + 1 ));
                        time = minute * 3600 + second * 60 + millisecond;
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
        Collections.sort(LyricArray, new SortByTime());
        for(LyricItem lyricItem:LyricArray){
            Log.e( "time",lyricItem.getTime()+"" );
            Log.e( "lyric",lyricItem.getLyric() );
        }
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
