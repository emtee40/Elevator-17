package com.cityfreqs.elevator17;

import android.content.Context;
import android.media.AudioManager;

public class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {
	AudioManager mAM;
	MusicFocusable mFocusable;
	
	public AudioFocusHelper(Context ctx, MusicFocusable focusable) {
		mAM = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
		mFocusable = focusable;
	}
	
	public boolean requestFocus() {
		return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
				mAM.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
	}
	
	public boolean abandonFocus() {
		return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAM.abandonAudioFocus(this);
	}
	
	@Override
	public void onAudioFocusChange(int focusChange) {
		if (mFocusable == null) return;
		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_GAIN:
			mFocusable.onGainedAudioFocus();
			break;
		case AudioManager.AUDIOFOCUS_LOSS:
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			mFocusable.onLostAudioFocus(false);
			break;
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			mFocusable.onLostAudioFocus(true);
			break;
		default:		
		}
	}
}