package com.paperairplane.music.share;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

class Utilities {
	private final static String API_URL = "http://paperairplane.sinaapp.com/proxy.php?q=";
	private final static int INTERNET_ERROR = 3;
	private final static String DEBUG_TAG = "Music Share DEBUG";
	final private static int MUSIC = 0, ARTWORK = 1, ARTIST = 2, ALBUM = 3,
			VERSION = 4;

	/**
	 * ��integer���͵�ʱ�䳤�ȸ�ʽ��
	 * 
	 * @param _duration int���͵�ʱ�䳤�ȣ�ms��
	 * @return ��ʽ���õ�ʱ���ַ���
	 */
	public static String convertDuration(long _duration) {
		/*_duration /= 1000;
		String min, hour, sec;
		if (_duration / 3600 > 0) {
			return (((hour = ((Integer) (_duration / 3600)).toString())
					.length() == 1) ? "0" + hour : hour)
					+ ":"
					+ (((min = ((Integer) (_duration / 60)).toString())
							.length() == 1) ? "0" + min : min)
					+ ":"
					+ (((sec = ((Integer) (_duration % 60)).toString())
							.length() == 1) ? "0" + sec : sec);
		} else {
			return (((min = ((Integer) (_duration / 60)).toString()).length() == 1) ? "0"
					+ min
					: min)
					+ ":"
					+ (((sec = ((Integer) (_duration % 60)).toString())
							.length() == 1) ? "0" + sec : sec);
		}
		*/
        StringBuffer sb = new StringBuffer();
        long m = _duration / (60 * 1000);
        sb.append(m < 10 ? "0" + m : m);
        sb.append(":");
        long s = (_duration % (60 * 1000)) / 1000;
        sb.append(s < 10 ? "0" + s : s);
        return sb.toString();
        //��,ֱ�����˼ҵķ�����,��
	}

	/**
	 * ͨ������API��ȡ���ֵ���Ϣ
	 * 
	 * @param title      ���ֱ���
	 * @param artist     ��������
	 * @param context    ���ڻ�ȡ��Դ��context
	 * @param handler    ���ڿ���UI�̵߳�Handler
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

}
