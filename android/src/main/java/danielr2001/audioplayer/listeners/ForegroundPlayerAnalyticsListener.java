package danielr2001.audioplayer.listeners;

import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import java.io.IOException;

public class ForegroundPlayerAnalyticsListener implements AnalyticsListener {
    private static final String TAG = "ForegroundAnalytics";

    private AnalyticsCallback callback;

    public ForegroundPlayerAnalyticsListener(AnalyticsCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady,
                                     int playbackState) {
        Log.v(TAG,
                "onPlayerStateChanged:\neventTime=" + eventTime.currentPlaybackPositionMs + "\nplayWhenReady=" + playWhenReady + " \nplaybackState=" + playbackState);
    }

    @Override
    public void onTimelineChanged(EventTime eventTime, int reason) {
        Log.v(TAG,
                "onTimelineChanged:\neventTime=" + eventTime.currentPlaybackPositionMs +
                        "\nreason=" + reason);
    }

    @Override
    public void onPositionDiscontinuity(EventTime eventTime, int reason) {
        Log.v(TAG,
                "onPositionDiscontinuity:\neventTime=" + eventTime.currentPlaybackPositionMs + "\nreason=" + reason);
    }

    @Override
    public void onSeekStarted(EventTime eventTime) {
        Log.v(TAG,
                "onSeekStarted:\neventTime=" + eventTime.currentPlaybackPositionMs);
    }

    @Override
    public void onSeekProcessed(EventTime eventTime) {
        Log.v(TAG,
                "onSeekProcessed:\neventTime=" + eventTime.currentPlaybackPositionMs);
    }

    @Override
    public void onPlaybackParametersChanged(EventTime eventTime,
                                            PlaybackParameters playbackParameters) {
        Log.v(TAG,
                "onPlaybackParametersChanged:\neventTime=" + eventTime.currentPlaybackPositionMs + " PlaybackParameters:" + "\n    pitch=" + playbackParameters.pitch + "\n    skipSilence=" + playbackParameters.skipSilence + "\n     speed=" + playbackParameters.speed);
    }

    @Override
    public void onRepeatModeChanged(EventTime eventTime, int repeatMode) {
        Log.v(TAG,
                "onRepeatModeChanged:\neventTime=" + eventTime.currentPlaybackPositionMs + "\nrepeatMode=" + repeatMode);
    }

