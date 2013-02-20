package com.paperairplane.music.share;

import java.io.File;

//import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
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
import android.widget.Toast;
import cn.jpush.android.api.JPushInterface;

import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.sso.SsoHandler;

public class Main extends ListActivity {
	// �洢������Ϣ
	private MusicData[] musics;// ������������
	private ListView listview;// �б����
	private final String[] media_info = new String[] {
			MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION,
			MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA,
			MediaStore.Audio.Media.ALBUM };
	private final int INTERNET_ERROR = 3, SEND_WEIBO = 4, SEND_SUCCEED = 5,
			AUTH_ERROR = 6, SEND_ERROR = 7, NOT_AUTHORIZED_ERROR = 8,
			AUTH_SUCCEED = 9;
	private final int WEIBO = 0, OTHERS = 1;
	private final int DIALOG_SHARE = 0, DIALOG_ABOUT = 1;
	private final String APP_KEY = "1006183120";
	private final String REDIRECT_URI = "https://api.weibo.com/oauth2/default.html";
	public static Oauth2AccessToken accessToken = null;
	private Weibo weibo = Weibo.getInstance(APP_KEY, REDIRECT_URI);
	private final static String DEBUG_TAG = "Music Share DEBUG";
	private Receiver receiver;
	private AlertDialog dialogMain, dialogAbout;
	private SsoHandler ssoHandler;

