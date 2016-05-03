package com.tonycase.simplechime;

/**
 * Listing 9-13: Implementing an Intent Service
 */
import android.app.IntentService;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import java.io.IOException;

/**
 * Play a sound, as specified by the uri in the Intent.
 *
 * An intent service calls onDestroy() immediately on complete of handleIntent(), so unless we
 * do something else, (make it live? use regular service?) we lose our reference to media player
 * and can't shut it down.
 * This would be nice in case of a phone calls (maybe), or in the case of a user playing around with
 * volume or sounds.  But without more sophisticated code, we won't be doing that --
 * see https://code.google.com/p/npr-android-app/source/browse/Npr/src/org/npr/android/news/PlaybackService.java.
 *
 * Note that a media player instance cannot be set with a new source.
 */
public class PlaySoundIntentService extends IntentService {

	public static final String URI_KEY = "soundUri";
	private static final String TAG = "PlaySoundIntentService";
	public static final String VOL_KEY = "volume";
	public static final String PLAY_SOFTER_KEY = "key_play_softer";

    MediaPlayer resPlayer = null;
    private boolean isPrepared = false;
    private Handler handler;

    public PlaySoundIntentService() {
		super(TAG);
		Log.d(TAG, "empty contructor");
	}
	
	public PlaySoundIntentService(String name) {
		super(name);
		Log.d(TAG, "contructor");
	}

	@Override
	public void onCreate() {
		super.onCreate();
        Log.d(TAG, "onCreate");
        handler = new Handler();
	}

	@Override
	protected void onHandleIntent(Intent intent) {

        Log.d(TAG, "onHandleIntent");

		Bundle extras = intent.getExtras();
		if (extras == null) {
			Log.w(TAG, "Extras are null");
			return;
		}
		String uriStr = extras.getString(URI_KEY);
		boolean playSofter = extras.getBoolean(PLAY_SOFTER_KEY);

		if (uriStr == null) {
			Log.w(TAG, "uriStr is null");
			return;
		}
		Uri uri = Uri.parse(uriStr);

		int volume = extras.getInt(VOL_KEY, 10);
        Log.d(TAG, "resPlayer create");
        resPlayer = MediaPlayer.create(this, uri);

		if (resPlayer == null)
			resPlayer = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		if (resPlayer == null) return;

		// Set the actual volumes for volume settings 0 to 10.  We use the softer sequence if that's
		//   specified, which I believe it is for phone sounds, as opposed to our sounds with the app.
        float[] volumes = playSofter
				? new float[] { 0f,  .0015f, .004f, .015f, .04f,
                                       .1f,  .2f,   .36f,  .59f,  .8f,  1.0f }
				: new float[] { 0f,  .003f, .009f,  .028f, .08f,
									 .16f,  .27f,  .41f, .61f, .81f,  1.0f };
        volume = Math.min(volume, 10);
        volume = Math.max(0, volume);
		float vol = volumes[volume];
		Log.d(TAG, "setting volume to " + vol);
		resPlayer.setVolume(vol, vol);
		resPlayer.start();
        isPrepared = true;

        handler.postDelayed(new StopPlayerRunnable(intent), 4500);
    }

    private class StopPlayerRunnable implements Runnable {

		private @Nullable Intent intent;

		StopPlayerRunnable() {
			// no args constructor Ok.
		}

		StopPlayerRunnable(@Nullable Intent intent) {
			this.intent = intent;
		}

        @Override
        public void run() {
            stopPlayer();
			if (intent != null) {
                // release the wake lock in case this came from our wakeful receiver.
				WakefulBroadcastReceiver.completeWakefulIntent(intent);
			}
		}
    };

    private void stopPlayer() {
        Log.d(TAG, "stop player");
        if (resPlayer != null && isPrepared) {
            if (resPlayer.isPlaying()) {
                resPlayer.stop();
            }
            resPlayer.release();
        }
        isPrepared = false;
    }
}
