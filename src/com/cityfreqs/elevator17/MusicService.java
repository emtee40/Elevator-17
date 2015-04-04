package com.cityfreqs.elevator17;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;


public class MusicService extends Service implements OnCompletionListener, OnPreparedListener,
	OnErrorListener, MusicFocusable {
	
	NotificationManager mNotificationManager;
	MediaPlayer mPlayer = null;
	AudioFocusHelper mAudioFocusHelper = null;
	
	enum State {
		Stopped,		// stopped and not prepared
		Preparing,		// ...
		Playing,		// playback active, can be paused in lost focus
		Paused,			// paused but ready to play			
		Ended			// reached eof
	};
	State mState = State.Stopped;
	
	// flag to indicate init state to ready to play
	boolean mStartPlayingAfterRetrieve = false;
	/*
	enum PauseReason {
		UserRequest,
		FocusLoss
	};
	PauseReason mPauseReason = PauseReason.UserRequest;
	*/
	enum AudioFocus {
		NoFocusNoDuck,
		NoFocusCanDuck,
		Focused
	}
	AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
	
	// Intent actions to be handled here for control of mediaplayer
	public static final String ACTION_PLAY = "com.cityfreqs.elevator17.action.PLAY";
	public static final String ACTION_PAUSE = "com.cityfreqs.elevator17.action.PAUSE";
	public static final String ACTION_STOP = "com.cityfreqs.elevator17.action.STOP";
	
	public final float DUCK_VOLUME = 0.1f;
	final int NOTIFICATION_ID = 1;
	Notification mNotification = null;
	// travelFloors
	public static final String ACTION_BROADCAST = "com.cityfreqs.elevator17.action.BROADCAST";
	private final Handler handler = new Handler();
	Intent broadcastIntent;
	
	void createMediaPlayerIfNeeded() {
		if (mPlayer == null) {
			mPlayer = new MediaPlayer();
			try {
				mPlayer = MediaPlayer.create(this, R.raw.cfp_raem_elevator17_live);
			} catch (Exception ex) {
				ex.printStackTrace();
				//bad times
			}
			mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
			mPlayer.setOnPreparedListener(this);
			mPlayer.setOnCompletionListener(this);
			mPlayer.setOnErrorListener(this);
		}
		else
			mPlayer.reset();
	}
	
	@Override
	public void onCreate() {
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		broadcastIntent = new Intent(ACTION_BROADCAST);
		
		if (android.os.Build.VERSION.SDK_INT >= 8) 
			mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
		else
			mAudioFocus = AudioFocus.Focused; // no focus feature so always have focus
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		// travelFloors
		handler.removeCallbacks(sendUpdatesToUI);
		handler.postDelayed(sendUpdatesToUI, 1000);
		
		if (action.equals(ACTION_PLAY)) processPlayRequest();
		else if (action.equals(ACTION_PAUSE)) processPauseRequest();
		else if (action.equals(ACTION_STOP)) processStopRequest();
		// means started service, but don't restart service if its killed
		return START_NOT_STICKY;
	}
	
	// travelFloors
	private Runnable sendUpdatesToUI = new Runnable() {
		public void run() {
			displayLoggingInfo();
			handler.postDelayed(this, 2000);
		}
	};
	private void displayLoggingInfo() {
		if (mState == State.Playing || mState == State.Paused) {
			broadcastIntent.putExtra("playhead", mPlayer.getCurrentPosition());			
			if (mState == State.Playing)
				broadcastIntent.putExtra("state", 1);
			else if (mState == State.Paused)
				broadcastIntent.putExtra("state", 2);
			
			sendBroadcast(broadcastIntent);
		}
		else if (mState == State.Ended) {
			broadcastIntent.putExtra("state", 4);
			sendBroadcast(broadcastIntent);
		}
	}
	
	void processPlayRequest() {
		tryToGetAudioFocus();
		
		if (mState == State.Stopped) {
			playSong();
		}
		else if (mState == State.Paused) {
			mState = State.Playing;
			setUpAsForeground("Elevator 17 playing");
			configAndStartMediaPlayer();
		}	
	}
	
	void processPauseRequest() {
		if (mState == State.Playing) {
			mState = State.Paused;
			mPlayer.pause();
			relaxResources(false); // retain mPlayer while paused
			giveUpAudioFocus();
		}
	}
	
	void processStopRequest() {
		if (mState == State.Playing || mState == State.Paused) {
			mState = State.Stopped;
			relaxResources(true);
			giveUpAudioFocus();
			stopSelf(); // end service
		} else {
			// possible null of mPlayer here
			relaxResources(true);
			giveUpAudioFocus();
			stopSelf(); // end service
		}
	}
	
	void relaxResources(boolean releaseMediaPlayer) {
		stopForeground(true);
		
		if (releaseMediaPlayer && mPlayer != null) {
			mPlayer.reset();
			mPlayer.release();
			mPlayer = null;
		}
	}
	
	void giveUpAudioFocus() {
		if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
				&& mAudioFocusHelper.abandonFocus())
			mAudioFocus = AudioFocus.NoFocusNoDuck;
	}
	
	void configAndStartMediaPlayer() {
		if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
			if (mPlayer.isPlaying()) mPlayer.pause();
			return;
		}
		else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
			mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);
		else
			mPlayer.setVolume(1.0f, 1.0f);
		
		if (!mPlayer.isPlaying()) mPlayer.start();
	}

	void tryToGetAudioFocus() {
		if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
				&& mAudioFocusHelper.requestFocus())
			mAudioFocus = AudioFocus.Focused;
	}
	
	void playSong() {
		mState = State.Stopped;
		relaxResources(false);
		
		try {
			createMediaPlayerIfNeeded();
			mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mState = State.Preparing;
			setUpAsForeground("Elevator 17 loading");
			// getting warning error here from below line
			//mPlayer.prepareAsync();
		} 
		catch (Exception ex) {
			Log.e("Elevator 17", "music service: " + ex.getMessage());
			ex.printStackTrace();
		}
	}	
	
	@Override
	public void onCompletion(MediaPlayer player) {
		// player reaches end of track
		mState = State.Ended;
		// attempt to notify UI of eof
		displayLoggingInfo();
		relaxResources(true);
		giveUpAudioFocus();
		stopSelf();
		// stop updates
		handler.removeCallbacks(sendUpdatesToUI);
	}
	
	public void onPrepared(MediaPlayer player) {
		mState = State.Playing;
		updateNotification("Elevator 17 playing");
		configAndStartMediaPlayer();
	}
	
	@SuppressWarnings("deprecation")
	void updateNotification(String text) {
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
				new Intent(getApplicationContext(), MainActivity.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		mNotification.setLatestEventInfo(getApplicationContext(), "Elevator 17", text, pi);
		mNotificationManager.notify(NOTIFICATION_ID, mNotification);
	}
	
	@SuppressWarnings("deprecation")
	void setUpAsForeground(String text) {
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
				new Intent(getApplicationContext(), MainActivity.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		mNotification = new Notification();
		mNotification.tickerText = text;
		mNotification.icon = R.drawable.ic_launcher;
		mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
		mNotification.setLatestEventInfo(getApplicationContext(), "Elevator 17", text, pi);
		startForeground(NOTIFICATION_ID, mNotification);
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mState = State.Stopped;
		relaxResources(true);
		giveUpAudioFocus();
		return true; // true indicates handled the error
	}
	
	@Override
	public void onGainedAudioFocus() {
		mAudioFocus = AudioFocus.Focused;		
		if (mState == State.Playing)
			configAndStartMediaPlayer();
	}
	
	@Override
	public void onLostAudioFocus(boolean canDuck) {
		mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;		
		if (mPlayer != null && mPlayer.isPlaying())
			configAndStartMediaPlayer();
	}
	
	@Override
	public void onDestroy() {
		mState = State.Stopped;
		relaxResources(true);
		giveUpAudioFocus();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}