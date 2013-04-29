package com.paperairplane.music.share;

import java.util.Iterator;
import java.util.List;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.paperairplane.music.share.MyLogger;

public class IntentResolver {
	private Context mCtx;
	private PackageManager pm;
	private Handler mHandler;
	private Class<?> mClassInternalR, mClassId, mClassLayout;

	public void handleIntent(Context ctx, Intent i, Handler handler) {
		mCtx = ctx;
		mHandler = handler;
		boolean view = i.getAction().equals(Intent.ACTION_VIEW);
		try {
			mClassInternalR = Class.forName("com.android.internal.R");
			@SuppressWarnings("rawtypes")
			Class[] classArr = mClassInternalR.getClasses();
			mClassId = classArr[8];
			mClassLayout = classArr[6];
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		// �ҵ����ˡ����ڲ���ֻ����ô�á���
		pm = ctx.getPackageManager();
		List<ResolveInfo> info = pm.queryIntentActivities(i,
				PackageManager.MATCH_DEFAULT_ONLY);
		MyLogger.d(Consts.DEBUG_TAG, "handleIntent");
		// ��������ACTION_SEND��ACTION_VIEW
		Iterator<ResolveInfo> it = info.iterator();
		while (it.hasNext()) {
			if (it.next().labelRes == R.string.title_activity_main) {
				it.remove();
			}
		}
		if (!view) {
			// ��ΪACTION_VIEW,ȥ������ѡ��
			// ��ΪSEND���������õ�΢��������
			// ͬѧ,SENDҲҪȥ���Լ���,��Ȼ�����Լ���jiong��
			// ����,Ŀ�ⲻ��,��ô�������ò��Ż��з��͵�ʱ�򲻳����Լ�����
			ResolveInfo share2weibo = new ResolveInfo();
			share2weibo.icon = R.drawable.weibo_logo;
			share2weibo.labelRes = R.string.share2weibo;
			share2weibo.activityInfo = new ActivityInfo();
			share2weibo.activityInfo.flags = Consts.ShareMeans.INTERNAL;
			info.add(0, share2weibo);
		}
		showDialog(info, view, i);
	}

	private class IntentListAdapter extends BaseAdapter {

		List<ResolveInfo> info;

		public IntentListAdapter(List<ResolveInfo> info) {
			this.info = info;
		}

		@Override
		public int getCount() {

			return info.size();
		}

		@Override
		public Object getItem(int position) {

			return info.get(position);
		}

		@Override
		public long getItemId(int position) {

			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			int idResolveListItem = 0, idIcon = 0, idText1 = 0, idText2 = 0;
			try {

				idResolveListItem = mClassLayout.getField("resolve_list_item")
						.getInt(null);
				idIcon = mClassId.getField("icon").getInt(null);
				idText1 = mClassId.getField("text1").getInt(null);
				idText2 = mClassId.getField("text2").getInt(null);

			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}

			View vwItem = LayoutInflater.from(mCtx).inflate(idResolveListItem,
					null);
			// Ϊ�˷�������������,������Ͻ��������Ǹ�android.jar
			// Ϊ��Դ����ֵ
			ImageView ivItemIcon = (ImageView) vwItem.findViewById(idIcon);
			TextView tvItemLabel = (TextView) vwItem.findViewById(idText1);
			TextView tvItemExtended = (TextView) vwItem.findViewById(idText2);
			Drawable icon;
			String label, extended;
			ResolveInfo ri = info.get(position);
			if (ri.activityInfo.flags != Consts.ShareMeans.INTERNAL) {
				icon = ri.activityInfo.loadIcon(pm);
				label = ri.activityInfo.loadLabel(pm).toString();
				// �ܾ������ǻ�ȡ�Ķ���������,����(��Ҫ�²�)΢�ŵķ���������
				// һ����������Ȧһ�����͸�����,���ڶ���ʾ��"΢��"
				// �ҿ��Ǵ���������һ��ʲô����,������LabeledIntent
				// extended = ri.activityInfo.packageName;
			} else {
				icon = mCtx.getResources().getDrawable(ri.icon);
				label = mCtx.getString(ri.labelRes);
				extended = mCtx.getPackageName();
			}
			ivItemIcon.setImageDrawable(icon);
			tvItemLabel.setText(label);
			// tvItemExtended.setText(extended);
			tvItemExtended.setVisibility(View.GONE);
			return vwItem;
		}

	}

	private void showDialog(final List<ResolveInfo> info, boolean view,
			final Intent i) {
		final Dialog intentDialog = new Dialog(mCtx);
		OnItemClickListener listener = new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ResolveInfo ri = info.get(position);
				boolean isInternal = (ri.activityInfo.flags == Consts.ShareMeans.INTERNAL);
				if (!isInternal) {
					// ������������ʽ
					Intent intent = new Intent(i);
					intent.setFlags(intent.getFlags()
							& ~Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
					intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT
							| Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
							| Intent.FLAG_ACTIVITY_NEW_TASK);
					ComponentName cn = new ComponentName(
							ri.activityInfo.applicationInfo.packageName,
							ri.activityInfo.name);
					intent.setComponent(new ComponentName(cn.getPackageName(),
							cn.getClassName()));
					mCtx.startActivity(intent);
				} else {
					// �������õķ���ʽ
					Bundle bundle;
					bundle = i.getExtras();
					Message m = mHandler.obtainMessage(
							Consts.Status.SEND_WEIBO, bundle);
					mHandler.sendMessage(m);
				}
				intentDialog.cancel();
				// ���������!��Ȼ��������
			}

		};

		ListView v = new ListView(mCtx);
		// Annoying ListView Solved
		/*
		 * int color = 0; try { color =
		 * mClassDrawable.getField("activity_picker_bg").getInt(null); } catch
		 * (IllegalArgumentException e) { e.printStackTrace(); } catch
		 * (IllegalAccessException e) { e.printStackTrace(); } catch
		 * (NoSuchFieldException e) { e.printStackTrace(); }
		 */
		// TODO ��ǿ�պ�
		if (Build.VERSION.SDK_INT < 10) {
			v.setBackgroundColor(mCtx.getResources().getColor(
					android.R.color.background_dark));
		}
		v.setCacheColorHint(0);
		v.setAdapter(new IntentListAdapter(info));
		v.setOnItemClickListener(listener);
		intentDialog.setContentView(v);
		String title = (view) ? mCtx.getString(R.string.how_to_play) : mCtx
				.getString(R.string.how_to_share);
		intentDialog.setTitle(title);
		intentDialog.show();

	}

}
