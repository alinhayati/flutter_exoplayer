package danielr2001.audioplayer.listeners;

public interface EventListenerCallback {
    void doOnTracksChanged();

    void doOnPlayerStatusChanged(boolean playWhenReady, int playbackState);
}
