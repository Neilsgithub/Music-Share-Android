package com.paperairplane.music.share;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import cn.jpush.android.api.JPushInterface;

public class Receiver extends BroadcastReceiver{
	private static final String TAG = "MyReceiver";
    private Toast toast = null;
	public void onReceive(Context context, Intent intent){
		String action=intent.getAction();
		if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)){
			//System.out.println("Scanning");
			toast = Toast.makeText(context,R.string.refresh_on_process, Toast.LENGTH_SHORT);
			toast.show();
		}else if(Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)){
            toast.setText(R.string.refresh_success);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.show();
		}
		else{
	        Bundle bundle = intent.getExtras();
			Log.d(TAG, "onReceive - " + intent.getAction() + ", extras: " + printBundle(bundle));
			
	        if (JPushInterface.ACTION_REGISTRATION_ID.equals(intent.getAction())) {
	            
	        } else if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
	        	Log.d(TAG, "���ܵ������������Զ�����Ϣ: " + bundle.getString(JPushInterface.EXTRA_MESSAGE));
	        
	        } else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
	            Log.d(TAG, "���ܵ�����������֪ͨ");
	        	
	        } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {
	            Log.d(TAG, "�û��������֪ͨ");
	            
	        	//���Զ����Activity
				Uri uri = Uri.parse(context.getString(R.string.url));
				Intent update = new Intent(Intent.ACTION_VIEW, uri);
				update.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(update);
	        } else {
	        	Log.d(TAG, "Unhandled intent - " + intent.getAction());
	        }
		}
		
	}
	private static String printBundle(Bundle bundle) {
		StringBuilder sb = new StringBuilder();
		for (String key : bundle.keySet()) {
			sb.append("\nkey:" + key + ", value:" + bundle.getString(key));
		}
		return sb.toString();
	}

}
