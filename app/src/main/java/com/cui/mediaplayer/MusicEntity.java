package com.cui.mediaplayer;

import java.io.Serializable;

/**
 * Created by cuiyang on 16/8/21.
 */

public class MusicEntity{
    private String url;
    private String singer;
    private String musicTitle;
    private String album;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSinger() {
        return singer;
    }

    public void setSinger(String singer) {
        this.singer = singer;
    }

    public String getMusicTitle() {
        return musicTitle;
    }

    public void setMusicTitle(String musicTitle) {
        this.musicTitle = musicTitle;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }
}
