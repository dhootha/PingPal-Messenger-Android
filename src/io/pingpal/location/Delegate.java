package io.pingpal.location;

import io.pingpal.outbox.Outbox;
import io.pingpal.outbox.Outbox.Filter;
import io.pingpal.outbox.Outbox.Inbox;

import java.util.HashMap;
import java.util.Map;

import android.location.Location;
import android.os.Handler;
import android.util.Log;

class Delegate implements TrackerListenerProtocol {
	
	private Inbox stopTrackingInbox;
	private Tracker tracker;
	private String sendTo, sendSeq;
	
	Delegate(String from, String seq, Long durationSec) {

		sendTo = from;
		sendSeq = seq;
		
		stopTrackingInbox = new Outbox.Inbox() {

			@Override
			public void call(Map<String, Object> payload,
					Map<String, Object> options, Outbox outbox) {

				Log.i("LocationManager", "Tracking stop received.");
				
				stop();
			}

		};

		Outbox.attachInbox(stopTrackingInbox, new Filter() {

			public boolean call(Map<String, Object> payload,
					Map<String, Object> options) {

				return "stopTracking".equals(payload.get("action"));
			}
		});
		
        tracker = Tracker.getSharedTracker();
        tracker.addDelegate(this); 
		
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
          	  
          	  stop();
            }
          }, (long) (durationSec * 1000));
	}

	void stop() {
			Log.i("LocationManager", "Tracking timeout.");
	        tracker = Tracker.getSharedTracker();
	        tracker.removeDelegate(this);
	}

	@Override
	public void setLocation(Location location) {
		double latitude = location.getLatitude();
		double longitude = location.getLongitude();
		double altitude = location.getAltitude();
		float accuracy = location.getAccuracy();
		float course = location.getBearing();
		float speed = location.getSpeed();

		Map<String, Object> locationData = new HashMap<String, Object>();
		locationData.put("latitude", latitude);
		locationData.put("longitude", longitude);
		locationData.put("altitude", altitude);
		locationData.put("horizontalAccuracy", accuracy);
		locationData.put("verticalAccuracy", accuracy);
		locationData.put("course", course);
		locationData.put("speed", speed);
		
		long timeStamp = System.currentTimeMillis() / 1000l;
		locationData.put("timestamp", timeStamp);

		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("location", locationData);
		
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("yourseq", sendSeq);

		Log.i("LocationManager", "Sending location.");

		Outbox.put(sendTo, payload, options);
	}
}
