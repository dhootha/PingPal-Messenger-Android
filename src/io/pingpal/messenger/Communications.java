
package io.pingpal.messenger;

import io.pingpal.database.FriendsDataSource;
import io.pingpal.database.MessagesDataSource;
import io.pingpal.models.Message;
import io.pingpal.models.Person;
import io.pingpal.outbox.Outbox;
import io.pingpal.outbox.Outbox.Callback;
import io.pingpal.outbox.Outbox.Filter;
import io.pingpal.outbox.Outbox.Inbox;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * @author Paul Mallon & Robin Dahlström 22-07-2014
 */
public class Communications implements Serializable {

    public static final String PROPERTY_REG_ID = "registration_id";

    public static final String EXTRA_MESSAGE = "message";

    public static final String KEY_COMMUNICATIONS = "communications";

    public static final String ANDROID_RESOURCE = "android.resource://";

    public static final String FSLASH = "/";

    protected static final String PREFS_PUSH_MESSAGING = "push_messaging";

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final String TAG = Communications.class.getSimpleName();

    private final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private final String PREFS_KEY_DEVICE_TAG = "device_tag";

    private final String PREFS_NAME = "pingpal";

    private final String PROPERTY_APP_VERSION = "appVersion";

    private MainActivity mMainActivity;

    private Context mContext;

    public Communications(MainActivity main) {
        mMainActivity = main;
        mContext = main.getApplicationContext();
    }

    private String deviceTag;

    private GoogleCloudMessaging gcm;

    private String gcmRegId;
    
    private static Inbox messageInbox;

    public void setupApptimateOutbox() {
    	
        String userTag = mMainActivity.getUserTag();
        
        SharedPreferences prefs = mMainActivity.getSharedPreferences(PREFS_NAME,
                FragmentActivity.MODE_PRIVATE);
        Editor prefsEditor = prefs.edit();
        deviceTag = prefs.getString(PREFS_KEY_DEVICE_TAG, null);
        //boolean isPush = prefs.getBoolean(PREFS_PUSH_MESSAGING, false);
        //Log.v(TAG, "Registered for Push Messaging: " + isPush);

        final String PUBLIC_KEY = mContext.getString(R.string.outbox_public_key);
        final String PRIVATE_KEY = mContext.getString(R.string.outbox_private_key);
        
        Outbox.setAPIKeys(PUBLIC_KEY, PRIVATE_KEY);

        if (deviceTag == null) {
            deviceTag = Outbox.createUniqueTag();
            //Log.v("TAG", "DeviceTag: " + deviceTag);
            prefsEditor.putString(PREFS_KEY_DEVICE_TAG, deviceTag);
            prefsEditor.commit();
        } else {
            //Log.v(TAG, "Device Tag: " + deviceTag);
        }

        attachApptimateInbox(userTag);
        
        //starts Outbox and listens when someone sends to appUser
        Outbox.startWithAlias(deviceTag, userTag);
        
        //get group tags
        MessagesDataSource db = new MessagesDataSource(mContext);
        db.open();
        List<String> groupTag = db.getAllGroupTags(userTag);
        db.close();
        
        //listen to group tags
        for (String aGroupTag : groupTag) {
        	Outbox.subscribeTag(userTag, aGroupTag);
		}
        
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(mMainActivity);
            gcmRegId = getRegistrationId(mContext);

            if (gcmRegId.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");

        }
    }

