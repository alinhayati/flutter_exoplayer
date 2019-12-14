package danielr2001.audioplayer.audioplayers;

class InsightExoPlayerConstants {
    /**
     * The default minimum duration of media that the player will attempt to ensure is buffered at all
     * times, in milliseconds.
     */
    static final int DEFAULT_MIN_BUFFER_MS = 15000;

    /**
     * The default maximum duration of media that the player will attempt to buffer, in milliseconds.
     * <p>
     * We want to let exoplayer to burst buffering initially instead of keeping a long-live connection and buffer gradually
     * 2 HOURS
     */
    static final int DEFAULT_MAX_BUFFER_MS = 2 * 60 * 60 * 1000;

    /**
     * The default duration of media that must be buffered for playback to start or resume following a
     * user action such as a seek, in milliseconds.
     */
    static final int DEFAULT_BUFFER_FOR_PLAYBACK_MS = 2500;

    /**
     * The default duration of media that must be buffered for playback to resume after a rebuffer,
     * in milliseconds. A rebuffer is defined to be caused by buffer depletion rather than a user
     * action.
     */
    static final int DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5000;

    static final  long DEFAULT_MEDIA_CACHE_SIZE = 200 * 1024 * 1024L;
}
