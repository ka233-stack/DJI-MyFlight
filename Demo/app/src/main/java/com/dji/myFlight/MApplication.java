package com.dji.myFlight;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import androidx.multidex.MultiDex;

import com.secneo.sdk.Helper;

import dji.ux.beta.core.communication.DefaultGlobalPreferences;
import dji.ux.beta.core.communication.GlobalPreferencesManager;

import static com.dji.myFlight.DJIConnectionControlActivity.ACCESSORY_ATTACHED;

/**
 * An application that loads the SDK classes.
 */
public class MApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //For the global preferences to take effect, this must be done before the widgets are initialized
        //If this is not done, no global preferences will take effect or persist across app restarts
        GlobalPreferencesManager.initialize(new DefaultGlobalPreferences(this));

        BroadcastReceiver br = new OnDJIUSBAttachedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACCESSORY_ATTACHED);
        registerReceiver(br, filter);
    }

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        MultiDex.install(this);
    }

}
