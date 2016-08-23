package com.cui.mediaplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by cuiyang on 16/8/21.
 */

public class MediaPlayerHelper implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener {

    public MediaPlayer mMediaPlayer;
    private PlaybackStateCompat mPlaybackState;
    private MediaSessionCompat mMediaSession;
    private MediaControllerCompat mMediaController;
    private MediaPlayerUpdateCallBack playerUpdateListener;
    private Context mContext;
    private AppCompatSeekBar seekBar;
    private Timer mTimer = new Timer(); // 计时器
    private List<MusicEntity> list_data;
    private int last_index;

    private final Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int position = mMediaPlayer.getCurrentPosition();//当前位置
            int duration = mMediaPlayer.getDuration();//持续时间
            if (duration > 0) {
                // 计算进度（获取进度条最大刻度*当前音乐播放位置 / 当前音乐时长）
                long pos = (seekBar.getMax() * position / duration);
                seekBar.setProgress((int) pos);
                if (mMediaPlayer.getDuration() / 1000 == pos + 1) {
                    mPlaybackState = new PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
                            .build();
                    mMediaSession.setPlaybackState(mPlaybackState);
                    MediaPlayerReset();
                    playerUpdateListener.onCompletion(mMediaPlayer);
                }
            }
            return false;
        }
    });


    public MediaPlayerHelper(Context mContext) {
        this.mContext = mContext;
        initService(mContext);
    }


    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
