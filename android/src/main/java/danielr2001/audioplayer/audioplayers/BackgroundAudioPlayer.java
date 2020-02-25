package danielr2001.audioplayer.audioplayers;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.URLUtil;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
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
import danielr2001.audioplayer.enums.PlayerMode;
import danielr2001.audioplayer.enums.PlayerState;
import danielr2001.audioplayer.interfaces.AudioPlayer;
import danielr2001.audioplayer.listeners.BackgroundEventListener;
import danielr2001.audioplayer.listeners.BackgroundPlayerAnalyticsListener;
import danielr2001.audioplayer.listeners.EventListenerCallback;
import danielr2001.audioplayer.models.AudioObject;

public class BackgroundAudioPlayer implements AudioPlayer {
    private static final String TAG = "BackgroundAudioPlayer";
    private Context context;
    private AudioPlayerPlugin ref;
    private BackgroundAudioPlayer backgroundAudioPlayer;
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

    @Override
    public void initAudioPlayer(AudioPlayerPlugin ref, Activity activity, String playerId) {
        this.initialized = true;

        this.ref = ref;
        this.context = activity.getApplicationContext();
        this.playerId = playerId;
        this.backgroundAudioPlayer = this;
    }

    @Override
    public void initExoPlayer(int index) {
        DefaultTrackSelector trackSelector =
                new DefaultTrackSelector();
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setAllocator(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
                .setBufferDurationsMs(
                        InsightExoPlayerConstants.DEFAULT_MIN_BUFFER_MS,
                        InsightExoPlayerConstants.DEFAULT_MAX_BUFFER_MS,
                        InsightExoPlayerConstants.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                        InsightExoPlayerConstants.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                ).createDefaultLoadControl();
        player = ExoPlayerFactory.newSimpleInstance(this.context, trackSelector, loadControl);
        if (cache == null) {
            cache = new SimpleCache(
                    new File(context.getCacheDir(), "media"),
                    new LeastRecentlyUsedCacheEvictor(InsightExoPlayerConstants.DEFAULT_MEDIA_CACHE_SIZE),
                    new ExoDatabaseProvider(context));
        }
        DataSource.Factory offlineDataSourceFactory = new DefaultDataSourceFactory(this.context,
                Util.getUserAgent(this.context, "exoPlayerLibrary"));
        DataSource.Factory onlineDataSourceFactory =
                new InsightCacheDataSourceFactory(this.context, cache);
        // playlist/single audio load
        InsightLoadErrorPolicy insightLoadErrorPolicy = new InsightLoadErrorPolicy();
        if (playerMode == PlayerMode.PLAYLIST) {
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
            player.prepare(concatenatingMediaSource);
            if (index != 0) {
                player.seekTo(index, 0);
            }
        } else if (this.audioObject != null) {
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
        //handle audio focus
        if (this.respectAudioFocus) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MUSIC)
                    .build();
            player.setAudioAttributes(audioAttributes, true);
        }
        //set repeat mode
        if (repeatMode) {
            player.setRepeatMode(player.REPEAT_MODE_ALL);
        }
    }

    @Override
    public void play(AudioObject audioObject, List<String> fallbackUrlList, int maxAttemptsPerUrl) {
        if (this.completed || this.stopped) {
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
            player.stop(true);
        }
    }

    @Override
    public void release() {
        if (!this.released) {
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
            player.release();
            player = null;
            ref.handleStateChange(this, PlayerState.RELEASED);
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
        if (!this.released && playerMode == PlayerMode.PLAYLIST) {
            player.seekTo(index, 0);
        }
    }

    @Override
    public boolean isPlaying() {
        return this.playing;
    }

    @Override
    public boolean isBackground() {
        return true;
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

    private void doOnPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case Player.STATE_BUFFERING: {
                // buffering
                buffering = true;
                ref.handleStateChange(backgroundAudioPlayer, PlayerState.BUFFERING);
                break;
            }
            case Player.STATE_READY: {
                if (completed) {
                    buffering = false;
                    ref.handleStateChange(backgroundAudioPlayer, PlayerState.COMPLETED);
                } else if (buffering) {
                    // playing
                    buffering = false;
                    if (playWhenReady) {
                        playing = true;
                        ref.handlePositionUpdates();
                        ref.handleStateChange(backgroundAudioPlayer, PlayerState.PLAYING);
                    } else {
                        ref.handleStateChange(backgroundAudioPlayer, PlayerState.PAUSED);
                    }
                } else if (playWhenReady) {
                    // resumed
                    playing = true;
                    ref.handlePositionUpdates();
                    ref.handleStateChange(backgroundAudioPlayer, PlayerState.PLAYING);
                } else if (!playWhenReady) {
                    // paused
                    playing = false;
                    ref.handleStateChange(backgroundAudioPlayer, PlayerState.PAUSED);
                }
                break;
            }
            case Player.STATE_ENDED: {
                // completed
                playing = false;
                completed = true;
                player.setPlayWhenReady(false);
                player.seekTo(0, 0);
                break;
            }
            case Player.STATE_IDLE: {
                buffering = true;
                playing = false;
                stopped = false;
                completed = false;
                ref.handleStateChange(backgroundAudioPlayer, PlayerState.BUFFERING);
                break;
            }
            // handle of released is in release method!
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
        Log.d(TAG, "getFallbackUrl: fallbackUrlList=" + fallbackUrlList);
        Log.d(TAG, "getFallbackUrl: currentFallbackUrlIndex=" + currentFallbackUrlIndex);
        return fallbackUrlList.get(currentFallbackUrlIndex);
    }

    private void addAnalyticsListener() {
        player.addAnalyticsListener(new BackgroundPlayerAnalyticsListener(new BackgroundPlayerAnalyticsListener.AnalyticsCallback() {
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
                ref.handleAudioSessionIdChange(backgroundAudioPlayer, audioSessionId);
            }
        }));
    }

    private void addEventListener() {
        player.addListener(new BackgroundEventListener(new EventListenerCallback() {
            public void doOnTracksChanged() {
                ref.handlePlayerIndex(backgroundAudioPlayer);
            }

            @Override
            public void doOnPlayerStatusChanged(boolean playWhenReady, int playbackState) {
                doOnPlayerStateChanged(playWhenReady, playbackState);
            }
        }));
    }
}