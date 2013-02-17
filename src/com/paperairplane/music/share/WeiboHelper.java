package com.paperairplane.music.share;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.WeiboAuthListener;
import com.weibo.sdk.android.WeiboDialogError;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.WeiboParameters;
import com.weibo.sdk.android.api.StatusesAPI;
import com.weibo.sdk.android.net.AsyncWeiboRunner;
import com.weibo.sdk.android.net.RequestListener;

public class WeiboHelper {
	private StatusesAPI api = null;
	final private int HARRY_UID = 1689129907, XAVIER_UID = 2121014783,
			APP_UID = 1153267341;
	private final static String DEBUG_TAG = "Music Share DEBUG";
	private Handler handler = null;
	final private int SEND_SUCCEED = 5, AUTH_ERROR = 6, SEND_ERROR = 7,
			AUTH_SUCCEED = 9;
	private Context applicationContext;
	private AuthDialogListener listener;
	private RequestListener requestListener;
/**
 * WeiboHelper���캯��
 * @param _handler ���ڿ���UI�̵߳�Handler
 * @param _applicationContext �����ݴ�΢�����ݺ�������Ϣ�ģ�Application��Context
 */
	//��ô���ʡ�����ĵڶ���Activity��ContextҪ����ʲô?�ƺ���û�õ�����
	public WeiboHelper(Handler _handler, Context _applicationContext) {
		handler = _handler;
		applicationContext = _applicationContext;
	}
/**
 * ��ȡ΢����Ȩ������ʵ��
 * @return ΢����Ȩ������
 */
	public AuthDialogListener getListener() {
		listener = new AuthDialogListener();
		return listener;
	}
/**
 * ����΢��
 * @param content Ҫ���͵�΢������
 * @param artworkUrl Ҫ���͵�ͼƬ
 * @param willFollow �Ƿ�Ҫ��ע
 */
	public void sendWeibo(String content, String artworkUrl, boolean willFollow) {
		api = new StatusesAPI(Main.accessToken);
		initRequestListener();

		if (artworkUrl == null) {
			Log.v(DEBUG_TAG, "������ͼ΢��");
			api.update(content, null, null, requestListener);
		} else {
			Log.v(DEBUG_TAG, "���ʹ�ͼ΢����url=" + artworkUrl);
			String url = "https://api.weibo.com/2/statuses/upload_url_text.json";
			WeiboParameters params = new WeiboParameters();
			params.add("access_token", Main.accessToken.getToken());
			params.add("status", content);
			params.add("url", artworkUrl);
			AsyncWeiboRunner.request(url, params, "POST", requestListener);
		}
		if (willFollow == true) {// �ж��Ƿ�Ҫ��ע������
			follow(HARRY_UID);// ��עHarry Chen
			follow(XAVIER_UID);// ��עXavier Yao
			follow(APP_UID);// ��ע�ٷ�΢��
		}
	}

	private void initRequestListener() {
		requestListener = new RequestListener() {
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

	public class AuthDialogListener implements WeiboAuthListener {
		Message m = handler.obtainMessage();

		@Override
		public void onComplete(Bundle values) {
			String token = values.getString("access_token");
			String expires_in = values.getString("expires_in");
			Main.accessToken = new Oauth2AccessToken(token, expires_in);
			AccessTokenKeeper.keepAccessToken(applicationContext, Main.accessToken);
			handler.sendEmptyMessage(AUTH_SUCCEED);
			Log.v(DEBUG_TAG, "��Ȩ�ɹ���\n AccessToken:" + token);
			SharedPreferences preferences = applicationContext
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

}
