package com.paperairplane.music.share;

import java.io.File;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import cn.jpush.android.api.JPushInterface;

import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.sso.SsoHandler;

public class Main extends ListActivity {
	// �洢������Ϣ
	private MusicData[] musics;// ������������
	private ListView listview;// �б����
	public static Oauth2AccessToken accessToken = null;
	private Weibo weibo = Weibo
			.getInstance(Consts.APP_KEY, Consts.REDIRECT_URI);
	private Receiver receiver;
	private AlertDialog dialogMain, dialogAbout, dialogSearch;
	private SsoHandler ssoHandler;

	@Override
	// ����
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			setContentView(R.layout.main);
			initListView();
			showMusicList();
			ssoHandler = new SsoHandler(Main.this, weibo);
			Log.v(Consts.DEBUG_TAG, "Push Start");
			// this.getResources().updateConfiguration(conf, null);
			// JPushInterface.setAliasAndTags(getApplicationContext(),
			// "XavierYao",
			// null);
			// ����JPush��Debug��ǩ
			JPushInterface.init(getApplicationContext());
		} catch (Exception e) {
			// Log.e(Consts.DEBUG_TAG, e.getMessage());
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
		// listview.setEmptyView(findViewById(R.id.empty));
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
					handler.sendEmptyMessage(Consts.Status.NOT_AUTHORIZED_ERROR);
				} else {
					new AlertDialog.Builder(this)
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setTitle(R.string.unauth_confirm)
							.setPositiveButton(android.R.string.ok,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface arg0, int arg1) {
											Main.accessToken = null;
											AccessTokenKeeper
													.clear(getApplicationContext());
											Toast.makeText(
													Main.this,
													getString(R.string.unauthed),
													Toast.LENGTH_SHORT).show();
										}
									})
							.setNegativeButton(android.R.string.cancel,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface arg0, int arg1) {
										}
									}).show();

				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.v(Consts.DEBUG_TAG, e.getMessage());
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
		showCustomDialog(0, Consts.Dialogs.SEARCH);
	}

	// �Ի�����

	private void showCustomDialog(final int _id, int whichDialog) {
		switch (whichDialog) {
		case Consts.Dialogs.ABOUT:
			DialogInterface.OnClickListener listenerAbout = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					switch (whichButton) {
					case DialogInterface.BUTTON_POSITIVE:
						dialogAbout.cancel();
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						Uri uri = Uri.parse(getString(R.string.url));
						Intent intent = new Intent(Intent.ACTION_VIEW, uri);
						startActivity(intent);
						break;
					case DialogInterface.BUTTON_NEUTRAL:
						View feedback = LayoutInflater.from(Main.this).inflate(
								R.layout.feedback, null);
						final EditText content = (EditText) feedback
								.findViewById(R.id.et_feedback);
						new AlertDialog.Builder(Main.this)
								.setView(feedback)
								.setPositiveButton(R.string.feedback,
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												String contentString = content
														.getText().toString()
														.trim();
												if (contentString.equals("")) {
													showCustomDialog(
															0,
															Consts.Dialogs.EMPTY);
												} else {
													SendFeedback feedback = new SendFeedback(
															contentString,
															handler);
													feedback.start();
												}
											}
										}).show();
						break;
					}
				}
			};
			// ��Ȼ��˵������,��,�������ӾͲ�������
			// ��������ʾ���ڴ����Ƿ�����һ���������ûɶ��
			// ������ֻ��˵�����Ǹ�ע���������ȥ�ġ����������ǲ������������ˣ�
			dialogAbout = new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(getString(R.string.menu_about))
					.setMessage(getString(R.string.about_content))
					.setPositiveButton(android.R.string.ok, listenerAbout)
					.setNegativeButton(R.string.about_contact, listenerAbout)
					.setNeutralButton(R.string.feedback, listenerAbout).show();
			break;
		case Consts.Dialogs.SHARE:
			View musicInfoView = getMusicInfoView(_id);
			DialogInterface.OnClickListener listenerMain = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					switch (whichButton) {
					case DialogInterface.BUTTON_POSITIVE:
						shareMusic(musics[_id].getTitle(),
								musics[_id].getArtist(),
								musics[_id].getAlbum(), Consts.ShareMeans.WEIBO);
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						shareMusic(musics[_id].getTitle(),
								musics[_id].getArtist(),
								musics[_id].getAlbum(),
								Consts.ShareMeans.OTHERS);
						break;
					}
				}
			};
			dialogMain = new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.choose_an_operation)
					.setView(musicInfoView)
					.setNegativeButton(R.string.share2others, listenerMain)
					.setPositiveButton(R.string.share2weibo, listenerMain)
					.show();
			break;
		case Consts.Dialogs.SEARCH:
			Log.v(Consts.DEBUG_TAG, "���footer");
			View search = LayoutInflater.from(this).inflate(R.layout.search,
					null);
			final EditText et_title = (EditText) search
					.findViewById(R.id.et_title);
			final EditText et_artist = (EditText) search
					.findViewById(R.id.et_artist);
			final EditText et_album = (EditText) search
					.findViewById(R.id.et_album);
			Button button_weibo = (Button) search
					.findViewById(R.id.btn_share2weibo);
			OnClickListener listenerButton = new OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (v.getId()) {
					case R.id.btn_share2weibo:
						if (et_title.getText().toString().trim().equals("")) {
							showCustomDialog(0, Consts.Dialogs.EMPTY);
						} else {
							shareMusic(et_title.getText().toString(), et_artist
									.getText().toString(), et_album.getText()
									.toString(), Consts.ShareMeans.WEIBO);
							dialogSearch.cancel();
						}
						break;
					case R.id.btn_share2others:
						if (et_title.getText().toString().trim().equals("")) {
						} else {
							shareMusic(et_title.getText().toString(), et_artist
									.getText().toString(), et_album.getText()
									.toString(), Consts.ShareMeans.OTHERS);
							dialogSearch.cancel();
						}
						break;
					}
				}
			};
			button_weibo.setOnClickListener(listenerButton);
			Button button_others = (Button) search
					.findViewById(R.id.btn_share2others);
			button_others.setOnClickListener(listenerButton);
			dialogSearch = new AlertDialog.Builder(this).setView(search)
					.setCancelable(true).create();
			dialogSearch.show();
			break;
		case Consts.Dialogs.EMPTY:
			new AlertDialog.Builder(Main.this)
					.setMessage(getString(R.string.empty))
					.setPositiveButton(getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
								}
							}).show();
			break;
		default:
			throw new RuntimeException("What the hell are you doing?");
		}
	}

	private View getMusicInfoView(final int _id) {
		View musicInfo = LayoutInflater.from(this).inflate(R.layout.music_info,
				null);
		ImageButton albumArt = (ImageButton) musicInfo
				.findViewById(R.id.image_music);
		TextView textTitle = (TextView) musicInfo.findViewById(R.id.text_title);
		TextView textArtist = (TextView) musicInfo
				.findViewById(R.id.text_artist);
		TextView textAlbum = (TextView) musicInfo.findViewById(R.id.text_album);
		TextView textDuration = (TextView) musicInfo
				.findViewById(R.id.text_duration);
		textTitle.setText(getString(R.string.title) + " : "
				+ musics[_id].getTitle());
		textArtist.setText(getString(R.string.artist) + " : "
				+ musics[_id].getArtist());
		textAlbum.setText(getString(R.string.album) + " : "
				+ musics[_id].getAlbum());
		textDuration.setText(getString(R.string.duration) + ":"
				+ musics[_id].getDuration());
		Bitmap bmpAlbum = Utilities.getLocalArtwork(Main.this,
				musics[_id].getAlbumId(), 100, 100);
		try {
			Log.d(Consts.DEBUG_TAG, bmpAlbum.toString());
			albumArt.setImageBitmap(bmpAlbum);
			Log.d(Consts.DEBUG_TAG, "Oh Oh Oh Yeah!!");
		} catch (NullPointerException e) {
			e.printStackTrace();
			Log.d(Consts.DEBUG_TAG, "Oh shit, we got null again");
		}
		albumArt.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				playMusic(_id);
			}
		});
		return musicInfo;
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
			showCustomDialog(position, Consts.Dialogs.SHARE);
		}
	}

	// �����б�
	private void showMusicList() {

		Cursor cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Consts.MEDIA_INFO,
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
			musics[i].setAlbumId(cursor.getLong(5));
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
		showCustomDialog(0, Consts.Dialogs.ABOUT);
	}

	// ������Ϣ����
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Consts.Status.INTERNET_ERROR:// �������
				Toast.makeText(getApplicationContext(),
						getString(R.string.error_internet), Toast.LENGTH_SHORT)
						.show();
				break;
			case Consts.Status.SEND_WEIBO:// ����΢��
				View sendweibo = LayoutInflater.from(getApplicationContext())
						.inflate(R.layout.sendweibo, null);
				final EditText et = (EditText) sendweibo.getRootView()
						.findViewById(R.id.et_content);
				final CheckBox cb = (CheckBox) sendweibo
						.findViewById(R.id.cb_follow);
				Bundle bundle = (Bundle) msg.obj;
				String _content = bundle.getString("content");
				final String artworkUrl = bundle.getString("artworkUrl");
				// Log.v(Consts.DEBUG_TAG, artworkUrl);
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
											handler.sendEmptyMessage(Consts.Status.NOT_AUTHORIZED_ERROR);
											saveSendStatus(content,
													cb.isChecked(), artworkUrl);
											ssoHandler.authorize(weiboHelper
													.getListener());// ��Ȩ
										} else {
											weiboHelper.sendWeibo(content,
													artworkUrl, cb.isChecked());
										}

									}

								}).show();
				Log.v(Consts.DEBUG_TAG, "�����Ի���");
				break;
			case Consts.Status.SEND_SUCCEED:// ���ͳɹ�
				Toast.makeText(Main.this, R.string.send_succeed,
						Toast.LENGTH_SHORT).show();
				break;
			case Consts.Status.NOT_AUTHORIZED_ERROR:// ��δ��Ȩ
				Toast.makeText(Main.this, R.string.not_authorized_error,
						Toast.LENGTH_SHORT).show();
				break;
			case Consts.Status.AUTH_ERROR:// ��Ȩ����
				Toast.makeText(Main.this,
						R.string.auth_error + (String) msg.obj,
						Toast.LENGTH_SHORT).show();
				Log.e(Consts.DEBUG_TAG, "����" + (String) msg.obj);
				break;
			case Consts.Status.SEND_ERROR:// ���ʹ���
				Toast.makeText(Main.this,
						R.string.send_error + (String) msg.obj,
						Toast.LENGTH_SHORT).show();
				Log.e(Consts.DEBUG_TAG, "����" + (String) msg.obj);
				break;
			case Consts.Status.AUTH_SUCCEED:// ��Ȩ�ɹ�
				Toast.makeText(Main.this, R.string.auth_succeed,
						Toast.LENGTH_SHORT).show();
				break;
			case Consts.Status.FEEDBACK_SUCCEED:
				Toast.makeText(Main.this, R.string.feedback_succeed,
						Toast.LENGTH_LONG).show();
				break;
			case Consts.Status.FEEDBACK_FAIL:
				Toast.makeText(Main.this, R.string.feedback_failed,
						Toast.LENGTH_LONG).show();
				SharedPreferences preferences = getApplicationContext()
						.getSharedPreferences(Consts.Preferences.FEEDBACK,
								Context.MODE_PRIVATE);
				preferences.edit().putString("content", (String) msg.obj)
						.commit();
				// TODO ����һ��������Ҫ����
				break;
			}
		}
	};

	private void saveSendStatus(String content, boolean checked,
			String artworkUrl) {
		SharedPreferences preferences = getApplicationContext()
				.getSharedPreferences(Consts.Preferences.SHARE,
						Context.MODE_PRIVATE);
		preferences.edit().putString("content", content).commit();
		preferences.edit().putBoolean("willFollow", checked).commit();
		preferences.edit().putString("artworkUrl", artworkUrl).commit();

	}

}
/**
 * Paper Airplane Dev Team ���ң�@author @HarryChen-SIGKILL- http://weibo.com/yszzf
 * ���ң�@author @Ҧ��Ȼ http://weibo.com/xavieryao ������@author @��ֻС��1997
 * http://weibo.com/u/1579617160 Code Version 0030 2013.2.17 RTM
 * P.S.��ʦ����������Ź���ζ������ P.P.S.û�Թ�����ȷ�е�˵������Ҳû�������� ���У��ҵĺ�����ҵ��������������
 **/
