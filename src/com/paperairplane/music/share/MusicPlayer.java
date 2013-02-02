package com.paperairplane.music.share;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MusicPlayer extends Activity {
	private int nowPlaying, nowDuration, maxTime;
	private final static int PLAY = 0, PAUSE = 1, STOP = 2,
			PROGRESS_CHANGE = 3;
	private boolean isPlaying = false;
	private final static String DEBUG_TAG = "Music Share DEBUG";
	private Intent musicIntent;
	private SeekBar seekBar;
	private Button btnPP = null;
	private Button btnRT = null;
	private RemoteViews rv = null;
	private NotificationManager nm = null;
	private String title, artist;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);
		int id = 0;
		String duration = null,path=null;
		SharedPreferences pref = MusicPlayer.this.getSharedPreferences("Play Status", Context.MODE_PRIVATE);
		try{
		Bundle extras = getIntent().getExtras();
		id = extras.getInt("id");
		duration = extras.getString("duration");
		path = extras.getString("path");
		title = extras.getString("title");
		artist = extras.getString("artist");
		pref.edit().putInt("id", id).commit();
		pref.edit().putString("duration",duration).commit();
		pref.edit().putString("path", path).commit();
		pref.edit().putString("title", title).commit();
		pref.edit().putString("artist", artist).commit();
		}catch(Exception e){
			id = pref.getInt("id", 0);
			duration = pref.getString("duration", null);
			path = pref.getString("path",null);
			title = pref.getString("title", null);
			artist = pref.getString("artist", null);
			isPlaying = true;//FIXME: �������ˡ����ػ�Activity��ʱ����Ȼ�������˿���û���ָ�״̬��������
		}

		initReceiver();
		initNotification();
		if (!isPlaying || nowPlaying != id) {
			try {
				musicIntent = new Intent();
				musicIntent
						.setAction("com.paperairplane.music.share.PLAYMUSIC");
				Bundle bundle = new Bundle();
				bundle.putString("path", path);
				bundle.putInt("op", PLAY);
				musicIntent.putExtras(bundle);
				startService(musicIntent);
				isPlaying = true;
				nowPlaying = id;
			} catch (Throwable e) {
				Log.d(DEBUG_TAG, "!!" + e.getMessage());
				e.printStackTrace();
			}
		}

		final TextView tvTitle = (TextView) findViewById(R.id.text_player_title);
		final TextView tvSinger = (TextView) findViewById(R.id.text_player_singer);
		tvTitle.setText(title + "(" + duration + ")"
				+ getString(R.string.very_long));
		tvSinger.setText(artist + getString(R.string.very_long));
		seekBar = (SeekBar) findViewById(R.id.seekMusic);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
				play();
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				pause();
			}

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					musicIntent = new Intent();
					musicIntent
							.setAction("com.paperairplane.music.share.PLAYMUSIC");
					Bundle bundle = new Bundle();
					bundle.putInt("op", PROGRESS_CHANGE);
					bundle.putInt("progress", progress);
					musicIntent.putExtras(bundle);
					startService(musicIntent);
				}
			}
		});
		btnPP = (Button) findViewById(R.id.button_player_pause);
		btnRT = (Button) findViewById(R.id.button_player_return);
		btnPP.setBackgroundDrawable(getResources().getDrawable(
				android.R.drawable.ic_media_pause));
		btnRT.setBackgroundDrawable(getResources().getDrawable(
				android.R.drawable.ic_delete));
		btnPP.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isPlaying == true) {
					pause();

				} else if (isPlaying == false) {
					musicIntent = new Intent();
					play();
				}
			}
		});
		btnRT.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				musicIntent = new Intent();
				musicIntent
						.setAction("com.paperairplane.music.share.PLAYMUSIC");
				Bundle bundle = new Bundle();
				bundle.putInt("op", STOP);
				musicIntent.putExtras(bundle);
				startService(musicIntent);
				isPlaying = false;
				finish();

			}
		});
	}

	private void initNotification() {
		try {
			nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			rv = new RemoteViews(MusicPlayer.this.getPackageName(),
					R.layout.remoteplayer);
			String notification_title = getString(R.string.now_playing) + title;
			@SuppressWarnings("deprecation")
			Notification n = new Notification(R.drawable.ic_launcher, notification_title,
					System.currentTimeMillis());
			n.flags = Notification.FLAG_ONGOING_EVENT;
			rv.setTextViewText(R.id.text_remoteplayer_title, title);
			rv.setTextViewText(R.id.text_remoteplayer_singer, artist);
			n.contentView = rv;
			Intent intent = new Intent(this, MusicPlayer.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					intent, PendingIntent.FLAG_UPDATE_CURRENT);
			n.contentIntent = contentIntent;
			nm.notify(0, n);
			Log.d(DEBUG_TAG, "����֪ͨ");
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	protected void play() {
		musicIntent = new Intent();
		musicIntent.setAction("com.paperairplane.music.share.PLAYMUSIC");
		Bundle bundle = new Bundle();
		bundle.putInt("op", PLAY);
		musicIntent.putExtras(bundle);
		startService(musicIntent);
		btnPP.setBackgroundDrawable(getResources().getDrawable(
				android.R.drawable.ic_media_pause));
		isPlaying = true;
	}

	protected void pause() {
		musicIntent = new Intent();
		musicIntent.setAction("com.paperairplane.music.share.PLAYMUSIC");
		Bundle bundle = new Bundle();
		bundle.putInt("op", PAUSE);
		musicIntent.putExtras(bundle);
		startService(musicIntent);
		btnPP.setBackgroundDrawable(getResources().getDrawable(
				android.R.drawable.ic_media_play));
		isPlaying = false;
	}

	private void initReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.paperairplane.music.share.CurrentTime");
		filter.addAction("com.paperairplane.music.share.MaxTime");
		registerReceiver(positionReceiver, filter);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(positionReceiver);
	}

	private BroadcastReceiver positionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals("com.paperairplane.music.share.CurrentTime")) {
				nowDuration = intent.getExtras().getInt("currentTime");
				// Log.d(DEBUG_TAG, "�յ���ʱ���" + nowDuration);
				seekBar.setProgress(nowDuration);
				rv.setInt(R.id.progressMusic, "setProgress", nowDuration);
			} else if (action.equals("com.paperairplane.music.share.MaxTime")) {
				maxTime = intent.getExtras().getInt("maxTime");
				seekBar.setMax(maxTime);
				rv.setInt(R.id.progressMusic, "setMax", maxTime);
				Log.d(DEBUG_TAG, "����seekBar-Max" + maxTime);
				//FIXME RemoteView���ProgressBar�����˰���������
			}
		}
	};

}
