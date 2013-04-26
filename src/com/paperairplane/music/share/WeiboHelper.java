package com.paperairplane.music.share;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.WeiboAuthListener;
import com.weibo.sdk.android.WeiboDialogError;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.WeiboParameters;
import com.weibo.sdk.android.api.StatusesAPI;
import com.weibo.sdk.android.net.AsyncWeiboRunner;
import com.weibo.sdk.android.net.RequestListener;

import com.paperairplane.music.share.Consts;
import com.paperairplane.music.share.MyLogger;

public class WeiboHelper {
	private StatusesAPI mApi = null;
	private Handler mHandler = null;
	private Context mContext;
	private AuthDialogListener mAuthListener;
	private RequestListener mRequestListener;
	private SharedPreferences mPreferences;
	private Editor mEditor;

	/**
	 * WeiboHelper���캯��
	 * 
	 * @param handler
	 *            ���ڿ���UI�̵߳�Handler
	 * @param context
	 *            �����ݴ�΢�����ݺ�������Ϣ�ģ�Application��Context
	 */
	// ��ô���ʡ�����ĵڶ���Activity��ContextҪ����ʲô?�ƺ���û�õ�����
	public WeiboHelper(Handler handler, Context context) {
		mHandler = handler;
		mContext = context;
		mPreferences = mContext.getSharedPreferences(
				Consts.Preferences.WEIBO, Context.MODE_APPEND);
		mEditor = mPreferences.edit();
	}

	/**
	 * ��ȡ΢����Ȩ������ʵ��
	 * 
	 * @return ΢����Ȩ������
	 */
	public AuthDialogListener getListener() {
		if (mAuthListener == null) {
			mAuthListener = new AuthDialogListener();
		}
		MyLogger.v(Consts.DEBUG_TAG,"����WeiboHelper::getListener()������");
		return mAuthListener;
	}

	/**
	 * ����΢��
	 * 
	 * @param content
	 *            Ҫ���͵�΢������
	 * @param artworkUrl
	 *            Ҫ���͵�ͼƬ
	 * @param fileName
	 * @param willFollow
	 *            �Ƿ�Ҫ��ע
	 */
	public void sendWeibo(String content, String artworkUrl, String fileName,
			boolean willFollow) {
		mApi = new StatusesAPI(Main.sAccessToken);
		initRequestListener();

		if (artworkUrl == null) {
			MyLogger.v(Consts.DEBUG_TAG, "������ͼ΢��");
			mApi.update(content, null, null, mRequestListener);
		} else if (fileName != null) {
			MyLogger.v(Consts.DEBUG_TAG, "���������ط����΢��");
			mApi.upload(content, fileName, null, null, mRequestListener);
		} else {
			MyLogger.v(Consts.DEBUG_TAG, "���ʹ�ͼ΢����url=" + artworkUrl);
			String url = "https://mApi.weibo.com/2/statuses/upload_url_text.json";
			WeiboParameters params = new WeiboParameters();
			params.add("access_token", Main.sAccessToken.getToken());
			params.add("status", content);
			params.add("url", artworkUrl);
			AsyncWeiboRunner.request(url, params, "POST", mRequestListener);
		}
		if (willFollow == true) {// �ж��Ƿ�Ҫ��ע������
			follow(Consts.WeiboUid.HARRY_UID);// ��עHarry Chen
			follow(Consts.WeiboUid.XAVIER_UID);// ��עXavier Yao
			follow(Consts.WeiboUid.APP_UID);// ��ע�ٷ�΢��
		}
	}

	private void initRequestListener() {
		mRequestListener = new RequestListener() {
			Message m = mHandler.obtainMessage();

			@Override
			public void onComplete(String arg0) {
				mHandler.sendEmptyMessage(Consts.Status.SEND_SUCCEED);
			}

			@Override
			public void onError(WeiboException e) {
				String error = e.getMessage();
				m.what = Consts.Status.SEND_ERROR;
				m.obj = error;
				mHandler.sendMessage(m);
			}

			@Override
			public void onIOException(IOException arg0) {
				String error = arg0.getMessage();
				m.what = Consts.Status.SEND_ERROR;
				m.obj = error;
				mHandler.sendMessage(m);
			}

		};
	}

	// ��עĳ��
	private void follow(int uid) {
		WeiboParameters params = new WeiboParameters();
		params.add("access_token", Main.sAccessToken.getToken());
		params.add("uid", uid);
		String url = "https://mApi.weibo.com/2/friendships/create.json";
		try {
			AsyncWeiboRunner.request(url, params, "POST",
					new RequestListener() {
						@Override
						public void onComplete(String arg0) {
							MyLogger.v("Music Share DUBUG", "followed");
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

	private class AuthDialogListener implements WeiboAuthListener {
		Message m = mHandler.obtainMessage();

		@Override
		public void onComplete(Bundle values) {
			MyLogger.d(Consts.DEBUG_TAG, "���յ���Ȩ��Ϣ");
			String token = values.getString("access_token");
			String expires_in = values.getString("expires_in");
			Main.sAccessToken = new Oauth2AccessToken(token, expires_in);
			keepAccessToken(Main.sAccessToken);
			mHandler.sendEmptyMessage(Consts.Status.AUTH_SUCCEED);
			MyLogger.v(Consts.DEBUG_TAG, "��Ȩ�ɹ���\n AccessToken:" + token);
			SharedPreferences preferences = mContext
					.getSharedPreferences("ShareStatus", Context.MODE_PRIVATE);
			if (preferences.getBoolean("read", false)) {
				String content = preferences.getString("content", null);
				String artworkUrl = preferences.getString("artworkUrl", null);
				String fileName = preferences.getString("fileName", null);
				boolean willFollow = preferences
						.getBoolean("willFollow", false);
				MyLogger.v(Consts.DEBUG_TAG, "��ȡ״̬\n" + content + "\n" + artworkUrl
						+ "\n" + willFollow);
				sendWeibo(content, artworkUrl, fileName, willFollow);
				preferences.edit().putBoolean("read", false).commit();
			}
		}

		@Override
		public void onCancel() {

		}

		@Override
		public void onError(WeiboDialogError e) {
			String error = e.getMessage();
			m.what = Consts.Status.AUTH_ERROR;
			m.obj = error;
			mHandler.sendMessage(m);
		}

		@Override
		public void onWeiboException(WeiboException e) {
			String error = e.getMessage();
			m.what = Consts.Status.AUTH_ERROR;
			m.obj = error;
			mHandler.sendMessage(m);
		}
	}

	private void keepAccessToken(Oauth2AccessToken token) {
		mEditor.putString("token", token.getToken());
		mEditor.putLong("expiresTime", token.getExpiresTime());
		mEditor.commit();
	}

	public void clear() {
		mEditor.clear();
		mEditor.commit();
	}

	public Oauth2AccessToken readAccessToken() {
		Oauth2AccessToken token = new Oauth2AccessToken();
		token.setToken(mPreferences.getString("token", ""));
		token.setExpiresTime(mPreferences.getLong("expiresTime", 0));
		MyLogger.d(Consts.DEBUG_TAG,"Read Token:"+token.getToken());
		return token;
	}

}
