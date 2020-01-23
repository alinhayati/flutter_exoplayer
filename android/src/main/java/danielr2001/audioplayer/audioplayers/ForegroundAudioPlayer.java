package danielr2001.audioplayer.audioplayers;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.Surface;
import android.webkit.URLUtil;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import danielr2001.audioplayer.AudioPlayerPlugin;
import danielr2001.audioplayer.R;

import danielr2001.audioplayer.enums.NotificationDefaultActions;
import danielr2001.audioplayer.enums.PlayerMode;
import danielr2001.audioplayer.enums.PlayerState;
import danielr2001.audioplayer.interfaces.AsyncResponse;
import danielr2001.audioplayer.interfaces.AudioPlayer;
import danielr2001.audioplayer.models.AudioObject;
import danielr2001.audioplayer.notifications.DescriptionAdapter;
import danielr2001.audioplayer.notifications.LoadImageFromUrl;

public class ForegroundAudioPlayer extends Service implements AudioPlayer {

    private final IBinder binder = new LocalBinder();
    private ForegroundAudioPlayer foregroundAudioPlayer;
    private Context context;
    private AudioPlayerPlugin ref;
    private MediaSessionCompat mediaSession;
    private MediaSessionConnector mediaSessionConnector;
    private String playerId;
    //player attributes
    private float volume = 1;
    private boolean repeatMode;
    private boolean respectAudioFocus;
    private PlayerMode playerMode;
    //player states
    private boolean initialized = false;
    private boolean buffering = false;
    private boolean playing = false;
    private boolean stopped = false;
    private boolean released = true;
    private boolean completed = false;
    //ExoPlayer
    private SimpleExoPlayer player;
    private ArrayList<AudioObject> audioObjects;
    private AudioObject audioObject;
    private Cache cache;
    PlayerNotificationManager playerNotificationManager;
    Activity activity;
    public static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "Playback";
    private Bitmap albumArt;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.context = getApplicationContext();
        createTempNotificationWhileInitializingPlayer();
        mediaSession = new MediaSessionCompat(this.context, "playback");
        mediaSessionConnector = new MediaSessionConnector(mediaSession);
        mediaSessionConnector.setQueueNavigator(new TimelineQueueNavigator(mediaSession) {
            @Override
            public MediaDescriptionCompat getMediaDescription(Player player, int windowIndex) {
                return updateMediaSessionMetaData();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
            builder.putLong(MediaMetadata.METADATA_KEY_DURATION, -1);
            mediaSession.setMetadata(builder.build());
        }

        // ! TODO handle MediaButtonReceiver's callbacks
        // MediaButtonReceiver.handleIntent(mediaSession, intent);
        // mediaSession.setCallback(mediaSessionCallback);
        if (intent != null && intent.getAction() != null) {
            return START_STICKY;
        } else {
            return START_REDELIVER_INTENT;
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        this.release();
    }

    private void createTempNotificationWhileInitializingPlayer() {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, this.getClass());
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Initializing Audio Player")
                .setContentIntent(pendingIntent)
                .setSound(null)
                .setColorized(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setVibrate(null)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setSound(null, null);
            serviceChannel.enableLights(false);
            serviceChannel.enableVibration(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void initAudioPlayer(AudioPlayerPlugin ref, Activity activity, String playerId) {
        this.activity = activity;
        this.initialized = true;

        this.playerId = playerId;
        this.ref = ref;
        this.foregroundAudioPlayer = this;
    }

    @Override
    public void initExoPlayer(int index) {
        DefaultTrackSelector trackSelector = new DefaultTrackSelector();
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setAllocator(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
                .setBufferDurationsMs(
                        InsightExoPlayerConstants.DEFAULT_MIN_BUFFER_MS,
                        InsightExoPlayerConstants.DEFAULT_MAX_BUFFER_MS,
                        InsightExoPlayerConstants.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                        InsightExoPlayerConstants.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                ).createDefaultLoadControl();
        if (cache == null) {
            cache = new SimpleCache(
                    new File(context.getCacheDir(), "media"),
                    new LeastRecentlyUsedCacheEvictor(InsightExoPlayerConstants.DEFAULT_MEDIA_CACHE_SIZE),
                    new ExoDatabaseProvider(context));
        }
        setNotificationBar();
        player = ExoPlayerFactory.newSimpleInstance(this.context, trackSelector, loadControl);
        player.setForegroundMode(true);
        DataSource.Factory offlineDataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this.context, "exoPlayerLibrary"));
        DataSource.Factory onlineDataSourceFactory = new InsightCacheDataSourceFactory(this.context, cache);
        // playlist/single audio load
        InsightLoadErrorPolicy insightLoadErrorPolicy = new InsightLoadErrorPolicy();
        if (this.playerMode == PlayerMode.PLAYLIST) {
            ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();
            for (AudioObject audioObject : audioObjects) {
                String url = audioObject.getUrl();
                MediaSource mediaSource;
                if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
                    mediaSource = new ProgressiveMediaSource.Factory(onlineDataSourceFactory)
                            .setLoadErrorHandlingPolicy(insightLoadErrorPolicy)
                            .createMediaSource(Uri.parse(url));
                } else {
                    mediaSource = new ProgressiveMediaSource.Factory(offlineDataSourceFactory)
                            .setLoadErrorHandlingPolicy(insightLoadErrorPolicy)
                            .createMediaSource(Uri.parse(url));
                }
                concatenatingMediaSource.addMediaSource(mediaSource);
            }
            player.prepare(concatenatingMediaSource, true, false);
            if (index != 0) {
                player.seekTo(index, 0);
            }
        } else if (this.audioObject != null) {
            if (audioObject.getLargeIconUrl() != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    loadImageFromUrl(audioObject.getLargeIconUrl(), audioObject.getIsLocal());
                }
            }
            String url = this.audioObject.getUrl();
            MediaSource mediaSource;
            if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
                mediaSource = new ProgressiveMediaSource.Factory(onlineDataSourceFactory)
                        .setLoadErrorHandlingPolicy(insightLoadErrorPolicy)
                        .createMediaSource(Uri.parse(url));
            } else {
                mediaSource = new ProgressiveMediaSource.Factory(offlineDataSourceFactory)
                        .setLoadErrorHandlingPolicy(insightLoadErrorPolicy)
                        .createMediaSource(Uri.parse(url));
            }
            player.prepare(mediaSource, true, false);
        }


        mediaSessionConnector.setPlayer(player);
        playerNotificationManager.setPlayer(player);

        // handle audio focus
        if (this.respectAudioFocus) { // ! TODO catch duck pause!
            AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MUSIC).build();
            player.setAudioAttributes(audioAttributes, true);
        }
        // set repeat mode
        if (repeatMode) {
            player.setRepeatMode(player.REPEAT_MODE_ALL);
        }
    }

    @Override
    public void play(AudioObject audioObject) {
        if (this.completed) {
            this.resume();
        } else {
            this.released = false;

            this.audioObject = audioObject;
            this.initExoPlayer(0);
            initEventListeners();
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void playAll(ArrayList<AudioObject> audioObjects, int index) {
        if (this.completed || this.stopped) {
            this.resume();
        } else {
            this.released = false;

            this.audioObjects = audioObjects;
            this.initExoPlayer(index);
            initEventListeners();
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void next() {
        if (!this.released) {
            player.next();
            resume();
        }
    }

    @Override
    public void previous() {
        if (!this.released) {
            player.previous();
            resume();
        }
    }

    @Override
    public void pause() {
        if (!this.released && this.playing) {
            stopForeground(false);
            player.setPlayWhenReady(false);
        }
    }

    @Override
    public void resume() {
        if (!this.released && !this.playing) {
            if (!this.stopped) {
                this.completed = false;
                player.setPlayWhenReady(true);
            } else {
                this.stopped = false;
                this.initExoPlayer(0);
                initEventListeners();
                player.setPlayWhenReady(true);
            }
        }
    }

    @Override
    public void stop() {
        if (!this.released) {
            stopForeground(true);
            player.stop(true);
        }
    }

    @Override
    public void release() {
        if (!this.released) {
            if (this.playing) {
                stopForeground(true);
            }
            this.initialized = false;
            this.buffering = false;
            this.playing = false;
            this.stopped = false;
            this.released = true;
            this.completed = false;
            this.cache.release();
            this.cache = null;
            this.audioObject = null;
            this.audioObjects = null;
            player.release();
            player = null;
            ref.handleStateChange(this, PlayerState.RELEASED);
            stopSelf();
        }
    }

    @Override
    public void seekPosition(int position) {
        if (!this.released) {
            player.seekTo(player.getCurrentWindowIndex(), position);
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void seekIndex(int index) {
        if (!this.released) {
            player.seekTo(index, 0);
        }
    }

    @Override
    public boolean isPlaying() {
        return this.playing;
    }

    @Override
    public boolean isBackground() {
        return false;
    }

    @Override
    public boolean isPlayerInitialized() {
        return this.initialized;
    }

    @Override
    public boolean isPlayerReleased() {
        return this.released;
    }

    @Override
    public boolean isPlayerCompleted() {
        return this.completed;
    }

    @Override
    public String getPlayerId() {
        return this.playerId;
    }

    @Override
    public long getDuration() {
        if (!this.released) {
            return player.getDuration();
        } else {
            return -1;
        }
    }

    @Override
    public long getCurrentPosition() {
        if (!this.released) {
            return player.getCurrentPosition();
        } else {
            return -1;
        }
    }

    @Override
    public int getCurrentPlayingAudioIndex() {
        return player.getCurrentWindowIndex();
    }

    @Override
    public float getVolume() {
        return player.getVolume();
    }

    @Override
    public void setVolume(float volume) {
        if (!this.released && this.volume != volume) {
            this.volume = volume;
            player.setVolume(volume);
        }
    }

    @Override
    public void setPlayerAttributes(boolean repeatMode, boolean respectAudioFocus,
                                    PlayerMode playerMode) {
        this.repeatMode = repeatMode;
        this.respectAudioFocus = respectAudioFocus;
        this.playerMode = playerMode;
    }

    @Override
    public void setRepeatMode(boolean repeatMode) {
        if (!this.released && this.repeatMode != repeatMode) {
            this.repeatMode = repeatMode;
            if (this.repeatMode) {
                player.setRepeatMode(player.REPEAT_MODE_ALL);
            } else {
                player.setRepeatMode(player.REPEAT_MODE_OFF);
            }
        }
    }

    private void initEventListeners() {
        addCustomListeners();
        player.addListener(new Player.EventListener() {

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups,
                                        TrackSelectionArray trackSelections) {
                ref.handlePlayerIndex(foregroundAudioPlayer);
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING: {
                        // buffering
                        buffering = true;
                        playerNotificationManager.setUseChronometer(false);
                        playerNotificationManager.invalidate();
                        ref.handleStateChange(foregroundAudioPlayer, PlayerState.BUFFERING);
                        break;
                    }
                    case Player.STATE_READY: {
                        if (completed) {
                            buffering = false;
                            playerNotificationManager.setUseChronometer(false);
                            playerNotificationManager.invalidate();
                            ref.handleStateChange(foregroundAudioPlayer, PlayerState.COMPLETED);
                        } else if (buffering) {
                            // playing
                            buffering = false;
                            if (playWhenReady) {
                                playing = true;
                                playerNotificationManager.setUseChronometer(true);
                                playerNotificationManager.invalidate();
                                ref.handleStateChange(foregroundAudioPlayer, PlayerState.PLAYING);
                                ref.handlePositionUpdates();
                            } else {
                                playerNotificationManager.setUseChronometer(false);
                                playerNotificationManager.invalidate();
                                ref.handleStateChange(foregroundAudioPlayer, PlayerState.PAUSED);
                            }
                        } else if (playWhenReady) {
                            // resumed
                            playing = true;
                            ref.handlePositionUpdates();
                            ref.handleStateChange(foregroundAudioPlayer, PlayerState.PLAYING);
                            playerNotificationManager.setUseChronometer(true);
                            playerNotificationManager.invalidate();
                        } else if (!playWhenReady) {
                            // paused
                            playing = false;
                            ref.handleStateChange(foregroundAudioPlayer, PlayerState.PAUSED);
                            playerNotificationManager.setUseChronometer(false);
                            playerNotificationManager.invalidate();
                        }

                        break;
                    }
                    case Player.STATE_ENDED: {
                        // completed
                        playing = false;
                        completed = true;

                        stopForeground(false);
                        player.setPlayWhenReady(false);
                        player.seekTo(0, 0);
                        break;
                    }
                    case Player.STATE_IDLE: {
                        // unable to fetch data packet from URL
                        buffering = true;
                        playing = false;
                        stopped = false;
                        completed = false;
                        playerNotificationManager.setUseChronometer(false);
                        ref.handleStateChange(foregroundAudioPlayer, PlayerState.BUFFERING);

                        break;
                    } // handle of released is in release method!
                }
            }
        });
    }

    private void addCustomListeners() {
        player.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady,
                                             int playbackState) {
                Log.d("BackgroundAudioPlayer",
                        "onPlayerStateChanged:\neventTime=" + eventTime.currentPlaybackPositionMs + "\nplayWhenReady=" + playWhenReady + " \nplaybackState=" + playbackState);
            }

            @Override
            public void onTimelineChanged(EventTime eventTime, int reason) {
                Log.d("BackgroundAudioPlayer",
                        "onTimelineChanged:\neventTime=" + eventTime.currentPlaybackPositionMs +
                                "\nreason=" + reason);
            }

            @Override
            public void onPositionDiscontinuity(EventTime eventTime, int reason) {
                Log.d("BackgroundAudioPlayer",
                        "onPositionDiscontinuity:\neventTime=" + eventTime.currentPlaybackPositionMs + "\nreason=" + reason);
            }

            @Override
            public void onSeekStarted(EventTime eventTime) {
                Log.d("BackgroundAudioPlayer",
                        "onSeekStarted:\neventTime=" + eventTime.currentPlaybackPositionMs);
            }

            @Override
            public void onSeekProcessed(EventTime eventTime) {
                Log.d("BackgroundAudioPlayer",
                        "onSeekProcessed:\neventTime=" + eventTime.currentPlaybackPositionMs);
            }

            @Override
            public void onPlaybackParametersChanged(EventTime eventTime,
                                                    PlaybackParameters playbackParameters) {
                Log.d("BackgroundAudioPlayer",
                        "onPlaybackParametersChanged:\neventTime=" + eventTime.currentPlaybackPositionMs + " PlaybackParameters:" + "\n    pitch=" + playbackParameters.pitch + "\n    skipSilence=" + playbackParameters.skipSilence + "\n     speed=" + playbackParameters.speed);
            }

            @Override
            public void onRepeatModeChanged(EventTime eventTime, int repeatMode) {
                Log.d("BackgroundAudioPlayer",
                        "onRepeatModeChanged:\neventTime=" + eventTime.currentPlaybackPositionMs + "\nrepeatMode=" + repeatMode);
            }

            @Override
            public void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) {
                Log.d("BackgroundAudioPlayer",
                        "onShuffleModeChanged:\neventTime=" + eventTime.currentPlaybackPositionMs + "\nrepeatMode=" + shuffleModeEnabled);
            }

            @Override
            public void onLoadingChanged(EventTime eventTime, boolean isLoading) {
                Log.d("BackgroundAudioPlayer",
                        "onLoadingChanged:\neventTime=" + eventTime.currentPlaybackPositionMs +
                                "\nrepeatMode=" + isLoading);
            }

            @Override
            public void onPlayerError(EventTime eventTime, ExoPlaybackException error) {
                Log.d("BackgroundAudioPlayer",
                        "onPlayerError:\neventTime=" + eventTime.currentPlaybackPositionMs +
                                "\nerror=" + error.getMessage());
            }

            @Override
            public void onTracksChanged(EventTime eventTime, TrackGroupArray trackGroups,
                                        TrackSelectionArray trackSelections) {
                Log.d("BackgroundAudioPlayer", "onTracksChanged");
            }

            @Override
            public void onLoadStarted(EventTime eventTime,
                                      MediaSourceEventListener.LoadEventInfo loadEventInfo,
                                      MediaSourceEventListener.MediaLoadData mediaLoadData) {
                Log.d("BackgroundAudioPlayer",
                        "onLoadStarted:\neventTime=" + eventTime.currentPlaybackPositionMs +
                                "\nloadEventInfo:" + "\n  bytes loaded:" + loadEventInfo.bytesLoaded + "\n    dataSpec:" + "\n       key=" + loadEventInfo.dataSpec.key + "\n       absoluteStreamPosition=" + loadEventInfo.dataSpec.absoluteStreamPosition + "\n       flags=" + loadEventInfo.dataSpec.flags + "\n       uri=" + loadEventInfo.dataSpec.uri + "\n       httpMethodString=" + loadEventInfo.dataSpec.getHttpMethodString() + "\n    elapsedRealtimeMs:" + loadEventInfo.elapsedRealtimeMs + "\n    loadDurationMs:" + loadEventInfo.loadDurationMs + "\n    entrySet:" + loadEventInfo.responseHeaders.entrySet() + "\nMediaLoadData:" + "\n    dataType=" + mediaLoadData.dataType + "\n    mediaEndTimeMs=" + mediaLoadData.mediaEndTimeMs + "\n    mediaStartTimeMs=" + mediaLoadData.mediaStartTimeMs + "\n    trackSelectionReason=" + mediaLoadData.trackSelectionReason + "\n    trackType=" + mediaLoadData.trackType);
            }

            @Override
            public void onLoadCompleted(EventTime eventTime,
                                        MediaSourceEventListener.LoadEventInfo loadEventInfo,
                                        MediaSourceEventListener.MediaLoadData mediaLoadData) {
                Log.d("BackgroundAudioPlayer",
                        "onLoadCompleted:\neventTime=" + eventTime.currentPlaybackPositionMs +
                                "\nloadEventInfo:" + "\n  bytes loaded:" + loadEventInfo.bytesLoaded + "\n    dataSpec:" + "\n       key=" + loadEventInfo.dataSpec.key + "\n       absoluteStreamPosition=" + loadEventInfo.dataSpec.absoluteStreamPosition + "\n       flags=" + loadEventInfo.dataSpec.flags + "\n       uri=" + loadEventInfo.dataSpec.uri + "\n       httpMethodString=" + loadEventInfo.dataSpec.getHttpMethodString() + "\n    elapsedRealtimeMs:" + loadEventInfo.elapsedRealtimeMs + "\n    loadDurationMs:" + loadEventInfo.loadDurationMs + "\n    entrySet:" + loadEventInfo.responseHeaders.entrySet() + "\nMediaLoadData:" + "\n    dataType=" + mediaLoadData.dataType + "\n    mediaEndTimeMs=" + mediaLoadData.mediaEndTimeMs + "\n    mediaStartTimeMs=" + mediaLoadData.mediaStartTimeMs + "\n    trackSelectionReason=" + mediaLoadData.trackSelectionReason + "\n    trackType=" + mediaLoadData.trackType);
            }

            @Override
            public void onLoadCanceled(EventTime eventTime,
                                       MediaSourceEventListener.LoadEventInfo loadEventInfo,
                                       MediaSourceEventListener.MediaLoadData mediaLoadData) {
                Log.d("BackgroundAudioPlayer",
                        "onLoadCanceled:\neventTime=" + eventTime.currentPlaybackPositionMs +
                                "\nloadEventInfo:" + "\n  bytes loaded:" + loadEventInfo.bytesLoaded + "\n    dataSpec:" + "\n       key=" + loadEventInfo.dataSpec.key + "\n       absoluteStreamPosition=" + loadEventInfo.dataSpec.absoluteStreamPosition + "\n       flags=" + loadEventInfo.dataSpec.flags + "\n       uri=" + loadEventInfo.dataSpec.uri + "\n       httpMethodString=" + loadEventInfo.dataSpec.getHttpMethodString() + "\n    elapsedRealtimeMs:" + loadEventInfo.elapsedRealtimeMs + "\n    loadDurationMs:" + loadEventInfo.loadDurationMs + "\n    entrySet:" + loadEventInfo.responseHeaders.entrySet() + "\nMediaLoadData:" + "\n    dataType=" + mediaLoadData.dataType + "\n    mediaEndTimeMs=" + mediaLoadData.mediaEndTimeMs + "\n    mediaStartTimeMs=" + mediaLoadData.mediaStartTimeMs + "\n    trackSelectionReason=" + mediaLoadData.trackSelectionReason + "\n    trackType=" + mediaLoadData.trackType);
            }

            @Override
            public void onLoadError(EventTime eventTime,
                                    MediaSourceEventListener.LoadEventInfo loadEventInfo,
                                    MediaSourceEventListener.MediaLoadData mediaLoadData,
                                    IOException error, boolean wasCanceled) {
                Log.d("BackgroundAudioPlayer",
                        "onLoadError:\neventTime=" + eventTime.currentPlaybackPositionMs +
                                "\nloadEventInfo:" + "\n  bytes loaded:" + loadEventInfo.bytesLoaded + "\n    dataSpec:" + "\n       key=" + loadEventInfo.dataSpec.key + "\n       absoluteStreamPosition=" + loadEventInfo.dataSpec.absoluteStreamPosition + "\n       flags=" + loadEventInfo.dataSpec.flags + "\n       uri=" + loadEventInfo.dataSpec.uri + "\n       httpMethodString=" + loadEventInfo.dataSpec.getHttpMethodString() + "\n    elapsedRealtimeMs:" + loadEventInfo.elapsedRealtimeMs + "\n    loadDurationMs:" + loadEventInfo.loadDurationMs + "\n    entrySet:" + loadEventInfo.responseHeaders.entrySet() + "\nMediaLoadData:" + "\n    dataType=" + mediaLoadData.dataType + "\n    mediaEndTimeMs=" + mediaLoadData.mediaEndTimeMs + "\n    mediaStartTimeMs=" + mediaLoadData.mediaStartTimeMs + "\n    trackSelectionReason=" + mediaLoadData.trackSelectionReason + "\n    trackType=" + mediaLoadData.trackType + "\n   error=" + error.getMessage() + "\n  wasCanceled=" + wasCanceled);
            }

            @Override
            public void onDownstreamFormatChanged(EventTime eventTime,
                                                  MediaSourceEventListener.MediaLoadData mediaLoadData) {
                Log.d("BackgroundAudioPlayer",
                        "onDownstreamFormatChanged:\neventTime=" + eventTime.currentPlaybackPositionMs + "\nMediaLoadData:" + "\n    dataType=" + mediaLoadData.dataType + "\n    mediaEndTimeMs=" + mediaLoadData.mediaEndTimeMs + "\n    mediaStartTimeMs=" + mediaLoadData.mediaStartTimeMs + "\n    trackSelectionReason=" + mediaLoadData.trackSelectionReason + "\n    trackType=" + mediaLoadData.trackType);
            }

            @Override
            public void onUpstreamDiscarded(EventTime eventTime,
                                            MediaSourceEventListener.MediaLoadData mediaLoadData) {
                Log.d("BackgroundAudioPlayer",
                        "onUpstreamDiscarded:\neventTime=" + eventTime.currentPlaybackPositionMs + "\nMediaLoadData:" + "\n    dataType=" + mediaLoadData.dataType + "\n    mediaEndTimeMs=" + mediaLoadData.mediaEndTimeMs + "\n    mediaStartTimeMs=" + mediaLoadData.mediaStartTimeMs + "\n    trackSelectionReason=" + mediaLoadData.trackSelectionReason + "\n    trackType=" + mediaLoadData.trackType);
            }

            @Override
            public void onMediaPeriodCreated(EventTime eventTime) {
                Log.d("BackgroundAudioPlayer",
                        "onMediaPeriodCreated:\neventTime=" + eventTime.currentPlaybackPositionMs);
            }

            @Override
            public void onMediaPeriodReleased(EventTime eventTime) {
                Log.d("BackgroundAudioPlayer",
                        "onMediaPeriodReleased:\neventTime=" + eventTime.currentPlaybackPositionMs);
            }

            @Override
            public void onReadingStarted(EventTime eventTime) {
                Log.d("BackgroundAudioPlayer",
                        "onReadingStarted:\neventTime=" + eventTime.currentPlaybackPositionMs);
            }

            @Override
            public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs,
                                            long totalBytesLoaded, long bitrateEstimate) {
                Log.d("BackgroundAudioPlayer",
                        "onBandwidthEstimate:\neventTime=" + eventTime.currentPlaybackPositionMs + "\ntotalLoadTimeMs=" + totalLoadTimeMs + "\ntotalBytesLoaded=" + totalBytesLoaded + "\nbitrateEstimate" + bitrateEstimate);
            }

            @Override
            public void onSurfaceSizeChanged(EventTime eventTime, int width, int height) {
                Log.d("BackgroundAudioPlayer",
                        "onSurfaceSizeChanged:\neventTime=" + eventTime.currentPlaybackPositionMs);
            }

            @Override
            public void onMetadata(EventTime eventTime, Metadata metadata) {
                Log.d("BackgroundAudioPlayer",
                        "onMetadata:\neventTime=" + eventTime.currentPlaybackPositionMs);
            }

            @Override
            public void onDecoderEnabled(EventTime eventTime, int trackType,
                                         DecoderCounters decoderCounters) {
                Log.d("BackgroundAudioPlayer",
                        "onDecoderEnabled:\neventTime=" + eventTime.currentPlaybackPositionMs);
            }

            @Override
            public void onDecoderInitialized(EventTime eventTime, int trackType,
                                             String decoderName, long initializationDurationMs) {
                Log.d("BackgroundAudioPlayer",
                        "onDecoderInitialized:\neventTime=" + eventTime.currentPlaybackPositionMs);
            }

            @Override
            public void onDecoderInputFormatChanged(EventTime eventTime, int trackType,
                                                    Format format) {
                Log.d("BackgroundAudioPlayer",
                        "onDecoderInputFormatChanged:\neventTime=" + eventTime.currentPlaybackPositionMs);
            }

            @Override
            public void onDecoderDisabled(EventTime eventTime, int trackType,
                                          DecoderCounters decoderCounters) {
                Log.d("BackgroundAudioPlayer",
                        "onDecoderDisabled:\neventTime=" + eventTime.currentPlaybackPositionMs);
            }

            @Override
            public void onAudioSessionId(EventTime eventTime, int audioSessionId) {
                ref.handleAudioSessionIdChange(foregroundAudioPlayer, audioSessionId);
                Log.d("BackgroundAudioPlayer",
                        "onAudioSessionId:\neventTime=" + eventTime.currentPlaybackPositionMs +
                                "\naudioSessionId=" + audioSessionId);

            }

            @Override
            public void onAudioAttributesChanged(EventTime eventTime,
                                                 AudioAttributes audioAttributes) {
                Log.d("BackgroundAudioPlayer",
                        "onAudioAttributesChanged:\neventTime=" + eventTime.currentPlaybackPositionMs + "\naudioAttributes:" + "\n     contentType=" + audioAttributes.contentType);
            }

            @Override
            public void onVolumeChanged(EventTime eventTime, float volume) {

            }

            @Override
            public void onAudioUnderrun(EventTime eventTime, int bufferSize, long bufferSizeMs,
                                        long elapsedSinceLastFeedMs) {

            }

            @Override
            public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames,
                                             long elapsedMs) {

            }

            @Override
            public void onVideoSizeChanged(EventTime eventTime, int width, int height,
                                           int unappliedRotationDegrees,
                                           float pixelWidthHeightRatio) {

            }

            @Override
            public void onRenderedFirstFrame(EventTime eventTime, @Nullable Surface surface) {
                Log.d("BackgroundAudioPlayer",
                        "onRenderedFirstFrame:\neventTime=" + eventTime.currentPlaybackPositionMs);
            }
        });
    }

    public class LocalBinder extends Binder {
        public ForegroundAudioPlayer getService() {
            return ForegroundAudioPlayer.this;
        }
    }

    public void setNotificationBar() {
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                context, CHANNEL_ID, R.string.notification_channel_name, NOTIFICATION_ID, new DescriptionAdapter(audioObject, activity), new PlayerNotificationManager.NotificationListener() {
                    @Override
                    public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
                        stopSelf();
                    }

                    @Override
                    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
                        startForeground(notificationId, notification);
                    }
                });

        playerNotificationManager.setPriority(NotificationCompat.PRIORITY_HIGH);
        playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());
        int icon = this.context.getResources().getIdentifier(audioObject.getSmallIconFileName(), "drawable",
                this.context.getPackageName());
        playerNotificationManager.setSmallIcon(icon);
        playerNotificationManager.setColorized(true);
        playerNotificationManager.setUseStopAction(false);
        playerNotificationManager.setUseNavigationActions(false);
        playerNotificationManager.setUseNavigationActionsInCompactView(true);
        playerNotificationManager.setFastForwardIncrementMs(0);
        playerNotificationManager.setRewindIncrementMs(0);

        if (audioObject.getNotificationActionMode() == NotificationDefaultActions.FORWARD ||
                audioObject.getNotificationActionMode() == NotificationDefaultActions.BACKWARD ||
                audioObject.getNotificationActionMode() == NotificationDefaultActions.ALL) {
            playerNotificationManager.setFastForwardIncrementMs(15000);
            playerNotificationManager.setRewindIncrementMs(15000);
        }
    }

    private MediaDescriptionCompat updateMediaSessionMetaData() {
        MediaMetadataCompat.Builder metaDataBuilder = new MediaMetadataCompat.Builder();
        metaDataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt);
        metaDataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, albumArt);
        mediaSession.setMetadata(metaDataBuilder.build());
        mediaSessionConnector.invalidateMediaSessionMetadata();
        return metaDataBuilder.build().getDescription();
    }

    private Bitmap loadImageFromUrl(String imageUrl, boolean isLocal) {
        try {
            new LoadImageFromUrl(imageUrl, isLocal, new AsyncResponse() {
                @Override
                public void processFinish(Map<String, Bitmap> bitmapMap) {
                    if (bitmapMap != null) {
                        if (bitmapMap.get(audioObject.getLargeIconUrl()) != null) {
                            audioObject.setLargeIcon(bitmapMap.get(audioObject.getLargeIconUrl()));
                            albumArt = bitmapMap.get(audioObject.getLargeIconUrl());
                            mediaSessionConnector.invalidateMediaSessionMetadata();
                        } else {
                            Log.e("ExoPlayerPlugin", "canceled showing notification!");
                        }
                    } else {
                        Log.e("ExoPlayerPlugin", "Failed loading image!");
                    }
                }
            }).execute();
        } catch (Exception e) {
            Log.e("ExoPlayerPlugin", "Failed loading image!");
        }
        return albumArt;
    }

}