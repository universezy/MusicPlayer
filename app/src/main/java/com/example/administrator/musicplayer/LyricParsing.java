package com.example.administrator.musicplayer;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class LyricParsing {
    private String MusicName;
    private String strArtist;
    private String strTitle;
    private String strAlbum;
    private String strBy;
    private int offset;
    private int startTime;
    private String strLyric;
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

    // 遍历接收一个文件路径，然后把文件子目录中的所有文件遍历并输出来
    private void Traverse(File root) {
        File files[] = root.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    Traverse( f );
                } else {
                    if (f.getName().endsWith( ".lrc" ) && f.getName().contains( MusicName )) {
                        this.pathLyric = f.getAbsolutePath();
                        break;
                    }
                }
            }
        }
    }

    public void Parsing() {
        File file = new File( this.pathLyric );
        try {
            FileInputStream fileInputStream = new FileInputStream( file );
            InputStreamReader inputStreamReader = new InputStreamReader( fileInputStream, "utf-8" );
            BufferedReader bufferedReader = new BufferedReader( inputStreamReader );
            String s;
            int index;
            int Second, minute;
            while ((s = bufferedReader.readLine()) != null) {
                if ((index = s.indexOf( "[ar:" )) != -1) {
                    this.strArtist = s.substring( index + 1, s.indexOf( "]" ) );
                } else if ((index = s.indexOf( "[ti:" )) != -1) {
                    this.strTitle = s.substring( index + 1, s.indexOf( "]" ) );
                } else if ((index = s.indexOf( "[al:" )) != -1) {
                    this.strAlbum = s.substring( index + 1, s.indexOf( "]" ) );
                } else if ((index = s.indexOf( "[by:" )) != -1) {
                    this.strBy = s.substring( index + 1, s.indexOf( "]" ) );
                } else if ((index = s.indexOf( ":" )) != -1 && (index = s.indexOf( "." )) != -1) {

                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
