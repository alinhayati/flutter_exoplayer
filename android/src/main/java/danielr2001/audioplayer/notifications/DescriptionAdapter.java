package danielr2001.audioplayer.notifications;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import java.util.Map;

import danielr2001.audioplayer.interfaces.AsyncResponse;
import danielr2001.audioplayer.models.AudioObject;


public class DescriptionAdapter implements PlayerNotificationManager.MediaDescriptionAdapter {

    private AudioObject audioObject;
    private Activity activity;
    private PendingIntent currentContentPendingIntent = null;

    public DescriptionAdapter(AudioObject audioObject, Activity activity) {
        this.audioObject = audioObject;
        this.activity = activity;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public String getCurrentContentTitle(Player player) {
        return audioObject.getTitle();
    }

    @Nullable
    @Override
    public PendingIntent createCurrentContentIntent(Player player) {
        return createCurrentContentPendingIntent();
    }

    @Nullable
    @Override
    public String getCurrentContentText(Player player) {
        return audioObject.getSubTitle();
    }

    @Nullable
    @Override
    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
        if (audioObject.getLargeIconUrl() != null) {
            return loadImageFromUrl(audioObject.getLargeIconUrl(), audioObject.getIsLocal());
        } else {
            return null;
        }
    }

    private PendingIntent createCurrentContentPendingIntent() {
        if (currentContentPendingIntent != null) return currentContentPendingIntent;
        Intent notificationIntent = new Intent(activity, activity.getClass());
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        currentContentPendingIntent = PendingIntent.getActivity(activity, 0, notificationIntent, 0);
        return currentContentPendingIntent;
    }

    private Bitmap bitmap = null;
    private Bitmap loadImageFromUrl(String imageUrl, boolean isLocal) {
        try {
            new LoadImageFromUrl(imageUrl, isLocal, new AsyncResponse() {
                @Override
                public void processFinish(Map<String,Bitmap> bitmapMap) {
                    if (bitmapMap != null) {
                        if(bitmapMap.get(audioObject.getLargeIconUrl()) != null){
                            audioObject.setLargeIcon(bitmapMap.get(audioObject.getLargeIconUrl()));
                            bitmap = bitmapMap.get(audioObject.getLargeIconUrl());
                        }else{
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
        return  bitmap;
    }
}
