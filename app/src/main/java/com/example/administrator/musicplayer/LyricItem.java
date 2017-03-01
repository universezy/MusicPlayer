package com.example.administrator.musicplayer;

public class LyricItem {
    private int time;
    private String lyric;

    void setTime(int time) {
        this.time = time;
    }

    void setLyric(String lyric) {
        this.lyric = lyric;
    }

    int getTime() {
        return this.time;
    }

    String getLyric() {
        return this.lyric;
    }
}
