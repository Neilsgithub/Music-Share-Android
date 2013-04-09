package com.paperairplane.music.share;

import java.io.File;
import java.util.Locale;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.paperairplane.music.share.ShakeDetector.OnShakeListener;
import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.sso.SsoHandler;

public class Main extends ListActivity {
	// �洢������Ϣ
	private MusicData[] musics;// ������������
	private ListView listview;// �б����
	public static Oauth2AccessToken accessToken = null;
	private Weibo weibo = Weibo.getInstance(Consts.APP_KEY,
			Consts.Url.AUTH_REDIRECT);
	private Receiver receiver;
	private AlertDialog dialogMain, dialogAbout, dialogSearch, dialogThank,
			dialogWelcome, dialogChangeColor, dialogSendWeibo,
			dialogBackgroundChooser;
	private SsoHandler ssoHandler;
	private WeiboHelper weiboHelper;
	private TextView indexOverlay;
	private static int versionCode, checkForUpdateCount = 0;
	private String versionName;
	private ImageView iv;
	private ShakeDetector shakeDetector;
	private String background_path = null;
	private SharedPreferences theme_preferences;

	@Override
	// ����
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		theme_preferences = getApplicationContext().getSharedPreferences(
				Consts.Preferences.GENERAL, Context.MODE_PRIVATE);
		initListView();
		showMusicList();
		firstShow();
		ssoHandler = new SsoHandler(Main.this, weibo);
		weiboHelper = new WeiboHelper(handler, getApplicationContext());
		try {
			Main.versionCode = getPackageManager().getPackageInfo(
					getPackageName(), 0).versionCode;
			this.versionName = getPackageManager().getPackageInfo(
					getPackageName(), 0).versionName;
		} catch (Exception e) {
			e.printStackTrace();
		}
		// ��ȡ�Ѵ洢����Ȩ��Ϣ
		Main.accessToken = weiboHelper.readAccessToken();
		Utilities.checkForUpdate(Main.versionCode, handler, Main.this,
				getResources().getConfiguration().locale);
		setBackground();
		initShakeDetector();
	}

	/**
	 * һ���ԲеĹ���==
	 */
	private void initShakeDetector() {
		shakeDetector = new ShakeDetector(Main.this);
		shakeDetector.shakeThreshold = 2000;// �����������
		shakeDetector.registerOnShakeListener(new OnShakeListener() {
			@Override
			public void onShake() {
				Log.d(Consts.DEBUG_TAG, "��⵽ҡ��");
				int position = 0;
				Random r = new Random();
				position = r.nextInt(listview.getAdapter().getCount());
				Log.d(Consts.DEBUG_TAG, "���������" + position);
				indexOverlay.setVisibility(View.INVISIBLE);
				Toast.makeText(Main.this, R.string.shake_random,
						Toast.LENGTH_LONG).show();
				showCustomDialog(position, Consts.Dialogs.SHARE);
			}
		});
		shakeDetector.start();
	}

	/**
	 * @param void
	 * @return void
	 * @author Xavier Yao ��ʼ��������ListView������ԣ���ʼ����������
	 * 
	 */
	private void initListView() {
		/*
		 * ���ð�ť����ʱ��Ч��
		 */
		iv = (ImageView) findViewById(R.id.float_search_button);
		iv.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					iv.setImageResource(R.drawable.search_button_pressed);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					iv.setImageResource(R.drawable.search_button_normal);
				}
				return false;
			}
		});
		/*
		 * ��ʼ��Overlay����SharedPreferences���ȡOverlay�ı���ɫ
		 */
		indexOverlay = (TextView) View.inflate(Main.this, R.layout.indexer,
				null);
		showOverlay();
		SharedPreferences preference = getSharedPreferences(
				Consts.Preferences.GENERAL, MODE_PRIVATE);
		if (preference.contains(Consts.Preferences.BG_COLOR)) {
			indexOverlay.setBackgroundColor(android.graphics.Color
					.parseColor(preference.getString(
							Consts.Preferences.BG_COLOR, "")));
		}
		/*
		 * ��ʼ��ListView
		 */
		listview = (ListView) findViewById(android.R.id.list);// ��LisView��ID
		listview.setOnItemClickListener(new MusicListOnClickListener());// ����һ��ListView����������
		listview.setOnScrollListener(new OnScrollListener() {
			boolean visible;

			@SuppressLint("NewApi")
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if (visible) {
					// ����ΪOverlay�������֡�ʶ��ڴ�the,a,an
					String firstChar = musics[firstVisibleItem].getTitle();
					if (firstChar.toLowerCase(Locale.getDefault()).startsWith(
							"the ")) {
						firstChar = firstChar.substring(4, 5);
					} else if (firstChar.toLowerCase(Locale.getDefault())
							.startsWith("a ")) {
						firstChar = firstChar.substring(2, 3);
					} else if (firstChar.toLowerCase(Locale.getDefault())
							.startsWith("an ")) {
						firstChar = firstChar.substring(3, 4);
					} else {
						firstChar = firstChar.substring(0, 1);
					}
					indexOverlay.setText(firstChar.toUpperCase(Locale
							.getDefault()));
					indexOverlay.setVisibility(View.VISIBLE);
				}
				if (firstVisibleItem == 0
						|| (firstVisibleItem + visibleItemCount) == totalItemCount) {
					indexOverlay.setVisibility(View.INVISIBLE);
				}
				if ((firstVisibleItem + visibleItemCount) >= (totalItemCount - 3)
						&& visibleItemCount < totalItemCount) {
				}
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				visible = true;
				if (scrollState == ListView.OnScrollListener.SCROLL_STATE_IDLE) {
					indexOverlay.setVisibility(View.INVISIBLE);
				}
			}

		});

	}
	@SuppressWarnings("deprecation")
	private void setBackground(){
		background_path = theme_preferences.getString(Consts.Preferences.BG_PATH, null);
		Log.d(Consts.DEBUG_TAG,"��ȡ���ĵ�ַ"+background_path);
		View main_layout = findViewById(R.id.main_linearLayout);
		if (background_path == null){
		main_layout.setBackgroundResource(
				R.drawable.background_holo_dark);
		Log.d(Consts.DEBUG_TAG,"����ΪĬ�ϱ�ֽ");
		}else{
			main_layout.setBackgroundDrawable(Drawable.createFromPath(background_path));
			Log.d(Consts.DEBUG_TAG, "����Ϊ�Զ���ֽ"+background_path);
		}
	}

	/**
	 * ��ʾOverlay�ķ��������indexOverlay���View
	 */
	private void showOverlay() {
		try {
			getWindowManager()
					.addView(
							indexOverlay,
							new WindowManager.LayoutParams(
									LayoutParams.WRAP_CONTENT,
									LayoutParams.WRAP_CONTENT,
									WindowManager.LayoutParams.TYPE_APPLICATION,
									WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
											| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
									PixelFormat.TRANSLUCENT));
		} catch (IllegalStateException e) {
			Log.e(Consts.DEBUG_TAG, "Overlay Exception");
		}
	}

	@Override
	protected void onStop() {
		Log.d(Consts.DEBUG_TAG, "onStop()");
		// ����ǰOverlay����ʾ״̬���浽SharedPreferences
		if (indexOverlay.getVisibility() == View.VISIBLE) {
			getWindowManager().removeView(indexOverlay);
			SharedPreferences pref = getSharedPreferences(
					Consts.Preferences.OVERLAY, MODE_PRIVATE);
			Editor edit = pref.edit();
			edit.putBoolean("resume", true);
			edit.commit();
		}
		// �ر�ҡ�����
		shakeDetector.stop();
		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// �ָ�ҡ�����
		shakeDetector.start();
		// ��ȡ���ָ�Overlay�Ŀɼ���
		SharedPreferences pref = getSharedPreferences(
				Consts.Preferences.OVERLAY, MODE_PRIVATE);
		boolean resume = pref.getBoolean("resume", false);
		if (resume) {
			showOverlay();
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// �����жϽ��յ���Intent������AtSuggestion����΢��SSO��Ȩ
		if (requestCode == Consts.LOOK_FOR_SUGGESTION_REQUEST_CODE) {
			Log.d(Consts.DEBUG_TAG, "����");
			// �������bundle����������dialogSendWeibo
			dialogSendWeibo.dismiss();
			Message m = handler.obtainMessage(Consts.Status.SEND_WEIBO);
			m.obj = data.getExtras();
			m.sendToTarget();
		} else if (requestCode == Consts.PICK_BACKGROUND_REQUEST_CODE
				&& resultCode == RESULT_OK && null != data) {
			Uri selectedImage = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			background_path = cursor.getString(columnIndex);
			cursor.close();
			Log.d(Consts.DEBUG_TAG, "ȡ���ı�����ַ��" + background_path);
			showCustomDialog(0, Consts.Dialogs.CHANGE_BACKGROUND);

		} else {
			ssoHandler.authorizeCallBack(requestCode, resultCode, data);
		}
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		getMenuInflater().inflate(R.menu.main, menu);
		SubMenu submenu = menu.addSubMenu(Menu.NONE, Menu.NONE, 3,
				R.string.menu_customize).setIcon(
				android.R.drawable.ic_menu_manage);
		getMenuInflater().inflate(R.menu.customize, submenu);
		if (Build.VERSION.SDK_INT >= 11) {
			menu.add(Menu.NONE, Consts.MenuItem.REFRESH, 1,
					R.string.menu_refresh)
					.setIcon(android.R.drawable.ic_popup_sync)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		} else {
			menu.add(Menu.NONE, Consts.MenuItem.REFRESH, 1,
					R.string.menu_refresh).setIcon(
					android.R.drawable.ic_menu_recent_history);
		}
		if (!isAccessTokenExistAndValid()) {
			menu.add(Menu.NONE, Consts.MenuItem.AUTH, 2, R.string.auth)
					.setIcon(android.R.drawable.ic_menu_add);
		} else {
			menu.add(Menu.NONE, Consts.MenuItem.UNAUTH, 2, R.string.unauth)
					.setIcon(android.R.drawable.ic_menu_delete);
		}
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
		case R.id.menu_change_color:
			showCustomDialog(0, Consts.Dialogs.CHANGE_COLOR);
			break;
		case R.id.menu_clean_cache:
			String ARTWORK_PATH = getCacheDir().getAbsolutePath()
					+ "/.artworkCache/";
			int fileCount = 0;
			try {
				File[] files = new File(ARTWORK_PATH).listFiles();
				fileCount = files.length;
				for (File f : files) {
					f.delete();
					Log.v(Consts.DEBUG_TAG, f.getName() + " deleted.");
					// ��Ȼ����������for�������ܲ�����������������ô��ҷ�����for-each!
					// Effective Java�������for-each����
				}
			} catch (Exception e) {
				// e.printStackTrace();
				Log.e(Consts.DEBUG_TAG, "Exception: NO FILE deleted.");
				// �ʴ�һ�㣬��ɫ��������������������
			}
			String toastText = getString(R.string.clean_cache_done) + "\n"
					+ getString(R.string.delete_file_count) + fileCount;
			Toast.makeText(Main.this, toastText, Toast.LENGTH_LONG).show();
			break;
		case Consts.MenuItem.UNAUTH:
			try {
				new AlertDialog.Builder(this)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setMessage(R.string.unauth_confirm)
						.setTitle(R.string.unauth)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									@SuppressLint("NewApi")
									@Override
									public void onClick(DialogInterface arg0,
											int arg1) {
										Main.accessToken = null;
										weiboHelper.clear();
										if (Build.VERSION.SDK_INT > 10) {
											invalidateOptionsMenu();
										}
										Toast.makeText(Main.this,
												getString(R.string.unauthed),
												Toast.LENGTH_SHORT).show();
									}
								})
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface arg0,
											int arg1) {
									}
								}).show();

			} catch (Exception e) {
				e.printStackTrace();
				Log.v(Consts.DEBUG_TAG, e.getMessage());
			}
			break;
		case Consts.MenuItem.AUTH:
			try {
				ssoHandler.authorize(weiboHelper.getListener());
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case Consts.MenuItem.REFRESH:
			refreshMusicList();
			showMusicList();
			break;
		case R.id.menu_update:
			Main.checkForUpdateCount++;
			Utilities.checkForUpdate(Main.versionCode, handler, Main.this,
					getResources().getConfiguration().locale);
			break;
		case R.id.menu_change_background:
			showCustomDialog(0, Consts.Dialogs.CHANGE_BACKGROUND);
			break;
		}
		return true;
	}

	/**
	 * @param View
	 *            v
	 * @return void ��������ʾΪ��ʱ��ť�������
	 * 
	 */
	public void btn_empty(View v) {
		refreshMusicList();
	}

	/**
	 * 
	 * @return ���AccessToken�Ĵ��ڼ��Ϸ���
	 */
	private boolean isAccessTokenExistAndValid() {
		boolean flag = true;
		if (Main.accessToken.isSessionValid() == false) {
			flag = false;
		}
		Log.d(Consts.DEBUG_TAG, "����isAccessTokenExistAndValid()������,���" + flag);
		return flag;
	}

	/**
	 * @param View
	 *            v
	 * @return void ������������ť�������
	 * 
	 */
	public void footer(View v) {
		showCustomDialog(0, Consts.Dialogs.SEARCH);
	}

	// �Ի�����

	/**
	 * @param int _id �����������������id������Ϊ0
	 * @param int whichDialog ����Consts.Dialog����ı���ж���ʲô�Ի���
	 * @return void
	 * @author Harry Chen ��ʾ����ĸ����Զ���Ի��򣬰���dialogMain, dialogAbout, dialogSearch,
	 *         dialogThank, dialogWelcome, dialogChangeColor
	 * 
	 */
	private void showCustomDialog(final int _id, int whichDialog) {
		shakeDetector.stop();
		final DialogInterface.OnCancelListener onCancelListener = new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				try {
					shakeDetector.start();
				} catch (Throwable t) {

				}
			}
		};
		switch (whichDialog) {
		case Consts.Dialogs.ABOUT:
			DialogInterface.OnClickListener listenerAbout = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					switch (whichButton) {
					case DialogInterface.BUTTON_POSITIVE:
						dialogThank = new AlertDialog.Builder(Main.this)
								.setOnCancelListener(onCancelListener)
								.setTitle(R.string.thank_title)
								.setIcon(android.R.drawable.ic_dialog_info)
								.setMessage(R.string.thank_content)
								.setPositiveButton(android.R.string.ok,
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface dialog,
													int whichButton) {
												dialogThank.cancel();
												dialogAbout.show();
											}
										}).create();
						dialogThank.show();
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
						SharedPreferences pref = getSharedPreferences(
								Consts.Preferences.FEEDBACK, MODE_PRIVATE);
						String text = pref.getString("content", "");
						content.setText(text);
						pref.edit().clear().commit();
						final AlertDialog.Builder builder = new AlertDialog.Builder(
								Main.this);
						DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								if (whichButton==DialogInterface.BUTTON_NEUTRAL){
									//dialog.cancel();
									//builder.create().show();
									//��Ȼ��������...
									//FIXME �Բۡ���Exception�׵ûţ���ô������������ʧ��
									return;
								}
								String contentString = content.getText()
										.toString().trim();
								if (contentString.equals("")) {
									showCustomDialog(0, Consts.Dialogs.EMPTY);
								} else {
									String versionCode = "NameNotFoundException";
									versionCode = Integer
											.toString(Main.versionCode);
									SendFeedback feedback = new SendFeedback(
											contentString, handler,
											versionCode, Main.this);
									switch (whichButton) {
									case DialogInterface.BUTTON_POSITIVE:
										feedback.setMeans(Consts.ShareMeans.OTHERS);
										feedback.start();
										break;
									case DialogInterface.BUTTON_NEGATIVE:
										feedback.setMeans(Consts.ShareMeans.WEIBO);
										feedback.start();
										break;
									}
								}
							}
						};

						builder.setView(feedback)
								.setPositiveButton(R.string.send_feedback,
										listener)
								.setNeutralButton(R.string.reset, listener)
								.setTitle(R.string.thank_for_feedback)
								.setIcon(android.R.drawable.ic_dialog_info)
								.setOnCancelListener(onCancelListener);
						if (isAccessTokenExistAndValid()) {
							builder.setNegativeButton(R.string.feedback_weibo,
									listener);
						}
						builder.show();
						break;
					}
				}
			};
			dialogAbout = new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(getString(R.string.menu_about))
					.setOnCancelListener(onCancelListener)
					.setMessage(
							getString(R.string.about_content) + "\n\n"
									+ Consts.RELEASE_DATE + "\nVer "
									+ versionName + " / " + versionCode + "\n"
									+ getString(R.string.update_whats_new)
									+ Consts.WHATSNEW)
					.setPositiveButton(R.string.thank_list, listenerAbout)
					.setNegativeButton(R.string.about_contact, listenerAbout)
					.setNeutralButton(R.string.send_feedback, listenerAbout)
					.create();
			dialogAbout.show();
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
								musics[_id].getAlbum(),
								musics[_id].getAlbumId(),
								Consts.ShareMeans.WEIBO);
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						shareMusic(musics[_id].getTitle(),
								musics[_id].getArtist(),
								musics[_id].getAlbum(),
								musics[_id].getAlbumId(),
								Consts.ShareMeans.OTHERS);
						break;
					case DialogInterface.BUTTON_NEUTRAL:
						sendFile(musics[_id]);
						break;
					}
				}
			};
			dialogMain = new AlertDialog.Builder(this)
					.setOnCancelListener(onCancelListener)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.choose_an_operation)
					.setView(musicInfoView)
					.setNegativeButton(R.string.share2others, listenerMain)
					.setPositiveButton(R.string.share2weibo, listenerMain)
					.setNeutralButton(R.string.send_file, listenerMain).show();
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
			DialogInterface.OnClickListener listenerSearch = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					switch (whichButton) {
					case DialogInterface.BUTTON_POSITIVE:
						if (et_title.getText().toString().trim().equals("")) {
							showCustomDialog(0, Consts.Dialogs.EMPTY);
							
						} else {
							shareMusic(et_title.getText().toString(), et_artist
									.getText().toString(), et_album.getText()
									.toString(), Consts.NULL,
									Consts.ShareMeans.WEIBO);
							dialogSearch.cancel();
						}
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						if (et_title.getText().toString().trim().equals("")) {
							showCustomDialog(0, Consts.Dialogs.EMPTY);
						} else {
							shareMusic(et_title.getText().toString(), et_artist
									.getText().toString(), et_album.getText()
									.toString(), Consts.NULL,
									Consts.ShareMeans.OTHERS);
							dialogSearch.cancel();
						}
						break;
					}
				}
			};
			dialogSearch = new AlertDialog.Builder(this).setView(search)
					.setCancelable(true).setOnCancelListener(onCancelListener)
					.setPositiveButton(R.string.share2weibo, listenerSearch)
					.setNegativeButton(R.string.share2others, listenerSearch)
					.setTitle(R.string.search)
					.setIcon(android.R.drawable.ic_dialog_info)
					.create();
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

		case Consts.Dialogs.CHANGE_COLOR:
			View changeColor = View.inflate(Main.this, R.layout.color_chooser,
					null);
			final SeekBar seekColor[] = new SeekBar[4];
			final TextView textColor[] = new TextView[4];
			final TextView textColorCode = (TextView) changeColor
					.findViewById(R.id.text_color);
			final TextView textShowColor = (TextView) changeColor
					.findViewById(R.id.text_show_color);
			seekColor[Consts.Color.RED] = (SeekBar) changeColor
					.findViewById(R.id.seek_red);
			seekColor[Consts.Color.GREEN] = (SeekBar) changeColor
					.findViewById(R.id.seek_green);
			seekColor[Consts.Color.BLUE] = (SeekBar) changeColor
					.findViewById(R.id.seek_blue);
			seekColor[Consts.Color.OPACITY] = (SeekBar) changeColor
					.findViewById(R.id.seek_trans);
			textColor[Consts.Color.RED] = (TextView) changeColor
					.findViewById(R.id.text_red);
			textColor[Consts.Color.GREEN] = (TextView) changeColor
					.findViewById(R.id.text_green);
			textColor[Consts.Color.BLUE] = (TextView) changeColor
					.findViewById(R.id.text_blue);
			textColor[Consts.Color.OPACITY] = (TextView) changeColor
					.findViewById(R.id.text_trans);

			OnSeekBarChangeListener seekListener = new OnSeekBarChangeListener() {

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					switch (seekBar.getId()) {
					case R.id.seek_red:
						textColor[Consts.Color.RED]
								.setText(getString(R.string.red) + ":"
										+ progress);
						break;
					case R.id.seek_green:
						textColor[Consts.Color.GREEN]
								.setText(getString(R.string.green) + ":"
										+ progress);
						break;
					case R.id.seek_blue:
						textColor[Consts.Color.BLUE]
								.setText(getString(R.string.blue) + ":"
										+ progress);
						break;
					case R.id.seek_trans:
						textColor[Consts.Color.OPACITY]
								.setText(getString(R.string.opacity) + ":"
										+ progress * 100 / 255 + "%");
						break;
					}
					changeColor();

				}

				private void changeColor() {
					String color[] = new String[4];
					color[Consts.Color.RED] = Integer
							.toHexString(seekColor[Consts.Color.RED]
									.getProgress());
					color[Consts.Color.GREEN] = Integer
							.toHexString(seekColor[Consts.Color.GREEN]
									.getProgress());
					color[Consts.Color.BLUE] = Integer
							.toHexString(seekColor[Consts.Color.BLUE]
									.getProgress());
					color[Consts.Color.OPACITY] = Integer
							.toHexString(seekColor[Consts.Color.OPACITY]
									.getProgress());
					for (int i = 0; i < 4; i++) {
						if (color[i].length() == 1)
							color[i] = "0" + color[i];
					}
					String hexColor = ("#" + color[Consts.Color.OPACITY]
							+ color[Consts.Color.RED]
							+ color[Consts.Color.GREEN] + color[Consts.Color.BLUE])
							.toUpperCase(Locale.getDefault());
					// Log.d(Consts.DEBUG_TAG, "Color: "+hexColor);
					textColorCode.setText(hexColor);
					textShowColor.setBackgroundColor(android.graphics.Color
							.parseColor(hexColor));
				}
			};
			for (int i = 0; i < 4; i++) {
				seekColor[i].setOnSeekBarChangeListener(seekListener);
			}
			String nowColor;
			if (theme_preferences.contains(Consts.Preferences.BG_COLOR)) {
				nowColor = theme_preferences.getString(
						Consts.Preferences.BG_COLOR, "");
				Log.d(Consts.DEBUG_TAG, "Got origin color");
			}
			else{
				nowColor=Consts.ORIGIN_COLOR;
			}
				int colorInt[] = new int[4];
				colorInt[Consts.Color.RED] = Integer.valueOf(
						nowColor.substring(3, 5), 16);
				colorInt[Consts.Color.GREEN] = Integer.valueOf(
						nowColor.substring(5, 7), 16);
				colorInt[Consts.Color.BLUE] = Integer.valueOf(
						nowColor.substring(7, 9), 16);
				colorInt[Consts.Color.OPACITY] = Integer.valueOf(
						nowColor.substring(1, 3), 16);
				Log.d(Consts.DEBUG_TAG, "Integers are: " + colorInt[0] + " "
						+ colorInt[1] + " " + colorInt[2] + " " + colorInt[3]);
				for (int i = 0; i < 4; i++) {
					seekColor[i].setProgress(colorInt[i]);
				}

			DialogInterface.OnClickListener listenerColor = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					switch (whichButton) {
					case DialogInterface.BUTTON_POSITIVE:
						String color = textColorCode.getText().toString();
						if (color.contains("#")) {
							theme_preferences
									.edit()
									.putString(Consts.Preferences.BG_COLOR,
											color).commit();
							indexOverlay
									.setBackgroundColor(android.graphics.Color
											.parseColor(color));
							Log.d(Consts.DEBUG_TAG, "�Զ�����ɫ:" + color);
						}
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						dialogChangeColor.cancel();
						break;
					case DialogInterface.BUTTON_NEUTRAL:
						theme_preferences
								.edit()
								.putString(Consts.Preferences.BG_COLOR,
										Consts.ORIGIN_COLOR).commit();
						indexOverlay.setBackgroundColor(android.graphics.Color
								.parseColor(Consts.ORIGIN_COLOR));
						dialogChangeColor.cancel();
						break;
					}
				}
			};
			dialogChangeColor = new AlertDialog.Builder(Main.this)
					.setOnCancelListener(onCancelListener).setView(changeColor)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.change_overlay_color)
					.setPositiveButton(android.R.string.ok, listenerColor)
					.setNegativeButton(android.R.string.cancel, listenerColor)
					.setNeutralButton(R.string.reset, listenerColor).create();
			dialogChangeColor.show();
			break;
		case Consts.Dialogs.CHANGE_BACKGROUND:
			View v = View.inflate(Main.this, R.layout.background_chooser, null);
			final ImageView iv_background = (ImageView) v
					.findViewById(R.id.imageView_background);
			if (background_path != null) {
				Drawable background = Drawable.createFromPath(background_path);
				BitmapDrawable bd = (BitmapDrawable) background;
				Bitmap bm = bd.getBitmap();
				iv_background.setImageBitmap(bm);
			}
			DialogInterface.OnClickListener listenerBackground = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						if (background_path != null) {
							theme_preferences.edit()
									.putString(Consts.Preferences.BG_PATH,
											background_path).commit();
						}
						setBackground();
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						Intent i = new Intent(
								Intent.ACTION_PICK,
								android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
						startActivityForResult(i,
								Consts.PICK_BACKGROUND_REQUEST_CODE);
						break;
					case DialogInterface.BUTTON_NEUTRAL:
						background_path = null;
						iv_background
								.setImageResource(R.drawable.background_holo_dark);
						theme_preferences.edit().remove(Consts.Preferences.BG_PATH).commit();
						setBackground();
						break;
					}

				}
			};
			dialogBackgroundChooser = new AlertDialog.Builder(Main.this)
					.setOnCancelListener(onCancelListener)
					.setView(v)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.menu_change_background)
					.setPositiveButton(android.R.string.ok, listenerBackground)
					.setNegativeButton(R.string.choose_picture,
							listenerBackground)
					.setNeutralButton(R.string.choose_default,
							listenerBackground).create();
			dialogBackgroundChooser.show();
			break;
		default:
			throw new RuntimeException("What the hell are you doing?");
		}
	}

	/**
	 * @param int _id �����������������λ��id
	 * @return View ���ڳ�ʼ���Ի����View
	 * @author Harry Chen ����dialogMain����ʾ������Ϣ
	 * 
	 */
	private View getMusicInfoView(final int _id) {
		View musicInfo = LayoutInflater.from(this).inflate(R.layout.music_info,
				null);
		ImageView albumArt = (ImageView) musicInfo
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
		textDuration.setText(getString(R.string.duration) + " : "
				+ musics[_id].getDuration());
		int size = Utilities.getAdaptedSize(Main.this);
		Bitmap bmpAlbum = Utilities.getLocalArtwork(Main.this,
				musics[_id].getAlbumId(), size, size);
		try {
			Log.d(Consts.DEBUG_TAG, "width:" + bmpAlbum.getWidth());
			albumArt.setImageBitmap(bmpAlbum);
			Log.d(Consts.DEBUG_TAG, "Oh Oh Oh Yeah!!");
		} catch (NullPointerException e) {
			e.printStackTrace();
			Log.v(Consts.DEBUG_TAG,
					"Oh shit, we got null again ...... Don't panic");
		}
		albumArt.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				playMusic(_id);
			}
		});
		// Log.d(Consts.DEBUG_TAG,"view:"+
		// albumArt.getHeight()+","+albumArt.getWidth());
		return musicInfo;
	}

	/**
	 * @author Xavier Yao ��������߳���Ϣ
	 * 
	 */
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
				shakeDetector.stop();
				View sendweibo = LayoutInflater.from(getApplicationContext())
						.inflate(R.layout.sendweibo, null);
				final EditText et = (EditText) sendweibo.getRootView()
						.findViewById(R.id.et_content);
				final CheckBox cb = (CheckBox) sendweibo
						.findViewById(R.id.cb_follow);
				final Bundle bundle = (Bundle) msg.obj;
				String _content = bundle.getString("content");
				final String artworkUrl = bundle.getString("artworkUrl");
				final String fileName = bundle.getString("fileName");
				int selection = bundle.getInt("selection", _content.length());
				// Log.v(Consts.DEBUG_TAG, artworkUrl);
				cb.setChecked(bundle.getBoolean("isChecked", true));
				et.setText(_content);
				et.setSelection(selection);
				et.addTextChangedListener(new TextWatcher() {
					@Override
					public void afterTextChanged(Editable arg0) {
					}

					@Override
					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
						try {
							if (s.toString().charAt(start) == '@') {
								Log.d(Consts.DEBUG_TAG, "@ CATCHED!"); // @����
								Intent i = new Intent(Main.this,
										AtSuggestionActivity.class);
								bundle.putString("content", s.toString());
								bundle.putBoolean("isChecked", cb.isChecked());
								bundle.putInt("start", start);
								i.putExtras(bundle);
								startActivityForResult(i,
										Consts.LOOK_FOR_SUGGESTION_REQUEST_CODE);
							}
						} catch (Exception e) {

						}
					}
				});
				dialogSendWeibo = new AlertDialog.Builder(Main.this)
						.setView(sendweibo)
						.setOnCancelListener(
								new DialogInterface.OnCancelListener() {
									@Override
									public void onCancel(DialogInterface dialog) {
										shakeDetector.start();
									}
								})
						.setPositiveButton(getString(R.string.share),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										String content = et.getText()
												.toString();

										if (!isAccessTokenExistAndValid()) {// ���֮ǰ�Ƿ���Ȩ��
											handler.sendEmptyMessage(Consts.Status.NOT_AUTHORIZED_ERROR);
											saveSendStatus(content,
													cb.isChecked(), artworkUrl,
													fileName);
											ssoHandler.authorize(weiboHelper
													.getListener());// ��Ȩ
										} else {
											weiboHelper.sendWeibo(content,
													artworkUrl, fileName,
													cb.isChecked());
										}

									}

								}).show();
				Log.v(Consts.DEBUG_TAG, "����΢���༭�Ի���");
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
						Toast.LENGTH_LONG).show();
				Log.e(Consts.DEBUG_TAG, "��Ȩ����" + (String) msg.obj);
				break;
			case Consts.Status.SEND_ERROR:// ���ʹ���
				Toast.makeText(Main.this,
						R.string.send_error + (String) msg.obj,
						Toast.LENGTH_LONG).show();
				Log.e(Consts.DEBUG_TAG, "���ʹ���" + (String) msg.obj);
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
				break;
			case Consts.Status.NO_UPDATE:
				Toast toast = Toast.makeText(Main.this, R.string.no_update,
						Toast.LENGTH_LONG);
				if (Main.checkForUpdateCount != 0) {
					toast.show();
				}
				break;
			case Consts.Status.HAS_UPDATE:
				updateApp((String[]) msg.obj);
				break;
			case Consts.Status.REFRESH_LIST_FINISHED:
				try {
					unregisterReceiver(receiver);
				} catch (Throwable t) {

				}
				break;
			}

		}
	};

	/**
	 * @author Xavier Yao �б���������
	 * 
	 */
	private class MusicListOnClickListener implements OnItemClickListener {
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long id) {
			// if (position != listview.getCount()) {
			try {
				dialogMain.cancel();
			} catch (Exception e) {
			}
			// } ע�͵��Ĵ����������ǰ��footer�õ�
			indexOverlay.setVisibility(View.INVISIBLE);
			showCustomDialog(position, Consts.Dialogs.SHARE);
		}
	}

	/**
	 * @param void
	 * @return void
	 * @author Xavier Yao ��ʼ���õ���������Ϣ���飬����������ListView
	 * 
	 */
	private void showMusicList() {

		Cursor cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				Consts.MEDIA_INFO,
				MediaStore.Audio.Media.DURATION + ">='" + 30000 + "' AND "
						+ MediaStore.Audio.Media.MIME_TYPE + "<>'audio/amr'",
				// ������Ҳ���õ����ҵ�¼��!
				null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		// ����С��30s������
		cursor.moveToFirst();
		musics = new MusicData[cursor.getCount()];
		for (int i = 0; i < cursor.getCount(); i++) {
			musics[i] = new MusicData();
			musics[i].setTitle(cursor.getString(0).trim());
			musics[i].setDuration(Utilities.convertDuration(cursor.getInt(1)));
			musics[i].setArtist(cursor.getString(2).trim());
			musics[i].setPath(cursor.getString(3));
			musics[i].setAlbum(cursor.getString(4).trim());
			musics[i].setAlbumId(cursor.getLong(5));
			musics[i].setType(cursor.getString(6));
			cursor.moveToNext();
		}
		listview.setAdapter(new MusicListAdapter(this, musics));
		cursor.close();

	}

	/**
	 * @param String
	 *            title ���ֱ���
	 * @param String
	 *            artist����������
	 * @param String
	 *            album ����ר����
	 * @param long album_id ����ר������ID
	 * @param int means ������ͼ��Դ��Consts.ShareMeans
	 * @return void
	 * @author Xavier Yao �������ֵ�����������������QueryAndShareMusicInfo��
	 * 
	 */
	private void shareMusic(String title, String artist, String album,
			long album_id, int means) {
		QueryAndShareMusicInfo query = new QueryAndShareMusicInfo(title,
				artist, album, album_id, means, getApplicationContext(),
				handler);
		query.start();
		Toast.makeText(this, getString(R.string.querying), Toast.LENGTH_LONG)
				.show();
	}

	/**
	 * @param int position ��������Ϣ�����е�λ��
	 * @return void
	 * @author Xavier Yao �������ֵ���������
	 * 
	 */
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
					.setMessage(getString(R.string.no_app_found))
					.setPositiveButton(getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
								}
							}).show();
		}

	}

	/**
	 * @param void
	 * @return void
	 * @author Xavier Yao ˢ�������б�
	 * 
	 */
	private void refreshMusicList() {
		try {
			IntentFilter filter = new IntentFilter(
					Intent.ACTION_MEDIA_SCANNER_STARTED);
			filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
			filter.addDataScheme("file");
			receiver = new Receiver(handler);
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

	/**
	 * @param void
	 * @return void
	 * @author Harry Chen ��ʾ���ڴ���
	 * 
	 */
	private void showAbout() {
		showCustomDialog(0, Consts.Dialogs.ABOUT);
	}

	/**
	 * @author Xavier Yao
	 * @param String
	 *            content ΢������
	 * @param boolean checked �Ƿ��ע������
	 * @param String
	 *            artworkUrl ΢��ͼƬ��ַ
	 * @param String
	 *            fileName ͼƬ�ļ���
	 * @return void ����΢���Լ�����״̬������
	 */
	private void saveSendStatus(String content, boolean checked,
			String artworkUrl, String fileName) {
		SharedPreferences preferences = getApplicationContext()
				.getSharedPreferences(Consts.Preferences.SHARE,
						Context.MODE_PRIVATE);
		preferences.edit().putBoolean("read", true).commit();
		preferences.edit().putString("content", content).commit();
		preferences.edit().putBoolean("willFollow", checked).commit();
		preferences.edit().putString("artworkUrl", artworkUrl).commit();
		preferences.edit().putString("fileName", fileName);

	}

	/**
	 * @author Harry Chen
	 * @param String
	 *            path ����·��
	 * @return void ���������ļ���ͨ������App
	 */
	private void sendFile(MusicData music) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_SEND);
		intent.setType(music.getType());
		// ��Ȼ�Ǹ��ö�������������������޹صģ����ڿ���Ҫ��Ҫ�Ļ�ȥ��
		intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(music.getPath())));
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			new AlertDialog.Builder(Main.this)
					.setMessage(getString(R.string.no_app_found))
					.setPositiveButton(getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
								}
							}).show();
		}
	}

	/**
	 * @author Xavier Yao
	 * @return void
	 * @param String
	 *            [] info���صĸ��ָ�����Ϣ ͨ�����صĸ�����Ϣ��ʾ�Ի������û������Ƿ���³���
	 */
	private void updateApp(final String[] info) {
		new AlertDialog.Builder(Main.this)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(R.string.update_found)
				.setMessage(info[Consts.ArraySubscript.UPDATE_INFO])
				.setPositiveButton(R.string.update_download,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								Uri uri = Uri
										.parse(info[Consts.ArraySubscript.DOWNLOAD_URL]);
								Intent intent = new Intent(Intent.ACTION_VIEW,
										uri);
								startActivity(intent);
							}
						})
				.setNegativeButton(R.string.update_view,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								Uri uri = Uri
										.parse("market://details?id=com.paperairplane.music.share");
								Intent intent = new Intent(Intent.ACTION_VIEW,
										uri);
								try {
									startActivity(intent);
								} catch (ActivityNotFoundException e) {
									e.printStackTrace();
									Toast.makeText(
											getApplicationContext(),
											getString(R.string.update_no_market_found),
											Toast.LENGTH_SHORT).show();
								}
							}
						}).show();

	}

	/**
	 * @author Harry Chen
	 * @param void
	 * @return void �ж��Ƿ��״���������ʾ��ӭ�Ի���
	 */
	private void firstShow() {
		SharedPreferences preferences = getApplicationContext()
				.getSharedPreferences(Consts.Preferences.GENERAL,
						Context.MODE_PRIVATE);
		if (!preferences.getBoolean("hasFirstStarted", false)) {
			Log.d(Consts.DEBUG_TAG, "�״�����");
			dialogWelcome = new AlertDialog.Builder(Main.this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.welcome_title)
					.setMessage(
							getString(R.string.welcome_content)
									+ getString(R.string.update_whats_new)
									+ Consts.WHATSNEW + "\n\nP.S.���԰棬����ÿ�ζ���ʾ")
					.setPositiveButton(R.string.welcome_button,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
									dialogWelcome.cancel();
								}
							}).create();
			Log.v(Consts.DEBUG_TAG, "�״������Ի����ѳ�ʼ��");
			dialogWelcome.show();
			Log.v(Consts.DEBUG_TAG, "�״������Ի�������ʾ");
			preferences.edit().putBoolean("hasFirstStarted", true).commit();
		} else
			Log.d(Consts.DEBUG_TAG, "���״�����");
	}

}