	@Override
	// ����
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			setContentView(R.layout.main);
			initListView();
			showMusicList();
			ssoHandler=new SsoHandler(Main.this, weibo);
			Log.v(DEBUG_TAG, "Push Start");
//			JPushInterface.setAliasAndTags(getApplicationContext(), "XavierYao",
//					null);
			// ����JPush��Debug��ǩ
			JPushInterface.init(getApplicationContext());
		} catch (Exception e) {
			// Log.e(DEBUG_TAG, e.getMessage());
			e.printStackTrace();
			setContentView(R.layout.empty);
		}
		// ��ȡ�Ѵ洢����Ȩ��Ϣ
		try {
			Main.accessToken = AccessTokenKeeper
					.readAccessToken(getApplicationContext());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initListView() {
		listview = (ListView) findViewById(android.R.id.list);// ��ListView��ID
		listview.setOnItemClickListener(new MusicListOnClickListener());// ����һ��ListView����������
//		listview.setEmptyView(findViewById(R.id.empty));
		View footerView = LayoutInflater.from(this).inflate(R.layout.footer,
				null);
		listview.addFooterView(footerView);
	}

	@Override
	protected void onStop() {
		super.onStop();
		try {
			unregisterReceiver(receiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
            ssoHandler.authorizeCallBack(requestCode, resultCode, data);
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
		case R.id.menu_unauth:
			// �ж��Ƿ�������Ȩ
			try {
				if (Main.accessToken == null) {
					handler.sendEmptyMessage(NOT_AUTHORIZED_ERROR);
				} else {
					Main.accessToken = null;
					AccessTokenKeeper.clear(getApplicationContext());
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

	public void btn_empty(View v) {
		refreshMusicList();
	}

	public void footer(View v) {
		Log.v(DEBUG_TAG, "���footer");
		View search = LayoutInflater.from(this).inflate(R.layout.search, null);
		final EditText et_title = (EditText) search.findViewById(R.id.et_title);
		final EditText et_artist = (EditText) search
				.findViewById(R.id.et_artist);
		final EditText et_album = (EditText) search.findViewById(R.id.et_album);
		Button button_weibo = (Button) search
				.findViewById(R.id.btn_share2weibo);
		button_weibo.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (et_title.getText().toString().trim().equals("")) {
					new AlertDialog.Builder(Main.this)
							.setMessage(getString(R.string.empty))
							.setPositiveButton(getString(android.R.string.ok),
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
										}
									}).show();
				} else {
					shareMusic(et_title.getText().toString(), et_artist
							.getText().toString(), et_album.getText()
							.toString(), WEIBO);
				}
			}
		});
		Button button_others = (Button) search
				.findViewById(R.id.btn_share2others);
		button_others.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (et_title.getText().toString().trim().equals("")) {
					// ���˱���,ֻҪ��Title�͹���~
					new AlertDialog.Builder(Main.this)
							.setMessage(getString(R.string.empty))
							.setPositiveButton(getString(android.R.string.ok),
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
										}
									}).show();
				} else {
					shareMusic(et_title.getText().toString(), et_artist
							.getText().toString(), et_album.getText()
							.toString(), OTHERS);
				}
			}
		});
		new AlertDialog.Builder(this).setView(search).setCancelable(true)
				.show();
	}

	// �Ի�����

	private void showCustomDialog(final int _id, int whichDialog) {
		if (whichDialog == DIALOG_ABOUT) {
			// ��Ȼ��˵������,��,�������ӾͲ�������
			// ��������ʾ���ڴ����Ƿ�����һ���������ûɶ��
			// ������ֻ��˵�����Ǹ�ע���������ȥ�ġ����������ǲ������������ˣ�
			dialogAbout = new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(getString(R.string.menu_about))
					.setMessage(getString(R.string.about_content))
					.setPositiveButton(getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									dialogAbout.cancel();
								}
							})
					.setNegativeButton(getString(R.string.about_contact),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									Uri uri = Uri
											.parse(getString(R.string.url));
									Intent intent = new Intent(
											Intent.ACTION_VIEW, uri);
									startActivity(intent);
								}
							}).show();
		} else if (whichDialog == DIALOG_SHARE) {
			dialogMain = new AlertDialog.Builder(this)
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
									shareMusic(musics[_id].getTitle(),
											musics[_id].getArtist(),
											musics[_id].getAlbum(), OTHERS);
								}
							})
					.setNeutralButton(getString(R.string.share2weibo),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									shareMusic(musics[_id].getTitle(),
											musics[_id].getArtist(),
											musics[_id].getAlbum(), WEIBO);
								}
							}).show();
		} else {
			throw new RuntimeException("What the hell are you doing?");
		}
	}

	// �б���������
	private class MusicListOnClickListener implements OnItemClickListener {
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long id) {
			if (position != listview.getCount()) {
				try {
					dialogMain.cancel();
				} catch (Exception e) {
				}
			}
			showCustomDialog(position, DIALOG_SHARE);
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
	private void shareMusic(String title, String artist, String album, int means) {
		QueryAndShareMusicInfo query = new QueryAndShareMusicInfo(title,
				artist, album, means, getApplicationContext(), handler);
		query.start();
		Toast.makeText(this, getString(R.string.querying), Toast.LENGTH_LONG)
				.show();
	}

	// ��������
	private void playMusic(int position) {
		Intent musicIntent = new Intent();
		musicIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		musicIntent.setAction(android.content.Intent.ACTION_VIEW);
		musicIntent.setDataAndType(
				Uri.fromFile(new File(musics[position].getPath())), "audio/*");
		try {
			startActivity(musicIntent);
		} catch (ActivityNotFoundException e) {
			new AlertDialog.Builder(Main.this)
					.setMessage(getString(R.string.no_player_found))
					.setPositiveButton(getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
								}
							}).show();
		}

	}

	// ˢ�������б�
	private void refreshMusicList() {
		try {
			IntentFilter filter = new IntentFilter(
					Intent.ACTION_MEDIA_SCANNER_STARTED);
			filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
			filter.addDataScheme("file");
			receiver = new Receiver();
			registerReceiver(receiver, filter);
			sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
					Uri.parse("file://"
							+ Environment.getExternalStorageDirectory()
									.getAbsolutePath())));
			showMusicList();// �ҡ��ҿ϶����Ĵθĵ�ʱ���Բа����ɾ��
			// Ȼ������ûûЧ��?
		} catch (Exception e) {
			e.printStackTrace();
			setContentView(R.layout.empty);
		}
	}

	private void showAbout() { // ��ʾ���ڴ���
		showCustomDialog(0, DIALOG_ABOUT);
	}

	// ������Ϣ����
	private Handler handler = new Handler() {
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
				// Log.v(DEBUG_TAG, artworkUrl);
				final WeiboHelper weiboHelper = new WeiboHelper(handler,
						getApplicationContext());
				et.setText(_content);
				et.setSelection(_content.length());
				new AlertDialog.Builder(Main.this)
						.setView(sendweibo)
						.setPositiveButton(getString(R.string.share),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										String content = et.getText()
												.toString();

										if (Main.accessToken == null
												|| (Main.accessToken
														.isSessionValid() == false)) {// ���֮ǰ�Ƿ���Ȩ��
											handler.sendEmptyMessage(NOT_AUTHORIZED_ERROR);
											saveSendStatus(content,
													cb.isChecked(), artworkUrl);
											ssoHandler.authorize(
													weiboHelper.getListener());// ��Ȩ
										} else {
											weiboHelper.sendWeibo(content,
													artworkUrl, cb.isChecked());
										}

									}

								}).show();
				Log.v(DEBUG_TAG, "�����Ի���");
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

	private void saveSendStatus(String content, boolean checked,
			String artworkUrl) {
		SharedPreferences preferences = getApplicationContext()
				.getSharedPreferences("ShareStatus", Context.MODE_PRIVATE);
		preferences.edit().putString("content", content).commit();
		preferences.edit().putBoolean("willFollow", checked).commit();
		preferences.edit().putString("artworkUrl", artworkUrl).commit();

	}

}
/**
 * Paper Airplane Dev Team 
 * ���ң�@author @HarryChen-SIGKILL- http://weibo.com/yszzf
 * ���ң�@author @Ҧ��Ȼ http://weibo.com/xavieryao 
 * ������@author @��ֻС��1997 http://weibo.com/u/1579617160 
 * Code Version 0030 
 * 2013.2.17 RTM
 * P.S.��ʦ����������Ź���ζ������
 * P.P.S.û�Թ�����ȷ�е�˵������Ҳû�������� ���У��ҵĺ�����ҵ��������������
 **/
