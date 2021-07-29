package com.dji.myFlight;

import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;

/**
 * Controls the connection to the USB accessory. This activity listens for the USB attached action
 * and sends a broadcast with an internal code which is listened to by the
 * {@link OnDJIUSBAttachedReceiver}.
 */
public class DJIConnectionControlActivity extends Activity {

    public static final String ACCESSORY_ATTACHED = "dji.myFlight.ACCESSORY_ATTACHED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new View(this));

        Intent usbIntent = getIntent();
        if (usbIntent != null) {
            String action = usbIntent.getAction();
            if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
                Intent attachedIntent = new Intent();
                attachedIntent.setAction(ACCESSORY_ATTACHED);
                sendBroadcast(attachedIntent);
            }
        }

        finish();
    }
}
