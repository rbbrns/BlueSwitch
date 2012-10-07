package com.theultimatelabs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BlueBroadcastReceiver extends BroadcastReceiver {
	final static public String TAG =  BlueBroadcastReceiver.class.getName();
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG,intent.getAction());		
	}
}
