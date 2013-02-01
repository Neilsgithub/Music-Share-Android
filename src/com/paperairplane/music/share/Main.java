package com.paperairplane.music.share;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import cn.jpush.android.api.JPushInterface;

import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.WeiboAuthListener;
import com.weibo.sdk.android.WeiboDialogError;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.WeiboParameters;
import com.weibo.sdk.android.api.StatusesAPI;
import com.weibo.sdk.android.net.AsyncWeiboRunner;
import com.weibo.sdk.android.net.RequestListener;

public class Main extends Activity {
	// �洢������Ϣ
	private MusicData[] musics;// ������������
	private ListView listview;// �б����
	private Intent musicIntent;
	private String[] media_info = new String[] { MediaStore.Audio.Media.TITLE,
			MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ARTIST,
			MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM };
	private int PLAY = 0, PAUSE = 1, STOP = 2, nowPlaying;
	final private int INTERNET_ERROR = 3, SEND_WEIBO = 4, SEND_SUCCEED = 5,
			AUTH_ERROR = 6, SEND_ERROR = 7, NOT_AUTHORIZED_ERROR = 8,
			AUTH_SUCCEED = 9;
	final private int WEIBO = 10, OTHERS = 11;
	final private int HARRY_UID = 1689129907, XAVIER_UID = 2121014783,
			APP_UID = 1153267341;
	final private int MUSIC = 0, ARTWORK = 1;
	private boolean isPlaying = false;
	private final String APP_KEY = "1006183120";
	private final String REDIRECT_URI = "https://api.weibo.com/oauth2/default.html";
	public static Oauth2AccessToken accessToken = null;
	private Weibo weibo = Weibo.getInstance(APP_KEY, REDIRECT_URI);
	// fatal error����˭������
	private final static String DEBUG_TAG = "Music Share DEBUG";
	private StatusesAPI api = null;

