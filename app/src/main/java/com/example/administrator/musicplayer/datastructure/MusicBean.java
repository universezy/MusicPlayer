package com.example.administrator.musicplayer.datastructure;

import java.io.Serializable;
import java.util.ArrayList;

public class MusicBean implements Serializable {
    private String musicName;
    private String musicPath;
    private String musicArtist;
    private String musicAlbum;
    private String lyricPath;
    private ArrayList<LyricItem> lyricList = new ArrayList();

    public String getMusicName() {
        return musicName;
    }

    public void setMusicName(String musicName) {
        this.musicName = musicName;
    }

    public String getMusicPath() {
        return musicPath;
    }

    public void setMusicPath(String musicPath) {
        this.musicPath = musicPath;
    }

    public String getMusicArtist() {
        return musicArtist;
    }

    public void setMusicArtist(String musicArtist) {
        this.musicArtist = musicArtist;
    }

    public String getMusicAlbum() {
        return musicAlbum;
    }

    public void setMusicAlbum(String musicAlbum) {
        this.musicAlbum = musicAlbum;
    }

    public String getLyricPath() {
        return lyricPath;
    }

    public void setLyricPath(String lyricPath) {
        this.lyricPath = lyricPath;
    }

    public ArrayList<LyricItem> getLyricList() {
        return lyricList;
    }

    public void setLyricList(ArrayList<LyricItem> lyricList) {
        this.lyricList = lyricList;
    }
}