    @Override
    public void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) {
        Log.v(TAG,
                "onShuffleModeChanged:\neventTime=" + eventTime.currentPlaybackPositionMs + "\nrepeatMode=" + shuffleModeEnabled);
    }

    @Override
    public void onLoadingChanged(EventTime eventTime, boolean isLoading) {
        Log.v(TAG,
                "onLoadingChanged:\neventTime=" + eventTime.currentPlaybackPositionMs +
                        "\nrepeatMode=" + isLoading);
    }

    @Override
    public void onPlayerError(EventTime eventTime, ExoPlaybackException error) {
        Log.v(TAG,
                "onPlayerError:\neventTime=" + eventTime.currentPlaybackPositionMs +
                        "\nerror=" + error.getMessage());
    }

    @Override
    public void onTracksChanged(EventTime eventTime, TrackGroupArray trackGroups,
                                TrackSelectionArray trackSelections) {
        Log.v(TAG, "onTracksChanged");
    }

    @Override
    public void onLoadStarted(EventTime eventTime,
                              MediaSourceEventListener.LoadEventInfo loadEventInfo,
                              MediaSourceEventListener.MediaLoadData mediaLoadData) {
        Log.v(TAG,
                "onLoadStarted:\neventTime=" + eventTime.currentPlaybackPositionMs +
                        "\nloadEventInfo:" + "\n  bytes loaded:" + loadEventInfo.bytesLoaded + "\n    dataSpec:" + "\n       key=" + loadEventInfo.dataSpec.key + "\n       absoluteStreamPosition=" + loadEventInfo.dataSpec.absoluteStreamPosition + "\n       flags=" + loadEventInfo.dataSpec.flags + "\n       uri=" + loadEventInfo.dataSpec.uri + "\n       httpMethodString=" + loadEventInfo.dataSpec.getHttpMethodString() + "\n    elapsedRealtimeMs:" + loadEventInfo.elapsedRealtimeMs + "\n    loadDurationMs:" + loadEventInfo.loadDurationMs + "\n    entrySet:" + loadEventInfo.responseHeaders.entrySet() + "\nMediaLoadData:" + "\n    dataType=" + mediaLoadData.dataType + "\n    mediaEndTimeMs=" + mediaLoadData.mediaEndTimeMs + "\n    mediaStartTimeMs=" + mediaLoadData.mediaStartTimeMs + "\n    trackSelectionReason=" + mediaLoadData.trackSelectionReason + "\n    trackType=" + mediaLoadData.trackType);
    }

    @Override
    public void onLoadCompleted(EventTime eventTime,
                                MediaSourceEventListener.LoadEventInfo loadEventInfo,
                                MediaSourceEventListener.MediaLoadData mediaLoadData) {
        Log.v(TAG,
                "onLoadCompleted:\neventTime=" + eventTime.currentPlaybackPositionMs +
                        "\nloadEventInfo:" + "\n  bytes loaded:" + loadEventInfo.bytesLoaded + "\n    dataSpec:" + "\n       key=" + loadEventInfo.dataSpec.key + "\n       absoluteStreamPosition=" + loadEventInfo.dataSpec.absoluteStreamPosition + "\n       flags=" + loadEventInfo.dataSpec.flags + "\n       uri=" + loadEventInfo.dataSpec.uri + "\n       httpMethodString=" + loadEventInfo.dataSpec.getHttpMethodString() + "\n    elapsedRealtimeMs:" + loadEventInfo.elapsedRealtimeMs + "\n    loadDurationMs:" + loadEventInfo.loadDurationMs + "\n    entrySet:" + loadEventInfo.responseHeaders.entrySet() + "\nMediaLoadData:" + "\n    dataType=" + mediaLoadData.dataType + "\n    mediaEndTimeMs=" + mediaLoadData.mediaEndTimeMs + "\n    mediaStartTimeMs=" + mediaLoadData.mediaStartTimeMs + "\n    trackSelectionReason=" + mediaLoadData.trackSelectionReason + "\n    trackType=" + mediaLoadData.trackType);
    }

    @Override
    public void onLoadCanceled(EventTime eventTime,
                               MediaSourceEventListener.LoadEventInfo loadEventInfo,
                               MediaSourceEventListener.MediaLoadData mediaLoadData) {
        Log.v(TAG,
                "onLoadCanceled:\neventTime=" + eventTime.currentPlaybackPositionMs +
                        "\nloadEventInfo:" + "\n  bytes loaded:" + loadEventInfo.bytesLoaded + "\n    dataSpec:" + "\n       key=" + loadEventInfo.dataSpec.key + "\n       absoluteStreamPosition=" + loadEventInfo.dataSpec.absoluteStreamPosition + "\n       flags=" + loadEventInfo.dataSpec.flags + "\n       uri=" + loadEventInfo.dataSpec.uri + "\n       httpMethodString=" + loadEventInfo.dataSpec.getHttpMethodString() + "\n    elapsedRealtimeMs:" + loadEventInfo.elapsedRealtimeMs + "\n    loadDurationMs:" + loadEventInfo.loadDurationMs + "\n    entrySet:" + loadEventInfo.responseHeaders.entrySet() + "\nMediaLoadData:" + "\n    dataType=" + mediaLoadData.dataType + "\n    mediaEndTimeMs=" + mediaLoadData.mediaEndTimeMs + "\n    mediaStartTimeMs=" + mediaLoadData.mediaStartTimeMs + "\n    trackSelectionReason=" + mediaLoadData.trackSelectionReason + "\n    trackType=" + mediaLoadData.trackType);
    }

    @Override
    public void onLoadError(EventTime eventTime,
                            MediaSourceEventListener.LoadEventInfo loadEventInfo,
                            MediaSourceEventListener.MediaLoadData mediaLoadData,
                            IOException error, boolean wasCanceled) {
        Log.v(TAG,
                "onLoadError:\neventTime=" + eventTime.currentPlaybackPositionMs +
                        "\nloadEventInfo:" + "\n  bytes loaded:" + loadEventInfo.bytesLoaded + "\n    dataSpec:" + "\n       key=" + loadEventInfo.dataSpec.key + "\n       absoluteStreamPosition=" + loadEventInfo.dataSpec.absoluteStreamPosition + "\n       flags=" + loadEventInfo.dataSpec.flags + "\n       uri=" + loadEventInfo.dataSpec.uri + "\n       httpMethodString=" + loadEventInfo.dataSpec.getHttpMethodString() + "\n    elapsedRealtimeMs:" + loadEventInfo.elapsedRealtimeMs + "\n    loadDurationMs:" + loadEventInfo.loadDurationMs + "\n    entrySet:" + loadEventInfo.responseHeaders.entrySet() + "\nMediaLoadData:" + "\n    dataType=" + mediaLoadData.dataType + "\n    mediaEndTimeMs=" + mediaLoadData.mediaEndTimeMs + "\n    mediaStartTimeMs=" + mediaLoadData.mediaStartTimeMs + "\n    trackSelectionReason=" + mediaLoadData.trackSelectionReason + "\n    trackType=" + mediaLoadData.trackType + "\n   error=" + error.getMessage() + "\n  wasCanceled=" + wasCanceled);
        callback.doOnLoadError(eventTime, loadEventInfo);
    }

    @Override
    public void onDownstreamFormatChanged(EventTime eventTime,
                                          MediaSourceEventListener.MediaLoadData mediaLoadData) {
        Log.v(TAG,
                "onDownstreamFormatChanged:\neventTime=" + eventTime.currentPlaybackPositionMs + "\nMediaLoadData:" + "\n    dataType=" + mediaLoadData.dataType + "\n    mediaEndTimeMs=" + mediaLoadData.mediaEndTimeMs + "\n    mediaStartTimeMs=" + mediaLoadData.mediaStartTimeMs + "\n    trackSelectionReason=" + mediaLoadData.trackSelectionReason + "\n    trackType=" + mediaLoadData.trackType);
    }

    @Override
    public void onUpstreamDiscarded(EventTime eventTime,
                                    MediaSourceEventListener.MediaLoadData mediaLoadData) {
        Log.v(TAG,
                "onUpstreamDiscarded:\neventTime=" + eventTime.currentPlaybackPositionMs + "\nMediaLoadData:" + "\n    dataType=" + mediaLoadData.dataType + "\n    mediaEndTimeMs=" + mediaLoadData.mediaEndTimeMs + "\n    mediaStartTimeMs=" + mediaLoadData.mediaStartTimeMs + "\n    trackSelectionReason=" + mediaLoadData.trackSelectionReason + "\n    trackType=" + mediaLoadData.trackType);
    }

    @Override
    public void onMediaPeriodCreated(EventTime eventTime) {
        Log.v(TAG,
                "onMediaPeriodCreated:\neventTime=" + eventTime.currentPlaybackPositionMs);
    }

    @Override
    public void onMediaPeriodReleased(EventTime eventTime) {
        Log.v(TAG,
                "onMediaPeriodReleased:\neventTime=" + eventTime.currentPlaybackPositionMs);
    }

    @Override
    public void onReadingStarted(EventTime eventTime) {
        Log.v(TAG,
                "onReadingStarted:\neventTime=" + eventTime.currentPlaybackPositionMs);
    }

    @Override
    public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs,
                                    long totalBytesLoaded, long bitrateEstimate) {
        Log.v(TAG,
                "onBandwidthEstimate:\neventTime=" + eventTime.currentPlaybackPositionMs + "\ntotalLoadTimeMs=" + totalLoadTimeMs + "\ntotalBytesLoaded=" + totalBytesLoaded + "\nbitrateEstimate" + bitrateEstimate);
    }

    @Override
    public void onSurfaceSizeChanged(EventTime eventTime, int width, int height) {
        Log.v(TAG,
                "onSurfaceSizeChanged:\neventTime=" + eventTime.currentPlaybackPositionMs);
    }

    @Override
    public void onMetadata(EventTime eventTime, Metadata metadata) {
        Log.v(TAG,
                "onMetadata:\neventTime=" + eventTime.currentPlaybackPositionMs);
    }

    @Override
    public void onDecoderEnabled(EventTime eventTime, int trackType,
                                 DecoderCounters decoderCounters) {
        Log.v(TAG,
                "onDecoderEnabled:\neventTime=" + eventTime.currentPlaybackPositionMs);
    }

    @Override
    public void onDecoderInitialized(EventTime eventTime, int trackType,
                                     String decoderName, long initializationDurationMs) {
        Log.v(TAG,
                "onDecoderInitialized:\neventTime=" + eventTime.currentPlaybackPositionMs);
    }

    @Override
    public void onDecoderInputFormatChanged(EventTime eventTime, int trackType,
                                            Format format) {
        Log.v(TAG,
                "onDecoderInputFormatChanged:\neventTime=" + eventTime.currentPlaybackPositionMs);
    }

    @Override
    public void onDecoderDisabled(EventTime eventTime, int trackType,
                                  DecoderCounters decoderCounters) {
        Log.v(TAG,
                "onDecoderDisabled:\neventTime=" + eventTime.currentPlaybackPositionMs);
    }

    @Override
    public void onAudioSessionId(EventTime eventTime, int audioSessionId) {
        Log.v(TAG,
                "onAudioSessionId:\neventTime=" + eventTime.currentPlaybackPositionMs +
                        "\naudioSessionId=" + audioSessionId);
        callback.doOnAudioSessionId(audioSessionId);

    }

    @Override
    public void onAudioAttributesChanged(EventTime eventTime,
                                         AudioAttributes audioAttributes) {
        Log.v(TAG,
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
        Log.v(TAG,
                "onRenderedFirstFrame:\neventTime=" + eventTime.currentPlaybackPositionMs);
    }

    public interface AnalyticsCallback{
        void doOnLoadError(AnalyticsListener.EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo);
        void doOnAudioSessionId(int audioSessionId);
    }
}
