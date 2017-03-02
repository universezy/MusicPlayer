package com.example.administrator.musicplayer.datastructure;

public class LyricItem {
    private int time;
    private String lyric;

    public void setTime(int time) {
        this.time = time;
    }

    public void setLyric(String lyric) {
        this.lyric = lyric;
    }

    public int getTime() {
        return this.time;
    }

    public String getLyric() {
        return this.lyric;
    }
}