    /**
     * Creates and attaches a new Outbox.Inbox to the Apptimate Outbox
     */
    private void attachApptimateInbox(final String userTag) { 
    	
    	if (messageInbox != null){
    		Outbox.detachInbox(messageInbox);
    	}
    	
    	messageInbox = new Outbox.Inbox() {
			
            @Override
            public void call(Map<String, Object> payload, Map<String, Object> options, Outbox outbox) {
                //Log.v(TAG, "Message Payload: " + payload);
                //Log.v(TAG, "Message options: " + options);
                String receiverTag = (String)options.get("to");
                String senderTag = (String)options.get("from");

                String messageBody = null;
                Message message = null;
                
                //Get key from payload
                String payloadKey =  (String) payload.keySet().toArray()[0];
                
                messageBody = (String) payload.get(payloadKey);
                        
                if (!(messageBody == null) && !(senderTag == null) && !(senderTag.equals(userTag))) {
                    
                    //Log.v(TAG, "Message Received: to:" + receiverTag + " from: " + senderTag + " text: "
                    //        + messageBody);
                    
                    MessagesDataSource messagesDb = new MessagesDataSource(mContext);
                    
                    messagesDb.open(); 
                    
                    boolean isGroupMessage = false;
                    
                    //check if receiver is a group
                    /*if(payloadKey.equals(Keys.Payload.GROUP_MESSAGE)){
                    	isGroupMessage = true;
                    }*/
                    
                    if (messagesDb.getGroupID(receiverTag) != -1){
                    	isGroupMessage = true;
                    }
                	
                    if(isGroupMessage){
                    	int groupID = messagesDb.getGroupID(receiverTag);
                        message = new Message(senderTag, messageBody, new Date().getTime(), 
                        		-1, groupID, receiverTag);
                    }
                    else{
                        //Add conversation to db if not exist
                        int conversationId = messagesDb.addConversation(receiverTag, senderTag);
                        message = new Message(senderTag, messageBody, new Date().getTime(), 
                        		conversationId, -1, receiverTag);
                    }  
                    
                    messagesDb.addMessage(message);
                    messagesDb.close();  
                    
                    messageBody = renameIconLocationMessage(messageBody, mMainActivity.getApplicationContext());
                    buildNotificationMessage(messageBody, senderTag, isGroupMessage);
                    mMainActivity.updateConversationFragment();
                }
            }
            
            /**
             * @param text The message to appear in the notification
             * @param fromTag The ID of the user who sent the message
             */
            private void buildNotificationMessage(String text, String fromTag, boolean isGroupMessage) {

                Uri sound = Uri.parse(ANDROID_RESOURCE + mContext.getPackageName() + FSLASH
                        + R.raw.notification);
                
                Intent resultIntent = new Intent(mContext, MainActivity.class);
                resultIntent.putExtra("from", fromTag);
                
                //Get name of sender
                String name = "";
                FriendsDataSource db = new FriendsDataSource(mContext);
                
                db.open();
                db.getFriend(fromTag).getName();
                Person friend = db.getFriend(fromTag);
                
                //Get friends name from db
                if(friend != null){
                	name = friend.getName();
                }
                else{
                	name = "Unknown";
                }

                db.close();
                
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                        mContext);
                
                String notification;
                if(isGroupMessage){
                    notification = mContext.getString(R.string.group_notification) + " " +  name;
                }
                else{
                	notification = mContext.getString(R.string.notification) + " " +  name;
                }
          
                notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
                notificationBuilder.setContentTitle(notification);
                notificationBuilder.setContentText(text);
                notificationBuilder.setAutoCancel(true);
                
                //Check if sound is enabled
                SharedPreferences prefs = mMainActivity.getPreferences(Activity.MODE_PRIVATE);
                boolean soundEnabled = prefs.getBoolean(Keys.Preferences.SOUND_ENABLED, true);
                
                if (soundEnabled){
                    notificationBuilder.setSound(sound);
                }


                TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                notificationBuilder.setContentIntent(resultPendingIntent);
                NotificationManager notificationManager = (NotificationManager)mContext
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(0, notificationBuilder.build());
            }
		};
		
		Filter messageFilter = new Filter() {
			
			@Override
			public boolean call(Map<String, Object> payload, Map<String, Object> options) {
				
				if (payload.containsKey(Keys.Payload.MESSAGE)
						|| payload.containsKey(Keys.Payload.ICON)
						|| payload.containsKey(Keys.Payload.GROUP_MESSAGE)
						|| payload.containsKey(Keys.Payload.GROUP_ICON)) {
					return true;
				}
				
				return false;
			}
		};
		
		Outbox.attachInbox(messageInbox, messageFilter);
    }
    
	public static String renameIconLocationMessage(String messageBody, Context context) {
		//Rename if icon or message
		if(messageBody.startsWith("@locData")){
			messageBody = context.getString(R.string.sent_location);
		}
		else if (messageBody.endsWith("h100.png")){
			messageBody = context.getString(R.string.sent_icon);
		}
		return messageBody;
	}
	
    public void resetComms() {
    	
    	if(messageInbox != null){
        	Outbox.detachInbox(messageInbox);
    	}
        
        SharedPreferences prefs = mMainActivity.getSharedPreferences(PREFS_NAME,
                FragmentActivity.MODE_PRIVATE);
        deviceTag = prefs.getString(PREFS_KEY_DEVICE_TAG, "");
        
        String userTag = mMainActivity.getUserTag();
        
    	Outbox.unsubscribeTag(deviceTag, userTag);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If it
     * doesn't, display a dialog that allows users to download the APK from the
     * Google Play Store or enable it in the device's system settings.
     */
    public boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, mMainActivity,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                mMainActivity.finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            //Log.i(TAG, "Registration not found.");
            return "";
        }
        
        //Log.v(TAG, "App is registered with id: " + registrationId);
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }

        return registrationId;
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences,
        return mContext.getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(mContext);
                    }
                    
                    final String SENDER_ID = mContext.getString(R.string.gcm_project_id);
                    gcmRegId = gcm.register(SENDER_ID);
                    //msg = "Device registered, registration ID=" + gcmRegId;

                    sendRegistrationIdToBackend();
                    storeRegistrationId(mContext, gcmRegId);

                } catch (IOException ex) {
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return null;
            }
        }.execute();

    }

    /**
     * Registers for push notifications on Apptimate server
     */
    private void sendRegistrationIdToBackend() {
    	
        Outbox.Callback registeringCallback = buildRegisterCallback();
        Outbox.registerForPushNotifications(deviceTag, gcmRegId, false, registeringCallback);
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        //Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    private Callback buildRegisterCallback() {
    	
        Outbox.Callback callback = new Outbox.Callback() {

            @Override
            public void call(Throwable arg0, Object... arg1) {
            	
                SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME,
                        Context.MODE_PRIVATE);
                Editor prefsEditor = prefs.edit();
                if (arg0 == null) {
                    prefsEditor.putBoolean(PREFS_PUSH_MESSAGING, true);
                    //Log.v(TAG, "Successfully Registered for Push Messaging");
                } else {
                    prefsEditor.putBoolean(PREFS_PUSH_MESSAGING, false);
                    Log.v(TAG, "Failed to Register for Push Messaging");
                    Log.v(TAG, "Error: " + arg0);

                }
                prefsEditor.commit();
            }
        };
        return callback;
    }
    
}
