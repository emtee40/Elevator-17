package com.cityfreqs.elevator17;

public interface MusicFocusable {
	public void onGainedAudioFocus();
	public void onLostAudioFocus(boolean canDuck);
}