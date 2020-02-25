package danielr2001.audioplayer.listeners;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

public class BackgroundEventListener implements Player.EventListener {
    private EventListenerCallback callback;

    public BackgroundEventListener(EventListenerCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups,
                                TrackSelectionArray trackSelections) {
        callback.doOnTracksChanged();
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        callback.doOnPlayerStatusChanged(playWhenReady, playbackState);
    }
}