//            Uri uri = extras.getParcelable(AudioPlayerService.PARAM_TRACK_URI);
//            onPlayFromUri(uri, null);
            onPlayFromUri(null, null);
        }

        //播放网络歌曲
        //就是activity和notification的播放的回调方法。都会走到这里进行加载网络音频
        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            MusicEntity entity = list_data.get(last_index);
            try {
                switch (mPlaybackState.getState()) {
                    case PlaybackStateCompat.STATE_PLAYING:
                    case PlaybackStateCompat.STATE_PAUSED:
                    case PlaybackStateCompat.STATE_NONE:
                        MediaPlayerReset();
                        //设置播放地址
                        mMediaPlayer.setDataSource(entity.getUrl());
                        //异步进行播放
                        mMediaPlayer.prepareAsync();
                        //设置当前状态为连接中
                        mPlaybackState = new PlaybackStateCompat.Builder()
                                .setState(PlaybackStateCompat.STATE_CONNECTING, 0, 1.0f)
                                .build();
                        //告诉MediaSession当前最新的音频状态。
                        mMediaSession.setPlaybackState(mPlaybackState);
                        //设置音频信息；
                        mMediaSession.setMetadata(getMusicEntity(entity.getMusicTitle(),
                                entity.getSinger(), entity.getAlbum()));
                        break;
                }
            } catch (IOException e) {
            }
        }

        @Override
        public void onPause() {
            switch (mPlaybackState.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    mMediaPlayer.pause();
                    mPlaybackState = new PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
                            .build();
                    mMediaSession.setPlaybackState(mPlaybackState);
                    updateNotification();
                    break;

            }
        }

        @Override
        public void onPlay() {
            switch (mPlaybackState.getState()) {
                case PlaybackStateCompat.STATE_PAUSED:
                    mMediaPlayer.start();
                    mPlaybackState = new PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                            .build();
                    mMediaSession.setPlaybackState(mPlaybackState);
                    updateNotification();
                    break;
            }
        }

        //下一曲
        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            if (last_index < list_data.size() - 1)
                last_index++;
            else
                last_index = 0;
            onPlayFromUri(null, null);

        }

        //上一曲
        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            if (last_index > 0)
                last_index--;
            else
                last_index = list_data.size() - 1;
            onPlayFromUri(null, null);
        }
    };

    /**
     * 重置player和seekbar进度
     */
    private void MediaPlayerReset() {
        seekBar.setProgress(0);
        mMediaPlayer.reset();
    }


    /**
     * 初始化各服务
     *
     * @param mContext
     */
    private void initService(Context mContext) {

        //传递播放的状态信息
        mPlaybackState = new PlaybackStateCompat.Builder().
                setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .build();

        //初始化MediaSessionCompant
        mMediaSession = new MediaSessionCompat(mContext, AudioPlayerService.SESSION_TAG);
        mMediaSession.setCallback(mMediaSessionCallback);//设置播放控制回调
        //设置可接受媒体控制
        mMediaSession.setActive(true);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setPlaybackState(mPlaybackState);//状态回调

        //初始化MediaPlayer
        mMediaPlayer = new MediaPlayer();
        // 设置音频流类型
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);

        // 初始化MediaController
        try {
            mMediaController = new MediaControllerCompat(mContext, mMediaSession.getSessionToken());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    //播放前的准备动作回调
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mMediaPlayer.start();
        mPlaybackState = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                .build();
        mMediaSession.setPlaybackState(mPlaybackState);
        updateNotification();
        if (playerUpdateListener != null)
            playerUpdateListener.onPrepared(mediaPlayer);

    }

    //缓冲更新
    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
        if (playerUpdateListener != null)
            playerUpdateListener.onBufferingUpdate(mediaPlayer, percent);
    }

    public void destoryService() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mMediaSession != null) {
            mMediaSession.release();
            mMediaSession = null;
        }
    }

    private NotificationCompat.Action createAction(int iconResId, String title, String action) {
        Intent intent = new Intent(mContext, AudioPlayerService.class);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getService(mContext, 1, intent, 0);
        return new NotificationCompat.Action(iconResId, title, pendingIntent);
    }

    /**
     * 更新通知栏
     */
    private void updateNotification() {
        NotificationCompat.Action playPauseAction = mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING ?
                createAction(R.drawable.img_pause, "Pause", AudioPlayerService.ACTION_PAUSE) :
                createAction(R.drawable.img_play, "Play", AudioPlayerService.ACTION_PLAY);

        NotificationCompat.Builder notificationCompat = new NotificationCompat.Builder(mContext)
                .setContentTitle(list_data.get(last_index).getMusicTitle())
                .setContentText(list_data.get(last_index).getSinger())
                .setOngoing(mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING)
                .setShowWhen(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(false)
                .addAction(createAction(R.drawable.img_last_one_music, "last", AudioPlayerService.ACTION_LAST))
                .addAction(playPauseAction)
                .addAction(createAction(R.drawable.img_next_music, "next", AudioPlayerService.ACTION_NEXT))
                .setStyle(new android.support.v7.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mMediaSession.getSessionToken())
                        .setShowActionsInCompactView(1, 2));
        Notification notification = notificationCompat.build();
        ((NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, notification);


        //播放新歌曲时需要更新seekbar的max与总秒数对应。保证每一秒seekbar会走动一格。回传到View层来更新时间
        this.seekBar.setMax(mMediaPlayer.getDuration() / 1000);

        updateSeekBar();
    }

    /**
     * seekbar每秒更新一格进度
     */
    private void updateSeekBar() {
        // 计时器每一秒更新一次进度条
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
//                mediaPlayer在播放并且seebar没有被按下
                if (mMediaPlayer != null && mMediaPlayer.isPlaying() && !seekBar.isPressed()) {
                    MediaPlayerHelper.this.handler.sendEmptyMessage(0); //发送消息
                }
            }
        };
        // 每一秒触发一次
        mTimer.schedule(timerTask, 0, 1000);
    }

    public MediaControllerCompat getmMediaController() {
        return mMediaController;
    }

    public MediaSessionCompat.Token getMediaSessionToken() {
        return mMediaSession.getSessionToken();
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public void setSeekBar(AppCompatSeekBar seekBar) {
        this.seekBar = seekBar;
    }

    public void setMediaPlayerUpdateListener(MediaPlayerUpdateCallBack listener) {
        this.playerUpdateListener = listener;
    }

    public void setPlayeData(List<MusicEntity> list_data) {
        this.list_data = list_data;
    }

    /**
     * 设置通知(mediasession歌曲)信息
     */
    private MediaMetadataCompat getMusicEntity(String musicName, String Singer, String album) {
        MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder().build();
        mediaMetadataCompat.getBundle().putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, Singer);//歌手
        mediaMetadataCompat.getBundle().putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album);//专辑
        mediaMetadataCompat.getBundle().putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicName);//title
        return mediaMetadataCompat;
    }


    public interface MediaPlayerUpdateCallBack {
        void onCompletion(MediaPlayer mediaPlayer);

        void onBufferingUpdate(MediaPlayer mediaPlayer, int percent);

        void onPrepared(MediaPlayer mediaPlayer);
    }

}
