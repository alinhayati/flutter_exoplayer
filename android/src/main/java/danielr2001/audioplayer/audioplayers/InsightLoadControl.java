package danielr2001.audioplayer.audioplayers;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.util.PriorityTaskManager;
import com.google.android.exoplayer2.util.Util;

public class InsightLoadControl implements LoadControl {

    /**
     * The default minimum duration of media that the player will attempt to ensure is buffered at all
     * times, in milliseconds.
     */
    private static final int DEFAULT_MIN_BUFFER_MS = 15000;

    /**
     * The default maximum duration of media that the player will attempt to buffer, in milliseconds.
     * <p>
     * We want to let exoplayer to burst buffering initially instead of keeping a long-live connection and buffer gradually
     * 2 HOURS
     */
    private static final int DEFAULT_MAX_BUFFER_MS = 2 * 60 * 60 * 1000;

    /**
     * The default duration of media that must be buffered for playback to start or resume following a
     * user action such as a seek, in milliseconds.
     */
    private static final int DEFAULT_BUFFER_FOR_PLAYBACK_MS = 2500;

    /**
     * The default duration of media that must be buffered for playback to resume after a rebuffer,
     * in milliseconds. A rebuffer is defined to be caused by buffer depletion rather than a user
     * action.
     */
    private static final int DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5000;

    private static final int ABOVE_HIGH_WATERMARK = 0;
    private static final int BETWEEN_WATERMARKS = 1;
    private static final int BELOW_LOW_WATERMARK = 2;


    private int targetBufferSize = 0;
    private boolean isBuffering = false;

    private DefaultAllocator allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
    private PriorityTaskManager priorityTaskManager = new PriorityTaskManager();

    @Override
    public void onPrepared() {
        reset(false);
    }

    @Override
    public void onTracksSelected(Renderer[] renderers, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        targetBufferSize = 0;
        for (int i = 0; i < renderers.length; i++) {
            if (trackSelections.get(i) != null) {
                targetBufferSize += Util.getDefaultBufferSize(renderers[i].getTrackType());
            }
        }
        allocator.setTargetBufferSize(targetBufferSize);
    }

    @Override
    public void onStopped() {
        reset(true);
    }

    @Override
    public void onReleased() {
        reset(true);
    }

    @Override
    public Allocator getAllocator() {
        return allocator;
    }

    @Override
    public long getBackBufferDurationUs() {
        return 15L * 1000000L; // Return value is in microseconds
    }

    private int getBufferTimeState(Long bufferedDurationUs) {
        Long minBufferUs = DEFAULT_MIN_BUFFER_MS * 1000L;
        Long maxBufferUs = DEFAULT_MAX_BUFFER_MS * 1000L;
        if (bufferedDurationUs > maxBufferUs) {
            return ABOVE_HIGH_WATERMARK;
        } else if (bufferedDurationUs < minBufferUs) {
            return BELOW_LOW_WATERMARK;
        } else {
            return BETWEEN_WATERMARKS;
        }
    }

    @Override
    public boolean retainBackBufferFromKeyframe() {
        return false;
    }

    @Override
    public boolean shouldContinueLoading(long bufferedDurationUs, float playbackSpeed) {
        int bufferTimeState = getBufferTimeState(bufferedDurationUs);
        boolean targetBufferSizeReached = allocator.getTotalBytesAllocated() >= targetBufferSize;
        boolean wasBuffering = isBuffering;
        isBuffering = bufferTimeState == BELOW_LOW_WATERMARK
                || (bufferTimeState == BETWEEN_WATERMARKS
                && !targetBufferSizeReached);
        if (priorityTaskManager != null && isBuffering != wasBuffering) {
            if (isBuffering) {
                priorityTaskManager.add(C.PRIORITY_PLAYBACK);
            } else {
                priorityTaskManager.remove(C.PRIORITY_PLAYBACK);
            }
        }
        return isBuffering;
    }

    @Override
    public boolean shouldStartPlayback(long bufferedDurationUs, float playbackSpeed, boolean rebuffering) {
        long minBufferDurationUs;
        Long bufferForPlaybackUs = DEFAULT_BUFFER_FOR_PLAYBACK_MS * 1000L;
        Long bufferForPlaybackAfterRebufferUs = DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS * 1000L;
        if (rebuffering)
            minBufferDurationUs = bufferForPlaybackAfterRebufferUs;
        else
            minBufferDurationUs = bufferForPlaybackUs;
        return bufferedDurationUs >= minBufferDurationUs;
    }

    private void reset(boolean resetAllocator) {
        targetBufferSize = 0;
        if (priorityTaskManager != null && isBuffering) {
            priorityTaskManager.remove(C.PRIORITY_PLAYBACK);
        }
        isBuffering = false;
        if (resetAllocator) {
            allocator.reset();
        }
    }
}
