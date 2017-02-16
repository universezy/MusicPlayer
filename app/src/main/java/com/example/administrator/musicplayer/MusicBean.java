package com.example.administrator.musicplayer;

import android.os.Parcel;
import android.os.Parcelable;

class MusicBean implements Parcelable {
    private String musicName;
    private String musicPath;

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(musicName);
        dest.writeString(musicPath);
    }

    /**
     * 必须用 public static final 修饰符
     * 对象必须用 CREATOR
     */
    public static final Creator<MusicBean> CREATOR = new Creator<MusicBean>() {
        @Override
        public MusicBean createFromParcel(Parcel source) {
            String name = source.readString();
            String path = source.readString();

            MusicBean music = new MusicBean();
            music.setMusicName(name);
            music.setMusicPath(path);

            return music;
        }

        @Override
        public MusicBean[] newArray(int size) {
            return new MusicBean[size];
        }
    };
}