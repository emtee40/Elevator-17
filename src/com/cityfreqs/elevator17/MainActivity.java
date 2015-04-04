package com.cityfreqs.elevator17;

import com.cityfreqs.elevator17.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.FloatMath;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class MainActivity extends Activity implements OnClickListener {
	// UI controls
	private ImageButton closeButton;
	private ImageButton openButton;
	private ImageButton pauseButton;
	// photo slide show 
	private ImageView photoView;
	// text view intros
	private TextView gndIntro;
	// website linker views
	private TextView footer;
	private ImageView cfpLogo;
	// floor letters for colour changes etc
	private TextView floorG;
	private TextView floor1;
	private TextView floor2;
	private TextView floor3;
	private TextView floor4;
	private TextView floor5;
	private TextView floor6;
	private TextView floor7;
	private TextView floor8;
	private TextView floor9;
	private TextView floor10;
	private int floor;
	private int key;

	private int photoRes;
	private int photoCount;
	private Handler photoHandler;
	private Animation animFadeIn;
	private Animation animFadeOut;
	private int currentPos;
	private int mState; //0=stopped, 1=playing, 2=paused, 4=eof
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // reference all xml objects here
        photoView = (ImageView) findViewById(R.id.photoView);
        closeButton = (ImageButton) findViewById(R.id.close_button1);
        openButton = (ImageButton) findViewById(R.id.open_button1);
        pauseButton = (ImageButton) findViewById(R.id.pause_button1);
        // site url linkers
        footer = (TextView) findViewById(R.id.footer);
        cfpLogo = (ImageView) findViewById(R.id.cfpLogo);
        // button controls
        closeButton.setOnClickListener(this); //plays music, pause
        closeButton.setEnabled(true);
        openButton.setOnClickListener(this); // stop, exits player
        openButton.setEnabled(true);
        pauseButton.setOnClickListener(this); // pause audio
        pauseButton.setEnabled(false);
        // add photo slide manual control
        photoView.setOnClickListener(this);
        // footer listener
        footer.setOnClickListener(this);
        cfpLogo.setOnClickListener(this);
        // intro text views
        gndIntro = (TextView) findViewById(R.id.gndIntro);
        // floor texts ref here
        floorG = (TextView) findViewById(R.id.floorG);
        floor1 = (TextView) findViewById(R.id.floor1);
        floor2 = (TextView) findViewById(R.id.floor2);
        floor3 = (TextView) findViewById(R.id.floor3);
        floor4 = (TextView) findViewById(R.id.floor4);
        floor5 = (TextView) findViewById(R.id.floor5);
        floor6 = (TextView) findViewById(R.id.floor6);
        floor7 = (TextView) findViewById(R.id.floor7);
        floor8 = (TextView) findViewById(R.id.floor8);
        floor9 = (TextView) findViewById(R.id.floor9);
        floor10 = (TextView) findViewById(R.id.floor10); 
        
        photoCount = 0; // 1 is already displayed
        photoHandler = new Handler();
        animFadeIn = AnimationUtils.loadAnimation(this, R.anim.fadein);
        animFadeOut = AnimationUtils.loadAnimation(this, R.anim.fadeout);
        key = 224000; // ms per floor
    }
    
    @Override
    public void onClick(View target) {
    	// send intent to music service
    	if (target == closeButton) {
    		startService(new Intent(MusicService.ACTION_PLAY));
    		noIntroText();
    		playingButtons();
    		nextPhoto();
    		photoHandler.removeCallbacks(slideShow);
    		photoHandler.postDelayed(slideShow, 13800);
    	}
    	else if (target == openButton) {
    		startService(new Intent(MusicService.ACTION_STOP));
    		addExitButton();
    	}
    	else if (target == pauseButton) {
    		startService(new Intent(MusicService.ACTION_PAUSE));
    		pausingButtons();
			Toast.makeText(getApplicationContext(), "Elevator 17 paused",
					Toast.LENGTH_SHORT).show();
    	}
    	else if (target == photoView) {
    		nextPhoto();
    		photoHandler.removeCallbacks(slideShow);
    		photoHandler.postDelayed(slideShow, 13800);
    	}
    	else if (target == footer || target == cfpLogo) {
    		openWebsite();
    	}
    }
	
	private void addExitButton() {			
		// exit the elevator, if while playing or paused
		if (mState == 1 || mState == 2) {
			// change state to stopped
			mState = 0;
			Toast.makeText(getApplicationContext(), "Elevator 17 cleared",
					Toast.LENGTH_SHORT).show();
		}
		photoHandler.removeCallbacks(slideShow);
		finish();
	}
	
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateUI(intent);
		}
	};
	
	private void updateUI(Intent intent) {		
		// floor is approx 224000 ms of audio 		
		currentPos = intent.getIntExtra("playhead", 0);
		mState = intent.getIntExtra("state", 0);
		// round down
		floor = (int)FloatMath.floor(currentPos / key);
		travelNextFloor((int)FloatMath.floor(floor));
		
		// refresh UI after started playing
		if (currentPos > 0) {
			// media service is active
			if (mState == 1) {
				// player is playing
				playingButtons();
			} else if (mState == 2) {
				// player is paused
				pausingButtons();
			}
		}
		
		if (mState == 4) {
			//player has eof
			Toast.makeText(getApplicationContext(), "Elevator 17 end of journey",
					Toast.LENGTH_SHORT).show();
			pausingButtons();
		}
	}
	
	private Runnable slideShow = new Runnable() {
		public void run() {
			nextPhoto();
			photoHandler.postDelayed(this, 13800);
		}
	};
	
	private void nextPhoto() {
		// slide show auto
		noIntroText();
		photoCount++;
		// photos named raem1.jgp - raem179.jpg 
		photoRes = getResources().getIdentifier("raem" + photoCount, "drawable", getPackageName());
		// transition new photo in
		photoView.startAnimation(animFadeOut);		
		photoView.setImageResource(photoRes);
		photoView.startAnimation(animFadeIn);
		
		//check max photos reached
		if (photoCount >= 179) {
			// set for first photo
			photoCount = 1;
		}
	}
	
	private void travelNextFloor(int floor) {
		if (floor <=10) {
			switch(floor) {
			case 0:
				resetFloors();
				floorG.setTextColor(Color.parseColor("#ffdf0b"));
				break;
			case 1:
				resetFloors();
				floor1.setTextColor(Color.parseColor("#ffdf0b"));
				break;
			case 2:
				resetFloors();
				floor2.setTextColor(Color.parseColor("#ffdf0b"));
				break;
			case 3:
				resetFloors();
				floor3.setTextColor(Color.parseColor("#ffdf0b"));
				break;
			case 4:
				resetFloors();
				floor4.setTextColor(Color.parseColor("#ffdf0b"));
				break;
			case 5:
				resetFloors();
				floor5.setTextColor(Color.parseColor("#ffdf0b"));
				break;
			case 6:
				resetFloors();
				floor6.setTextColor(Color.parseColor("#ffdf0b"));
				break;
			case 7:
				resetFloors();
				floor7.setTextColor(Color.parseColor("#ffdf0b"));
				break;
			case 8:
				resetFloors();
				floor8.setTextColor(Color.parseColor("#ffdf0b"));
				break;
			case 9:
				resetFloors();
				floor9.setTextColor(Color.parseColor("#ffdf0b"));
				break;
			case 10:
				resetFloors();
				floor10.setTextColor(Color.parseColor("#ffdf0b"));
				break;
			default:
				// catch here
				// do nothing
			}
		}
	}
	
	private void noIntroText() {
		gndIntro.setVisibility(View.GONE);
		cfpLogo.setVisibility(View.GONE);
	}
	
	private void playingButtons() {
		//resets buttons to be playing
		closeButton.setEnabled(false);
		pauseButton.setEnabled(true);
	}
	
	private void pausingButtons() {
		//resets buttons to be paused
		closeButton.setEnabled(true);
		pauseButton.setEnabled(false);
	}
	
	private void resetFloors() {
		// reset all floors from any state
		floorG.setTextColor(Color.parseColor("#666666"));
		floor1.setTextColor(Color.parseColor("#666666"));
		floor2.setTextColor(Color.parseColor("#666666"));
		floor3.setTextColor(Color.parseColor("#666666"));
		floor4.setTextColor(Color.parseColor("#666666"));
		floor5.setTextColor(Color.parseColor("#666666"));
		floor6.setTextColor(Color.parseColor("#666666"));
		floor7.setTextColor(Color.parseColor("#666666"));
		floor8.setTextColor(Color.parseColor("#666666"));
		floor9.setTextColor(Color.parseColor("#666666"));
		floor10.setTextColor(Color.parseColor("#666666"));
	}
	
	private boolean isMediaServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if ("com.cityfreqs.elevator17.MusicService".equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
	
	private void openWebsite() {
		Intent siteIntent = new Intent();
		siteIntent.setAction(Intent.ACTION_VIEW);
		siteIntent.addCategory(Intent.CATEGORY_BROWSABLE);
		siteIntent.setData(Uri.parse("http://www.cityfreqs.com.au"));
		startActivity(siteIntent);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(broadcastReceiver);
		SharedPreferences elevator17state = getSharedPreferences("elevator17state", MODE_PRIVATE);
		SharedPreferences.Editor edit = elevator17state.edit();
		edit.putInt("photoNum", this.photoCount);
		edit.putInt("mState", this.mState);		
		edit.commit();
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();	
		registerReceiver(broadcastReceiver, new IntentFilter(MusicService.ACTION_BROADCAST));		
		SharedPreferences elevator17state = getSharedPreferences("elevator17state", MODE_PRIVATE);
		mState = elevator17state.getInt("mState", 0);
		// catch for stale mState after eof
		boolean isRunning = isMediaServiceRunning();
		if (!isRunning) {
			// media service not running
			pausingButtons();
			travelNextFloor(0);
		} else {
			// media service running
			if (mState == 1) {
				noIntroText();
				// is playing, continue slides
				photoCount = elevator17state.getInt("photoNum", 0);
				playingButtons();
				nextPhoto();
	    		photoHandler.removeCallbacks(slideShow);
	    		photoHandler.postDelayed(slideShow, 13800);
			} else if (mState == 2) {
				// is paused
				noIntroText();
				pausingButtons();
				nextPhoto();
				photoCount = elevator17state.getInt("photoNum", 0);
			} else if (mState == 0) {
				// resumed from stop
				pausingButtons();
			} else if (mState == 4) {
				// resumed at eof
				pausingButtons();
				travelNextFloor(0);
			}
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
}