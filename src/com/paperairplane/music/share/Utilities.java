package com.paperairplane.music.share;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

class Utilities {
	private final static String API_URL = "http://paperairplane.sinaapp.com/proxy.php?q=";
	private final static String FEEDBACK_URL="http://paperairplane.sinaapp.com/feedback.php";
	private final static int INTERNET_ERROR = 3;
	private final static String DEBUG_TAG = "Music Share DEBUG";
	final private static int MUSIC = 0, ARTWORK = 1, ARTIST = 2, ALBUM = 3,
			VERSION = 4;

	/**
	 * ��integer���͵�ʱ�䳤�ȸ�ʽ��
	 * 
	 * @param _duration
	 *            int���͵�ʱ�䳤�ȣ�ms��
	 * @return ��ʽ���õ�ʱ���ַ���
	 */
	public static String convertDuration(long _duration) {
		/*
		 * _duration /= 1000; String min, hour, sec; if (_duration / 3600 > 0) {
		 * return (((hour = ((Integer) (_duration / 3600)).toString()) .length()
		 * == 1) ? "0" + hour : hour) + ":" + (((min = ((Integer) (_duration /
		 * 60)).toString()) .length() == 1) ? "0" + min : min) + ":" + (((sec =
		 * ((Integer) (_duration % 60)).toString()) .length() == 1) ? "0" + sec
		 * : sec); } else { return (((min = ((Integer) (_duration /
		 * 60)).toString()).length() == 1) ? "0" + min : min) + ":" + (((sec =
		 * ((Integer) (_duration % 60)).toString()) .length() == 1) ? "0" + sec
		 * : sec); }
		 */
		StringBuffer sb = new StringBuffer();
		long m = _duration / (60 * 1000);
		sb.append(m < 10 ? "0" + m : m);
		sb.append(":");
		long s = (_duration % (60 * 1000)) / 1000;
		sb.append(s < 10 ? "0" + s : s);
		return sb.toString();
		// ��,ֱ�����˼ҵķ�����,��
	}

	/**
	 * ͨ������API��ȡ���ֵ���Ϣ
	 * 
	 * @param title
	 *            ���ֱ���
	 * @param artist
	 *            ��������
	 * @param context
	 *            ���ڻ�ȡ��Դ��context
	 * @param handler
	 *            ���ڿ���UI�̵߳�Handler
	 * @return �������������ַ�����֡�ר��������orר����ר��������ַ�������
	 */
	public static String[] getMusicAndArtworkUrl(String title, String artist,
			Context context, Handler handler) {
		Log.v(DEBUG_TAG, "���� getMusicAndArtworkUrl������");
		String json = getJson(title, artist, handler);
		String info[] = new String[5];
		if (json == null) {
			info[MUSIC] = context.getString(R.string.no_music_url_found);
			Log.v(DEBUG_TAG, "���� getMusicAndArtworkUrl��ÿյ�json�ַ���");
		} else {
			try {
				JSONObject rootObject = new JSONObject(json);
				int count = rootObject.getInt("count");
				if (count == 1) {
					JSONArray contentArray = rootObject.getJSONArray("musics");
					JSONObject item = contentArray.getJSONObject(0);
					info[MUSIC] = item.getString("mobile_link");
					info[ARTWORK] = item.getString("image");
					info[ARTIST] = item.getJSONArray("author").getJSONObject(0)
							.getString("name");
					info[ALBUM] = item.getJSONObject("attrs")
							.getString("title");
					info[VERSION] = item.getJSONObject("attrs").getString(
							"version");
					// ����,����,�����Ͳ����е��۵Ŀհ״�����
				} else {
					info[MUSIC] = context
							.getString(R.string.no_music_url_found);
					info[ARTWORK] = null;
					info[ARTIST] = null;
					info[ALBUM] = null;
					info[VERSION] = null;
				}
			} catch (JSONException e) {
				Log.e(DEBUG_TAG, "JSON��������");
				e.printStackTrace();
				info[MUSIC] = context.getString(R.string.no_music_url_found);
				info[ARTWORK] = null;
				info[ARTIST] = null;
				info[ALBUM] = null;
				info[VERSION] = null;
			}
		}
		if (info[ALBUM] != null) {
			info[ALBUM] = info[ALBUM].replace("[\"", "").replace("\"]", "");
			Log.d(DEBUG_TAG, info[ALBUM]);
		}
		// Log.v(DEBUG_TAG, info[MUSIC]);
		// Log.v(DEBUG_TAG, info[ARTWORK]);
		// ��Log�Ļ��������������ֵ��null�ͻ������������catch
		return info;
	}

	public static InputStream getImageStream(String artwork_url)
			throws Exception {
		URL url = new URL(artwork_url);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(5 * 1000);
		conn.setRequestMethod("GET");
		if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
			return conn.getInputStream();
		}
		return null;
	}

	public static void saveFile(Bitmap bitmap, String fileName,
			String artwork_path) throws IOException {
		File dirFile = new File(artwork_path);
		if (!dirFile.exists()) {
			dirFile.mkdir();
		}
		File artwork = new File(artwork_path + fileName);
		BufferedOutputStream bos = new BufferedOutputStream(
				new FileOutputStream(artwork));
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
		bos.flush();
		bos.close();

	}

	public static String getArtwork(String artwork_url, String title,
			String artwork_path) {
		String fileName = title + ".jpg";
		if (new File(artwork_path + fileName).exists())
			return fileName;
		try {
			Bitmap bitmap = BitmapFactory.decodeStream(Utilities
					.getImageStream(artwork_url));
			Utilities.saveFile(bitmap, fileName, artwork_path);
			Log.v(DEBUG_TAG, "��ȡר������ɹ�");
			return fileName;
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(DEBUG_TAG, "��ȡר������ʧ��" + e.getMessage());
			return null;
		}
	}

	// ͨ������API��ȡ������Ϣ
	private static String getJson(String title, String artist, Handler handler) {
		Log.v(DEBUG_TAG, "���� getJSON������");
		String json = null;
		HttpResponse httpResponse;
		try {
			String api_url = API_URL
					+ java.net.URLEncoder.encode(title + "+" + artist, "UTF-8");
			Log.v(DEBUG_TAG, "���� getJSON��Ҫ���е�����Ϊ" + api_url);
			HttpGet httpGet = new HttpGet(api_url);

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

	public static boolean sendFeedback(String content) {
		HttpPost post=new HttpPost(FEEDBACK_URL);
		List<NameValuePair> params=new ArrayList<NameValuePair>();
		Log.v(DEBUG_TAG,"content is "+content);
		try {
			params.add(new BasicNameValuePair("content", java.net.URLEncoder.encode(content,"UTF-8")));
			Log.v(DEBUG_TAG,"param is "+params.toString());
			post.setEntity(new UrlEncodedFormEntity(params));
			HttpResponse response=new DefaultHttpClient().execute(post);
			if (response.getStatusLine().getStatusCode()==200){
				Log.v(DEBUG_TAG,"Feedback succeed");
				return true;
			}
			else throw new RuntimeException();
		} catch (Exception e) {
			Log.d(DEBUG_TAG,"Feedbak failed");
			e.printStackTrace();
			return false;
		}

		
		
	}

}
