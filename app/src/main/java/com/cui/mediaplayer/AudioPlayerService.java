package com.cui.mediaplayer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;

/**
 * Created by cuiyang on 16/8/18.
 */

public class AudioPlayerService extends Service {

    public static final String SESSION_TAG = "mMusic";
    public static final String ACTION_PLAY = "play";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_NEXT = "next";
    public static final String ACTION_LAST = "last";
    public static final String PARAM_TRACK_URI = "uri";
    public static final String PLAY_DATA = "data";

    private MediaPlayerHelper mediaPlayerHelper;
    private AudioManager mAudioManager;


    public class ServiceBinder extends Binder {

        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }

    private Binder mBinder = new ServiceBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //获取audio service
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaPlayerHelper = new MediaPlayerHelper(getApplicationContext());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:
                    mediaPlayerHelper.getmMediaController().getTransportControls().play();
                    break;
                case ACTION_NEXT:
                    mediaPlayerHelper.getmMediaController().getTransportControls().skipToNext();
                    break;
                case ACTION_LAST:
                    mediaPlayerHelper.getmMediaController().getTransportControls().skipToPrevious();
                    break;
                case ACTION_PAUSE:
                    mediaPlayerHelper.getmMediaController().getTransportControls().pause();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayerHelper.destoryService();
    }

    public MediaSessionCompat.Token getMediaSessionToken() {
        return mediaPlayerHelper.getMediaSessionToken();
    }
    public MediaPlayerHelper getMediaPlayerHelper() {
        return mediaPlayerHelper;
    }
}
