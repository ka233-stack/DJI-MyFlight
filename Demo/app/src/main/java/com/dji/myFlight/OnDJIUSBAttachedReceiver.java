package com.dji.myFlight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import dji.sdk.sdkmanager.DJISDKManager;

/**
 * This receiver will detect the USB attached event.
 * It will check if the app has been previously started.
 * If the app is already running, it will prevent a new activity from being started and bring the
 * existing activity to the top of the stack.
 */

public class OnDJIUSBAttachedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ConnectionActivity.isStarted()) {
            Intent startIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(context.getPackageName());
            startIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(startIntent);
        } else {
            Intent attachedIntent = new Intent();
            attachedIntent.setAction(DJISDKManager.USB_ACCESSORY_ATTACHED);
            context.sendBroadcast(attachedIntent);
        }
    }
}
