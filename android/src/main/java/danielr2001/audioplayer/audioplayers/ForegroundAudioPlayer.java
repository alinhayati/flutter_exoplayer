package danielr2001.audioplayer.audioplayers;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.webkit.URLUtil;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import danielr2001.audioplayer.AudioPlayerPlugin;
import danielr2001.audioplayer.R;
import danielr2001.audioplayer.enums.NotificationDefaultActions;
import danielr2001.audioplayer.enums.PlayerMode;
import danielr2001.audioplayer.enums.PlayerState;
import danielr2001.audioplayer.interfaces.AudioPlayer;
import danielr2001.audioplayer.listeners.EventListenerCallback;
import danielr2001.audioplayer.listeners.ForegroundEventListener;
import danielr2001.audioplayer.listeners.ForegroundPlayerAnalyticsListener;
import danielr2001.audioplayer.models.AudioObject;
import danielr2001.audioplayer.notifications.DescriptionAdapter;

public class ForegroundAudioPlayer extends Service implements AudioPlayer {
    public static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "Playback";
    private static final String TAG = "ForegroundAudioPlayer";
    private final IBinder binder = new LocalBinder();
    PlayerNotificationManager playerNotificationManager;
    Activity activity;
    MediaSessionConnector mediaSessionConnector;
    private ForegroundAudioPlayer foregroundAudioPlayer;
    private Context context;
    private AudioPlayerPlugin ref;
    private MediaSessionCompat mediaSession;
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
    private List<String> fallbackUrlList;
    private int currentFallbackUrlIndex = -1;
    private int maxAttemptsPerUrl = DEFAULT_MAX_ATTEMPTS_PER_URL;
    private int attempts = 1;

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
            if (manager != null) manager.createNotificationChannel(serviceChannel);
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
        // This allows things like google assistant, android auto, tv, etc. work with the player
        // (e.g. 'Ok google, pause')
        mediaSessionConnector = new MediaSessionConnector(mediaSession);
        setNotificationBar();
        player = ExoPlayerFactory.newSimpleInstance(this.context, trackSelector, loadControl);
        player.setForegroundMode(true);
        DataSource.Factory offlineDataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this.context, "exoPlayerLibrary"));
        DataSource.Factory onlineDataSourceFactory =
                new InsightCacheDataSourceFactory(this.context, cache);
        // playlist/single audio load
        InsightLoadErrorPolicy insightCustomLoadErrorPolicy = new InsightLoadErrorPolicy();
        if (this.playerMode == PlayerMode.PLAYLIST) {
            ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();
            for (AudioObject audioObject : audioObjects) {
                String url = audioObject.getUrl();
                MediaSource mediaSource;
                if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
                    mediaSource = new ProgressiveMediaSource.Factory(onlineDataSourceFactory)
                            .setLoadErrorHandlingPolicy(insightCustomLoadErrorPolicy)
                            .createMediaSource(Uri.parse(url));
                } else {
                    mediaSource = new ProgressiveMediaSource.Factory(offlineDataSourceFactory)
                            .setLoadErrorHandlingPolicy(insightCustomLoadErrorPolicy)
                            .createMediaSource(Uri.parse(url));
                }
                concatenatingMediaSource.addMediaSource(mediaSource);
            }
            player.prepare(concatenatingMediaSource, true, false);
            if (index != 0) {
                player.seekTo(index, 0);
            }
        } else if (this.audioObject != null) {
            String url = this.audioObject.getUrl();
            MediaSource mediaSource;
            if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
                mediaSource = new ProgressiveMediaSource.Factory(onlineDataSourceFactory)
                        .setLoadErrorHandlingPolicy(insightCustomLoadErrorPolicy)
                        .createMediaSource(Uri.parse(url));
            } else {
                mediaSource = new ProgressiveMediaSource.Factory(offlineDataSourceFactory)
                        .setLoadErrorHandlingPolicy(insightCustomLoadErrorPolicy)
                        .createMediaSource(Uri.parse(url));
            }
            player.prepare(mediaSource, true, false);
        }

        playerNotificationManager.setPlayer(player);
        mediaSessionConnector.setPlayer(player);

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
    public void play(AudioObject audioObject, List<String> fallbackUrlList, int maxAttemptsPerUrl) {
        if (this.completed) {
            this.resume();
        } else {
            this.released = false;
            this.audioObject = audioObject;
            this.fallbackUrlList = fallbackUrlList;
            if (maxAttemptsPerUrl > 0) this.maxAttemptsPerUrl = maxAttemptsPerUrl;
            this.attempts = 1;
            Log.d(TAG, "play: fallbackUrlList=" + fallbackUrlList);
            this.currentFallbackUrlIndex = -1;
            initialiseAndPlay();
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
                initialiseAndPlay();
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
            this.fallbackUrlList = null;
            this.maxAttemptsPerUrl = DEFAULT_MAX_ATTEMPTS_PER_URL;
            this.attempts = 1;
            this.currentFallbackUrlIndex = -1;
            this.audioObjects = null;
            mediaSessionConnector.setPlayer(null);
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
        addAnalyticsListener();
        addEventListener();
    }

    private void addEventListener() {
        player.addListener(new ForegroundEventListener(new EventListenerCallback() {
            @Override
            public void doOnTracksChanged() {
                ref.handlePlayerIndex(foregroundAudioPlayer);
            }

            @Override
            public void doOnPlayerStatusChanged(boolean playWhenReady, int playbackState) {
                doOnPlayerStateChanged(playWhenReady, playbackState);
            }
        }));
    }

    private void doOnPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case Player.STATE_BUFFERING: {
                // buffering
                buffering = true;
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
                        ref.handleStateChange(foregroundAudioPlayer, PlayerState.PLAYING);
                        ref.handlePositionUpdates();
                    } else {
                        ref.handleStateChange(foregroundAudioPlayer, PlayerState.PAUSED);
                    }
                } else if (playWhenReady) {
                    // resumed
                    playing = true;
                    ref.handleStateChange(foregroundAudioPlayer, PlayerState.PLAYING);
                    ref.handlePositionUpdates();
                    playerNotificationManager.invalidate();
                } else if (!playWhenReady) {
                    // paused
                    playing = false;
                    ref.handleStateChange(foregroundAudioPlayer, PlayerState.PAUSED);
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


    private void initialiseAndPlay() {
        this.initExoPlayer(0);
        initEventListeners();
        player.setPlayWhenReady(true);
    }

    private void initialiseAndPlayAndSeek(long failTime) {
        this.initExoPlayer(0);
        initEventListeners();
        player.seekTo(failTime);
        player.setPlayWhenReady(true);
    }

    private void playFallbackAudioUrl(long failTime) {
        String fallbackUrl = getFallbackUrl();
        if (fallbackUrl == null) return;
        audioObject.setUrl(fallbackUrl);
        if (failTime == 0) {
            initialiseAndPlay();
        } else {
            initialiseAndPlayAndSeek(failTime);
        }
    }

    private String getFallbackUrl() {
        if (fallbackUrlList == null || fallbackUrlList.isEmpty() || currentFallbackUrlIndex++ > (fallbackUrlList.size() - 1))
            return null;
        return fallbackUrlList.get(currentFallbackUrlIndex);
    }

    private void addAnalyticsListener() {
        player.addAnalyticsListener(new ForegroundPlayerAnalyticsListener(new ForegroundPlayerAnalyticsListener.AnalyticsCallback() {
            @Override
            public void doOnLoadError(AnalyticsListener.EventTime eventTime,
                                      MediaSourceEventListener.LoadEventInfo loadEventInfo) {
                if (attempts <= maxAttemptsPerUrl) {
                    Log.d(TAG, "Attempt" + attempts + ": " + loadEventInfo.dataSpec.uri);
                    attempts++;
                    return;
                }
                attempts = 1;
                stop();
                playFallbackAudioUrl(eventTime.currentPlaybackPositionMs);
            }

            @Override
            public void doOnAudioSessionId(int audioSessionId) {
                ref.handleAudioSessionIdChange(foregroundAudioPlayer, audioSessionId);
            }
        }));
    }

    public void setNotificationBar() {
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                context, CHANNEL_ID, R.string.notification_channel_name, NOTIFICATION_ID,
                new DescriptionAdapter(audioObject, activity),
                new PlayerNotificationManager.NotificationListener() {
                    @Override
                    public void onNotificationCancelled(int notificationId,
                                                        boolean dismissedByUser) {
                        stopSelf();
                    }

                    @Override
                    public void onNotificationPosted(int notificationId,
                                                     Notification notification, boolean ongoing) {
                        startForeground(notificationId, notification);
                    }
                });

        playerNotificationManager.setPriority(NotificationCompat.PRIORITY_HIGH);
        playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());
        if (audioObject != null && audioObject.getSmallIconFileName() != null) {
            int icon =
                    this.context.getResources().getIdentifier(audioObject.getSmallIconFileName(),
                            "drawable",
                    this.context.getPackageName());
            playerNotificationManager.setSmallIcon(icon);
        }
        playerNotificationManager.setColorized(true);
        playerNotificationManager.setUseStopAction(false);
        playerNotificationManager.setUseNavigationActions(false);
        playerNotificationManager.setUseNavigationActionsInCompactView(true);
        playerNotificationManager.setFastForwardIncrementMs(0);
        playerNotificationManager.setRewindIncrementMs(0);

        if (audioObject != null && audioObject.getNotificationActionMode() != null) {
            if (audioObject.getNotificationActionMode() == NotificationDefaultActions.FORWARD ||
                    audioObject.getNotificationActionMode() == NotificationDefaultActions.BACKWARD ||
                    audioObject.getNotificationActionMode() == NotificationDefaultActions.ALL) {
                playerNotificationManager.setFastForwardIncrementMs(15000);
                playerNotificationManager.setRewindIncrementMs(15000);
            } else {
                mediaSessionConnector.setQueueNavigator(new TimelineQueueNavigator(mediaSession) {
                    @Override
                    public MediaDescriptionCompat getMediaDescription(Player player,
                                                                      int windowIndex) {
                        Bundle extras = new Bundle();
                        extras.putInt(MediaMetadataCompat.METADATA_KEY_DURATION, -1);

                        return new MediaDescriptionCompat.Builder()
                                .setMediaId(CHANNEL_ID)
                                .setExtras(extras)
                                .build();
                    }
                });
            }
        }
    }

    public class LocalBinder extends Binder {
        public ForegroundAudioPlayer getService() {
            return ForegroundAudioPlayer.this;
        }
    }
}