package com.example.administrator.musicplayer;

import java.io.Serializable;

class MusicBean implements Serializable {
    private String musicName;
    private String musicPath;
    private String musicArtist;
    private String musicAlbum;

    String getMusicName() {
        return musicName;
    }

    void setMusicName(String musicName) {
        this.musicName = musicName;
    }

    String getMusicPath() {
        return musicPath;
    }

    void setMusicPath(String musicPath) {
        this.musicPath = musicPath;
    }

    String getMusicArtist() {
        return musicArtist;
    }

    void setMusicArtist(String musicArtist) {
        this.musicArtist = musicArtist;
    }

    String getMusicAlbum() {
        return musicAlbum;
    }

    void setMusicAlbum(String musicAlbum) {
        this.musicAlbum = musicAlbum;
    }
}