	@Override
	// ����
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			setContentView(R.layout.main);
			listview = (ListView) findViewById(R.id.list);// ��ListView��ID
			listview.setOnItemClickListener(new MusicListOnClickListener());// ����һ��ListView����������
			listview.setEmptyView(findViewById(R.id.empty));
			View footerView = LayoutInflater.from(this).inflate(
					R.layout.footer, null);
			listview.addFooterView(footerView);
			showMusicList();
			Log.v(DEBUG_TAG, "Push Start");
			JPushInterface.init(getApplicationContext());
		} catch (Exception e) {
			Log.e(DEBUG_TAG, e.getMessage());
			e.printStackTrace();
			setContentView(R.layout.empty);
		}
		// ��ȡ�Ѵ洢����Ȩ��Ϣ
		try {
			Main.accessToken = AccessTokenKeeper.readAccessToken(this);
		} catch (Exception e) {
			e.printStackTrace();
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
			musicIntent = new Intent();
			musicIntent.setAction("com.paperairplane.music.share.PLAYMUSIC");
			stopService(musicIntent);
			finish();
			System.exit(0);
			break;
		case R.id.menu_about:
			showAbout();
			break;
		case R.id.menu_unauth:
			// �ж��Ƿ�������Ȩ
			try {
				if (Main.accessToken == null) {
					handler.sendEmptyMessage(NOT_AUTHORIZED_ERROR);
				} else {
					Main.accessToken = null;
					AccessTokenKeeper.clear(Main.this);
					Toast.makeText(Main.this, getString(R.string.unauthed),
							Toast.LENGTH_SHORT).show();
				}
			} catch (Exception e) {
				Log.v(DEBUG_TAG, e.getMessage());
			}
			break;
		case R.id.menu_refresh:
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
					Uri uri = Uri.parse(getString(R.string.url));
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
					.setNegativeButton(getString(R.string.share2others),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									shareMusic(musics[_id].getTitle(), musics[_id].getArtist(), musics[_id].getAlbum(), OTHERS);
								}
							})
					.setNeutralButton(getString(R.string.share2weibo),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									shareMusic(musics[_id].getTitle(), musics[_id].getArtist(), musics[_id].getAlbum(), WEIBO);

								}
							}).create();
		} else {
			int newid = _id - 65535;
			if (!isPlaying || nowPlaying != newid) {
				try {
					musicIntent = new Intent();
					musicIntent
							.setAction("com.paperairplane.music.share.PLAYMUSIC");
					Bundle bundle = new Bundle();
					bundle.putString("path", musics[newid].getPath());
					bundle.putInt("op", PLAY);
					musicIntent.putExtras(bundle);
					startService(musicIntent);
					isPlaying = true;
					nowPlaying = newid;
				} catch (Exception e) {
				}
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
					if (isPlaying == true) {
						musicIntent = new Intent();
						musicIntent
								.setAction("com.paperairplane.music.share.PLAYMUSIC");
						Bundle bundle = new Bundle();
						bundle.putInt("op", PAUSE);
						musicIntent.putExtras(bundle);
						startService(musicIntent);
						btnPP.setBackgroundDrawable(getResources().getDrawable(
								android.R.drawable.ic_media_play));
						isPlaying = false;
					} else if (isPlaying == false) {
						musicIntent = new Intent();
						musicIntent
								.setAction("com.paperairplane.music.share.PLAYMUSIC");
						Bundle bundle = new Bundle();
						bundle.putInt("op", PLAY);
						musicIntent.putExtras(bundle);
						startService(musicIntent);
						// mediaPlayer.start();
						btnPP.setBackgroundDrawable(getResources().getDrawable(
								android.R.drawable.ic_media_pause));
						isPlaying = true;
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
					removeDialog(_id);
					isPlaying = false;
				}
			});
			return new AlertDialog.Builder(this).setView(dialogView)
					.setCancelable(true).show();
		}

	}

	public void footer(View v) {
		Log.v(DEBUG_TAG, "���");
		View search = LayoutInflater.from(this).inflate(
				R.layout.search, null);
		final EditText et_title = (EditText)search.findViewById(R.id.et_title);
		final EditText et_artist = (EditText)search.findViewById(R.id.et_artist);
		final EditText et_album = (EditText)search.findViewById(R.id.et_album);
		Button button_weibo = (Button) search.findViewById(R.id.btn_share2weibo);
		button_weibo.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(et_title.getText().toString().trim().equals("")&&et_artist.getText().toString().trim().equals("")){
					new AlertDialog.Builder(Main.this)
					.setMessage(
							getString(R.string.empty))
					.setPositiveButton(
							getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										DialogInterface dialog,
										int which) {
								}
							}).show();
				}else{
					shareMusic(et_title.getText().toString(), et_artist.getText().toString(), et_album.getText().toString(), WEIBO);
				}
			}
		});
		Button button_others = (Button) search.findViewById(R.id.btn_share2others);
		button_others.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(et_title.getText().toString().trim().equals("")&&et_artist.getText().toString().trim().equals("")){
					new AlertDialog.Builder(Main.this)
					.setMessage(
							getString(R.string.empty))
					.setPositiveButton(
							getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										DialogInterface dialog,
										int which) {
								}
							}).show();
				}else{
					shareMusic(et_title.getText().toString(), et_artist.getText().toString(), et_album.getText().toString(), OTHERS);
				}
			}
		});
		new AlertDialog.Builder(this).setView(search).setCancelable(true)
				.show();
	}

	// �б���������
	public class MusicListOnClickListener implements OnItemClickListener {
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long id) {
			if (position != listview.getCount()) {

				try {
					dismissDialog(position);
				} catch (Exception e) {
				}
				showDialog(position);
			}
		}
	}

	// �����б�
	private void showMusicList() {

		Cursor cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, media_info,
				MediaStore.Audio.Media.DURATION + ">='" + 30000 + "'", null,
				MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		// ����С��30s������
		cursor.moveToFirst();
		musics = new MusicData[cursor.getCount()];
		for (int i = 0; i < cursor.getCount(); i++) {
			musics[i] = new MusicData();
			musics[i].setTitle(cursor.getString(0));
			musics[i].setDuration(Utilities.convertDuration(cursor.getInt(1)));
			musics[i].setArtist(cursor.getString(2));
			musics[i].setPath(cursor.getString(3));
			musics[i].setAlbum(cursor.getString(4));
			cursor.moveToNext();
		}
		listview.setAdapter(new MusicListAdapter(this, musics));

	}

	// ��������
	private void shareMusic(String title,String artist,String album, int means) {
		QueryAndShareMusicInfo query = new QueryAndShareMusicInfo(title, artist,album, means);
		query.start();
		Toast.makeText(this, getString(R.string.querying), Toast.LENGTH_LONG)
				.show();
	}

	// ��������
	private void playMusic(int position) {
		try {
			dismissDialog(position + 65535);
		} catch (Exception e) {
		}
		showDialog(position + 65535);
	}

	// ˢ�������б�
	private void refreshMusicList() {
		try {
			IntentFilter filter = new IntentFilter(
					Intent.ACTION_MEDIA_SCANNER_STARTED);
			filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
			filter.addDataScheme("file");
			Receiver receiver = new Receiver();
			registerReceiver(receiver, filter);
			sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
					Uri.parse("file://"
							+ Environment.getExternalStorageDirectory()
									.getAbsolutePath())));
		} catch (Exception e) {
			e.printStackTrace();
			setContentView(R.layout.empty);
		}
	}

	private void showAbout() { // ��ʾ���ڴ���
		showDialog(R.layout.about);// ��ô,���ûɶ��,ֻ�Ǹ���ϵͳһ����ʶ,��onCreateDialog�����ж�һ�µ�
	}

	// ��ѯ+�����߳�
	class QueryAndShareMusicInfo extends Thread {
		private int means;
		private String artist, title, album;

		public void run() {
			// ��ȡ��Ϣ�����ַ���
			String urls[] = getMusicAndArtworkUrl(title, artist);
			String content = getString(R.string.share_by) + artist
					+ getString(R.string.music_artist) + title
					+ getString(R.string.music_album) + album + "  "
					+ urls[MUSIC];
			String artworkUrl = null;
			artworkUrl = urls[ARTWORK].replace("spic", "lpic");
			Bundle bundle = new Bundle();
			bundle.putString("content", content);
			bundle.putString("artworkUrl", artworkUrl);
			Log.e(DEBUG_TAG, "��ȡ������");
			switch (means) {
			// ���ݷ���ʽִ�в���
			case OTHERS:
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_SUBJECT,
						getString(R.string.app_name));
				intent.putExtra(Intent.EXTRA_TEXT, content);
				startActivity(Intent.createChooser(intent,
						getString(R.string.how_to_share)));
				break;
			case WEIBO:
				Message m = handler.obtainMessage(SEND_WEIBO, bundle);
				handler.sendMessage(m);
				break;
			}

		}

		// ��ȡ���ֵ�ַ�Լ�ר�������ַ,�����ϲ�
		private String[] getMusicAndArtworkUrl(String title, String artist) {
			Log.v(DEBUG_TAG, "���� getMusicAndArtworkUrl������");
			String json = getJson(title, artist);
			String urls[] = new String[2];
			if (json == null) {
				urls[MUSIC] = getString(R.string.no_music_url_found);
				Log.v(DEBUG_TAG, "���� getMusicAndArtworkUrl��ÿյ�json�ַ���");
			} else {
				try {
					JSONObject rootObject = new JSONObject(json);
					int count = rootObject.getInt("count");
					if (count == 1) {
						JSONArray contentArray = rootObject
								.getJSONArray("musics");
						JSONObject item = contentArray.getJSONObject(0);
						urls[MUSIC] = item.getString("mobile_link");
						urls[ARTWORK] = item.getString("image");
					} else {
						urls[MUSIC] = getString(R.string.no_music_url_found);
						urls[ARTWORK] = null;
					}
				} catch (JSONException e) {
					Log.e(DEBUG_TAG, "JSON��������");
					e.printStackTrace();
					urls[MUSIC] = getString(R.string.no_music_url_found);
					urls[ARTWORK] = null;
				}
			}
			Log.v(DEBUG_TAG, urls[MUSIC]);
			Log.v(DEBUG_TAG, urls[ARTWORK]);
			return urls;
		}

		// ͨ������API��ȡ������Ϣ
		private String getJson(String title, String artist) {
			Log.v(DEBUG_TAG, "���� getJSON������");
			String api_url = "http://paperairplane.sinaapp.com/proxy.php?q="
					+ java.net.URLEncoder.encode(title + "+" + artist);
			Log.v(DEBUG_TAG, "���� getJSON��Ҫ���е�����Ϊ" + api_url);
			String json = null;
			HttpResponse httpResponse;
			HttpGet httpGet = new HttpGet(api_url);
			try {
				httpResponse = new DefaultHttpClient().execute(httpGet);
				Log.v(DEBUG_TAG, "���е�HTTP GET����״̬Ϊ"
						+ httpResponse.getStatusLine().getStatusCode());
				if (httpResponse.getStatusLine().getStatusCode() == 200) {
					json = EntityUtils.toString(httpResponse.getEntity());
					Log.v(DEBUG_TAG, "���ؽ��Ϊ" + json);
				} else {
					handler.sendEmptyMessage(INTERNET_ERROR);
					json = null;
				}
			} catch (Exception e) {
				Log.v(DEBUG_TAG, "�׳�����" + e.getMessage());
				handler.sendEmptyMessage(INTERNET_ERROR);
				e.printStackTrace();
				json = null;
			}
			return json;

		}

		public QueryAndShareMusicInfo(String _title, String _artist,
				String _album, int _means) {
			title = _title;
			artist = _artist;
			album = _album;
			means = _means;
		}
	}

	// ������Ϣ����
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case INTERNET_ERROR:// �������
				Toast.makeText(getApplicationContext(),
						getString(R.string.error_internet), Toast.LENGTH_SHORT)
						.show();
				break;
			case SEND_WEIBO:// ����΢��
				View sendweibo = LayoutInflater.from(getApplicationContext())
						.inflate(R.layout.sendweibo, null);
				final EditText et = (EditText) sendweibo.getRootView()
						.findViewById(R.id.et_content);
				final CheckBox cb = (CheckBox) sendweibo
						.findViewById(R.id.cb_follow);
				Bundle bundle = (Bundle) msg.obj;
				String _content = bundle.getString("content");
				final String artworkUrl = bundle.getString("artworkUrl");
				Log.v(DEBUG_TAG, artworkUrl);
				et.setText(_content);
				new AlertDialog.Builder(Main.this)
						.setView(sendweibo)
						.setPositiveButton(getString(R.string.share),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										String content = et.getText()
												.toString();
										if (Utilities.calculateLength(content) > 140) {// �ж������Ƿ񳬹�140
											Log.v(DEBUG_TAG, "��������");
											new AlertDialog.Builder(Main.this)
													.setMessage(
															getString(R.string.too_long))
													.setPositiveButton(
															getString(android.R.string.ok),
															new DialogInterface.OnClickListener() {
																@Override
																public void onClick(
																		DialogInterface dialog,
																		int which) {
																}
															}).show();
										} else if (Main.accessToken == null
												|| (Main.accessToken
														.isSessionValid() == false)) {// ���֮ǰ�Ƿ���Ȩ��
											handler.sendEmptyMessage(NOT_AUTHORIZED_ERROR);
											SharedPreferences preferences = getApplicationContext()
													.getSharedPreferences(
															"ShareStatus",
															Context.MODE_PRIVATE);
											preferences
													.edit()
													.putString("content",
															content).commit();
											preferences
													.edit()
													.putBoolean("willFollow",
															cb.isChecked())
													.commit();
											preferences
													.edit()
													.putString("artworkUrl",
															artworkUrl)
													.commit();
											weibo.authorize(Main.this,
													new AuthDialogListener());// ��Ȩ
										} else {
											sendWeibo(content, artworkUrl,
													cb.isChecked());
										}

									}
								}).show();
				Log.e(DEBUG_TAG, "�����Ի���");
				break;
			case SEND_SUCCEED:// ���ͳɹ�
				Toast.makeText(Main.this, R.string.send_succeed,
						Toast.LENGTH_SHORT).show();
				break;
			case NOT_AUTHORIZED_ERROR:// ��δ��Ȩ
				Toast.makeText(Main.this, R.string.not_authorized_error,
						Toast.LENGTH_SHORT).show();
				break;
			case AUTH_ERROR:// ��Ȩ����
				Toast.makeText(Main.this,
						R.string.auth_error + (String) msg.obj,
						Toast.LENGTH_SHORT).show();
				Log.e(DEBUG_TAG, "����" + (String) msg.obj);
				break;
			case SEND_ERROR:// ���ʹ���
				Toast.makeText(Main.this,
						R.string.send_error + (String) msg.obj,
						Toast.LENGTH_SHORT).show();
				Log.e(DEBUG_TAG, "����" + (String) msg.obj);
				break;
			case AUTH_SUCCEED:// ��Ȩ�ɹ�
				Toast.makeText(Main.this, R.string.auth_succeed,
						Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

	// ����΢��
	private void sendWeibo(String content, String fileDir, boolean willFollow) {
		api = new StatusesAPI(Main.accessToken);
		if (fileDir == null) {
			Log.v(DEBUG_TAG, "������ͼ΢��");
			api.update(content, null, null, requestListener);
		} else {
			Log.v(DEBUG_TAG, "���ʹ�ͼ΢����url=" + fileDir);
			String url = "https://api.weibo.com/2/statuses/upload_url_text.json";
			WeiboParameters params = new WeiboParameters();
			params.add("access_token", Main.accessToken.getToken());
			params.add("status", content);
			params.add("url", fileDir);
			AsyncWeiboRunner.request(url, params, "POST", requestListener);
		}
		if (willFollow == true) {// �ж��Ƿ�Ҫ��ע������
			follow(HARRY_UID);// ��עHarry Chen
			follow(XAVIER_UID);// ��עXavier Yao
			follow(APP_UID);// ��ע�ٷ�΢��
		}
	}

	// ��עĳ��
	private void follow(int uid) {
		WeiboParameters params = new WeiboParameters();
		params.add("access_token", Main.accessToken.getToken());
		params.add("uid", uid);
		String url = "https://api.weibo.com/2/friendships/create.json";
		try {
			AsyncWeiboRunner.request(url, params, "POST",
					new RequestListener() {
						@Override
						public void onComplete(String arg0) {
							Log.v("Music Share DUBUG", "followed");
						}

						@Override
						public void onError(WeiboException arg0) {
						}

						@Override
						public void onIOException(IOException arg0) {
						}
					});
		} catch (Exception e) {
		}
		// ��Ȼ��ע�����ĵؽ��в�������
	}

	// ΢����Ȩ������
	class AuthDialogListener implements WeiboAuthListener {
		Message m = handler.obtainMessage();

		@Override
		public void onComplete(Bundle values) {
			String token = values.getString("access_token");
			String expires_in = values.getString("expires_in");
			Main.accessToken = new Oauth2AccessToken(token, expires_in);
			AccessTokenKeeper.keepAccessToken(Main.this, accessToken);
			handler.sendEmptyMessage(AUTH_SUCCEED);
			Log.v(DEBUG_TAG, "��Ȩ�ɹ���\n AccessToken:" + token);
			SharedPreferences preferences = getApplicationContext()
					.getSharedPreferences("ShareStatus", Context.MODE_PRIVATE);
			String content = preferences.getString("content", null);
			String artworkUrl = preferences.getString("artworkUrl", null);
			boolean willFollow = preferences.getBoolean("willFollow", false);
			Log.v(DEBUG_TAG, "��ȡ״̬\n" + content + "\n" + artworkUrl + "\n"
					+ willFollow);
			sendWeibo(content, artworkUrl, willFollow);
		}

		@Override
		public void onCancel() {

		}

		@Override
		public void onError(WeiboDialogError e) {
			String error = e.getMessage();
			m.what = AUTH_ERROR;
			m.obj = error;
			handler.sendMessage(m);
		}

		@Override
		public void onWeiboException(WeiboException e) {
			String error = e.getMessage();
			m.what = AUTH_ERROR;
			m.obj = error;
			handler.sendMessage(m);
		}
	}

	private RequestListener requestListener = new RequestListener() {
		Message m = handler.obtainMessage();

		@Override
		public void onComplete(String arg0) {
			handler.sendEmptyMessage(SEND_SUCCEED);
		}

		@Override
		public void onError(WeiboException e) {
			String error = e.getMessage();
			m.what = SEND_ERROR;
			m.obj = error;
			handler.sendMessage(m);
		}

		@Override
		public void onIOException(IOException arg0) {
			String error = arg0.getMessage();
			m.what = SEND_ERROR;
			m.obj = error;
			handler.sendMessage(m);
		}

	};

}
/**
 * Paper Airplane Dev Team ����1��@author @HarryChen-SIGKILL-
 * http://weibo.com/yszzf ����2��@author @Ҧ��Ȼ http://weibo.com/xavieryao ������@author @��ֻС��1997
 * http://weibo.com/u/1579617160 Code Version 0015 2013.1.30
 * p.s.������ǰ���߲�֪��Bug�ܲ��ܸ��ꡭ��
 **/
