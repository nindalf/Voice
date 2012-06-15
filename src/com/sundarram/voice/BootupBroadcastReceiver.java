package com.sundarram.voice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This BroadcastReceiver is launched upon receiving any of the following intents.
 * ACTION_MY_PACKAGE_REPLACED
 * ACTION_PACKAGE_FIRST_LAUNCH
 * ACTION_BOOT_COMPLETED
 * ACTION_PACKAGE_RESTARTED
 * It is not possible to start the service upon install, but perhaps these 4 intents should take care of it.
 */
public class BootupBroadcastReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Intent startServiceIntent = new Intent(context, VoiceService.class);
        context.startService(startServiceIntent);
        Log.i("VoiceService", "BroadcastReceiver Starting VoiceService.");
    }
}
