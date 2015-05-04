
package io.pingpal.messenger;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Activity;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

/**
 * @author Robin Dahlström 22-03-2015
 */
public class GcmIntentService extends IntentService {

    public static final int NOTIFICATION_ID = 1;

    @SuppressWarnings("unused")
	private static final String TAG = GcmIntentService.class.getSimpleName();

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    	
    	if(!isAppActive()){       
           	sendNotification(getString(R.string.got_message));
    	}

        // Release the wake lock provided by the WakefulBroadcastReceiver
        GcmBroadcastReceiver.completeWakefulIntent(intent);
        
    }

    private NotificationManager mNotificationManager;

    private NotificationCompat.Builder mBuilder;

    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)this
                .getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                MainActivity.class), 0);
        
        Uri sound = Uri.parse(Communications.ANDROID_RESOURCE + getApplicationContext().getPackageName() + Communications.FSLASH
                + R.raw.notification);

        mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("PingPal Messenger")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg)).setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        
        //Check if sound is enabled
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("pingpal", Activity.MODE_PRIVATE);
        boolean soundEnabled = prefs.getBoolean(Keys.Preferences.SOUND_ENABLED, true);
        
        if (soundEnabled){
        	mBuilder.setSound(sound);
        }
             
        mBuilder.setAutoCancel(true);
        
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

    }
    
	public boolean isAppActive(){
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> services = activityManager
                .getRunningTasks(Integer.MAX_VALUE);
        boolean isActivityFound = false;

        if (services.get(0).topActivity.getPackageName().toString()
                .equalsIgnoreCase(getApplicationContext().getPackageName().toString())) {
            isActivityFound = true;
        }
        
        if (isActivityFound) {
        	return true;
        } else {
        	return false;
        }
	}

}
