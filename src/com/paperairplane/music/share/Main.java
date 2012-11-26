package com.paperairplane.music.share;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity {
	// �洢������Ϣ
	private MusicData[] musics;// ������������
	private ListView listview;// �б����
	private MediaPlayer mediaPlayer;
	private RefreshMusicListReceiver receiver;
	private String[] media_info = new String[] { MediaStore.Audio.Media.TITLE,
			MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ARTIST,
			MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM };

	// �Ѿ���
	@Override
	// ����
	public void onCreate(Bundle savedInstanceState) {
		if(Integer.parseInt(android.os.Build.VERSION.SDK) > 9){
			damnHighVersion();
		}
		super.onCreate(savedInstanceState);
		try {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.reset();
			setContentView(R.layout.main);
			listview = (ListView) findViewById(R.id.list);// ��ListView��ID
			listview.setOnItemClickListener(new MusicListOnClickListener());// ����һ��ListView����������
			listview.setEmptyView(findViewById(R.id.empty));
			showMusicList();
		} catch (Exception e) {
			setContentView(R.layout.empty);
		}

	}

	public void btn_empty(View v) {
				refreshMusicList();
	}

	@Override
	// �����˵�
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	// �˵��ж�
	public boolean onOptionsItemSelected(MenuItem menu) {
		super.onOptionsItemSelected(menu);
		switch (menu.getItemId()) {
		case R.id.menu_exit:
			finish();
			System.exit(0);
			break;
		case R.id.menu_about:
			showAbout();
			break;
		case R.id.menu_refresh:
			mediaPlayer.stop();
			mediaPlayer.reset();
			refreshMusicList();
			showMusicList();
			break;
		}
		return true;
	}

	// �Ի�����
	public Dialog onCreateDialog(final int _id) {
		if (_id == R.layout.about) { // ����ǹ��ڴ���
			// �Ի�����������
			// ���ڻ��պϡ���RelativeLayout�Ǹ��ö���
			View about = LayoutInflater.from(this)
					.inflate(R.layout.about, null);
			Button button_about = (Button) about
					.findViewById(R.id.button_about);
			button_about.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					removeDialog(R.layout.about);
				}
			});
			Button button_contact = (Button) about
					.findViewById(R.id.button_contact);
			button_contact.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Uri uri = Uri.parse("http://weibo.com/xavieryao");
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(intent);
				}

			});
			return new AlertDialog.Builder(this).setView(about).create();
		} else if (_id <= 65535) { // ����ǲ�������
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(getString(R.string.choose_an_operation))
					.setPositiveButton(getString(R.string.play),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									playMusic(_id);
								}
							})
					.setNegativeButton(getString(R.string.share),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									shareMusic(_id);
								}
							}).create();
		} else { // ����Ƿ���
			int newid = _id - 65535;
			try {
				mediaPlayer.reset();
				mediaPlayer.setDataSource(musics[newid].getPath());
				mediaPlayer.prepare();
				mediaPlayer.start();
			} catch (Exception e) {
			}
			final View dialogView = LayoutInflater.from(this).inflate(
					R.layout.player, null);
			final TextView tvTitle = (TextView) dialogView
					.findViewById(R.id.text_player_title);
			final TextView tvSinger = (TextView) dialogView
					.findViewById(R.id.text_player_singer);
			tvTitle.setText(musics[newid].getTitle() + "("
					+ musics[newid].getDuration() + ")"
					+ getString(R.string.very_long));
			tvSinger.setText(musics[newid].getArtist()
					+ getString(R.string.very_long));
			final Button btnPP = (Button) dialogView
					.findViewById(R.id.button_player_pause);
			final Button btnRT = (Button) dialogView
					.findViewById(R.id.button_player_return);
			btnPP.setBackgroundDrawable(getResources().getDrawable(
					android.R.drawable.ic_media_pause));
			btnRT.setBackgroundDrawable(getResources().getDrawable(
					android.R.drawable.ic_delete));
			btnPP.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (mediaPlayer.isPlaying() == true) {
						mediaPlayer.pause();
						btnPP.setBackgroundDrawable(getResources().getDrawable(
								android.R.drawable.ic_media_play));
					} else if (mediaPlayer.isPlaying() == false) {
						mediaPlayer.start();
						btnPP.setBackgroundDrawable(getResources().getDrawable(
								android.R.drawable.ic_media_pause));
					}
				}
			});
			btnRT.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					mediaPlayer.stop();
					removeDialog(_id);
				}
			});
			return new AlertDialog.Builder(this).setView(dialogView).create();
		}

	}

	public class MusicListOnClickListener implements OnItemClickListener {
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long id) {

			removeDialog(position);
			showDialog(position);
		}
	}

	// �����б�
	private void showMusicList() {

		Cursor cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, media_info,
				MediaStore.Audio.Media.SIZE + ">='" + 15000 + "'", null,
				MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		cursor.moveToFirst();
		musics = new MusicData[cursor.getCount()];
		for (int i = 0; i < cursor.getCount(); i++) {
			musics[i] = new MusicData();
			musics[i].setTitle(cursor.getString(0));
			musics[i].setDuration(convertDuration(cursor.getInt(1)));
			musics[i].setArtist(cursor.getString(2));
			musics[i].setPath(cursor.getString(3));
			musics[i].setAlbum(cursor.getString(4));
			cursor.moveToNext();
		}
		listview.setAdapter(new MusicListAdapter(this, musics));

	}

	// ת��������Duration
	// ��˵���ܲ����Ż�һ�����,����ͷ�ΰ�
	// ͦ�õģ�����׶�����
	private String convertDuration(int _duration) {
		String duration = "";
		_duration /= 1000;
		String hour = ((Integer) (_duration / 3600)).toString();
		String min = ((Integer) (_duration / 60)).toString();
		String sec = ((Integer) (_duration % 60)).toString();
		if (hour.length() == 1)
			hour = "0" + hour;
		if (hour.equals("0") || hour.equals("00"))
			hour = "";
		if (min.length() == 1)
			min = "0" + min;
		if (sec.length() == 1)
			sec = "0" + sec;
		if (hour.length() != 0)
			duration = hour + ":" + min + ":" + sec;
		if (hour.length() == 0)
			duration = min + ":" + sec;
		return duration;
	}

	// ��������
	private void shareMusic(int position) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
		intent.putExtra(
				Intent.EXTRA_TEXT,
				getString(R.string.music_title) + "����"
						+ musics[position].getTitle() + "��"
						+ getString(R.string.music_artist) + "����"
						+ musics[position].getArtist() + "��"
						+ getString(R.string.music_album) + "����"
						+ musics[position].getAlbum() + "��" 
						+ getString(R.string.music_url) +"��"
						+ getMusicUrl(position) +"��(" 
						+ getString(R.string.share_by) + "��"
						+ getString(R.string.app_name) +" "
						+ getString(R.string.about_download_info)
						+ getString(R.string.url) + ")" );
		startActivity(Intent.createChooser(intent,
				getString(R.string.how_to_share)));
	}

	// ��ȡ���ֵ�ַ
	private String getMusicUrl(int position) {
		
		Log.v("Music Share DEBUG","���� getMusicUrl������,�������Ϊ"+position);
		String json = getJson(position);
		String music_url = null;
        if (json == null){
			music_url = getString(R.string.no_music_url_found);
			Log.v("Music Share DEBUG","���� getMusicUrl��ÿյ�json�ַ���");
        }else{
		try {
			JSONObject rootObject = new JSONObject(json);
            int count = rootObject.getInt("count");
            if(count == 1){
    			JSONArray contentArray = rootObject.getJSONArray("musics");
    			JSONObject item = contentArray.getJSONObject(0);
    			music_url = item.getString("mobile_link");
                     }else{                   
                    music_url = getString(R.string.no_music_url_found);
            }
		} catch (JSONException e) {
			music_url = getString(R.string.no_music_url_found);
		}
        }
		return music_url;

	}

	// ͨ������API��ȡ������Ϣ
	private String getJson(int position) {
		Log.v("Music Share DEBUG","���� getJSON������,�������Ϊ"+position);
		String api_url = "https://api.douban.com/v2/music/search?count=1&q="
				+ java.net.URLEncoder.encode(musics[position].getTitle() + "+" + musics[position].getArtist());
		Log.v("Music Share DEBUG","���� getJSON��Ҫ���е�����Ϊ"+api_url);
		String json = null;
		HttpResponse httpResponse;
		HttpGet httpGet = new HttpGet(api_url);
		try {
			httpResponse = new DefaultHttpClient().execute(httpGet);
			Log.v("Music Share DEBUG","���е�HTTP GET����״̬Ϊ"+httpResponse.getStatusLine().getStatusCode());
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				json = EntityUtils.toString(httpResponse.getEntity());
			} else {
				Toast.makeText(this,getString(R.string.error_internet) , Toast.LENGTH_SHORT).show();				
				json = null;
			}
		} catch (Exception e) {
			Log.v("Music Share DEBUG","�׳�����"+e.getMessage());
			Toast.makeText(this, getString(R.string.error_internet), Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			json = null;
		}
		return json;

	}

	// ��������
	private void playMusic(int position) {
		removeDialog(position + 65535);
		showDialog(position + 65535);
	}

	// ˢ�������б�
	private void refreshMusicList() {
		try {
			IntentFilter filter = new IntentFilter(
					Intent.ACTION_MEDIA_SCANNER_STARTED);
			filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
			filter.addDataScheme("file");
			receiver = new RefreshMusicListReceiver();
			registerReceiver(receiver, filter);
			sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
					Uri.parse("file://"
							+ Environment.getExternalStorageDirectory()
									.getAbsolutePath())));
		} catch (Exception e) {
			setContentView(R.layout.empty);
		}
	}

	private void showAbout() { // ��ʾ���ڴ���
		showDialog(R.layout.about);// ��ô,���ûɶ��,ֻ�Ǹ���ϵͳһ����ʶ,��onCreateDialog�����ж�һ�µ�
	}
	
	@TargetApi(9)
	private void damnHighVersion(){
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().permitAll().penaltyLog().build());
	}
}
/**
 * Paper Airplane Dev Team
 * ����1��@author @HarryChen-���ɳ���15- http://weibo.com/yszzf
 * ����2��@author @Ҧ��Ȼ http://weibo.com/xavieryao 
 * ������@author @��ֻС��1997 http://weibo.com/u/1579617160 2012.11.17
 **/
