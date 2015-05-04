package io.pingpal.location;

import io.pingpal.outbox.Outbox;
import io.pingpal.outbox.Outbox.Filter;
import io.pingpal.outbox.Outbox.Inbox;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Log;

public class Manager {
	private static AccessHandler accessHandler;

	private static Inbox startTrackingInbox;
	private static Inbox startDialogInbox;
	@SuppressWarnings("unused")
	private static Map<String, Object> push;

	/**
	 * Initialises the LocationManager and setups the inboxes to receive pings. Needs to be run only once and after the start of Outbox
	 * @param context
	 */
	public final static void Setup(Context context) {

		Tracker.Setup(context);

		attachInboxes();

	}

	private static void attachInboxes() {
		
		if(startTrackingInbox != null){
			Outbox.detachInbox(startTrackingInbox);
		}
		
		startTrackingInbox = new Outbox.Inbox() {
			
			@Override
			public void call(Map<String, Object> payload,
					Map<String, Object> options, Outbox outbox) {

				Log.i("LocationManager",
						"Tracking request received: Payload: " + payload
								+ ". Options: " + options);

				if (accessHandler != null) {

					accessHandler.isItOkToProced(payload, options,
							new Manager.AccessHandler.Callback() {

								@Override
								public void call(
										Map<String, Object> payload,
										Map<String, Object> options,
										Manager.AccessHandler.Callback callback) {

									String from = (String) options
											.get("from");
									String seq = (String) options
											.get("yourseq");

									long time = (Long) payload
											.get("duration");

									new Delegate(from, seq, time);

								}
							});

				} else {

					String from = (String) options.get("from");
					String seq = (String) options.get("yourseq");

					long time = (Long) payload.get("duration");

					new Delegate(from, seq, time);
				}
			}

		};

		Outbox.attachInbox(startTrackingInbox, new Filter() {

			public boolean call(Map<String, Object> payload,
					Map<String, Object> options) {

				return "startTracking".equals(payload.get("action"));
			}
		});
		
		if(startDialogInbox != null){
			Outbox.detachInbox(startDialogInbox);
		}
		
		startDialogInbox = new Outbox.Inbox() {

			@Override
			public void call(Map<String, Object> payload,
					Map<String, Object> options, Outbox outbox) {

				Log.i("LocationManager",
						"Position request received: Payload: " + payload
								+ ". Options: " + options);

				if (accessHandler != null) {

					accessHandler.isItOkToProced(payload, options,
							new Manager.AccessHandler.Callback() {

								@Override
								public void call(
										Map<String, Object> payload,
										Map<String, Object> options,
										Manager.AccessHandler.Callback callback) {

									String from = (String) options
											.get("from");
									String seq = (String) options
											.get("yourseq");

									double accuracy = (Double) payload
											.get("accuracy");
									double timeout = (Double) payload
											.get("timeout");

									new DialogDelegate(from, seq, accuracy,
											timeout);
								}

							});

				} else {

					String from = (String) options.get("from");
					String seq = (String) options.get("yourseq");

					double accuracy = (Double) payload.get("accuracy");
					double timeout = (Double) payload.get("timeout");

					new DialogDelegate(from, seq, accuracy, timeout);
				}

			}
		};

		Outbox.attachInbox(startDialogInbox, new Filter() {

			public boolean call(Map<String, Object> payload,
					Map<String, Object> options) {

				return "startDialog".equals(payload.get("action"));
			}
		});
	}

	
	/**
	 * Used to track another device.
	 * @param tag
	 * @param duration
	 * @param inbox
	 * @param options
	 */
	void trackDevicePosition(String tag, Long duration, Inbox inbox,
			Map<String, Object> options) {

		if (tag == null) {
			Log.i("LocationManager",
					"Can't send tracking request without a tag.");
			return;
		}

		Log.i("LocationManager", "Sending tracking request.");

		Map<String, Object> payload = new HashMap<String, Object>();

		payload.put("action", "startTracking");
		payload.put("duration", duration);

		Outbox.put(tag, payload, options, inbox);

	}

	/**
	 * Sends a message to the tag that stops tracking. Should only be called if you are tracking that tag.
	 * @param tag The tag of the user you want to stop tracking
	 */
	void stopTrackingDevicePosition(String tag) {

		if (tag == null) {
			Log.i("LocationManager", "Can't send stop tracking without a tag.");

			return;
		}

		Log.i("LocationManager", "Sending stop tracking.");

		Map<String, Object> payload = new HashMap<String, Object>();

		payload.put("action", "stopTracking");

		Outbox.put(tag, payload);
	}

	/**
	 * Used to get a single position from another user. It will return a position when it comes within the wanted accuracy, or when the time runs out.
	 * @param tag The tag of whom you want to request a location.
	 * @param accuracy The accuracy you want the position to be, in meters. Defaults to 100. If set to 0 the default will be used.
	 * @param timeout The amount of time you want to wait for a position within your accuracy, in seconds. Defaults to 30. If set to 0 the default will be used.
	 * @param inbox The inbox where you want to receive the location.
	 * @param options The options data.
	 */
	public final static void getDevicePosition(String tag, double accuracy,
			double timeout, Inbox inbox, Map<String, Object> options) {

		if (tag == null) {
			Log.i("LocationManager",
					"Can't send position request without a tag.");

			return;
		}

		Log.i("LocationManager", "Sending position request.");

		Map<String, Object> payload = new HashMap<String, Object>();

		payload.put("action", "startDialog");
		payload.put("accuracy", accuracy);
		payload.put("timeout", timeout);

		Outbox.put(tag, payload, options, inbox);
	}
	
	public void answerTrackingWithPush(Map<String, Object> push){
		Manager.push = push;
		
	}
	
/**
 * This is used to set an AccessHandler to handle access. 
 * You can use the information in the call to check if you want to answer the ping or not. 
 * If you want to answer the ping simply call call callback.call() in the AccessHandler.
 * If the AccessHandler is not set every ping will automatically be accepted.
 * @param theAccessHandler
 */
	public final static void setAccesshandler(AccessHandler theAccessHandler) {
		accessHandler = theAccessHandler;
	}
	
	public interface AccessHandler {
		
		/**
		 * Use this to decide if it's okay to proceed after a location request. 
		 * If you want to answer the ping simply call callback.call() to proceed.
		 * 
		 * @param payload
		 * @param options
		 * @param callback
		 */
		public void isItOkToProced(Map<String, Object> payload,
				Map<String, Object> options, Callback callback);

		static public interface Callback {

			public void call(Map<String, Object> payload,
					Map<String, Object> options, Callback callback);
		}
	}
}
