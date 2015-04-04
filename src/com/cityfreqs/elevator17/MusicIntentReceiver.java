package com.cityfreqs.elevator17;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MusicIntentReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context ctx, Intent intent) {
		if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			Toast.makeText(ctx, "Headphones disconnected", Toast.LENGTH_SHORT).show();
			
			ctx.startService(new Intent(MusicService.ACTION_PAUSE));
		}
	}
}